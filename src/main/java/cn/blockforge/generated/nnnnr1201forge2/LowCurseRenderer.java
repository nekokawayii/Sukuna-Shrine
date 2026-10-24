package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 低级咒灵渲染器：运行时解析 {@code models/basement_wanderer.bbmodel}，
 * 贴图按 {@link BbModel} 约定取 {@code textures/entity/basement_wanderer_<i>.png}（模型内嵌了 9 张）。
 * <p>模型按「正常大小」缩放到 2 格高。
 */
public final class LowCurseRenderer extends EntityRenderer<LowCurseEntity> {
    private static final BbModel MODEL = new BbModel("basement_wanderer", "basement_wanderer");
    /** 目标身高（格）：夏油杰体型的一半左右。 */
    private static final float TARGET_HEIGHT = 2.0f;
    private static final String ANIM_IDLE = "扭曲行走", ANIM_RUN = "冲刺", ANIM_ATTACK = "攻击";

    public LowCurseRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.7f;
    }

    @Override public ResourceLocation getTextureLocation(LowCurseEntity entity) { return MODEL.texture(0); }

    @Override public boolean shouldRender(LowCurseEntity e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(4));
    }

    @Override public void render(LowCurseEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        float age = e.actionAge() + partial;
        float seconds = (e.tickCount + partial) / 20f;
        String animation = e.action() == LowCurseEntity.ATTACK ? ANIM_ATTACK
            : e.getDeltaMovement().horizontalDistanceSqr() > 0.0016 ? ANIM_RUN : ANIM_IDLE;
        if (e.action() == LowCurseEntity.ATTACK) seconds = age / 20f;

        float rawHeight = Math.max(0.05f, MODEL.heightBlocks());
        float scale = TARGET_HEIGHT / rawHeight;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        ps.scale(scale, scale, scale);
        float alpha = 1f;
        if (e.emerging()) {
            // 没有「爬出」片段：用下沉 + 渐显模拟破土而出的过程。
            float t = Math.min(1f, age / 20f);
            ps.translate(0, -rawHeight * (1 - t) * 0.9, 0);
            alpha = Math.max(0.05f, t);
        }
        MODEL.render(ps, buffers, light, animation, seconds, 1, 1, 1, alpha);
        ps.popPose();
        super.render(e, yaw, partial, ps, buffers, light);
    }
}
