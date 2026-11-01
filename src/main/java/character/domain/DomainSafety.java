package character.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 领域里的「落点是否站得住」判定。
 *
 * <p><b>r216：这里已经没有任何传送了。</b>用户两次要求删掉领域安全传送（原话「领域的安全传送删了吧，bug太多了」
 * →「安全传送还是有」）。r214 只是把 {@link #evacuate} 改成空实现，但真正在挪人的是
 * {@code recover(LivingEvent)} 那个每 10 刻的监听（领域一消失就把范围内、还站在旧包围盒里的生物
 * findExit / emergencyPlatform / carveRefuge 三连救出去），它压根不走 evacuate，所以看起来"还是有"。
 * 现在整段连同 {@code ZONES} 登记、{@code watch}、散列救援一起删干净了：
 * 领域收起/还原时，站在里面的生物就地留下（可能被还原的方块埋住，这是用户接受的代价）。
 *
 * <p>只保留两种纯判定（不移动任何东西）：{@link #safe} 与 {@link #safeForMovement}，
 * 供真人灵魂闪避挑落点、万里之锁挑落地位置使用。
 */
public final class DomainSafety {
    /** r216：兼容旧调用点，不登记任何区域（不再有疏散）。 */
    public static void watch(ServerLevel level, java.util.UUID owner, Vec3 center, double radius) {
    }

    /** r216：兼容旧调用点，什么都不做。 */
    public static void release(ServerLevel level, java.util.UUID owner, AABB fallback) {
    }

    /** r216：用户要求删除安全传送——空实现，兼容 DomainTerrain / NpcDomain / Mainline 的旧调用。 */
    public static void evacuate(ServerLevel level, AABB area) {
    }

    private static boolean dangerous(BlockState s) {
        return s.is(Blocks.LAVA) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.MAGMA_BLOCK)
            || s.is(Blocks.CACTUS) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE)
            || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.WITHER_ROSE);
    }

    private static boolean aquatic(LivingEntity e) {
        return e instanceof net.minecraft.world.entity.animal.WaterAnimal;
    }

    private static boolean safeWater(ServerLevel level, LivingEntity e, Vec3 at) {
        AABB box = e.getBoundingBox().move(at.subtract(e.position())).deflate(.001);
        if (!level.getWorldBorder().isWithinBounds(box) || box.minY < level.getMinBuildHeight() + 1
            || box.maxY >= level.getMaxBuildHeight()) return false;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
            BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
            if (!level.hasChunkAt(p) || DomainTerrain.changing(level, p)
                || !level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)) return false;
        return level.noCollision(e, box);
    }

    public static boolean safe(ServerLevel level, LivingEntity e, Vec3 feet) {
        return aquatic(e) ? safeWater(level, e, feet) : safe(level, e, feet, true);
    }

    public static boolean safeForMovement(ServerLevel level, LivingEntity e, Vec3 feet) {
        return safe(level, e, feet, false);
    }

    private static boolean safe(ServerLevel level, LivingEntity e, Vec3 feet, boolean stable) {
        AABB box = e.getBoundingBox().move(feet.subtract(e.position())).deflate(.001);
        if (box.minY <= level.getMinBuildHeight() || box.maxY >= level.getMaxBuildHeight()
            || !level.getWorldBorder().isWithinBounds(box)) return false;
        BlockPos support = BlockPos.containing(feet.x, feet.y - .05, feet.z);
        if (!level.hasChunkAt(support) || !level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY - .05, box.minZ),
            BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!level.hasChunkAt(pos) || stable && DomainTerrain.changing(level, pos)) return false;
            BlockState s = level.getBlockState(pos);
            if (dangerous(s) || !s.getFluidState().isEmpty()) return false;
        }
        if (!level.noCollision(e, box)) return false;
        // 覆盖整个脚底，杜绝宽体咒灵只靠一个方块悬在悬崖边。
        int y = (int) Math.floor(feet.y - .05);
        for (int x = (int) Math.floor(box.minX); x <= Math.floor(box.maxX); x++)
            for (int z = (int) Math.floor(box.minZ); z <= Math.floor(box.maxZ); z++) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState s = level.getBlockState(p);
                if (stable && DomainTerrain.changing(level, p) || dangerous(s)
                    || !s.isFaceSturdy(level, p, Direction.UP)) return false;
            }
        return true;
    }

    private DomainSafety() {}
}
