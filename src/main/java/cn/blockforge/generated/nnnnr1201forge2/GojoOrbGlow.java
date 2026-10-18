package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 苍 / 赫 / 茈 的**顶点通路发光壳**（用户要求：只发光、阴影真实、用顶点通路）。
 *
 *  <p>和 Photon 加法亮斑的区别：这里走模组自己的 `POSITION_COLOR` 顶点通路，**开启深度测试**，
 *  所以光壳会被地形与生物**正常遮挡**、也会随环境明暗变化 —— 观感上是"一团半透明的实体光壳"，
 *  而不是"贴在屏幕上的亮斑"。三层同心面片由内到外变淡，颜色分别对应蓝(苍)/红(赫)/紫(茈)。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class GojoOrbGlow {
    private record Orb(int kind,Vec3 at,double size,long until){}
    private static final List<Orb> ORBS=new ArrayList<>();

    /** 由 {@code GojoFx} 在每次收到 BLUE/RED/PURPLE 特效时喂进来（寿命很短，靠每刻刷新延续）。 */
    public static void track(int kind,Vec3 at,double size){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        long now=mc.level.getGameTime();
        ORBS.removeIf(o->o.kind()==kind&&o.until()<now);
        ORBS.add(new Orb(kind,at,Math.max(0.6,size),now+8));
    }

    private static int colorOf(int kind){
        if(kind==GojoFx.BLUE)return 0x6E5AA8FF;   // 苍：蓝
        if(kind==GojoFx.RED)return 0x6EFF5B3A;    // 赫：红
        return 0x6EB062FF;                        // 茈：紫
    }

    /** 世界渲染：**保留深度测试**（不 disable），被挡住就是被挡住 —— 这就是"真实阴影"。 */
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(ORBS.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        long now=mc.level.getGameTime();
        ORBS.removeIf(o->o.until()<now);
        if(ORBS.isEmpty())return;
        Vec3 cam=e.getCamera().getPosition();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        var ps=e.getPoseStack();ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        for(Orb o:ORBS){
            int base=colorOf(o.kind());
            double t=(o.until()-now)/8.0;
            for(int layer=0;layer<3;layer++){
                double r=o.size()*(1.0+layer*0.55)*(0.92+0.08*Math.sin(now*0.5+layer));
                int a=(int)((((base>>>24)&255)*(1.0-layer*0.28))*Math.min(1,t*1.6));
                int col=(a<<24)|(base&0xFFFFFF);
                billboard(b,ps,o.at(),cam,r,col);
            }
        }
        BufferUploader.drawWithShader(b.end());
        ps.popPose();RenderSystem.enableCull();RenderSystem.disableBlend();
    }

    /** 一张面向相机的方形面片（顶点通路，深度测试由 RenderSystem 当前状态决定）。 */
    private static void billboard(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 cam,double r,int color){
        Vec3 n=cam.subtract(c);if(n.lengthSqr()<1e-6)n=new Vec3(0,0,1);n=n.normalize();
        Vec3 up=new Vec3(0,1,0).subtract(n.scale(n.y));if(up.lengthSqr()<1e-4)up=new Vec3(1,0,0);up=up.normalize();
        Vec3 right=n.cross(up).normalize();
        Vec3 u=up.scale(r),w=right.scale(r);
        quad(b,ps,c.subtract(w).subtract(u),c.add(w).subtract(u),c.add(w).add(u),c.subtract(w).add(u),color);
    }
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 p0,Vec3 p1,Vec3 p2,Vec3 p3,int color){
        for(Vec3 p:new Vec3[]{p0,p1,p2,p3})
            b.vertex(ps.last().pose(),(float)p.x,(float)p.y,(float)p.z).color(color).endVertex();
    }
    public static void clear(){ORBS.clear();}
    private GojoOrbGlow(){}
}
