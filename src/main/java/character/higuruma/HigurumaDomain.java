package character.higuruma;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * 诛伏赐死领域：几何由代码逐块生成（布局烘焙在 {@link HigurumaLayout}，取自用户调好的 48³ 法庭结构），
 * 不加载 NBT 结构。固定锚点、先记录原始方块、按距离逐步施工；再按收回/到期/关服整批还原。
 */
public final class HigurumaDomain {
    private static final int PER_TICK = 2500, LIFE = 600;
    private record Cell(int x, int y, int z, BlockState state) {}

    private static List<Cell> layout;

    private static List<Cell> layout() {
        if (layout != null) return layout;
        try {
            byte[] raw = java.util.Base64.getDecoder().decode(HigurumaLayout.DATA);
            var inflater = new java.util.zip.Inflater();
            inflater.setInput(raw);
            byte[] out = new byte[HigurumaLayout.SIZE_X * HigurumaLayout.SIZE_Y * HigurumaLayout.SIZE_Z * 4];
            int n = inflater.inflate(out);
            List<Cell> cells = new ArrayList<>();
            for (int i = 0; i + 3 < n; i += 4) {
                int x = out[i] & 0xFF, y = out[i + 1] & 0xFF, z = out[i + 2] & 0xFF, p = out[i + 3] & 0xFF;
                if (p >= HigurumaLayout.PALETTE.length) continue;
                Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(HigurumaLayout.PALETTE[p]));
                if (b == Blocks.AIR) continue;
                cells.add(new Cell(x, y, z, b.defaultBlockState()));
            }
            layout = cells;
            return layout;
        } catch (Exception e) {
            layout = List.of();
            return layout;
        }
    }

    private static final class Built {
        final ServerLevel level; final long end;
        final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        final Map<BlockPos, BlockState> todo = new LinkedHashMap<>();
        Built(ServerLevel level, long end) { this.level = level; this.end = end; }
    }
    private static final Map<UUID, Built> ACTIVE = new HashMap<>();

    public static boolean active(ServerPlayer p) { return ACTIVE.containsKey(p.getUUID()); }

    public static void toggle(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        if (active(p)) { close(p, "领域展开·诛伏赐死：收回"); return; }
        BlockPos anchor = p.blockPosition();
        BlockPos origin = anchor.offset(-HigurumaLayout.SIZE_X / 2, -23, -HigurumaLayout.SIZE_Z / 2);
        Built built = new Built(level, level.getGameTime() + LIFE);
        var light13 = Blocks.LIGHT.defaultBlockState()
            .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 13);
        Set<Long> filled = new HashSet<>();
        for (Cell cell : layout()) {
            filled.add(BlockPos.asLong(origin.getX() + cell.x(), origin.getY() + cell.y(), origin.getZ() + cell.z()));
        }
        // 结构内部剩余空位补 13 级光源方块（球半径 21 以内）
        double cx = HigurumaLayout.SIZE_X / 2.0, cy = 23.0, cz = HigurumaLayout.SIZE_Z / 2.0;
        for (int x = 0; x < HigurumaLayout.SIZE_X; x++)
            for (int y = 0; y < HigurumaLayout.SIZE_Y; y++)
                for (int z = 0; z < HigurumaLayout.SIZE_Z; z++) {
                    double d = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz));
                    if (d > 21.0) continue;
                    BlockPos pos = origin.offset(x, y, z);
                    if (filled.contains(pos.asLong())) continue;
                    if (level.getBlockState(pos).hasBlockEntity()) continue;
                    built.originals.putIfAbsent(pos.immutable(), level.getBlockState(pos));
                    built.todo.put(pos.immutable(), light13);
                }
        for (Cell cell : layout()) {
            BlockPos pos = origin.offset(cell.x(), cell.y(), cell.z());
            if (level.getBlockState(pos).hasBlockEntity()) continue;   // 保守：容器/机器不动
            built.originals.putIfAbsent(pos.immutable(), level.getBlockState(pos));
            built.todo.put(pos.immutable(), cell.state());
        }
        ACTIVE.put(p.getUUID(), built);
        p.sendSystemMessage(Component.literal("领域展开·诛伏赐死（再按收回）").withStyle(ChatFormatting.DARK_PURPLE));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (var it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Built b = e.getValue();
            if (b.level != level) continue;
            int budget = PER_TICK;
            var q = b.todo.entrySet().iterator();
            while (budget-- > 0 && q.hasNext()) {
                var entry = q.next();
                if (b.level.getBlockState(entry.getKey()).hasBlockEntity()) { q.remove(); continue; }
                b.level.setBlock(entry.getKey(), entry.getValue(), 2);
                q.remove();
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (owner != null) {
                if (now >= b.end) { it.remove(); restore(level, b); closeMsg(owner); continue; }
                owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 60, 0, false, false));
            }
        }
    }

    /** 服务端每刻驱动（施工/补光/到期）。 */
    public static void serverTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        for (ServerLevel level : event.getServer().getAllLevels()) tick(level);
    }

    public static void serverStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        for (Built b : ACTIVE.values()) restore(b.level, b);
        ACTIVE.clear();
    }

    public static void close(ServerPlayer p, String msg) {
        Built b = ACTIVE.remove(p.getUUID());
        if (b != null) restore(p.level() instanceof ServerLevel sl ? sl : b.level, b);
        p.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY));
    }

    private static void restore(ServerLevel level, Built b) {
        for (var entry : b.originals.entrySet()) {
            if (!level.isLoaded(entry.getKey())) continue;
            level.setBlock(entry.getKey(), entry.getValue(), 2);
        }
    }

    private static void closeMsg(ServerPlayer p) {
        p.sendSystemMessage(Component.literal("领域展开·诛伏赐死：结束").withStyle(ChatFormatting.GRAY));
    }

    private HigurumaDomain() {}
}
