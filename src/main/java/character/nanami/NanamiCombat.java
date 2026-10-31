package character.nanami;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 七海建人 · 十划咒法。
 *
 * <p>被动：半径 15 格内"最靠近准星"的敌人身上会缓慢浮现 7:3 数轴（10 段、第 7 段金色），
 * 冷却 10 秒；用刀/技能精确命中 70% 高度那条分割线 → 无视 70% 防御、伤害 ×3。
 *
 * <p>轮盘：1 强打（扇形 + 强制上弱点）/ 2 瓦落瓦落（1.5 秒完美格挡 + 反击眩晕）/ 3 隙（细线狙击，有弱点必暴击）/ 4 加班（攻速移速 buff、弱点放大、每秒掉 1% 血）。
 */
public final class NanamiCombat {
    public static final int VALOR_GUARD_TICKS = 30;   // 1.5 秒架刀
    public static final int VALOR_STUN = 40;          // 反击眩晕 2 秒
    public static final double VALOR_COUNTER = 30, VALOR_KNOCKBACK = 1.8;
    public static final double SMASH_DAMAGE = 20, SMASH_RANGE = 5, SMASH_HALF_ANGLE = 75;
    public static final double GAP_DAMAGE = 10, GAP_RANGE = 32;
    public static final int OVERTIME_TICKS = 200;     // 10 秒
    public static final double OVERTIME_ATTACK_SPEED = 0.50, OVERTIME_SPEED = 0.30;
    public static final double OVERTIME_DRAIN = 0.01; // 每秒 1% 最大生命

    private static final String OVERTIME_UNTIL = "NanamiOvertimeUntil";
    private static final UUID SPEED_ID = UUID.fromString("7a11c0de-2600-4a4a-9c9c-000000000002");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("7a11c0de-2600-4a4a-9c9c-000000000003");

    public static boolean isNanami(Player p) {
        var d = character.CharacterService.selected(p);
        return d != null && "nanami".equals(d.id());
    }

