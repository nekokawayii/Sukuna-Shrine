package character.culling;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.phys.AABB;

/**
 * 死灭洄游专用的「羂索（夏油杰）」（r230）。
 *
 * <p>用户要求：界内不要再刷 Boss 夏油杰和自定义咒灵（太卡），改成
 * <b>一个独立的、只会召唤强化原版生物的羂索</b>。所以这里刻意做成极轻的
 * {@link PathfinderMob}：没有领域、没有轰击、没有自定义模型动画，
 * 只有「走路 / 近战 / 每 5 秒召唤一波强化原版怪」。
 */
public final class CullingGetoEntity extends PathfinderMob {
    /** 召唤间隔（刻）与每批数量。 */
    private static final int SUMMON_INTERVAL = 100, SUMMON_BATCH = 2;
    /** 同一时间附近最多保留多少只召唤物。 */
    private static final int SUMMON_CAP = 8;
    private static final EntityType<?>[] POOL = {
        EntityType.ZOMBIE, EntityType.HUSK, EntityType.SKELETON,
        EntityType.SPIDER, EntityType.CREEPER, EntityType.STRAY
    };

    public CullingGetoEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 300)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 8)
            .add(Attributes.FOLLOW_RANGE, 32)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % SUMMON_INTERVAL != 0) return;
        if (!(level() instanceof ServerLevel level)) return;
        int alive = level.getEntitiesOfClass(Mob.class, new AABB(blockPosition()).inflate(40),
            m -> m.isAlive() && m.getPersistentData().getBoolean("CullingSummon")).size();
        if (alive >= SUMMON_CAP) return;
        for (int i = 0; i < SUMMON_BATCH; i++) summon(level);
    }

    private void summon(ServerLevel level) {
        EntityType<?> type = POOL[level.random.nextInt(POOL.length)];
        if (!(type.create(level) instanceof Mob mob)) return;
        BlockPos at = blockPosition().offset(level.random.nextInt(9) - 4, 0, level.random.nextInt(9) - 4);
        mob.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, level.random.nextFloat() * 360f, 0);
        level.addFreshEntity(mob);
        level.getScoreboard().removePlayerFromTeam(mob.getScoreboardName());
        mob.getPersistentData().putBoolean("CullingEnemy", true);
        mob.getPersistentData().putBoolean("CullingSummon", true);
        mob.getPersistentData().putString("CullingKind", "curse");
        mob.getPersistentData().putLong("ExpeditionDespawnAt", level.getGameTime() + 9600);
        // 强化很多：血量按诅咒基准 ×5、攻击 ×2、移速 +15%
        int stage = getPersistentData().getInt("CullingStage");
        double hp = 90 * (1 + .25 * stage) * 5;
        var max = mob.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) { max.setBaseValue(hp); mob.setHealth((float) hp); }
        var atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(Math.min(200, 12 * (1 + .2 * stage)));
        var spd = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(spd.getBaseValue() * 1.15);
        mob.setCustomName(Component.literal("强化召唤物 · " + mob.getName().getString()));
        mob.setCustomNameVisible(false);
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }
}
