package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 无量空处 · 半径 100 格的绝对画布。
 *
 *  <p>画面内容（银河、星空、白墨泼溅、七彩超细线条、白色吸积盘、黑洞本体）全部由 Photon 粒子构成，
 *  装在一个 FX 里挂在同一个 root 上；客户端每刻把 root 移到**施法者身上**，所以画布与边界的距离恒定，
 *  玩家怎么走视觉上都保持同样的距离，转身看才换个方向。</p>
 *
 *  <p>Photon 的叠加混合 + bloom 画不出纯黑，所以"纯黑盘面 + 白色硬边"这一小块仍用模组自己的顶点通路，
 *  和整块底衬一起画在 {@code AFTER_CUTOUT_BLOCKS}：比天空与方块高一级、比实体低一级，
 *  并顺手清掉深度缓冲，让远处的粒子不再被（已经被盖住的地形）挡掉，而怪物与玩家依旧压在画布之上。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class GojoVoidClient {
    private static int ownerId=Integer.MIN_VALUE;
    private static Vec3 center,anchor,holeAt,holeOffset;
    private static double radius;
    private static long end,time;
    private static PhotonFx.Handle canvas;
    private static int canvasLife,canvasDuration;
    /** 展开：记下施法者，之后每刻跟着他走。 */
    public static void open(int owner,Vec3 at,Vec3 face,double r,int life){
        ownerId=owner;center=at;anchor=at;radius=Math.max(16,r);end=time+Math.max(20,life);
        Vec3 flat=new Vec3(face.x,0,face.z);
        if(flat.lengthSqr()<1e-4)flat=new Vec3(1,0,0);
        flat=flat.normalize();
        // 黑洞立在正前方 60 格、上方 20 格：抬头就是画面中央那口吸积盘
        holeOffset=new Vec3(flat.x*radius*0.60,radius*0.20,flat.z*radius*0.60);
        holeAt=anchor.add(holeOffset);
        dropCanvas();
    }
    /** 心跳 / 对拼延长：只延寿命与校正锚点，画布本身不重建。 */
    public static void refresh(int owner,Vec3 at,double r,int life){
        if(center==null)return;
        ownerId=owner;center=at;
        double old=radius;
        if(r>0)radius=Math.max(16,r);
        if(old!=radius||holeAt==null){ // 半径变了就重算黑洞位置
            Vec3 flat=new Vec3(holeAt==null?1:holeAt.x-anchor.x,0,holeAt==null?0:holeAt.z-anchor.z);
            if(flat.lengthSqr()<1e-4)flat=new Vec3(1,0,0);
            flat=flat.normalize();
            holeOffset=new Vec3(flat.x*radius*0.60,radius*0.20,flat.z*radius*0.60);
        }
        end=time+Math.max(20,life);
    }
    public static void close(){ownerId=Integer.MIN_VALUE;center=null;anchor=null;holeAt=null;holeOffset=null;radius=0;end=0;dropCanvas();}
    public static boolean active(){return anchor!=null;}
    public static Vec3 center(){return anchor;}
    public static double radius(){return radius;}
    private static void dropCanvas(){if(canvas!=null)PhotonFx.stop(canvas);canvas=null;canvasLife=0;canvasDuration=0;}
    public static void tick(){
        time++;
        if(center==null){return;}
        if(time>end){close();return;}
        updateAnchor();
        if(canvas==null)buildCanvas();
        else PhotonFx.move(canvas,anchor);
    }
    /** 锚点跟着施法者实体走（拿不到实体就用服务端心跳给的位置）。 */
    private static void updateAnchor(){
        var mc=Minecraft.getInstance();
        Entity owner=ownerId==Integer.MIN_VALUE||mc.level==null?null:mc.level.getEntity(ownerId);
        if(owner!=null&&owner.isAlive())center=owner.position();
        anchor=center;
        holeAt=anchor.add(holeOffset);
    }
    private static void buildCanvas(){
        if(!PhotonFx.ready())return;
        canvasLife=(int)Math.max(200,Math.min(20000,end-time+120)); // 寿命覆盖整场领域，中途不重建
        canvasDuration=Math.min(20000,Math.max(2000,canvasLife));
        canvas=PhotonFx.canvas(anchor,radius,holeAt,radius*0.28,canvasDuration);
    }

    // ------------------------------------------------------------ 底衬 + 纯黑盘面（Photon 画不出的那一小块）
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(anchor==null||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        PoseStack ps=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();
        if(cam.distanceTo(anchor)>radius*1.35){ // 远远超出边界就不再铺底（只在领域附近生效）
            ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
            RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.depthMask(false);RenderSystem.enableDepthTest();
            BufferBuilder b=Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            sphereSilhouette(b,ps,cam);
            BufferUploader.drawWithShader(b.end());
            RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();
            ps.popPose();return;
        }
        ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.depthMask(false);RenderSystem.disableDepthTest();
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        backdrop(b,ps,e.getCamera(),cam); // 纯黑底衬：地形、天空、领域雾全部让位
        hole(b,ps,cam);                   // 纯黑盘面 + 白色硬边
        BufferUploader.drawWithShader(b.end());
        // 地形深度已经被底衬盖住，清掉它，远处粒子（银河/星空/彩线）才不会被看不见的方块挡掉；
        // 实体在后面的阶段重新写入深度，所以怪物与玩家仍然压在画布之上。
        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT,false);
        RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();
        ps.popPose();
    }
    /** 镜头前 40 格处的一张巨大幕布：极深的蓝黑，给银河当底。 */
    private static void backdrop(BufferBuilder b,PoseStack ps,Camera cam,Vec3 eye){
        var look=cam.getLookVector();var up=cam.getUpVector();var left=cam.getLeftVector();
        Vec3 f=eye.add(look.x()*40,look.y()*40,look.z()*40);
        Vec3 u=new Vec3(up.x(),up.y(),up.z()).scale(420);
        Vec3 r=new Vec3(-left.x(),-left.y(),-left.z()).scale(420);
        int top=0xFF05030C,bottom=0xFF000000;
        quadGrad(b,ps,f.subtract(r).subtract(u),f.add(r).subtract(u),f.add(r).add(u),f.subtract(r).add(u),top,top,bottom,bottom);
    }
    // ------------------------------------------------------------ 纯黑盘面 + 白边 + 吸积环
    private static void hole(BufferBuilder b,PoseStack ps,Vec3 cam){
        Vec3 at=holeAt;if(at==null)return;
        double hole=radius*0.28;
        Vec3 n=cam.subtract(at);if(n.lengthSqr()<1e-6)return;n=n.normalize();
        Vec3 sv=n.cross(new Vec3(0,1,0));if(sv.lengthSqr()<1e-6)sv=new Vec3(1,0,0);sv=sv.normalize();
        Vec3 uv=n.cross(sv).normalize();
        double spin=(time%3600)/3600.0*Math.PI*2;
        int segments=64;
        if(cam.distanceTo(at)>hole*0.9)for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,z=(i+1)*Math.PI*2/segments;
            Vec3 p=at.add(sv.scale(Math.cos(a)*hole)).add(uv.scale(Math.sin(a)*hole));
            Vec3 q=at.add(sv.scale(Math.cos(z)*hole)).add(uv.scale(Math.sin(z)*hole));
            quad(b,ps,at,p,q,at,0xFF000000);
        }
        band(b,ps,at,sv,uv,hole*1.02,hole*1.10,0xFFFFFFFF,segments);
        band(b,ps,at,sv,uv,hole*1.10,hole*1.22,0x99E8F0FF,segments);
        accretion(b,ps,at,sv,uv,hole*1.45,hole*1.52,0.9,spin*2,0xCCFFFFFF);
        accretion(b,ps,at,sv,uv,hole*1.80,hole*1.85,-0.6,spin*2+1.2,0x66CFE4FF);
    }
    /** 远远在外时：量出一颗半径 100 的黑球轮廓。 */
    private static void sphereSilhouette(BufferBuilder b,PoseStack ps,Vec3 cam){
        double d=cam.distanceTo(anchor);
        if(d<=radius*1.0001)return;
        Vec3 n=anchor.subtract(cam).normalize();
        Vec3 at=cam.add(n.scale(d-radius*radius/d));
        double sr=Math.max(0.5,radius*Math.sqrt(Math.max(0,1-(radius*radius)/(d*d))));
        Vec3 sv=n.cross(new Vec3(0,1,0));if(sv.lengthSqr()<1e-6)sv=new Vec3(1,0,0);sv=sv.normalize();
        Vec3 uv=n.cross(sv).normalize();
        int segments=64;
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,z=(i+1)*Math.PI*2/segments;
            Vec3 p=at.add(sv.scale(Math.cos(a)*sr)).add(uv.scale(Math.sin(a)*sr));
            Vec3 q=at.add(sv.scale(Math.cos(z)*sr)).add(uv.scale(Math.sin(z)*sr));
            quad(b,ps,at,p,q,at,0xF0000000);
        }
        band(b,ps,at,sv,uv,sr*0.99,sr*1.02,0x88E8F0FF,segments);
    }
    private static void band(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 sv,Vec3 uv,double r0,double r1,int color,int segments){
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,z=(i+1)*Math.PI*2/segments;
            Vec3 p0=c.add(sv.scale(Math.cos(a)*r0)).add(uv.scale(Math.sin(a)*r0));
            Vec3 p1=c.add(sv.scale(Math.cos(z)*r0)).add(uv.scale(Math.sin(z)*r0));
            Vec3 q1=c.add(sv.scale(Math.cos(z)*r1)).add(uv.scale(Math.sin(z)*r1));
            Vec3 q0=c.add(sv.scale(Math.cos(a)*r1)).add(uv.scale(Math.sin(a)*r1));
            quad(b,ps,p0,p1,q1,q0,color);
        }
    }
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
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int color){
        int red=(color>>16)&255,green=(color>>8)&255,blue=color&255,alpha=(color>>>24)&255;
        var pose=ps.last().pose();
        b.vertex(pose,(float)a.x,(float)a.y,(float)a.z).color(red,green,blue,alpha).endVertex();
        b.vertex(pose,(float)c.x,(float)c.y,(float)c.z).color(red,green,blue,alpha).endVertex();
        b.vertex(pose,(float)d.x,(float)d.y,(float)d.z).color(red,green,blue,alpha).endVertex();
        b.vertex(pose,(float)e.x,(float)e.y,(float)e.z).color(red,green,blue,alpha).endVertex();
    }
    private static void quadGrad(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int ca,int cc,int cd,int ce){
        var pose=ps.last().pose();
        vertex(b,pose,a,ca);vertex(b,pose,c,cc);vertex(b,pose,d,cd);vertex(b,pose,e,ce);
    }
    private static void vertex(BufferBuilder b,org.joml.Matrix4f pose,Vec3 v,int color){
        b.vertex(pose,(float)v.x,(float)v.y,(float)v.z).color((color>>16)&255,(color>>8)&255,color&255,(color>>>24)&255).endVertex();
    }
    private GojoVoidClient(){}
}
