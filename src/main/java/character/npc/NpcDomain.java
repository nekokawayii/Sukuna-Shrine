package character.npc;

import cn.blockforge.generated.nnnnr1201forge2.Network;
import cn.blockforge.generated.nnnnr1201forge2.NpcCombat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * **NPC 的"真领域"**（r384，用户选 A：跟玩家一样真改地形）。
 *
 * <p>玩家的领域类全部吃 `ServerPlayer`，NPC 传不进去；这里抽出一套给 `Mob` 用的：
 * 逐块记录原始方块 → 施工（黑混凝土球壳 + 内部清空 + 地板）→ 持续伤害 → 到期/死亡/关服整批还原。
 */
public final class NpcDomain {
    /** 各角色自己的领域风格（r387 用户要求：石流龙=烟花、秤金次=赌场…）。 */
    public enum Style { FIREWORK, PACHINKO, LAVA, ICE, BLOOD, GENERIC }

    public static final int RADIUS = 26;
    public static final int TICKS = 400;          // 20 秒
    private static final int BATCH = 2000;
    private static final double DAMAGE = 6;

    private static final class Job {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final long end;
        final Style style;
        final List<long[]> saved = new ArrayList<>();     // 编码：pos | (stateId << 64) 分两段存
        final List<BlockState> states = new ArrayList<>();
        final List<BlockPos> plan = new ArrayList<>();
        int cursor;

        Job(UUID owner, ServerLevel level, BlockPos center, long end, Style style) {
            this.owner = owner; this.level = level; this.center = center; this.end = end; this.style = style;
        }
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    public static boolean active(Mob mob) { return mob != null && JOBS.containsKey(mob.getUUID()); }

    public static void open(Mob mob, double radiusOverride) { open(mob, radiusOverride, Style.GENERIC); }

    public static void open(Mob mob, double radiusOverride, Style style) {
        if (!(mob.level() instanceof ServerLevel level) || active(mob)) return;
        BlockPos center = mob.blockPosition();
        Job job = new Job(mob.getUUID(), level, center, level.getGameTime() + TICKS, style);
        int r = (int) Math.round(radiusOverride);
        // 施工计划：外壳球壳 + 地板（内部先清空气，保证是空地）
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    double d2 = (double) dx * dx + dy * dy + dz * dz;
                    boolean shell = d2 <= (double) r * r && d2 > (double) (r - 1) * (r - 1);
                    boolean floor = dy == -1 && d2 <= (double) r * r;
                    boolean inside = d2 <= (double) (r - 1) * (r - 1) && dy >= 0;
                    if (shell || floor) job.plan.add(center.offset(dx, dy, dz));
                    else if (inside) job.plan.add(center.offset(dx, dy, dz).immutable());
                }
        JOBS.put(mob.getUUID(), job);
        Network.send(level, new Network.Effect(character.ryu.RyuFx.DOMAIN, mob.getId(), Vec3.atCenterOf(center),
            Vec3.ZERO, r, TICKS, 0));
    }

