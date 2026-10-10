package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import static cn.blockforge.generated.nnnnr1201forge2.Client.*;

/** 紫芯黑边斩击、跟随目标的裂隙网络、黑火和汇聚球。 */
public final class VoidTechniqueClient {
    private record Visual(Network.Effect e,long born){}
    private static final Map<String,Visual> wounds=new LinkedHashMap<>();
    private static final List<Visual> effects=new ArrayList<>();
    private static long tick;
    private static boolean blueTint;
    public static int tint(int color){if(!blueTint)return color;int r=color>>16&255,g=color>>8&255,b=color&255,max=Math.max(r,Math.max(g,b));if(max<35||max-Math.min(r,Math.min(g,b))<12)return color;return color&0xFF000000|(max/5)<<16|(int)(max*0.78)<<8|max;}
    public static void collect(Vec3 center,double radius,List<Vec3> out){effects.removeIf(v->{if(v.e.position().distanceToSqr(center)>radius*radius)return false;if(out.size()<6000)out.add(v.e.position());return true;});wounds.values().removeIf(v->{if(v.e.position().distanceToSqr(center)>radius*radius)return false;if(out.size()<6000)out.add(v.e.position());return true;});}
    public static void clear(){wounds.clear();effects.clear();tick=0;}
    public static void tick(){tick++;wounds.values().removeIf(v->tick-v.born>v.e.life());effects.removeIf(v->tick-v.born>v.e.life());}
    public static void receive(Network.Effect e){
        String key=e.owner()+"/"+e.seed();
        if(e.kind()==113){if(wounds.size()<2048||wounds.containsKey(key))wounds.put(key,new Visual(e,tick));return;}
        if(e.kind()==114){wounds.remove(key);return;}
        if(effects.size()>=768)effects.remove(0);effects.add(new Visual(e,tick));
    }
    public static void cut(BufferBuilder b,PoseStack ps,Vec3 p,Vec3 axis,float length,float fade,Vec3 cam){cut(b,ps,p,axis,length,fade,cam,false);}
    public static void cut(BufferBuilder b,PoseStack ps,Vec3 p,Vec3 axis,float length,float fade,Vec3 cam,boolean blue){
        axis=axis.normalize();SlashStyle.draw(b,ps,p.subtract(axis.scale(length/2)),p.add(axis.scale(length/2)),Math.min(.4,Math.max(.065,length*.018)),fade,cam);
    }
    private static void sphere(BufferBuilder b,PoseStack ps,Vec3 p,double radius,int color){
        for(int y=0;y<12;y++)for(int x=0;x<24;x++){
            double a=x*Math.PI/12,z=(x+1)*Math.PI/12,h=-Math.PI/2+y*Math.PI/12,H=h+Math.PI/12;
            quad(b,ps,point(p,radius,a,h),point(p,radius,z,h),point(p,radius,z,H),point(p,radius,a,H),color);
        }
    }
    private static Vec3 point(Vec3 p,double r,double a,double h){return p.add(Math.cos(a)*Math.cos(h)*r,Math.sin(h)*r,Math.sin(a)*Math.cos(h)*r);}
    private static void flames(BufferBuilder b,PoseStack ps,Vec3 p,double size,float age,Vec3 cam){
        Vec3 side=cam.subtract(p).cross(new Vec3(0,1,0)).normalize();
        for(int j=0;j<9;j++){
            double x=(j-4)*size*0.13,h=size*(0.7+0.3*Math.sin(age*0.2+j));
            Vec3 base=p.add(side.scale(x)).add(0,-size*0.25,0),tip=base.add(side.scale(Math.sin(age*0.14+j)*size*0.12)).add(0,h,0);
            ribbon(b,ps,base,tip,size*0.115,0xE28532BD,cam);
            Vec3 near=cam.subtract(base).normalize().scale(0.012);ribbon(b,ps,base.add(near),tip.add(near),size*0.082,0xFA050109,cam);
        }
    }
    public static void drawSwordQi(BufferBuilder b,PoseStack ps,Network.Effect e,Vec3 cam,float age){
        SlashAppearance.use(e);blueTint=VoidBossClient.blue(e.owner());
        float fade=Math.max(0,1-age/e.life());Vec3 axis=e.direction().normalize();Vec3 side=axis.cross(cam.subtract(e.position())).normalize();
        if(side.lengthSqr()<0.01)side=new Vec3(0,1,0);Vec3 start=e.position().subtract(axis.scale(e.size()*0.5)),end=e.position().add(axis.scale(e.size()*0.5));
        SlashStyle.draw(b,ps,start,end,Math.min(.6,e.size()*.06),fade,cam);
        blueTint=false;
    }

