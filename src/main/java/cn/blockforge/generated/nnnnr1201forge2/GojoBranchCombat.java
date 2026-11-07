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
    /** 苍·环绕：围绕施术者半径 30 格高速旋转的巨大苍球。 */
    private record Orbit(LivingEntity owner, ServerLevel level, long end, double angle) { }
    private static final List<Orbit> ORBITS = new ArrayList<>();
    /** 重压冲击：先在被斩中的目标身上留"斩痕"，10 刻后一起炸开（类似宿傩的解多重斩）。 */
    private record Mark(ServerPlayer owner, ServerLevel level, LivingEntity target, long at) { }
    private static final List<Mark> MARKS = new ArrayList<>();

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
        com.mojang.logging.LogUtils.getLogger().info("[五条悟分支] 开始结算 " + GojoBranches.name(skill));
        boolean[] ok = {true};
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        switch (skill) {  // 下面每个 case 结算完，若 ok[0] 仍为 true 才写冷却

            case GojoBranches.SINGULARITY -> {
                // 准星位置十字排开 10 个「1 格大的苍」：中心 1 个 + 上下左右各两格位（间距 1.6 格）
                Vec3 c = eye.add(look.scale(10));
                Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
                Vec3 up = right.cross(look).normalize();
                int n = 0;
                SINGULARITIES.add(new Singularity(p, level, c, now + 40));
                Network.send(level, new Network.Effect(GojoBranches.FX_SINGULARITY, p.getId(), c, look, 1f, 60, 0));
                for (int d = 0; d < 4; d++) {
                    Vec3 axis = switch (d) { case 0 -> right; case 1 -> right.scale(-1); case 2 -> up; default -> up.scale(-1); };
                    for (int k = 1; k <= 2 && n < 9; k++, n++) {
                        Vec3 pos = c.add(axis.scale(1.6 * k));
                        SINGULARITIES.add(new Singularity(p, level, pos, now + 40));
                        Network.send(level, new Network.Effect(GojoBranches.FX_SINGULARITY, p.getId(), pos, look, 1f, 60, 0));
                    }
                    if (n >= 9) break;
                }
            }
            case GojoBranches.TEAR -> {
                // 前方扇形：长 25 格、半角 55°，扇形内所有敌人吃撕裂 + 凋零
                Network.send(level, new Network.Effect(GojoBranches.FX_TEAR, p.getId(), eye, look, 25f, 30, 0));
                int hits = 0;
                for (LivingEntity t : targets(p, eye.add(look.scale(12)), 26)) {
                    Vec3 to = t.position().add(0, t.getBbHeight() * .5, 0).subtract(eye);
                    double dist = to.length();
                    if (dist < .8 || dist > 25) continue;
                    if (to.normalize().dot(look) < Math.cos(Math.toRadians(55))) continue;
                    t.invulnerableTime = 0;
                    CombatSettings.hurt(p, t, p.damageSources().magic(), 40f);
                    t.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.WITHER, 120, 1, false, false));
                    t.invulnerableTime = 0;
                    hits++;
                }
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("苍·引力撕裂：扇形命中 " + hits + " 个目标"), true);
            }
            case GojoBranches.METEOR -> {
                for (int i = 0; i < 5; i++) {
                    LivingEntity target = nearest(p, eye, 15);
                    METEORS.add(new Meteor(p, level, eye.add(look.scale(1)), target, now + 60));
                }
                Network.send(level, new Network.Effect(GojoBranches.FX_METEOR, p.getId(), eye, look, 2f, 20, 0));
            }
            case GojoBranches.HEAVY -> {
                // 类似宿傩的解多重斩：先在前方扇形留下斩痕，10 刻后所有被标记者一起被斩开
                Vec3 at = eye.add(look.scale(4));
                Network.send(level, new Network.Effect(GojoBranches.FX_HEAVY, p.getId(), at, look, 6f, 20, 0));
                int marked = 0;
                for (LivingEntity t : targets(p, at, 7)) {
                    Vec3 to = t.position().add(0, t.getBbHeight() * .5, 0).subtract(eye);
                    if (to.length() > 7 || to.normalize().dot(look) < Math.cos(Math.toRadians(60))) continue;
                    MARKS.add(new Mark(p, level, t, now + 10));
                    marked++;
                }
                if (marked == 0) { Curse.addRaw(p, GojoBranches.cost(skill)); ok[0] = false;
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal("赫·重压冲击：前方 7 格扇形内没有目标"), true); }
                else p.displayClientMessage(net.minecraft.network.chat.Component.literal("赫·重压冲击：斩痕附着 " + marked + " 个目标"), true);
            }
            case GojoBranches.TURBULENCE -> {
                // 5 发极小的赫：体积小、飞行快、单发爆炸小，第 5 发仍带核爆
                for (int i = 0; i < 5; i++) SHOTS.add(new Shot(p, level, eye, look, now + i * 3, i));
                Network.send(level, new Network.Effect(GojoBranches.FX_TURBULENCE, p.getId(), eye, look, 1.2f, 24, 0));
            }
            case GojoBranches.BRAINDEAD -> {
                LivingEntity target = rayTarget(p, 20);
                if (target == null) {
                    Curse.addRaw(p, GojoBranches.cost(skill));   // 没打中：退咒力、不写冷却
                    ok[0] = false;
                    p.displayClientMessage(net.minecraft.network.chat.Component.literal("准星 20 格内没有可命中的目标"), true);
                    break;
                }
                Network.send(level, new Network.Effect(GojoBranches.FX_BRAIN, p.getId(), target.getEyePosition(), Vec3.ZERO, 2f, 40, 0));
                long dur = target.getMaxHealth() > 2000 ? 40 : 100;
                target.getPersistentData().putLong("GojoBrainDeep", now + dur);
                if (target instanceof Mob m) character.npc.NpcControl.stun(m, "gojo_brain", (int) dur);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, (int) dur, 6, false, false));
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WEAKNESS, (int) dur, 3, false, false));
                target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
                target.invulnerableTime = 0;
                CombatSettings.hurt(p, target, p.damageSources().magic(), 8f);   // 至少有可见伤害
                target.invulnerableTime = 0;
                p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "无量空处·局部脑死：" + target.getName().getString() + " 瘫痪 " + (dur / 20) + " 秒"), true);
            }
            case GojoBranches.SURROUND -> {
                ORBITS.add(new Orbit(p, level, now + 200, 0));
                Network.send(level, new Network.Effect(GojoFx.BLUE, p.getId(),
                    p.position().add(30, 1.5, 0), Vec3.ZERO, 7.5f, 8, 0));
            }
            default -> ok[0] = false;
        }
        if (ok[0]) Curse.data(p).putLong(key, now + GojoBranches.cooldown(skill));
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
            new Network.Effect(GojoBranches.FX_METEOR + 50, p.getId(), Vec3.ZERO, Vec3.ZERO, 0f, 0, 0));
    }

    /** Boss 版苍·环绕（r344）。 */
    public static void orbit(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        ORBITS.add(new Orbit(owner, level, level.getGameTime() + 200, 0));
        Network.send(level, new Network.Effect(GojoFx.BLUE, owner.getId(),
            owner.position().add(30, 1.5, 0), Vec3.ZERO, 7.5f, 8, 0));
    }

    private static List<LivingEntity> targetsOf(LivingEntity owner, Vec3 at, double r) {
        return owner.level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
            t -> t != owner && t.isAlive() && (!(owner instanceof ServerPlayer sp) || CombatSafety.canTarget(sp, t)));
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
                for (LivingEntity t : targets(s.owner, s.center, 2.6)) {
                    t.invulnerableTime = 0;
                    CombatSettings.hurt(s.owner, t, s.owner.damageSources().genericKill(), 25f);
                    t.invulnerableTime = 0;
                }
                Network.send(s.level, new Network.Effect(GojoBranches.FX_SINGULARITY, s.owner.getId(),
                    s.center, Vec3.ZERO, 1.2f, 20, 0));
                it.remove();
                continue;
            }
            for (LivingEntity t : targets(s.owner, s.center, 4.0)) {
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
        for (var it = ORBITS.iterator(); it.hasNext(); ) {
            Orbit o = it.next();
            if (now >= o.end || !o.owner.isAlive()) { it.remove(); continue; }
            double angle = o.angle + 0.18;   // 约 5.4 格/刻：连续环绕，不再是"一格一格跳"
            Vec3 at = o.owner.position().add(Math.cos(angle) * 30, 1.5, Math.sin(angle) * 30);
            ORBITS.set(ORBITS.indexOf(o), new Orbit(o.owner, o.level, o.end, angle));
            // 苍是引力：48 格内的敌人被快速吸向球心（不击退）
            for (LivingEntity t : targetsOf(o.owner, at, 48)) {
                Vec3 pull = at.subtract(t.position());
                if (pull.lengthSqr() > .25) {
                    Vec3 flat = new Vec3(pull.x, 0, pull.z);
                    if (flat.lengthSqr() > 1e-4) flat = flat.normalize().scale(2.4); // 吸得更快，追得上高速环绕
                    // 竖直：最多只允许比球心高 1 格；超过就不再给向上的量
                    double vy = t.getY() < at.y + 1.0 ? 0.12 : -0.06;
                    t.setDeltaMovement(flat.x, vy, flat.z);
                    t.hurtMarked = true;
                    // 被吸着撞墙：一次 1000% 伤害（每 2 秒最多一次，避免贴墙连打）
                    var pdt = t.getPersistentData();
                    if (now >= pdt.getLong("OrbitWallHit")
                        && !o.level.noCollision(t, t.getBoundingBox().deflate(.05).move(flat.x, 0, flat.z))) {
                        pdt.putLong("OrbitWallHit", now + 40);
                        t.invulnerableTime = 0;
                        if (o.owner instanceof ServerPlayer sp) CombatSettings.hurt(sp, t, sp.damageSources().genericKill(), 250f);
                        else t.hurt(o.owner.damageSources().genericKill(), 250f);
                        t.invulnerableTime = 0;
                        Network.send(o.level, new Network.Effect(GojoFx.BLUE_END, o.owner.getId(), t.position(), Vec3.ZERO, 3f, 12, 0));
                    }
                }
            }
            // 球体本身碰到：每次 25 伤，不击退
            if (now % 4 == 0) {
                for (LivingEntity t : targetsOf(o.owner, at, 7.5)) {
                    t.invulnerableTime = 0;
                    if (o.owner instanceof ServerPlayer sp) CombatSettings.hurt(sp, t, sp.damageSources().magic(), 25f);
                    else t.hurt(o.owner.damageSources().magic(), 25f);
                    t.invulnerableTime = 0;
                }
            }
            Network.send(o.level, new Network.Effect(GojoFx.BLUE, o.owner.getId(), at, Vec3.ZERO, 7.5f, 4, 0));
        }
        for (var it = MARKS.iterator(); it.hasNext(); ) {
            Mark m = it.next();
            LivingEntity t = m.target;
            if (now < m.at) continue;
            it.remove();
            if (t == null || !t.isAlive() || t.level() != m.level) continue;
            t.invulnerableTime = 0;
            CombatSettings.hurt(m.owner, t, m.owner.damageSources().magic(), 55f);
            Vec3 push = t.position().subtract(m.owner.position()).normalize().scale(2.6);
            t.setDeltaMovement(push.x, .7, push.z);
            t.hurtMarked = true;
            t.setAbsorptionAmount(0);
            var armor = t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            if (armor != null) armor.setBaseValue(0);
            t.invulnerableTime = 0;
            Network.send(m.level, new Network.Effect(GojoBranches.FX_HEAVY, m.owner.getId(), t.position(), Vec3.ZERO, 2f, 14, 0));
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
                CombatSettings.hurt(s.owner, hit, s.owner.damageSources().magic(), 10f);
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
