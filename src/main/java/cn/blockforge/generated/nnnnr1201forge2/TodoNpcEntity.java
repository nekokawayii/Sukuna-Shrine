package cn.blockforge.generated.nnnnr1201forge2;

import character.todo.TodoCombat;
import character.todo.TodoMarks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 东堂葵 NPC：玩家模型（皮肤等用户给图，先用默认 Steve）。
 * <p>行为和玩家角色一致：先给对方「注入咒力」挂标记，再与准星最近的有咒力的东西换位贴脸；
 * 被打时会用换位闪掉这一次伤害（前后各 0.3 秒免疫）。
 */
public class TodoNpcEntity extends PathfinderMob {
    private static final String SWAP_READY = "TodoNpcSwapReady", INJECT_READY = "TodoNpcInjectReady";

    public TodoNpcEntity(EntityType<? extends TodoNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, NpcBalance.health("sukuna:todo_npc"))
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true,
            e -> !(e instanceof TodoNpcEntity) && e.isAlive()));
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        long now = level().getGameTime();
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            target = nearestEnemy();
            if (target == null) return;
            setTarget(target);
        }
        // ① 注入咒力：给自己锁定的目标挂标记（永久有效）
        if (now >= getPersistentData().getLong(INJECT_READY) && distanceToSqr(target) <= 24 * 24) {
            getPersistentData().putLong(INJECT_READY, now + TodoCombat.NPC_INJECT_INTERVAL);
            TodoMarks.markEntity(this, target);
        }
        // ② 换位：想传就传（不看准星方向与距离），优先和自己的目标换
        if (now >= getPersistentData().getLong(SWAP_READY)) {
            getPersistentData().putLong(SWAP_READY, now + TodoCombat.NPC_SWAP_INTERVAL);
            if (TodoMarks.swapFree(this, target)) {
                level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.8f, 1.6f);
                getNavigation().stop();
            }
        }
    }

    /** 被打时用换位闪避；返回 true 表示已经换位（这一次伤害要取消）。 */
    public boolean tryDodge() {
        if (level().isClientSide()) return false;
        long now = level().getGameTime();
        if (now < getPersistentData().getLong(SWAP_READY)) return false;
        if (!TodoMarks.swapFree(this, getTarget())) return false;
        getPersistentData().putLong(SWAP_READY, now + TodoCombat.NPC_SWAP_INTERVAL);
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
            net.minecraft.sounds.SoundSource.HOSTILE, 0.9f, 1.4f);
        return true;
    }

    private LivingEntity nearestEnemy() {
        LivingEntity best = null;
        double bestDistance = 24 * 24;
        for (var candidate : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(24),
                candidate -> candidate != this && candidate.isAlive() && !candidate.isSpectator()
                    && candidate instanceof Monster)) {
            double d = distanceToSqr(candidate);
            if (d < bestDistance) { bestDistance = d; best = candidate; }
        }
        return best;
    }
}
