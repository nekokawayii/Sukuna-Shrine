package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** 原版宽臂玩家骨架，64×64 UV 自动对应用户256×256高清皮肤。 */
public final class GuardianRenderer extends MobRenderer<GuardianEntity,GuardianRenderer.Model> {
    private static final ResourceLocation SKIN=new ResourceLocation("sukuna","textures/entity/yuji_guardian.png");
    public GuardianRenderer(EntityRendererProvider.Context context){super(context,new Model(),0.5f);}
    @Override public ResourceLocation getTextureLocation(GuardianEntity entity){return SKIN;}
    public static final class Model extends PlayerModel<GuardianEntity> {
        Model(){super(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,false),64,64).bakeRoot(),false);}
        @Override public void setupAnim(GuardianEntity e,float swing,float amount,float age,float yaw,float pitch){body.xRot=body.yRot=body.zRot=0;super.setupAnim(e,swing,amount,age,yaw,pitch);
            switch(e.action()){
                case 1,3,9 -> {rightArm.xRot=-1.25f+(float)Math.sin(age*2)*0.8f;leftArm.xRot=-1.25f-(float)Math.sin(age*2)*0.8f;body.xRot=0.12f;}
                case 4 -> {rightArm.xRot=-1.5f;rightArm.yRot=-0.2f;}
                case 5 -> {rightArm.xRot=leftArm.xRot=-0.45f;rightArm.zRot=0.25f;leftArm.zRot=-0.25f;}
                case 6 -> {rightArm.xRot=-1.35f;rightArm.yRot=-0.8f;leftArm.xRot=-0.7f;leftArm.yRot=0.5f;}
                case 11 -> {rightArm.xRot=leftArm.xRot=-1.05f;rightArm.yRot=-0.35f;leftArm.yRot=0.35f;}
                case 12 -> {rightArm.xRot=leftArm.xRot=-1.57f+head.xRot;rightArm.yRot=-0.23f;leftArm.yRot=0.23f;body.xRot=0.08f;}
                case 8 -> {rightArm.xRot=leftArm.xRot=-1.15f;rightArm.yRot=-0.65f;leftArm.yRot=0.65f;rightArm.zRot=0.2f;leftArm.zRot=-0.2f;}
                case 10 -> {rightArm.xRot=leftArm.xRot=-0.7f;rightArm.yRot=-0.5f;leftArm.yRot=0.5f;}
                default -> {}
            }
            rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);jacket.copyFrom(body);rightPants.copyFrom(rightLeg);leftPants.copyFrom(leftLeg);hat.copyFrom(head);hat.visible=false;
        }
    }
}
