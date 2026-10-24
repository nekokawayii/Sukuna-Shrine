package character.jogo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/** 盖棺铁围山的主体：真正不透明、写深度的球壳（粒子做不到遮挡，这里必须用几何）。 */
public final class JogoDomainRender {
    private static final int RINGS=64, SEGMENTS=64;

    public static void render(RenderLevelStageEvent e) {
        var shader=JogoDomainShaders.shell;
        var domains=JogoFxClient.domains();
        if(shader==null||domains.isEmpty()||Minecraft.getInstance().level==null)return;
        Vec3 cam=e.getCamera().getPosition();
        // 与 vanilla 一样：用事件给的 poseStack 自己做 -cam 平移，把变换烘进顶点。
        // 之前手动在顶点里减 cam、又不经过 poseStack，会和这一帧的模型矩阵重复/缺失平移，表现就是球壳跟着玩家跑。
        var pose=e.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x,-cam.y,-cam.z);
        var matrix=pose.last().pose();
        float time=(Minecraft.getInstance().level.getGameTime()+e.getPartialTick())*0.05f;
        var buffer=Tesselator.getInstance().getBuilder();
        int previous0=RenderSystem.getShaderTexture(0),previous1=RenderSystem.getShaderTexture(1);
        RenderSystem.setShader(()->shader);
        if(JogoDomainShaders.textured){
            RenderSystem.setShaderTexture(0,JogoDomainShaders.WALL);
            RenderSystem.setShaderTexture(1,JogoDomainShaders.EMISSION);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        shader.safeGetUniform("DomainTime").set(time);
        shader.safeGetUniform("PrimaryColor").set(0.62f,0.10f,0.045f);   // 暗红火山色
        buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR_TEX);
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
                    vertex(buffer,matrix,c.x+Math.cos(a0)*rr0,c.y+y0,c.z+Math.sin(a0)*rr0,u0,v0);
                    vertex(buffer,matrix,c.x+Math.cos(a1)*rr0,c.y+y0,c.z+Math.sin(a1)*rr0,u1,v0);
                    vertex(buffer,matrix,c.x+Math.cos(a1)*rr1,c.y+y1,c.z+Math.sin(a1)*rr1,u1,v1);
                    vertex(buffer,matrix,c.x+Math.cos(a0)*rr1,c.y+y1,c.z+Math.sin(a0)*rr1,u0,v1);
                }
            }
        }
        pose.popPose();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.setShaderTexture(0,previous0);
        RenderSystem.setShaderTexture(1,previous1);
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
    }
    private static void vertex(com.mojang.blaze3d.vertex.BufferBuilder buffer,org.joml.Matrix4f matrix,double x,double y,double z,float u,float v) {
        buffer.vertex(matrix,(float)x,(float)y,(float)z).color(255,255,255,255).uv(u,v).endVertex();
    }
    private JogoDomainRender() {}
}
