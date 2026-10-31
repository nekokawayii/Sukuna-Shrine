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
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 领域展开 · 星轨坠亡（九十九由基）。
 * <p>与乙骨「真赝相爱」同尺寸（R=50）：外壳与内壁都是「星轨壁」，并额外铺一层地板。
 * <p>效果：领域内敌对生物无法移动、持续虚弱；<b>生命上限低于施术者的生物会被压扁后死亡</b>
 * （复用真人无为转变的 303 形态缩放通道：先压扁再处决）。
 */
public final class YukiDomain {
    public static final int SKILL = 2004;

    /** 与乙骨领域同尺寸。 */
    private static final int RADIUS = 50;
    /** 每刻最多改动的方块数。 */
    private static final int BATCH = 4000;
    private static final int MAX_TICKS = 6000;
    private static final int DRAIN_PER_SECOND = 30;
    private static final int SPAWN_INTERVAL = 20;
    private static final int SPAWN_CAP = 12;

    private record Rec(BlockPos pos, BlockState state) {}

    private static final class Job {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final List<Rec> saved = new ArrayList<>();
        long extra;
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

    /** 领域对拼获胜：延长领域存在时间。 */
    public static void extend(ServerPlayer p, int ticks) {
        Job job = JOBS.get(p.getUUID());
        if (job != null) job.extra += ticks;
    }

    public static void close(ServerPlayer p, String reason) {
        Job job = JOBS.remove(p.getUUID());
        if (job == null) return;
        restore(job);
        p.displayClientMessage(Component.literal("领域解除：" + reason), false);
    }

    /** 地板（y=-1 圆盘）+ 外壳 + 内壁，全部使用星轨壁；内部清空。 */
    private static void buildPlan(Job job) {
        BlockPos c = job.center;
        int r = RADIUS;
        var star = cn.blockforge.generated.nnnnr1201forge2.Content.YUKI_STAR_SHELL.get().defaultBlockState();

        // ① 地板：r² 圆盘
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > r * r) continue;
                job.plan.add(new Rec(c.offset(x, -1, z), star));
            }
        }

        // ② 外壳 + 内壁两层星轨壁；内部只清非空气（星轨壁自带 15 级光，不必再铺光源）
        int outer2 = r * r;
        int inner2 = (r - 1) * (r - 1);
        for (int y = 0; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    int d2 = x * x + y * y + z * z;
                    if (d2 > outer2) continue;
                    BlockPos pos = c.offset(x, y, z);
                    if (d2 > inner2) {
                        job.plan.add(new Rec(pos, star));
                    } else if (!job.level.getBlockState(pos).isAir()) {
                        job.plan.add(new Rec(pos, Blocks.AIR.defaultBlockState()));
                    }
                }
            }
        }
    }

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
                    job.level.setBlock(e.pos(), e.state(), 3);
                }
                if (job.cursor >= job.plan.size()) {
                    job.built = true;
                    p.displayClientMessage(Component.literal("星轨闭环 · 领域成型"), false);
                }
                continue;
            }

            if (age > MAX_TICKS + job.extra) { close(p, "星轨散尽"); it.remove(); continue; }
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

    /** 领域压制：不能移动 + 虚弱；生命上限低于施术者的先被压扁、随后死亡。 */
    private static void suppress(Job job, ServerPlayer owner) {
        double hp = owner.getHealth();
        for (LivingEntity e : inside(job)) {
            if (!(e instanceof Enemy)) continue;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6, false, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 2, false, false, false));
            e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
            e.hurtMarked = true;
            if (e.getMaxHealth() < hp && !e.isDeadOrDying() && !e.getPersistentData().getBoolean("YukiCrushed")) {
                crush(job, owner, e);
            }
        }
    }

    /**
     * 压扁后处决：先用真人同款的 303 通道把模型压成薄片（宽 ×1.9、高 ×0.12），
     * 6 刻后再结算致死伤害，视觉上就是「被压扁」。
     */
    private static void crush(Job job, ServerPlayer owner, LivingEntity e) {
        float wide = 1.9f, flat = 0.12f;
        e.getPersistentData().putBoolean("YukiCrushed", true);
        cn.blockforge.generated.nnnnr1201forge2.Network.send(job.level,
            new cn.blockforge.generated.nnnnr1201forge2.Network.Effect(303, e.getId(), e.position(),
                Vec3.ZERO, wide, (int) Math.max(1, flat * 1000), 0));
        job.level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 0.2, e.getZ(), 12, 0.4, 0.1, 0.4, 0.05);
        job.level.playSound(null, e.blockPosition(), net.minecraft.sounds.SoundEvents.SLIME_SQUISH,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 0.5f);
        owner.server.tell(new TickTask(6, () -> {
            if (e.isAlive()) {
                e.hurt(owner.damageSources().magic(), e.getMaxHealth() + 200f);
                job.level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + 0.2, e.getZ(), 1, 0, 0, 0, 0);
            }
        }));
    }

    private static List<LivingEntity> inside(Job job) {
        return job.level.getEntitiesOfClass(LivingEntity.class,
            new AABB(job.center).inflate(RADIUS - 1), e -> e.isAlive() && !e.isSpectator());
    }

    private static void spawn(Job job) {
        long count = inside(job).stream().filter(e -> e instanceof Enemy).count();
        if (count >= SPAWN_CAP) return;
        for (int i = 0; i < 2; i++) {
            double a = job.level.random.nextDouble() * Math.PI * 2;
            double rr = job.level.random.nextDouble() * (RADIUS - 6);
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
