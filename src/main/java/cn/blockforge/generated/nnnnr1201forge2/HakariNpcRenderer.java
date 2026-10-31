package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 秤金次 NPC：玩家模型（皮肤先用默认 Steve，用户给图后改这一行）。 */
public final class HakariNpcRenderer extends MobRenderer<HakariNpcEntity, PlayerModel<HakariNpcEntity>> {
    private static final ResourceLocation SKIN =
        new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public HakariNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(HakariNpcEntity entity) { return SKIN; }
}
