package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 鹿紫云一 NPC：玩家模型 + 用户提供的皮肤。 */
public final class KashimoNpcRenderer extends MobRenderer<KashimoNpcEntity, PlayerModel<KashimoNpcEntity>> {
    private static final ResourceLocation SKIN =
        new ResourceLocation("sukuna", "textures/entity/kashimo_npc.png");

    public KashimoNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(KashimoNpcEntity entity) { return SKIN; }
}
