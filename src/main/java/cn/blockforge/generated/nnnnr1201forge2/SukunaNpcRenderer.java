package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** 两面宿傩 NPC：玩家模型 + 用户提供的皮肤；原身形态换皮并多长两只手臂（r274）。 */
public final class SukunaNpcRenderer extends MobRenderer<SukunaNpcEntity, PlayerModel<SukunaNpcEntity>> {
    private static final ResourceLocation SKIN =
        new ResourceLocation("sukuna", "textures/entity/sukuna_npc.png");
    /** 原身形态皮肤。 */
    private static final ResourceLocation TRUE_FORM =
        new ResourceLocation("sukuna", "textures/entity/sukuna_true_form.png");

    public SukunaNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this,
            context.getItemInHandRenderer()));
        addLayer(new ExtraArmsLayer(this, context));
    }

    @Override public ResourceLocation getTextureLocation(SukunaNpcEntity entity) {
        return SukunaNpcTrueFormClient.has(entity.getId()) ? TRUE_FORM : SKIN;
    }

    /** 原身形态的"四只手"：在原有双臂外侧再画一对。 */
    private static final class ExtraArmsLayer extends RenderLayer<SukunaNpcEntity, PlayerModel<SukunaNpcEntity>> {
        private final EntityRendererProvider.Context context;
        private PlayerModel<SukunaNpcEntity> extra;

        ExtraArmsLayer(MobRenderer<SukunaNpcEntity, PlayerModel<SukunaNpcEntity>> parent,
                       EntityRendererProvider.Context context) {
            super(parent);
            this.context = context;
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buffer, int light, SukunaNpcEntity entity,
                           float limb, float amount, float partial, float age, float yaw, float pitch) {
            if (!SukunaNpcTrueFormClient.has(entity.getId())) return;
            if (extra == null) {
                try {
                    extra = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
                } catch (Exception error) {
                    return;
                }
            }
            extra.setupAnim(entity, limb, amount, age, yaw, pitch);
            var b = buffer.getBuffer(RenderType.entityCutoutNoCull(TRUE_FORM));
            // r282：直接用模型坐标摆位（1 像素 = 1/16 格）。玩家模型肩宽：手臂轴在 x=±5，
            // 身子半宽 4 → 放到 x=±10 就已经在肩膀外侧，不再靠 PoseStack 平移猜方向。
            for (int side = -1; side <= 1; side += 2) {
                var arm = side < 0 ? extra.leftArm : extra.rightArm;
                var sleeve = side < 0 ? extra.leftSleeve : extra.rightSleeve;
                arm.x = side * 10f;
                arm.y = 2f;
                arm.z = 1f;
                arm.zRot = side * 0.55f;
                arm.xRot = 1.1f;
                sleeve.x = arm.x;
                sleeve.y = arm.y;
                sleeve.z = arm.z;
                sleeve.zRot = arm.zRot;
                sleeve.xRot = arm.xRot;
                ps.pushPose();
                arm.render(ps, b, light, OverlayTexture.NO_OVERLAY);
                sleeve.render(ps, b, light, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }
        }
    }
}
