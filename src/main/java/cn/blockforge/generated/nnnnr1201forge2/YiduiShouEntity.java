package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 「圆钝果实」实体：完全不可移动（渲染用 一堆手.bbmodel）。 */
public class YiduiShouEntity extends Mob {
    public YiduiShouEntity(EntityType<? extends YiduiShouEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1024.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override public boolean isPushable() { return false; }
    @Override public void push(double x, double y, double z) { }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public void aiStep() { super.aiStep(); setDeltaMovement(Vec3.ZERO); }
    @Override protected void registerGoals() { }
}
