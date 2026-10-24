package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.phys.Vec3;

/** 仅绘制服务端确认的存活片段；断掉的一段绝不由客户端继续预测。 */
public final class SlashTerrainClient {
    private record Flight(SlashTerrain.Frame frame,long born){}
    private record Scar(Network.Effect effect,long born){}
    private record Key(long id,int batch){}
    private static final Map<Key,Flight> flights=new LinkedHashMap<>();
    private static final Deque<Scar> scars=new ArrayDeque<>();
    private static long tick;
    public static void receive(SlashTerrain.Frame frame){
        if(frame.batch()==0)flights.keySet().removeIf(key->key.id==frame.id()&&key.batch>=frame.batches());
        Key key=new Key(frame.id(),frame.batch());
        if(frame.lines().isEmpty()){flights.keySet().removeIf(k->k.id==frame.id());return;}
        if(flights.size()>=4096&&!flights.containsKey(key))flights.remove(flights.keySet().iterator().next());
        flights.put(key,new Flight(frame,tick));
    }
    public static void scar(Network.Effect effect){
        if(scars.size()>=2048)scars.removeFirst();scars.addLast(new Scar(effect,tick));
        var level=Minecraft.getInstance().level;if(level==null)return;
        Vec3 normal=Vec3.atLowerCornerOf(Direction.from3DDataValue((int)effect.size()).getNormal());
        for(int i=0;i<6;i++){Vec3 at=effect.position().add(effect.direction().scale(i/5.0)).add(normal.scale(.04));level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,at.x,at.y,at.z,normal.x*.05,.035+normal.y*.05,normal.z*.05);}
    }
    public static void collect(Vec3 center,double radius,List<Vec3> out){
        flights.values().removeIf(f->{boolean close=f.frame.lines().stream().anyMatch(s->s.a().add(s.step()).lerp(s.b().add(s.step()),.5).distanceToSqr(center)<=radius*radius);if(close&&out.size()<6000)out.add(f.frame.lines().get(0).a());return close;});
    }
    public static void tick(){
        tick++;flights.values().removeIf(f->tick-f.born>4);
        var level=Minecraft.getInstance().level;
        scars.removeIf(s->tick-s.born>=s.effect.life()||level==null||level.getBlockState(BlockPos.of(s.effect.seed())).isAir());
    }
    public static void clear(){flights.clear();scars.clear();tick=0;}
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 camera,float partial){
        for(Flight f:flights.values()){
            double progress=Math.min(1,Math.max(0,tick-f.born+partial));
            SlashAppearance.use(new Network.Effect(SlashFormRules.FLIGHT,f.frame.owner(),Vec3.ZERO,Vec3.ZERO,1,4,0).wallpaper(f.frame.wallpaperOwner(),f.frame.desktopSlashes()));
            SlashAppearance.age(4);
            for(var s:f.frame.lines()){
                Vec3 move=s.step().scale(progress);SlashStyle.draw(b,ps,s.a().add(move),s.b().add(move),.13,1,camera);
            }
        }
        SlashAppearance.reset();
        for(Scar scar:scars){
            var e=scar.effect;float alpha=(float)Math.min(1,(e.life()-(tick-scar.born+partial))/30.0);
            Vec3 normal=Vec3.atLowerCornerOf(Direction.from3DDataValue((int)e.size()).getNormal());
            Vec3 a=e.position(),z=a.add(e.direction()),mid=a.lerp(z,.5),side=normal.cross(e.direction()).normalize().scale(.025);
            Client.quad(b,ps,a,mid.add(side),z,mid.subtract(side),((int)(220*alpha)<<24)|0x15101A);
            if(tick-scar.born<5){Vec3 lift=normal.scale(.001);Client.quad(b,ps,a.add(lift),mid.add(side.scale(.4)).add(lift),z.add(lift),mid.subtract(side.scale(.4)).add(lift),((int)(150*alpha)<<24)|0xFFD0A0);}
        }
    }
    private SlashTerrainClient(){}
}