    public static void close(Mob mob, String reason) {
        Job job = JOBS.remove(mob.getUUID());
        if (job == null) return;
        // 还原
        ServerLevel level = job.level;
        for (int i = job.saved.size() - 1; i >= 0; i--) {
            BlockPos pos = BlockPos.of(job.saved.get(i)[0]);
            int id = net.minecraft.world.level.block.Block.getId(job.states.get(i));
            level.setBlock(pos, net.minecraft.world.level.block.Block.stateById(id), 2);
        }
        if (mob != null && !mob.level().isClientSide())
            Network.send(level, new Network.Effect(character.ryu.RyuFx.DOMAIN, mob.getId(), mob.position(),
                Vec3.ZERO, 0f, 0, 0));
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || JOBS.isEmpty()) return;
        // 遍历副本：close() 自己会从 JOBS 里移除，边遍历边 remove 会 ConcurrentModificationException（r385 崩服）
        var shell = Blocks.BLACK_CONCRETE.defaultBlockState();
        var air = Blocks.AIR.defaultBlockState();
        var light = Blocks.LIGHT.defaultBlockState()
            .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 13);
        for (var entry : new ArrayList<>(JOBS.entrySet())) {
            Job job = entry.getValue();
            Mob owner = job.level.getEntity(entry.getKey()) instanceof Mob mob ? mob : null;
            if (owner == null || !owner.isAlive() || job.level.getGameTime() >= job.end) {
                close(owner, "结束");
                continue;
            }
            int done = 0;
            while (job.cursor < job.plan.size() && done < BATCH) {
                BlockPos pos = job.plan.get(job.cursor++);
                BlockState old = job.level.getBlockState(pos);
                if (old.getBlock() == Blocks.BEDROCK) continue;
                job.saved.add(new long[]{pos.asLong()});
                job.states.add(old);
                double dd = pos.distSqr(job.center);
                boolean isShell = dd <= (double) RADIUS * RADIUS && dd > (double) (RADIUS - 1) * (RADIUS - 1);
                boolean isFloor = pos.getY() == job.center.getY() - 1 && dd <= (double) RADIUS * RADIUS;
                boolean outer = dd > (double) (RADIUS - 1) * (RADIUS - 1) && dd <= (double) RADIUS * RADIUS;
                var block = (isFloor || isShell)
                    ? (job.style == Style.FIREWORK && outer ? Blocks.BEDROCK.defaultBlockState() : shell)
                    : air;
                job.level.setBlock(pos, block, 2);
                if (!isShell && !isFloor && (pos.getX() + pos.getY() + pos.getZ()) % 5 == 0)
                    job.level.setBlock(pos, light, 2);
                done++;
            }
            // ── 石流龙·打上花火：每 1 秒两枚火箭炸出追踪弹幕（与玩家同款机制：必中） ──
            if (job.style == Style.FIREWORK && job.level.getGameTime() % 20 == 0) {
                ServerLevel level = job.level;
                var rand = level.random;
                for (int i = 0; i < 2; i++) {
                    Vec3 rise = Vec3.atCenterOf(job.center)
                        .add((rand.nextDouble() - 0.5) * 40, 20 + rand.nextDouble() * 14,
                             (rand.nextDouble() - 0.5) * 40);
                    Network.send(level, new Network.Effect(character.ryu.RyuFx.FIREWORK_RISE,
                        owner.getId(), rise, Vec3.ZERO, 1f, 20, 0));
                    Network.send(level, new Network.Effect(character.ryu.RyuFx.FIREWORK_BURST,
                        owner.getId(), rise, Vec3.ZERO, 3f, 25, 0));
                    int shells = 15 + rand.nextInt(31);
                    for (int s = 0; s < shells; s++) {
                        Vec3 target = rise.add((rand.nextDouble() - 0.5) * 36,
                            -14 - rand.nextDouble() * 10, (rand.nextDouble() - 0.5) * 36);
                        Network.send(level, new Network.Effect(character.ryu.RyuFx.FIREWORK_SHELL,
                            owner.getId(), target, Vec3.ZERO, 0.6f, 30, 0));
                    }
                }
                // 必中：领域内所有玩家吃一波烟花伤害
                for (var pl : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(job.center).inflate(RADIUS), t -> t != owner && t instanceof net.minecraft.world.entity.player.Player)) {
                    pl.hurt(pl.damageSources().magic(), (float) DAMAGE);
                }
            }
            if (job.level.getGameTime() % 10 == 0) {
                double r2 = (double) RADIUS * RADIUS;
                for (LivingEntity living : job.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(job.center).inflate(RADIUS), t -> t != owner && t.isAlive()
                            && !(t instanceof Mob m && m.getType() == owner.getType()))) {
                    if (living.distanceToSqr(Vec3.atCenterOf(job.center)) > r2) continue;
                    switch (job.style) {
                        case FIREWORK -> {                       // 石流龙：打上花火 · 追踪烟花
                            living.hurt(living.damageSources().magic(), (float) DAMAGE);
                            if (living.level() instanceof ServerLevel sl)
                                Network.send(sl, new Network.Effect(character.ryu.RyuFx.FIREWORK_BURST,
                                    living.getId(), living.position(), Vec3.ZERO, 2f, 20, 0));
                        }
                        case PACHINKO -> {                       // 秤金次：赌场 · 双倍伤害
                            living.hurt(living.damageSources().magic(), (float) DAMAGE * 2);
                        }
                        case LAVA -> {                           // 漏壶：盖棺铁围山 · 点燃
                            living.hurt(living.damageSources().magic(), (float) DAMAGE);
                            living.setSecondsOnFire(2);
                        }
                        case ICE -> {                            // 里梅：冰冻减速
                            living.hurt(living.damageSources().magic(), (float) DAMAGE);
                            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                        }
                        case BLOOD -> {                          // 胀相：赤血 · 流血
                            living.hurt(living.damageSources().magic(), (float) DAMAGE);
                            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.WITHER, 40, 0));
                        }
                        default -> living.hurt(living.damageSources().magic(), (float) DAMAGE);
                    }
                }
            }
        }
    }

    @SubscribeEvent public static void stop(ServerStoppingEvent e) {
        for (var entry : new ArrayList<>(JOBS.entrySet()))
            close(entry.getValue().level.getEntity(entry.getKey()) instanceof Mob mob ? mob : null, "关服");
    }

    private NpcDomain() {}
}
