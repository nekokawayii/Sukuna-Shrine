package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 两面宿傩 NPC：玩家模型 + 用户提供的皮肤（1024² 原图）。 */
public final class SukunaNpcRenderer extends MobRenderer<SukunaNpcEntity, PlayerModel<SukunaNpcEntity>> {
    private static final ResourceLocation SKIN =
        new ResourceLocation("sukuna", "textures/entity/sukuna_npc.png");

    public SukunaNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(SukunaNpcEntity entity) { return SKIN; }
}
