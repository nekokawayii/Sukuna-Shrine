package character.yuta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * 「真赝相爱」的封闭领域地形（照《Cursed Fate 封闭式领域制作方法》实现）。
 * <p>流程：固定锚点 → 记录原方块 → 分批施工（先逐环铺地板，再自下而上合球壳并清空内部）→ 结束时逆序还原。
 * <p>只记录**非空气**方块，大幅降低内存与还原耗时；每刻有写入预算，避免一次写完造成卡顿。
 * <p>本类只管地形；领域状态、音效、耗蓝在 {@link YutaDomain}。
 */
public final class YutaTerrain {
    /** 每刻最多写入的方块数。 */
    private static final int BATCH = 2500;
    /** 地板每刻铺几环（越大地板扩张越快）。 */
    private static final int FLOOR_RINGS_PER_TICK = 2;

    private record Rec(BlockPos pos, BlockState state) {}

    private static final class Job {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final int radius;
        final List<Rec> saved = new ArrayList<>();
        int floorR = 0;
        boolean floorDone;
        int y = 1;

        Job(UUID owner, ServerLevel level, BlockPos center, int radius) {
            this.owner = owner;
            this.level = level;
            this.center = center;
            this.radius = radius;
        }
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    /** 以玩家为中心展开：半径、锚点固定，之后不再跟随玩家移动。 */
    public static void begin(ServerPlayer p, int radius) {
        if (!(p.level() instanceof ServerLevel level)) return;
        Job old = JOBS.remove(p.getUUID());
        if (old != null) restore(old);
        BlockPos center = p.blockPosition();
        JOBS.put(p.getUUID(), new Job(p.getUUID(), level, center, radius));
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("领域展开：清场记录中…"), false);
    }

    public static boolean active(ServerPlayer p) { return p != null && JOBS.containsKey(p.getUUID()); }

    /** 结束并还原该玩家领地形的所有方块（幂等）。 */
    public static void restore(ServerPlayer p) {
        if (p == null) return;
        Job job = JOBS.remove(p.getUUID());
        if (job != null) restore(job);
    }

    private static void restore(Job job) {
        // 逆序回填：后写的先还原，避免同一格被多次覆盖时取错值。
        for (int i = job.saved.size() - 1; i >= 0; i--) {
            Rec r = job.saved.get(i);
            job.level.setBlock(r.pos(), r.state(), 2);
        }
        job.saved.clear();
    }

    /** 每个服务器刻调用一次，按预算推进施工。 */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        for (Iterator<Job> it = JOBS.values().iterator(); it.hasNext(); ) {
            Job job = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(job.owner);
            if (p == null || !p.isAlive()) {
                // 掉线/死亡：直接还原，避免残留半成品领域。
                restore(job);
                it.remove();
                continue;
            }
            if (job.level != p.level()) {
                restore(job);
                it.remove();
                continue;
            }
            if (!job.floorDone) buildFloor(job);
            else buildShell(job);
        }
    }

    /** 第一阶段：地板逐环铺开（先原石，铺满后贴 nbt 图案）。 */
    private static void buildFloor(Job job) {
        int budget = BATCH;
        for (int ring = 0; ring < FLOOR_RINGS_PER_TICK && job.floorR <= job.radius; ring++, job.floorR++) {
            int r = job.floorR;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z > r * r) continue;
                    // 只处理当前这一环，避免重复写内圈
                    if (r > 0 && x * x + z * z <= (r - 1) * (r - 1)) continue;
                    if (budget-- <= 0) return;
                    set(job, job.center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                }
            }
        }
        if (job.floorR > job.radius && !job.floorDone) {
            job.floorDone = true;
            placePattern(job);
            placeGraves(job);
        }
    }

    /** 把 resources/data/sukuna/structures/zhenyanxiangai.nbt 贴在地板中心。 */
    private static void placePattern(Job job) {
        var manager = job.level.getServer().getStructureManager();
        var template = manager.get(new ResourceLocation("sukuna", "zhenyanxiangai"));
        if (template.isEmpty()) return;
        StructureTemplate t = template.get();
        BlockPos origin = job.center.offset(-t.getSize().getX() / 2, 0, -t.getSize().getZ() / 2);
        var settings = new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true);
        t.placeInWorld(job.level, origin, origin, settings, job.level.random, 2);
    }

    /** 按「已复制术式数量」在领域里摆对应数量的坟头，单次领域不刷新。 */
    private static void placeGraves(Job job) {
        ServerPlayer owner = job.level.getServer().getPlayerList().getPlayer(job.owner);
        int count = owner == null ? 0 : YutaCopies.copies(owner).size();
        if (count <= 0) return;
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            int x = (int) Math.round(Math.cos(a) * 12);
            int z = (int) Math.round(Math.sin(a) * 12);
            set(job, job.center.offset(x, 0, z),
                cn.blockforge.generated.nnnnr1201forge2.Content.YUTA_GRAVE.get().defaultBlockState());
        }
        if (owner != null) owner.displayClientMessage(
            net.minecraft.network.chat.Component.literal("坟头已立：" + count + " 座（对应你已复制的术式）"), false);
    }

    /** 第二阶段：自下而上合球壳，并把内部清空（只处理非空气，减少记录量）。 */
    private static void buildShell(Job job) {
        if (job.y > job.radius) return;
        int y = job.y;
        int layerR = (int) Math.floor(Math.sqrt((double) job.radius * job.radius - (double) y * y));
            int budget = BATCH;
        for (int x = -layerR; x <= layerR; x++) {
            for (int z = -layerR; z <= layerR; z++) {
                int d2 = x * x + z * z;
                if (d2 > layerR * layerR) continue;
                // 最外环：黑混凝土球壳
                boolean shell = d2 > (layerR - 1) * (layerR - 1);
                if (shell) {
                    if (budget-- <= 0) return;
                    set(job, job.center.offset(x, y, z), Blocks.BLACK_CONCRETE.defaultBlockState());
                } else {
                    // 内部：只清非空气，且不做记录的话无法还原，所以同样走 set()
                    if (budget <= 0) return;
                    BlockPos pos = job.center.offset(x, y, z);
                    BlockState now = job.level.getBlockState(pos);
                    if (!now.isAir()) {
                        budget--;
                        set(job, pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        job.y++;
    }

    /** 记录一次原方块（只记第一次），然后写入新状态。 */
    private static void set(Job job, BlockPos pos, BlockState state) {
        BlockState now = job.level.getBlockState(pos);
        if (now == state) return;
        job.saved.add(new Rec(pos.immutable(), now));
        job.level.setBlock(pos, state, 2);
    }

    private YutaTerrain() {}
}
