package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 「纯客户端特效被注入的那一瞬间变成的实体」。
 * <p>不渲染（视觉由 {@code TodoVisuals} 的咒力环负责）、无碰撞、不可攻击；
 * 作用就是给那一团特效一个真实坐标，让不义游戏可以跟它换位。换位消耗后立刻 discard。
 */
public class TodoAnchorEntity extends Entity {
    public TodoAnchorEntity(EntityType<? extends TodoAnchorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
        setInvulnerable(true);
        setSilent(true);
    }

    @Override public void tick() {
        super.tick();
        // 兜底：标记被清掉/玩家再也没用它，也不会在世界里留下永久实体（10 分钟）
        if (!level().isClientSide() && tickCount > 20 * 60 * 10) discard();
    }

    @Override protected void defineSynchedData() {}

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override public boolean canBeCollidedWith() { return false; }

    @Override public boolean isPickable() { return false; }

    @Override public boolean isAttackable() { return false; }
}
