package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 「斩击切屏」：观测者对着镜头挥刀时，画面真的裂开——不是红屏，是玻璃裂纹，
 * 碎片之间露出的不是虚空，是**玩家的桌面**（复用壁纸那条屏幕坐标采样着色器）。
 * 全部在 GUI 空间：自设正交投影 + 单位模型矩阵，几何用像素坐标，桌面按 gl_FragCoord 采样。
 */
public final class ScreenCracks {
    private static final int LIFE=72;
    private record Hit(long start,long seed){}
    private static final List<Hit> hits=new ArrayList<>();
    private static long tick;
    public static void trigger(long seed){if(hits.size()>=6)hits.remove(0);hits.add(new Hit(tick,seed));}
    public static void advance(){tick++;hits.removeIf(h->tick-h.start>=LIFE);}
    public static void clear(){hits.clear();}
    public static boolean visible(){return !hits.isEmpty();}

    public static void render(float width,float height){
        if(hits.isEmpty())return;
        var mc=Minecraft.getInstance();
        ResourceLocation desktop=null;
        if(mc.player!=null&&WallpaperClient.hasImage(mc.player.getUUID()))desktop=WallpaperClient.texture(mc.player.getUUID());
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0f,width,height,0f,1000f,21000f),VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack mv=RenderSystem.getModelViewStack();
        mv.pushPose();mv.setIdentity();mv.translate(0f,0f,-2000f);RenderSystem.applyModelViewMatrix();
        PoseStack ps=new PoseStack();
        RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.depthMask(false);
        // ① 玻璃碎片：相邻两条裂纹之间的一角，露出桌面
        if(desktop!=null&&WallpaperSlash.shader()!=null){
            BufferBuilder b=Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(Hit hit:hits)shards(b,ps,hit,width,height);
            RenderSystem.setShader(WallpaperSlash::shader);
            WallpaperSlash.desktopUniforms();
            RenderSystem.setShaderTexture(0,desktop);
            BufferUploader.drawWithShader(b.end());
        }
        // ② 裂纹线 + 中心冲击点
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        for(Hit hit:hits)cracks(b,ps,hit,width,height);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(b.end());
        mv.popPose();RenderSystem.applyModelViewMatrix();
        RenderSystem.enableCull();RenderSystem.depthMask(true);RenderSystem.disableBlend();
    }

    private static void shards(BufferBuilder b,PoseStack ps,Hit hit,float width,float height){
        Random rng=new Random(hit.seed()*1000003L+7);
        double cx=width*(0.3+rng.nextDouble()*0.4),cy=height*(0.3+rng.nextDouble()*0.4);
        int rays=5+rng.nextInt(3);
        double[] ang=new double[rays],reach=new double[rays];
        for(int i=0;i<rays;i++){ang[i]=i*Math.PI*2/rays+(rng.nextDouble()-0.5)*0.5;reach[i]=(0.35+rng.nextDouble()*0.55)*Math.max(width,height);}
        float alpha=fade(hit);
        int color=((int)(170*alpha)<<24)|0xFFFFFF;
        for(int i=0;i<rays;i++){
            int j=(i+1)%rays;
            double x1=cx+Math.cos(ang[i])*reach[i]*0.55,y1=cy+Math.sin(ang[i])*reach[i]*0.55;
            double x2=cx+Math.cos(ang[j])*reach[j]*0.55,y2=cy+Math.sin(ang[j])*reach[j]*0.55;
            tri(b,ps,cx,cy,x1,y1,x2,y2,color);
        }
    }
    private static void cracks(BufferBuilder b,PoseStack ps,Hit hit,float width,float height){
        Random rng=new Random(hit.seed()*1000003L+7);
        double cx=width*(0.3+rng.nextDouble()*0.4),cy=height*(0.3+rng.nextDouble()*0.4);
        int rays=5+rng.nextInt(3);
        double[] ang=new double[rays],reach=new double[rays];
        for(int i=0;i<rays;i++){ang[i]=i*Math.PI*2/rays+(rng.nextDouble()-0.5)*0.5;reach[i]=(0.35+rng.nextDouble()*0.55)*Math.max(width,height);}
        float alpha=fade(hit);
        for(int i=0;i<rays;i++){
            double px=cx,py=cy;
            for(int k=1;k<=6;k++){
                double t=k/6.0,len=reach[i]*t,jitter=(rng.nextDouble()-0.5)*30*t;
                double nx=cx+Math.cos(ang[i])*len+Math.cos(ang[i]+Math.PI/2)*jitter;
                double ny=cy+Math.sin(ang[i])*len+Math.sin(ang[i]+Math.PI/2)*jitter;
                line(b,ps,px,py,nx,ny,6.5f,((int)(66*alpha)<<24)|0x101014);
                line(b,ps,px,py,nx,ny,2.4f,((int)(225*alpha)<<24)|0xFFFFFF);
                px=nx;py=ny;
            }
        }
        double r=9+22*(1-fade(hit));
        quad(b,ps,cx-r,cy-r,cx+r,cy-r,cx+r,cy+r,cx-r,cy+r,((int)(190*alpha)<<24)|0xFFFFFF);
    }
    private static float fade(Hit hit){return Math.max(0,Math.min(1,1-(tick-hit.start)/(float)LIFE))*(tick-hit.start<2?1f:1f);}
    private static void tri(BufferBuilder b,PoseStack ps,double x0,double y0,double x1,double y1,double x2,double y2,int color){
        vertex(b,ps,x0,y0,color);vertex(b,ps,x1,y1,color);vertex(b,ps,x2,y2,color);vertex(b,ps,x2,y2,color);
    }
    private static void quad(BufferBuilder b,PoseStack ps,double x0,double y0,double x1,double y1,double x2,double y2,double x3,double y3,int color){
        vertex(b,ps,x0,y0,color);vertex(b,ps,x1,y1,color);vertex(b,ps,x2,y2,color);vertex(b,ps,x3,y3,color);
    }
    private static void line(BufferBuilder b,PoseStack ps,double x0,double y0,double x1,double y1,float w,int color){
        double dx=x1-x0,dy=y1-y0,len=Math.sqrt(dx*dx+dy*dy);if(len<1e-4)return;
        double nx=-dy/len*w/2,ny=dx/len*w/2;
        quad(b,ps,x0+nx,y0+ny,x1+nx,y1+ny,x1-nx,y1-ny,x0-nx,y0-ny,color);
    }
    private static void vertex(BufferBuilder b,PoseStack ps,double x,double y,int color){
        b.vertex(ps.last().pose(),(float)x,(float)y,0f).color((color>>16)&255,(color>>8)&255,color&255,(color>>>24)&255).endVertex();
    }
    private ScreenCracks(){}
}
