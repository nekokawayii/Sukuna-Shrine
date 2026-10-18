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
    /** 宇宙大爆炸：白热核心一闪 → 三层不同速度的壳 → 径向光针 → 迟到的第二圈冲击。 */
    private record Bang(Vec3 at,double size,long born){}
    private static final List<Blast> blasts=new ArrayList<>();
    private static final List<Bang> bangs=new ArrayList<>();
    /** 大爆炸的演出总时长（刻）：2.7 秒，比普通命中闪光长得多。 */
    private static final int BANG_LIFE=54;
    private static long time;
    /** radius &lt;= 0 表示什么都不画（自然消散的弹体只消失，不爆）。 */
    public static void spawn(Vec3 at,double radius,int argb){
        if(at==null||radius<=0||!Double.isFinite(radius))return;
        blasts.add(new Blast(at,Math.min(64,radius),argb&0xFFFFFF,time,26));
        if(blasts.size()>24)blasts.remove(0);
    }
    public static void tick(){time++;blasts.removeIf(b->time-b.born()>=b.life());bangs.removeIf(b->time-b.born()>=BANG_LIFE);}
    public static void clear(){blasts.clear();bangs.clear();time=0;}
    /** 茈「一下打不死」的引爆：以 size 格为基准铺开三层壳 + 径向光针 + 二段冲击（纯演出，不结算伤害）。 */
    public static void bigBang(Vec3 at,double size){
        if(at==null||!Double.isFinite(size)||size<=0)return;
        bangs.add(new Bang(at,Math.min(28,Math.max(2,size)),time));
        if(bangs.size()>6)bangs.remove(0);
    }
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
        for(Bang bg:bangs){
            float t=(time-bg.born()+e.getPartialTick())/BANG_LIFE;
            if(t<0||t>=1)continue;
            bang(b,ps,bg.at(),cam,bg.size(),t);
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
    // ------------------------------------------------------------ 宇宙大爆炸
    /** 一帧的大爆炸：t ∈ [0,1)。所有层都是镜头朝向的圆盘/圆环 + 极角排布的光针，不依赖任何贴图。 */
    private static void bang(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 cam,double s,float t){
        // ① 白热核心：头 20% 最亮，随后塌成一层紫色余晖
        double core=Math.max(0,1-t/0.20);
        if(core>0.02){
            double r=s*0.55,r1=s*(0.55+1.6*t);
            int a=(int)(255*core);
            if(cam.distanceTo(c)>=r)band(b,ps,c,cam,0,r,a<<24|0xFFFFFF); // 站在爆心里时省掉实心眼，只留边缘
            band(b,ps,c,cam,r,r1,(a/2)<<24|0xC0A050);
        }
        // ② 三层壳：速度不同 —— 外层最快最紫，内层最慢最白，逐层先褪
        shell(b,ps,c,cam,s*(0.30+1.95*Math.sqrt(t)),s*0.10,0xB0A050FF,1-t);
        shell(b,ps,c,cam,s*(0.25+1.45*Math.sqrt(t)),s*0.07,0x9090C8FF,1-t*0.8f);
        shell(b,ps,c,cam,s*(0.20+1.00*Math.sqrt(t)),s*0.05,0x70FFFFFF,1-t*0.6f);
        // ③ 径向光针：从核心向外抽出的细线，随爆炸外扩，只在前半段存在
        if(t<0.55){
            double inner=s*(0.35+1.5*t),outer=s*(0.35+2.3*t);
            int a=(int)(215*(1-t/0.55));
            rays(b,ps,c,cam,inner,outer,a,22);
        }
        // ④ 迟到的第二圈冲击：t>0.35 才出现，慢慢飘出去，给"大爆炸"一个收尾
        if(t>0.35){
            double k=(t-0.35)/0.65,r=s*(1.5+1.4*k),th=s*0.07;
            int a=(int)(150*(1-k));
            if(a>4){band(b,ps,c,cam,r,r+th,a<<24|0x8FB8FF);band(b,ps,c,cam,r+th,r+th*1.6,(a/2)<<24|0xC8A0FF);}
        }
    }
    /** 一层向外扩张的壳（亮边 + 外侧淡晕）。k 是这个壳的残余亮度系数；rgb 只取低 24 位（alpha 由 k 算）。 */
    private static void shell(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 cam,double r,double th,int rgb,float k){
        int a=(int)(130*Math.max(0,k));
        if(a<4||r<=th)return;
        int rgb24=rgb&0xFFFFFF;
        band(b,ps,c,cam,r-th,r,a<<24|rgb24);
        band(b,ps,c,cam,r,r+th*0.6,(a/2)<<24|rgb24);
    }
    /** 镜头平面上按极角排布的 n 根光针：外端更细，整体随时间缓慢旋转。 */
    private static void rays(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 cam,double r0,double r1,int a,int n){
        if(a<4)return;
        Vec3 nrm=cam.subtract(c);if(nrm.lengthSqr()<1e-6)return;nrm=nrm.normalize();
        Vec3 sv=nrm.cross(new Vec3(0,1,0));if(sv.lengthSqr()<1e-6)sv=new Vec3(1,0,0);sv=sv.normalize();
        Vec3 uv=nrm.cross(sv).normalize();
        double spin=time*0.06;
        for(int i=0;i<n;i++){
            double ang=spin+i*Math.PI*2/n;
            Vec3 d=sv.scale(Math.cos(ang)).add(uv.scale(Math.sin(ang)));
            Vec3 perp=d.cross(nrm).normalize();
            double w0=Math.max(0.06,r1*0.035),w1=w0*0.35;
            Vec3 p0=c.add(d.scale(r0)),p1=c.add(d.scale(r1));
            quad(b,ps,p0.add(perp.scale(w0)),p0.subtract(perp.scale(w0)),p1.subtract(perp.scale(w1)),p1.add(perp.scale(w1)),a<<24|0xFFE8C8);
        }
    }
    private GojoBlastClient(){}
}
