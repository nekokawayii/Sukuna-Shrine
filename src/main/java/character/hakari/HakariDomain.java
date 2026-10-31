package character.hakari;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 领域展开 · 坐杀博打（秤金次）。
 * <p>R=30 的赌场球壳：黑混凝土 + 金色地板，内部清空；每 2 秒自动开奖一次。
 * <p>施工与还原沿用「先算清单、每刻消费、结束逆序还原」的做法。
 */
public final class HakariDomain {
    public static final int SKILL = 3002;

    private static final int RADIUS = 30;
    private static final int BATCH = 2500;
    private static final int MAX_TICKS = 6000;
    private static final int DRAIN_PER_SECOND = 40;
    /** 自动开奖间隔（刻）。 */
    private static final int SPIN_INTERVAL = 40;

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
        p.displayClientMessage(Component.literal("领域展开 · 坐杀博打"), false);
        level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(),
            net.minecraft.sounds.SoundSource.PLAYERS, 1.4f, 0.8f);
    }

    public static void close(ServerPlayer p, String reason) {
        Job job = JOBS.remove(p.getUUID());
        if (job == null) return;
        restore(job);
        p.displayClientMessage(Component.literal("领域解除：" + reason), false);
    }

    private static void buildPlan(Job job) {
        BlockPos c = job.center;
        int r = RADIUS;
        var floor = Blocks.GOLD_BLOCK.defaultBlockState();
        var shell = Blocks.BLACK_CONCRETE.defaultBlockState();
        var light = Blocks.LIGHT.defaultBlockState()
            .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15);

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > r * r) continue;
                job.plan.add(new Rec(c.offset(x, -1, z), floor));
            }
        }
        int outer2 = r * r, inner2 = (r - 1) * (r - 1);
        for (int y = 0; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    int d2 = x * x + y * y + z * z;
                    if (d2 > outer2) continue;
                    BlockPos pos = c.offset(x, y, z);
                    if (d2 > inner2) {
                        job.plan.add(new Rec(pos, shell));
                    } else if (!job.level.getBlockState(pos).is(Blocks.LIGHT)) {
                        job.plan.add(new Rec(pos, light));
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
                    job.level.setBlock(e.pos(), e.state(), e.state().is(Blocks.LIGHT) ? 2 : 3);
                }
                if (job.cursor >= job.plan.size()) {
                    job.built = true;
                    p.displayClientMessage(Component.literal("赌场就绪 · 开始开奖"), false);
                }
                continue;
            }

            if (age > MAX_TICKS) { close(p, "赌场关闭"); it.remove(); continue; }
            if (age > 0 && age % 20 == 0
                && !cn.blockforge.generated.nnnnr1201forge2.Curse.spend(p, DRAIN_PER_SECOND)) {
                close(p, "咒力耗尽"); it.remove(); continue;
            }
            if (age % SPIN_INTERVAL == 0) {
                HakariCombat.trySpin(p, false);
            }
            if (age % 10 == 0) {
                job.level.sendParticles(ParticleTypes.END_ROD,
                    job.center.getX() + 0.5, job.center.getY() + 3, job.center.getZ() + 0.5,
                    4, 1.5, 1.0, 1.5, 0.01);
            }
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

    private HakariDomain() {}
}