    // ------------------------------------------------------------ 命中 / 格挡
    @SubscribeEvent public static void hit(LivingHurtEvent e) {
        if (e.getAmount() <= 0) return;
        LivingEntity victim = e.getEntity();
        // 瓦落瓦落：架刀窗口内被打 → 完美格挡（不掉血）+ 强力反击
        if (victim instanceof ServerPlayer p && isNanami(p) && NanamiRatio.guarding(p)
            && e.getSource().getEntity() instanceof LivingEntity attacker && attacker != p) {
            NanamiRatio.clearGuard(p);
            e.setCanceled(true);
            counter(p, attacker);
            return;
        }
        if (NanamiRatio.critting()) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !isNanami(p)) return;
        if (victim == p || !CombatSettings.canHit(p, victim)) return;
        if (!NanamiRatio.marked(p, victim)) return;
        if (!NanamiRatio.onWeakLine(p, victim)) return;
        NanamiRatio.amplify(p, victim, e);
    }

    private static void counter(ServerPlayer p, LivingEntity attacker) {
        Vec3 away = attacker.position().subtract(p.position());
        if (away.lengthSqr() < 1e-4) away = p.getLookAngle().scale(-1);
        away = away.normalize().scale(VALOR_KNOCKBACK);
        attacker.setDeltaMovement(away.x, 0.55, away.z);
        attacker.hurtMarked = true;
        AttackTypes.mark(AttackTypes.MAGIC);
        try {
            CombatSettings.hurt(p, attacker, p.damageSources().playerAttack(p),
                CombatSettings.casterDamage(p, VALOR_COUNTER));
        } finally {
            AttackTypes.clear();
        }
        NanamiRatio.stun(attacker, VALOR_STUN);
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, VALOR_STUN, 6, false, false));
        attacker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, VALOR_STUN, 0, false, false));
        if (attacker instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (p.level() instanceof ServerLevel level) {
            Network.send(level, new Network.Effect(NanamiFx.CLOCK, p.getId(), p.position(), Vec3.ZERO, -2f, 14, 0));
            Network.send(level, new Network.Effect(NanamiFx.CRIT, p.getId(),
                attacker.getBoundingBox().getCenter(), Vec3.ZERO, 0.6f, 10, 0));
            level.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.4f);
            level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.9f, 1.6f);
        }
        p.displayClientMessage(Component.literal("瓦落瓦落：完美格挡 → 反击！"), true);
    }

    @SubscribeEvent public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer victim && isNanami(victim)) {
            NanamiRatio.stopOvertime(victim);
            NanamiRatio.clearGuard(victim);
            clearModifiers(victim);
        }
        if (e.getSource().getEntity() instanceof ServerPlayer killer && isNanami(killer)) {
            NanamiRatio.overtimeKill(killer);
        }
    }

    // ------------------------------------------------------------ 每刻
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!isNanami(p)) {
            clearModifiers(p);
            return;
        }
        long now = p.level().getGameTime();
        // ① 被动数轴：冷却好了就在"最靠近准星的敌人"身上浮现 7:3 弱点
        if (now % 10 == 0 && NanamiRatio.axisReady(p)) {
            LivingEntity target = NanamiRatio.nearestToCrosshair(p, NanamiRatio.REACH);
            if (target != null) {
                NanamiRatio.mark(p, target);
                NanamiRatio.cooldown(p, NanamiRatio.AXIS_COOLDOWN);
            }
        }
        // ② 加班：属性 + 每秒 1% 最大生命
        CompoundTag data = Curse.data(p);
        long overtimeUntil = data.getLong(OVERTIME_UNTIL);
        if (overtimeUntil != 0) {
            if (now >= overtimeUntil) {
                NanamiRatio.stopOvertime(p);
                clearOvertimeModifiers(p);
                Network.send(p.serverLevel(), new Network.Effect(NanamiFx.OVERTIME, p.getId(), p.position(),
                    Vec3.ZERO, 0f, 0, 0));
                p.sendSystemMessage(Component.literal("加班结束：属性恢复（社畜也有下班的时候）")
                    .withStyle(ChatFormatting.GRAY));
            } else {
                setModifier(p, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, "NanamiOvertimeAttack", OVERTIME_ATTACK_SPEED);
                setModifier(p, Attributes.MOVEMENT_SPEED, SPEED_ID, "NanamiOvertimeSpeed", OVERTIME_SPEED);
                if (now % 20 == 0 && NanamiRatio.overtimeDraining(p)) {
                    float drain = (float) (p.getMaxHealth() * OVERTIME_DRAIN);
                    p.setHealth(Math.max(1f, p.getHealth() - drain));
                    p.displayClientMessage(Component.literal("加班：-" + String.format(Locale.ROOT, "%.0f", drain)
                        + " 生命（击杀可停）"), true);
                }
            }
        }
    }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.getServer() != null) {
            NanamiRatio.stripExpire(e.getServer().overworld().getGameTime());
        }
    }

    // ------------------------------------------------------------ 轮盘技能
    /** 1 强打：前方扇形伤害 + 强制在所有命中目标身上生成 7:3 弱点。 */
    public static void smash(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Network.send(level, new Network.Effect(NanamiFx.SMASH, p.getId(), p.position(), look,
            (float) SMASH_RANGE, 14, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.9f);
        int hits = 0;
        for (var target : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(SMASH_RANGE),
                t -> t != p && t.isAlive() && !t.isSpectator() && CombatSettings.canHit(p, t))) {
            Vec3 delta = target.getBoundingBox().getCenter().subtract(eye);
            if (delta.lengthSqr() > SMASH_RANGE * SMASH_RANGE || delta.lengthSqr() < 1e-4) continue;
            if (look.dot(delta.normalize()) < Math.cos(Math.toRadians(SMASH_HALF_ANGLE))) continue;
            AttackTypes.mark(AttackTypes.SLASH);
            try {
                CombatSettings.hurt(p, target, p.damageSources().playerAttack(p),
                    CombatSettings.casterDamage(p, SMASH_DAMAGE));
            } finally {
                AttackTypes.clear();
            }
            if (target.isAlive() && CombatSettings.canHit(p, target)) NanamiRatio.mark(p, target);
            hits++;
        }
        p.displayClientMessage(Component.literal(hits > 0 ? "强打：命中 " + hits + " 个目标 · 已生成 7:3 弱点" : "强打"), true);
    }

    /** 2 瓦落瓦落：架刀 1.5 秒，被打就完美格挡 + 反击。 */
    public static void valor(ServerPlayer p) {
        NanamiRatio.guard(p, VALOR_GUARD_TICKS);
        if (p.level() instanceof ServerLevel level) {
            Network.send(level, new Network.Effect(NanamiFx.CLOCK, p.getId(), p.position(), Vec3.ZERO,
                2f, VALOR_GUARD_TICKS, 0));
            level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.8f);
        }
        p.displayClientMessage(Component.literal("瓦落瓦落：架刀 1.5 秒"), true);
    }

    /** 3 隙：极细咒力线；目标有弱点时自动微调轨迹直击分割线（暴击）。 */
    public static void gap(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(GAP_RANGE));
        var blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();
        LivingEntity target = null;
        Vec3 hitAt = null;
        double bestDistance = Double.MAX_VALUE;
        for (var candidate : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, end).inflate(1.2), t -> t != p && t.isAlive() && CombatSettings.canHit(p, t))) {
            var clip = candidate.getBoundingBox().inflate(0.3).clip(eye, end);
            if (clip.isEmpty()) continue;
            double distance = clip.get().distanceToSqr(eye);
            if (distance < bestDistance) { bestDistance = distance; target = candidate; hitAt = clip.get(); }
        }
        if (target == null) {
            Network.send(level, new Network.Effect(NanamiFx.GAP, p.getId(), eye, end, 1f, 10, 0));
            p.displayClientMessage(Component.literal("隙：没有命中目标"), true);
            return;
        }
        boolean crit = NanamiRatio.marked(p, target);
        Vec3 to = crit ? new Vec3(target.getX(), NanamiRatio.weakY(target), target.getZ()) : hitAt;
        Network.send(level, new Network.Effect(NanamiFx.GAP, p.getId(), eye, to, 1f, 10, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8f, 1.6f);
        if (crit) {
            NanamiRatio.directCrit(p, target, NanamiRatio.GAP_CRIT_BASE);
        } else {
            CombatSettings.hurt(p, target, p.damageSources().indirectMagic(p, p),
                CombatSettings.spellDamage(p, GAP_DAMAGE));
            p.displayClientMessage(Component.literal("隙：命中（目标没有弱点，只是小额伤害）"), true);
        }
    }

    /** 4 加班：攻速 +50%、移速 +30%、弱点判定 ×1.5；每秒掉 1% 最大生命，击杀一次即停。 */
    public static void overtime(ServerPlayer p) {
        NanamiRatio.startOvertime(p, OVERTIME_TICKS);
        if (p.level() instanceof ServerLevel level) {
            Network.send(level, new Network.Effect(NanamiFx.OVERTIME, p.getId(), p.position(), Vec3.ZERO,
                1f, OVERTIME_TICKS, 0));
            level.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.9f, 0.7f);
        }
        p.sendSystemMessage(Component.literal("加班（10 秒）：攻速 +50% · 移速 +30% · 弱点判定 ×1.5 · 每秒 -1% 最大生命（击杀可停）")
            .withStyle(ChatFormatting.GOLD));
        p.displayClientMessage(Component.literal("加班"), true);
    }

    /** 创造恢复符：清掉弱点/加班/格挡状态与属性。 */
    public static void testReset(ServerPlayer p) {
        NanamiRatio.testReset(p);
        clearModifiers(p);
    }

    // ------------------------------------------------------------ 属性
    private static void clearOvertimeModifiers(ServerPlayer p) {
        setModifier(p, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, "NanamiOvertimeAttack", 0);
        setModifier(p, Attributes.MOVEMENT_SPEED, SPEED_ID, "NanamiOvertimeSpeed", 0);
    }

    private static void clearModifiers(ServerPlayer p) { clearOvertimeModifiers(p); }

    private static void setModifier(ServerPlayer p, Attribute attribute, UUID id, String name, double amount) {
        AttributeInstance instance = p.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (amount == 0) {
            if (current != null) instance.removeModifier(id);
            return;
        }
        if (current != null && Math.abs(current.getAmount() - amount) < 1e-6) return;
        if (current != null) instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private NanamiCombat() {}
}
