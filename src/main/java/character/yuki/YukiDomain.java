package character.yuki;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 领域展开 · 星轨坠亡（九十九由基）。
 * <p>结构：内壁纯黑混凝土，外壳套一层「星轨壁」（用星空贴图）。
 * <p>效果：领域内敌对生物无法移动、持续虚弱；<b>生命上限低于施术者的生物直接被压扁致死</b>。
 * <p>地形按「先算清单、再按刻消费」的方式施工，结束时逆序还原。
 */
public final class YukiDomain {
    public static final int SKILL = 2004;

    private static final int RADIUS = 20;
    /** 每刻最多改动的方块数。 */
    private static final int BATCH = 900;
    /** 领域最长维持时间（5 分钟）。 */
    private static final int MAX_TICKS = 6000;
    /** 每秒扣的咒力。 */
    private static final int DRAIN_PER_SECOND = 30;
    /** 刷怪间隔（刻）。 */
    private static final int SPAWN_INTERVAL = 20;
    /** 领域内最多同时存在的刷怪数。 */
    private static final int SPAWN_CAP = 12;

    private record Rec(BlockPos pos, BlockState state) {}

    private static final class Job {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final List<Rec> saved = new ArrayList<>();
        final List<Rec> plan = new ArrayList<>();
        int cursor;
        boolean built;
        long start;

        Job(UUID owner, ServerLevel level, BlockPos center) {
            this.owner = owner;
            this.level = level;
            this.center = center;
        }
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    public static boolean active(ServerPlayer p) { return p != null && JOBS.containsKey(p.getUUID()); }

    public static void open(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        if (active(p)) { close(p, "已收起"); return; }
        Job job = new Job(p.getUUID(), level, p.blockPosition());
        buildPlan(job);
        job.start = level.getGameTime();
        JOBS.put(p.getUUID(), job);
        p.displayClientMessage(Component.literal("领域展开 · 星轨坠亡"), false);
        level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.5f);
    }

    public static void close(ServerPlayer p, String reason) {
        Job job = JOBS.remove(p.getUUID());
        if (job == null) return;
        restore(job);
        p.displayClientMessage(Component.literal("领域解除：" + reason), false);
    }

    /** 内壁黑混凝土、外壳星轨壁，内部清空。 */
    private static void buildPlan(Job job) {
        BlockPos c = job.center;
        int r = RADIUS;
        int outer2 = r * r;
        int inner2 = (r - 1) * (r - 1);
        int black2 = (r - 2) * (r - 2);
        var star = cn.blockforge.generated.nnnnr1201forge2.Content.YUKI_STAR_SHELL.get().defaultBlockState();
        var black = Blocks.BLACK_CONCRETE.defaultBlockState();
        var light = Blocks.LIGHT.defaultBlockState()
            .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15);
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    int d2 = x * x + y * y + z * z;
                    if (d2 > outer2) continue;
                    BlockPos pos = c.offset(x, y, z);
                    if (d2 > inner2) {
                        job.plan.add(new Rec(pos, star));          // 外壳：星空
                    } else if (d2 > black2) {
                        job.plan.add(new Rec(pos, black));          // 内壁：纯黑混凝土
                    } else if (!job.level.getBlockState(pos).is(Blocks.LIGHT)) {
                        job.plan.add(new Rec(pos, light));          // 内部：光源（不可见，只照亮）
                    }
                }
            }
        }
    }

    /** 服务端每刻推进（注册到 Forge 事件总线）。 */
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        tick(e.getServer());
    }

    private static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        for (Iterator<Job> it = JOBS.values().iterator(); it.hasNext(); ) {
            Job job = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(job.owner);
            if (p == null || !p.isAlive() || p.level() != job.level) {
                restore(job);
                it.remove();
                continue;
            }
            long age = job.level.getGameTime() - job.start;

            if (!job.built) {
                int budget = BATCH;
                while (job.cursor < job.plan.size() && budget-- > 0) {
                    Rec e = job.plan.get(job.cursor++);
                    BlockState now = job.level.getBlockState(e.pos());
                    if (now == e.state()) continue;
                    job.saved.add(new Rec(e.pos(), now));
                    job.level.setBlock(e.pos(), e.state(), e.state().is(Blocks.LIGHT) ? 2 : 3);
                }
                if (job.cursor >= job.plan.size()) {
                    job.built = true;
                    p.displayClientMessage(Component.literal("星轨闭环 · 领域成型"), false);
                }
                continue;
            }

            // 维持：5 分钟 + 每秒 30 咒力
            if (age > MAX_TICKS) { close(p, "星轨散尽"); it.remove(); continue; }
            if (age > 0 && age % 20 == 0
                && !cn.blockforge.generated.nnnnr1201forge2.Curse.spend(p, DRAIN_PER_SECOND)) {
                close(p, "咒力耗尽"); it.remove(); continue;
            }

            suppress(job, p);
            if (age % SPAWN_INTERVAL == 0) spawn(job);
            if (age % 10 == 0) {
                job.level.sendParticles(ParticleTypes.END_ROD,
                    job.center.getX() + 0.5, job.center.getY() + 2, job.center.getZ() + 0.5,
                    6, 1.5, 1.5, 1.5, 0.01);
            }
        }
    }

    /** 领域压制：不能移动 + 虚弱；生命上限低于施术者的直接压扁。 */
    private static void suppress(Job job, ServerPlayer owner) {
        double hp = owner.getHealth();
        for (LivingEntity e : inside(job)) {
            if (!(e instanceof Enemy)) continue;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6, false, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 2, false, false, false));
            e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
            e.hurtMarked = true;
            if (e.getMaxHealth() < hp) {
                e.hurt(owner.damageSources().magic(), e.getMaxHealth() + 100f);
                job.level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + 0.8, e.getZ(),
                    1, 0, 0, 0, 0);
            }
        }
    }

    private static List<LivingEntity> inside(Job job) {
        return job.level.getEntitiesOfClass(LivingEntity.class,
            new AABB(job.center).inflate(RADIUS - 1), e -> e.isAlive() && !e.isSpectator());
    }

    /** 刷怪：在领域内随机位置生成低级敌对生物（会被压制/压扁）。 */
    private static void spawn(Job job) {
        long count = inside(job).stream().filter(e -> e instanceof Enemy).count();
        if (count >= SPAWN_CAP) return;
        for (int i = 0; i < 2; i++) {
            double a = job.level.random.nextDouble() * Math.PI * 2;
            double rr = job.level.random.nextDouble() * (RADIUS - 4);
            BlockPos at = job.center.offset((int) (Math.cos(a) * rr), 0, (int) (Math.sin(a) * rr));
            var type = job.level.random.nextBoolean()
                ? net.minecraft.world.entity.EntityType.ZOMBIE : net.minecraft.world.entity.EntityType.SKELETON;
            var mob = type.create(job.level);
            if (mob == null) continue;
            mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            job.level.addFreshEntity(mob);
        }
    }

    private static void restore(Job job) {
        for (int i = job.saved.size() - 1; i >= 0; i--) {
            Rec r = job.saved.get(i);
            job.level.setBlock(r.pos(), r.state(), 3);
        }
        job.saved.clear();
        job.plan.clear();
    }

    private YukiDomain() {}
}
