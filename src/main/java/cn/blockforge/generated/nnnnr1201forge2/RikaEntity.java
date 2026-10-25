package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * 里香：乙骨忧太「真赝相爱」的召唤物模型 {@code rika.bbmodel}。
 * <p>无限血、存在 2400 刻（2 分钟）；自动攻击最近的敌对生物，
 * 交替使用「撕咬」（bite）与「拳击」（punch/punch_2）。
 * <p>伤害随主人的章节（{@code CharacterService.rank}）提升：咬 25+5×章，拳 15+3×章。
 */
public final class RikaEntity extends PathfinderMob {
    public static final int BITE = 1, PUNCH = 2;
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
        SynchedEntityData.defineId(RikaEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(RikaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(RikaEntity.class, EntityDataSerializers.INT);
    private static final int ATTACK_INTERVAL = 20;

    private int life = RikaRules.DURATION;
    private long nextAttack;

    public RikaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1024)
            .add(Attributes.MOVEMENT_SPEED, 0.34)
            .add(Attributes.FOLLOW_RANGE, 32)
            .add(Attributes.ATTACK_DAMAGE, 25);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER, Optional.empty());
        entityData.define(ACTION, 0);
        entityData.define(AGE, 0);
    }

    public int action() { return entityData.get(ACTION); }
    public int actionAge() { return entityData.get(AGE); }
    public UUID ownerId() { return entityData.get(OWNER).orElse(null); }

    public void setOwner(UUID id) { entityData.set(OWNER, Optional.ofNullable(id)); }
    public void setLifetime(int ticks) { life = ticks; }
    public int lifetime() { return life; }

    /** 让里香立刻进入撕咬动作（复制术式时短暂出现用）。 */
    public void forceBite(LivingEntity target) {
        entityData.set(ACTION, BITE);
        entityData.set(AGE, 0);
        if (target != null) lookAt(target, 30f, 30f);
    }

    private Player owner() {
        UUID id = ownerId();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    /** 无限血：任何伤害都不结算。 */
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean canBeCollidedWith() { return false; }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        ServerLevel server = (ServerLevel) level();
        if (--life <= 0) { vanish(server); return; }
        // 每秒给主人播报剩余时间（actionbar）。
        if (life % 20 == 0 && owner() instanceof ServerPlayer sp)
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal("里香 剩余 " + (life / 20) + " 秒"), true);
        if (action() != 0) entityData.set(AGE, actionAge() + 1);
        if (action() != 0 && actionAge() > 20) entityData.set(ACTION, 0);

        Player owner = owner();
        LivingEntity target = findTarget();
        long now = server.getGameTime();
        if (target != null) {
            getNavigation().moveTo(target, 1.5);
            if (distanceToSqr(target) <= 6.25 && now >= nextAttack) {
                nextAttack = now + ATTACK_INTERVAL;
                strike(server, target);
            }
        } else if (owner != null && distanceToSqr(owner) > 36) {
            getNavigation().moveTo(owner, 1.3);
        } else {
            getNavigation().stop();
        }
    }

    private LivingEntity findTarget() {
        List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(20),
            e -> e.isAlive() && e != this && e instanceof Enemy && !(e instanceof RikaEntity));
        list.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));
        return list.isEmpty() ? null : list.get(0);
    }

    private void strike(ServerLevel server, LivingEntity target) {
        int rank = 0;
        Player owner = owner();
        if (owner != null) rank = character.CharacterService.rank(owner);
        boolean bite = (server.getGameTime() / ATTACK_INTERVAL) % 2 == 0;
        float damage = bite ? 25f + 5f * rank : 15f + 3f * rank;
        target.hurt(damageSources().mobAttack(this), damage);
        entityData.set(ACTION, bite ? BITE : PUNCH);
        entityData.set(AGE, 0);
        server.sendParticles(new DustParticleOptions(new Vector3f(0.85f, 0.9f, 1f), 0.9f),
            target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        server.playSound(null, blockPosition(), bite ? SoundEvents.GENERIC_EAT : SoundEvents.PLAYER_ATTACK_STRONG,
            SoundSource.NEUTRAL, 1f, bite ? 0.7f : 1.1f);
    }

    private void vanish(ServerLevel server) {
        server.sendParticles(new DustParticleOptions(new Vector3f(0.8f, 0.9f, 1f), 1.2f),
            getX(), getY() + 1, getZ(), 40, 0.5, 0.9, 0.5, 0.05);
        server.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.2f, 0.8f);
        Player owner = owner();
        if (owner != null) RikaRules.clear(owner); // 解除无限咒力
        discard();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RikaLife", life);
        UUID id = ownerId();
        if (id != null) tag.putUUID("RikaOwner", id);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        life = tag.contains("RikaLife") ? tag.getInt("RikaLife") : RikaRules.DURATION;
        if (tag.hasUUID("RikaOwner")) setOwner(tag.getUUID("RikaOwner"));
    }

    @Override public boolean shouldShowName() { return false; }
}
