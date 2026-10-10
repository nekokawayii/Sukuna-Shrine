package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;

/** 所有控制、层数和治疗限制均在服务端执行。 */
public final class SoulCombat {
    private static final UUID SLOW=UUID.fromString("479fd7ac-c9d2-4b0f-a01b-11a0a712cf53"), PEEL_SLOW=UUID.fromString("479fd7ac-c9d2-4b0f-a01b-11a0a712cf54"), PEEL_ATTACK=UUID.fromString("479fd7ac-c9d2-4b0f-a01b-11a0a712cf55");
    public static int layers(Player p){return Journey.nine(p)?(Curse.data(p).contains("SoulLayers")?Math.max(0,Curse.data(p).getInt("SoulLayers")):9):0;}
    public static int stacks(Player p){return Curse.active(p,"SoulStiffUntil")?Math.min(5,Curse.data(p).getInt("SoulStiffStacks")):0;}
    public static double cooldown(Player p){return (1+stacks(p)*0.10)*GuardianCombat.cooldown(p);}
    public static boolean suppressed(Player p){return p.getPersistentData().getLong("VoidCaptured")>p.level().getGameTime()||p.getPersistentData().getLong("SoulGapUntil")>p.level().getGameTime()||Curse.active(p,"SoulSuppressed")||Curse.active(p,"SoulGap")||p.getPersistentData().getLong("SukunaSoulPeelUntil")>p.level().getGameTime();}
    public static void suppress(ServerPlayer p,int ticks){Curse.status(p,"SoulSuppressed",ticks);Skills.closeDomain(p);}
    public static void stiff(ServerPlayer p){Curse.data(p).putInt("SoulStiffStacks",Math.min(5,stacks(p)+1));Curse.status(p,"SoulStiffUntil",100);applySlow(p);sync(p);}
    public static void peelTarget(ServerPlayer owner,net.minecraft.world.entity.LivingEntity target){
        if(target==owner||!CombatSettings.canHit(owner,target))return;
        long until=target.level().getGameTime()+100;target.getPersistentData().putLong("SukunaSoulPeelUntil",until);target.getPersistentData().putUUID("SukunaSoulPeelOwner",owner.getUUID());target.getPersistentData().putBoolean("SukunaNoHeal",true);if(target instanceof Player player)Curse.data(player).putLong("SoulPeelUntil",until);
        var speed=target.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null){speed.removeModifier(PEEL_SLOW);speed.addTransientModifier(new AttributeModifier(PEEL_SLOW,"灵魂剥离移速",-0.30,AttributeModifier.Operation.MULTIPLY_TOTAL));}
        var attack=target.getAttribute(Attributes.ATTACK_DAMAGE);if(attack!=null){attack.removeModifier(PEEL_ATTACK);attack.addTransientModifier(new AttributeModifier(PEEL_ATTACK,"灵魂剥离伤害",-0.30,AttributeModifier.Operation.MULTIPLY_TOTAL));}
        directTarget(owner,target,92,100);owner.sendSystemMessage(Component.literal("灵魂剥离 · 目标进入虚弱 5 秒，无法治疗与使用术式"));
    }
    private static void directTarget(ServerPlayer owner,net.minecraft.world.entity.LivingEntity target,int kind,int life){Network.send(owner.serverLevel(),new Network.Effect(kind,target.getId(),target.getBoundingBox().getCenter(),net.minecraft.world.phys.Vec3.ZERO,1,life,owner.getId()));}
    public static void peel(ServerPlayer p,boolean lethal){int before=layers(p);Curse.data(p).putInt("SoulLayers",Math.max(0,before-1));sync(p);
        if(lethal&&before<3&&!p.isCreative()){Curse.status(p,"SoulExecution",2);p.hurt(p.damageSources().genericKill(),Float.MAX_VALUE);}
    }
    public static void gap(net.minecraft.world.entity.LivingEntity target,int ticks,int boss){
        ServerPlayer caster=MaidBridge.progression(target);
        if(caster!=null){Curse.status(caster,"SoulGap",ticks);suppress(caster,ticks);direct(caster,82,ticks,boss);}
        else{target.getPersistentData().putLong("SoulGapUntil",target.level().getGameTime()+ticks);target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,ticks,2));}
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS,ticks));
    }
    public static void soulHit(ServerPlayer owner,net.minecraft.world.entity.LivingEntity target){target.getPersistentData().putBoolean("SukunaNoHeal",true);}
    public static void iwate(ServerPlayer p,int ticks){Curse.status(p,"Iwate",ticks);}
    private static void direct(ServerPlayer p,int kind,int life,long seed){if(MaidBridge.isCaster(p))return;Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new Network.Effect(kind,p.getId(),p.position(),net.minecraft.world.phys.Vec3.ZERO,layers(p),life,seed));}
    public static void sync(ServerPlayer p){direct(p,89,stacks(p),layers(p));}
    private static void applySlow(ServerPlayer p){var a=MaidBridge.body(p).getAttribute(Attributes.MOVEMENT_SPEED);if(a==null)return;int n=stacks(p);var old=a.getModifier(SLOW);if(old!=null&&Math.abs(old.getAmount()+n*0.15)<0.001)return;a.removeModifier(SLOW);if(n>0)a.addTransientModifier(new AttributeModifier(SLOW,"灵魂僵直",-n*0.15,AttributeModifier.Operation.MULTIPLY_TOTAL));}
    public static void clearOwnedEffects(ServerPlayer owner,net.minecraft.world.entity.LivingEntity target){
        Skills.protectAlly(owner,target.getUUID());protect(owner,target);
        var data=target.getPersistentData();
        if(data.hasUUID("SukunaSoulFireOwner")&&data.getUUID("SukunaSoulFireOwner").equals(owner.getUUID())){data.remove("SukunaSoulFire");data.remove("SukunaSoulFireOwner");data.remove("SukunaSoulFireMaid");target.clearFire();}
        if(data.hasUUID("SukunaSoulSlashOwner")&&data.getUUID("SukunaSoulSlashOwner").equals(owner.getUUID()))data.remove("SukunaSoulSlashOwner");
        if(!data.getBoolean("SukunaSoulFire")&&!data.hasUUID("SukunaSoulSlashOwner")&&data.getLong("SukunaSoulPeelUntil")<=target.level().getGameTime())data.remove("SukunaNoHeal");
    }
    public static void protect(ServerPlayer owner,net.minecraft.world.entity.LivingEntity target){
        var data=target.getPersistentData();
        if(data.hasUUID("SukunaSoulPeelOwner")&&data.getUUID("SukunaSoulPeelOwner").equals(owner.getUUID())){data.remove("SukunaSoulPeelUntil");data.remove("SukunaSoulPeelOwner");if(target instanceof Player p)Curse.data(p).remove("SoulPeelUntil");clearPeel(target);}
    }
    private static void clearPeel(net.minecraft.world.entity.LivingEntity target){
        var data=target.getPersistentData();if(data.getLong("SukunaSoulPeelUntil")>target.level().getGameTime())return;
        var speed=target.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(PEEL_SLOW);var attack=target.getAttribute(Attributes.ATTACK_DAMAGE);if(attack!=null)attack.removeModifier(PEEL_ATTACK);
        data.remove("SukunaSoulPeelUntil");data.remove("SukunaSoulPeelOwner");
        if(!data.getBoolean("SukunaSoulFire")&&!data.hasUUID("SukunaSoulSlashOwner"))data.remove("SukunaNoHeal");
    }
    @SubscribeEvent public static void healing(LivingHealEvent e){if(e.getEntity().getPersistentData().getBoolean("SukunaNoHeal")||e.getEntity().getPersistentData().getLong("SoulGapUntil")>e.getEntity().level().getGameTime()){e.setCanceled(true);return;}ServerPlayer caster=MaidBridge.progression(e.getEntity());if(caster!=null){if(Curse.active(caster,"SoulGap"))e.setCanceled(true);else if(Curse.active(caster,"SoulHealHalf"))e.setAmount(e.getAmount()*0.5f);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void death(LivingDeathEvent e){ServerPlayer p=MaidBridge.progression(e.getEntity());if(p==null||!Journey.nine(p)||Curse.active(p,"SoulExecution")||e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return;if(layers(p)>=3){e.setCanceled(true);Curse.data(p).putInt("SoulLayers",layers(p)-1);e.getEntity().setHealth(e.getEntity().getMaxHealth()*0.3f);e.getEntity().invulnerableTime=30;sync(p);p.sendSystemMessage(Component.literal("咒胎受肉挡下致命伤，剩余 "+layers(p)+" 层"));}}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%5!=0)return;applySlow(p);clearPeel(p);if(p.tickCount%20==0)sync(p);}
    @SubscribeEvent public static void entities(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||e.getServer().getTickCount()%5!=0)return;for(ServerLevel level:e.getServer().getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.LivingEntity living)clearPeel(living);}
    static void testReset(ServerPlayer p){clearTransient(p);Curse.data(p).putInt("SoulLayers",Journey.nine(p)?9:0);applySlow(p);clearPeel(p);sync(p);}
    public static void clearTransient(Player p){for(String key:new String[]{"SoulGap","SoulSuppressed","SoulStiffUntil","SoulStiffStacks","Iwate","SoulHealHalf","SoulExecution","SoulPeelUntil","PureLandNoHeal","GuardianPoisonUntil","PlayerGuardianPoisonUntil"})Curse.data(p).remove(key);p.getPersistentData().remove("SukunaSoulPeelUntil");}
    @SubscribeEvent public static void clone(PlayerEvent.PlayerRespawnEvent e){clearTransient(e.getEntity());if(e.getEntity() instanceof ServerPlayer p){applySlow(p);sync(p);}}
    @SubscribeEvent public static void changed(PlayerEvent.PlayerChangedDimensionEvent e){clearTransient(e.getEntity());}
}
