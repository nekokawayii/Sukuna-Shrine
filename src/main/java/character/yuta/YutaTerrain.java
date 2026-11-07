package character.yuta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * 「真赝相爱」的封闭领域地形（照《Cursed Fate 封闭式领域制作方法》实现）。
 * <p><b>施工方式：开局一次性算出完整清单，再按刻消费。</b>不使用"游标逐环/逐层"的方式，
 * 因此不会出现预算耗尽时跳环、缺层、残留的问题。
 * <p>顺序：地板（由内向外逐环）→ 贴 nbt 地面图案 → 立坟 → 自下而上合黑混凝土球壳并清空内部。
 * <p>每个被改动的方块都记录原状态，结束/死亡/掉线时逆序还原。
 */
public final class YutaTerrain {
    /** 每刻最多改动的方块数（约 1200 → 全领域 4 万块约 33 刻 ≈ 1.7 秒）。 */
    private static final int BATCH = 4000;

    private record Rec(BlockPos pos, BlockState state) {}

    private static final class Job {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final int radius;
        final character.domain.DomainTerrain.Handle terrain;
        /** 施工清单（state 为目标状态）。 */
        final List<Rec> plan = new ArrayList<>();
        int floorEnd;
        int cursor;
        boolean extrasDone;
        boolean finished;
        long lastGraveRefresh;

        Job(UUID owner, ServerLevel level, BlockPos center, int radius) {
            this.owner = owner;
            this.terrain=character.domain.DomainTerrain.begin(level,owner);
            this.level = level;
            this.center = center;
            this.radius = radius;
        }
    }

    private static final Map<UUID, Job> JOBS = new HashMap<>();

    public static boolean active(ServerPlayer p) { return p != null && JOBS.containsKey(p.getUUID()); }

    public static void begin(ServerPlayer p, int radius) {
        if (!(p.level() instanceof ServerLevel level)) return;
        Job old = JOBS.remove(p.getUUID());
        if (old != null) restore(old);
        Job job = new Job(p.getUUID(), level, p.blockPosition(), radius);
        buildPlan(job);
        JOBS.put(p.getUUID(), job);
    }

