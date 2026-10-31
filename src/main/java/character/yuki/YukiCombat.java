package character.yuki;

import character.CharacterSkill;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 九十九由基的战斗实现：只用原版伤害与粒子，不引入新实体。
 * <p>数值全部取 {@link CharacterSkill} 里的 damage / damageAt(章节)，方便后续调平衡。
 */
public final class YukiCombat {
    private static List<LivingEntity> around(ServerPlayer p, Vec3 center, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
            e -> e != p && e.isAlive() && !(e instanceof Player q && q.isCreative()));
    }

    /** 质量拳：前方锥形，重击 + 强击退。 */
    public static void massFist(ServerPlayer p, CharacterSkill skill, int rank) {
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        double range = skill.range();
        for (LivingEntity e : around(p, eye.add(dir.scale(range * 0.5)), range * 0.6 + skill.radius())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > range) continue;
            if (to.normalize().dot(dir) < Math.cos(Math.toRadians(skill.halfAngle()))) continue;
            e.hurt(p.damageSources().playerAttack(p), (float) skill.damageAt(rank));
            Vec3 kb = to.normalize().scale(2.2);
            e.push(kb.x, 0.7, kb.z);
            e.hurtMarked = true;
        }
        if (p.level() instanceof ServerLevel level) {
            for (int i = 1; i <= 12; i++) {
                Vec3 at = eye.add(dir.scale(range * i / 12.0));
                level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 0.6f);
        }
    }

    /** 迦楼罗：沿准星贯穿，命中全部沿途目标。 */
    public static void garuda(ServerPlayer p, CharacterSkill skill, int rank) {
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        double range = skill.range();
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        for (double t = 1; t <= range; t += 1.0) {
            Vec3 at = eye.add(dir.scale(t));
            for (LivingEntity e : around(p, at, skill.radius())) hit.add(e);
        }
        for (LivingEntity e : hit) {
            e.hurt(p.damageSources().playerAttack(p), (float) skill.damageAt(rank));
            e.hurtMarked = true;
        }
        if (p.level() instanceof ServerLevel level) {
            for (double t = 0; t <= range; t += 0.7) {
                Vec3 at = eye.add(dir.scale(t));
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 2, 0.15, 0.15, 0.15, 0.01);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS, 1.4f, 0.8f);
        }
    }

    /** 加重：自身增幅（复用原版效果，省得再写一套属性系统）。 */
    public static void starRage(ServerPlayer p, CharacterSkill skill, int rank) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 3, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 2, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1, false, false, true));
        p.displayClientMessage(Component.literal("星之怒·加重：质量提升，攻击与抗性大幅上升"), true);
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ASH, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.8, 0.6, 0.02);
        }
    }

    /** 黑洞：以准星落点为中心牵引并造成巨额伤害，自身虚弱。 */
    public static void blackHole(ServerPlayer p, CharacterSkill skill, int rank) {
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        Vec3 center = eye.add(dir.scale(skill.range()));
        double radius = skill.radius();
        for (LivingEntity e : around(p, center, radius)) {
            Vec3 pull = center.subtract(e.position());
            double dist = Math.max(0.4, pull.length());
            Vec3 v = pull.normalize().scale(1.8 / dist);
            e.push(v.x * 6, v.y * 4, v.z * 6);
            e.hurtMarked = true;
            e.hurt(p.damageSources().magic(), (float) skill.damageAt(rank));
        }
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2, false, false, true));
        if (p.level() instanceof ServerLevel level) {
            for (int i = 0; i < 120; i++) {
                double a = i * Math.PI / 30, r = radius * (1 - i / 140.0);
                level.sendParticles(ParticleTypes.PORTAL,
                    center.x + Math.cos(a) * r, center.y + (i % 12) * 0.3 - 1.5, center.z + Math.sin(a) * r,
                    2, 0.1, 0.1, 0.1, 0.05);
            }
            level.playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.6f, 0.5f);
        }
        p.displayClientMessage(Component.literal("星之怒·黑洞：奇点炸开，自身陷入虚弱"), false);
    }

    private YukiCombat() {}
}
