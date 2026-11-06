package character.uraume;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 里梅·领域展开「霜天宫殿」（r295，按 Cursed Fate 封闭式领域文档重做）：
 *
 * <p>固定锚点原地清场 → 分批施工双层球壳（外壳浮冰、内壁领域方块）→ 完整记录原方块 →
 * 分批还原；接 {@link cn.blockforge.generated.nnnnr1201forge2.DomainClash} 对拼。
 * 不再使用"贴图大平面天空"，天空由领域方块自身贴图（image=8 霜月雪云）表现。
 */
public final class UraumePalaceDomain {
    public static final int RADIUS = 26, TICKS = 600;
    private static final int BUILD_BATCH = 3500, RESTORE_BATCH = 4000;

    private static final class Job {
        final UUID owner; final ServerLevel level; final BlockPos center; final long end;
        final Map<BlockPos, BlockState> old; final List<BlockPos> build; int cursor;
        final List<BlockPos> restore; int restoreCursor; boolean built, opened;
        Job(UUID owner, ServerLevel level, BlockPos center, long end, Map<BlockPos, BlockState> old,
            List<BlockPos> build, int cursor, List<BlockPos> restore, int restoreCursor,
            boolean built, boolean opened) {
            this.owner = owner; this.level = level; this.center = center; this.end = end;
            this.old = old; this.build = build; this.cursor = cursor;
            this.restore = restore; this.restoreCursor = restoreCursor;
            this.built = built; this.opened = opened;
        }
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    public static boolean active(ServerPlayer p) { return JOBS.containsKey(p.getUUID()); }

    public static boolean inDomain(net.minecraft.world.entity.Entity e) {
        for (Job job : JOBS.values()) {
            if (e.level() != job.level) continue;
            if (e.position().distanceToSqr(Vec3.atCenterOf(job.center)) <= (double) RADIUS * RADIUS) return true;
        }
        return false;
    }

