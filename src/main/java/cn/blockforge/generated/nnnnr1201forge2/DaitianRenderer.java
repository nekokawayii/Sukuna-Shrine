package cn.blockforge.generated.nnnnr1201forge2;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class DaitianRenderer extends EntityRenderer<DaitianEntity>{
    private final BbModel model=new BbModel("daitian","daitian");
    public DaitianRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0.65f;}
    @Override public ResourceLocation getTextureLocation(DaitianEntity entity){return new ResourceLocation("sukuna","textures/entity/daitian_0.png");}
    @Override public void render(DaitianEntity e,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int light){ps.pushPose();ps.mulPose(Axis.YP.rotationDegrees(180-yaw));ps.translate(0,0.035,0);model.action(e.action());model.render(ps,buffers,light,"battle_idle",(e.tickCount+partial)/20f,1,1,1,1);ps.popPose();super.render(e,yaw,partial,ps,buffers,light);}
}
