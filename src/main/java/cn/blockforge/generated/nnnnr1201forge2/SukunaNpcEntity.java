package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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

    /** 从死亡边缘复活（咒胎受肉体用）：清死亡/受击状态并回满血。 */
    public void reviveFromDeath(double hp) {
        dead = false;
        deathTime = 0;
        hurtTime = 0;
        invulnerableTime = 20;
        setHealth((float) Math.max(1.0, hp));
    }


    /** 把"已进入原身"同步给客户端（皮肤与四只手靠它）。 */
    private void syncTrueForm() {
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            Network.send(sl, new Network.Effect(640, getId(), position(), Vec3.ZERO, 1f, 0, 0));
        }
    }

    /** r274：原身形态同时使用两种术式——神武解的扇形雷击 + 飞天的风压拉人斩击。 */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !getPersistentData().getBoolean("SukunaTrueForm")) return;
        if (level().getGameTime() % 100 == 0) syncTrueForm();   // 重连/远处玩家补同步
        // r277：原身出招无冷却——每刻都结算两把咒具（不再每 2 秒一次）
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        Vec3 forward = target.position().subtract(position());
        if (forward.lengthSqr() < 0.5) forward = getLookAngle();
        forward = forward.normalize();

        // ① 神武解：面前 30 格、半角 45° 的扇形雷击（基准 15 × 20 = 300 原值）
        for (LivingEntity t : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(30), t -> t != this && t.isAlive()
                    && (t instanceof net.minecraft.world.entity.Mob || t instanceof Player))) {
            Vec3 d = t.position().subtract(position());
            if (d.lengthSqr() > 0.25 && d.normalize().dot(forward) < Math.cos(Math.PI / 4)) continue;
            t.invulnerableTime = 0;
            NpcCombat.hurt(this, t, 300f);
            t.invulnerableTime = 0;
            if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    t.getX(), t.getY() + 1, t.getZ(), 18, .5, .6, .5, .08);
            }
        }

        // ② 飞天：周围 10 格 + 面前扇形 40 格的敌人先被风拽到前方 6 格，再吃 15 格内 20 伤
        for (LivingEntity t : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(40), t -> t != this && t.isAlive()
                    && (t instanceof net.minecraft.world.entity.Mob || t instanceof Player))) {
            Vec3 d = t.position().subtract(position());
            boolean near = d.lengthSqr() <= 100;
            boolean cone = d.lengthSqr() > 0.25 && d.normalize().dot(forward) >= Math.cos(Math.PI / 4);
            if (!near && !cone) continue;
            Vec3 pull = position().add(forward.scale(6)).subtract(t.position());
            if (pull.lengthSqr() > 0.01) {
                pull = pull.normalize().scale(Math.min(2.2, pull.length()));
                t.teleportTo(t.getX() + pull.x, t.getY(), t.getZ() + pull.z);
            }
            if (t.position().distanceToSqr(position()) <= 225) NpcCombat.hurt(this, t, 20f);
        }
        if (level().getGameTime() % 20 == 0) {
            level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.1f, 1.2f);
        }
    }
    /** r278：原身形态不吃击退，也不会被生物推挤。 */
    @Override
    public void knockback(double strength, double x, double z) {
        if (getPersistentData().getBoolean("SukunaTrueForm")) return;
        super.knockback(strength, x, z);
    }

    @Override
    public void push(net.minecraft.world.entity.Entity entity) {
        if (getPersistentData().getBoolean("SukunaTrueForm")) return;
        super.push(entity);
    }

    @Override
    public boolean isPushable() {
        return !getPersistentData().getBoolean("SukunaTrueForm") && super.isPushable();
    }

    /** 进入「原身形态」：换皮肤 + 血量（由 {@link SukunaNpcTrueForm} 在受击流程里调用）。 */
    public void enterTrueForm() {
        if (getPersistentData().getBoolean("SukunaTrueForm")) return;
        getPersistentData().putBoolean("SukunaTrueForm", true);
        double trueHp = BalanceRules.modpackMode() ? 300000.0 : 10000.0;
        var maxHp = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (maxHp != null) maxHp.setBaseValue(trueHp);
        // r274：原身四只手，同时持有「神武解」（主手）与「飞天」（副手）
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
            new ItemStack(VoidContent.SHINBUKE.get()));
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
            new ItemStack(VoidContent.HITEN.get()));
        // 清掉死亡/受击动画状态，避免"半死不死抽搐"
        dead = false; deathTime = 0; hurtTime = 0; invulnerableTime = 10;
        setHealth((float) trueHp);
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
            net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, .6f);
        syncTrueForm();
    }
}
