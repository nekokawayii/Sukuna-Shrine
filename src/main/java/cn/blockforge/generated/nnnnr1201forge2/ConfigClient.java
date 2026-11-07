package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** 只在客户端注册，专用服务器不加载界面类。 */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigClient {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        // 构造期已经挂过；这里只做兜底（重复注册同一扩展点会覆盖，无副作用）。
        registerScreen();
    }

    /** 由 GeneratedMod 在构造期调用（Forge 的配置界面扩展点必须在构造期注册，否则模组列表不显示 Config 按钮）。 */
    public static void registerScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new SukunaConfigScreen(parent)));
    }

    private ConfigClient() {}
}
