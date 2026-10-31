package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import character.higuruma.HigurumaJudgeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 审判者渲染：读 BbModel models/higuruma_judge.bbmodel。 */
public final class HigurumaJudgeRenderer extends EntityRenderer<HigurumaJudgeEntity> {
    private static final BbModel MODEL = new BbModel("higuruma_judge", "higuruma_judge");

    public HigurumaJudgeRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 1f; }

    @Override public ResourceLocation getTextureLocation(HigurumaJudgeEntity entity) {
        return new ResourceLocation("sukuna", "textures/entity/higuruma_judge_0.png");
    }

    @Override public void render(HigurumaJudgeEntity entity, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        MODEL.render(ps, buffers, light, "", (entity.tickCount + partial) / 20f, 1f, 1f, 1f, 1f);
        ps.popPose();
        super.render(entity, yaw, partial, ps, buffers, light);
    }
}
