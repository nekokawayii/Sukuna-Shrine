package character.jogo;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * 盖棺铁围山的球壳。
 * <p>两层：①纯色不透明底壳（保证任何环境下都能看见、几何可验证）；
 * ②在底壳上再叠一层贴图壳（用户给的熔岩裂缝图，等距圆柱 UV 只贴一圈、clamp 采样，避免重复接缝把贴图撕成"红黑圆环"）。
 * 都不走自绘 core shader，光影包也能接管。</p>
 */
public final class JogoDomainRender {
    private static final int RINGS=64, SEGMENTS=64;
    private static RenderType solidType, texturedType;

    private static RenderType.CompositeState.CompositeStateBuilder states(RenderStateShard.TextureStateShard texture) {
        return RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(texture==null?GameRenderer::getPositionColorShader:GameRenderer::getPositionTexColorShader))
            .setTransparencyState(new RenderStateShard.TransparencyStateShard("jogo_no_transparency",()->{},()->{}))
            .setCullState(new RenderStateShard.CullStateShard(false))
            .setDepthTestState(new RenderStateShard.DepthTestStateShard("jogo_lequal",515))
            .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true,true));
    }
    private static RenderType solid() {
        if(solidType==null)solidType=RenderType.create("sukuna_jogo_shell",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false,false,
            states(null).createCompositeState(false));
        return solidType;
    }
    private static RenderType textured() {
        if(texturedType==null)texturedType=RenderType.create("sukuna_jogo_shell_tex",
            DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false,false,
            states(new RenderStateShard.TextureStateShard(JogoDomainShaders.WALL,false,false)).createCompositeState(false));
        return texturedType;
    }

    public static void render(RenderLevelStageEvent e) {
        var domains=JogoFxClient.domains();
        var mc=Minecraft.getInstance();
        if(domains.isEmpty()||mc.level==null)return;
        var cam=e.getCamera().getPosition();
        var pose=e.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x,-cam.y,-cam.z);
        var matrix=pose.last().pose();
        var source=mc.renderBuffers().bufferSource();
        // ① 纯色底壳
        var solid=solid();
        VertexConsumer base=source.getBuffer(solid);
        for(var domain:domains) sphere(domain,base,matrix,false);
        source.endBatch(solid);
        // ② 贴图壳（贴图缺失时自动跳过）
        if(JogoDomainShaders.textured&&JogoDomainShaders.WALL!=null){
            var tex=textured();
            VertexConsumer layer=source.getBuffer(tex);
            for(var domain:domains) sphere(domain,layer,matrix,true);
            source.endBatch(tex);
        }
        pose.popPose();
    }

    private static void sphere(JogoFxClient.Domain domain,VertexConsumer vc,org.joml.Matrix4f matrix,boolean textured) {
        double r=Math.max(6,domain.radius());
        Vec3 c=domain.center();
        for(int i=0;i<RINGS;i++){
            double lat0=Math.PI*i/RINGS,lat1=Math.PI*(i+1)/RINGS;
            double y0=Math.cos(lat0)*r,y1=Math.cos(lat1)*r;
            double rr0=Math.sin(lat0)*r,rr1=Math.sin(lat1)*r;
            float f0=(float)(0.5-Math.cos(lat0)*0.5),f1=(float)(0.5-Math.cos(lat1)*0.5);
            int c0=shade(f0),c1=shade(f1);
            float v0=(float)i/RINGS,v1=(float)(i+1)/RINGS;
            for(int j=0;j<SEGMENTS;j++){
                double a0=2*Math.PI*j/SEGMENTS,a1=2*Math.PI*(j+1)/SEGMENTS;
                float u0=(float)j/SEGMENTS,u1=(float)(j+1)/SEGMENTS;
                if(textured){
                    vertexTex(vc,matrix,c.x+Math.cos(a0)*rr0,c.y+y0,c.z+Math.sin(a0)*rr0,u0,v0);
                    vertexTex(vc,matrix,c.x+Math.cos(a1)*rr0,c.y+y0,c.z+Math.sin(a1)*rr0,u1,v0);
                    vertexTex(vc,matrix,c.x+Math.cos(a1)*rr1,c.y+y1,c.z+Math.sin(a1)*rr1,u1,v1);
                    vertexTex(vc,matrix,c.x+Math.cos(a0)*rr1,c.y+y1,c.z+Math.sin(a0)*rr1,u0,v1);
                }else{
                    vertexColor(vc,matrix,c.x+Math.cos(a0)*rr0,c.y+y0,c.z+Math.sin(a0)*rr0,c0);
                    vertexColor(vc,matrix,c.x+Math.cos(a1)*rr0,c.y+y0,c.z+Math.sin(a1)*rr0,c0);
                    vertexColor(vc,matrix,c.x+Math.cos(a1)*rr1,c.y+y1,c.z+Math.sin(a1)*rr1,c1);
                    vertexColor(vc,matrix,c.x+Math.cos(a0)*rr1,c.y+y1,c.z+Math.sin(a0)*rr1,c1);
                }
            }
        }
    }
    private static int shade(float f) {
        float t=Math.max(0,Math.min(1,f));
        int r=(int)(38+150*t),g=(int)(24+52*t*t),b=(int)(22+26*t*t*t);
        return 0xFF000000|(r<<16)|(g<<8)|b;
    }
    private static void vertexColor(VertexConsumer vc,org.joml.Matrix4f m,double x,double y,double z,int color) {
        vc.vertex(m,(float)x,(float)y,(float)z).color(color).endVertex();
    }
    private static void vertexTex(VertexConsumer vc,org.joml.Matrix4f m,double x,double y,double z,float u,float v) {
        vc.vertex(m,(float)x,(float)y,(float)z).color(200,150,140,255).uv(u,v).endVertex();   // 压暗，保持火山岩质感
    }
    private JogoDomainRender() {}
}
