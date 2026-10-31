package character.todo;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/**
 * 不义游戏的「咒力标记」：任何方块 / 实体（含弹幕）都能被注入咒力，**永久有效直到被换位消耗**。
 * <p>标记只存在服务端（世界对象），换位时按"离准星最近"挑一个。
 */
public final class TodoMarks {
    /** 注入咒力的射线距离。 */
    public static final double INJECT_RANGE = 24;
    /** 换位能选到的最远距离。 */
    public static final double SWAP_RANGE = 32;
    /** 准星前方向量夹角门槛（约 ±70° 内才算"准星附近"）。 */
    public static final double AIM_DOT = 0.35;

    private record Ent(ServerLevel level, UUID mark, UUID owner, long at) {}
    private record Blk(ServerLevel level, BlockPos pos, UUID owner, long at) {}
    private record Key(ResourceKey<Level> dim, long pos) {}

    /** 实体标记：键是实体 UUID（弹幕也是实体）。 */
    private static final Map<UUID, Ent> ENTITIES = new LinkedHashMap<>();
    /** 方块标记：键是维度 + BlockPos.asLong。 */
    private static final Map<Key, Blk> BLOCKS = new LinkedHashMap<>();

    /** 换位候选：实体（含弹幕）或方块二选一。 */
    public record Swap(Entity entity, BlockPos block, Vec3 pos) {}

