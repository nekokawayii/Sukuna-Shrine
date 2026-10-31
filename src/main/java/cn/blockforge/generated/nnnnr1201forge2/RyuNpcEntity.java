package cn.blockforge.generated.nnnnr1201forge2;

import character.ryu.RyuCombat;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 石流龙 NPC：玩家模型（皮肤等用户给图，先用默认 Steve）。
 * <p>和玩家同一套招：远距离蓄力光炮、中距离追踪弹、贴脸咒力喷涌；
 * 咒力见底又离得远时会坐下来吃甜品回蓝（咒力上限 10000、每秒只回 5）。
 */
public class RyuNpcEntity extends PathfinderMob {
    private static final String BLAST_READY = "RyuNpcBlastReady", SCATTER_READY = "RyuNpcScatterReady";
    private static final String BURST_READY = "RyuNpcBurstReady", DESSERT_READY = "RyuNpcDessertReady";

    public RyuNpcEntity(EntityType<? extends RyuNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 260;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, BalanceRules.modpackMode() ? 700.0 * 30.0 : 700.0)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
            e -> e instanceof Player player && !player.isCreative() && !player.isSpectator()));
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        long now = level().getGameTime();
        // 咒力：每秒 +5；吃甜品时翻倍
        if (now % 20 == 0) RyuCombat.npcRegen(this, RyuCombat.npcDessertActive(this) ? 2 : 1);
        if (RyuCombat.npcDessertActive(this)) return;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        double distance = distanceToSqr(target);
        // ① 甜品：咒力见底又离得远 → 坐下来吃
        if (RyuCombat.npcEnergy(this) < 2500 && distance > 64
            && now >= getPersistentData().getLong(DESSERT_READY)) {
            getPersistentData().putLong(DESSERT_READY, now + 400);
            RyuCombat.npcDessert(this);
            return;
        }
        // ② 贴脸：咒力喷涌解围（不耗咒力）
        if (distance <= 9 && now >= getPersistentData().getLong(BURST_READY)) {
            getPersistentData().putLong(BURST_READY, now + 200);
            RyuCombat.npcBurst(this);
            return;
        }
        // ③ 中距离：追踪弹
        if (distance <= 400 && now >= getPersistentData().getLong(SCATTER_READY)) {
            getPersistentData().putLong(SCATTER_READY, now + 80);
            if (RyuCombat.npcEnergy(this) >= RyuCombat.NPC_SCATTER_COST) RyuCombat.npcScatter(this, target);
            return;
        }
        // ④ 远距离：蓄力光炮
        if (distance > 36 && now >= getPersistentData().getLong(BLAST_READY)) {
            getPersistentData().putLong(BLAST_READY, now + 100);
            RyuCombat.npcBlast(this, target);
        }
    }

    /** 被近身或掉血时：没别的办法就喷涌一下。 */
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && !RyuCombat.npcDessertActive(this)
            && level().getGameTime() >= getPersistentData().getLong(BURST_READY)) {
            getPersistentData().putLong(BURST_READY, level().getGameTime() + 200);
            RyuCombat.npcBurst(this);
        }
        return hurt;
    }
}
