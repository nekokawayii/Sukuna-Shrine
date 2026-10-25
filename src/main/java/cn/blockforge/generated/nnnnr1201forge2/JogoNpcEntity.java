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
import net.minecraft.world.phys.AABB;

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
                    NpcCombat.hurt(this, t, 4f);
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
        if (!NpcCurse.spend(this, 30)) { shotAt = (int) now + 15; return; }
        shotAt = (int) now + 10;   // 冷却极低：每 0.5 秒换一招
        cast(skillIndex++ % 6, target);
    }

    /** 灾厄火焰全套（轮流）：火砾虫 / 陨石 / 岩浆·火焰喷发 / 火山喷发 / 岩浆陷阱 / 火山灰隐身。 */
    private void cast(int idx, LivingEntity target) {
        var world = (ServerLevel) level();
        Vec3 eye = getEyePosition(), to = target.getBoundingBox().getCenter();
        Vec3 dir = to.subtract(eye).normalize();
        switch (idx) {
            case 0 -> { // 火砾虫
                Network.send(world, new Network.Effect(character.jogo.JogoFx.BUG, getId(), eye, dir, 1f, 20, 0));
                if (distanceToSqr(target) <= 24 * 24) {
                    NpcCombat.hurt(this, target, 6f);
                    target.setSecondsOnFire(3);
                }
            }
            case 1 -> { // 陨石
                Network.send(world, new Network.Effect(character.jogo.JogoFx.METEOR_IMPACT, getId(), to, Vec3.ZERO, 4f, 16, 0));
                for (var t : near(to, 4.5)) { NpcCombat.hurt(this, t, 12f); t.setSecondsOnFire(4); }
            }
            case 2 -> { // 岩浆·火焰喷发
                Network.send(world, new Network.Effect(character.jogo.JogoFx.PILLAR, 0, eye, dir, 2f, 14, 0));
                for (var t : world.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(18),
                        e -> e instanceof Player p && !p.isCreative() && e.isAlive()
                            && e.position().subtract(position()).normalize().dot(dir) > 0.45)) {
                    NpcCombat.hurt(this, t, 7f);
                    t.setSecondsOnFire(5);
                }
            }
            case 3 -> { // 火山喷发：直线火柱 40 格
                Network.send(world, new Network.Effect(character.jogo.JogoFx.PILLAR, 0, eye, dir, 2f, 16, 0));
                for (var t : world.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(40),
                        e -> e instanceof Player p && !p.isCreative() && e.isAlive()
                            && Math.abs(e.position().subtract(eye).normalize().dot(dir) - 1) < 0.06)) {
                    NpcCombat.hurt(this, t, 10f);
                    t.setSecondsOnFire(6);
                }
            }
            case 4 -> { // 岩浆陷阱：目标周围数道火柱 + 击飞
                for (int i = 0; i < 5; i++) {
                    Vec3 at = to.add((getRandom().nextDouble() - .5) * 6, 0, (getRandom().nextDouble() - .5) * 6);
                    Network.send(world, new Network.Effect(character.jogo.JogoFx.PILLAR, 0, at, Vec3.ZERO, 1.5f, 12, 0));
                    for (var t : near(at, 2.6)) {
                        NpcCombat.hurt(this, t, 7f);
                        t.setDeltaMovement(t.getDeltaMovement().add(0, .8, 0)); t.hurtMarked = true;
                        t.setSecondsOnFire(3);
                    }
                }
            }
            default -> { // 火山灰隐身
                addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 2, false, false));
                Network.send(world, new Network.Effect(character.jogo.JogoFx.BUG_BURST, getId(), position(), Vec3.ZERO, 2f, 10, 0));
            }
        }
    }

    private java.util.List<LivingEntity> near(Vec3 at, double r) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
            e -> e instanceof Player p && !p.isCreative() && e.isAlive());
    }
}
