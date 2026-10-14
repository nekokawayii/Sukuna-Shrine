package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 玩家头上的法轮：**把「法轮」戴在头盔槽**才会出现（{@link MahoragaRules#wheelOnHead}）。
 *
 *  <p>它是**横着**悬在头顶上方的（像光环一样平躺、绕自己转），不覆盖也不参与头部建模——
 *  原版防具层对这件物品返回空模型（{@link MahoragaArmorModel}），所以头上不会出现方盒子，
 *  只有这一圈法轮。进度 = 适应完成度：每完全适应一种来源占 1 格，共 4 格。</p>
 *
 *  <p><b>转动特效（重做）</b>：转格时**不再使用 Photon 粒子**，改为用模组自己的顶点通路，
 *  在法轮外侧画一圈**由内向外扩散并渐隐的金色光环**（{@code POSITION_COLOR} + 加色混合），
 *  见 {@link #ring}。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class MahoragaWheelRenderer {
    private static final BbModel WHEEL=new BbModel("mahoraga_wheel","mahoraga_wheel");
    /** 平躺在头顶上方的高度：相对玩家碰撞箱顶（含潜行自动降低），0.28 格刚好压着头皮之上。 */
    private static final double HEAD_LIFT=0.28;
    private static final float SCALE=1.35f;
    /** 自转速度（度/刻）与转动光环的半径范围。 */
    private static final float SELF_SPIN=0.9f;
    private static final float RING_INNER=1.10f,RING_OUTER=2.05f;
    @SubscribeEvent public static void stage(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null)return;
        float age=mc.level.getGameTime()+e.getPartialTick();
        var camera=e.getCamera().getPosition();
        PoseStack ps=e.getPoseStack();
        for(MahoragaWheelClient.Entry entry:MahoragaWheelClient.entries()){
            Entity owner=mc.level.getEntity(entry.owner);
            if(!(owner instanceof net.minecraft.world.entity.player.Player player)||!player.isAlive())continue;
            if(!MahoragaRules.wheelOnHead(player))continue; // 没戴在头上：不画
            if(camera.distanceToSqr(owner.position())>96*96)continue;
            Vec3 at=owner.position().add(0,owner.getBbHeight()+HEAD_LIFT+Math.sin(age*0.06f)*0.03,0);
            float[] tint=tint(entry.cells);
            // 转动期间把法轮本体也提亮一档：即使那圈光环没注意到，也能一眼看出"轮子转了"
            if(entry.ring>0){
                float k=entry.flash*(1f-entry.ringProgress()*0.5f);
                tint=new float[]{Math.min(1f,tint[0]*(1f+0.35f*k)),Math.min(1f,tint[1]*(1f+0.55f*k)),Math.min(1f,tint[2]*(1f+1.10f*k))};
            }
            // ---- 法轮本体（横着，自转） ----
            ps.pushPose();
            ps.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);
            ps.mulPose(Axis.XP.rotationDegrees(-90));           // 盘面在 XY 平面 → 放平
            ps.mulPose(Axis.ZP.rotationDegrees(age*SELF_SPIN)); // 平躺后绕世界 Y 轴自转
            ps.scale(SCALE,SCALE,SCALE);
            RenderSystem.disableCull();RenderSystem.enableDepthTest();RenderSystem.depthMask(false);
            MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
            WHEEL.render(ps,buffers,15728880,"spin",entry.shown,tint[0],tint[1],tint[2],0.94f);
            buffers.endBatch();
            RenderSystem.depthMask(true);RenderSystem.enableCull();
            ps.popPose();
            // ---- 转动特效：自绘扩散金环（重做，无 Photon） ----
            if(entry.ring>0){
                ring(ps,camera,at,entry.ringProgress(),tint,entry.flash);
                // 兜底：再画一圈更大的亮环，保证"转了"这件事在任何视角都看得见
                bigRing(ps,camera,at,entry.ringProgress(),tint,entry.flash);
            }
            if(debug++%40==0)System.out.println("[SUKUNA-WHEEL] draw owner="+entry.owner+" cells="+entry.cells+" ring="+entry.ring+" shown="+entry.shown+" dist="+(int)Math.sqrt(camera.distanceToSqr(owner.position()))+" camY="+(int)camera.y+" atY="+(int)at.y);
        }
    }
    /** 一圈水平放置、由内向外扩散并渐隐的金色光环（POSITION_COLOR + 加色混合，纯自绘）。 */
    private static void ring(PoseStack ps,Vec3 camera,Vec3 at,float t,float[] tint,float flash){
        float fade=1f-Math.max(0,Math.min(1,t));            // 越往后越淡
        float radius=RING_INNER+(RING_OUTER-RING_INNER)*t;  // 由内向外扩散
        int alpha=(int)(255*fade*Math.max(0.2f,Math.min(1f,flash)));
        if(alpha<=6)return;
        int red=(int)(255*tint[0]),green=(int)(255*tint[1]),blue=(int)(255*tint[2]);
        int segments=48,inner=(int)(alpha*0.28f);
        float halfWidth=0.035f+0.03f*t;
        ps.pushPose();
        ps.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);
        RenderSystem.disableCull();RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE); // 加色
        RenderSystem.depthMask(false);RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        var pose=ps.last().pose();
        float bright=RING_INNER+(RING_OUTER-RING_INNER)*t*0.55f; // 内侧那圈走得慢，形成两道
        for(int i=0;i<segments;i++){
            double a0=i*Math.PI*2/segments,a1=(i+1)*Math.PI*2/segments;
            // 主环（外扩）
            band(b,pose,a0,a1,radius,halfWidth,red,green,blue,inner,alpha);
            // 内侧亮环（后随）
            band(b,pose,a0,a1,bright,halfWidth*0.75f,red,green,blue,(int)(alpha*0.55f),alpha);
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableDepthTest();RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();RenderSystem.enableCull();RenderSystem.disableBlend();
        ps.popPose();
    }
    /** 兜底大环：半径 2.2→3.4 格、更粗更亮，确保肉眼一定能看到。 */
    private static void bigRing(PoseStack ps,Vec3 camera,Vec3 at,float t,float[] tint,float flash){
        float fade=1f-Math.max(0,Math.min(1,t));
        int alpha=(int)(170*fade*Math.max(0.35f,Math.min(1f,flash)));
        if(alpha<=6)return;
        int red=(int)(255*tint[0]),green=(int)(255*tint[1]),blue=(int)(255*tint[2]);
        float radius=2.2f+1.2f*t,thickness=0.06f+0.05f*t;
        ps.pushPose();ps.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);
        RenderSystem.disableCull();RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        var pose=ps.last().pose();
        for(int i=0;i<48;i++){
            double a0=i*Math.PI*2/48,a1=(i+1)*Math.PI*2/48;
            band(b,pose,a0,a1,radius,thickness,red,green,blue,alpha/3,alpha);
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableDepthTest();RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();RenderSystem.enableCull();RenderSystem.disableBlend();
        ps.popPose();
    }
    /** 一段环带（内外两圈共用）。 */
    private static void band(BufferBuilder b,org.joml.Matrix4f pose,double a0,double a1,float radius,float halfWidth,int r,int g,int bl,int inner,int outer){
        float x0=(float)Math.cos(a0)*radius,z0=(float)Math.sin(a0)*radius;
        float x1=(float)Math.cos(a1)*radius,z1=(float)Math.sin(a1)*radius;
        vertex(b,pose,x0,-halfWidth,z0,r,g,bl,inner);
        vertex(b,pose,x1,-halfWidth,z1,r,g,bl,inner);
        vertex(b,pose,x1,halfWidth,z1,r,g,bl,outer);
        vertex(b,pose,x0,halfWidth,z0,r,g,bl,outer);
    }
    private static void vertex(BufferBuilder b,org.joml.Matrix4f pose,float x,float y,float z,int r,int g,int bl,int a){
        b.vertex(pose,x,y,z).color(r,g,bl,a).endVertex();
    }
    /** 转满 4 格时法轮由暗金变为亮金。 */
    private static float[] tint(int cells){
        if(cells>=MahoragaEntity.CELLS)return new float[]{1f,0.88f,0.44f};
        if(cells>0)return new float[]{1f,0.78f,0.30f};
        return new float[]{0.82f,0.70f,0.36f};
    }
    private static int debug;
    private MahoragaWheelRenderer(){}
}
