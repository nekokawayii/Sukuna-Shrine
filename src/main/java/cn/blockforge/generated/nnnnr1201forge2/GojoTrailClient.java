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

/** 茈 / 苍 的**拖尾**。
 *
 *  <p>服务端每刻发一发弹体特效包（茈走 {@link GojoFx#PURPLE}、苍走 {@link GojoFx#BLUE}），包里的
 *  {@code seed} 就是弹体 id。这里按 id 攒一串位置点，用模组自己的顶点通路画一条**面向镜头的渐隐缎带**，
 *  不依赖 Photon：没装 Photon 的客户端也有拖尾。</p>
 *
 *  <p>茈 1 格/刻 → 拖尾最长约 24 格；苍引导时 0.25 格/刻 → 缎带短而粗。两种颜色与宽度分开配。
 *  最后一发包之后再留几刻再丢，免得弹体消失时缎带"啪"地断掉。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class GojoTrailClient {
    public static final int BLUE=0,PURPLE=1;
    /** 每条拖尾最多留几个点 / 最后一次采点之后还留几刻。 */
    private static final int PURPLE_POINTS=24,BLUE_POINTS=18;
    private static final int PURPLE_KEEP=14,BLUE_KEEP=24;
    /** 缎带基础半宽（格）。 */
    private static final double PURPLE_WIDTH=0.62,BLUE_WIDTH=0.95;
    /** 颜色：茈偏紫、苍偏蓝（与 Photon 那边的光球同色系）。 */
    private static final int PURPLE_RGB=0x8F5CFF,BLUE_RGB=0x5C9BFF;
    private static final class Trail {
        final int kind;final ArrayDeque<Vec3> points=new ArrayDeque<>();long last;
        Trail(int kind,long now){this.kind=kind;this.last=now;}
    }
    private static final Map<Long,Trail> trails=new HashMap<>();
    private static long time;
    /** 弹体这一帧在哪：同色的弹体按 seed 分开攒点。 */
    public static void feed(int kind,long seed,Vec3 pos){
        if(pos==null||trails.size()>64&&!trails.containsKey(seed))return; // 极端情况：别让拖尾表无限长
        Trail t=trails.get(seed);
        if(t==null||t.kind!=kind){t=new Trail(kind,time);trails.put(seed,t);}
        t.last=time;
        Vec3 lastPoint=t.points.peekLast();
        if(lastPoint!=null&&lastPoint.distanceToSqr(pos)<1e-3)return; // 原地不动（停住的苍）不重复采点
        t.points.addLast(pos);
        int cap=t.kind==PURPLE?PURPLE_POINTS:BLUE_POINTS;
        while(t.points.size()>cap)t.points.removeFirst();
    }
    /** 弹体消失（苍收起 / 茈引爆）：立刻收掉这一条。 */
    public static void drop(long seed){trails.remove(seed);}
    public static void clear(){trails.clear();time=0;}
    public static void tick(){
        time++;
        trails.values().removeIf(t->time-t.last>(t.kind==PURPLE?PURPLE_KEEP:BLUE_KEEP));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(trails.isEmpty()||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        PoseStack ps=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();
        ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(Trail t:trails.values())ribbon(b,ps,t,cam,e.getPartialTick());
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();
        ps.popPose();
    }
    /** 一条从头（最老、最细）到尾（最新、最粗）的缎带：每段两边各外扩一个垂直于"段 + 视线"的宽度。 */
    private static void ribbon(BufferBuilder b,PoseStack ps,Trail t,Vec3 cam,float partial){
        int n=t.points.size();
        if(n<2)return;
        Vec3[] pts=t.points.toArray(new Vec3[0]);
        double baseW=t.kind==PURPLE?PURPLE_WIDTH:BLUE_WIDTH;
        int rgb=t.kind==PURPLE?PURPLE_RGB:BLUE_RGB;
        // 越靠近尾部（越老）越淡：尾巴散开，头贴着弹体最亮
        double fadeHead=Math.min(1.0,(time-t.last+partial)/(t.kind==PURPLE?PURPLE_KEEP:BLUE_KEEP));
        for(int i=0;i<n-1;i++){
            double f=n<2?1:(i/(double)(n-1));
            Vec3 a=pts[i],c=pts[i+1];
            Vec3 seg=c.subtract(a);
            if(seg.lengthSqr()<1e-6)continue;
            Vec3 view=cam.subtract(a);
            if(view.lengthSqr()<1e-6)continue;
            Vec3 side=seg.cross(view);
            if(side.lengthSqr()<1e-6)continue;
            side=side.normalize();
            double w=baseW*(0.28+0.72*f);
            int alpha=(int)(155*Math.pow(f,1.35)*fadeHead);
            if(alpha<4)continue;
            Vec3 a0=a.add(side.scale(w)),a1=a.subtract(side.scale(w));
            Vec3 c0=c.add(side.scale(w)),c1=c.subtract(side.scale(w));
            quad(b,ps,a0,a1,c1,c0,alpha<<24|rgb);                       // 主体
            double iw=w*0.45;
            quad(b,ps,a.add(side.scale(iw)),a.subtract(side.scale(iw)),c.subtract(side.scale(iw)),c.add(side.scale(iw)),(alpha*2/3)<<24|0xFFFFFF); // 内芯
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
    private GojoTrailClient(){}
}
