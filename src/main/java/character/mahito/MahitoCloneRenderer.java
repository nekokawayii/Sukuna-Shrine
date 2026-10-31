package character.mahito;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 分身 / 尾巴渲染：玩家模型（默认 Steve 皮肤）。 */
public final class MahitoCloneRenderer extends MobRenderer<MahitoCloneEntity, PlayerModel<MahitoCloneEntity>> {
    private static final ResourceLocation SKIN = new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public MahitoCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(MahitoCloneEntity entity) { return SKIN; }
}
