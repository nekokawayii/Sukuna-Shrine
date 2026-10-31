package character.mahito;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

/**
 * 无为转变的「分身」/「尾巴」：
 * · 分身：玩家模型、友方，跟随主人并攻击敌对生物；血量对半（由主人侧属性修饰符结算）。
 * · 尾巴：同一实体，tail=true，体型更小、始终贴在玩家正后方，攻击面前的敌人（无击退）。
 */
public class MahitoCloneEntity extends Monster {
    public static final String OWNER = "MahitoOwner";
    public static final String TAIL = "MahitoTail";

    public MahitoCloneEntity(EntityType<? extends MahitoCloneEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20)
            .add(Attributes.ATTACK_DAMAGE, 6)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.FOLLOW_RANGE, 32)
            .add(Attributes.ARMOR, 4);
    }

    public boolean isTail() { return getPersistentData().getBoolean(TAIL); }

    public ServerPlayer owner() {
        if (!(level() instanceof ServerLevel sl)) return null;
        CompoundTag data = getPersistentData();
        if (!data.hasUUID(OWNER)) return null;
        UUID id = data.getUUID(OWNER);
        return sl.getServer().getPlayerList().getPlayer(id);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(6, new FollowOwnerGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
            e -> e != this && !(e instanceof MahitoCloneEntity)));
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerPlayer owner = owner();
        if (owner == null || !owner.isAlive() || owner.level() != level()) {
            discard();
            return;
        }
        if (distanceToSqr(owner) > 40 * 40) {
            teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }
        if (isTail()) {
            // 尾巴：贴到玩家正后方，只打面前的敌人（无击退）
            Vec3 back = new Vec3(-Math.sin(Math.toRadians(owner.getYRot())), 0, Math.cos(Math.toRadians(owner.getYRot())))
                .normalize().scale(1.6);
            Vec3 target = owner.position().add(back);
            moveTo(target.x, owner.getY(), target.z, owner.getYRot(), 0f);
            if (tickCount % 10 == 0) {
                LivingEntity front = level().getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(owner.position(), owner.position()).inflate(4),
                        e -> e != owner && e != this && e.isAlive() && !(e instanceof MahitoCloneEntity))
                    .stream()
                    .filter(e -> e.position().subtract(owner.position()).normalize()
                        .dot(owner.getLookAngle()) > 0.5)
                    .min(Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
                if (front != null) front.hurt(damageSources().mobAttack(this), 5f);
            }
        }
    }

    /** 跟随主人：尾巴以外保持 2~3 格距离。 */
    private static final class FollowOwnerGoal extends Goal {
        private final MahitoCloneEntity clone;
        FollowOwnerGoal(MahitoCloneEntity clone) { this.clone = clone; }
        @Override public boolean canUse() { return !clone.isTail() && clone.owner() != null; }
        @Override public void tick() {
            ServerPlayer owner = clone.owner();
            if (owner == null) return;
            if (clone.distanceToSqr(owner) > 16 && clone.getTarget() == null) {
                clone.getNavigation().moveTo(owner, 1.1);
            }
        }
    }
}
