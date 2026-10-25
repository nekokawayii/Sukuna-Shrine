package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 低级咒灵：夏油杰「咒灵操术·群袭」召唤出来的杂兵。
 * <p>模型 {@code basement_wanderer}（Blockbench 运行时解析），自带「扭曲行走 / 冲刺 / 攻击」三段动画；
 * 出场没有「爬出」片段，用 {@link #EMERGING} 状态做上升 + 透明度渐入来替代。
 */
public final class LowCurseEntity extends Monster {
    public static final int EMERGING = 2, ATTACK = 1, IDLE = 0;
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(LowCurseEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(LowCurseEntity.class, EntityDataSerializers.INT);
    private static final int EMERGE_TICKS = 20;

    public LowCurseEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 100)
            .add(Attributes.ATTACK_DAMAGE, 18)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 48)
            .add(Attributes.ARMOR, 2);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ACTION, 0);
        entityData.define(AGE, 0);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false) {
            @Override public boolean canUse() { return !emerging() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !emerging() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9) {
            @Override public boolean canUse() { return !emerging() && super.canUse(); }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override public boolean canUse() { return !emerging() && super.canUse(); }
        });
    }

    public int action() { return entityData.get(ACTION); }
    public int actionAge() { return entityData.get(AGE); }
    public boolean emerging() { return action() == EMERGING; }

    /** 由夏油杰在召唤时调用：从 {@code from} 的位置破土而出。 */
    public void emergeFrom(Vec3 from) {
        entityData.set(ACTION, EMERGING);
        entityData.set(AGE, 0);
    }

    public void beginAttack() {
        entityData.set(ACTION, ATTACK);
        entityData.set(AGE, 0);
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (action() != IDLE) {
            int age = actionAge() + 1;
            entityData.set(AGE, age);
            if (action() == EMERGING) {
                getNavigation().stop();
                if (age >= EMERGE_TICKS) { entityData.set(ACTION, IDLE); entityData.set(AGE, 0); }
            } else if (action() == ATTACK && age >= 18) {
                entityData.set(ACTION, IDLE);
                entityData.set(AGE, 0);
            }
        }
    }

    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) beginAttack();
        return hit;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CurseAction", action());
        tag.putInt("CurseAge", actionAge());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ACTION, tag.getInt("CurseAction"));
        entityData.set(AGE, tag.getInt("CurseAge"));
    }
}
