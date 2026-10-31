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
        boolean expanded;
        BlockPos amethyst, budding;
        final Set<UUID> pinned = new HashSet<>();
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
        // 用户结构是 48³ 裁切版，边缘可能缺壳：按球面半径补齐黑混凝土，避免「球壳边缘空了」
        double scx = HigurumaLayout.SIZE_X / 2.0, scy = 23.0, scz = HigurumaLayout.SIZE_Z / 2.0;
        for (int x = 0; x < HigurumaLayout.SIZE_X; x++)
            for (int y = 0; y < HigurumaLayout.SIZE_Y; y++)
                for (int z = 0; z < HigurumaLayout.SIZE_Z; z++) {
                    double d = Math.sqrt((x - scx) * (x - scx) + (y - scy) * (y - scy) + (z - scz) * (z - scz));
                    if (d <= 21.0 || d > 23.5) continue;
                    BlockPos pos = origin.offset(x, y, z);
                    if (filled.contains(pos.asLong())) continue;
                    if (level.getBlockState(pos).hasBlockEntity()) continue;
                    built.originals.putIfAbsent(pos.immutable(), level.getBlockState(pos));
                    built.todo.put(pos.immutable(), Blocks.BLACK_CONCRETE.defaultBlockState());
                }
        for (Cell cell : layout()) {
            BlockPos pos = origin.offset(cell.x(), cell.y(), cell.z());
            if (level.getBlockState(pos).hasBlockEntity()) continue;   // 保守：容器/机器不动
            built.originals.putIfAbsent(pos.immutable(), level.getBlockState(pos));
            built.todo.put(pos.immutable(), cell.state());
        }
        for (Cell cell : layout()) {
            if (cell.state().is(net.minecraft.world.level.block.Blocks.AMETHYST_BLOCK))
                built.amethyst = origin.offset(cell.x(), cell.y(), cell.z());
            else if (cell.state().is(net.minecraft.world.level.block.Blocks.BUDDING_AMETHYST))
                built.budding = origin.offset(cell.x(), cell.y(), cell.z());
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
            if (b.todo.isEmpty() && !b.expanded) {
                b.expanded = true;
                expandFinish(b, e.getKey());
            }
            if (b.expanded) holdPinned(b);
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

    /** 完全展开：日车传送到紫水晶块上；其他生物传送到紫水晶母块上并定住。 */
    private static void expandFinish(Built b, UUID ownerId) {
        if (b.amethyst == null || b.budding == null) return;
        ServerPlayer owner = b.level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null) owner.teleportTo(b.amethyst.getX() + 0.5, b.amethyst.getY() + 1.0, b.amethyst.getZ() + 0.5);
        var center = net.minecraft.world.phys.Vec3.atCenterOf(b.amethyst != null ? b.amethyst : b.budding);
        for (var ent : b.level.getEntities((net.minecraft.world.entity.Entity) null,
                new net.minecraft.world.phys.AABB(center, center).inflate(24),
                ent -> ent instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive())) {
            if (ent.getUUID().equals(ownerId)) continue;
            b.pinned.add(ent.getUUID());
            if (ent instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
            ent.teleportTo(b.budding.getX() + 0.5, b.budding.getY() + 1.0, b.budding.getZ() + 0.5);
        }
    }

    private static void holdPinned(Built b) {
        for (UUID id : b.pinned) {
            var ent = b.level.getEntity(id);
            if (ent == null || !ent.isAlive()) continue;
            double dx = ent.getX() - (b.budding.getX() + 0.5), dz = ent.getZ() - (b.budding.getZ() + 0.5);
            if (dx * dx + dz * dz > 0.04 || Math.abs(ent.getY() - (b.budding.getY() + 1.0)) > 0.6)
                ent.teleportTo(b.budding.getX() + 0.5, b.budding.getY() + 1.0, b.budding.getZ() + 0.5);
        }
    }

    /** 领域内所有人/生物互相不能造成伤害。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void noDamage(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
        for (Built b : ACTIVE.values()) {
            if (!b.expanded) continue;
            if (inside(b, e.getEntity())) { e.setCanceled(true); return; }
            if (e.getSource().getEntity() != null && inside(b, e.getSource().getEntity())) { e.setCanceled(true); return; }
        }
    }

    private static boolean inside(Built b, net.minecraft.world.entity.Entity ent) {
        if (b.amethyst == null || ent.level() != b.level) return false;
        var c = net.minecraft.world.phys.Vec3.atCenterOf(b.amethyst);
        return ent.position().distanceToSqr(c) < 21 * 21;
    }

    private static void restore(ServerLevel level, Built b) {
        for (UUID id : b.pinned) {
            var ent = level.getEntity(id);
            if (ent instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(false);
        }
        b.pinned.clear();
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
