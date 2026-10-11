package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 五条悟：直接用玩家模型与指定皮肤渲染，不新增独立骨架。 */
public final class GojoRenderer extends MobRenderer<GojoEntity,PlayerModel<GojoEntity>> {
    private static final ResourceLocation SKIN=new ResourceLocation("sukuna","textures/entity/gojo.png");
    public GojoRenderer(EntityRendererProvider.Context context){
        super(context,new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),false),0.5f);
    }
    @Override public ResourceLocation getTextureLocation(GojoEntity entity){return SKIN;}
    @Override protected void scale(GojoEntity entity,PoseStack poses,float partial){poses.scale(1.0f,1.0f,1.0f);}
}
