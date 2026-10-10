package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL30;
import java.util.List;

/** 仅在显存复制游戏自身的最终画面（含聊天），不读取桌面，不写文件，不联网。 */
public final class FinaleScreenGlass {
    private static TextureTarget snapshot;
    private static long token;
    public static void clear(){if(snapshot!=null){snapshot.destroyBuffers();snapshot=null;}token=0;}
    public static void draw(GuiGraphics g,int w,int h,double age,long cast){
        var mc=Minecraft.getInstance();var main=mc.getMainRenderTarget();g.flush();
        if(snapshot==null||snapshot.width!=main.width||snapshot.height!=main.height||token!=cast){
            clear();token=cast;snapshot=new TextureTarget(main.width,main.height,false,Minecraft.ON_OSX);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,snapshot.frameBufferId);
            GL30.glBlitFramebuffer(0,0,main.width,main.height,0,0,main.width,main.height,GL30.GL_COLOR_BUFFER_BIT,GL30.GL_NEAREST);
            main.bindWrite(true);
        }
        g.fill(0,0,w,h,0xFF080A10);g.flush();
        RenderSystem.disableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.setShaderTexture(0,snapshot.getColorTextureId());RenderSystem.setShaderColor(1,1,1,1);
        var from=GlassGeometryRules.shards(w,h,0);var to=GlassGeometryRules.shards(w,h,age);
        BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_TEX_COLOR);
        int alpha=(int)(255*Math.max(0,1-age/10));
        for(int i=0;i<to.size();i++){
            var a=from.get(i);var z=to.get(i);var uv=List.of(a.a(),a.b(),a.c());var points=List.of(z.a(),z.b(),z.c());
            for(int j=0;j<3;j++){var p=points.get(j);var texture=uv.get(j);b.vertex(g.pose().last().pose(),(float)p.x(),(float)p.y(),0).uv((float)(texture.x()/w),(float)(1-texture.y()/h)).color(255,255,255,alpha).endVertex();}
        }
        BufferUploader.drawWithShader(b.end());RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.enableDepthTest();
    }
    private FinaleScreenGlass(){}
}
