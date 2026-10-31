package character.higuruma;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * 诛伏赐死领域：固定锚点、先记录原始方块、按距离由内向外逐步施工；
 * 再次施放/到期/关服整批还原。内部补光（避免太黑）。
 */
public final class HigurumaDomain {
    private static final int RADIUS = 25, HEIGHT = 15, PER_TICK = 2500, LIFE = 600;
    private static final class Built {
        final ServerLevel level; final BlockPos anchor; final long end;
        final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        final Deque<BlockPos> openQueue = new ArrayDeque<>();
        Built(ServerLevel level, BlockPos anchor, long end) { this.level = level; this.anchor = anchor; this.end = end; }
    }
    private static final Map<UUID, Built> ACTIVE = new HashMap<>();

    public static boolean active(ServerPlayer p) { return ACTIVE.containsKey(p.getUUID()); }

    public static void toggle(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        if (active(p)) { close(p, "领域展开·诛伏赐死：收回"); return; }
        BlockPos anchor = p.blockPosition();
        Built built = new Built(level, anchor, level.getGameTime() + LIFE);
        List<BlockPos> positions = new ArrayList<>();
        int cx = anchor.getX(), cy = anchor.getY(), cz = anchor.getZ();
        // 球形球壳（半径 25，一格外壳），内部清成空气
        for (int dx = -RADIUS; dx <= RADIUS; dx++)
            for (int dy = -RADIUS; dy <= RADIUS; dy++)
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > RADIUS) continue;
                    BlockPos pos = new BlockPos(cx + dx, cy + dy, cz + dz);
                    positions.add(pos);
                }
        positions.sort(Comparator.comparingDouble(p0 -> p0.distSqr(anchor)));
        for (BlockPos pos : positions) {
            if (level.getBlockState(pos).hasBlockEntity()) continue;   // 保守：容器/机器一律不动
            built.originals.put(pos.immutable(), level.getBlockState(pos));
            built.openQueue.add(pos.immutable());
        }
        // 断头台：8 座 15 格高
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            BlockPos at = anchor.offset((int) Math.round(Math.cos(a) * (RADIUS - 5)), 1, (int) Math.round(Math.sin(a) * (RADIUS - 5)));
            for (int y = 0; y < HEIGHT; y++)
                for (int k = 0; k < 2; k++) {
                    BlockPos pos = at.offset(k, y, 0);
                    if (level.getBlockState(pos).hasBlockEntity()) continue;
                    built.originals.putIfAbsent(pos.immutable(), level.getBlockState(pos));
                    built.openQueue.add(pos.immutable());
                }
        }
        ACTIVE.put(p.getUUID(), built);
        p.sendSystemMessage(Component.literal("领域展开·诛伏赐死（再按收回）").withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** 施工/还原/补光/到期的 tick。 */
    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (var it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Built b = e.getValue();
            if (b.level != level) continue;
            int budget = PER_TICK;
            while (budget-- > 0 && !b.openQueue.isEmpty()) {
                BlockPos pos = b.openQueue.poll();
                BlockState original = b.originals.get(pos);
                if (original != null && original.hasBlockEntity()) continue;
                BlockState state;
                int dy = pos.getY() - b.anchor.getY();
                double d = Math.sqrt(pos.distSqr(b.anchor));
                int rx = pos.getX() - b.anchor.getX(), rz = pos.getZ() - b.anchor.getZ();
                double dist = Math.sqrt(pos.distSqr(b.anchor));
                if (dist > RADIUS - 1.5) state = Blocks.BLACK_CONCRETE.defaultBlockState();   // 球壳
                else if (dy == -1) state = Blocks.BIRCH_PLANKS.defaultBlockState();            // 地板
                else if (dy == 0) {
                    // 法庭：法官席（北侧高台）+ 被告席 + 旁听长椅 + 过道
                    if (rz <= -12 && rz >= -16 && Math.abs(rx) <= 7) state = Blocks.DARK_OAK_PLANKS.defaultBlockState();
                    else if (Math.abs(rx) <= 1 && rz > -12 && rz < 0) state = Blocks.POLISHED_ANDESITE.defaultBlockState();
                    else if (rz >= 2 && rz <= 16 && Math.abs(rx) <= 8 && rz % 3 == 2) state = Blocks.DARK_OAK_PLANKS.defaultBlockState();
                    else state = Blocks.BIRCH_PLANKS.defaultBlockState();
                }
                else if (dy == 1 && rz <= -12 && rz >= -16 && Math.abs(rx) <= 7) state = Blocks.DARK_OAK_PLANKS.defaultBlockState(); // 法官席台阶
                else if (dy == 2 && rz <= -14 && rz >= -16 && Math.abs(rx) <= 6) state = Blocks.DARK_OAK_PLANKS.defaultBlockState(); // 法官席面
                else if (dy >= 1 && Math.abs(rx) == 9 && (rz == -10 || rz == 10)) state = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(); // 柱子
                else state = Blocks.LIGHT.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15);
                level.setBlock(pos, state, 2);
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (owner != null) {
                if (now >= b.end) { it.remove(); startRestore(e.getKey(), b); closeMessage(owner); continue; }
                if (owner.distanceToSqr(b.anchor.getX(), b.anchor.getY(), b.anchor.getZ()) < RADIUS * RADIUS)
                    owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 60, 0, false, false));
            }
        }
    }

    private static void startRestore(UUID id, Built b) {
        for (var entry : b.originals.entrySet()) {
            b.level.setBlock(entry.getKey(), entry.getValue(), 2);
        }
    }

    /** 服务端每刻驱动（施工/补光/到期）。 */
    public static void serverTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        for (ServerLevel level : event.getServer().getAllLevels()) tick(level);
    }

    public static void serverStopped(net.minecraftforge.event.server.ServerStoppedEvent event) { stopAll(); }

    public static void close(ServerPlayer p, String msg) {
        Built b = ACTIVE.remove(p.getUUID());
        if (b != null) startRestore(p.getUUID(), b);
        p.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY));
    }

    private static void closeMessage(ServerPlayer p) { p.sendSystemMessage(Component.literal("领域展开·诛伏赐死：结束").withStyle(ChatFormatting.GRAY)); }

    public static void stopAll() {
        for (Built b : ACTIVE.values()) startRestore(UUID.randomUUID(), b);
        ACTIVE.clear();
    }

    private HigurumaDomain() {}
}
