package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 石流龙 NPC：玩家模型 + 默认 Steve 皮肤。
 * <p>用户还没给皮肤，发图后把这里换成 {@code textures/entity/ryu_npc.png} 即可（一行）。
 */
public final class RyuNpcRenderer extends MobRenderer<RyuNpcEntity, PlayerModel<RyuNpcEntity>> {
    private static final ResourceLocation SKIN =
        new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public RyuNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(RyuNpcEntity entity) { return SKIN; }
}
