package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.world.phys.Vec3;

public final class SlashFormVisuals {
    private record Visual(Network.Effect e,long born){}
    private static final List<Visual> visuals=new ArrayList<>();private static long tick;
    public static void collect(Vec3 center,double radius,List<Vec3> out){visuals.removeIf(v->{Vec3 at=v.e.position().add(v.e.direction().scale(.5));if(v.e.kind()!=SlashFormRules.STILL)at=at.add(SukunaBranches.unpack(v.e.seed()).scale(SlashFormRules.travel(tick-v.born,v.e.kind()==SlashFormRules.ACCELERATING)));if(at.distanceToSqr(center)>radius*radius)return false;if(out.size()<6000)out.add(at);return true;});}
    public static void clear(){visuals.clear();tick=0;}
    public static void stop(Network.Effect e){visuals.removeIf(v->v.e.owner()==e.owner()&&v.e.position().distanceToSqr(e.position())<1e-8&&v.e.direction().distanceToSqr(e.direction())<1e-8&&v.e.seed()==e.seed());}
    public static void receive(Network.Effect e){if(visuals.size()<12000)visuals.add(new Visual(e,tick));}
    public static void tick(){tick++;visuals.removeIf(v->tick-v.born>=v.e.life());}
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 camera,float partial){
        for(Visual v:visuals){var e=v.e;SlashAppearance.use(e);double age=tick-v.born+partial;
            if(e.kind()==DismantleMotionRules.RING_FX){
                double radius=DismantleMotionRules.ringRadius(age);int alpha=(int)(255*Math.min(1,(e.life()-age)/4));
                for(int i=0;i<DismantleMotionRules.RING_SEGMENTS;i++){double a=i*Math.PI*2/DismantleMotionRules.RING_SEGMENTS,z=(i+1)*Math.PI*2/DismantleMotionRules.RING_SEGMENTS;Vec3 from=e.position().add(Math.cos(a)*radius,0,Math.sin(a)*radius),to=e.position().add(Math.cos(z)*radius,0,Math.sin(z)*radius);SlashStyle.draw(b,ps,from,to,.12,alpha/255f,camera);}continue;
            }
            Vec3 move=e.kind()==SlashFormRules.STILL?Vec3.ZERO:SukunaBranches.unpack(e.seed()).scale(SlashFormRules.travel(age,e.kind()==SlashFormRules.ACCELERATING));Vec3 a=e.position().add(move),z=a.add(e.direction());
            if(e.kind()==SlashFormRules.WORLD_FX){Vec3 axis=e.direction().normalize(),center=a.lerp(z,0.5);double along=camera.subtract(center).dot(axis);center=center.add(axis.scale(along));double visible=Math.max(512,net.minecraft.client.Minecraft.getInstance().options.getEffectiveRenderDistance()*32.0);a=center.subtract(axis.scale(visible));z=center.add(axis.scale(visible));}
            float fade=(float)Math.min(1,(e.life()-age)/4);
            if(e.kind()==SlashFormRules.WORLD_FX)WallpaperSlash.draw(b,ps,a,z,e.direction().cross(SukunaBranches.unpack(e.seed())),fade,camera,e.wallpaperOwner());
            else SlashStyle.draw(b,ps,a,z,.16,fade,camera);
        }
        FreehandClient.preview(b,ps,camera);
    }
    private SlashFormVisuals(){}
}
