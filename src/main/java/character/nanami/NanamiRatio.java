package character.nanami;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 十划咒法的 7:3 弱点：把目标的高度等分为十段，第 7 段（60%~70%）是金色区，
 * 命中 **70% 高度那条分割线** 就算弱点命中 → 无视 70% 防御、伤害 ×3。
 *
 * <p>命中判定 = 玩家准星射线打到目标包围盒的交点，与 70% 高度的差 &lt; 容差。
 * 怪物体型不同 → 这条线的高度不同，这就是"需要准星跟随"的难点。
 */
public final class NanamiRatio {
    /** 7:3 的比例位置（从脚底往上算）。 */
    public static final double RATIO = 0.70;
    /** 基础命中容差（格）：很小，必须真的瞄准那条线。 */
    public static final double TOLERANCE = 0.15;
    /** 加班期间容差 ×1.5；被瓦落瓦落眩晕的目标弱点 ×2。 */
    public static final double OVERTIME_TOLERANCE = 1.5, STUN_TOLERANCE = 2.0;
    /** 暴击倍率与护甲无视比例。 */
    public static final double CRIT_MULTIPLIER = 3.0, ARMOR_IGNORE = 0.70;
    /** 弱点存在时长 / 数轴冷却 / 自动上标记距离。 */
    public static final int MARK_TICKS = 200, AXIS_COOLDOWN = 200, REACH = 15;
    /** 隙的暴击基础伤害。 */
    public static final double GAP_CRIT_BASE = 30;

    private static final String MARK_UNTIL = "NanamiRatioUntil", MARK_BY = "NanamiRatioBy";
    private static final String AXIS_READY = "NanamiAxisReady", OVERTIME_UNTIL = "NanamiOvertimeUntil";
    private static final String OVERTIME_KILL = "NanamiOvertimeKill", GUARD_UNTIL = "NanamiGuardUntil";
    private static final UUID STRIP_ID = UUID.fromString("7a11c0de-2600-4a4a-9c9c-000000000001");

    /** 被眩晕到什么时候（弱点放大两倍）。 */
    private static final Map<UUID, Long> STUNNED = new HashMap<>();
    /** 护甲削减修饰：到点拆掉（只活 2 刻）。 */
    private record Strip(LivingEntity target, long until) {}
    private static final Map<UUID, Strip> STRIPPED = new HashMap<>();
    /** 正在由本类直接结算伤害（避免 hit 事件里二次触发弱点判定）。 */
    private static boolean critting;

    private static long now(LivingEntity e) { return e.level().getGameTime(); }

    // ------------------------------------------------------------ 标记
    public static boolean marked(ServerPlayer p, LivingEntity target) {
        CompoundTag tag = target.getPersistentData();
        return now(target) < tag.getLong(MARK_UNTIL) && p.getUUID().toString().equals(tag.getString(MARK_BY));
    }

    public static void mark(ServerPlayer p, LivingEntity target) {
        CompoundTag tag = target.getPersistentData();
        tag.putLong(MARK_UNTIL, now(target) + MARK_TICKS);
        tag.putString(MARK_BY, p.getUUID().toString());
        sync(target, true);
    }

    public static void unmark(LivingEntity target) {
        CompoundTag tag = target.getPersistentData();
        tag.remove(MARK_UNTIL);
        tag.remove(MARK_BY);
        sync(target, false);
    }

    /** 轴的开/关同步（别的玩家也能看见这条数轴）。 */
    public static void sync(LivingEntity target, boolean on) {
        if (!(target.level() instanceof ServerLevel level)) return;
        long flags = 0;
        if (stunned(target)) flags |= 1;
        Network.send(level, new Network.Effect(NanamiFx.AXIS, target.getId(), target.position(), Vec3.ZERO,
            on ? 1f : 0f, on ? MARK_TICKS : 0, flags));
    }

    // ------------------------------------------------------------ 弱点几何
    /** 弱点分割线的高度：从脚底往上 70% 身高。 */
    public static double weakY(LivingEntity target) {
        return target.getY() + target.getBbHeight() * RATIO;
    }

    public static double tolerance(ServerPlayer p, LivingEntity target) {
        double tolerance = TOLERANCE;
        if (stunned(target)) tolerance *= STUN_TOLERANCE;
        if (overtime(p)) tolerance *= OVERTIME_TOLERANCE;
        return tolerance;
    }

    /** 玩家准星射线落在目标身上的高度。 */
    public static double aimY(ServerPlayer p, LivingEntity target) {
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        AABB box = target.getBoundingBox().inflate(0.4);
        var hit = box.clip(eye, eye.add(dir.scale(64)));
        if (hit.isPresent()) return hit.get().y;
        Vec3 center = box.getCenter();
        double t = Math.max(0, center.subtract(eye).dot(dir));
        return eye.add(dir.scale(t)).y;
    }

    /** 是否精确命中那条分割线。 */
    public static boolean onWeakLine(ServerPlayer p, LivingEntity target) {
        return Math.abs(aimY(p, target) - weakY(target)) <= tolerance(p, target);
    }

    // ------------------------------------------------------------ 暴击
    /** 在 LivingHurtEvent 里把这一下改成弱点暴击：伤害 ×3 + 临时削掉 70% 护甲。 */
    public static void amplify(ServerPlayer p, LivingEntity target, net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        event.setAmount(event.getAmount() * (float) CRIT_MULTIPLIER);
        stripArmor(target);
        finishCrit(p, target);
    }

