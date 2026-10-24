package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 舌头是真正的双向定身，不只给缓慢效果；结束、死亡、收回时立即释放。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class ShadowBindings {
    private record Hold(ShadowEntity frog,LivingEntity target,Vec3 frogAt,Vec3 targetAt,long end){}
    private static final Map<UUID,Hold> holds=new HashMap<>();
    public static void bind(ShadowEntity frog,LivingEntity target,int ticks){holds.put(frog.getUUID(),new Hold(frog,target,frog.position(),target.position(),frog.level().getGameTime()+ticks));}
    public static boolean boundBy(LivingEntity target,ShadowEntity frog){Hold h=holds.get(frog.getUUID());return h!=null&&h.target==target;}
    public static void release(ShadowEntity frog){holds.remove(frog.getUUID());}
    private static void pin(LivingEntity e,Vec3 at){e.setDeltaMovement(Vec3.ZERO);e.fallDistance=0;if(e instanceof Mob mob)mob.getNavigation().stop();if(e.position().distanceToSqr(at)>.001){if(e instanceof ServerPlayer p)p.connection.teleport(at.x,at.y,at.z,p.getYRot(),p.getXRot());else e.teleportTo(at.x,at.y,at.z);}e.hurtMarked=true;}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(var it=holds.values().iterator();it.hasNext();){Hold h=it.next();if(!h.frog.isAlive()||h.frog.isRemoved()||!h.target.isAlive()||h.frog.level()!=h.target.level()||h.frog.level().getGameTime()>=h.end||!h.frog.validTarget(h.target)||h.frog.distanceToSqr(h.target)>16*16){it.remove();continue;}pin(h.frog,h.frogAt);pin(h.target,h.targetAt);}
    }
    @SubscribeEvent public static void teleport(EntityTeleportEvent event){if(event.isCancelable())for(Hold h:holds.values())if(event.getEntity()==h.target||event.getEntity()==h.frog){event.setCanceled(true);return;}}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){holds.clear();}
    private ShadowBindings(){}
}
