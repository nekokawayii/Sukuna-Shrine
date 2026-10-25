package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 里香渲染器：运行时解析 {@code models/rika.bbmodel}，贴图 {@code textures/entity/rika_0.png}。 */
public final class RikaRenderer extends EntityRenderer<RikaEntity> {
    private static final BbModel MODEL = new BbModel("rika", "rika");
    /** 里香设定为 2.2 格高。 */
    private static final float TARGET_HEIGHT = 2.2f;

    public RikaRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.4f;
    }

    @Override public ResourceLocation getTextureLocation(RikaEntity entity) { return MODEL.texture(0); }

    @Override public boolean shouldRender(RikaEntity e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(6));
    }

    @Override public void render(RikaEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial;
        String animation;
        float seconds;
        if (e.action() == RikaEntity.BITE) { animation = "bite"; seconds = (e.actionAge() + partial) / 20f; }
        else if (e.action() == RikaEntity.PUNCH) {
            boolean second = e.actionAge() > 10;
            animation = second ? "punch_2" : "punch";
            seconds = (e.actionAge() + partial) / 20f;
        } else if (e.getDeltaMovement().horizontalDistanceSqr() > 0.0016) {
            animation = "fast_flight";
            seconds = age / 20f;
        } else {
            animation = "floating_walk";
            seconds = age / 20f;
        }
        float rawHeight = Math.max(0.05f, MODEL.heightBlocks());
        float scale = TARGET_HEIGHT / rawHeight;
        float alpha = Math.min(1f, e.lifetime() / 20f);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        ps.scale(scale, scale, scale);
        ps.translate(0, 0.35, 0);
        MODEL.render(ps, buffers, light, animation, seconds, 1, 1, 1, Math.max(0.15f, alpha));
        ps.popPose();
        super.render(e, yaw, partial, ps, buffers, light);
    }
}
