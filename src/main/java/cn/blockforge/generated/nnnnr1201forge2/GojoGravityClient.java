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

/** 苍 的**引力视界**：黑洞同款三件套。
 *
 *  <p>① 绝对黑的视界盘（Photon 的叠加混合画不出纯黑，所以这一块和领域那口大黑洞一样走自己的顶点通路）；
 *  ②贴着视界的光子环（硬白边 + 外侧淡蓝）；③外面两圈反向旋转、被压扁的吸积弧，再加几根被拽弯的丝线。
 *  画法与 {@link GojoVoidClient} 的 {@code hole()} 同源，只是锚在蓝色光球上、按光球尺寸缩放。</p>
 *
 *  <p>服务端每刻发一发 {@link GojoFx#BLUE} 包（{@code seed} = 弹体 id），这里按 id 追踪位置；
 *  {@code BLUE_END} 或宇宙大爆炸时丢掉。没装 Photon 的客户端也照样有这层视界。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class GojoGravityClient {
    private static final class Horizon{Vec3 pos;double size;long last;double phase;}
    private static final Map<Long,Horizon> horizons=new HashMap<>();
    private static long time;
    /** 最后一发包之后再留几刻：苍收起的瞬间视界不会当场消失，留一点余辉。 */
    private static final int KEEP=3;
    /** 弹体这一帧在哪（苍每秒都发，位置基本不动也没关系，只更新不新增）。 */
    public static void track(long seed,Vec3 pos,double size){
        if(pos==null)return;
        Horizon h=horizons.get(seed);
        if(h==null){h=new Horizon();h.phase=(seed%97)*0.37;horizons.put(seed,h);}
        h.pos=pos;h.size=Math.max(0.8,size);h.last=time;
    }
    public static void drop(long seed){horizons.remove(seed);}
    public static void clear(){horizons.clear();time=0;}
    public static void tick(){time++;horizons.values().removeIf(h->time-h.last>KEEP);}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(horizons.isEmpty()||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        PoseStack ps=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();
        ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(Horizon h:horizons.values())horizon(b,ps,h,cam,e.getPartialTick());
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();
        ps.popPose();
    }
    private static void horizon(BufferBuilder b,PoseStack ps,Horizon h,Vec3 cam,float partial){
        Vec3 at=h.pos;if(at==null)return;
        double s=h.size;
        Vec3 n=cam.subtract(at);if(n.lengthSqr()<1e-6)return;n=n.normalize();
        Vec3 sv=n.cross(new Vec3(0,1,0));if(sv.lengthSqr()<1e-6)sv=new Vec3(1,0,0);sv=sv.normalize();
        Vec3 uv=n.cross(sv).normalize();
        double spin=(time+partial)*0.045+h.phase;
        double rH=s*0.92;                        // 视界半径：比光球本体略小一圈
        // ① 视界：纯黑盘面。相机钻进视界内侧时就不画了，免得整屏糊黑
        if(cam.distanceTo(at)>rH*0.85)disc(b,ps,at,sv,uv,rH*0.92,0x2EA8D8FF,40);   // 引力透镜：中间只留极淡的冷蓝雾，不再是实心黑盘
        // ② 光子环：贴着视界的硬白边 + 外侧淡蓝过渡
        band(b,ps,at,sv,uv,rH*1.00,rH*1.10,0xFFF4FFFF,40);
        band(b,ps,at,sv,uv,rH*1.10,rH*1.26,0x99CFE4FF,40);
        // ③ 引力透镜晕：更外一圈低亮度、会呼吸的环
        double breathe=1+0.04*Math.sin((time+partial)*0.11+h.phase);
        band(b,ps,at,sv,uv,rH*1.55*breathe,rH*1.63*breathe,0x46A8D8FF,36);
        // ④ 吸积盘：两圈被压扁、反向旋转的弧（和领域黑洞同款）
        accretion(b,ps,at,sv,uv,rH*1.34,rH*1.44,1.0,spin*2.3,0xCCFFFFFF);
        accretion(b,ps,at,sv,uv,rH*1.70,rH*1.77,-0.7,spin*2.3+1.1,0x66CFE4FF);
        // ⑤ 被拽弯的丝线：从光子环外缘拧出去的细弧，表达"空间在这里被拽了个弯"
        warp(b,ps,at,sv,uv,n,rH*1.15,rH*2.05,spin*1.6,8,0x55B8E0FF);
    }
    /** 纯色圆盘（按 billboard 三角扇铺满）。 */
    private static void disc(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 sv,Vec3 uv,double r,int color,int segments){
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,z=(i+1)*Math.PI*2/segments;
            Vec3 p=c.add(sv.scale(Math.cos(a)*r)).add(uv.scale(Math.sin(a)*r));
            Vec3 q=c.add(sv.scale(Math.cos(z)*r)).add(uv.scale(Math.sin(z)*r));
            quad(b,ps,c,p,q,c,color);
        }
    }
    private static void band(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 sv,Vec3 uv,double r0,double r1,int color,int segments){
        if(r1<=r0)return;
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,z=(i+1)*Math.PI*2/segments;
            Vec3 p0=c.add(sv.scale(Math.cos(a)*r0)).add(uv.scale(Math.sin(a)*r0));
            Vec3 p1=c.add(sv.scale(Math.cos(z)*r0)).add(uv.scale(Math.sin(z)*r0));
            Vec3 q1=c.add(sv.scale(Math.cos(z)*r1)).add(uv.scale(Math.sin(z)*r1));
            Vec3 q0=c.add(sv.scale(Math.cos(a)*r1)).add(uv.scale(Math.sin(a)*r1));
            quad(b,ps,p0,p1,q1,q0,color);
        }
    }
    /** 压扁的旋转弧：uv 方向乘 0.35 就是"斜着看的盘"，speed 为负即反向。 */
    private static void accretion(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 sv,Vec3 uv,double r0,double r1,double speed,double phase,int color){
        int segments=48;
        for(int i=0;i<segments;i++){
            double a=phase+i*Math.PI*2/segments*speed,z=phase+(i+1)*Math.PI*2/segments*speed;
            Vec3 p0=c.add(sv.scale(Math.cos(a)*r0)).add(uv.scale(Math.sin(a)*r0*0.35));
            Vec3 p1=c.add(sv.scale(Math.cos(z)*r0)).add(uv.scale(Math.sin(z)*r0*0.35));
            Vec3 q1=c.add(sv.scale(Math.cos(z)*r1)).add(uv.scale(Math.sin(z)*r1*0.35));
            Vec3 q0=c.add(sv.scale(Math.cos(a)*r1)).add(uv.scale(Math.sin(a)*r1*0.35));
            quad(b,ps,p0,p1,q1,q0,color);
        }
    }
    /** 从内圈拧到外圈的细丝：每根是一段带俯仰的短弧，宽度向外收细。 */
    private static void warp(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 sv,Vec3 uv,Vec3 nrm,double r0,double r1,double spin,int count,int color){
        for(int j=0;j<count;j++){
            double a0=spin+j*Math.PI*2/count;
            double a1=a0+0.62;
            Vec3 d0=sv.scale(Math.cos(a0)).add(uv.scale(Math.sin(a0)));
            Vec3 d1=sv.scale(Math.cos(a1)).add(uv.scale(Math.sin(a1)));
            Vec3 p0=c.add(d0.scale(r0)),p1=c.add(d1.scale(r1));
            Vec3 perp=nrm.cross(d0);
            if(perp.lengthSqr()<1e-6)continue;
            perp=perp.normalize();
            double w0=r0*0.05,w1=w0*0.35;
            quad(b,ps,p0.add(perp.scale(w0)),p0.subtract(perp.scale(w0)),p1.subtract(perp.scale(w1)),p1.add(perp.scale(w1)),color);
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
    private GojoGravityClient(){}
}