    // ------------------------------------------------------------ 注入
    /** 沿着施术者的准星注入：先看实体（含弹幕），再看方块。返回是否命中。 */
    public static boolean inject(LivingEntity caster, double range) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        Vec3 eye = caster.getEyePosition(), look = caster.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        AABB search = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(caster, eye, end, search,
            entity -> entity != caster && entity.isAlive() && !entity.isSpectator(), range * range);
        BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, caster));
        double entityDistance = entityHit == null ? Double.MAX_VALUE : entityHit.getLocation().distanceToSqr(eye);
        double blockDistance = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : blockHit.getLocation().distanceToSqr(eye);
        if (entityHit != null && entityDistance <= blockDistance) {
            return markEntity(caster, entityHit.getEntity());
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            return markBlock(caster, blockHit.getBlockPos());
        }
        return false;
    }

    public static boolean markEntity(LivingEntity caster, Entity target) {
        if (!(caster.level() instanceof ServerLevel level) || target == caster) return false;
        ENTITIES.put(target.getUUID(), new Ent(level, target.getUUID(), caster.getUUID(), level.getGameTime()));
        Network.send(level, new Network.Effect(TodoFx.MARK, target.getId(), target.position(), Vec3.ZERO,
            2f, 0, target.getId()));
        Network.send(level, new Network.Effect(TodoFx.INJECT, caster.getId(), target.position(), Vec3.ZERO,
            1.6f, 10, 0));
        return true;
    }

    public static boolean markBlock(LivingEntity caster, BlockPos pos) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        BLOCKS.put(new Key(level.dimension(), pos.asLong()), new Blk(level, pos, caster.getUUID(), level.getGameTime()));
        Network.send(level, new Network.Effect(TodoFx.MARK, 0, Vec3.atCenterOf(pos), Vec3.ZERO, 1f, 0, pos.asLong()));
        Network.send(level, new Network.Effect(TodoFx.INJECT, caster.getId(), Vec3.atCenterOf(pos), Vec3.ZERO,
            1.6f, 10, 0));
        return true;
    }

    // ------------------------------------------------------------ 找最近的标记
    public static Swap nearest(LivingEntity caster) {
        if (!(caster.level() instanceof ServerLevel level)) return null;
        Vec3 eye = caster.getEyePosition(), look = caster.getLookAngle().normalize();
        double bestScore = Double.NEGATIVE_INFINITY;
        Swap best = null;
        for (Iterator<Map.Entry<UUID, Ent>> it = ENTITIES.entrySet().iterator(); it.hasNext(); ) {
            Ent mark = it.next().getValue();
            Entity entity = level.getEntity(mark.mark());
            if (entity == null || !entity.isAlive() || entity == caster) { it.remove(); continue; }
            double score = score(eye, look, entity.getBoundingBox().getCenter(), SWAP_RANGE);
            if (score > bestScore) { bestScore = score; best = new Swap(entity, null, entity.getBoundingBox().getCenter()); }
        }
        for (Iterator<Map.Entry<Key, Blk>> it = BLOCKS.entrySet().iterator(); it.hasNext(); ) {
            Blk mark = it.next().getValue();
            if (mark.level() != level) continue;
            if (!mark.level().hasChunkAt(mark.pos())) continue;
            double score = score(eye, look, Vec3.atCenterOf(mark.pos()), SWAP_RANGE);
            if (score > bestScore) { bestScore = score; best = new Swap(null, mark.pos(), Vec3.atCenterOf(mark.pos())); }
        }
        return best;
    }

    private static double score(Vec3 eye, Vec3 look, Vec3 at, double range) {
        Vec3 delta = at.subtract(eye);
        double distance = delta.length();
        if (distance > range || distance < 0.5) return Double.NEGATIVE_INFINITY;
        double dot = delta.scale(1 / distance).dot(look);
        if (dot < AIM_DOT) return Double.NEGATIVE_INFINITY;
        return dot * 100 - distance;
    }

    // ------------------------------------------------------------ 换位
    /** 与准星最近的有咒力的东西换位；换位前后各 0.3 秒（共 12 刻）免疫伤害。 */
    public static boolean swap(LivingEntity caster) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        Swap target = nearest(caster);
        if (target == null) return false;
        Vec3 from = caster.position();
        if (target.entity() != null) {
            Entity other = target.entity();
            ENTITIES.remove(other.getUUID());
            unmark(level, other.getId(), other.position(), 2f, other.getId());
            Vec3 to = other.position();
            other.teleportTo(from.x, from.y, from.z);
            other.setDeltaMovement(Vec3.ZERO);
            caster.teleportTo(to.x, to.y, to.z);
            caster.setDeltaMovement(Vec3.ZERO);
            caster.fallDistance = 0;
            other.fallDistance = 0;
            if (other instanceof ServerPlayer sp) sp.connection.teleport(from.x, from.y, from.z, sp.getYRot(), sp.getXRot());
        } else {
            BlockPos pos = target.block();
            BLOCKS.remove(new Key(level.dimension(), pos.asLong()));
            unmark(level, 0, Vec3.atCenterOf(pos), 1f, pos.asLong());
            swapBlock(caster, pos);
        }
        dodge(caster, TodoCombat.DODGE_TICKS);
        Network.send(level, new Network.Effect(TodoFx.SWAP, caster.getId(), from, caster.position(), 1.4f, 12, 0));
        level.playSound(null, caster.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.5f);
        return true;
    }

    /** 方块换位：目标方块搬到施术者脚下，施术者站到目标方块原来的位置。 */
    private static void swapBlock(LivingEntity caster, BlockPos pos) {
        ServerLevel level = (ServerLevel) caster.level();
        BlockPos feet = caster.blockPosition();
        ServerPlayer authority = caster instanceof ServerPlayer sp ? sp : null;
        boolean allowed = authority != null
            && SkillDestruction.canMove(authority, pos) && SkillDestruction.canMove(authority, feet);
        if (allowed) {
            BlockState target = level.getBlockState(pos);
            BlockState old = level.getBlockState(feet);
            level.setBlock(feet, target, 3);
            level.setBlock(pos, old, 3);
            caster.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        } else {
            // 受保护/无法改动地形：只做位移，站到方块上方
            caster.teleportTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            if (authority != null) authority.displayClientMessage(
                net.minecraft.network.chat.Component.literal("该方块受保护：只瞬移，不交换方块"), true);
        }
        caster.setDeltaMovement(Vec3.ZERO);
        caster.fallDistance = 0;
    }

    /** 规避伤害窗口（刻）。 */
    public static void dodge(LivingEntity caster, int ticks) {
        long until = caster.level().getGameTime() + ticks;
        if (caster instanceof ServerPlayer p) Curse.data(p).putLong(TodoCombat.DODGE_UNTIL, until);
        else caster.getPersistentData().putLong(TodoCombat.DODGE_UNTIL, until);
    }

    public static boolean dodging(LivingEntity caster) {
        long until = caster instanceof ServerPlayer p ? Curse.data(p).getLong(TodoCombat.DODGE_UNTIL)
            : caster.getPersistentData().getLong(TodoCombat.DODGE_UNTIL);
        return until > caster.level().getGameTime();
    }

    private static void unmark(ServerLevel level, int owner, Vec3 pos, float size, long seed) {
        Network.send(level, new Network.Effect(TodoFx.UNMARK, owner, pos, Vec3.ZERO, size, 0, seed));
    }

    /** 清掉某名施术者挂的所有标记（创造恢复符 / 退出时用）。 */
    public static void release(UUID owner) {
        ENTITIES.entrySet().removeIf(e -> e.getValue().owner().equals(owner));
        BLOCKS.entrySet().removeIf(e -> e.getValue().owner().equals(owner));
    }

    public static void reset() {
        ENTITIES.clear();
        BLOCKS.clear();
    }

    public static int count(LivingEntity caster) {
        int n = 0;
        for (Ent e : ENTITIES.values()) if (e.owner().equals(caster.getUUID())) n++;
        for (Blk b : BLOCKS.values()) if (b.owner().equals(caster.getUUID())) n++;
        return n;
    }

    private TodoMarks() {}
}
