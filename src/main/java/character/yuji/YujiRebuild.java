package character.yuji;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import cn.blockforge.generated.nnnnr1201forge2.CombatSafety;
import cn.blockforge.generated.nnnnr1201forge2.CombatSettings;
import cn.blockforge.generated.nnnnr1201forge2.Curse;
import cn.blockforge.generated.nnnnr1201forge2.Network;
import cn.blockforge.generated.nnnnr1201forge2.YujiFx;

/** 虎杖重做（r316）：连击狂热 / 黑闪的才能 / 14 式体术。 */
public final class YujiRebuild {
    private static final java.util.UUID ATK_SPEED_ID = java.util.UUID.fromString("5b2f0b48-1a11-4a52-9a35-111111111111");
    private static final java.util.UUID MOVE_ID = java.util.UUID.fromString("5b2f0b48-1a11-4a52-9a35-222222222222");
    private static final int COMBO_MAX = 5, COMBO_TIMEOUT = 60, FOCUS_MAX = 100;

    /** 延时结算任务：二段径庭拳 / 蓄力重击 / 拳风飞行体。 */
    private record Job(ServerPlayer owner, ServerLevel level, LivingEntity target, Vec3 pos, Vec3 dir, long at, int kind, int step) { }
    private static final List<Job> JOBS = new ArrayList<>();
    private static final List<Job> PENDING = new ArrayList<>();

    // ===== 被动：连击狂热 =====
    public static int combo(ServerPlayer p) { return Curse.data(p).getInt("YujiCombo"); }
    private static void addCombo(ServerPlayer p) {
        var d = Curse.data(p);
        int n = Math.min(COMBO_MAX, d.getInt("YujiCombo") + 1);
        d.putInt("YujiCombo", n);
        d.putLong("YujiComboAt", p.level().getGameTime());
        var atk = p.getAttribute(Attributes.ATTACK_SPEED);
        var mv = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (atk != null) { atk.removeModifier(ATK_SPEED_ID); atk.addTransientModifier(new AttributeModifier(ATK_SPEED_ID, "yuji_combo_atk", n * 0.10, AttributeModifier.Operation.MULTIPLY_TOTAL)); }
        if (mv != null) { mv.removeModifier(MOVE_ID); mv.addTransientModifier(new AttributeModifier(MOVE_ID, "yuji_combo_move", n * 0.05, AttributeModifier.Operation.MULTIPLY_TOTAL)); }
    }
    private static void clearCombo(ServerPlayer p) {
        var d = Curse.data(p); d.putInt("YujiCombo", 0);
        var atk = p.getAttribute(Attributes.ATTACK_SPEED); if (atk != null) atk.removeModifier(ATK_SPEED_ID);
        var mv = p.getAttribute(Attributes.MOVEMENT_SPEED); if (mv != null) mv.removeModifier(MOVE_ID);
    }

    // ===== 被动：黑闪的才能 =====
    private static int focus(ServerPlayer p) { return Curse.data(p).getInt("YujiFocus"); }
    private static void addFocus(ServerPlayer p, int n) {
        var d = Curse.data(p); d.putInt("YujiFocus", Math.min(FOCUS_MAX, d.getInt("YujiFocus") + n));
    }

    public static void testReset(ServerPlayer p) {
        clearCombo(p);
        var d = Curse.data(p);
        for (String k : new ArrayList<>(d.getAllKeys())) if (k.startsWith("Yuji")) d.remove(k);
    }

    /** 径庭拳是被动：左键攻击必定挂上二段（哪怕这一下被闪避/无敌帧吃掉），0.5 秒后在敌人体内炸开。 */
    @SubscribeEvent
    public static void attack(net.minecraftforge.event.entity.player.AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
        if (!"yuji".equals(role(p))) return;
        if (!(e.getTarget() instanceof LivingEntity t) || !t.isAlive()) return;
        if (!cn.blockforge.generated.nnnnr1201forge2.CombatSafety.canTarget(p, t)) return;
        addCombo(p);
        addFocus(p, 10);
        if (JOBS.stream().noneMatch(j -> j.target == t && j.kind == 0))
            JOBS.add(new Job(p, (ServerLevel) p.level(), t, t.position(), Vec3.ZERO,
                p.level().getGameTime() + 10, 0, 0));
    }

