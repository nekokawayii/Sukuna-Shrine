package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 甚尔 QTE 的 HUD 注册。
 *
 * <p>⚠️ `RegisterGuiOverlaysEvent` 是**模组总线（MOD bus）**事件，必须单独用
 * `bus = Mod.EventBusSubscriber.Bus.MOD` 的订阅者注册；挂在 Forge 总线上永远不会触发
 * （r350 提示面板不显示的根因就是这个——红光和提示一起哑掉）。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TojiOverlay {
    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("sukuna_toji_qte",
            (gui, graphics, partialTick, width, height) -> TojiQteVisuals.drawOverlay(graphics, width, height));
    }

    private TojiOverlay() {}
}
