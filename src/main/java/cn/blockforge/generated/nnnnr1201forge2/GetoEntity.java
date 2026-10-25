package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 夏油杰：第二章人形 Boss。
 * <p>阶段一（血量 &gt;70%）：本体无敌且不可选中，只在场中召唤低级咒灵。
 * <p>阶段二（血量 ≤70%）：本体下场，三格内使用「体术·游云」三连棍。
 * <p>「极之番·漩涡」两阶段都会用：蓄力 3 秒吸收场上咒灵，伤害 50 + 每只×30，3 格爆炸。
 */
public final class GetoEntity extends Monster {
    /** 动作编号，与 {@link GetoRenderer} 的姿态分支一一对应。 */
    public static final int WAVE = 1, CHARGE = 2, THROW = 3, DRAW = 4, SWEEP = 5, PUNCH = 6;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ACTION =
        net.minecraft.network.syncher.SynchedEntityData.defineId(GetoEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> AGE =
        net.minecraft.network.syncher.SynchedEntityData.defineId(GetoEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> PHASE2 =
        net.minecraft.network.syncher.SynchedEntityData.defineId(GetoEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    private static final int SUMMON_INTERVAL = 200, SUMMON_BATCH = 2, CURSE_CAP = 5;
    private static final int VORTEX_CHARGE = 60, VORTEX_FLIGHT = 12;
    private static final double VORTEX_BASE = 50, VORTEX_PER_CURSE = 30, VORTEX_RADIUS = 3;
    private static final float MELEE_DAMAGE = 12f;
    private static final float MELEE_KNOCKBACK = 1.2f;
    private static final int MELEE_RANGE_CHECK = 3;

    private final ServerBossEvent bar = new ServerBossEvent(
        Component.literal("夏油杰"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    private long nextSummon, nextVortex, nextMelee, nextSweep;
    private int absorbed, sweepsLeft, thrownAt;
    private boolean vortexFired;

    public GetoEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 1200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1500)
            .add(Attributes.ARMOR, 10)
            .add(Attributes.ATTACK_DAMAGE, MELEE_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.FOLLOW_RANGE, 64)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ACTION, 0);
        entityData.define(AGE, 0);
        entityData.define(PHASE2, false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // 阶段一不下场：MeleeAttackGoal 自己判断阶段，避免阶段一追着玩家跑。
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override public boolean canUse() { return phase2() && super.canUse(); }
            @Override public boolean canContinueToUse() { return phase2() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override public boolean canUse() { return phase2() && super.canUse(); }
        });
    }

    public int action() { return entityData.get(ACTION); }
    public int actionAge() { return entityData.get(AGE); }
    public boolean phase2() { return entityData.get(PHASE2); }
    public int absorbedCurses() { return absorbed; }

    private void begin(int action) {
        entityData.set(ACTION, action);
        entityData.set(AGE, 0);
    }

    private void finish() {
        entityData.set(ACTION, 0);
        entityData.set(AGE, 0);
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        ServerLevel server = (ServerLevel) level();
        long now = server.getGameTime();
        if (action() != 0) entityData.set(AGE, actionAge() + 1);

        bar.setProgress(getMaxHealth() <= 0 ? 0 : getHealth() / getMaxHealth());

        // 阶段一固定在场：不主动移动，只维持朝向。
        if (!phase2() && action() == 0) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().x * 0.5, getDeltaMovement().y, getDeltaMovement().z * 0.5);
        }

        if (action() == 0) {
            if (now >= nextSummon) {
                nextSummon = now + SUMMON_INTERVAL;
                summonCurses(server, SUMMON_BATCH);
                if (!phase2()) begin(WAVE);
            }
            if (now >= nextVortex && !nearbyCurses(server).isEmpty()) {
                nextVortex = now + 400;
                absorbed = 0;
                vortexFired = false;
                begin(CHARGE);
                server.playSound(null, blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.HOSTILE, 1.2f, 0.6f);
            }
            if (phase2() && now >= nextMelee && getTarget() != null
                && distanceToSqr(getTarget()) <= (double) MELEE_RANGE_CHECK * MELEE_RANGE_CHECK) {
                nextMelee = now + 120;
                begin(DRAW);
            }
        } else if (action() == WAVE && actionAge() >= 20) {
            finish();
        } else if (action() == CHARGE) {
            if (actionAge() >= VORTEX_CHARGE) {
                absorbed = absorbCurses(server);
                begin(THROW);
                thrownAt = 0;
            }
        } else if (action() == THROW) {
            if (++thrownAt >= VORTEX_FLIGHT && !vortexFired) {
                vortexFired = true;
                fireVortex(server);
            }
            if (thrownAt >= VORTEX_FLIGHT + 12) finish();
        } else if (action() == DRAW) {
            if (actionAge() >= 10) {
                sweepsLeft = 3;
                nextSweep = now;
                begin(SWEEP);
            }
        } else if (action() == SWEEP) {
            if (sweepsLeft > 0 && now >= nextSweep) {
                sweep(server);
                sweepsLeft--;
                nextSweep = now + 8;
            } else if (sweepsLeft <= 0) {
                finish();
            }
        }
    }

