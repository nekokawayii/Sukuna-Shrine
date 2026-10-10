package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/** 斩击、冰柱用轻量几何绘制；数据均取自服务端命中轨迹。 */
public final class BranchVisuals {
    private record Visual(Network.Effect e,long born){}
    private static final List<Visual> effects=new ArrayList<>();
    private static final Map<Integer,Visual> frozen=new HashMap<>();
    private static long tick;
    public static void collect(Vec3 center,double radius,List<Vec3> out){effects.removeIf(v->{if(v.e.kind()!=BranchRules.FX_LINE&&v.e.kind()!=BranchRules.FX_WALL)return false;Vec3 at=v.e.position().add(v.e.direction().scale(.5));if(v.e.kind()==BranchRules.FX_WALL)at=at.add(SukunaBranches.unpack(v.e.seed()).scale((tick-v.born)*1.5));if(at.distanceToSqr(center)>radius*radius)return false;if(out.size()<6000)out.add(at);return true;});}
    public static void clear(){effects.clear();frozen.clear();tick=0;}
    public static void receive(Network.Effect e){
        if(e.kind()==BranchRules.FX_FREEZE){if(e.life()<=0)frozen.remove(e.owner());else frozen.put(e.owner(),new Visual(e,tick));return;}
        int limit=e.kind()==BranchRules.FX_LINE?10000:12000;if(effects.size()<limit)effects.add(new Visual(e,tick));
    }
    public static void tick(){
        tick++;effects.removeIf(v->tick-v.born>=v.e.life());frozen.values().removeIf(v->tick-v.born>=v.e.life());var l=Minecraft.getInstance().level;if(l==null)return;
        for(Visual v:effects)if(v.e.kind()==BranchRules.FX_FROST&&tick-v.born<2){Vec3 from=v.e.position(),dir=v.e.direction(),side=NetBranches.side(dir),up=dir.cross(side);double growth=Math.log1p(BranchRules.upgrades((int)v.e.seed()));
            for(int i=0;i<12;i++){Vec3 point=from.add(dir.scale(l.random.nextDouble()*v.e.size())).add(side.scale((l.random.nextDouble()-0.5)*(5+growth))).add(up.scale((l.random.nextDouble()-0.5)*(3+growth*0.5)));l.addParticle(ParticleTypes.SNOWFLAKE,point.x,point.y,point.z,dir.x*0.2,dir.y*0.2,dir.z*0.2);if(i%3==0)l.addParticle(ParticleTypes.CLOUD,point.x,point.y,point.z,dir.x*0.07,dir.y*0.07,dir.z*0.07);}
        }
    }
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 camera,float partial){
        for(Visual v:effects){Network.Effect e=v.e;SlashAppearance.use(e);double age=tick-v.born+partial;float fade=(float)Math.min(1,(e.life()-age)/3);Vec3 from=e.position(),to=from.add(e.direction());
            if(e.kind()==BranchRules.FX_WALL){Vec3 motion=SukunaBranches.unpack(e.seed()).scale(age*1.5);from=from.add(motion);to=to.add(motion);SlashStyle.draw(b,ps,from,to,.16,fade,camera);}
            else if(e.kind()==BranchRules.FX_LINE){SlashStyle.draw(b,ps,from,to,age>=19?.15:.08,fade,camera);}
            else if(e.kind()==BranchRules.FX_ICE){Vec3 point=from.add(e.direction().scale(age));crystal(b,ps,point,e.direction().normalize(),0.32,2.8,fade,camera);}
        }
        var l=Minecraft.getInstance().level;if(l==null)return;
        for(Visual v:frozen.values()){var t=l.getEntity(v.e.owner());if(t==null)continue;Vec3 point=t.position();int n=7;for(int i=0;i<n;i++){double a=i*Math.PI*2/n;crystal(b,ps,point.add(Math.cos(a)*0.55,0,Math.sin(a)*0.55),new Vec3(Math.cos(a)*0.15,1,Math.sin(a)*0.15).normalize(),0.22,v.e.size()+0.5,0.6f,camera);}}
    }
    private static void crystal(BufferBuilder b,PoseStack ps,Vec3 p,Vec3 dir,double radius,double length,float fade,Vec3 camera){
        Vec3 side=NetBranches.side(dir),up=dir.cross(side),tip=p.add(dir.scale(length));
        int[] colors={0x90A5E8FF,0xC3DDFBFF,0xAA448FD3,0xB972C7F6};
        for(int i=0;i<4;i++){double a=i*Math.PI/2,z=(i+1)*Math.PI/2;Vec3 x=p.add(side.scale(Math.cos(a)*radius)).add(up.scale(Math.sin(a)*radius)),y=p.add(side.scale(Math.cos(z)*radius)).add(up.scale(Math.sin(z)*radius));int color=colors[i],alpha=(int)(((color>>>24)&255)*fade);Client.quad(b,ps,x,y,tip,tip,(alpha<<24)|(color&0xFFFFFF));Client.ribbon(b,ps,x,tip,0.012,0xBCE9FCFF,camera);}
    }
    private BranchVisuals(){}
}
