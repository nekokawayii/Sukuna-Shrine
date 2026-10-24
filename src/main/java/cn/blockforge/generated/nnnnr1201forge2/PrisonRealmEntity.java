package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** 不可被普通攻击、爆炸或活塞摧毁的地面咒物；只有圣光的显式解封路径可破坏。 */
public final class PrisonRealmEntity extends Entity {
    private static final EntityDataAccessor<Boolean> OPEN=SynchedEntityData.defineId(PrisonRealmEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FRAME=SynchedEntityData.defineId(PrisonRealmEntity.class,EntityDataSerializers.INT);
    private UUID owner;
    public PrisonRealmEntity(EntityType<? extends PrisonRealmEntity> type,Level level){super(type,level);setNoGravity(true);setInvulnerable(true);blocksBuilding=true;noCulling=true;}
    public void configure(UUID id){owner=id;}
    public UUID owner(){return owner;}
    public boolean open(){return entityData.get(OPEN);}
    public int frame(){return entityData.get(FRAME);}
    public void open(boolean value){entityData.set(OPEN,value);}
    @Override protected void defineSynchedData(){entityData.define(OPEN,false);entityData.define(FRAME,0);}
    @Override public void tick(){super.tick();if(!level().isClientSide){int frame=frame();entityData.set(FRAME,open()?Math.min(PrisonRules.OPEN_TICKS,frame+1):Math.max(0,frame-2));if(tickCount==1)PrisonSystem.registerRealm(this);}}
    @Override public boolean isPickable(){return true;}
    @Override public boolean canBeCollidedWith(){return true;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean hurt(DamageSource source,float amount){return false;}
    @Override public void kill(){} // 普通kill也不能丢掉仍有囚徒的封印载体。
    @Override public InteractionResult interact(Player player,InteractionHand hand){
        // 手持邪去悔天梯时必须放行，不吞掉道具右键。
        if(hand!=InteractionHand.MAIN_HAND||!player.getItemInHand(hand).isEmpty())return InteractionResult.PASS;
        if(player instanceof ServerPlayer p&&!PrisonSystem.locked(p)){
            if(!p.getUUID().equals(owner)){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("只有放置者可以收起狱门僵"));return InteractionResult.CONSUME;}
            if(open()||frame()>0){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("请先闭门，并等待狱门僵合拢后再空手右键收起"));return InteractionResult.CONSUME;}
            PrisonSystem.registerRealm(this);PrisonStore store=PrisonStore.get(p.server);
            CompoundTag snapshot=store.pack(getUUID());
            if(snapshot==null){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("封印状态已改变，不能重复收起"));return InteractionResult.CONSUME;}
            // 空的主手直接接收唯一封印物品，不走可能掉落或复制的满背包分支。
            p.setItemInHand(hand,PrisonRealmItem.packed(snapshot));PrisonInventory.sync(p);discard();
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("狱门僵已收起 · 内部生物与封印不变，重新放下后可用邪去悔天梯解封"));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override protected void addAdditionalSaveData(CompoundTag t){if(owner!=null)t.putUUID("Owner",owner);t.putBoolean("Open",open());t.putInt("Frame",frame());}
    @Override protected void readAdditionalSaveData(CompoundTag t){owner=t.hasUUID("Owner")?t.getUUID("Owner"):null;entityData.set(OPEN,t.getBoolean("Open"));entityData.set(FRAME,Math.max(0,Math.min(PrisonRules.OPEN_TICKS,t.getInt("Frame"))));}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
