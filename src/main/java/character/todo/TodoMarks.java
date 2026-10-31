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
 * 不义游戏的「咒力」记账。
 * <p>规则（用户口径）：
 * ①**生物默认自带咒力** —— 换位可以直接对任何生物使用，不必先注入；
 * ②方块 / 弹幕需要「注入咒力」，注入后**永久有效**直到被换位消耗；
 * ③纯客户端特效没有服务端对象 —— 注入命中的一瞬间就在那个位置**生成一个真正的实体**（{@code sukuna:todo_anchor}），
 * 之后就是普通的实体换位。
 */
public final class TodoMarks {
    /** 注入咒力的射线距离。 */
    public static final double INJECT_RANGE = 24;
    /** 换位能选到的最远距离。 */
    public static final double SWAP_RANGE = 32;
    /** NPC 的"随便传送"：不受准星夹角与距离限制时的搜索半径。 */
    public static final double FREE_RANGE = 64;
    /** 准星前方向量夹角门槛（约 ±70° 内才算"准星附近"）。 */
    public static final double AIM_DOT = 0.35;
    /** 优先目标加分（NPC 优先和自己的攻击目标换位）。 */
    public static final double PREFER_BONUS = 60;
    /** 客户端特效锚点：效果发出后多少刻内可以被注入。 */
    public static final int ANCHOR_TICKS = 40;
    /** 射线离特效锚点多近算命中（格）。 */
    public static final double ANCHOR_SNAP = 3.0;

    private record Ent(ServerLevel level, UUID mark, UUID owner, long at) {}
    private record Blk(ServerLevel level, BlockPos pos, UUID owner, long at) {}
    private record Key(ResourceKey<Level> dim, long pos) {}
    private record Anchor(ServerLevel level, Vec3 pos, long until) {}

    /** 实体标记：键是实体 UUID（弹幕、以及被注入后实体化的特效锚点都走这里）。 */
    private static final Map<UUID, Ent> ENTITIES = new LinkedHashMap<>();
    /** 方块标记：键是维度 + BlockPos.asLong。 */
    private static final Map<Key, Blk> BLOCKS = new LinkedHashMap<>();
    /** 最近发出的特效（服务端只知道位置，用来把"纯客户端特效"实体化）。 */
    private static final List<Anchor> ANCHORS = new ArrayList<>();

    /** 换位候选：实体或方块二选一。marked=带注入标记（换位后要消耗）。 */
    public record Swap(Entity entity, BlockPos block, Vec3 pos, boolean marked) {}

