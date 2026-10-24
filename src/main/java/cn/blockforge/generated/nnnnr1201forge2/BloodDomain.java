package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 胎海封闭结界：不修改地形，范围与所有命中由服务器负责。 */
public final class BloodDomain {
    public static final int FX_DOMAIN=430,FX_CLOSE=431;
    private static final List<Domain> domains=new ArrayList<>();
    private static class Domain {
        final ServerPlayer owner;final ServerLevel level;final Vec3 center;final long end;final long id;
        final Map<UUID,Vec3> captives=new HashMap<>();long lastHeal;
        Domain(ServerPlayer p,Vec3 center){owner=p;level=p.serverLevel();this.center=center;end=level.getGameTime()+BloodRules.DOMAIN_TICKS;id=++serial;}
    }
    private static long serial;
    public static boolean active(ServerPlayer p){return domains.stream().anyMatch(d->d.owner==p);}
    public static boolean free(ServerPlayer p){return domains.stream().anyMatch(d->d.owner==p&&d.level==p.level()&&d.end>d.level.getGameTime()&&BloodRules.inside(p.position().distanceToSqr(d.center)));}
    public static void open(ServerPlayer p,Vec3 center,boolean echo){
        if(!echo)close(p);if(domains.size()>=32)return;
        Domain d=new Domain(p,center);domains.add(d);sync(d);
        for(LivingEntity t:d.level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(BloodRules.DOMAIN_RADIUS),e->e==MaidBridge.body(p)||CombatSafety.canAffect(p,e)))if(BloodRules.inside(t.position().distanceToSqr(center)))d.captives.put(t.getUUID(),t.position());
    }
    private static void sync(Domain d){Network.send(d.level,new Network.Effect(FX_DOMAIN,d.owner.getId(),d.center,Vec3.ZERO,100,(int)Math.max(1,d.end-d.level.getGameTime()),d.id));}
    public static void close(ServerPlayer p){for(Iterator<Domain> it=domains.iterator();it.hasNext();){Domain d=it.next();if(d.owner!=p)continue;Network.send(d.level,new Network.Effect(FX_CLOSE,p.getId(),d.center,Vec3.ZERO,0,0,d.id));it.remove();}}
    public static void clear(){for(Domain d:List.copyOf(domains))close(d.owner);domains.clear();}
    public static void onHit(ServerPlayer p,LivingEntity target){if(DeathStar.anemic(p)||!free(p))return;for(Domain d:domains)if(d.owner==p&&BloodRules.inside(target.position().distanceToSqr(d.center))&&d.lastHeal+4<=d.level.getGameTime()){d.lastHeal=d.level.getGameTime();MaidBridge.body(p).heal(Math.max(.2f,MaidBridge.body(p).getMaxHealth()*.005f));BloodArts.fx(p,BloodArts.FX_DRAIN,target.getBoundingBox().getCenter(),p.getEyePosition(),1,12,0);break;}}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(Domain d:List.copyOf(domains)){
            long now=d.level.getGameTime();ServerPlayer p=d.owner;
            if(!p.isAlive()||p.isRemoved()||!(p instanceof MaidBridge.Actor)&&p.hasDisconnected()||p.level()!=d.level||MaidBridge.body(p).level()!=d.level||now>=d.end||PrisonSystem.locked(MaidBridge.body(p))){close(p);continue;}
            if(!Curse.canUseTechnique(p)||SoulCombat.suppressed(p)){close(p);continue;}
            if(now%40==0)sync(d);
            var nearby=d.level.getEntitiesOfClass(LivingEntity.class,new AABB(d.center,d.center).inflate(103),e->e.isAlive()&&!e.isSpectator());
            for(LivingEntity t:nearby){if(t instanceof Player player&&player.isCreative()&&t!=MaidBridge.body(p))continue;if(t!=MaidBridge.body(p)&&!CombatSafety.canAffect(p,t)){d.captives.remove(t.getUUID());continue;}if(BloodRules.inside(t.position().distanceToSqr(d.center)))d.captives.put(t.getUUID(),t.position());}
            for(var entry:List.copyOf(d.captives.entrySet())){
                Entity entity=d.level.getEntity(entry.getKey());if(!(entity instanceof LivingEntity t)||!t.isAlive()){d.captives.remove(entry.getKey());continue;}
                if(t!=MaidBridge.body(p)&&!CombatSafety.canAffect(p,t)){d.captives.remove(entry.getKey());continue;}
                if(!BloodRules.inside(t.position().distanceToSqr(d.center))){Vec3 at=entry.getValue();t.teleportTo(at.x,at.y,at.z);t.setDeltaMovement(Vec3.ZERO);t.fallDistance=0;t.hurtMarked=true;}
                if(now%20==Math.floorMod(t.getId(),20)&&t!=MaidBridge.body(p)&&!DeathStar.anemic(p)&&!DomainClash.active(p)&&!DomainClash.active(t)){int rays=1+BloodArts.rank(p);for(int i=0;i<rays;i++){double a=i*Math.PI*2/rays;Vec3 start=t.getEyePosition().add(Math.cos(a)*i,10,Math.sin(a)*i);BloodArts.pierce(p,start,t.getBoundingBox().getCenter().subtract(start).normalize(),false,true,t);}}
            }
        }
    }
    @SubscribeEvent public static void teleport(EntityTeleportEvent event){
        if(!event.isCancelable()||event.getEntity().level().isClientSide)return;
        for(Domain d:domains)if(d.level==event.getEntity().level()&&d.captives.containsKey(event.getEntity().getUUID())&&event.getEntity() instanceof LivingEntity target&&(target==MaidBridge.body(d.owner)||CombatSafety.canAffect(d.owner,target))&&!BloodRules.inside(event.getTarget().distanceToSqr(d.center))){event.setCanceled(true);return;}
    }
    @SubscribeEvent public static void dimension(EntityTravelToDimensionEvent event){
        if(event.getEntity().level().isClientSide)return;
        for(Domain d:domains)if(d.level==event.getEntity().level()&&d.captives.containsKey(event.getEntity().getUUID())&&event.getEntity() instanceof LivingEntity target&&(target==MaidBridge.body(d.owner)||CombatSafety.canAffect(d.owner,target))&&event.getEntity().isAlive()&&!PrisonSystem.locked(event.getEntity())){event.setCanceled(true);return;}
    }
    private BloodDomain(){}
}
