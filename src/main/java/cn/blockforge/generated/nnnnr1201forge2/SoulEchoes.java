package cn.blockforge.generated.nnnnr1201forge2;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
/** 金色光点按玩家附近短时存在；只能在服务端距离校验通过后捕捉一次。 */
public final class SoulEchoes {
    private record Echo(ServerLevel level,Vec3 pos,long expires){}
    private static final List<Echo> ECHOES=new ArrayList<>();
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%20!=0||p.isSpectator())return;
        ServerLevel l=p.serverLevel();if(!l.dimension().location().toString().equals("sukuna:soul_corridor"))return;
        ECHOES.removeIf(a->a.expires< a.level.getGameTime());
        if(p.tickCount%200==0&&ECHOES.stream().filter(a->a.level==l&&a.pos.distanceToSqr(p.position())<1024).count()<3&&ECHOES.size()<128){
            Vec3 at=p.position().add(l.random.nextInt(13)-6,1+l.random.nextDouble()*2,l.random.nextInt(13)-6);
            if(l.isEmptyBlock(net.minecraft.core.BlockPos.containing(at)))ECHOES.add(new Echo(l,at,l.getGameTime()+1200));
        }
        var dust=new DustParticleOptions(new org.joml.Vector3f(1,.78f,.16f),1.1f);
        for(Echo a:ECHOES)if(a.level==l&&a.pos.distanceToSqr(p.position())<2304)l.sendParticles(p,dust,false,a.pos.x,a.pos.y,a.pos.z,12,.12,.15,.12,0);
    }
    public static boolean capture(ServerPlayer p){for(var it=ECHOES.iterator();it.hasNext();){Echo a=it.next();if(a.level==p.level()&&a.expires>p.level().getGameTime()&&a.pos.distanceToSqr(p.getEyePosition())<=25){it.remove();return true;}}return false;}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){ECHOES.clear();}
}
