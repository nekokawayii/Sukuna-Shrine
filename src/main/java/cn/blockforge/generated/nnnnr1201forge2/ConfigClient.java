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
        // 此时处于本模组的加载上下文；不要挪入延迟执行的 enqueueWork。
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new SukunaConfigScreen(parent)));
    }

    private ConfigClient() {}
}
