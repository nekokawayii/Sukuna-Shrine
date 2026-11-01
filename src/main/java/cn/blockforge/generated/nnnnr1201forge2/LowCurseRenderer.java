package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** 按原始模型的实际包围盒落地、居中、等比缩放，不改写用户模型。 */
public final class LowCurseRenderer extends EntityRenderer<LowCurseEntity> {
    private static final java.util.Map<String,BbModel> models=new java.util.HashMap<>();
    public static void install(java.util.Map<String,BbModel> prepared){models.clear();models.putAll(prepared);}
    public LowCurseRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.8f;}
    private BbModel model(LowCurseEntity e){return models.computeIfAbsent(e.form().id(),id->new BbModel(id,id));}
    @Override public ResourceLocation getTextureLocation(LowCurseEntity e){return model(e).texture(0);}
    @Override public boolean shouldRender(LowCurseEntity e,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){return e.shouldRender(x,y,z)&&frustum.isVisible(e.getBoundingBox().inflate(9));}
    @Override public void render(LowCurseEntity e,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int light){
        var form=e.form();BbModel model=model(e);
        String animation=e.action()==LowCurseEntity.ATTACK?form.attack():e.getDeltaMovement().horizontalDistanceSqr()>.0003?form.walk():form.idle();
        if(animation.isEmpty())animation=form.idle();
        float seconds=(e.action()==LowCurseEntity.ATTACK?e.actionAge()+partial:e.tickCount+partial)/20f;
        float emerge=e.emerging()?Math.min(1,(e.actionAge()+partial)/20f):1;
        ps.pushPose();ps.mulPose(Axis.YP.rotationDegrees(180-yaw));ps.translate(0,-form.height()*(1-emerge),0);
        float scale=form.height()/form.originalHeight();ps.scale(scale,scale,scale);ps.translate(-form.centerX(),-form.floor(),-form.centerZ());
        model.render(ps,buffers,light,animation,seconds,1,1,1,Math.max(.15f,emerge));ps.popPose();super.render(e,yaw,partial,ps,buffers,light);
    }
}
