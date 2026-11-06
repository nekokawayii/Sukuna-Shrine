package character.culling;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 死灭洄游的两类「门」（r251）：
 *
 * <p>① 主世界的半透明黑色立方体结界（方式一）：极低概率在玩家附近生成，黑色玻璃外壳 + 一道红色缝隙，
 * 走进缝隙就被拉进结界；
 * <p>② 结界内的「初始登记室」：位于死灭洄游维度 (1024, 1024)，默认出生点；对着房间里的木门右键
 * 才真正进入 512×512 的城市。
 */
public final class CullingGate {
    /** 登记室：房间左下角参考点（地面那一层）。 */
    public static final int ROOM_X = 1024, ROOM_Y = 70, ROOM_Z = 1024;
    /** 城市出生点（原来的进入位置）。 */
    public static final double CITY_X = .5, CITY_Y = 65, CITY_Z = -15.5;
    /** 立方体结界的缝隙方块位置（渲染/判定用）。 */
    private static final List<BlockPos> SEAMS = new ArrayList<>();
    /** 结界立方体中心（避免同一片地方刷两个）。 */
    private static final List<BlockPos> CUBES = new ArrayList<>();
    private static final int CUBE_HALF = 4;

    // ====================== 登记室 ======================

    /** 把 1024,1024 的房间盖出来（幂等：只补空气/缺失方块）。 */
    public static void ensureRoom(ServerLevel level) {
        BlockPos center = new BlockPos(ROOM_X, ROOM_Y, ROOM_Z);
        if (!level.getBlockState(center.below()).is(Blocks.BLACK_CONCRETE)) {
            build(level, center);
        }
    }

