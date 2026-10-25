package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 漏壶（NPC）：使用从 model.bbmodel 转换出的独立模型。 */
public final class JogoNpcRenderer extends MobRenderer<JogoNpcEntity, JogoNpcModel> {
    private static final ResourceLocation SKIN = new ResourceLocation("sukuna", "textures/entity/jogo_npc.png");
    public JogoNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new JogoNpcModel(context.bakeLayer(JogoNpcModel.LAYER)), 0.6f);
    }
    @Override public ResourceLocation getTextureLocation(JogoNpcEntity entity) { return SKIN; }
}
