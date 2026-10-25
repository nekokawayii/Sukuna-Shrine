package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 胀相：中立角色（不主动攻击、被打才还手），会赤血操术全部 20 招；胎海/死胎星 300 秒冷却。 */
public class ChosoEntity extends PathfinderMob {
    /** 与 BloodRules.NAMES 顺序一致。 */
    private static final String[] SKILLS = {
        "赤缚", "苅祓", "百敛", "穿血", "赤鳞跃动", "血刃", "血星磊", "血铠", "超新星", "运河",
        "翅王", "血雾", "血棘", "血镜", "血之领域·胎海", "极之番·赤血操术「死胎星」", "血洋", "血爆", "赤鳞跃动·载", "血刃·千本"
    };
    private static final int DOMAIN_INDEX = 14, DEATH_STAR_INDEX = 15;
    private static final int DOMAIN_TIDE = 6000, DEATH_STAR_TIDE = 6000;   // 300 秒

    private int shotAt;
    private int fxIndex;
    private int skillIndex;
    private long domainReady, deathStarReady;

    public ChosoEntity(EntityType<? extends ChosoEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 300;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
            this, net.minecraft.world.entity.monster.Monster.class, true,
            e -> !(e instanceof ChosoEntity) && e.isAlive()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, BalanceRules.modpackMode() ? 550.0 * 30.0 : 550.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.ARMOR, 4.0);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            target = fallbackTarget();
            if (target == null) return;
            setTarget(target);
        }
        long now = level().getGameTime();
        if (now < shotAt) return;
        if (distanceToSqr(target) > 32 * 32) return;
        for (int guard = 0; guard < SKILLS.length; guard++) {
            int idx = skillIndex++ % SKILLS.length;
            if (idx == DOMAIN_INDEX && now < domainReady) continue;
            if (idx == DEATH_STAR_INDEX && now < deathStarReady) continue;
            if (idx == DOMAIN_INDEX) domainReady = now + DOMAIN_TIDE;
            if (idx == DEATH_STAR_INDEX) deathStarReady = now + DEATH_STAR_TIDE;
            shotAt = (int) now + 70;
            if (!character.CharacterCasting.castFor(this, idx)) shotAt = (int) now + 20;
            return;
        }
        shotAt = (int) now + 70;
    }

    /** index 对应 BloodRules.NAMES 顺序；效果为简化版，但每招都有独立表现。 */
    private void cast(int index, LivingEntity target) {
        ServerLevel world = (ServerLevel) level();
        Vec3 eye = getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        Vec3 dir = to.subtract(eye).normalize();
        fxIndex = index;
        com.mojang.logging.LogUtils.getLogger().info("[胀相] 施放 {} -> {}", SKILLS[index], target.getName().getString());
        switch (index) {
            case 0 -> { // 赤缚：定身
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6, false, false));
                target.setDeltaMovement(0, 0, 0); target.hurtMarked = true;
                hurt(target, 6f);
                fx(world, to, Vec3.ZERO, 3, 10);
            }
            case 1 -> { fx(world, position(), Vec3.ZERO, 6, 14); for (var t : near(6)) hurt(t, 12f); }        // 苅祓
            case 2 -> { // 百敛：强化
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1, false, false));
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1, false, false));
                fx(world, position().add(0, 1, 0), Vec3.ZERO, 2, 12);
            }
            case 3 -> { fx(world, eye, dir, 24, 12); for (var t : near(24)) hurt(t, 15f); }                   // 穿血
            case 4 -> { // 赤鳞跃动
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, false, false));
                getNavigation().moveTo(target, 1.6); fx(world, position(), dir, 5, 10);
            }
            case 5 -> { fx(world, to, dir, 4, 10); if (distanceToSqr(target) <= 16) hurt(target, 18f); }      // 血刃
            case 6 -> { for (int i = 0; i < 3; i++) bloodStar(world, to, 14f); }                               // 血星磊
            case 7 -> { // 血铠
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2, false, false));
                fx(world, position().add(0, 1, 0), Vec3.ZERO, 2.5f, 14);
            }
            case 8 -> { fx(world, position(), Vec3.ZERO, 8, 20); for (var t : near(8)) hurt(t, 20f); }         // 超新星
            case 9 -> { // 运河：拉近
                Vec3 pull = position().subtract(target.position()).normalize().scale(1.2);
                target.setDeltaMovement(pull.x, 0.4, pull.z); target.hurtMarked = true;
                fx(world, target.position(), position().subtract(target.position()).normalize(), 12, 10);
            }
            case 10 -> { // 翅王
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 2, false, false));
                addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 1, false, false));
                fx(world, position().add(0, 1.2, 0), Vec3.ZERO, 3, 14);
            }
            case 11 -> { // 血雾
                for (var t : near(8)) {
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, false));
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
                }
                fx(world, position(), Vec3.ZERO, 8, 18);
            }
            case 12 -> { // 血棘
                Vec3 flat = new Vec3(dir.x, 0, dir.z).normalize();
                for (int lane = -2; lane <= 2; lane++) {
                    Vec3 ray = flat.yRot((float) (lane * Math.PI / 12));
                    for (int i = 1; i <= 4; i++) {
                        Vec3 at = position().add(ray.scale(i * 2));
                        fx(world, at, Vec3.ZERO, 1, 10);
                        for (var t : near(at, 2.4)) { hurt(t, 8f); t.setDeltaMovement(t.getDeltaMovement().add(0, 0.7, 0)); t.hurtMarked = true; }
                    }
                }
            }
            case 13 -> { // 血镜
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 3, false, false));
                setHealth(Math.min(getMaxHealth(), getHealth() + 20f));
                fx(world, position().add(0, 1, 0), Vec3.ZERO, 2, 16);
            }
            case DOMAIN_INDEX -> { // 血之领域·胎海（300 秒）
                fx(world, position(), Vec3.ZERO, 30, 120);
                for (var t : near(30)) {
                    hurt(t, 10f);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, false));
                }
            }
            case DEATH_STAR_INDEX -> { // 极之番·死胎星（300 秒）
                fx(world, position().add(0, 2, 0), Vec3.ZERO, 20, 60);
                for (var t : near(20)) hurt(t, 40f);
            }
            case 16 -> { // 血洋
                for (var t : near(12)) if (t.position().distanceToSqr(position()) <= 144) hurt(t, 8f);
                fx(world, position(), Vec3.ZERO, 12, 24);
            }
            case 17 -> { // 血爆
                fx(world, to, Vec3.ZERO, 3, 14);
                for (var t : near(to, 3)) hurt(t, 16f);
            }
            case 18 -> { // 赤鳞跃动·载
                getNavigation().moveTo(target, 1.8);
                fx(world, to, Vec3.ZERO, 2, 20);
                if (distanceToSqr(target) <= 25) hurt(target, 12f);
            }
            default -> { // 血刃·千本
                fx(world, to, dir, 4, 10);
                if (distanceToSqr(target) <= 16) hurt(target, 14f);
                for (int i = 0; i < 6; i++) {
                    Vec3 at = to.add((getRandom().nextDouble() - .5) * 3, getRandom().nextDouble() * 1.5, (getRandom().nextDouble() - .5) * 3);
                    fx(world, eye, at.subtract(eye).normalize(), 10, 8);
                    hurt(target, 4f);
                }
            }
        }
    }

    private void bloodStar(ServerLevel world, Vec3 center, float damage) {
        Vec3 at = center.add((getRandom().nextDouble() - .5) * 6, 0, (getRandom().nextDouble() - .5) * 6);
        fx(world, at.add(0, 8, 0), new Vec3(0, -1, 0), 3, 20);
        for (var t : near(at, 4)) hurt(t, damage);
    }

    private void fx(ServerLevel world, Vec3 at, Vec3 dir, double size, int life) {
        int kind = 400 + Math.min(31, fxIndex);
        int shown = Math.max(40, life * 4);   // 血系特效的 life 单位很短，放大 4 倍并至少 2 秒
        Network.send(world, new Network.Effect(kind, getId(), at, dir, (float) size, shown, 0));
    }
    /** 记录当前施放的是第几招，让特效号随招式变化（400+index）。 */
    private void fx(ServerLevel world, int index, Vec3 at, Vec3 dir, double size, int life) {
        fxIndex = index;
        fx(world, at, dir, size, life);
    }

    /** 兜底索敌：附近敌怪优先，其次 24 格内挑衅过自己的玩家。 */
    private LivingEntity fallbackTarget() {
        var closest = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16),
            e -> e != this && e.isAlive() && e instanceof net.minecraft.world.entity.monster.Monster);
        LivingEntity best = null; double bestDist = Double.MAX_VALUE;
        for (var e : closest) { double d = e.distanceToSqr(this); if (d < bestDist) { bestDist = d; best = e; } }
        if (best != null) return best;
        for (var p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(24),
                p -> p.isAlive() && !p.isCreative() && p.getLastHurtByMob() == this)) return p;
        return null;
    }

    private java.util.List<LivingEntity> near(double radius) { return near(position(), radius); }

    private java.util.List<LivingEntity> near(Vec3 at, double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius),
            e -> e != this && e.isAlive() && !(e instanceof ChosoEntity)
                && !(e instanceof Player p && p.isCreative()));
    }

    private void hurt(LivingEntity target, float amount) { NpcCombat.hurt(this, target, amount); }
}
