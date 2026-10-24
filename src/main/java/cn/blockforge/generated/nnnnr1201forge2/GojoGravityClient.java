package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL30;

/** 独立世界空间亮核和光子环；不依赖粒子模组、屏幕深度或后期着色器成功。 */
public final class GojoGravityClient {
    public record Orb(Vec3 position,float size,int color,boolean lens){}
    static void render(RenderLevelStageEvent e,List<Orb> orbs,double time){
        if(orbs.isEmpty())return;
        PoseStack ps=e.getPoseStack();Vec3 camera=e.getCamera().getPosition();
        boolean depth=GL30.glIsEnabled(GL30.GL_DEPTH_TEST),blend=GL30.glIsEnabled(GL30.GL_BLEND),cull=GL30.glIsEnabled(GL30.GL_CULL_FACE);
        boolean mask=GL30.glGetBoolean(GL30.GL_DEPTH_WRITEMASK);
        int srcRgb=GL30.glGetInteger(GL30.GL_BLEND_SRC_RGB),dstRgb=GL30.glGetInteger(GL30.GL_BLEND_DST_RGB);
        int srcAlpha=GL30.glGetInteger(GL30.GL_BLEND_SRC_ALPHA),dstAlpha=GL30.glGetInteger(GL30.GL_BLEND_DST_ALPHA);
        var shader=RenderSystem.getShader();float[] color=RenderSystem.getShaderColor().clone();
        ps.pushPose();try{
            ps.translate(-camera.x,-camera.y,-camera.z);
            RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE,GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
            BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(Orb orb:orbs)orb(b,ps,orb,camera,time);
            BufferUploader.drawWithShader(b.end());
        }finally{
            ps.popPose();RenderSystem.depthMask(mask);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
        }
    }
    private static void orb(BufferBuilder b,PoseStack ps,Orb orb,Vec3 camera,double time){
        Vec3 at=orb.position(),n=camera.subtract(at);double distance=n.length();if(distance<.08)return;n=n.normalize();
        Vec3 side=n.cross(new Vec3(0,1,0));if(side.lengthSqr()<1e-6)side=new Vec3(1,0,0);else side=side.normalize();
        Vec3 up=n.cross(side).normalize();double size=orb.size(),radius=size*GojoOpticsRules.LENS_SCALE;
        // 镜头进入球内时淡出，避免在近脸蓄力时铺满整个屏幕。
        float fade=(float)Math.min(1,Math.max(0,(distance-size*.25)/Math.max(.2,size*.7)));
        if(fade<=0)return;
        int tint=orb.color(),pale=mix(tint,0xFFFFFF,.72f);
        for(int i=0;i<12;i++){
            double r0=size*i/7.0,r1=size*(i+1)/7.0;
            float alpha=(float)(Math.exp(-Math.pow((i+.5)/7.0,2)*2.3)*.24*fade);
            band(b,ps,at,side,up,r0,r1,tint,alpha,64);
        }
        band(b,ps,at,side,up,0,size*.26,pale,.8f*fade,48);
        if(!orb.lens())return;
        // 透镜环放在粒子球外，避免亮核把细环盖住；不拿光环冒充背景折射。
        double pulse=1+.025*Math.sin(time*.12);
        band(b,ps,at,side,up,radius*.97*pulse,radius*1.025*pulse,0xF5F4FF,.90f*fade,96);
        band(b,ps,at,side,up,radius*1.025*pulse,radius*1.12*pulse,tint,.40f*fade,96);
        band(b,ps,at,side,up,radius*1.12*pulse,radius*1.3*pulse,tint,.12f*fade,96);
        for(int turn=0;turn<2;turn++){
            double phase=time*(turn==0?.055:-.04)+turn*2.1,r=radius*(turn==0?1.32:1.6);
            for(int j=0;j<48;j++){
                double a=phase+j*Math.PI/32,z=phase+(j+1)*Math.PI/32;
                Vec3 p=at.add(side.scale(Math.cos(a)*r)).add(up.scale(Math.sin(a)*r*.42));
                Vec3 q=at.add(side.scale(Math.cos(z)*r)).add(up.scale(Math.sin(z)*r*.42));
                Vec3 width=up.scale(size*.025);
                quad(b,ps,p.subtract(width),q.subtract(width),q.add(width),p.add(width),pale,(float)(.45*Math.sin((j+.5)/48*Math.PI))*fade);
            }
        }
    }
    private static int mix(int a,int b,float t){
        int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t),g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t),v=(int)((a&255)*(1-t)+(b&255)*t);
        return r<<16|g<<8|v;
    }
    private static void band(BufferBuilder b,PoseStack ps,Vec3 at,Vec3 side,Vec3 up,double inner,double outer,int color,float alpha,int count){
        for(int i=0;i<count;i++){
            double a=i*Math.PI*2/count,z=(i+1)*Math.PI*2/count;
            Vec3 p=side.scale(Math.cos(a)).add(up.scale(Math.sin(a))),q=side.scale(Math.cos(z)).add(up.scale(Math.sin(z)));
            quad(b,ps,at.add(p.scale(inner)),at.add(q.scale(inner)),at.add(q.scale(outer)),at.add(p.scale(outer)),color,alpha);
        }
    }
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 c,Vec3 d,Vec3 e,int color,float alpha){
        var matrix=ps.last().pose();int r=color>>16&255,g=color>>8&255,v=color&255,opacity=Math.max(0,Math.min(255,(int)(alpha*255)));
        b.vertex(matrix,(float)a.x,(float)a.y,(float)a.z).color(r,g,v,opacity).endVertex();
        b.vertex(matrix,(float)c.x,(float)c.y,(float)c.z).color(r,g,v,opacity).endVertex();
        b.vertex(matrix,(float)d.x,(float)d.y,(float)d.z).color(r,g,v,opacity).endVertex();
        b.vertex(matrix,(float)e.x,(float)e.y,(float)e.z).color(r,g,v,opacity).endVertex();
    }
    private GojoGravityClient(){}
}
