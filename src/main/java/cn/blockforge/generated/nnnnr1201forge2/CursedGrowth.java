package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** 清理旧存档的玩家共鸣倍率；首领属性与玩家成长无关。 */
public final class CursedGrowth {
    private static final UUID HEALTH=UUID.fromString("84113872-88e2-4b3d-82fc-010300000001"),ATTACK=UUID.fromString("84113872-88e2-4b3d-82fc-010300000002");
    public static double scale(LivingEntity target){return 1;}
    public static void update(LivingEntity mob,LivingEntity target){var health=mob.getAttribute(Attributes.MAX_HEALTH);var attack=mob.getAttribute(Attributes.ATTACK_DAMAGE);if(health!=null)health.removeModifier(HEALTH);if(attack!=null)attack.removeModifier(ATTACK);if(mob.getHealth()>mob.getMaxHealth())mob.setHealth(mob.getMaxHealth());}
}
