package character.npc;

import cn.blockforge.generated.nnnnr1201forge2.Network;
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
    public static final int RADIUS = 26;
    public static final int TICKS = 400;          // 20 秒
    private static final int BATCH = 2000;
    private static final double DAMAGE = 6;

    private static final class Job {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final long end;
        final List<long[]> saved = new ArrayList<>();     // 编码：pos | (stateId << 64) 分两段存
        final List<BlockState> states = new ArrayList<>();
        final List<BlockPos> plan = new ArrayList<>();
        int cursor;

        Job(UUID owner, ServerLevel level, BlockPos center, long end) {
            this.owner = owner; this.level = level; this.center = center; this.end = end;
        }
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    public static boolean active(Mob mob) { return mob != null && JOBS.containsKey(mob.getUUID()); }

    public static void open(Mob mob, double radiusOverride) {
        if (!(mob.level() instanceof ServerLevel level) || active(mob)) return;
        BlockPos center = mob.blockPosition();
        Job job = new Job(mob.getUUID(), level, center, level.getGameTime() + TICKS);
        int r = (int) Math.round(radiusOverride);
        // 施工计划：外壳球壳 + 地板（内部先清空气，保证是空地）
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    double d2 = (double) dx * dx + dy * dy + dz * dz;
                    boolean shell = d2 <= (double) r * r && d2 > (double) (r - 1) * (r - 1);
                    boolean floor = dy == -1 && d2 <= (double) r * r;
                    if (shell || floor) job.plan.add(center.offset(dx, dy, dz));
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
        for (var it = JOBS.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Job job = entry.getValue();
            Mob owner = job.level.getEntity(entry.getKey()) instanceof Mob mob ? mob : null;
            if (owner == null || !owner.isAlive() || job.level.getGameTime() >= job.end) {
                close(owner, "结束");
                it.remove();
                continue;
            }
            // 逐批施工
            var shell = Blocks.BLACK_CONCRETE.defaultBlockState();
            int done = 0;
            while (job.cursor < job.plan.size() && done < BATCH) {
                BlockPos pos = job.plan.get(job.cursor++);
                BlockState old = job.level.getBlockState(pos);
                if (old.getBlock() == Blocks.BEDROCK) continue;
                job.saved.add(new long[]{pos.asLong()});
                job.states.add(old);
                job.level.setBlock(pos, shell, 2);
                done++;
            }
            // 领域内持续伤害（每 10 刻）
            if (job.level.getGameTime() % 10 == 0) {
                double r2 = (double) RADIUS * RADIUS;
                for (LivingEntity living : job.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(job.center).inflate(RADIUS), t -> t != owner && t.isAlive()
                            && !(t instanceof Mob m && m.getType() == owner.getType()))) {
                    if (living.distanceToSqr(Vec3.atCenterOf(job.center)) <= r2)
                        net.minecraftforge.common.ForgeHooks.onLivingHurt(living,
                            living.damageSources().magic(), (float) DAMAGE);
                }
            }
        }
    }

    @SubscribeEvent public static void stop(ServerStoppingEvent e) {
        for (var it = JOBS.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            close(entry.getValue().level.getEntity(entry.getKey()) instanceof Mob mob ? mob : null, "关服");
            it.remove();
        }
    }

    private NpcDomain() {}
}
