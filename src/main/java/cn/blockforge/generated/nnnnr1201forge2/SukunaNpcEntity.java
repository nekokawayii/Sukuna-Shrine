package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 两面宿傩 NPC：御厨子本体与全部斩击、灶、动作分支。
 * <p>招式与领域由 {@link character.npc.NpcCombatCasting} 调用真正技能；
 * 咒力、冷却、结印和持续技能归属独立，不再借用其他角色的领域。
 */
public class SukunaNpcEntity extends PathfinderMob {
    public SukunaNpcEntity(EntityType<? extends SukunaNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 999;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, NpcBalance.health("sukuna:sukuna_npc"))
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.MOVEMENT_SPEED, 0.34)
            .add(Attributes.FOLLOW_RANGE, 48.0)
            .add(Attributes.ARMOR, 12.0);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    /** r269：被打到 1 血后进入「原身形态」（换皮肤，一次性）。 */
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getHealth() <= 1.0f
            && !getPersistentData().getBoolean("SukunaTrueForm")) {
            getPersistentData().putBoolean("SukunaTrueForm", true);
            level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, .6f);
            if (getTarget() instanceof net.minecraft.server.level.ServerPlayer sp)
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("两面宿傩 · 原身显现")
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
    }
}
