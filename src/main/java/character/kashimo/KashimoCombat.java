package character.kashimo;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 鹿紫云一 · 幻兽琥珀：电荷（挂电 / 引爆 / 必中落雷）与终奥（幻兽·琥珀 / 肉体崩溃）。
 *
 * <p>核心三条：
 * ①任何攻击命中 → 目标 +1 层正电荷（附着期间 +2），自身 +1 层负电荷；
 * ②引爆 = 无视距离/地形/闪避，所有被自己挂了电荷的敌人各按层数吃落雷，并清空负电荷；
 * ③终奥 15 秒免疫物理、双倍移速攻速、普攻自带落雷与扇形电磁波，结束后 60 秒肉体崩溃。
 */
public final class KashimoCombat {
    /** 终奥技能号：不进轮盘格，由轮盘中心点击后以 phase=7 直接请求。 */
    public static final int ULT = 2405;

    public static final int ATTACH_TICKS = 200;        // 雷神·附着 10 秒
    public static final int ULT_TICKS = 300;           // 幻兽·琥珀 15 秒
    public static final int CRASH_TICKS = 1200;        // 肉体崩溃 60 秒
    public static final double ULT_TRIGGER_HP = 0.30;  // 生命低于 30% 自动触发
    public static final double NEG_SPEED = 0.004;      // 每层负电荷 +0.4% 移速（20 层 = +8%）
    public static final double DASH_DISTANCE = 10;     // 雷走突进距离（格）
    public static final double DASH_LAND_RADIUS = 3;   // 雷走落点雷击半径
    public static final double DISCHARGE_RADIUS = 6;   // 放电半径
    public static final double ATTRACT_RANGE = 24;     // 电磁吸附搜索半径
    public static final double ATTRACT_PULL = 1.8;     // 拉到自己面前的距离
    public static final int FAN_RANGE = 5;             // 琥珀：电磁波扇形半径
    public static final int FAN_HALF_ANGLE = 60;       // 琥珀：扇形半角（合计 120°）
    public static final int FAN_MAX = 8;               // 琥珀：一次扇形最多命中几个

    private static final String ATTACH_UNTIL = "KashimoAttachUntil", ULT_UNTIL = "KashimoUltUntil";
    private static final String CRASH_UNTIL = "KashimoCrashUntil", ULT_USED = "KashimoUltUsed";
    private static final String SKILL_COOLDOWN = "CharacterSkillCooldown_";

    private static final UUID SPEED_ID = UUID.fromString("6a1e5c20-2400-4a4a-9c9c-000000000001");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("6a1e5c20-2400-4a4a-9c9c-000000000002");
    private static final UUID DAMAGE_ID = UUID.fromString("6a1e5c20-2400-4a4a-9c9c-000000000003");

    /** 落雷结算期间为 true：避免「雷击命中 → 再挂电 → 再落雷」的递归。 */
    private static boolean striking;

    public static boolean isKashimo(Player p) {
        var d = character.CharacterService.selected(p);
        return d != null && "kashimo".equals(d.id());
    }

    private static CompoundTag data(ServerPlayer p) { return Curse.data(p); }

    public static boolean attachActive(ServerPlayer p) { return data(p).getLong(ATTACH_UNTIL) > p.level().getGameTime(); }
    public static boolean ultimate(ServerPlayer p) { return data(p).getLong(ULT_UNTIL) > p.level().getGameTime(); }
    public static boolean crashing(ServerPlayer p) { return data(p).getLong(CRASH_UNTIL) > p.level().getGameTime(); }

