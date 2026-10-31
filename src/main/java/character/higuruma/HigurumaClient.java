package character.higuruma;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** 处刑人之剑：用 bbmodel 做手持 3D 模型（BlockEntityWithoutLevelRenderer）。 */
public final class HigurumaClient {
    private static final cn.blockforge.generated.nnnnr1201forge2.BbModel MODEL =
        new cn.blockforge.generated.nnnnr1201forge2.BbModel("executioner_sword", "executioner_sword");

    public static void item(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    var mc = Minecraft.getInstance();
                    renderer = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                        @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ps,
                                                           MultiBufferSource buffers, int light, int overlay) {
                            ps.pushPose();
                            ps.translate(0.5, 0.45, 0.5);
                            float scale = context == ItemDisplayContext.GUI ? 0.55f : 0.7f;
                            ps.scale(scale, scale, scale);
                            ps.mulPose(Axis.YP.rotationDegrees(45f));
                            ps.mulPose(Axis.XP.rotationDegrees(-35f));
                            MODEL.render(ps, buffers, LightTexture.FULL_BRIGHT, "", 0f, 1f, 1f, 1f, 1f);
                            ps.popPose();
                        }
                    };
                }
                return renderer;
            }
        });
    }

    private HigurumaClient() {}
}