    public static void draw(BufferBuilder b,PoseStack ps,Vec3 cam,float partial){
        var level=Minecraft.getInstance().level;if(level==null)return;float time=tick+partial;
        for(Visual v:wounds.values()){
            Network.Effect e=v.e;SlashAppearance.use(e);blueTint=VoidBossClient.blue(e.owner());var target=level.getEntity((int)e.seed());if(target==null||!target.isAlive())continue;
            Vec3 center=target.getPosition(partial).add(0,target.getBbHeight()*0.5,0),toward=cam.subtract(center).normalize();
            double surface=Math.min(target.getBbWidth()/2/Math.max(0.001,Math.max(Math.abs(toward.x),Math.abs(toward.z))),target.getBbHeight()/2/Math.max(0.001,Math.abs(toward.y)));
            Vec3 p=center.add(toward.scale(surface+0.055)),side=new Vec3(-toward.z,0,toward.x).normalize();
            int count=Math.min(20,(int)e.direction().x),flags=(int)e.direction().z;double growth=1-Math.max(0,e.direction().y-(tick-v.born))/100.0;
            Vec3 previous=null;
            for(int i=0;i<count;i++){
                double angle=i*2.399963;Vec3 q=p.add(side.scale(Math.cos(angle)*target.getBbWidth()*0.32)).add(0,Math.sin(angle)*target.getBbHeight()*0.3,0);
                Vec3 axis=side.scale(Math.cos(angle)).add(0,Math.sin(angle),0).normalize();double length=e.size()*(0.35+0.55*growth);
                Vec3 a=q.subtract(axis.scale(length/2)),z=q.add(axis.scale(length/2));
                double width=0.045+growth*0.07;Vec3 n=toward.scale(0.007);
                SlashStyle.draw(b,ps,a,z,width+.025,1,cam);
                for(int k=0;k<3;k++){Vec3 start=a.lerp(z,(k+1)/4.0),end=start.add(side.scale((k%2==0?1:-1)*0.14)).add(0,0.12,0);SlashStyle.detail(b,ps,start,end,0.012,0xE5E5B94E,cam);}
                if(previous!=null){SlashStyle.detail(b,ps,previous,q,0.018,0xDA9438D5,cam);if((flags&2)!=0){Vec3 emit=q.add(axis.scale(length)),dest=center.add(side.scale(Math.sin(time*0.07+i)*0.5));if(((int)time+i*3)%20<4)cut(b,ps,emit.lerp(dest,0.5),dest.subtract(emit),(float)emit.distanceTo(dest),0.8f,cam);}}
                previous=q;
            }
            if((flags&1)!=0)flames(b,ps,center,VoidTechniqueRules.FIRE_SCALE,time,cam);
            if((flags&2)!=0&&Client.voidPulse(e.owner(),partial))ring(b,ps,center,Math.max(0.6,target.getBbWidth()),0.08,0xEDA75AE8,cam);
        }
        for(Visual v:effects){Network.Effect e=v.e;SlashAppearance.use(e);blueTint=VoidBossClient.blue(e.owner());float age=time-v.born,fade=Math.max(0,1-age/e.life());Vec3 p=e.position();
            if(e.size()==SlashReach.INFINITE){Client.drawInfiniteCut(b,ps,e,cam,age);continue;}
            switch(e.kind()){
                case 115->{Vec3 side=new Vec3(-e.direction().z,0,e.direction().x).normalize();if(side.lengthSqr()<0.01)side=new Vec3(1,0,0);Vec3 up=e.direction().cross(side).normalize();double a=Math.toRadians(e.seed());Vec3 axis=side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));cut(b,ps,p,axis,e.size()*Math.min(1,age/3),fade,cam);}
                case 116->{double f=Math.min(1,age/60);Vec3 point=p.lerp(e.direction(),f);double radius=(1-f)*2,a=age*0.32+e.seed();point=point.add(Math.cos(a)*radius,Math.sin(a)*radius,Math.sin(a*0.7)*radius);flames(b,ps,point,Math.max(0.4,3*(1-f)),age,cam);ribbon(b,ps,point,point.lerp(e.direction(),0.15),0.1,0xD26F32CC,cam);}
                case 117->{double r=0.3+e.size()*Math.min(1,age/60);sphere(b,ps,p,r,0xFF030108);for(int i=0;i<12;i++){double a=i*Math.PI/6+age*0.075;Vec3 q=p.add(Math.cos(a)*r,Math.sin(a*2)*r*0.5,Math.sin(a)*r);ribbon(b,ps,q,q.lerp(p,0.14),0.06,0xEB943ADE,cam);Vec3 outer=p.add(Math.cos(a)*(r+7*(1-age/60)),Math.sin(a)*2,Math.sin(a)*(r+7*(1-age/60)));ribbon(b,ps,outer,outer.lerp(q,0.6),0.12,0xC455187C,cam);}}
                case 118->{double r=e.size()*Math.sin(Math.min(1,age/e.life())*Math.PI);sphere(b,ps,p,Math.max(0.05,r*0.4),((int)(220*fade)<<24)|0x040109);ring(b,ps,p,r,0.13,((int)(255*fade)<<24)|0xBD68EA,cam);}
                case 119->{for(int i=0;i<9;i++){double a=i*Math.PI*2/9;Vec3 side=new Vec3(Math.cos(a),Math.sin(a),Math.sin(a*3)*0.3);Vec3 mid=p.add(side.scale(e.size()*0.5)),end=mid.add(side.scale(e.size()*0.4)).add(0.1,0.18,0);SlashStyle.detail(b,ps,p,mid,0.055,((int)(255*fade)<<24)|0x050108,cam);SlashStyle.detail(b,ps,mid,end,0.03,((int)(255*fade)<<24)|0x9E44DE,cam);}}
                case 121->{Vec3 end=e.direction(),point=p.lerp(end,Math.min(1,age/5));cut(b,ps,point,end.subtract(p),e.size(),fade,cam);SlashStyle.detail(b,ps,p,point,0.025,((int)(120*fade)<<24)|0x8734B9,cam);}
                case 122->drawSwordQi(b,ps,e,cam,age);
                case 120->{double r=e.size()*Math.min(1,age/24);Vec3 center=e.direction().lerp(p,1-Math.min(1,age/24));for(int j=-3;j<=3;j++){double lat=j*Math.PI/8;Vec3 q=center.add(0,Math.sin(lat)*r,0);ring(b,ps,q,Math.cos(lat)*r,1.7,((int)(220*fade)<<24)|0x030106,cam);ring(b,ps,q.add(0,0.07,0),Math.cos(lat)*r+0.35,0.35,((int)(255*fade)<<24)|0xAF4DE5,cam);}}
            }
        }
        blueTint=false;
    }
    private VoidTechniqueClient(){}
}