    /** 一次性算出完整清单：先地板，再把球壳与清空排在其后。 */
    private static void buildPlan(Job job) {
        int R = job.radius;
        BlockPos c = job.center;

        // ① 地板：由内向外逐环，纯内存计算
        for (int r = 0; r <= R; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    int d2 = x * x + z * z;
                    if (d2 > r * r) continue;
                    if (r > 0 && d2 <= (r - 1) * (r - 1)) continue;
                    job.plan.add(new Rec(c.offset(x, -1, z), Blocks.STONE.defaultBlockState()));
                }
            }
        }
        job.floorEnd = job.plan.size();

        // ② 球壳（黑混凝土）+ 内侧光源层 + 内部清空
        // 用真三维距离判定 d²=x²+y²+z²，壳是一层连续球面：
        //   (R-1)² < d² ≤ R²        → 黑混凝土外壳（含正上方顶点，不会留洞）
        //   (R-2)² < d² ≤ (R-1)²    → 15 级光源层（照亮内部）
        //   d² ≤ (R-2)²             → 清空（只收非空气）
        var light = Blocks.LIGHT.defaultBlockState()
            .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15);
        int outer2 = R * R;
        int shell2 = (R - 1) * (R - 1);
        int glow2 = Math.max(0, R - 2) * Math.max(0, R - 2);
        for (int y = 0; y <= R; y++) {
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    int d2 = x * x + y * y + z * z;
                    if (d2 > outer2) continue;
                    BlockPos pos = c.offset(x, y, z);
                    if (d2 > shell2) {
                        job.plan.add(new Rec(pos, Blocks.BLACK_CONCRETE.defaultBlockState()));
                    } else {
                        job.plan.add(new Rec(pos, light));
                    }
                }
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        for (Iterator<Job> it = JOBS.values().iterator(); it.hasNext(); ) {
            Job job = it.next();
            ServerPlayer p = cn.blockforge.generated.nnnnr1201forge2.NpcBridge.find(server,job.owner);
            if (p == null || !p.isAlive() || p.level() != job.level) {
                restore(job);
                it.remove();
                continue;
            }
            if (job.finished) {
                if (job.level.getGameTime() - job.lastGraveRefresh >= 40) {
                    job.lastGraveRefresh = job.level.getGameTime();
                    placeGraves(job);
                }
                continue;
            }

            // 地板铺完只提示，图案与坟等「内部填充完成」后再放，否则会被光源覆盖
            if (!job.extrasDone && job.cursor >= job.floorEnd) {
                job.extrasDone = true;
                p.displayClientMessage(Component.literal("地板完成 · 正在填充内部光源"), false);
            }

            if (job.level.getGameTime() - job.lastGraveRefresh >= 40) {
                job.lastGraveRefresh = job.level.getGameTime();
                placeGraves(job);
            }
            int budget = BATCH;
            while (job.cursor < job.plan.size() && budget-- > 0) {
                apply(job, job.plan.get(job.cursor++));
            }
            if (job.cursor >= job.plan.size()) {
                job.finished = true;
                placePattern(job);   // 填充完再贴图案，避免被光源方块覆盖
                placeGraves(job);    // 同理：坟也放到最后
                p.displayClientMessage(Component.literal("领域成型 · 真赝相爱"), false);
            }
        }
    }

    /** 贴 resources/data/jujutsu_kaisen_cursed_paths/structures/zhenyanxiangai.nbt；只把真正变化的方块写入记录。 */
    private static void placePattern(Job job) {
        var manager = job.level.getServer().getStructureManager();
        var template = manager.get(new ResourceLocation("jujutsu_kaisen_cursed_paths", "zhenyanxiangai"));
        if (template.isEmpty()) return;
        StructureTemplate t = template.get();
        net.minecraft.core.Vec3i size = t.getSize();
        BlockPos origin = job.center.offset(-size.getX() / 2, 0, -size.getZ() / 2);

        // 保留用户 NBT 原格式；逐格读取原模板并交给同一个事务，不能绕过日志直接 placeInWorld。
        var tag=t.save(new net.minecraft.nbt.CompoundTag());
        var palette=tag.getList("palette",net.minecraft.nbt.Tag.TAG_COMPOUND);
        if(palette.isEmpty()){
            var palettes=tag.getList("palettes",net.minecraft.nbt.Tag.TAG_LIST);
            if(!palettes.isEmpty())palette=(net.minecraft.nbt.ListTag)palettes.get(0);
        }
        var blocks=tag.getList("blocks",net.minecraft.nbt.Tag.TAG_COMPOUND);
        var lookup=job.level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
        for(int i=0;i<blocks.size();i++){
            var block=blocks.getCompound(i);int index=block.getInt("state");
            if(index<0||index>=palette.size())continue;
            var xyz=block.getList("pos",net.minecraft.nbt.Tag.TAG_INT);if(xyz.size()!=3)continue;
            var state=net.minecraft.nbt.NbtUtils.readBlockState(lookup,palette.getCompound(index));
            if(state.hasBlockEntity())continue; // 容器与机器不覆盖，也不生成物品副本。
            apply(job,new Rec(origin.offset(xyz.getInt(0),xyz.getInt(1),xyz.getInt(2)),state));
        }
    }

    /** 固定的 3 个坟位（相对锚点）。 */
    private static final int[][] GRAVE_SPOTS = {{0, -13}, {12, 7}, {-12, 7}};

    /** 摆坟：最多 3 座、固定点位；只要还有已复制术式，拔走后会在 refresh 中重新长出。 */
    private static void placeGraves(Job job) {
        ServerPlayer owner = cn.blockforge.generated.nnnnr1201forge2.NpcBridge.find(job.level.getServer(),job.owner);
        if (owner == null || YutaCopies.copies(owner).isEmpty()) return;
        BlockState grave = cn.blockforge.generated.nnnnr1201forge2.Content.YUTA_GRAVE.get().defaultBlockState();
        for (int[] spot : GRAVE_SPOTS) {
            BlockPos pos = job.center.offset(spot[0], 0, spot[1]);
            if (!job.level.getBlockState(pos).is(grave.getBlock())) apply(job, new Rec(pos, grave));
        }
    }

    private static void apply(Job job, Rec e) {
        job.terrain.place(e.pos(),e.state());
    }

    public static void restore(ServerPlayer p) {
        if (p == null) return;
        Job job = JOBS.remove(p.getUUID());
        if (job != null) restore(job);
    }

    private static void restore(Job job) {
        job.terrain.close();
        job.plan.clear();
    }

    private YutaTerrain() {}
}
