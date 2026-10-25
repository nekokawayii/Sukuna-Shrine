package character.yuta;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** 客户端：把鬼妖村正换成 BEWLR 自定义渲染（813 元素武士刀模型）。 */
@OnlyIn(Dist.CLIENT)
public final class YutaClient {
    private static BlockEntityWithoutLevelRenderer renderer;

    public static IClientItemExtensions katanaExtensions() {
        return new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new YutaKatanaRenderer(
                        Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                        Minecraft.getInstance().getEntityModels());
                }
                return renderer;
            }
        };
    }

    private YutaClient() {}
}
