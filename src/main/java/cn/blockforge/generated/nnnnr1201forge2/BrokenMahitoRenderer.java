package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 使用提供的真人续作皮肤渲染破碎真人。 */
public final class BrokenMahitoRenderer extends MobRenderer<BrokenMahitoEntity,BrokenMahitoRenderer.Model> {
    private static final ResourceLocation SKIN=new ResourceLocation("sukuna","textures/entity/broken_mahito.png");
    public BrokenMahitoRenderer(EntityRendererProvider.Context context){super(context,new Model(),0.4f);}
    @Override public ResourceLocation getTextureLocation(BrokenMahitoEntity entity){return SKIN;}
    @Override protected RenderType getRenderType(BrokenMahitoEntity entity,boolean visible,boolean translucent,boolean glowing){return RenderType.entityCutoutNoCull(SKIN);}
    public static final class Model extends PlayerModel<BrokenMahitoEntity>{
        Model(){super(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,false),64,64).bakeRoot(),false);}
        @Override public void setupAnim(BrokenMahitoEntity e,float limbSwing,float limbAmount,float age,float yaw,float pitch){super.setupAnim(e,limbSwing,limbAmount,age,yaw,pitch);if(e.action()==1){rightArm.xRot=-1.7f;leftArm.xRot=-1.2f;}if(e.action()==2){rightArm.xRot=-1.25f;leftArm.xRot=-0.8f;}rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);}
    }
}