    /** 命中：连击 + 专注（技能命中同样叠层）。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void hurt(LivingHurtEvent e) {
        if (e.getEntity().level().isClientSide) return;
        var src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || !"yuji".equals(role(p))) return;
        LivingEntity t = e.getEntity();
        if (!CombatSettings.canHit(p, t)) return;
        boolean melee = src.getDirectEntity() == p;
        if (melee) {
            addCombo(p);
            addFocus(p, 20);
            if (focus(p) >= FOCUS_MAX) { // 黑闪：2.5 次方
                double dmg = Math.pow(Math.max(1, e.getAmount()), 2.5);
                e.setAmount((float) Math.min(1.0e7, dmg));
                Curse.data(p).putInt("YujiFocus", 0);
                Network.send((ServerLevel) p.level(), new Network.Effect(YujiFx.BLACK_FLASH, p.getId(), t.position(), p.getLookAngle(), 3f, 20, 0));
            }
        }
    }

    private static String role(ServerPlayer p) {
        var d = character.CharacterService.selected(p);
        return d == null ? "" : d.id();
    }

    public static void cast(ServerPlayer p, int skill) {
        if (!(p.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        String key = "YujiCD_" + skill;
        if (Curse.data(p).getLong(key) > now) { msg(p, "冷却中"); return; }
        int cost = costOf(skill);
        if (!Curse.spend(p, cost)) { msg(p, "咒力不足"); return; }
        boolean ok = true;
        switch (skill) {
            case YujiRebuildSkills.BREAK -> {
                step(p, look, 1.2);
                for (LivingEntity t : front(p, 4.5, 40)) {
                    hit(p, t, 30, true);
                    var armor = t.getAttribute(Attributes.ARMOR);
                    if (armor != null) armor.setBaseValue(Math.max(0, armor.getBaseValue() * 0.6));
                    if (t instanceof Mob m) character.npc.NpcControl.stun(m, "yuji_break", 20);
                }
                fx(p, level, YujiFx.BREAK, eye.add(look.scale(2)), look, 1.5f, 20);
            }
            case YujiRebuildSkills.BARRAGE -> {
                int hits = 5 + level.random.nextInt(4);
                for (int i = 0; i < hits; i++) JOBS.add(new Job(p, level, null, p.position(), look, now + i * 3, 1, i == hits - 1 ? 1 : 0));
                fx(p, level, YujiFx.BARRAGE, eye.add(look.scale(2)), look, 2f, 24);
            }
            case YujiRebuildSkills.CHARGE -> {
                JOBS.add(new Job(p, level, null, p.position(), look, now + 20, 2, 0));
                fx(p, level, YujiFx.CHARGE, eye, look, 1.5f, 24);
                msg(p, "蓄力重击·碎骨：1 秒");
            }
            case YujiRebuildSkills.RISING -> {
                for (LivingEntity t : front(p, 3.5, 50)) {
                    hit(p, t, 35, true);
                    t.setDeltaMovement(t.getDeltaMovement().add(0, 1.1, 0));
                    t.hurtMarked = true;
                }
                fx(p, level, YujiFx.RISING, eye.add(look.scale(1.5)), look, 1.5f, 16);
            }
            case YujiRebuildSkills.HOOK -> {
                boolean intercept = false;
                for (LivingEntity t : front(p, 3.5, 50)) {
                    if (t instanceof Mob m && m.getTarget() != null) intercept = true;
                    hit(p, t, 25, true);
                    if (t instanceof Mob m) character.npc.NpcControl.stun(m, "yuji_hook", intercept ? 30 : 10);
                }
                fx(p, level, YujiFx.HOOK, eye.add(look.scale(1.5)), look, 1.2f, 14);
                msg(p, intercept ? "勾拳·截击：成功打断" : "勾拳·截击");
            }
            case YujiRebuildSkills.KICK -> {
                p.setDeltaMovement(look.x * 0.9, 0.55, look.z * 0.9); p.hurtMarked = true;
                for (LivingEntity t : front(p, 5, 40)) {
                    hit(p, t, 40, true);
                    if (!t.onGround()) t.setDeltaMovement(t.getDeltaMovement().add(0, -1.4, 0));
                    t.hurtMarked = true;
                }
                fx(p, level, YujiFx.KICK, eye.add(look.scale(2)), look, 1.6f, 18);
            }
            case YujiRebuildSkills.DASH -> {
                Vec3 from = p.position();
                Vec3 to = from.add(look.scale(10));
                for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        new AABB(from, to).inflate(1.6), x -> x != p && x.isAlive() && CombatSafety.canTarget(p, x)))
                    hit(p, t, 22, true);
                p.teleportTo(to.x, to.y, to.z);
                p.fallDistance = 0;
                fx(p, level, YujiFx.DASH, from, look, 10f, 16);
            }
            case YujiRebuildSkills.ROLL -> {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, false, false));
                p.invulnerableTime = 40;
                p.setDeltaMovement(p.getDeltaMovement().add(0, 0.35, 0));
                p.hurtMarked = true;
                fx(p, level, YujiFx.ROLL, p.position(), look, 2f, 20);
            }
            case YujiRebuildSkills.SWEEP -> {
                for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        p.getBoundingBox().inflate(4.5), x -> x != p && x.isAlive() && CombatSafety.canTarget(p, x))) {
                    hit(p, t, 26, true);
                    t.setDeltaMovement(t.getDeltaMovement().add(0, 0.55, 0));
                    t.hurtMarked = true;
                }
                fx(p, level, YujiFx.SWEEP, p.position(), look, 4.5f, 20);
            }
            case YujiRebuildSkills.WALL -> {
                var clip = level.clip(new net.minecraft.world.level.ClipContext(eye, eye.add(look.scale(3)),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
                if (clip.getType() == net.minecraft.world.phys.HitResult.Type.MISS) { msg(p, "前方 3 格内没有墙面"); ok = false; }
                else {
                    p.teleportTo(p.getX(), p.getY() + 1.6, p.getZ());
                    p.setDeltaMovement(look.scale(1.1).add(0, 0.45, 0)); p.hurtMarked = true;
                    fx(p, level, YujiFx.WALL, clip.getLocation(), look, 1f, 16);
                }
            }
            case YujiRebuildSkills.WAVE -> {
                JOBS.add(new Job(p, level, null, eye, look, now, 3, 0));
                fx(p, level, YujiFx.WAVE, eye.add(look.scale(1.2)), look, 1.2f, 16);
            }
            case YujiRebuildSkills.STOMP -> {
                p.teleportTo(p.getX(), p.getY() + 2.2, p.getZ());
                p.setDeltaMovement(0, -1.2, 0); p.hurtMarked = true;
                for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        p.getBoundingBox().inflate(8), x -> x != p && x.isAlive() && CombatSafety.canTarget(p, x))) {
                    hit(p, t, 45, true);
                    Vec3 push = t.position().subtract(p.position()).normalize().scale(1.1);
                    t.setDeltaMovement(push.x, 0.8, push.z); t.hurtMarked = true;
                }
                fx(p, level, YujiFx.STOMP, p.position(), look, 8f, 24);
            }
            case YujiRebuildSkills.ELBOW -> {
                for (LivingEntity t : front(p, 3, 45)) {
                    hit(p, t, 28, true);
                    Vec3 push = t.position().subtract(p.position()).normalize().scale(3.0);
                    t.setDeltaMovement(push.x, 0.3, push.z); t.hurtMarked = true;
                }
                fx(p, level, YujiFx.ELBOW, eye.add(look.scale(1.2)), look, 1f, 14);
            }
            case YujiRebuildSkills.KNEE -> {
                for (LivingEntity t : front(p, 3, 45)) {
                    hit(p, t, 35, true);
                    t.setAbsorptionAmount(Math.max(0, t.getAbsorptionAmount() - 30));
                    var armor = t.getAttribute(Attributes.ARMOR);
                    if (armor != null) armor.setBaseValue(Math.max(0, armor.getBaseValue() * 0.4));
                }
                fx(p, level, YujiFx.KNEE, eye.add(look.scale(1.2)), look, 1.2f, 16);
            }
            default -> ok = false;
        }
        if (ok) Curse.data(p).putLong(key, now + cooldownOf(skill));
        else Curse.addRaw(p, cost);
    }

    private static int costOf(int skill) {
        return switch (skill) {
            case YujiRebuildSkills.BREAK -> 120; case YujiRebuildSkills.BARRAGE -> 200;
            case YujiRebuildSkills.CHARGE -> 240; case YujiRebuildSkills.RISING -> 150;
            case YujiRebuildSkills.HOOK -> 140; case YujiRebuildSkills.KICK -> 180;
            case YujiRebuildSkills.DASH -> 160; case YujiRebuildSkills.ROLL -> 80;
            case YujiRebuildSkills.SWEEP -> 180; case YujiRebuildSkills.WALL -> 100;
            case YujiRebuildSkills.WAVE -> 200; case YujiRebuildSkills.STOMP -> 300;
            case YujiRebuildSkills.ELBOW -> 120; case YujiRebuildSkills.KNEE -> 180;
            default -> 0;
        };
    }
    private static int cooldownOf(int skill) {
        return switch (skill) {
            case YujiRebuildSkills.BREAK -> 100; case YujiRebuildSkills.BARRAGE -> 160;
            case YujiRebuildSkills.CHARGE -> 220; case YujiRebuildSkills.RISING -> 120;
            case YujiRebuildSkills.HOOK -> 140; case YujiRebuildSkills.KICK -> 140;
            case YujiRebuildSkills.DASH -> 120; case YujiRebuildSkills.ROLL -> 200;
            case YujiRebuildSkills.SWEEP -> 180; case YujiRebuildSkills.WALL -> 120;
            case YujiRebuildSkills.WAVE -> 160; case YujiRebuildSkills.STOMP -> 240;
            case YujiRebuildSkills.ELBOW -> 80; case YujiRebuildSkills.KNEE -> 140;
            default -> 100;
        };
    }

    private static List<LivingEntity> front(ServerPlayer p, double range, double halfAngleDeg) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, eye).inflate(range), x -> x != p && x.isAlive() && CombatSafety.canTarget(p, x))) {
            Vec3 to = t.position().add(0, t.getBbHeight() * .5, 0).subtract(eye);
            if (to.length() > range || to.length() < .3) continue;
            if (to.normalize().dot(look) < Math.cos(Math.toRadians(halfAngleDeg))) continue;
            out.add(t);
        }
        return out;
    }

    private static void step(ServerPlayer p, Vec3 dir, double d) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z).normalize().scale(d);
        p.teleportTo(p.getX() + flat.x, p.getY(), p.getZ() + flat.z);
    }

    private static void hit(ServerPlayer p, LivingEntity t, float dmg, boolean trueDamage) {
        t.invulnerableTime = 0;
        if (trueDamage) {
            var holder = p.serverLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL);
            t.hurt(new net.minecraft.world.damagesource.DamageSource(holder, p, p), dmg);
        } else CombatSettings.hurt(p, t, p.damageSources().playerAttack(p), dmg);
        t.invulnerableTime = 0;
        addCombo(p);
    }
    private static void fx(ServerPlayer p, ServerLevel level, int kind, Vec3 at, Vec3 dir, float size, int life) {
        Network.send(level, new Network.Effect(kind, p.getId(), at, dir, size, life, 0));
    }
    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(net.minecraft.network.chat.Component.literal(s), true); }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        for (Job j : new ArrayList<>(JOBS)) {
            var it = JOBS.listIterator(JOBS.indexOf(j));
            long now = j.level.getGameTime();
            if (now < j.at) continue;
            if (j.owner == null || !j.owner.isAlive()) { it.remove(); continue; }
            switch (j.kind) {
                case 0 -> { // 径庭拳二段
                    it.remove();
                    if (j.target != null && j.target.isAlive()) {
                        j.target.invulnerableTime = 0;
                        var holder = j.level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL);
                        j.target.hurt(new net.minecraft.world.damagesource.DamageSource(holder, j.owner, j.owner), 10f);
                        j.target.invulnerableTime = 0;
                        fx(j.owner, j.level, YujiFx.IKKI, j.target.position().add(0, 1, 0), j.owner.getLookAngle(), 1.2f, 14);
                    }
                }
                case 1 -> { // 乱打每一段
                    it.remove();
                    if (j.step == 1) {
                        for (LivingEntity t : front(j.owner, 4, 50)) {
                            hit(j.owner, t, 9, false);
                            Vec3 push = t.position().subtract(j.owner.position()).normalize().scale(2.2);
                            t.setDeltaMovement(push.x, 0.75, push.z); t.hurtMarked = true;
                        }
                    } else {
                        for (LivingEntity t : front(j.owner, 4, 50)) hit(j.owner, t, 9, false);
                    }
                    fx(j.owner, j.level, YujiFx.BARRAGE, j.owner.getEyePosition().add(j.dir.scale(1.5)), j.dir, 1f, 8);
                }
                case 2 -> { // 蓄力重击
                    it.remove();
                    for (LivingEntity t : front(j.owner, 4, 40)) {
                        hit(j.owner, t, 60, true);
                        if (t instanceof Mob m) character.npc.NpcControl.stun(m, "yuji_charge", 30);
                        var armor = t.getAttribute(Attributes.ARMOR);
                        if (armor != null) armor.setBaseValue(0);
                    }
                    fx(j.owner, j.level, YujiFx.CHARGE_HIT, j.owner.getEyePosition().add(j.dir.scale(2)), j.dir, 2f, 20);
                }
                case 3 -> { // 拳风飞行体：每刻前进，撞到敌人即结算
                    it.remove();
                    Vec3 pos = j.pos;
                    for (int step = 0; step < 8; step++) {
                        pos = pos.add(j.dir.scale(1.2));
                        boolean done = false;
                        for (LivingEntity t : j.level.getEntitiesOfClass(LivingEntity.class,
                                new AABB(pos, pos).inflate(1.6), x -> x.isAlive() && CombatSafety.canTarget(j.owner, x))) {
                            hit(j.owner, t, 34, true);
                            fx(j.owner, j.level, YujiFx.WAVE_HIT, pos, j.dir, 1.5f, 12);
                            done = true;
                            break;
                        }
                        if (done || pos.distanceTo(j.pos) > 22) break;
                    }
                    PENDING.add(new Job(j.owner, j.level, null, pos, j.dir, now + 1, 3, 0));
                }
                default -> it.remove();
            }
        }
        if (!PENDING.isEmpty()) { JOBS.addAll(PENDING); PENDING.clear(); }
        // 连击超时清空
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (!"yuji".equals(role(p))) continue;
            int n = combo(p);
            if (n > 0 && p.level().getGameTime() - Curse.data(p).getLong("YujiComboAt") > COMBO_TIMEOUT) clearCombo(p);
        }
    }

    private YujiRebuild() { }
}