    public static void open(ServerPlayer p) {
        if (active(p) || !(p.level() instanceof ServerLevel level)) return;
        BlockPos center = p.blockPosition();
        // 施工清单：先地板（由内向外）→ 后双层球壳（由内向外）+ 内部清场 + 地下填充
        List<BlockPos> plan = new ArrayList<>();
        int r = RADIUS;
        for (int dy = -1; dy >= -1; dy--)
            for (int h = 0; h <= r; h++)
                for (int dx = -h; dx <= h; dx++) for (int dz = -h; dz <= h; dz++)
                    if (dx * dx + dz * dz == h * h) plan.add(center.offset(dx, dy, dz));
        for (int d = 0; d <= r; d++)
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz != d * d) continue;
                    plan.add(center.offset(dx, dy, dz));
                }
            }
        JOBS.put(p.getUUID(), new Job(p.getUUID(), level, center, level.getGameTime() + TICKS,
            new HashMap<>(), plan, 0, new ArrayList<>(), 0, false, false));
        level.playSound(null, center, net.minecraft.sounds.SoundEvents.GLASS_PLACE,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.6f, .5f);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("领域展开 · 霜天宫殿"), true);
    }

    public static void close(ServerPlayer p, String why) {
        Job job = JOBS.get(p.getUUID());
        if (job == null || !job.restore.isEmpty()) return;  // 已在还原中
        job.restore.addAll(job.old.keySet());
        job.old.clear();
        JOBS.put(p.getUUID(), new Job(p.getUUID(), job.level, job.center, job.end, job.old, job.build,
            job.cursor, job.restore, 0, job.built, job.opened));
        if (!why.isEmpty()) p.displayClientMessage(net.minecraft.network.chat.Component.literal("霜天宫殿 · 解除（" + why + "）"), true);
    }

    public static void forceClose(ServerPlayer p) {
        Job job = JOBS.remove(p.getUUID());
        if (job == null) return;
        job.old.forEach((pos, state) -> job.level.setBlock(pos, state, 2));
    }

    private static BlockState palace() {
        return cn.blockforge.generated.nnnnr1201forge2.Content.DOMAIN_SCREEN.get().defaultBlockState()
            .setValue(cn.blockforge.generated.nnnnr1201forge2.DomainScreenBlock.IMAGE, 8);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || JOBS.isEmpty()) return;
        for (var it = JOBS.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Job job = entry.getValue();
            ServerLevel level = job.level;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entry.getKey());

            // 还原阶段
            if (!job.restore.isEmpty()) {
                int done = 0;
                while (job.restoreCursor < job.restore.size() && done < RESTORE_BATCH) {
                    BlockPos pos = job.restore.get(job.restoreCursor++);
                    BlockState state = job.old.get(pos);
                    if (state != null) level.setBlock(pos, state, 2);
                    done++;
                }
                if (job.restoreCursor >= job.restore.size()) {
                    it.remove();
                    if (owner != null)
                        owner.displayClientMessage(net.minecraft.network.chat.Component.literal("霜天宫殿 · 已还原"), true);
                }
                continue;
            }

            if (owner == null || !owner.isAlive() || owner.level() != level) {
                close(owner == null ? null : owner, "");
                if (owner == null) it.remove();
                continue;
            }
            if (level.getGameTime() >= job.end && job.built) { close(owner, "时间到"); continue; }

            // 施工阶段：固定锚点，记录原方块后再写入
            int done = 0;
            boolean half = job.cursor * 2 >= job.build.size();
            while (job.cursor < job.build.size() && done < BUILD_BATCH) {
                BlockPos pos = job.build.get(job.cursor++);
                BlockState old = level.getBlockState(pos);
                if (old.getBlock() == Blocks.BEDROCK) continue;
                if (old.hasBlockEntity()) continue;
                job.old.putIfAbsent(pos, old);
                int dx = pos.getX() - job.center.getX(), dy = pos.getY() - job.center.getY(),
                    dz = pos.getZ() - job.center.getZ();
                double h2 = (double) dx * dx + dz * dz, d2 = h2 + (double) dy * dy;
                BlockState put;
                if (dy == -1 && h2 <= (double) RADIUS * RADIUS) put = palace();                       // 地板
                else if (dy < -1) put = Blocks.PACKED_ICE.defaultBlockState();                        // 地下填充
                else if (d2 > (double) RADIUS * RADIUS) continue;                                     // 外部不动
                else if (d2 > (double) (RADIUS - 1) * (RADIUS - 1)) put = Blocks.PACKED_ICE.defaultBlockState(); // 外壳浮冰
                else if (d2 > (double) (RADIUS - 2) * (RADIUS - 2)) put = palace();                   // 内壁领域方块
                else put = Blocks.AIR.defaultBlockState();                                            // 内部清空
                level.setBlock(pos, put, 2);
                done++;
            }
            if (job.cursor >= job.build.size() && !job.built) {
                job.built = true;
                // 施工完成后登记领域对拼
                cn.blockforge.generated.nnnnr1201forge2.DomainClash.opened(owner,
                    Vec3.atCenterOf(job.center), RADIUS);
                job.opened = true;
            }

            // 界内效果：禁飞、禁跳、冻结、霜寒满层
            if (level.getGameTime() % 20 == 0) {
                for (ServerPlayer p : level.players()) {
                    if (p == owner) continue;
                    if (p.position().distanceToSqr(Vec3.atCenterOf(job.center)) > (double) RADIUS * RADIUS) continue;
                    if (p.getAbilities().flying) { p.getAbilities().flying = false; p.onUpdateAbilities(); }
                    if (p.getDeltaMovement().y > 0.42) {
                        var v = p.getDeltaMovement();
                        p.setDeltaMovement(v.x, 0.42, v.z);
                        p.hurtMarked = true;
                    }
                    p.setTicksFrozen(Math.max(p.getTicksFrozen(), 100));
                    p.getPersistentData().putInt("FrostStacks",
                        cn.blockforge.generated.nnnnr1201forge2.UraumeIce.FROST_MAX);
                }
            }
        }
    }

    private UraumePalaceDomain() { }
}
