package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** 使用64×32旧版玩家UV，直接读取用户256×128皮肤，不缩图。 */
public final class YujiRenderer extends MobRenderer<YujiEntity,YujiRenderer.Model> {
    private static final ResourceLocation SKIN=new ResourceLocation("sukuna","textures/entity/yuji.png");
    public YujiRenderer(EntityRendererProvider.Context context){super(context,new Model(),0.4f);}
    @Override public ResourceLocation getTextureLocation(YujiEntity entity){return SKIN;}
    @Override protected RenderType getRenderType(YujiEntity entity,boolean visible,boolean translucent,boolean glowing){return RenderType.entityTranslucent(SKIN);}
    @Override public void render(YujiEntity entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
        // 真身略深，幻影略浅；拖尾只在百裂或突进时绘制，不改变碰撞。
        model.alpha=entity.decoy()?0.42f:entity.action()==4?0.64f:1;
        super.render(entity,yaw,partial,poses,buffers,light);
        if(entity.action()==4||entity.action()==2&&entity.actionAge()>20){for(int i=1;i<=3;i++){poses.pushPose();var delta=entity.getDeltaMovement();poses.translate(-delta.x*i*0.8,0,-delta.z*i*0.8);model.alpha=(entity.decoy()?0.10f:0.14f)/i;super.render(entity,yaw,partial,poses,buffers,light);poses.popPose();}}
        model.alpha=1;
    }
    public static final class Model extends HumanoidModel<YujiEntity> {
        float alpha=1;
        Model(){super(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32).bakeRoot());}
        @Override public void setupAnim(YujiEntity entity,float swing,float amount,float age,float yaw,float pitch){
            super.setupAnim(entity,swing,amount,age,yaw,pitch);float t=entity.actionAge();
            body.xRot=0;body.y=0;rightArm.zRot=0;leftArm.zRot=0;
            switch(entity.action()){
                case 1 -> {rightArm.xRot=t<8?-0.5f:-1.6f;leftArm.xRot=-0.5f;}
                case 2 -> {body.xRot=t<=20?0.15f:0.35f;rightArm.xRot=t<=20?0.9f:-1.6f;leftArm.xRot=-0.6f;rightArm.yRot=t<=20?-0.4f:0;}
                case 3,6 -> {rightArm.xRot=leftArm.xRot=-1.15f;rightArm.yRot=-0.65f;leftArm.yRot=0.65f;rightArm.zRot=0.2f;leftArm.zRot=-0.2f;}
                case 4 -> {rightArm.xRot=-1.3f+(float)Math.sin(age*1.8)*0.6f;leftArm.xRot=-1.3f-(float)Math.sin(age*1.8)*0.6f;}
                case 5 -> {if(t<12){body.xRot=0.3f;body.y=1.5f;rightArm.xRot=leftArm.xRot=0.7f;rightLeg.xRot=leftLeg.xRot=-0.3f;}else{rightArm.xRot=-1.2f+(float)Math.sin(age*Math.PI/3)*0.8f;leftArm.xRot=-1.2f-(float)Math.sin(age*Math.PI/3)*0.8f;}}
                case 7 -> {rightArm.xRot=t<10?-1.35f:-0.85f;rightArm.yRot=t<10?-0.5f:0.65f;leftArm.xRot=-0.5f;}
                default -> {}
            }
            hat.copyFrom(head);
        }
        @Override public void renderToBuffer(PoseStack poses,VertexConsumer buffer,int light,int overlay,float red,float green,float blue,float opacity){super.renderToBuffer(poses,buffer,light,overlay,red,green,blue,opacity*alpha);}
    }
}