    // ------------------------------------------------------------ 注入
    /** 沿着施术者的准星注入：实体（含弹幕）→ 方块 → 纯客户端特效（就地实体化）。 */
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
        if (entityHit != null && entityDistance <= blockDistance) return markEntity(caster, entityHit.getEntity());
        if (blockHit.getType() != HitResult.Type.MISS) return markBlock(caster, blockHit.getBlockPos());
        // ③ 纯客户端特效：射线擦到最近发出去的特效 → 就地生成锚点实体再标记
        Vec3 spot = nearestAnchor(level, eye, look, range);
        if (spot != null) {
            TodoAnchorEntity anchor = new TodoAnchorEntity(Content.TODO_ANCHOR.get(), level);
            anchor.moveTo(spot.x, spot.y, spot.z, caster.getYRot(), 0);
            level.addFreshEntity(anchor);
            return markEntity(caster, anchor);
        }
        return false;
    }

    public static boolean markEntity(LivingEntity caster, Entity target) {
        if (!(caster.level() instanceof ServerLevel level) || target == caster) return false;
        ENTITIES.put(target.getUUID(), new Ent(level, target.getUUID(), caster.getUUID(), level.getGameTime()));
        // 锚点实体没有模型，客户端按"固定点"画标记（不需要跟着实体走）
        boolean asPoint = target instanceof TodoAnchorEntity;
        Network.send(level, new Network.Effect(TodoFx.MARK, asPoint ? 0 : target.getId(), target.position(),
            Vec3.ZERO, asPoint ? 1f : 2f, 0, target.getId()));
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

    /** 服务端每发一条特效就登记一次位置：这样"纯客户端特效"也能被注入成实体。 */
    public static void anchor(ServerLevel level, Network.Effect effect) {
        if (effect.life() <= 0 || effect.size() <= 0) return;
        int kind = effect.kind();
        if (kind >= TodoFx.FIRST && kind <= TodoFx.LAST) return;          // 自己这套不参与
        if (kind == 144 || kind == 300 || kind == 302 || kind == 303 || kind == 305
            || kind == 306 || kind == 307 || kind == 308) return;        // UI / 状态同步类
        long until = level.getGameTime() + ANCHOR_TICKS;
        if (ANCHORS.size() > 256) ANCHORS.remove(0);
        ANCHORS.add(new Anchor(level, effect.position(), until));
    }

    private static Vec3 nearestAnchor(ServerLevel level, Vec3 eye, Vec3 look, double range) {
        long now = level.getGameTime();
        ANCHORS.removeIf(a -> a.until() < now);
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Anchor anchor : ANCHORS) {
            if (anchor.level() != level) continue;
            Vec3 delta = anchor.pos().subtract(eye);
            double along = delta.dot(look);
            if (along < 0.5 || along > range) continue;
            double perpendicular = delta.subtract(look.scale(along)).length();
            if (perpendicular > ANCHOR_SNAP) continue;
            if (along < bestDistance) { bestDistance = along; best = anchor.pos(); }
        }
        return best;
    }

    // ------------------------------------------------------------ 找准星最近的有咒力的东西
    public static Swap nearest(LivingEntity caster) { return nearest(caster, null, false); }

    public static Swap nearest(LivingEntity caster, Entity prefer, boolean free) {
        if (!(caster.level() instanceof ServerLevel level)) return null;
        Vec3 eye = caster.getEyePosition(), look = caster.getLookAngle().normalize();
        double range = free ? FREE_RANGE : SWAP_RANGE;
        double bestScore = Double.NEGATIVE_INFINITY;
        Swap best = null;
        for (Iterator<Map.Entry<UUID, Ent>> it = ENTITIES.entrySet().iterator(); it.hasNext(); ) {
            Ent mark = it.next().getValue();
            Entity entity = level.getEntity(mark.mark());
            if (entity == null || !entity.isAlive() || entity == caster) { it.remove(); continue; }
            double score = free ? caster.distanceToSqr(entity) * -1
                : score(eye, look, entity.getBoundingBox().getCenter(), range);
            if (entity == prefer) score += PREFER_BONUS;
            if (score > bestScore) { bestScore = score; best = new Swap(entity, null, entity.getBoundingBox().getCenter(), true); }
        }
        for (Iterator<Map.Entry<Key, Blk>> it = BLOCKS.entrySet().iterator(); it.hasNext(); ) {
            Blk mark = it.next().getValue();
            if (mark.level() != level) continue;
            if (!mark.level().hasChunkAt(mark.pos())) continue;
            double score = free ? caster.distanceToSqr(Vec3.atCenterOf(mark.pos())) * -1
                : score(eye, look, Vec3.atCenterOf(mark.pos()), range);
            if (score > bestScore) { bestScore = score; best = new Swap(null, mark.pos(), Vec3.atCenterOf(mark.pos()), true); }
        }
        // 生物默认有咒力：没被注入过的生物也能直接换位
        for (var living : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(range),
                e -> e != caster && e.isAlive() && !e.isSpectator())) {
            double score = free ? caster.distanceToSqr(living) * -1
                : score(eye, look, living.getBoundingBox().getCenter(), range);
            if (living == prefer) score += PREFER_BONUS;
            if (score > bestScore) {
                bestScore = score;
                best = new Swap(living, null, living.getBoundingBox().getCenter(), ENTITIES.containsKey(living.getUUID()));
            }
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
    public static boolean swap(LivingEntity caster) { return swap(caster, null, false); }

    public static boolean swap(LivingEntity caster, Entity prefer) { return swap(caster, prefer, false); }

    /** NPC 用：不受准星/距离限制，想传就传。 */
    public static boolean swapFree(LivingEntity caster, Entity prefer) { return swap(caster, prefer, true); }

    public static boolean swap(LivingEntity caster, Entity prefer, boolean free) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        Swap target = nearest(caster, prefer, free);
        if (target == null) return false;
        Vec3 from = caster.position();
        if (target.entity() != null) {
            Entity other = target.entity();
            if (target.marked()) {
                ENTITIES.remove(other.getUUID());
                boolean asPoint = other instanceof TodoAnchorEntity;
                unmark(level, asPoint ? 0 : other.getId(), other.position(), asPoint ? 1f : 2f, other.getId());
                if (asPoint) other.discard();
            }
            Vec3 to = other.position();
            other.teleportTo(from.x, from.y, from.z);
            other.setDeltaMovement(Vec3.ZERO);
            caster.teleportTo(to.x, to.y, to.z);
            caster.setDeltaMovement(Vec3.ZERO);
            caster.fallDistance = 0;
            other.fallDistance = 0;
            if (other instanceof ServerPlayer sp) sp.connection.teleport(from.x, from.y, from.z, sp.getYRot(), sp.getXRot());
            // 拉到友方时，友方也吃同一段无敌帧（免得换过来就被集火）
            if (other instanceof LivingEntity living && !hostileTo(caster, living)) dodge(living, TodoCombat.DODGE_TICKS);
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

    /** 目标对施术者是否敌对（敌对的吃不到那次友方无敌帧）。 */
    private static boolean hostileTo(LivingEntity caster, LivingEntity other) {
        if (caster instanceof ServerPlayer sp) return CombatSettings.canHit(sp, other);
        if (caster instanceof TodoNpcEntity npc) return npc.getTarget() == other;
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
        ANCHORS.clear();
    }

    public static int count(LivingEntity caster) {
        int n = 0;
        for (Ent e : ENTITIES.values()) if (e.owner().equals(caster.getUUID())) n++;
        for (Blk b : BLOCKS.values()) if (b.owner().equals(caster.getUUID())) n++;
        return n;
    }

    private TodoMarks() {}
}
