package character.toji;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 伏黑甚尔 20 条连招。触发条件全部是「闪避/后撤/滑铲后 COMBO_TICKS 内的输入序列」，
 * 由 {@link TojiQte} 收集 token、这里按后缀匹配（长的先匹配）。
 *
 * <p>每招都是：判定 → 命中结算（伤害/击退/破甲/眩晕/位移）→ 广播一个 {@link TojiFx#COMBO} 特效
 * （客户端按 size 播对应的自绘动作与冲击特效）。
 */
public final class TojiCombos {
    // 伤害表（关闭/正常模式通用；本角色不吃整合包倍率）
    public static final double C1 = 18, C2A = 14, C2B = 22, C3 = 20, C4 = 40, C5 = 20, C6 = 26, C7 = 30,
        C8 = 26, C9 = 24, C10 = 45, C11A = 20, C11B = 28, C11C = 35, C12 = 26, C13 = 8, C13_LAST = 30,
        C14 = 36, C15 = 32, C16 = 0, C17 = 30, C18 = 40, C19A = 24, C19B = 34, C20 = 0.8;

    /** 匹配并执行。返回是否命中某招。 */
    public static boolean tryMatch(ServerPlayer p) {
        List<Integer> seq = TojiQte.sequence(p);
        if (seq.isEmpty()) return false;
        int size = seq.size();
        int last = seq.get(size - 1);
        // 20：完美闪避 3 次后按 F
        if (last == TojiQte.EXECUTE && TojiQte.perfectCount(p) >= TojiQte.PERFECT_PER_EXECUTE) {
            return done(p, 20) && execute(p);
        }
        // 12：敌人长前摇时提前按 R 投掷咒具（不需要闪避）
        if (last == TojiQte.THROW) return done(p, 12) && intercept(p);
        // 16：装备天逆鉾时右键弹反弹道
        if (last == TojiQte.USE) return done(p, 16) && deflect(p);
        // 19：跳 → 左键飞踢 → 空中闪避 → 左键下砸
        if (size >= 4 && ends(seq, TojiQte.ATTACK, TojiQte.AIR_DODGE, TojiQte.ATTACK)) {
            return done(p, 19) && aerialCombo(p);
        }
        // 13：闪避后左键连点 8 次
        if (size >= 9 && tailAll(seq, 8, TojiQte.ATTACK) && has(seq, TojiQte.DODGE)) {
            return done(p, 13) && flurry(p);
        }
        // 11：闪避 → 天逆鉾 → 左键 → 游云 → 左键 → 右键
        if (size >= 6 && ends(seq, TojiQte.DODGE, TojiQte.WEAPON_TENKA, TojiQte.ATTACK,
                TojiQte.WEAPON_YUYUN, TojiQte.ATTACK, TojiQte.USE)) {
            return done(p, 11) && tripleExecute(p);
        }
        // 10：闪避 → 游云 → 长按蓄力
        if (size >= 3 && ends(seq, TojiQte.DODGE, TojiQte.WEAPON_YUYUN, TojiQte.CHARGE_ATTACK)) {
            return done(p, 10) && chargedSmash(p);
        }
        // 7 / 18：闪避 → 游云 → 左键（目标在冲刺 → 18，否则 7）
        if (size >= 3 && ends(seq, TojiQte.DODGE, TojiQte.WEAPON_YUYUN, TojiQte.ATTACK)) {
            LivingEntity target = TojiQte.approach(p, 4.5);
            boolean charging = target != null && target.getDeltaMovement().horizontalDistanceSqr() > 0.06
                && target.getDeltaMovement().multiply(1, 0, 1).normalize()
                .dot(p.position().subtract(target.position()).multiply(1, 0, 1).normalize()) > 0.6;
            if (charging) return done(p, 18) && bat(p, target);
            return done(p, 7) && kneeStrike(p, target);
        }
        // 6：天逆鉾砍有增益/护盾的目标（不需要闪避）
        if (last == TojiQte.ATTACK && size >= 2 && seq.get(size - 2) == TojiQte.WEAPON_TENKA) {
            return done(p, 6) && breakMagic(p);
        }
        // 17：闪避 → E 缴械
        if (size >= 2 && ends(seq, TojiQte.DODGE, TojiQte.DISARM)) return done(p, 17) && disarm(p);
        // 8：按 G 抓取 → 左键
        if (size >= 2 && ends(seq, TojiQte.GRAB, TojiQte.ATTACK)) return done(p, 8) && hostage(p);
        // 4：闪避 → Shift+左键 过肩摔
        if (size >= 2 && ends(seq, TojiQte.DODGE, TojiQte.SHIFT_ATTACK)) return done(p, 4) && suplex(p);
        // 5：Ctrl 滑铲 → 左键
        if (size >= 2 && ends(seq, TojiQte.SLIDE, TojiQte.ATTACK)) return done(p, 5) && slideKick(p);
        // 3：Shift+空格后撤 → 左键
        if (size >= 2 && ends(seq, TojiQte.BACKSTEP, TojiQte.ATTACK)) return done(p, 3) && flyingKick(p);
        // 9：空中闪避 → 左键 踩头处决
        if (size >= 2 && ends(seq, TojiQte.AIR_DODGE, TojiQte.ATTACK)) return done(p, 9) && stomp(p);
        // 2：闪避 → 左键 → 左键（快速两连，必须排在最基础的反击之前）
        if (size >= 3 && ends(seq, TojiQte.DODGE, TojiQte.ATTACK, TojiQte.ATTACK)) return done(p, 2) && boneKnee(p);
        // 14：闪避 → 左键，且目标背后 2 格是墙
        if (size >= 2 && ends(seq, TojiQte.DODGE, TojiQte.ATTACK)) {
            LivingEntity target = TojiQte.approach(p, 4.0);
            if (target != null && wallBehind(p, target)) return done(p, 14) && wallSlam(p, target);
            return done(p, 1) && counter(p);
        }
        // 15：连续两次空中闪避（蹬墙）后左键
        if (size >= 3 && ends(seq, TojiQte.JUMP, TojiQte.JUMP, TojiQte.ATTACK)) return done(p, 15) && parkourKick(p);
        return false;
    }

    // ── 1~5 入门 ─────────────────────────────────────────
    private static boolean counter(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 4.5);
        if (target == null) { miss(p, 1); return false; }
        hit(p, target, C1, 1.2, 0, 0);
        fx(p, 1, target.position());
        say(p, "闪避反击");
        return true;
    }

    private static boolean boneKnee(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 4.0);
        if (target == null) return false;
        hit(p, target, C2A, 0.3, 0, 6);
        hit(p, target, C2B, 0.8, 0.2, 10);   // 第二段破甲 20%
        fx(p, 2, target.position());
        say(p, "裂骨膝撞 · 破甲 20%");
        return true;
    }

    private static boolean flyingKick(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 4.5);
        if (target == null) return false;
        hit(p, target, C3, 2.4, 0, 10);
        TojiQte.stun(target, 10);
        fx(p, 3, target.position());
        say(p, "飞踢拉扯 · 打断");
        return true;
    }

    private static boolean suplex(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 2.6);
        if (target == null) return false;
        if (target.getBbWidth() > p.getBbWidth() + 0.35) {
            p.displayClientMessage(Component.literal("体型太大，摔不动").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        hit(p, target, C4, 0.2, 0, 40);
        TojiQte.stun(target, 40);
        target.setDeltaMovement(0, -0.6, 0);
        TojiQte.dust(p.serverLevel(), target.position(), 2.2);
        fx(p, 4, target.position());
        say(p, "过肩重摔 · 眩晕 2 秒");
        return true;
    }

    private static boolean slideKick(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 3.5);
        if (target == null) return false;
        hit(p, target, C5, 0.4, 0, 30);
        TojiQte.stun(target, 30);
        fx(p, 5, target.position());
        say(p, "滑铲切足 · 硬直 1.5 秒");
        return true;
    }

    // ── 6~10 进阶 ────────────────────────────────────────
    private static boolean breakMagic(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 4.5);
        if (target == null) return false;
        target.getActiveEffects().stream().filter(effect -> effect.getEffect().isBeneficial()).toList()
            .forEach(effect -> target.removeEffect(effect.getEffect()));
        hit(p, target, C6, 0.6, 0.5, 10);
        fx(p, 6, target.position());
        say(p, "天逆鉾·破魔");
        return true;
    }

    private static boolean kneeStrike(ServerPlayer p, LivingEntity target) {
        if (target == null) return false;
        hit(p, target, C7, 0.7, 0, 8);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        fx(p, 7, target.position());
        say(p, "游云·破膝击");
        return true;
    }

    private static boolean hostage(ServerPlayer p) {
        LivingEntity shield = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity living : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(p.position(), p.position()).inflate(3.0),
                t -> t != p && t.isAlive() && CombatSettings.canHit(p, t))) {
            double d = living.distanceToSqr(p);
            if (d < best) { best = d; shield = living; }
        }
        CompoundTagHolder.hold(p, shield);            // 记住"这一下由它替我吃"
        Curse.data(p).putLong(TojiQte.K_IFRAME, TojiQte.now(p) + 20);
        Curse.data(p).putLong(TojiQte.K_NEXT, TojiQte.now(p) + 40);
        Curse.data(p).putDouble(TojiQte.K_MULT, 2.0);
        LivingEntity target = TojiQte.approach(p, 4.0);
        if (target != null) hit(p, target, C8, 1.0, 0, 8);
        fx(p, 8, p.position());
        say(p, shield != null ? "夺命挡箭牌 · 替死鬼就位" : "夺命挡箭牌（附近没有可抓的东西）");
        return true;
    }

    private static boolean stomp(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 4.0);
        if (target == null) return false;
        hit(p, target, C9, 0.6, 0, 40);
        target.setDeltaMovement(0, -0.4, 0);
        p.fallDistance = 0;
        Curse.data(p).putBoolean("TojiAirDodged", false);
        fx(p, 9, target.position());
        say(p, "踩头处决 · 击倒");
        return true;
    }

    private static boolean chargedSmash(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 4.5);
        if (target == null) return false;
        hit(p, target, C10, 1.4, 0.5, 20);
        for (LivingEntity near : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(target.position(), target.position()).inflate(2.0),
                t -> t != p && t != target && t.isAlive() && CombatSettings.canHit(p, t))) {
            hit(p, near, C10 * 0.5, 1.0, 0, 10);
        }
        TojiQte.dust(p.serverLevel(), target.position(), 3.0);
        fx(p, 10, target.position());
        say(p, "蓄力破甲 · 无视 50% 护甲");
        return true;
    }

    // ── 11~15 精通 ────────────────────────────────────────
    private static boolean tripleExecute(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 4.5);
        if (target == null) return false;
        Curse.data(p).putLong(TojiQte.K_IFRAME, TojiQte.now(p) + 30);
        hit(p, target, C11A, 0.2, 0.4, 10);
        hit(p, target, C11B, 0.6, 0, 14);
        hit(p, target, C11C, 1.8, 0, 20);
        fx(p, 11, target.position());
        say(p, "极速切武·三连处决");
        return true;
    }

    private static boolean intercept(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 12.0);
        if (target == null) return false;
        hit(p, target, C12, 1.2, 0, 20);
        TojiQte.stun(target, 20);
        fx(p, 12, target.position());
        say(p, "先之先·截击");
        return true;
    }

    private static boolean flurry(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 4.0);
        if (target == null) return false;
        for (int i = 0; i < 8; i++) hit(p, target, C13, 0.05, 0, 2);
        hit(p, target, C13_LAST, 1.6, 0, 14);
        target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45, 0));
        fx(p, 13, target.position());
        say(p, "游云·浮空乱打");
        return true;
    }

    private static boolean wallSlam(ServerPlayer p, LivingEntity target) {
        hit(p, target, C14, 1.0, 1.0, 40);      // 无视防御
        TojiQte.stun(target, 40);
        TojiQte.dust(p.serverLevel(), target.position(), 2.0);
        fx(p, 14, target.position());
        say(p, "墙壁反杀 · 脑震荡");
        return true;
    }

    private static boolean parkourKick(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 10.0);
        if (target == null) return false;
        Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1.2, 0, 1.2));
        p.teleportTo(behind.x, p.position().y, behind.z);
        p.hurtMarked = true;
        hit(p, target, C15, 2.6, 0, 20);
        fx(p, 15, target.position());
        say(p, "跑酷踩头 · 蹬墙处决");
        return true;
    }

    // ── 16~20 大师 ────────────────────────────────────────
    private static boolean deflect(ServerPlayer p) {
        Projectile nearest = null;
        double best = Double.MAX_VALUE;
        for (Projectile projectile : p.serverLevel().getEntitiesOfClass(Projectile.class,
                new AABB(p.position(), p.position()).inflate(5.0), t -> t.isAlive() && t.getOwner() != p)) {
            double d = projectile.distanceToSqr(p);
            if (d < best) { best = d; nearest = projectile; }
        }
        if (nearest == null) return false;
        Vec3 back = nearest.getDeltaMovement().scale(-2.0);
        nearest.setDeltaMovement(back);
        nearest.hurtMarked = true;
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.8f);
        fx(p, 16, p.getEyePosition());
        say(p, "刀反 · 弹道翻倍返还");
        return true;
    }

    private static boolean disarm(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 3.0);
        if (target == null) return false;
        var hand = target.getMainHandItem();
        if (!hand.isEmpty()) {
            target.spawnAtLocation(hand.copy());
            target.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        }
        hit(p, target, C17, 1.4, 0, 16);
        fx(p, 17, target.position());
        say(p, "夺刃反杀");
        return true;
    }

    private static boolean bat(ServerPlayer p, LivingEntity target) {
        if (target == null) return false;
        hit(p, target, C18, 0, 0, 16);
        Vec3 back = target.getDeltaMovement().scale(-1.6);
        target.setDeltaMovement(back.x, Math.max(0.3, back.y), back.z);
        target.hurtMarked = true;
        fx(p, 18, target.position());
        say(p, "游云·打棒球");
        return true;
    }

    private static boolean aerialCombo(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 4.5);
        if (target == null) return false;
        hit(p, target, C19A, 0.4, 0, 6);
        hit(p, target, C19B, 0.6, 0, 12);
        target.setDeltaMovement(0, -0.9, 0);      // 击坠
        target.hurtMarked = true;
        fx(p, 19, target.position());
        say(p, "空中体操 · 击坠");
        return true;
    }

    private static boolean execute(ServerPlayer p) {
        LivingEntity target = TojiQte.approach(p, 3.5);
        if (target == null) return false;
        double amount = Math.max(1, target.getMaxHealth() * C20);
        Curse.data(p).putInt(TojiQte.K_COUNT, 0);
        hit(p, target, amount, 1.2, 1.0, 60);
        TojiQte.data(target).putLong("TojiNoHeal", TojiQte.now(target) + 200);
        target.setHealth(Math.min(target.getHealth(), Math.max(0.5f, target.getHealth() * 0.05f)));
        fx(p, 20, target.position());
        say(p, "暴君处决 · 封回复");
        return true;
    }

    // ── 结算/特效工具 ────────────────────────────────────
    /** 没目标时也给反馈，避免"按了没反应"。 */
    private static void miss(ServerPlayer p, int combo) {
        p.displayClientMessage(Component.literal("连招 " + combo + "：附近没有可打的目标")
            .withStyle(ChatFormatting.GRAY), true);
        com.mojang.logging.LogUtils.getLogger().info("[甚尔] 连招 {} 没找到目标", combo);
    }

    private static boolean done(ServerPlayer p, int combo) {
        com.mojang.logging.LogUtils.getLogger().info("[甚尔] 触发连招 {}", combo);
        TojiQte.resetSequence(p);
        return true;
    }

    private static void say(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.RED), true);
    }

    private static void fx(ServerPlayer p, int combo, Vec3 at) {
        Network.send(p.serverLevel(), new Network.Effect(TojiFx.COMBO, p.getId(), at, p.getLookAngle(),
            combo, 14, 0));
    }

    /** 命中结算：伤害 ×完美闪避倍率，可选破甲（按当前护甲的比例）。 */
    public static void hit(ServerPlayer p, LivingEntity target, double damage, double knockback,
                           double armorIgnore, int stun) {
        if (!CombatSettings.canHit(p, target)) return;
        double mult = TojiQte.consumeNextHit(p);
        double amount = damage * mult;
        if (armorIgnore > 0) armorBreak(target, armorIgnore, 60);
        CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), (float) amount);
        if (knockback > 0) {
            Vec3 push = target.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(knockback);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, Math.max(0.15, push.y + 0.3), push.z));
            target.hurtMarked = true;
        }
        if (stun > 0) TojiQte.stun(target, stun);
        p.serverLevel().playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.9f);
    }

    /** 临时削减护甲（比例）。 */
    public static void armorBreak(LivingEntity target, double fraction, int ticks) {
        var attribute = target.getAttribute(Attributes.ARMOR);
        if (attribute == null) return;
        var id = java.util.UUID.nameUUIDFromBytes(("TojiArmorBreak" + target.getUUID()).getBytes());
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, "破甲",
            -Math.max(0, attribute.getValue()) * fraction, AttributeModifier.Operation.ADDITION));
    }

    private static boolean wallBehind(ServerPlayer p, LivingEntity target) {
        Vec3 away = target.position().subtract(p.position()).multiply(1, 0, 1).normalize();
        var pos = net.minecraft.core.BlockPos.containing(target.position().add(away.scale(1.2)));
        for (int i = 0; i < 2; i++) {
            var at = pos.above(i);
            if (!p.serverLevel().getBlockState(at).getCollisionShape(p.serverLevel(), at).isEmpty()) return true;
        }
        return false;
    }

    // 小工具：后缀/包含判断
    private static boolean ends(List<Integer> seq, int... tail) {
        if (seq.size() < tail.length) return false;
        for (int i = 0; i < tail.length; i++) {
            if (seq.get(seq.size() - tail.length + i) != tail[i]) return false;
        }
        return true;
    }

    private static boolean has(List<Integer> seq, int token) {
        return seq.contains(token);
    }

    private static boolean tailAll(List<Integer> seq, int count, int token) {
        if (seq.size() < count + 1) return false;
        for (int i = seq.size() - count; i < seq.size(); i++) if (seq.get(i) != token) return false;
        return true;
    }

    private TojiCombos() {}

    /** 替身（夺命挡箭牌）的短生命周期记录。 */
    static final class CompoundTagHolder {
        static void hold(ServerPlayer p, LivingEntity shield) {
            Curse.data(p).putInt("TojiHostage", shield == null ? -1 : shield.getId());
        }
    }
}
