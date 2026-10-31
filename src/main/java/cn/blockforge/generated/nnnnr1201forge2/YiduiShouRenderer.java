package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 「圆钝果实」渲染：直接读 Blockbench 模型 models/yiduishou.bbmodel（一堆手）。 */
public final class YiduiShouRenderer extends EntityRenderer<YiduiShouEntity> {
    private static final BbModel MODEL = new BbModel("yiduishou", "yiduishou");
    /** 用户指定：视觉放大到玩家的 5 倍。 */
    public static final float SCALE = 3f;

    public YiduiShouRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 1.0f;
    }

    @Override public ResourceLocation getTextureLocation(YiduiShouEntity entity) {
        return new ResourceLocation("sukuna", "textures/entity/yiduishou_0.png");
    }

    @Override public void render(YiduiShouEntity entity, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.scale(SCALE, SCALE, SCALE);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));   // 反过来：背朝玩家
        MODEL.render(ps, buffers, light, "", (entity.tickCount + partial) / 20f, 1f, 1f, 1f, 1f);
        ps.popPose();
        super.render(entity, yaw, partial, ps, buffers, light);
    }
}
