package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;

/** 漏壶（NPC）：敌对，使用漏壶的火焰系技能；见到玩家会开领域（冷却 300 秒）。 */
public class JogoNpcEntity extends Monster {
    private static final int DOMAIN_COOLDOWN = 6000;   // 300 秒
    private int shotAt;
    private int skillIndex;
    private long domainReady;
    private long domainEnd;

    public JogoNpcEntity(EntityType<? extends JogoNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 400;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
            e -> e instanceof Player p && !p.isCreative() && !p.isSpectator()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, BalanceRules.modpackMode() ? 800.0 * 30.0 : 800.0)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.FOLLOW_RANGE, 48.0)
            .add(Attributes.ARMOR, 5.0);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel world)) return;
        long now = world.getGameTime();
        LivingEntity target = getTarget();
        // 领域持续伤害
        if (now < domainEnd) {
            for (var t : world.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(27),
                    e -> e instanceof Player p && !p.isCreative() && e.isAlive())) {
                if (now % 10 == 0) {
                    t.hurt(damageSources().mobAttack(this), 4f);
                    t.setSecondsOnFire(3);
                }
            }
            if (now % 20 == 0) {
                Network.send(world, new Network.Effect(character.jogo.JogoFx.DOMAIN, getId(), position(),
                    Vec3.ZERO, 27f, 40, 0));
            }
        }
        if (target == null || !target.isAlive()) return;
        if (distanceToSqr(target) > 32 * 32) return;
        // 见到玩家：领域冷却好了就开
        if (now >= domainReady) {
            domainReady = now + DOMAIN_COOLDOWN;
            domainEnd = now + 600;   // 持续 30 秒
            Network.send(world, new Network.Effect(character.jogo.JogoFx.DOMAIN, getId(), position(), Vec3.ZERO, 27f, 600, 0));
            world.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.6f, 0.45f);
            sendSystemMessage(net.minecraft.network.chat.Component.literal("漏壶展开了盖棺铁围山"));
            return;
        }
        if (now < shotAt) return;
        shotAt = (int) now + 50;
        Vec3 eye = getEyePosition(), to = target.getBoundingBox().getCenter(), dir = to.subtract(eye).normalize();
        if ((skillIndex++ & 1) == 0) {
            // 火焰喷发：前方扇形火舌
            Network.send(world, new Network.Effect(character.jogo.JogoFx.PILLAR, 0, eye, dir, 1.6f, 12, 0));
            for (var t : world.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16),
                    e -> e instanceof Player p && !p.isCreative() && e.isAlive()
                        && e.position().subtract(position()).normalize().dot(dir) > 0.5)) {
                t.hurt(damageSources().mobAttack(this), 8f);
                t.setSecondsOnFire(4);
            }
        } else {
            // 陨石：目标位置爆炸
            Network.send(world, new Network.Effect(character.jogo.JogoFx.METEOR_IMPACT, 0, to, Vec3.ZERO, 3f, 14, 0));
            for (var t : world.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(to, to).inflate(4),
                    e -> e instanceof Player p && !p.isCreative() && e.isAlive())) {
                t.hurt(damageSources().mobAttack(this), 14f);
                t.setSecondsOnFire(3);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
            }
        }
    }
}
