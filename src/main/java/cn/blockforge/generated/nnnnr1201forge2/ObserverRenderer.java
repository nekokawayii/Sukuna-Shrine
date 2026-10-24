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
        Model(){super(MotionPlayerGeometry.player(false),false);}
        @Override public void setupAnim(ObserverEntity e,float swing,float amount,float age,float yaw,float pitch){
            body.xRot=body.yRot=body.zRot=0;
            leftArm.yRot=rightArm.yRot=leftArm.zRot=rightArm.zRot=0;
            super.setupAnim(e,swing,amount,age,yaw,pitch);
            switch(e.action()){
                case 1 -> {  // 挥刀：右臂抬起横扫，身体前压
                    float cut=(float)Math.sin(age*.65f);
                    rightArm.xRot=-1.45f+cut*.9f;rightArm.yRot=-0.55f+cut*.6f;rightArm.zRot=0.35f;
                    leftArm.xRot=-0.35f;leftArm.yRot=0.25f;body.xRot=0.18f;
                }
                case 2 -> {  // 抬头凝视
                    head.xRot=Math.min(head.xRot,-0.35f);body.xRot=-0.06f;
                }
                case 3 -> { // 抱头痛苦，胸口与双手持续颤抖
                    head.xRot=.5f;body.xRot=.2f;body.zRot=(float)Math.sin(age*1.8f)*.05f;
                    leftArm.xRot=rightArm.xRot=-2.8f;leftArm.zRot=-.35f;rightArm.zRot=.35f;
                    leftLeg.xRot=-.3f;rightLeg.xRot=.3f;
                }
                case 4 -> {leftArm.xRot=rightArm.xRot=-1.5f;leftArm.yRot=.65f;rightArm.yRot=-.65f;head.xRot=-.4f;}
                case 5 -> {rightArm.xRot=-1.6f+(float)Math.sin(age*1.4f)*1.1f;leftArm.xRot=-.7f;body.zRot=(float)Math.sin(age)*.18f;}
                case 6 -> {body.xRot=-.35f;head.xRot=-.6f;leftArm.zRot=-.8f;rightArm.zRot=.8f;}
                default -> {}
            }
            MotionPlayerGeometry.knees(this,0,0);
            var battle=ObserverFight.state();
            if(battle!=null&&battle.boss()==e.getId()&&battle.phase()==1&&e.action()!=6){
                String move=battle.move();int kind=move.contains("后翻")?1:move.contains("飞踢")?2:move.contains("旋转")?3:move.contains("滑铲")?6:move.contains("冲拳")?5:0;
                if(kind!=0){
                    var pose=DismantlePose.sample(kind,battle.age()%30);
                    leftArm.xRot=(float)Math.toRadians(pose.leftArm());rightArm.xRot=(float)Math.toRadians(pose.rightArm());
                    leftLeg.xRot=(float)Math.toRadians(pose.leftLeg());rightLeg.xRot=(float)Math.toRadians(pose.rightLeg());
                    leftArm.zRot=(float)-Math.toRadians(pose.spread());rightArm.zRot=(float)Math.toRadians(pose.spread());
                    body.xRot=(float)Math.toRadians(pose.body());MotionPlayerGeometry.knees(this,(float)Math.toRadians(pose.leftKnee()),(float)Math.toRadians(pose.rightKnee()));
                }else if(move.contains("炎")||move.contains("灶")){leftArm.xRot=-1.6f;rightArm.xRot=-1.4f;rightArm.yRot=-1.2f;leftArm.yRot=.2f;}
                else if(move.contains("苍")||move.contains("赫")||move.contains("茈")||move.contains("穿血")){rightArm.xRot=-1.6f;leftArm.xRot=-1.3f;leftArm.yRot=.6f;}
            }
            rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);jacket.copyFrom(body);rightPants.copyFrom(rightLeg);leftPants.copyFrom(leftLeg);hat.copyFrom(head);hat.visible=false;
        }
    }
}
