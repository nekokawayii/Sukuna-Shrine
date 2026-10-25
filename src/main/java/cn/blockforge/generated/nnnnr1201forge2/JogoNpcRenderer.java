package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 漏壶（NPC）：直接读取 Blockbench 模型 models/jogo_npc.bbmodel（与魔虚罗同一套 BbModel 管线）。 */
public final class JogoNpcRenderer extends EntityRenderer<JogoNpcEntity> {
    private static final BbModel MODEL = new BbModel("jogo_npc", "jogo_npc");

    public JogoNpcRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.8f;
    }

    @Override public ResourceLocation getTextureLocation(JogoNpcEntity entity) {
        return new ResourceLocation("sukuna", "textures/entity/jogo_npc_0.png");
    }

    @Override public void render(JogoNpcEntity entity, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        // 有目标时播放攻击/移动动画，否则待机
        String animation = entity.getTarget() != null ? "跑步" : "";
        MODEL.render(ps, buffers, light, animation, (entity.tickCount + partial) / 20f, 1f, 1f, 1f, 1f);
        ps.popPose();
        super.render(entity, yaw, partial, ps, buffers, light);
    }
}