    /** 隙：自己结算一次全额的弱点暴击。 */
    public static void directCrit(ServerPlayer p, LivingEntity target, double base) {
        stripArmor(target);
        boolean previous = critting;
        critting = true;
        try {
            AttackTypes.mark(AttackTypes.MAGIC);
            CombatSettings.hurt(p, target, p.damageSources().playerAttack(p),
                (float) (CombatSettings.casterDamage(p, base) * CRIT_MULTIPLIER));
        } finally {
            AttackTypes.clear();
            critting = previous;
        }
        finishCrit(p, target);
    }

    public static boolean critting() { return critting; }

    private static void finishCrit(ServerPlayer p, LivingEntity target) {
        unmark(target);
        cooldown(p, AXIS_COOLDOWN);
        if (target.level() instanceof ServerLevel level) {
            Network.send(level, new Network.Effect(NanamiFx.CRIT, p.getId(),
                new Vec3(target.getX(), weakY(target), target.getZ()), Vec3.ZERO, 1f, 12, 0));
            level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.85f, 1.9f);
            level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_HIT,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.7f);
        }
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("7:3 · 弱点暴击 ×"
            + (int) CRIT_MULTIPLIER + "（无视 70% 防御）"), true);
    }

    /** 把目标护甲临时压到 30%，下一次结算后（2 刻内）恢复。 */
    private static void stripArmor(LivingEntity target) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        double cut = armor.getValue() * ARMOR_IGNORE;
        if (cut <= 0) return;
        armor.removeModifier(STRIP_ID);
        armor.addTransientModifier(new AttributeModifier(STRIP_ID, "NanamiRatioStrip", -cut,
            AttributeModifier.Operation.ADDITION));
        STRIPPED.put(target.getUUID(), new Strip(target, now(target) + 2));
    }

    /** 到点把护甲修饰拆掉（世界 tick 调）。 */
    public static void stripExpire(long now) {
        if (STRIPPED.isEmpty()) return;
        var it = STRIPPED.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue().until() > now) continue;
            AttributeInstance armor = entry.getValue().target().getAttribute(Attributes.ARMOR);
            if (armor != null) armor.removeModifier(STRIP_ID);
            it.remove();
        }
    }

    // ------------------------------------------------------------ 眩晕
    public static void stun(LivingEntity target, int ticks) {
        STUNNED.put(target.getUUID(), now(target) + ticks);
        sync(target, markedByAny(target));
    }

    public static boolean stunned(LivingEntity target) {
        Long until = STUNNED.get(target.getUUID());
        return until != null && until > now(target);
    }

    private static boolean markedByAny(LivingEntity target) {
        CompoundTag tag = target.getPersistentData();
        return now(target) < tag.getLong(MARK_UNTIL);
    }

    // ------------------------------------------------------------ 加班
    public static boolean overtime(ServerPlayer p) {
        return Curse.data(p).getLong(OVERTIME_UNTIL) > p.level().getGameTime();
    }

    public static void startOvertime(ServerPlayer p, int ticks) {
        CompoundTag data = Curse.data(p);
        data.putLong(OVERTIME_UNTIL, p.level().getGameTime() + ticks);
        data.putBoolean(OVERTIME_KILL, false);
    }

    public static void stopOvertime(ServerPlayer p) {
        Curse.data(p).remove(OVERTIME_UNTIL);
        Curse.data(p).remove(OVERTIME_KILL);
    }

    /** 加班期间击杀一次 → 停止扣血。 */
    public static void overtimeKill(ServerPlayer p) {
        if (!overtime(p)) return;
        if (Curse.data(p).getBoolean(OVERTIME_KILL)) return;
        Curse.data(p).putBoolean(OVERTIME_KILL, true);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("加班：击杀了目标，停止扣血"), true);
    }

    public static boolean overtimeDraining(ServerPlayer p) {
        return overtime(p) && !Curse.data(p).getBoolean(OVERTIME_KILL);
    }

    // ------------------------------------------------------------ 守护窗口
    public static void guard(ServerPlayer p, int ticks) {
        Curse.data(p).putLong(GUARD_UNTIL, p.level().getGameTime() + ticks);
    }

    public static boolean guarding(ServerPlayer p) {
        return Curse.data(p).getLong(GUARD_UNTIL) > p.level().getGameTime();
    }

    public static void clearGuard(ServerPlayer p) { Curse.data(p).remove(GUARD_UNTIL); }

    // ------------------------------------------------------------ 冷却 / 找目标
    public static boolean axisReady(ServerPlayer p) {
        return Curse.data(p).getLong(AXIS_READY) <= p.level().getGameTime();
    }

    public static void cooldown(ServerPlayer p, int ticks) {
        Curse.data(p).putLong(AXIS_READY, p.level().getGameTime() + ticks);
    }

    public static int cooldownLeft(ServerPlayer p) {
        return (int) Math.max(0, Curse.data(p).getLong(AXIS_READY) - p.level().getGameTime());
    }

    /** 半径内"最靠近准星"的敌人。 */
    public static LivingEntity nearestToCrosshair(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (var target : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range),
                e -> e != p && e.isAlive() && !e.isSpectator() && CombatSettings.canHit(p, e))) {
            Vec3 delta = target.getBoundingBox().getCenter().subtract(eye);
            double distance = delta.length();
            if (distance > range || distance < 0.4) continue;
            double dot = delta.scale(1 / distance).dot(look);
            if (dot < 0.2) continue;
            if (dot * 100 - distance > bestScore) { bestScore = dot * 100 - distance; best = target; }
        }
        return best;
    }

    /** 关服：清掉全局的眩晕/护甲削减表。 */
    public static void reset() {
        STUNNED.clear();
        STRIPPED.clear();
    }

    public static void testReset(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        data.remove(AXIS_READY);
        data.remove(OVERTIME_UNTIL);
        data.remove(OVERTIME_KILL);
        data.remove(GUARD_UNTIL);
        STUNNED.clear();
    }

    private NanamiRatio() {}
}
