package cn.blockforge.generated.nnnnr1201forge2;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 秤金次 NPC：玩家模型，用的还是玩家那套「坐杀博徒」——
 * 贴脸砸车门、中距离撒钢珠、远距离列车冲撞；每 30 秒自己摇一次老虎机（假中奖，20 秒伤害 ×2 + 回血）。
 */
public class HakariNpcEntity extends PathfinderMob {
    private static final String NEXT_SMASH = "HakariNpcSmash", NEXT_BALLS = "HakariNpcBalls",
        NEXT_RUSH = "HakariNpcRush", NEXT_SPIN = "HakariNpcSpin", JACKPOT_UNTIL = "HakariNpcJackpot";

    public HakariNpcEntity(EntityType<? extends HakariNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 280;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, NpcBalance.health("sukuna:hakari_npc"))
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.31)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ARMOR, 5.0);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    private long next(String key, long fallback) {
        var data = getPersistentData();
        long now = level().getGameTime();
        long at = data.getLong(key);
        if (at == 0) { data.putLong(key, now + fallback); return 0; }
        return at;
    }

    private boolean ready(String key, long interval) {
        var data = getPersistentData();
        long now = level().getGameTime();
        if (data.getLong(key) > now) return false;
        data.putLong(key, now + interval);
        return true;
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        LivingEntity target = getTarget();
        var data = getPersistentData();
        long now = level().getGameTime();
        boolean jackpot = data.getLong(JACKPOT_UNTIL) > now;
        if (!jackpot && ready(NEXT_SPIN, 600)) {              // 每 30 秒摇一次（NPC 简化：直接中奖 20 秒）
            data.putLong(JACKPOT_UNTIL, now + 400);
            heal(4.0f);
        }
        if (target == null || !target.isAlive()) return;
        double distance = distanceTo(target);
        float scale = jackpot ? 2f : 1f;
        if (distance <= 5 && ready(NEXT_SMASH, 40)) doorSmash(target, scale);
        else if (distance <= 14 && ready(NEXT_BALLS, 50)) steelBalls(target, scale);
        else if (distance > 14 && ready(NEXT_RUSH, 80)) trainRush(target, scale);
    }

    private void doorSmash(LivingEntity target, float scale) {
        NpcCombat.hurt(this, target, 18f * scale);
        Vec3 push = target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(2.0);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.5, push.z));
        target.hurtMarked = true;
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND,
            net.minecraft.sounds.SoundSource.HOSTILE, 1f, 0.6f);
        Network.send((net.minecraft.server.level.ServerLevel) level(), new Network.Effect(character.hakari.HakariFx.DOOR, getId(),
            target.position(), getLookAngle(), 2.0f, 10, 0));
    }

    private void steelBalls(LivingEntity target, float scale) {
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(position(), position()).inflate(14),
                e -> e != this && e.isAlive() && e instanceof Player)) {
            NpcCombat.hurt(this, living, 7f * scale);
        }
        Network.send((net.minecraft.server.level.ServerLevel) level(), new Network.Effect(character.hakari.HakariFx.SPARK, getId(),
            getEyePosition(), getLookAngle(), 1.4f, 10, 0));
    }

    private void trainRush(LivingEntity target, float scale) {
        Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
        Vec3 at = position().add(dir.scale(Math.min(12, Math.max(2, position().distanceTo(target.position()) - 2))));
        if (!level().isClientSide && level().noCollision(this, getBoundingBox().move(at.subtract(position())))) {
            teleportTo(at.x, at.y, at.z);
            NpcCombat.hurt(this, target, 12f * scale);
            Network.send((net.minecraft.server.level.ServerLevel) level(), new Network.Effect(character.hakari.HakariFx.TRAIN, getId(),
                at, dir, 10f, 12, 0));
        }
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
}
