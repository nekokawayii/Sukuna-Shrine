package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 苍 / 赫 / 茈 的**加法光斑层**：往地面上糊"被照亮的亮斑"，让画面真的变亮。
 *
 * <p>为什么需要它：原版光照亮度上限就是 15 级（萤石级别），
 * {@link GojoColoredLights} 已经把三个光源方块顶到 15，**再没有更强的档位**。
 * 想要"更亮 10 倍"，只能靠**加法混合**在画面上叠亮度 —— 这一层就是那个倍数。</p>
 *
 * <p>和 r269 删掉的那层面片的区别（用户当时说"一层半透明贴图"）：</p>
 * <ul>
 *   <li>用 {@code glow.png}（中心 α248 → 边缘 0 的真径向渐变）**贴图**，不是一个纯色方块，没有边界感；</li>
 *   <li>**加法混合**（SRC_ALPHA, ONE）：是往画面加光，不是盖一层灰白半透明；</li>
 *   <li>光斑**贴着命中面**投放（地板就平躺、墙就贴墙），是"照在地面上"而不是"悬在空中的板"。</li>
 * </ul>
 *
 * <p>调亮度的旋钮只有 {@link #GAIN} 一个：它同时乘所有光斑的 alpha，改动直观。</p>
 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class GojoLightSpill {
    // ---- 可调常量 ----
    /** 总提亮倍数：所有光斑 alpha 统一乘它。用户觉得不够亮就加这里（会过曝，1.0=本版基准，2.5=当前）。 */
    private static final float GAIN=2.5f;
    /** 地面光斑半径 = 光球尺寸 × 这个系数（再加一点随距离的放大）。 */
    private static final double POOL_SCALE=2.2;
    /** 光斑最大半径（格）。 */
    private static final double POOL_MAX=9.0;
    /** 射线最远多少格。 */
    private static final double POOL_RANGE=20.0;
    /** 光斑中心 alpha（0-255），乘 {@link #GAIN}。 */
    private static final int POOL_ALPHA=210;
    /** 光球周围的空气柔光半径系数与 alpha。 */
    private static final double AIR_SCALE=3.6;
    private static final int AIR_ALPHA=110;
    /** 多少刻重算一次射线（不是每帧）。 */
    private static final int RAY_TICKS=2;
    /** 光球最多同时记几个（按颜色去重，同色只留最新的那个）。 */
    private static final int MAX_ORBS=4;

    private static final ResourceLocation TEX=new ResourceLocation("sukuna","textures/effect/glow.png");

    /** 一处命中面上的光斑：中心点、两条切向轴、半径、颜色。 */
    private record Pool(Vec3 at,Vec3 u,Vec3 v,double r,int color){}
    /** 一个光球（含它这一轮算出来的光斑）。 */
    private static final class Orb {
        final int kind; Vec3 at; double size; long until; long nextCast;
        final List<Pool> pools=new ArrayList<>();
        Orb(int kind,Vec3 at,double size,long until){this.kind=kind;this.at=at;this.size=size;this.until=until;}
    }
    private static final List<Orb> ORBS=new ArrayList<>();

    /** 由 {@code GojoFx} 在收到 BLUE/RED/PURPLE 时喂进来（每刻刷新，寿命很短）。 */
    public static void track(int kind,Vec3 at,double size){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        long now=mc.level.getGameTime();
        for(Orb orb:ORBS){
            if(orb.kind!=kind)continue;
            // 同色只维护一条：位置/尺寸就地更新，光斑与射线计时保留（不能每刻重算射线）
            orb.at=at;orb.size=Math.max(0.6,size);orb.until=now+8;
            return;
        }
        if(ORBS.size()>=MAX_ORBS)ORBS.remove(0);
        ORBS.add(new Orb(kind,at,Math.max(0.6,size),now+8));
    }

    private static int colorOf(int kind){
        if(kind==GojoFx.BLUE)return 0x5AA8FF;    // 苍：蓝
        if(kind==GojoFx.RED)return 0xFF5B3A;     // 赫：红
        return 0xB062FF;                         // 茈：紫
    }

    /** 射线重新投一轮光斑（每 {@link #RAY_TICKS} 刻一次，不是每帧）。 */
    private static void cast(Orb orb){
        var mc=Minecraft.getInstance();
        orb.pools.clear();
        if(mc.level==null||mc.player==null)return;
        long now=mc.level.getGameTime();
        orb.nextCast=now+RAY_TICKS;
        Vec3 from=orb.at;
        // 1 条正下方 + 4 条斜下 + 4 条稍平，够铺满脚下与四面的地面/墙。
        Vec3[] dirs={
            new Vec3(0,-1,0),
            new Vec3(0.55,-1,0).normalize(),new Vec3(-0.55,-1,0).normalize(),
            new Vec3(0,-1,0.55).normalize(),new Vec3(0,-1,-0.55).normalize(),
            new Vec3(1.6,-1,0).normalize(),new Vec3(-1.6,-1,0).normalize(),
            new Vec3(0,-1,1.6).normalize(),new Vec3(0,-1,-1.6).normalize()
        };
        int base=colorOf(orb.kind);
        for(Vec3 dir:dirs){
            Vec3 to=from.add(dir.scale(POOL_RANGE));
            BlockHitResult hit;
            try{hit=mc.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));}
            catch(Throwable ignored){continue;}
            if(hit.getType()!=HitResult.Type.BLOCK)continue;
            Vec3 p=hit.getLocation();
            double dist=p.distanceTo(from);
            if(dist>POOL_RANGE)continue;
            double r=Math.min(POOL_MAX,orb.size*POOL_SCALE+dist*0.22);
            int a=(int)Math.max(6,Math.min(255,POOL_ALPHA*(1.0-dist/POOL_RANGE*0.8)*GAIN));
            int col=(Math.min(255,a)<<24)|base;
            Vec3[] uv=tangents(hit.getDirection());
            orb.pools.add(new Pool(p.add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.03)),uv[0],uv[1],r,col));
        }
    }

    /** 命中面的两条切向轴：地板铺平、墙就贴在墙上。 */
    private static Vec3[] tangents(net.minecraft.core.Direction d){
        return switch(d.getAxis()){
            case Y -> new Vec3[]{new Vec3(1,0,0),new Vec3(0,0,1)};
            case Z -> new Vec3[]{new Vec3(1,0,0),new Vec3(0,1,0)};
            case X -> new Vec3[]{new Vec3(0,0,1),new Vec3(0,1,0)};
        };
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(ORBS.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        long now=mc.level.getGameTime();
        ORBS.removeIf(o->o.until<now);
        if(ORBS.isEmpty())return;
        for(Orb orb:ORBS)if(now>=orb.nextCast)cast(orb);

        Vec3 cam=e.getCamera().getPosition();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE,
            GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ONE);   // 加法：往画面加光
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0,TEX);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        var ps=e.getPoseStack();ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        for(Orb orb:ORBS){
            double fade=Math.min(1.0,(orb.until-now)/4.0);
            for(Pool p:orb.pools){
                int col=withAlpha(p.color(),(int)(((p.color()>>>24)&255)*fade));
                quad(b,ps,p.at().subtract(p.u().scale(p.r())).subtract(p.v().scale(p.r())),
                        p.at().add(p.u().scale(p.r())).subtract(p.v().scale(p.r())),
                        p.at().add(p.u().scale(p.r())).add(p.v().scale(p.r())),
                        p.at().subtract(p.u().scale(p.r())).add(p.v().scale(p.r())),col);
            }
            // 光球身边的空气柔光：一张面向相机的加法渐变面片，给"光在空气里散开"的感觉
            int air=(int)Math.min(255,AIR_ALPHA*fade*GAIN);
            billboard(b,ps,orb.at,cam,orb.size*AIR_SCALE,withAlpha(colorOf(orb.kind),air));
        }
        BufferUploader.drawWithShader(b.end());
        ps.popPose();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static int withAlpha(int argb,int a){return ((Math.min(255,Math.max(0,a))&255)<<24)|(argb&0xFFFFFF);}

    private static void billboard(BufferBuilder b,PoseStack ps,Vec3 c,Vec3 cam,double r,int color){
        Vec3 n=cam.subtract(c);if(n.lengthSqr()<1e-6)n=new Vec3(0,0,1);n=n.normalize();
        Vec3 up=new Vec3(0,1,0).subtract(n.scale(n.y));if(up.lengthSqr()<1e-4)up=new Vec3(1,0,0);up=up.normalize();
        Vec3 right=n.cross(up).normalize();
        Vec3 u=up.scale(r),w=right.scale(r);
        quad(b,ps,c.subtract(w).subtract(u),c.add(w).subtract(u),c.add(w).add(u),c.subtract(w).add(u),color);
    }

    private static void quad(BufferBuilder b,PoseStack ps,Vec3 p0,Vec3 p1,Vec3 p2,Vec3 p3,int color){
        b.vertex(ps.last().pose(),(float)p0.x,(float)p0.y,(float)p0.z).uv(0,0).color(color).endVertex();
        b.vertex(ps.last().pose(),(float)p1.x,(float)p1.y,(float)p1.z).uv(1,0).color(color).endVertex();
        b.vertex(ps.last().pose(),(float)p2.x,(float)p2.y,(float)p2.z).uv(1,1).color(color).endVertex();
        b.vertex(ps.last().pose(),(float)p3.x,(float)p3.y,(float)p3.z).uv(0,1).color(color).endVertex();
    }

    public static void clear(){ORBS.clear();}
    private GojoLightSpill(){}
}
