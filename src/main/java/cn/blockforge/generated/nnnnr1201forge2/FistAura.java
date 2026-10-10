package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL30;

/** 只处理右下角的画面与咒力，不取消、重画或移动原版手臂。 */
public final class FistAura {
    private static TextureTarget copy;
    public static void clear(){if(copy!=null){copy.destroyBuffers();copy=null;}}
    public static void draw(GuiGraphics g,float age){draw(g,age,false);}
    public static void heat(GuiGraphics g,float age){draw(g,age,true);}
    private static void draw(GuiGraphics g,float age,boolean heat){
        Minecraft mc=Minecraft.getInstance();var main=mc.getMainRenderTarget();
        if(copy==null)copy=new TextureTarget(main.width,main.height,false,Minecraft.ON_OSX);
        else if(copy.width!=main.width||copy.height!=main.height)copy.resize(main.width,main.height,Minecraft.ON_OSX);
        g.flush();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,copy.frameBufferId);
        GL30.glBlitFramebuffer(0,0,main.width,main.height,0,0,main.width,main.height,GL30.GL_COLOR_BUFFER_BIT,GL30.GL_NEAREST);
        main.bindWrite(true);
        double w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        double cx=heat?w*0.5:w*0.77,cy=heat?h*0.5:h*0.81,rx=heat?w*0.65:Math.min(w*0.16,h*0.25),ry=heat?h*0.7:h*0.19;
        RenderSystem.disableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
        RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.setShaderTexture(0,copy.getColorTextureId());
        BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        int cells=20;
        for(int y=0;y<cells;y++)for(int x=0;x<cells;x++){
            sample(b,g,cx,cy,rx,ry,w,h,age,-1+2.0*x/cells,-1+2.0*y/cells);
            sample(b,g,cx,cy,rx,ry,w,h,age,-1+2.0*(x+1)/cells,-1+2.0*y/cells);
            sample(b,g,cx,cy,rx,ry,w,h,age,-1+2.0*(x+1)/cells,-1+2.0*(y+1)/cells);
            sample(b,g,cx,cy,rx,ry,w,h,age,-1+2.0*x/cells,-1+2.0*(y+1)/cells);
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.setShader(GameRenderer::getPositionColorShader);b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(int flame=0;flame<(heat?0:15);flame++)for(int i=0;i<14;i++){
            double t=i/14.0,u=(i+1)/14.0,phase=age*0.34+flame*2.39996;
            double base=cx+(flame/14.0-0.5)*rx*1.5;
            double height=ry*(0.65+0.3*Math.sin(phase*0.7));
            double x1=base+Math.sin(phase-t*5)*rx*0.10*t,y1=cy+ry*0.34-t*height;
            double x2=base+Math.sin(phase-u*5)*rx*0.10*u,y2=cy+ry*0.34-u*height;
            int alpha=(int)(210*(1-t));double width=(2.5+rx*0.04)*(1-t);
            line(b,g,x1,y1,x2,y2,width*1.6,(alpha<<24)|0x080108);
            line(b,g,x1,y1,x2,y2,width,((int)(alpha*0.85)<<24)|0xA90928);
            line(b,g,x1,y1,x2,y2,width*0.3,((int)(alpha*0.7)<<24)|0xF02A38);
        }
        BufferUploader.drawWithShader(b.end());RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.enableDepthTest();
    }
    private static void sample(BufferBuilder b,GuiGraphics g,double cx,double cy,double rx,double ry,double w,double h,float age,double x,double y){
        double radius=Math.sqrt(x*x+y*y),falloff=Math.max(0,1-radius);falloff*=falloff;
        double px=cx+x*rx,py=cy+y*ry,shift=Math.sin(radius*19-age*0.22)*2.5*falloff;
        double u=(px+x*shift+Math.sin(age*0.10+y*8)*falloff)/w,v=1-(py+y*shift)/h;
        b.vertex(g.pose().last().pose(),(float)px,(float)py,-70).uv((float)u,(float)v).color(255,255,255,255).endVertex();
    }
    private static void line(BufferBuilder b,GuiGraphics g,double x,double y,double xx,double yy,double width,int color){
        double dx=xx-x,dy=yy-y,length=Math.max(0.001,Math.sqrt(dx*dx+dy*dy));dx=dx/length*width;dy=dy/length*width;
        vertex(b,g,x-dy,y+dx,color);vertex(b,g,xx-dy,yy+dx,color);vertex(b,g,xx+dy,yy-dx,color);vertex(b,g,x+dy,y-dx,color);
    }
    private static void vertex(BufferBuilder b,GuiGraphics g,double x,double y,int c){b.vertex(g.pose().last().pose(),(float)x,(float)y,-60).color((c>>16)&255,(c>>8)&255,c&255,(c>>>24)&255).endVertex();}
}
