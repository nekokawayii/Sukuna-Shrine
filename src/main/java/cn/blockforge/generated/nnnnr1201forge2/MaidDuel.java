package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;

/** 仅在安装女仆时加载，普通物品注册不再强制依赖女仆类。 */
public final class MaidDuel {
    private static final String DUEL_END="SukunaDuelEndUntil",DUEL_CLEANUP="SukunaDuelCleanup",DUEL_ACTIVE="SukunaDuelActiveR67";
    public static boolean duel(LivingEntity living){return living instanceof EntityMaid maid&&!EternalLove.settling(maid)&&maid.getMaidBauble().containsItem(EternalLove.TRUE_FALSE.get());}
    private static void clearTarget(EntityMaid maid){
        maid.setTarget(null);maid.setLastHurtByMob(null);maid.setLastHurtByPlayer(null);
        maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        maid.getBrain().eraseMemory(MemoryModuleType.HURT_BY);
        maid.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        maid.getNavigation().stop();
        var actor=MaidBridge.caster(maid);if(actor!=null)actor.aim=null;
    }
    private static void finishDuel(EntityMaid maid,ServerPlayer owner,boolean defeated){
        maid.getPersistentData().remove(DUEL_ACTIVE);
        maid.getPersistentData().putLong(DUEL_END,maid.level().getGameTime()+40);
        maid.getPersistentData().putBoolean(DUEL_CLEANUP,true);
        var inv=maid.getMaidBauble();
        for(int i=0;i<inv.getSlots();i++)if(inv.getStackInSlot(i).is(EternalLove.TRUE_FALSE.get())){
            ItemStack item=inv.extractItem(i,inv.getStackInSlot(i).getCount(),false);
            if(!item.isEmpty()){if(owner!=null)Journey.give(owner,item);else maid.spawnAtLocation(item);}
        }
        clearTarget(maid);
        if(defeated){maid.setHealth(1);maid.setHealth(maid.getMaxHealth());maid.invulnerableTime=40;}
        if(owner!=null)owner.sendSystemMessage(Component.literal(defeated?"真赝相爱：女仆败北，对战结束，女仆已恢复满血，饰品已返还。":"真赝相爱：对战结束，饰品已返还。"));
    }
    public static void returnDuelItem(ServerPlayer p){
        for(net.minecraft.server.level.ServerLevel level:p.server.getAllLevels())for(net.minecraft.world.entity.Entity entity:level.getAllEntities())
            if(entity instanceof EntityMaid maid&&p.getUUID().equals(maid.getOwnerUUID())&&duel(maid))finishDuel(maid,p,false);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void duelDamage(LivingDamageEvent event){
        if(event.getEntity().level().isClientSide||!(event.getEntity() instanceof EntityMaid maid)||event.getAmount()<=0)return;
        if(EternalLove.settling(maid)){event.setCanceled(true);return;}
        // 扣除护甲和吸收生命后，再判断是否会跌至一血。
        if(duel(maid)&&maid.getHealth()-event.getAmount()<=1){
            event.setCanceled(true);finishDuel(maid,maid.getOwner() instanceof ServerPlayer p?p:null,true);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void duelDeath(LivingDeathEvent event){
        if(event.getEntity().level().isClientSide||!(event.getEntity() instanceof EntityMaid maid))return;
        if(duel(maid)){event.setCanceled(true);finishDuel(maid,maid.getOwner() instanceof ServerPlayer p?p:null,true);}
        else if(EternalLove.settling(maid)){event.setCanceled(true);maid.setHealth(maid.getMaxHealth());}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void duelTick(LivingEvent.LivingTickEvent event){
        if(event.getEntity().level().isClientSide||!(event.getEntity() instanceof EntityMaid maid))return;
        boolean active=duel(maid);
        var data=maid.getPersistentData();
        if(active)data.putBoolean(DUEL_ACTIVE,true);
        else if(data.getBoolean(DUEL_ACTIVE))finishDuel(maid,maid.getOwner() instanceof ServerPlayer p?p:null,false);
        // r66旧档没有对决标记，也要清除遗留的主人锁定。
        var actor=MaidBridge.caster(maid);
        LivingEntity target=maid.getTarget();
        if(!active&&!EternalLove.settling(maid)&&((target instanceof net.minecraft.world.entity.player.Player&&actor!=null&&!CombatSettings.canHit(actor,target))||(actor!=null&&actor.aim instanceof net.minecraft.world.entity.player.Player&&!CombatSettings.canHit(actor,actor.aim))))finishDuel(maid,maid.getOwner() instanceof ServerPlayer p?p:null,false);
        if(active&&maid.getHealth()<=1)finishDuel(maid,maid.getOwner() instanceof ServerPlayer p?p:null,true);
        // 卸载或重进世界后，即使保护时间已经过期，也必须补完待执行的清理。
        if(!EternalLove.settling(maid)&&!data.getBoolean(DUEL_CLEANUP))return;
        clearTarget(maid);
        // 技能队列遍历结束后清理，避免反伤触发时修改正在遍历的集合。
        if(maid.getPersistentData().getBoolean(DUEL_CLEANUP)){
            maid.getPersistentData().remove(DUEL_CLEANUP);
            if(actor!=null){
                Skills.cancelCombat(actor);GuardianPlayerSkills.cancel(actor);
                if(maid.getOwner() instanceof ServerPlayer owner)SoulCombat.clearOwnedEffects(actor,owner);
            }
            if(maid.getOwner() instanceof ServerPlayer owner){Skills.protectAlly(owner,maid.getUUID());SoulCombat.protect(owner,maid);}
            for(String key:List.of("SukunaSoulFire","SukunaSoulFireOwner","SukunaSoulSlashOwner","SukunaSoulPeelUntil","SukunaSoulPeelOwner","SukunaNoHeal"))maid.getPersistentData().remove(key);
            maid.clearFire();
        }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer p)returnDuelItem(p);}
    private MaidDuel(){}
}
