package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL30;

/** 每帧复制当前画面，按随机切线裁成两片并反向错位，结束时释放缓存。 */
public final class ScreenSeverance {
    private static TextureTarget copy;
    public static void clear(){if(copy!=null){copy.destroyBuffers();copy=null;}}
    private record Point(double x,double y){}
    public static void draw(GuiGraphics graphics,float degrees,float remaining){
        var mc=Minecraft.getInstance();var main=mc.getMainRenderTarget();int width=main.width,height=main.height;
        if(copy==null)copy=new TextureTarget(width,height,false,Minecraft.ON_OSX);else if(copy.width!=width||copy.height!=height)copy.resize(width,height,Minecraft.ON_OSX);
        graphics.flush();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,main.frameBufferId);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,copy.frameBufferId);
        GL30.glBlitFramebuffer(0,0,width,height,0,0,width,height,GL30.GL_COLOR_BUFFER_BIT,GL30.GL_NEAREST);main.bindWrite(true);
        double w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight(),angle=Math.toRadians(degrees),nx=-Math.sin(angle),ny=Math.cos(angle);
        float fade=Math.max(0,Math.min(1,remaining));double offset=10*Math.min(1,(1-fade)*5)*fade;
        RenderSystem.disableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.setShaderTexture(0,copy.getColorTextureId());RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(int sign:new int[]{-1,1}){
            List<Point> polygon=clip(List.of(new Point(0,0),new Point(w,0),new Point(w,h),new Point(0,h)),w/2,h/2,nx,ny,sign);
            for(int i=1;i+1<polygon.size();i++)for(Point p:List.of(polygon.get(0),polygon.get(i),polygon.get(i+1))){double dx=-sign*nx*offset,dy=-sign*ny*offset;b.vertex(graphics.pose().last().pose(),(float)(p.x+dx),(float)(p.y+dy),-80).uv((float)(p.x/w),(float)(1-p.y/h)).color(255,255,255,(int)(255*Math.min(1,fade*3))).endVertex();}
        }
        BufferUploader.drawWithShader(b.end());RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.enableDepthTest();
    }
    private static List<Point> clip(List<Point> input,double cx,double cy,double nx,double ny,int sign){List<Point> out=new ArrayList<>();Point a=input.get(input.size()-1);double da=((a.x-cx)*nx+(a.y-cy)*ny)*sign;for(Point b:input){double db=((b.x-cx)*nx+(b.y-cy)*ny)*sign;if((da>=0)!=(db>=0)){double t=da/(da-db);out.add(new Point(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t));}if(db>=0)out.add(b);a=b;da=db;}return out;}
}
