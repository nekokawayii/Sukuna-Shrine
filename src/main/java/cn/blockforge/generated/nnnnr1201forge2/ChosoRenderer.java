package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 胀相：玩家模型 + 指定皮肤。 */
public final class ChosoRenderer extends MobRenderer<ChosoEntity,PlayerModel<ChosoEntity>> {
    private static final ResourceLocation SKIN=new ResourceLocation("sukuna","textures/entity/choso.png");
    public ChosoRenderer(EntityRendererProvider.Context context){
        super(context,new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER),false),0.5f);
    }
    @Override public ResourceLocation getTextureLocation(ChosoEntity entity){return SKIN;}
}