    // ------------------------------------------------------------ 事件：命中挂电
    @SubscribeEvent public static void hit(LivingHurtEvent e) {
        if (e.getAmount() <= 0) return;
        // 幻兽·琥珀：肉体已转化为能量，免疫一切非机制伤害。
        if (e.getEntity() instanceof ServerPlayer victim && ultimate(victim) && !unavoidable(e.getSource())) {
            e.setCanceled(true);
            return;
        }
        if (striking) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !isKashimo(p)) return;
        LivingEntity target = e.getEntity();
        if (target == p || !CombatSettings.canHit(p, target)) return;
        KashimoCharges.apply(p, target, attachActive(p) ? 2 : 1);
        KashimoCharges.addNegative(p, 1);
        if (ultimate(p)) strike(p, target, KashimoSkills.ULT_BOLT_DAMAGE);
    }

    /** 物理伤害豁免里的例外：掉出世界与 /kill 这类机制伤害照常生效。 */
    private static boolean unavoidable(DamageSource source) {
        // /kill 与掉出世界都属于 bypasses_invulnerability，机制伤害不能被「免疫物理」吃掉
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /** 必中落雷：不看距离、不看地形、不看闪避，直接结算一次雷电伤害。 */
    public static boolean strike(ServerPlayer owner, LivingEntity target, double base) {
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive()) return false;
        if (!CombatSettings.canHit(owner, target)) return false;
        boolean previous = striking;
        striking = true;
        try {
            AttackTypes.mark(AttackTypes.LIGHTNING);
            CombatSettings.hurt(owner, target, owner.damageSources().indirectMagic(owner, owner),
                CombatSettings.spellDamage(owner, base));
        } finally {
            AttackTypes.clear();
            striking = previous;
        }
        Network.send(level, new Network.Effect(KashimoFx.STRIKE, target.getId(), target.position(),
            Vec3.ZERO, 1f, 20 * 15, owner.getId()));
        return true;
    }

    /** 琥珀期间的电磁波形态：普攻顺带劈中前方扇形里的其他敌人。 */
    private static void fan(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (level == null) return;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        int hits = 0;
        for (var t : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(FAN_RANGE),
                t -> t != p && t.isAlive() && !t.isSpectator() && CombatSettings.canHit(p, t))) {
            Vec3 d = t.getBoundingBox().getCenter().subtract(eye);
            if (d.lengthSqr() > FAN_RANGE * FAN_RANGE || d.lengthSqr() < 1e-4) continue;
            if (look.dot(d.normalize()) < Math.cos(Math.toRadians(FAN_HALF_ANGLE))) continue;
            strike(p, t, KashimoSkills.ULT_BOLT_DAMAGE);
            if (++hits >= FAN_MAX) break;
        }
    }

    // ------------------------------------------------------------ 每刻
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!isKashimo(p)) {
            // 切走角色：把本角色的属性修饰全部拆掉（数据本身留在角色快照里）。
            clearModifiers(p);
            return;
        }
        long now = p.level().getGameTime();
        CompoundTag data = data(p);

        // ① 终奥到期 → 肉体崩溃
        long ultUntil = data.getLong(ULT_UNTIL);
        if (ultUntil != 0 && now >= ultUntil) {
            data.remove(ULT_UNTIL);
            clearUltModifiers(p);
            Network.send(p.serverLevel(), new Network.Effect(KashimoFx.ULT, p.getId(), p.position(),
                Vec3.ZERO, 0f, 0, 0));
            startCrash(p);
        }
        // ② 崩溃到期 → 解除
        long crashUntil = data.getLong(CRASH_UNTIL);
        if (crashUntil != 0 && now >= crashUntil) {
            data.remove(CRASH_UNTIL);
            clearCrashModifiers(p);
            Network.send(p.serverLevel(), new Network.Effect(KashimoFx.CRASH, p.getId(), p.position(),
                Vec3.ZERO, 0f, 0, 0));
            p.sendSystemMessage(Component.literal("肉体崩溃结束：生命与移速恢复（技能仍处于冷却）")
                .withStyle(ChatFormatting.AQUA));
        }
        // ③ 形态维持
        if (ultimate(p)) {
            applyUltModifiers(p);
        } else if (crashing(p)) {
            applyCrashModifiers(p);
            if (p.getHealth() > 1f) p.setHealth(1f);
        }
        // ④ 低血量自动触发终奥
        if (!ultimate(p) && !crashing(p) && p.getHealth() <= p.getMaxHealth() * ULT_TRIGGER_HP
            && usableOnce(p)) {
            startUltimate(p, false);
        }
        // ④' 琥珀的电磁波形态：每次普攻（含挥空）把前方扇形里的敌人各劈一道雷
        if (ultimate(p) && p.swinging && p.getAttackStrengthScale(0.5f) >= 0.95f
            && now - data.getLong("KashimoFanAt") >= 8) {
            data.putLong("KashimoFanAt", now);
            fan(p);
        }
        // ⑤ 负电荷：久未命中开始衰减；移速加成始终跟着层数
        int neg = KashimoCharges.negative(p);
        if (neg > 0 && now - data.getLong(KashimoCharges.NEG_AT) >= KashimoCharges.NEG_IDLE_TICKS
            && now % 20 == 0) {
            KashimoCharges.setNegative(p, neg - 1);
            neg = KashimoCharges.negative(p);
        }
        double speedBonus = (ultimate(p) ? 1.0 : 0.0) + (crashing(p) ? -0.5 : 0.0) + neg * NEG_SPEED;
        setModifier(p, Attributes.MOVEMENT_SPEED, SPEED_ID, "KashimoChargeSpeed", speedBonus);
        // 定时补发一次负电荷（切角色回来/重登后 HUD 才有值）
        if (now % 20 == 0) {
            KashimoCharges.prune();
            if (neg > 0) KashimoCharges.syncNegative(p);
        }
    }

    /** 攻击被禁（肉体崩溃）：连普攻一起禁。 */
    @SubscribeEvent public static void attack(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !isKashimo(p)) return;
        if (crashing(p)) {
            e.setCanceled(true);
            p.displayClientMessage(Component.literal("肉体崩溃中：无法攻击"), true);
        }
    }

    @SubscribeEvent public static void death(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag data = data(p);
        data.remove(ULT_UNTIL);
        data.remove(CRASH_UNTIL);
        data.remove(ATTACH_UNTIL);
        data.putBoolean(ULT_USED, false);
        data.putInt(KashimoCharges.NEG, 0);
        clearModifiers(p);
    }

    // ------------------------------------------------------------ 轮盘技能
    /** 1 雷走：向前突进 10 格，落点小范围落雷并挂正电荷。 */
    public static void dash(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 start = p.position(), eye = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        Vec3 end = start.add(flat.scale(DASH_DISTANCE));
        BlockHitResult wall = level.clip(new ClipContext(eye, eye.add(flat.scale(DASH_DISTANCE + 1)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) {
            Vec3 hit = wall.getLocation().subtract(flat.scale(0.8));
            end = new Vec3(hit.x, start.y, hit.z);
        }
        // 落点贴地：脚下 3 格内找地面，避免卡进方块或悬空。
        end = snapGround(level, p, end);
        Network.send(level, new Network.Effect(KashimoFx.DASH, p.getId(), start, end, 1f, 14, 0));
        p.teleportTo(end.x, end.y, end.z);
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1f, 1.35f);
        Network.send(level, new Network.Effect(KashimoFx.DISCHARGE, p.getId(), end, Vec3.ZERO,
            (float) DASH_LAND_RADIUS, 12, 0));
        int hits = 0;
        for (var t : level.getEntitiesOfClass(LivingEntity.class, new AABB(end, end).inflate(DASH_LAND_RADIUS),
                t -> t != p && t.isAlive() && !t.isSpectator() && CombatSettings.canHit(p, t))) {
            if (t.position().distanceToSqr(end) > DASH_LAND_RADIUS * DASH_LAND_RADIUS) continue;
            // 落点雷击走 strike（必中），因此挂电要显式补一次。
            if (strike(p, t, KashimoSkills.DASH_LAND_DAMAGE)) KashimoCharges.apply(p, t, 1);
            hits++;
        }
        level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.55f, 1.5f);
        p.displayClientMessage(Component.literal(hits > 0 ? "雷走：命中 " + hits + " 个目标" : "雷走"), true);
    }

    private static Vec3 snapGround(ServerLevel level, Player p, Vec3 at) {
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(at.x, at.y, at.z);
        for (int dy = 1; dy >= -3; dy--) {
            var below = pos.offset(0, dy - 1, 0);
            var at2 = pos.offset(0, dy, 0);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                && level.getBlockState(at2).getCollisionShape(level, at2).isEmpty()) {
                return new Vec3(at.x, at2.getY(), at.z);
            }
        }
        return at;
    }

    /** 2 放电：以自身为中心 6 格环形电流。 */
    public static void discharge(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Network.send(level, new Network.Effect(KashimoFx.DISCHARGE, p.getId(), p.position(), Vec3.ZERO,
            (float) DISCHARGE_RADIUS, 14, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.7f, 1.7f);
        int hits = 0;
        for (var t : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(DISCHARGE_RADIUS),
                t -> t != p && t.isAlive() && !t.isSpectator() && CombatSettings.canHit(p, t))) {
            if (t.distanceToSqr(p) > DISCHARGE_RADIUS * DISCHARGE_RADIUS) continue;
            AttackTypes.mark(AttackTypes.LIGHTNING);
            try {
                CombatSettings.hurt(p, t, p.damageSources().indirectMagic(p, p),
                    CombatSettings.spellDamage(p, KashimoSkills.DISCHARGE_DAMAGE));
            } finally {
                AttackTypes.clear();
            }
            hits++;
        }
        p.displayClientMessage(Component.literal("放电：" + hits + " 个目标挂了正电荷"), true);
    }

    /** 3 电磁吸附：把 24 格内最近的带正电荷目标拉到面前，再接一记铁棒重击。 */
    public static void attract(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        LivingEntity target = null;
        double best = Double.MAX_VALUE;
        for (var t : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(ATTRACT_RANGE),
                t -> t != p && t.isAlive() && !t.isSpectator() && CombatSettings.canHit(p, t))) {
            if (!KashimoCharges.charged(t)) continue;
            double d = p.distanceToSqr(t);
            if (d < best) { best = d; target = t; }
        }
        if (target == null) {
            refund(p, KashimoSkills.ATTRACT);
            p.sendSystemMessage(Component.literal("电磁吸附：24 格内没有带正电荷的目标").withStyle(ChatFormatting.GRAY));
            p.displayClientMessage(Component.literal("电磁吸附：没有带正电荷的目标"), true);
            return;
        }
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
        Vec3 to = p.position().add(flat.normalize().scale(ATTRACT_PULL));
        Vec3 from = target.position();
        Network.send(level, new Network.Effect(KashimoFx.ATTRACT, p.getId(), from, to, 1f, 12, 0));
        target.teleportTo(to.x, to.y, to.z);
        target.setDeltaMovement(Vec3.ZERO);
        KashimoCharges.syncMotion(target);
        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1f, 0.8f);
        AttackTypes.mark(AttackTypes.LIGHTNING);
        try {
            CombatSettings.hurt(p, target, p.damageSources().playerAttack(p),
                CombatSettings.casterDamage(p, KashimoSkills.ATTRACT_HIT_DAMAGE));
        } finally {
            AttackTypes.clear();
        }
        p.displayClientMessage(Component.literal("电磁吸附：" + target.getName().getString() + " → 铁棒重击"), true);
    }

    /** 4 雷神·附着：10 秒高压电弧，普攻挂 2 层正电荷且范围扩大。 */
    public static void attach(ServerPlayer p) {
        CompoundTag data = data(p);
        data.putLong(ATTACH_UNTIL, p.level().getGameTime() + ATTACH_TICKS);
        Network.send(p.serverLevel(), new Network.Effect(KashimoFx.ATTACH, p.getId(), p.position(), Vec3.ZERO,
            1f, ATTACH_TICKS, 0));
        p.level().playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.8f, 1.6f);
        p.sendSystemMessage(Component.literal("雷神·附着（10 秒）：普攻范围扩大 · 每次普攻挂 2 层正电荷")
            .withStyle(ChatFormatting.AQUA));
        p.displayClientMessage(Component.literal("雷神·附着 10 秒"), true);
    }

    /** 5 引爆：无视距离/地形/闪避，所有带正电荷的敌人按层数各吃一道落雷。 */
    public static void detonate(ServerPlayer p) {
        int neg = KashimoCharges.negative(p);
        if (neg <= 0) {
            refund(p, KashimoSkills.DETONATE);
            p.sendSystemMessage(Component.literal("引爆：身上没有负电荷（先打中敌人积累）").withStyle(ChatFormatting.GRAY));
            return;
        }
        List<LivingEntity> targets = KashimoCharges.chargedBy(p);
        if (targets.isEmpty()) {
            refund(p, KashimoSkills.DETONATE);
            p.sendSystemMessage(Component.literal("引爆：场上没有带正电荷的敌人").withStyle(ChatFormatting.GRAY));
            return;
        }
        double per = KashimoSkills.DETONATE_DAMAGE + neg * KashimoSkills.DETONATE_PER_NEG;
        int bolts = 0, victims = 0;
        for (LivingEntity t : targets) {
            int layers = KashimoCharges.layers(t);
            if (layers <= 0) continue;
            for (int i = 0; i < layers; i++) {
                if (strike(p, t, per)) bolts++;
            }
            KashimoCharges.clear(t);
            victims++;
        }
        KashimoCharges.setNegative(p, 0);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
            SoundSource.PLAYERS, 0.55f, 1.25f);
        String text = "引爆：" + victims + " 个目标 · " + bolts + " 道落雷 · 每道 "
            + String.format(Locale.ROOT, "%.0f", per);
        p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.AQUA));
        p.displayClientMessage(Component.literal(text), true);
    }

    // ------------------------------------------------------------ 终奥
    /** 轮盘中心：幻兽·琥珀（需要满咒力）。 */
    public static void requestUltimate(ServerPlayer p) {
        if (!isKashimo(p)) return;
        if (ultimate(p)) {
            p.displayClientMessage(Component.literal("幻兽·琥珀：已在形态中"), true);
            return;
        }
        if (crashing(p)) {
            p.displayClientMessage(Component.literal("肉体崩溃中：无法使用终奥"), true);
            return;
        }
        if (!usableOnce(p)) {
            p.sendSystemMessage(Component.literal("幻兽·琥珀：一生只能用一次（死亡后可再用；创造模式不限）")
                .withStyle(ChatFormatting.GRAY));
            p.displayClientMessage(Component.literal("幻兽·琥珀：本命已用尽"), true);
            return;
        }
        double energy = Curse.energy(p), max = Curse.maximum(p);
        if (energy < max - 1) {
            String text = "幻兽·琥珀需要满咒力（" + (int) energy + " / " + (int) max + "）";
            p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GRAY));
            p.displayClientMessage(Component.literal(text), true);
            return;
        }
        startUltimate(p, true);
    }

    private static boolean usableOnce(ServerPlayer p) {
        if (p.isCreative()) return true;
        return !data(p).getBoolean(ULT_USED);
    }

    public static void startUltimate(ServerPlayer p, boolean payEnergy) {
        if (payEnergy) Curse.add(p, -Curse.energy(p));
        CompoundTag data = data(p);
        data.putLong(ULT_UNTIL, p.level().getGameTime() + ULT_TICKS);
        data.putBoolean(ULT_USED, true);
        data.remove(CRASH_UNTIL);
        applyUltModifiers(p);
        Network.send(p.serverLevel(), new Network.Effect(KashimoFx.ULT, p.getId(), p.position(), Vec3.ZERO,
            1f, ULT_TICKS, 0));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
            SoundSource.PLAYERS, 0.9f, 0.8f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THUNDER,
            SoundSource.PLAYERS, 1f, 0.7f);
        p.sendSystemMessage(Component.literal("幻兽·琥珀（15 秒）：免疫物理伤害 · 移速攻速 ×2 · 普攻自带必中落雷 · 铁棒化为电磁波")
            .withStyle(ChatFormatting.AQUA));
        p.displayClientMessage(Component.literal("幻兽·琥珀 · 15 秒"), true);
    }

    public static void startCrash(ServerPlayer p) {
        CompoundTag data = data(p);
        data.putLong(CRASH_UNTIL, p.level().getGameTime() + CRASH_TICKS);
        clearUltModifiers(p);
        applyCrashModifiers(p);
        p.setHealth(1f);
        Network.send(p.serverLevel(), new Network.Effect(KashimoFx.CRASH, p.getId(), p.position(), Vec3.ZERO,
            1f, CRASH_TICKS, 0));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1f, 0.6f);
        p.sendSystemMessage(Component.literal("肉体崩溃（60 秒）：生命锁定 1 · 移速减半 · 无法攻击与施术")
            .withStyle(ChatFormatting.RED));
        p.displayClientMessage(Component.literal("肉体崩溃 · 60 秒 · 生命锁定 1"), true);
    }

    // ------------------------------------------------------------ 属性修饰
    private static void applyUltModifiers(ServerPlayer p) {
        // 只做「移速翻倍 + 攻速翻倍」，伤害不额外加成（按需求原样）
        setModifier(p, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, "KashimoUltAttackSpeed", 1.0);
    }

    private static void clearUltModifiers(ServerPlayer p) {
        setModifier(p, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, "KashimoUltAttackSpeed", 0);
        setModifier(p, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "KashimoUltDamage", 0);
    }

    private static void applyCrashModifiers(ServerPlayer p) {
        setModifier(p, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "KashimoUltDamage", -1.0);
        setModifier(p, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, "KashimoUltAttackSpeed", -0.5);
    }

    private static void clearCrashModifiers(ServerPlayer p) {
        setModifier(p, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "KashimoUltDamage", 0);
        setModifier(p, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, "KashimoUltAttackSpeed", 0);
    }

    private static void clearModifiers(ServerPlayer p) {
        setModifier(p, Attributes.MOVEMENT_SPEED, SPEED_ID, "KashimoChargeSpeed", 0);
        setModifier(p, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, "KashimoUltAttackSpeed", 0);
        setModifier(p, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "KashimoUltDamage", 0);
    }

    /** 只在数值变化时改修饰（每刻调用也安全）。amount = 0 表示移除。 */
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

    /** 技能条件不满足时把刚扣掉的咒力与冷却退回去。 */
    private static void refund(ServerPlayer p, int skill) {
        var definition = character.CharacterService.selected(p);
        var skillDef = definition == null ? null : definition.config().skill(skill);
        if (skillDef != null) Curse.add(p, skillDef.cost());
        data(p).putLong(SKILL_COOLDOWN + skill, 0);
    }

    private KashimoCombat() {}
}
