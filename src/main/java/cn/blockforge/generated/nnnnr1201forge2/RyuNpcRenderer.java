package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 石流龙 NPC：玩家模型 + 用户提供的皮肤（256×256 原图原样）。 */
public final class RyuNpcRenderer extends MobRenderer<RyuNpcEntity, PlayerModel<RyuNpcEntity>> {
    private static final ResourceLocation SKIN =
        new ResourceLocation("sukuna", "textures/entity/ryu_npc.png");

    public RyuNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(RyuNpcEntity entity) { return SKIN; }
}
