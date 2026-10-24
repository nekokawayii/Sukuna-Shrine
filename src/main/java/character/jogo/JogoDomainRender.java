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
 * <p>**先保证几何正确、光影下可见**：用原版 position_color 着色器 + 纯色不透明球（自下而上由熔岩红过渡到火山岩黑），
 * 不依赖贴图 UV，避免"一边红一边黑/圆环"这类贴图采样崩坏。贴图版本留到几何确认正确之后再叠。</p>
 */
public final class JogoDomainRender {
    private static final int RINGS=64, SEGMENTS=64;
    private static RenderType shellType;

    private static RenderType shellType() {
        if(shellType==null){
            shellType=RenderType.create("sukuna_jogo_shell",
                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
                RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
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
        if(domains.isEmpty()||mc.level==null)return;
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
                // 颜色：底部（y 小）熔岩红，向上过渡到火山岩黑
                float f0=(float)(0.5-Math.cos(lat0)*0.5),f1=(float)(0.5-Math.cos(lat1)*0.5);
                int c0=color(f0),c1=color(f1);
                for(int j=0;j<SEGMENTS;j++){
                    double a0=2*Math.PI*j/SEGMENTS,a1=2*Math.PI*(j+1)/SEGMENTS;
                    vertex(vc,matrix,c.x+Math.cos(a0)*rr0,c.y+y0,c.z+Math.sin(a0)*rr0,c0);
                    vertex(vc,matrix,c.x+Math.cos(a1)*rr0,c.y+y0,c.z+Math.sin(a1)*rr0,c0);
                    vertex(vc,matrix,c.x+Math.cos(a1)*rr1,c.y+y1,c.z+Math.sin(a1)*rr1,c1);
                    vertex(vc,matrix,c.x+Math.cos(a0)*rr1,c.y+y1,c.z+Math.sin(a0)*rr1,c1);
                }
            }
        }
        source.endBatch(type);
        pose.popPose();
    }
    /** f=0 顶部（火山岩黑）→1 底部（熔岩红）。 */
    private static int color(float f) {
        float t=Math.max(0,Math.min(1,f));
        int r=(int)(38+150*t),g=(int)(24+52*t*t),b=(int)(22+26*t*t*t);
        return 0xFF000000|(r<<16)|(g<<8)|b;
    }
    private static void vertex(VertexConsumer vc,org.joml.Matrix4f matrix,double x,double y,double z,int color) {
        vc.vertex(matrix,(float)x,(float)y,(float)z).color(color).endVertex();
    }
    private JogoDomainRender() {}
}
