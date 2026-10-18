package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** 观测者用**本地玩家自己的皮肤**渲染——你在白色空间里面对的，是你自己的身体。 */
public final class ObserverRenderer extends MobRenderer<ObserverEntity,ObserverRenderer.Model> {
    private static final ResourceLocation DEFAULT_SKIN=new ResourceLocation("textures/entity/steve.png");
    public ObserverRenderer(EntityRendererProvider.Context context){super(context,new Model(),0.5f);}
    @Override public ResourceLocation getTextureLocation(ObserverEntity entity){
        var player=Minecraft.getInstance().player;
        if(player!=null){var skin=player.getSkinTextureLocation();if(skin!=null)return skin;}
        return DEFAULT_SKIN;
    }
    public static final class Model extends PlayerModel<ObserverEntity> {
        Model(){super(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,false),64,64).bakeRoot(),false);}
        @Override public void setupAnim(ObserverEntity e,float swing,float amount,float age,float yaw,float pitch){
            body.xRot=body.yRot=body.zRot=0;
            super.setupAnim(e,swing,amount,age,yaw,pitch);
            switch(e.action()){
                case 1 -> {  // 挥刀：右臂抬起横扫，身体前压
                    rightArm.xRot=-1.45f;rightArm.yRot=-0.55f;rightArm.zRot=0.35f;
                    leftArm.xRot=-0.35f;leftArm.yRot=0.25f;body.xRot=0.18f;
                }
                case 2 -> {  // 抬头凝视
                    head.xRot=Math.min(head.xRot,-0.35f);body.xRot=-0.06f;
                }
                default -> {}
            }
            rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);jacket.copyFrom(body);rightPants.copyFrom(rightLeg);leftPants.copyFrom(leftLeg);hat.copyFrom(head);hat.visible=false;
        }
    }
}
