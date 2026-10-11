package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import static cn.blockforge.generated.nnnnr1201forge2.Client.*;

@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class VoidBossClient {
    private static final Map<Integer,Long> shrines=new HashMap<>();
    private static final Set<Integer> blueShrines=new HashSet<>();
    private static final Map<Integer,Long> blueCasters=new HashMap<>();
    private static long time;
    private static final BbModel SHRINE=new BbModel("void_shrine","void_shrine");
    public static void renderShrine(PoseStack ps,net.minecraft.client.renderer.MultiBufferSource source,float age,int owner){boolean blue=blue(owner);SHRINE.render(ps,source,15728880,"awaken",age/20,blue?0.14f:1,blue?0.85f:1,1,1);}
    public static boolean blue(int owner){return blueCasters.getOrDefault(owner,0L)>time||shrine(owner)&&blueShrines.contains(owner);}
    public static void closeShrine(int owner){shrines.remove(owner);blueShrines.remove(owner);}
    public static void clear(){shrines.clear();blueShrines.clear();blueCasters.clear();time=0;}
    public static void tick(){time++;shrines.values().removeIf(end->end<time);blueShrines.retainAll(shrines.keySet());}
    public static void receive(Network.Effect e){if(e.kind()==144){if(e.seed()==1)blueCasters.put(e.owner(),time+e.life());else blueCasters.remove(e.owner());return;}if(e.kind()==112){shrines.put(e.owner(),time+e.life());if(e.seed()==1)blueShrines.add(e.owner());else blueShrines.remove(e.owner());return;}}
    public static boolean shrine(int owner){return shrines.containsKey(owner);}
    @SubscribeEvent public static void fog(ViewportEvent.ComputeFogColor e){var mc=Minecraft.getInstance();if(mc.level!=null&&mc.level.dimension()==VoidJourney.ARENA){e.setRed(0.022f);e.setGreen(0.008f);e.setBlue(0.042f);}}
    public static void domain(BufferBuilder b,PoseStack ps,Vec3 center,Vec3 shrine,Vec3 cam,float age,float fade,int owner){
        boolean blue=blue(owner);int accent=blue?0x269FFF:0xA145DB,light=blue?0x91E8FF:0xC965ED,eyeColor=blue?0xEE53CFFF:0xEEB25BEA;
        float pulse=age%160<8?1:0.45f;
        // 固定的放射地裂，领域存续期间不消失；八秒与铃铛转动同步闪烁。
        for(int i=0;i<32;i++){
            double a=i*Math.PI/16;Vec3 last=shrine.add(Math.cos(a)*1.8,0.43,Math.sin(a)*1.8);
            for(int j=1;j<=8;j++){
                double r=j*23.0,angle=a+Math.sin(i*13+j*7)*0.045;Vec3 q=shrine.add(Math.cos(angle)*r,0.13,Math.sin(angle)*r);
                if(q.distanceToSqr(center)>200*200)break;
                SlashStyle.detail(b,ps,last,q,0.22,((int)(245*fade)<<24)|0x030106,cam);
                if(!SlashAppearance.desktop())ribbon(b,ps,last.add(0,0.015,0),q.add(0,0.015,0),0.035,((int)(240*pulse*fade)<<24)|accent,cam);
                if(j==1){Vec3 start=shrine.add(Math.cos(a)*1.8,0.44,Math.sin(a)*1.8),end=shrine.add(Math.cos(a)*5.8,0.44,Math.sin(a)*5.8);SlashStyle.detail(b,ps,start,end,0.04,(0xC5000000|accent),cam);}
                if((age+i*17+j*11)%190<16){float ghost=(float)Math.sin(((age+i*17+j*11)%190)/16*Math.PI);Vec3 g=q.add(0,0.1,0);ribbon(b,ps,g,g.add(0,1.2,0),0.19,((int)(95*ghost*fade)<<24)|0xC2AED5,cam);ribbon(b,ps,g.add(0,1.3,0),g.add(0,1.6,0),0.15,((int)(110*ghost*fade)<<24)|0xD3BDD9,cam);}
                last=q;
            }
        }
        // 黑色祭坛上的咒纹、核心的紫色暗芒。
        for(int sign:new int[]{-1,1})for(int j=0;j<3;j++){Vec3 p=shrine.add(sign*(0.35+j*0.2),1.36,-0.8+j*0.22);ribbon(b,ps,p,p.add(sign*0.25,0,0.3),0.035,(0xDD000000|accent),cam);}
        Vec3 heart=shrine.add(0,3.45,0);
        for(int i=0;i<12;i++){double a=age*0.024+i*Math.PI/6;Vec3 p=heart.add(Math.cos(a)*1.4,Math.sin(a*2)*0.8,Math.sin(a)*1.4);ribbon(b,ps,p,p.lerp(heart,0.25),0.025,((int)(210*fade)<<24)|light,cam);}
        // 四眼虚影与四角铃铛同周期旋转，睁眼后俯视整个领域。
        Vec3 crown=shrine.add(0,10.5,0);
        for(int i=0;i<4;i++){double a=i*Math.PI/2+age*Math.PI/80;Vec3 p=crown.add(Math.cos(a)*3.8,0,Math.sin(a)*3.8),side=new Vec3(-Math.sin(a),0,Math.cos(a));double open=Math.min(1,age/30);
            Vec3 l=p.subtract(side.scale(0.9)),r=p.add(side.scale(0.9));
            ribbon(b,ps,l,p.add(0,0.35*open,0),0.06,eyeColor,cam);ribbon(b,ps,p.add(0,0.35*open,0),r,0.06,eyeColor,cam);ribbon(b,ps,r,p.add(0,-0.35*open,0),0.06,eyeColor,cam);ribbon(b,ps,p.add(0,-0.35*open,0),l,0.06,eyeColor,cam);ribbon(b,ps,p.add(0,-0.28*open,0),p.add(0,0.28*open,0),0.1,0xF4050109,cam);
        }
        ring(b,ps,center.add(0,0.15,0),200,0.65,((int)(160*fade)<<24)|accent,cam);
    }
}
