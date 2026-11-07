package cn.blockforge.generated.nnnnr1201forge2;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 五条悟分支六式的服务端结算（r302）。 */
public final class GojoBranchCombat {
    private record Singularity(ServerPlayer owner, ServerLevel level, Vec3 center, long at) { }
    private record Meteor(ServerPlayer owner, ServerLevel level, Vec3 pos, LivingEntity target, long end) { }
    private record Shot(ServerPlayer owner, ServerLevel level, Vec3 pos, Vec3 dir, long at, int index) { }
    private static final List<Singularity> SINGULARITIES = new ArrayList<>();
    private static final List<Meteor> METEORS = new ArrayList<>();
    private static final List<Shot> SHOTS = new ArrayList<>();

    public static void cast(ServerPlayer p, int skill) {
        com.mojang.logging.LogUtils.getLogger().info("[五条悟分支] 服务端收到 skill=" + skill + " 玩家=" + p.getName().getString());
        if (!GojoBranches.branch(skill) || !(p.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        String key = "GojoBranchCD_" + skill;
        if (Curse.data(p).getLong(key) > now) {
            com.mojang.logging.LogUtils.getLogger().info("[五条悟分支] 冷却中，剩余 " + (Curse.data(p).getLong(key) - now) + " 刻");
            p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                GojoBranches.name(skill) + " 冷却中 · " + ((Curse.data(p).getLong(key) - now + 19) / 20) + " 秒"), true);
            return;
        }
        if (!Curse.spend(p, GojoBranches.cost(skill))) {
            com.mojang.logging.LogUtils.getLogger().info("[五条悟分支] 咒力不足，需要 " + GojoBranches.cost(skill) + " 当前 " + Curse.energy(p));
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("咒力不足"), true);
            return;
        }
        Curse.data(p).putLong(key, now + GojoBranches.cooldown(skill));
        com.mojang.logging.LogUtils.getLogger().info("[五条悟分支] 开始结算 " + GojoBranches.name(skill));
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        switch (skill) {
            case GojoBranches.SINGULARITY -> {
                Vec3 center = eye.add(look.scale(10));
                SINGULARITIES.add(new Singularity(p, level, center, now + 40));
                Network.send(level, new Network.Effect(GojoBranches.FX_SINGULARITY, p.getId(), center, Vec3.ZERO, 5f, 60, 0));
            }
            case GojoBranches.TEAR -> {
                LivingEntity hit = rayTarget(p, 24);
                Vec3 at = hit == null ? eye.add(look.scale(12)) : hit.position();
                Network.send(level, new Network.Effect(GojoBranches.FX_TEAR, p.getId(), at, look, 4f, 30, 0));
                if (hit != null) {
                    hit.invulnerableTime = 0;
                    CombatSettings.hurt(p, hit, p.damageSources().magic(), 40f);
                    hit.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.WITHER, 120, 1, false, false));
                    hit.invulnerableTime = 0;
                }
            }
            case GojoBranches.METEOR -> {
                for (int i = 0; i < 5; i++) {
                    LivingEntity target = nearest(p, eye, 15);
                    METEORS.add(new Meteor(p, level, eye.add(look.scale(1)), target, now + 60));
                }
                Network.send(level, new Network.Effect(GojoBranches.FX_METEOR, p.getId(), eye, look, 2f, 20, 0));
            }
            case GojoBranches.HEAVY -> {
                Vec3 at = eye.add(look.scale(2));
                Network.send(level, new Network.Effect(GojoBranches.FX_HEAVY, p.getId(), at, look, 3f, 20, 0));
                for (LivingEntity t : targets(p, at, 3.5)) {
                    t.invulnerableTime = 0;
                    CombatSettings.hurt(p, t, p.damageSources().magic(), 55f);
                    Vec3 push = t.position().subtract(p.position()).normalize().scale(2.6);
                    t.setDeltaMovement(push.x, .7, push.z);
                    t.hurtMarked = true;
                    t.setAbsorptionAmount(0);
                    var armor = t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
                    if (armor != null) armor.setBaseValue(0);
                }
            }
            case GojoBranches.TURBULENCE -> {
                for (int i = 0; i < 5; i++) SHOTS.add(new Shot(p, level, eye, look, now + i * 3, i));
                Network.send(level, new Network.Effect(GojoBranches.FX_TURBULENCE, p.getId(), eye, look, 5f, 30, 0));
            }
            case GojoBranches.BRAINDEAD -> {
                LivingEntity target = rayTarget(p, 20);
                if (target == null) { p.displayClientMessage(net.minecraft.network.chat.Component.literal("准星没有目标"), true); return; }
                Network.send(level, new Network.Effect(GojoBranches.FX_BRAIN, p.getId(), target.getEyePosition(), Vec3.ZERO, 2f, 40, 0));
                long dur = target.getMaxHealth() > 2000 ? 40 : 100;
                target.getPersistentData().putLong("GojoBrainDeep", now + dur);
                if (target instanceof Mob m) character.npc.NpcControl.stun(m, "gojo_brain", (int) dur);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, (int) dur, 6, false, false));
            }
            default -> { }
        }
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
            new Network.Effect(GojoBranches.FX_METEOR + 50, p.getId(), Vec3.ZERO, Vec3.ZERO, 0f, 0, 0));
    }

    private static List<LivingEntity> targets(ServerPlayer p, Vec3 at, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
            t -> t != p && t.isAlive() && CombatSafety.canTarget(p, t));
    }

    private static LivingEntity nearest(ServerPlayer p, Vec3 at, double r) {
        LivingEntity best = null; double bd = r * r;
        for (LivingEntity t : targets(p, at, r)) {
            double d = t.position().distanceToSqr(at);
            if (d < bd) { bd = d; best = t; }
        }
        return best;
    }

    private static LivingEntity rayTarget(ServerPlayer p, double len) {
        Vec3 eye = p.getEyePosition();
        var hit = p.level().clip(new net.minecraft.world.level.ClipContext(eye, eye.add(p.getLookAngle().scale(len)),
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        Vec3 end = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? eye.add(p.getLookAngle().scale(len)) : hit.getLocation();
        LivingEntity best = null; double bd = 4;
        for (LivingEntity t : targets(p, eye, len)) {
            var clip = t.getBoundingBox().inflate(.3).clip(eye, end);
            if (clip.isPresent()) { double d = clip.get().distanceToSqr(eye); if (d < bd) { bd = d; best = t; } }
        }
        return best;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void brainHurt(LivingHurtEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide) return;
        if (t.getPersistentData().getLong("GojoBrainDeep") > t.level().getGameTime()) e.setAmount(e.getAmount() * 2f);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        long now = e.getServer().overworld().getGameTime();
        for (var it = SINGULARITIES.iterator(); it.hasNext(); ) {
            Singularity s = it.next();
            if (now >= s.at) {
                for (LivingEntity t : targets(s.owner, s.center, 6)) {
                    t.invulnerableTime = 0;
                    CombatSettings.hurt(s.owner, t, s.owner.damageSources().genericKill(), 60f);
                    t.invulnerableTime = 0;
                }
                Network.send(s.level, new Network.Effect(GojoBranches.FX_SINGULARITY, s.owner.getId(),
                    s.center, Vec3.ZERO, 6f, 20, 0));
                it.remove();
                continue;
            }
            for (LivingEntity t : targets(s.owner, s.center, 10)) {
                Vec3 pull = s.center.subtract(t.position());
                if (pull.lengthSqr() > .04) {
                    pull = pull.normalize().scale(.9);
                    t.setDeltaMovement(t.getDeltaMovement().add(pull.x, .08, pull.z));
                    t.hurtMarked = true;
                }
                if (t instanceof Mob m) character.npc.NpcControl.stun(m, "gojo_singularity", 10);
                else t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));
            }
        }
        for (var it = METEORS.iterator(); it.hasNext(); ) {
            Meteor m = it.next();
            LivingEntity t = m.target;
            if (t == null || !t.isAlive() || t.level() != m.level) { it.remove(); continue; }
            Vec3 to = t.getBoundingBox().getCenter().subtract(m.pos);
            if (to.lengthSqr() < 4) {
                t.invulnerableTime = 0;
                CombatSettings.hurt(m.owner, t, m.owner.damageSources().magic(), 16f);
                t.invulnerableTime = 0;
                Network.send(m.level, new Network.Effect(GojoBranches.FX_METEOR, m.owner.getId(), t.position(), Vec3.ZERO, 1.2f, 10, 0));
                it.remove();
                continue;
            }
            Vec3 step = to.normalize().scale(.9);
            METEORS.set(METEORS.indexOf(m), new Meteor(m.owner, m.level, m.pos.add(step), t, m.end));
        }
        for (var it = SHOTS.iterator(); it.hasNext(); ) {
            Shot s = it.next();
            if (now < s.at) continue;
            it.remove();
            Vec3 at = s.pos.add(s.dir.scale(3 + s.index));
            Network.send(s.level, new Network.Effect(GojoBranches.FX_TURBULENCE, s.owner.getId(), at, s.dir, 2f, 12, 0));
            LivingEntity hit = nearest(s.owner, at, 3);
            if (hit != null) {
                hit.invulnerableTime = 0;
                CombatSettings.hurt(s.owner, hit, s.owner.damageSources().magic(), 18f);
                hit.invulnerableTime = 0;
                if (s.index == 4) {
                    Network.send(s.level, new Network.Effect(GojoBranches.FX_HEAVY, s.owner.getId(), hit.position(), Vec3.ZERO, 4f, 30, 0));
                    hit.invulnerableTime = 0;
                    CombatSettings.hurt(s.owner, hit, s.owner.damageSources().genericKill(), 60f);
                    hit.invulnerableTime = 0;
                }
            }
        }
    }

    private GojoBranchCombat() { }
}
