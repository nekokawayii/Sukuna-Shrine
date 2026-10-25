package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 漏壶（NPC）：玩家模型 + bbmodel 内嵌贴图。 */
public final class JogoNpcRenderer extends MobRenderer<JogoNpcEntity, PlayerModel<JogoNpcEntity>> {
    private static final ResourceLocation SKIN = new ResourceLocation("sukuna", "textures/entity/jogo_npc.png");
    public JogoNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.6f);
    }
    @Override public ResourceLocation getTextureLocation(JogoNpcEntity entity) { return SKIN; }
}
