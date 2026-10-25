package character.yuta;

import cn.blockforge.generated.nnnnr1201forge2.BbModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 坟头插刀的渲染：运行时解析 {@code oni_muramasa_sheathed.bbmodel}（241 元素）。
 * <p>刀被拔走后不再绘制。模型按 1.2 格高缩放，居中到方块中心。
 */
public final class YutaGraveRenderer implements BlockEntityRenderer<YutaGraveBlockEntity> {
    private static final BbModel MODEL = new BbModel("oni_muramasa_sheathed", "oni_muramasa_sheathed");
    /** 目标高度（格）：一把插在地上的刀，正常武器大小约 0.9 格。 */
    private static final float TARGET_HEIGHT = 0.9f;

    public YutaGraveRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(YutaGraveBlockEntity be, float partial, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (be.taken()) return;
        float raw = Math.max(0.05f, MODEL.heightBlocks());
        // 只缩小、不放大：模型本身若已比目标矮，保持原尺寸，避免被放大成巨型刀。
        float scale = Math.min(1.0f, TARGET_HEIGHT / raw);
        ps.pushPose();
        ps.translate(0.5, 0.02, 0.5);
        ps.scale(scale, scale, scale);
        MODEL.render(ps, buffers, light, "", 0f, 1f, 1f, 1f, 1f);
        ps.popPose();
    }
}
