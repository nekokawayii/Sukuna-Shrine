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

/** 命中/终结演出：只做大与渐隐，**不结算任何伤害**（伤害在服务端命中的那一刻已经结清）。
 *  Photon 的发射器没法在运行中改尺寸与不透明度，所以这一层用模组自己的顶点通路画。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class GojoBlastClient {
    private record Blast(Vec3 at,double radius,int rgb,long born,int life){}
    private static final List<Blast> blasts=new ArrayList<>();
    private static long time;
    /** radius &lt;= 0 表示什么都不画（自然消散的弹体只消失，不爆）。 */
    public static void spawn(Vec3 at,double radius,int argb){
        if(at==null||radius<=0||!Double.isFinite(radius))return;
        blasts.add(new Blast(at,Math.min(64,radius),argb&0xFFFFFF,time,26));
        if(blasts.size()>24)blasts.remove(0);
    }
    public static void tick(){time++;blasts.removeIf(b->time-b.born()>=b.life());}
    public static void clear(){blasts.clear();time=0;}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(blasts.isEmpty()||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        PoseStack ps=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();
        ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(Blast bl:blasts){
            float t=(time-bl.born()+e.getPartialTick())/bl.life();
            if(t<0||t>=1)continue;
            double r=bl.radius()*(0.18+0.82*Math.sqrt(t)); // 先快后慢地变大
            int a=(int)(0xD8*Math.pow(1-t,1.6));          // 不透明度逐渐减小
            if(a<=3)continue;
            int color=bl.rgb();
            // 站在爆心里面时省掉实心眼，只留边缘与光晕，免得整屏糊成一片
            if(cam.distanceTo(bl.at())>=r)band(b,ps,bl.at(),cam,0,r*0.90,(a/3)<<24|color);
            band(b,ps,bl.at(),cam,r*0.90,r,a<<24|color);          // 亮边
            band(b,ps,bl.at(),cam,r,r*1.14,(a/2)<<24|color);      // 外侧光晕
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();
        ps.popPose();
    }
    private static void band(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 cam,double r0,double r1,int color){
        if(r1<=r0)return;
        Vec3 n=cam.subtract(c);if(n.lengthSqr()<1e-6)return;n=n.normalize();
        Vec3 side=n.cross(new Vec3(0,1,0));if(side.lengthSqr()<1e-6)side=new Vec3(1,0,0);side=side.normalize();
        Vec3 up=n.cross(side).normalize();
        int segments=48;
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,z=(i+1)*Math.PI*2/segments;
            Vec3 p0=c.add(side.scale(Math.cos(a)*r0)).add(up.scale(Math.sin(a)*r0));
            Vec3 p1=c.add(side.scale(Math.cos(z)*r0)).add(up.scale(Math.sin(z)*r0));
            Vec3 q1=c.add(side.scale(Math.cos(z)*r1)).add(up.scale(Math.sin(z)*r1));
            Vec3 q0=c.add(side.scale(Math.cos(a)*r1)).add(up.scale(Math.sin(a)*r1));
            quad(b,ps,p0,p1,q1,q0,color);
        }
    }
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int color){
        int red=(color>>16)&255,green=(color>>8)&255,blue=color&255,alpha=(color>>>24)&255;
        var pose=ps.last().pose();
        b.vertex(pose,(float)a.x,(float)a.y,(float)a.z).color(red,green,blue,alpha).endVertex();
        b.vertex(pose,(float)c.x,(float)c.y,(float)c.z).color(red,green,blue,alpha).endVertex();
        b.vertex(pose,(float)d.x,(float)d.y,(float)d.z).color(red,green,blue,alpha).endVertex();
        b.vertex(pose,(float)e.x,(float)e.y,(float)e.z).color(red,green,blue,alpha).endVertex();
    }
    private GojoBlastClient(){}
}
