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
        // 客户端没有服务端仇恨目标；用同步的位移/挥手状态，避免永远待机或原地狂奔。
        double speed=entity.getDeltaMovement().horizontalDistanceSqr();
        String animation=entity.swinging?"连招":speed>.012?"跑步":speed>.0001?"走路":"";
        float seconds=entity.swinging?entity.getAttackAnim(partial)*2.25f:(entity.tickCount+partial)/20f;
        MODEL.render(ps, buffers, light, animation, seconds, 1f, 1f, 1f, 1f);
        ps.popPose();
        super.render(entity, yaw, partial, ps, buffers, light);
    }
}