    // ---------- 咒灵操术·群袭 ----------

    private List<LowCurseEntity> nearbyCurses(ServerLevel server) {
        return server.getEntitiesOfClass(LowCurseEntity.class, getBoundingBox().inflate(48), LowCurseEntity::isAlive);
    }

    private void summonCurses(ServerLevel server, int wanted) {
        int alive = nearbyCurses(server).size();
        int room = CURSE_CAP - alive;
        if (room <= 0) return;
        int count = Math.min(wanted, room);
        for (int i = 0; i < count; i++) {
            LowCurseEntity curse = Content.LOW_CURSE.get().create(server);
            if (curse == null) continue;
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 3 + random.nextDouble() * 4;
            double x = getX() + Math.cos(angle) * radius, z = getZ() + Math.sin(angle) * radius;
            curse.moveTo(x, getY(), z, random.nextFloat() * 360, 0);
            curse.emergeFrom(position());
            server.addFreshEntity(curse);
            if (getTarget() != null) curse.setTarget(getTarget());
            server.sendParticles(ParticleTypes.SQUID_INK, x, getY() + 0.2, z, 24, 0.6, 0.4, 0.6, 0.02);
        }
    }

    private int absorbCurses(ServerLevel server) {
        List<LowCurseEntity> curses = nearbyCurses(server);
        int count = 0;
        for (LowCurseEntity curse : curses) {
            Vec3 from = curse.position();
            for (int i = 0; i < 6; i++) {
                Vec3 at = from.lerp(getEyePosition(), i / 6.0);
                server.sendParticles(new DustParticleOptions(new Vector3f(0.12f, 0.02f, 0.18f), 1.1f),
                    at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
            curse.discard();
            count++;
        }
        return count;
    }

    // ---------- 极之番·漩涡 ----------

    private void fireVortex(ServerLevel server) {
        Vec3 start = getEyePosition();
        Vec3 dir = getLookAngle();
        Vec3 impact = start.add(dir.scale(12));
        double damage = VORTEX_BASE + VORTEX_PER_CURSE * absorbed;
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class,
                new AABB(impact, impact).inflate(VORTEX_RADIUS),
                e -> e != this && e.isAlive() && e instanceof Player)) {
            victim.hurt(damageSources().indirectMagic(this, this), (float) damage);
            Vec3 push = victim.position().subtract(impact).normalize().scale(0.6);
            victim.push(push.x, 0.35, push.z);
        }
        for (int i = 0; i < 40; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * VORTEX_RADIUS;
            server.sendParticles(new DustParticleOptions(new Vector3f(0.25f, 0.05f, 0.35f), 1.4f),
                impact.x + Math.cos(a) * r, impact.y + random.nextDouble() * 1.6, impact.z + Math.sin(a) * r,
                1, 0, 0, 0, 0);
        }
        server.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
        server.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2f, 0.7f);
    }

    // ---------- 体术·游云 ----------

    private void sweep(ServerLevel server) {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (distanceToSqr(target) > (double) (MELEE_RANGE_CHECK + 1) * (MELEE_RANGE_CHECK + 1)) return;
        target.hurt(damageSources().mobAttack(this), MELEE_DAMAGE);
        Vec3 push = target.position().subtract(position()).normalize().scale(MELEE_KNOCKBACK);
        target.push(push.x, 0.25, push.z);
        target.hurtMarked = true;
        Vec3 at = target.getEyePosition();
        server.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.85f, 0.4f), 0.8f), at.x, at.y, at.z, 8, 0.3, 0.3, 0.3, 0.05);
        server.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 0.8f);
    }

    // ---------- 阶段与受击 ----------

    private void enterPhase2() {
        entityData.set(PHASE2, true);
        setInvulnerable(false);
        finish();
        nextMelee = level().getGameTime() + 20;
        if (level() instanceof ServerLevel server) {
            server.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.6f, 0.8f);
            for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(48)))
                p.displayClientMessage(Component.literal("夏油杰：那就让我亲自来。"), true);
        }
    }

    @Override public void setHealth(float health) {
        super.setHealth(health);
        if (!level().isClientSide && !phase2() && getMaxHealth() > 0 && getHealth() <= getMaxHealth() * 0.7f) enterPhase2();
    }

    // 阶段一按需求「可被攻击、但不下场」：不覆写 hurt / isInvulnerableTo，
    // 本体全程正常受伤，血量降到 70% 时由上面的 setHealth 触发阶段二。
    // 阶段一的「不参战」只体现在目标与近战 goal 的 phase2() 判断上。

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bar.addPlayer(player);
    }

    @Override public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("GetoPhase2", phase2());
        tag.putLong("GetoNextSummon", nextSummon);
        tag.putLong("GetoNextVortex", nextVortex);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("GetoPhase2")) entityData.set(PHASE2, tag.getBoolean("GetoPhase2"));
        nextSummon = tag.getLong("GetoNextSummon");
        nextVortex = tag.getLong("GetoNextVortex");
    }
}
