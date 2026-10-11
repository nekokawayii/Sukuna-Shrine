package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 赫·最大功率打出去之后：一条真正的三维光轴 + **每隔 2 格一圈白色环**。
 *  环必须垂直于射线方向，Photon 的 billboard 画不出这种朝向，所以这一层走模组自己的顶点通路；
 *  红色圆柱体积由 Photon 的 Cylinder 发射器负责（见 PhotonFx.beam）。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class GojoBeamClient {
    private record Beam(Vec3 from,Vec3 dir,double length,double width,int life,long born){}
    private static final List<Beam> beams=new ArrayList<>();
    private static long time;
    /** 每 2 格一圈白环。 */
    private static final double RING_GAP=2.0;
    public static void spawn(Vec3 from,Vec3 to,double width,int life){
        if(from==null||to==null)return;
        Vec3 d=to.subtract(from);double len=d.length();
        if(len<0.4)return;
        beams.add(new Beam(from,d.normalize(),Math.min(len,160),Math.max(0.14,width),Math.max(2,life),time));
        if(beams.size()>16)beams.remove(0);
    }
    public static void tick(){time++;beams.removeIf(b->time-b.born()>=b.life());}
    public static void clear(){beams.clear();time=0;}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(beams.isEmpty()||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        PoseStack ps=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();
        ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(Beam bm:beams){
            double age=(time-bm.born())+e.getPartialTick();
            double k=Math.max(0,1-age/bm.life());          // 整体渐隐
            if(k<=0.02)continue;
            Vec3 dir=bm.dir();
            Vec3 u=perp(dir),v=dir.cross(u).normalize();
            double r=bm.width()*1.55;
            // 光轴：两条互相垂直的细亮带，形成一根真正有体积的中轴
            axis(b,ps,bm.from(),dir,u,r*0.42,bm.length(),k);
            axis(b,ps,bm.from(),dir,v,r*0.42,bm.length(),k);
            // 白环：每 2 格一圈
            for(double at=RING_GAP;at<bm.length();at+=RING_GAP){
                double f=Math.min(1,Math.min(at,bm.length()-at)/2.5); // 首尾两格渐隐
                int a=(int)(170*k*f);
                if(a<6)continue;
                Vec3 c=bm.from().add(dir.scale(at));
                ring(b,ps,c,u,v,r,r+bm.width()*0.42,a<<24|0xFFFFFF,24);
                ring(b,ps,c,u,v,r*0.92,r*0.99,(a*3/4)<<24|0xCFE4FF,24);
            }
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.enableDepthTest();
        ps.popPose();
    }
    /** 沿 dir 的细亮带：分成若干段以便首尾渐隐。 */
    private static void axis(BufferBuilder b,PoseStack ps,Vec3 from,Vec3 dir,Vec3 side,double halfW,double length,double k){
        int steps=(int)Math.max(2,Math.min(24,length/2));
        for(int i=0;i<steps;i++){
            double t0=i/(double)steps,t1=(i+1)/(double)steps;
            double f=Math.min(1,(1-t0)*2.2)*k;                 // 末端渐隐
            int a=(int)(200*f);
            if(a<6)continue;
            Vec3 p0=from.add(dir.scale(length*t0)),p1=from.add(dir.scale(length*t1));
            quad(b,ps,p0.add(side.scale(halfW)),p0.subtract(side.scale(halfW)),p1.subtract(side.scale(halfW)),p1.add(side.scale(halfW)),a<<24|0xFFF0E0);
            quad(b,ps,p0.add(side.scale(halfW*2.2)),p0.subtract(side.scale(halfW*2.2)),p1.subtract(side.scale(halfW*2.2)),p1.add(side.scale(halfW*2.2)),(a/3)<<24|0xFF5A20);
        }
    }
    private static void ring(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 u,Vec3 v,double r0,double r1,int color,int segments){
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,z=(i+1)*Math.PI*2/segments;
            Vec3 p0=c.add(u.scale(Math.cos(a)*r0)).add(v.scale(Math.sin(a)*r0));
            Vec3 p1=c.add(u.scale(Math.cos(z)*r0)).add(v.scale(Math.sin(z)*r0));
            Vec3 q1=c.add(u.scale(Math.cos(z)*r1)).add(v.scale(Math.sin(z)*r1));
            Vec3 q0=c.add(u.scale(Math.cos(a)*r1)).add(v.scale(Math.sin(a)*r1));
            quad(b,ps,p0,p1,q1,q0,color);
        }
    }
    private static Vec3 perp(Vec3 d){
        Vec3 a=Math.abs(d.y)<0.9?new Vec3(0,1,0):new Vec3(1,0,0);
        Vec3 p=d.cross(a);
        return p.lengthSqr()<1e-6?new Vec3(1,0,0):p.normalize();
    }
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int color){
        int red=(color>>16)&255,green=(color>>8)&255,blue=color&255,alpha=(color>>>24)&255;
        var pose=ps.last().pose();
        b.vertex(pose,(float)a.x,(float)a.y,(float)a.z).color(red,green,blue,alpha).endVertex();
        b.vertex(pose,(float)c.x,(float)c.y,(float)c.z).color(red,green,blue,alpha).endVertex();
        b.vertex(pose,(float)d.x,(float)d.y,(float)d.z).color(red,green,blue,alpha).endVertex();
        b.vertex(pose,(float)e.x,(float)e.y,(float)e.z).color(red,green,blue,alpha).endVertex();
    }
    private GojoBeamClient(){}
}
