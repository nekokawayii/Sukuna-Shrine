package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public final class VoidBossRenderer<T extends Mob> extends EntityRenderer<T> {
    private static final BbModel SHELL=new BbModel("entity/void_aggregate","void_aggregate"),CORE=new BbModel("void_core","void_core");
    public VoidBossRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=1.4f;}
    @Override public ResourceLocation getTextureLocation(T e){return new ResourceLocation("sukuna","textures/entity/void_aggregate_0.png");}
    @Override public boolean shouldRender(T e,net.minecraft.client.renderer.culling.Frustum f,double x,double y,double z){return f.isVisible(e.getBoundingBox().inflate(4));}
    @Override public void render(T e,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int light){ps.pushPose();ps.mulPose(Axis.YP.rotationDegrees(180-yaw));float time=(e.tickCount+partial)/20f;String animation="idle";
        if(e instanceof VoidBossEntity b){animation=b.action()==20||b.action()==15?"lunge":b.action()!=0?"attack":"idle";}
        if(e instanceof VoidNodeEntity n){if(n.kind()==0)ps.scale(0.3f,0.3f,0.3f);else{ps.scale(0.55f,0.55f,0.55f);ps.translate(0,-1.4,0);}}
        if(!(e instanceof VoidNodeEntity n)||n.kind()==0)SHELL.render(ps,buffers,Math.max(light,8388736),animation,time,1,1,1,1);
        if(e instanceof VoidNodeEntity)CORE.render(ps,buffers,15728880,"spin",time,1,1,1,1);ps.popPose();super.render(e,yaw,partial,ps,buffers,light);
    }
}
