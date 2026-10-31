package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 日车宽见 NPC：玩家模型 + 用户提供的皮肤。 */
public final class HigurumaNpcRenderer extends MobRenderer<HigurumaNpcEntity, PlayerModel<HigurumaNpcEntity>> {
    private static final ResourceLocation SKIN =
        new ResourceLocation("sukuna", "textures/entity/higuruma_npc.png");

    public HigurumaNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(HigurumaNpcEntity entity) { return SKIN; }
}
