package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/** 标准宽臂皮肤骨架，保留外层衣服、头发和原版行走/挥手动作。 */
public final class SorcererNpcRenderer extends MobRenderer<SorcererNpcEntity,PlayerModel<SorcererNpcEntity>> {
    public SorcererNpcRenderer(EntityRendererProvider.Context context){
        super(context,new NpcModel(context.bakeLayer(ModelLayers.PLAYER)),.5f);
        addLayer(new ItemInHandLayer<>(this,context.getItemInHandRenderer()));
    }
    private static final class NpcModel extends PlayerModel<SorcererNpcEntity> {
        NpcModel(net.minecraft.client.model.geom.ModelPart root){super(root,false);}
        @Override public void setupAnim(SorcererNpcEntity entity,float swing,float amount,float age,float yaw,float pitch){
            super.setupAnim(entity,swing,amount,age,yaw,pitch);
            if(!"toji".equals(entity.characterId()))return;
            var data=entity.getPersistentData();int combo=data.getInt("TojiVisualCombo");if(combo<=0)return;
            double elapsed=entity.level().getGameTime()-data.getLong("TojiVisualAt")+(age-entity.tickCount);
            var frame=MotionFallbackClient.sampleDefinition("sukuna:toji_c"+combo,elapsed);if(frame==null)return;
            var pose=frame.pose();
            body.xRot=rad(pose.body());head.xRot+=rad(pose.head());
            leftArm.xRot=rad(pose.leftArm());rightArm.xRot=rad(pose.rightArm());
            leftArm.zRot=rad(-pose.spread());rightArm.zRot=rad(pose.spread());
            leftLeg.xRot=rad(pose.leftLeg());rightLeg.xRot=rad(pose.rightLeg());
            leftLeg.zRot=rad(-pose.legZ());rightLeg.zRot=rad(pose.legZ());
            hat.copyFrom(head);jacket.copyFrom(body);leftSleeve.copyFrom(leftArm);rightSleeve.copyFrom(rightArm);
            leftPants.copyFrom(leftLeg);rightPants.copyFrom(rightLeg);
        }
        private static float rad(double degrees){return (float)Math.toRadians(degrees);}
    }
    @Override public ResourceLocation getTextureLocation(SorcererNpcEntity entity){
        return new ResourceLocation("sukuna","textures/entity/"+entity.characterId()+"_npc.png");
    }
}
