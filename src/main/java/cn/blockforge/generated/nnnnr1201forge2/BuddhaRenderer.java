package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public final class BuddhaRenderer<T extends Mob> extends EntityRenderer<T> {
    public static final BbModel BUDDHA=new BbModel("buddha","buddha");
    private static final BbModel PARASITE=new BbModel("parasite","parasite"),MOTHER=new BbModel("mother","parasite");
    public BuddhaRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0.6f;}
    @Override public boolean shouldRender(T entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){return frustum.isVisible(entity.getBoundingBox().inflate(entity instanceof BuddhaEntity?6:1));}
    @Override public ResourceLocation getTextureLocation(T entity){return new ResourceLocation("sukuna","textures/entity/"+(entity instanceof BuddhaEntity?"buddha":"parasite")+"_0.png");}
    @Override public void render(T entity,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int light){ps.pushPose();ps.mulPose(Axis.YP.rotationDegrees(180-yaw));float age=entity.tickCount+partial;
        if(entity instanceof BuddhaEntity b){BUDDHA.action(b.action());ps.scale(0.56f,0.56f,0.56f);ps.translate(0,0.01+Math.sin(age*0.05)*0.025,0);boolean red=b.phase()==2;String animation=age<148?"descend_and_stand":"cursed_idle";float seconds=age<148?age/20:(age-148)/20;if(b.action()==1||b.action()==9||b.action()==10){animation="descend_and_stand";seconds=0;}BUDDHA.render(ps,buffers,light,animation,seconds,1,red?0.52f:0.94f,red?0.65f:0.8f,1);if(b.action()==7||b.action()==11){ps.scale(1.018f,1.018f,1.018f);BUDDHA.render(ps,buffers,15728880,animation,seconds,1,0.65f,0.2f,0.2f);}if(b.action()==8){ps.translate(0,0.5,2);ps.scale(1.25f,1.25f,1.25f);BUDDHA.render(ps,buffers,15728880,"cursed_idle",age/20,0.9f,0.08f,0.2f,0.32f);}}
        else if(entity instanceof ParasiteEntity p){boolean mother=p.mother();ps.translate(0,Math.abs(Math.sin(age*0.3))*(mother?0.03:0.045),0);ps.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(age*0.3)*(mother?1:4)));(mother?MOTHER:PARASITE).render(ps,buffers,light,"idle",age/20,1,1,1,1);}
        ps.popPose();super.render(entity,yaw,partial,ps,buffers,light);
    }
}
