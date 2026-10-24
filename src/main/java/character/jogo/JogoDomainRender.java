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
 * 盖棺铁围山的贴图球壳。
 * <p>① 不走 RegisterShadersEvent：Oculus/Iris 光影会绕开自绘 core shader，球壳会整个消失；
 * ② 不用 entity 顶点格式：NEW_ENTITY 要求 overlay/normal 全填，少一个就崩游戏。
 * 所以改用**原版 position_tex_color 着色器 + 自建 POSITION_COLOR_TEX RenderType**，光影包也能正常接管。</p>
 */
public final class JogoDomainRender {
    private static final int RINGS=64, SEGMENTS=64;
    private static RenderType shellType;

    private static RenderType shellType() {
        if(shellType==null){
            shellType=RenderType.create("sukuna_jogo_shell",
                DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, false,
                RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(JogoDomainShaders.WALL,false,false))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("jogo_no_transparency",()->{},()->{}))
                    .setCullState(new RenderStateShard.CullStateShard(false))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("jogo_lequal",515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true,true))
                    .createCompositeState(false));
        }
        return shellType;
    }

    public static void render(RenderLevelStageEvent e) {
        var domains=JogoFxClient.domains();
        var mc=Minecraft.getInstance();
        if(domains.isEmpty()||mc.level==null||JogoDomainShaders.WALL==null)return;
        var cam=e.getCamera().getPosition();
        var pose=e.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x,-cam.y,-cam.z);
        var matrix=pose.last().pose();
        var type=shellType();
        var source=mc.renderBuffers().bufferSource();
        VertexConsumer vc=source.getBuffer(type);
        for(var domain:domains){
            double r=Math.max(6,domain.radius());
            Vec3 c=domain.center();
            for(int i=0;i<RINGS;i++){
                double lat0=Math.PI*i/RINGS,lat1=Math.PI*(i+1)/RINGS;
                double y0=Math.cos(lat0)*r,y1=Math.cos(lat1)*r;
                double rr0=Math.sin(lat0)*r,rr1=Math.sin(lat1)*r;
                float v0=(float)i/RINGS,v1=(float)(i+1)/RINGS;
                for(int j=0;j<SEGMENTS;j++){
                    double a0=2*Math.PI*j/SEGMENTS,a1=2*Math.PI*(j+1)/SEGMENTS;
                    float u0=(float)j/SEGMENTS,u1=(float)(j+1)/SEGMENTS;
                    vertex(vc,matrix,c.x+Math.cos(a0)*rr0,c.y+y0,c.z+Math.sin(a0)*rr0,u0*2f,v0);
                    vertex(vc,matrix,c.x+Math.cos(a1)*rr0,c.y+y0,c.z+Math.sin(a1)*rr0,u1*2f,v0);
                    vertex(vc,matrix,c.x+Math.cos(a1)*rr1,c.y+y1,c.z+Math.sin(a1)*rr1,u1*2f,v1);
                    vertex(vc,matrix,c.x+Math.cos(a0)*rr1,c.y+y1,c.z+Math.sin(a0)*rr1,u0*2f,v1);
                }
            }
        }
        source.endBatch(type);
        pose.popPose();
    }
    private static void vertex(VertexConsumer vc,org.joml.Matrix4f matrix,double x,double y,double z,float u,float v) {
        vc.vertex(matrix,(float)x,(float)y,(float)z).color(255,255,255,255).uv(u,v).endVertex();
    }
    private JogoDomainRender() {}
}
