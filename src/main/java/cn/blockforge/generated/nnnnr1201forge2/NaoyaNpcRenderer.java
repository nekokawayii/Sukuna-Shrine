package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 禅院直哉 NPC：保留玩家几何体，使用独立绘制的金发白袍皮肤。 */
public final class NaoyaNpcRenderer extends MobRenderer<NaoyaNpcEntity, PlayerModel<NaoyaNpcEntity>> {
    private static final ResourceLocation SKIN =
        new ResourceLocation("jujutsu_kaisen_cursed_paths", "textures/entity/naoya_npc.png");

    public NaoyaNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(NaoyaNpcEntity entity) { return SKIN; }
}
