package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;

/** 在地形之后、实体之前替换胎海环境；生物和手持物仍走原渲染，不清空真实地形。 */
public final class BloodRealmRenderer {
    private static ShaderInstance shader;
    private static TextureTarget copy;
    private static boolean warned;
    @SubscribeEvent public static void shaders(RegisterShadersEvent event){shader=null;warned=false;try{event.registerShader(new ShaderInstance(event.getResourceProvider(),new ResourceLocation("sukuna","blood_realm"),DefaultVertexFormat.POSITION),s->shader=s);}catch(java.io.IOException|RuntimeException ex){com.mojang.logging.LogUtils.getLogger().warn("胎海环境替换不可用，保留球壁、血海和血雨",ex);}}
    public static void clear(){if(copy!=null){copy.destroyBuffers();copy=null;}warned=false;}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS||shader==null||warned)return;
        var mc=Minecraft.getInstance();Vec3 center=BloodClient.domainCenter(event.getCamera().getPosition());if(center==null)return;
        var main=mc.getMainRenderTarget();if(main.width<=0||main.height<=0||main.getDepthTextureId()<0)return;
        int read=GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),draw=GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);if(draw!=main.frameBufferId)return;
        boolean depth=GL30.glIsEnabled(GL30.GL_DEPTH_TEST),blend=GL30.glIsEnabled(GL30.GL_BLEND),cull=GL30.glIsEnabled(GL30.GL_CULL_FACE),mask=GL30.glGetBoolean(GL30.GL_DEPTH_WRITEMASK);
        var previous=RenderSystem.getShader();float[] colors=RenderSystem.getShaderColor().clone();int[] textures={RenderSystem.getShaderTexture(0),RenderSystem.getShaderTexture(1),RenderSystem.getShaderTexture(2)};
        try{
            if(copy==null)copy=new TextureTarget(main.width,main.height,true,Minecraft.ON_OSX);else if(copy.width!=main.width||copy.height!=main.height)copy.resize(main.width,main.height,Minecraft.ON_OSX);
            if(main.isStencilEnabled()&&!copy.isStencilEnabled())copy.enableStencil();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,main.frameBufferId);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,copy.frameBufferId);
            GL30.glBlitFramebuffer(0,0,main.width,main.height,0,0,main.width,main.height,GL30.GL_COLOR_BUFFER_BIT|GL30.GL_DEPTH_BUFFER_BIT,GL30.GL_NEAREST);main.bindWrite(true);
            Matrix4f vp=new Matrix4f(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());Vec3 relative=center.subtract(event.getCamera().getPosition());
            shader.safeGetUniform("InverseViewProjection").set(new Matrix4f(vp).invert());shader.safeGetUniform("ViewProjection").set(vp);shader.safeGetUniform("Center").set((float)relative.x,(float)relative.y,(float)relative.z);shader.safeGetUniform("Radius").set(100f);shader.safeGetUniform("BloodTime").set((float)((mc.level.getGameTime()+event.getPartialTick())/20.0));
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();RenderSystem.disableCull();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(()->shader);RenderSystem.setShaderTexture(0,copy.getColorTextureId());RenderSystem.setShaderTexture(1,copy.getDepthTextureId());RenderSystem.setShaderTexture(2,new ResourceLocation("sukuna","textures/effect/living_blood.png"));
            var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);b.vertex(-1,-1,0).endVertex();b.vertex(1,-1,0).endVertex();b.vertex(1,1,0).endVertex();b.vertex(-1,1,0).endVertex();BufferUploader.drawWithShader(b.end());
        }catch(RuntimeException ex){warned=true;com.mojang.logging.LogUtils.getLogger().warn("胎海环境替换暂停，保留独立结界效果",ex);}finally{
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);RenderSystem.depthMask(mask);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();RenderSystem.setShader(()->previous);for(int i=0;i<3;i++)RenderSystem.setShaderTexture(i,textures[i]);RenderSystem.setShaderColor(colors[0],colors[1],colors[2],colors[3]);
        }
    }
    private BloodRealmRenderer(){}
}
