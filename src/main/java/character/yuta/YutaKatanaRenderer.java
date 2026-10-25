package character.yuta;

import cn.blockforge.generated.nnnnr1201forge2.BbModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 鬼妖村正的手持模型：运行时解析 {@code yuta_katana.bbmodel}（813 元素、8 张贴图）。
 * <p>物品的 baked model 会被这个自定义渲染器顶替（见 {@link YutaClient}）。
 * <p>模型按"刀长 1.5 格"缩放，并按手持/GUI 场景旋转到合适角度。
 */
public final class YutaKatanaRenderer extends BlockEntityWithoutLevelRenderer {
    private static final BbModel MODEL = new BbModel("yuta_katana", "yuta_katana");
    /** 目标刀长（格）。太长会穿模、太短不像武器。 */
    private static final float TARGET_LENGTH = 1.5f;

    public YutaKatanaRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ps,
            MultiBufferSource buffers, int light, int overlay) {
        float raw = Math.max(0.05f, MODEL.heightBlocks());
        float scale = TARGET_LENGTH / raw;

        ps.pushPose();
        ps.translate(0.5, 0.25, 0.5);
        ps.scale(scale, scale, scale);
        // 刀在模型里是竖着的：手持时倾斜，GUI 里斜放更像图标。
        ps.mulPose(Axis.ZP.rotationDegrees(-38f));
        if (context == ItemDisplayContext.GUI) {
            ps.mulPose(Axis.YP.rotationDegrees(35f));
            ps.mulPose(Axis.XP.rotationDegrees(20f));
        } else if (context == ItemDisplayContext.GROUND) {
            ps.mulPose(Axis.YP.rotationDegrees(90f));
            ps.mulPose(Axis.XP.rotationDegrees(90f));
        }
        MODEL.render(ps, buffers, light, "", 0f, 1f, 1f, 1f, 1f);
        ps.popPose();
    }
}