    private static void build(ServerLevel level, BlockPos c) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = -1; dy <= 5; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    boolean floor = dy == -1, roof = dy == 5;
                    boolean wall = Math.abs(dx) == 6 || Math.abs(dz) == 6;
                    BlockState state = Blocks.AIR.defaultBlockState();
                    if (floor) state = Blocks.BLACK_CONCRETE.defaultBlockState();
                    else if (roof) state = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
                    else if (wall) state = Blocks.BLACK_CONCRETE.defaultBlockState();
                    level.setBlockAndUpdate(p, state);
                }
            }
        }
        // 四角红柱 + 天花板红灯，营造「登记室」的红光
        for (int dx : new int[]{-5, 5}) for (int dz : new int[]{-5, 5})
            for (int dy = 0; dy <= 4; dy++)
                level.setBlockAndUpdate(c.offset(dx, dy, dz), Blocks.RED_CONCRETE.defaultBlockState());
        for (int dx = -3; dx <= 3; dx += 3) for (int dz = -3; dz <= 3; dz += 3)
            level.setBlockAndUpdate(c.offset(dx, 4, dz), Blocks.REDSTONE_LAMP.defaultBlockState());
        // 木门：+Z 面正中，两格高
        BlockPos lower = doorLower(c);
        BlockState door = Blocks.OAK_DOOR.defaultBlockState()
            .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
            .setValue(DoorBlock.FACING, net.minecraft.core.Direction.SOUTH);
        level.setBlockAndUpdate(lower, door);
        level.setBlockAndUpdate(lower.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        com.mojang.logging.LogUtils.getLogger().info("[死灭洄游] 登记室已生成 @{}", c);
    }

    /** 木门下半格坐标。 */
    public static BlockPos doorLower(BlockPos center) {
        return new BlockPos(center.getX(), center.getY(), center.getZ() + 6);
    }

    /** 房间出生点。 */
    public static BlockPos spawnPos() { return new BlockPos(ROOM_X, ROOM_Y, ROOM_Z); }

    /** 是否在登记室范围（用于放开边界回拉）。 */
    public static boolean inRoomArea(BlockPos p) {
        return Math.abs(p.getX() - ROOM_X) <= 16 && Math.abs(p.getZ() - ROOM_Z) <= 16
            && p.getY() >= ROOM_Y - 4 && p.getY() <= ROOM_Y + 12;
    }

    /** 右键木门 → 进城市。 */
    @SubscribeEvent
    public static void useDoor(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!p.level().dimension().equals(CullingGame.DIM)) return;
        BlockPos pos = e.getPos();
        BlockPos base = pos.below();
        boolean hit = pos.equals(doorLower(new BlockPos(ROOM_X, ROOM_Y, ROOM_Z)))
            || base.equals(doorLower(new BlockPos(ROOM_X, ROOM_Y, ROOM_Z)));
        if (!hit) return;
        e.setCanceled(true);
        ServerLevel level = p.serverLevel();
        p.teleportTo(level, CITY_X, CITY_Y, CITY_Z, 0, 0);
        p.fallDistance = 0;
        level.playSound(null, p.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1f, .8f);
        p.sendSystemMessage(Component.literal("木门之后，是 512×512 的废弃都市。").withStyle(ChatFormatting.RED));
    }

    // ====================== 方式一：主世界的黑色立方体 ======================

    /** 极低概率在玩家附近生成一个立方体结界。 */
    @SubscribeEvent
    public static void spawnCube(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!p.level().dimension().equals(Level.OVERWORLD) || p.isCreative()) return;
        if (p.tickCount % 600 != 0) return;                 // 每 30 秒掷一次
        if (p.getRandom().nextInt(24) != 0) return;          // 约 1/24 → 平均 12 分钟一个
        ServerLevel level = p.serverLevel();
        if (CUBES.stream().anyMatch(c -> c.distSqr(p.blockPosition()) < 96 * 96)) return;
        if (CUBES.size() > 8) CUBES.remove(0);
        BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            p.blockPosition().offset(p.getRandom().nextInt(41) - 20, 0, p.getRandom().nextInt(41) - 20));
        buildCube(level, at);
    }

    private static void buildCube(ServerLevel level, BlockPos base) {
        for (int dx = -CUBE_HALF; dx <= CUBE_HALF; dx++) {
            for (int dy = 0; dy <= CUBE_HALF * 2; dy++) {
                for (int dz = -CUBE_HALF; dz <= CUBE_HALF; dz++) {
                    BlockPos p = base.offset(dx, dy, dz);
                    level.setBlockAndUpdate(p, Blocks.BLACK_STAINED_GLASS.defaultBlockState());
                }
            }
        }
        // 正面（+Z）开一道 2 格宽、3 格高的红色缝隙
        BlockPos seam = base.offset(0, 1, CUBE_HALF);
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = seam.offset(dx, dy, 0);
            level.setBlockAndUpdate(p, dx == 0 ? Blocks.AIR.defaultBlockState() : Blocks.RED_CONCRETE.defaultBlockState());
            if (dx == 0 && dy == 0) SEAMS.add(p);
        }
        CUBES.add(base);
        level.playSound(null, base, SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1.2f, .55f);
        com.mojang.logging.LogUtils.getLogger().info("[死灭洄游] 立方体结界生成 @{}", base);
    }

    /** 走进缝隙 → 拉进结界。 */
    @SubscribeEvent
    public static void touchSeam(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!p.level().dimension().equals(Level.OVERWORLD) || p.isCreative()) return;
        if (p.tickCount % 5 != 0 || SEAMS.isEmpty()) return;
        for (BlockPos seam : SEAMS) {
            if (p.blockPosition().distSqr(seam) > 4) continue;
            p.sendSystemMessage(Component.literal("雪花噪点闪过，羂索的声音在耳边响起：「欢迎来到死灭回游，棋子。」")
                .withStyle(ChatFormatting.DARK_RED));
            p.level().playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1f, .7f);
            SEAMS.remove(seam);
            CullingGame.enter(p);
            return;
        }
    }

    private CullingGate() { }
}
