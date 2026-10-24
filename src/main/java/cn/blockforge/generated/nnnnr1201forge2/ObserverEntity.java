package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 「观测者」：站在纯白空间中央的假玩家。不动、不受伤、只转头盯着镜头，靠 action 播挥刀。 */
public class ObserverEntity extends Monster {
    /** 0 待机，1 挥刀（对镜头），2 抬头凝视。 */
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(ObserverEntity.class,EntityDataSerializers.INT);
    public ObserverEntity(EntityType<? extends Monster> type,Level level){
        super(type,level);
        setPersistenceRequired();setNoAi(true);setInvulnerable(true);setNoGravity(true);setSilent(false);xpReward=0;
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ACTION,0);}
    public int action(){return entityData.get(ACTION);}
    public void action(int value){entityData.set(ACTION,value);}
    @Override public void tick(){
        super.tick();
        if(!level().isClientSide&&tickCount%40==0&&getPersistentData().hasUUID("HighOwner")){
            var owner=((net.minecraft.server.level.ServerLevel)level()).getServer().getPlayerList().getPlayer(getPersistentData().getUUID("HighOwner"));
            if(owner==null||!ObserverRitual.active(owner)){discard();return;}
        }
        setDeltaMovement(Vec3.ZERO);setNoGravity(true);setInvulnerable(true);
    }
    @Override public boolean hurt(DamageSource source,float amount){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean canBeCollidedWith(){return false;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected boolean shouldDespawnInPeaceful(){return false;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("ObserverAction",action());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);action(tag.getInt("ObserverAction"));}
}
