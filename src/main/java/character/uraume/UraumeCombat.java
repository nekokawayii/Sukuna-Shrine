package character.uraume;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 里梅玩家版六招（r289）：霜凪/直瀑沿用旧实现，其余四招在此结算。 */
public final class UraumeCombat {
    public static void cast(ServerPlayer p, int skill) {
        if (skill == UraumeSkills.FROST) { Uraume.cast(p, BranchRules.FROST); return; }
        if (skill == UraumeSkills.ICEFALL) { Uraume.cast(p, BranchRules.ICEFALL); return; }
        if (skill == UraumeSkills.DOMAIN) {
            if (UraumePalaceDomain.active(p)) UraumePalaceDomain.close(p, "收起"); else UraumePalaceDomain.open(p);
            return;
        }
        Vec3 aim = p.getEyePosition().add(p.getLookAngle().scale(12));
        switch (skill) {
            case UraumeSkills.PRISON -> {
                Network.send(p.serverLevel(), new Network.Effect(BranchRules.FX_ICE, p.getId(),
                    aim.add(0, 2, 0), new Vec3(0, -1, 0), 5f, 40, 1));
                for (LivingEntity t : targets(p, aim, 4.5)) {
                    t.setTicksFrozen(240);
                    if (t instanceof net.minecraft.world.entity.Mob m)
                        character.npc.NpcControl.stun(m, "uraume_prison", 40);
                    hurt(p, t, 14f);
                    UraumeIce.addFrost(t, 1);
                }
            }
            case UraumeSkills.STORM -> {
                for (int pulse = 0; pulse < 5; pulse++) {
                    for (LivingEntity t : targets(p, p.position(), 9)) {
                        pull(t, p.position(), 1.4);
                        hurt(p, t, 4f);
                        UraumeIce.addFrost(t, 1);
                    }
                }
                Network.send(p.serverLevel(), new Network.Effect(BranchRules.FX_FROST, p.getId(),
                    p.position(), Vec3.ZERO, 9f, 100, 3));
            }
            case UraumeSkills.DRAGON -> {
                Vec3 dir = p.getLookAngle();
                Network.send(p.serverLevel(), new Network.Effect(BranchRules.FX_ICE, p.getId(),
                    p.position().add(0, 1, 0), dir, 20f, 30, 2));
                for (LivingEntity t : targets(p, p.position().add(dir.scale(10)), 12)) {
                    Vec3 d = t.position().subtract(p.position());
                    if (d.lengthSqr() > 1 && d.normalize().dot(dir) < .6) continue;
                    hurt(p, t, 25f);
                    UraumeIce.addFrost(t, 2);
                    t.setDeltaMovement(dir.scale(2.2).add(0, .55, 0));
                    t.hurtMarked = true;
                }
            }
            case UraumeSkills.ICEAGE -> {
                Network.send(p.serverLevel(), new Network.Effect(BranchRules.FX_FROST, p.getId(),
                    p.position(), Vec3.ZERO, 50f, 60, 9));
                for (LivingEntity t : targets(p, p.position(), 50)) {
                    t.setTicksFrozen(400);
                    if (t instanceof net.minecraft.world.entity.Mob m)
                        character.npc.NpcControl.stun(m, "uraume_iceage", 200);
                    hurt(p, t, 40f);
                    UraumeIce.addFrost(t, 3);
                    t.getPersistentData().putBoolean("IceStatue", true);
                }
                p.getPersistentData().putLong("FrostbiteUntil", p.level().getGameTime() + 600);
            }
            default -> { }
        }
    }

    private static java.util.List<LivingEntity> targets(ServerPlayer p, Vec3 at, double radius) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
            t -> t != p && t.isAlive() && CombatSafety.canTarget(p, t));
    }

    private static void hurt(ServerPlayer p, LivingEntity t, float amount) {
        // 领域内冰系伤害翻倍（r289）
        if (UraumeDomain.inDomain(t) || UraumePalaceDomain.inDomain(t)) amount *= 2f;
        CombatSettings.hurt(p, t, p.damageSources().magic(), amount);
    }

    private static void pull(LivingEntity t, Vec3 center, double power) {
        Vec3 d = center.subtract(t.position());
        if (d.lengthSqr() > .01) {
            d = d.normalize().scale(power);
            t.setDeltaMovement(t.getDeltaMovement().add(d.x, .05, d.z));
            t.hurtMarked = true;
        }
    }

    private UraumeCombat() { }
}
