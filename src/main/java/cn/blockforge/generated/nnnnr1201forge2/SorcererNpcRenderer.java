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
            rightArm.yScale=rightSleeve.yScale=1;leftArm.yScale=leftSleeve.yScale=1;body.xScale=body.zScale=body.yScale=1;
            super.setupAnim(entity,swing,amount,age,yaw,pitch);
            if("mahito".equals(entity.characterId())){
                switch(entity.mahitoPose()){
                    case 1 -> {rightArm.xRot=-1.5f;rightArm.yScale=3.4f;head.zRot=.16f;}
                    case 2 -> {leftArm.xRot=-1.6f;rightArm.xRot=-1.5f;leftArm.yRot=-.6f;body.zRot=.2f;head.xRot=-.25f;}
                    case 3 -> {leftArm.xRot=-1.2f;rightArm.xRot=-1.6f;rightArm.yRot=-.4f;}
                    case 4 -> {body.xRot=.7f;rightArm.xRot=-.8f;leftArm.xRot=.4f;}
                    case 5 -> {body.yScale=.3f;body.xScale=1.6f;body.zScale=1.4f;leftArm.xRot=rightArm.xRot=1.2f;}
                    case 6 -> {leftArm.xRot=-1.4f;leftArm.yRot=-.8f;rightArm.xRot=-.8f;rightArm.yRot=.8f;}
                    case 7 -> {leftArm.xRot=rightArm.xRot=-1.5f;leftArm.yRot=-.6f;rightArm.yRot=.6f;head.xRot=-.35f;}
                    default -> {head.zRot=(float)Math.sin(age*.06f)*.06f;}
                }
                hat.copyFrom(head);jacket.copyFrom(body);leftSleeve.copyFrom(leftArm);rightSleeve.copyFrom(rightArm);return;
            }
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
    @Override protected void scale(SorcererNpcEntity entity,com.mojang.blaze3d.vertex.PoseStack pose,float partial){if("mahito".equals(entity.characterId())){float s=3f/1.8f;pose.scale(s,s,s);}}
    @Override protected net.minecraft.client.renderer.RenderType getRenderType(SorcererNpcEntity entity,boolean visible,boolean translucent,boolean glowing){
        return super.getRenderType(entity,entity.mahitoPose()==5?false:visible,entity.mahitoPose()==5||translucent,glowing);
    }
    @Override public ResourceLocation getTextureLocation(SorcererNpcEntity entity){
        return new ResourceLocation("sukuna","textures/entity/"+entity.characterId()+"_npc.png");
    }
}
