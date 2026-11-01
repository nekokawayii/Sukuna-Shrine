package character.mahito;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** NPC 真人同款、补到玩家轮盘上的六个术式（r208）。 */
public final class MahitoExtras {
    public static void cast(ServerPlayer p, int id) {
        switch (id) {
            case MahitoSkills.WALL -> MahitoPowers.wall(p);
            case MahitoSkills.BRIDGE -> MahitoPowers.bridge(p);
            case MahitoSkills.HEAL -> MahitoSelf.heal(p);
            case MahitoSkills.BURST -> {
                LivingEntity target = aim(p);
                if (target != null) MahitoSelf.burst(p, target);
                else MahitoSelf.burst(p, p);
            }
            case MahitoSkills.CLONE -> MahitoSelf.clone(p);
            case MahitoSkills.POTENTIAL -> MahitoPowers.potential(p);
            default -> { }
        }
    }

    private static LivingEntity aim(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null;
        double dot = 0.9;
        for (LivingEntity living : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(p.position(), p.position()).inflate(10), t -> t != p && t.isAlive())) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > 10) continue;
            double d = to.normalize().dot(look);
            if (d > dot) { dot = d; best = living; }
        }
        return best;
    }

    private MahitoExtras() {}
}
