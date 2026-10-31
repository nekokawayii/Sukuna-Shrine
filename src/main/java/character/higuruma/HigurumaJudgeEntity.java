package character.higuruma;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 诛伏赐死的「审判者」：不移动、无 AI，领域结束时由领域代码移除。 */
public class HigurumaJudgeEntity extends Mob {
    public HigurumaJudgeEntity(EntityType<? extends HigurumaJudgeEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1000).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override public boolean isPushable() { return false; }
    @Override public void push(double x, double y, double z) {}
    @Override public void aiStep() { setDeltaMovement(Vec3.ZERO); }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override protected void registerGoals() {}
}
