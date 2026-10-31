package character.mahito;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「改变」后的体型视觉：给带 {@code SukunaScaleW/H} 标记的生物按比例缩放渲染。
 * 只影响画面，不改碰撞箱（1.20.1 通用实体尺寸不支持运行时改动）。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class TransformedScaleClient {
    public static final String SCALE_W = "SukunaScaleW";
    public static final String SCALE_H = "SukunaScaleH";

    @SubscribeEvent
    public static void pre(RenderLivingEvent.Pre<?, ?> event) {
        var data = event.getEntity().getPersistentData();
        if (!data.contains(SCALE_W) && !data.contains(SCALE_H)) return;
        float w = data.contains(SCALE_W) ? data.getFloat(SCALE_W) : 1f;
        float h = data.contains(SCALE_H) ? data.getFloat(SCALE_H) : 1f;
        event.getPoseStack().scale(w, h, w);
    }

    private TransformedScaleClient() {}
}
