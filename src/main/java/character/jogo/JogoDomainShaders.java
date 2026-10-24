package character.jogo;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;

/** 盖棺铁围山的不透明球壳材质：POSITION_COLOR_TEX，uv.x=经度、uv.y=纬度。 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class JogoDomainShaders {
    public static ShaderInstance shell;
    @SubscribeEvent public static void register(RegisterShadersEvent event) {
        shell = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation("sukuna","jogo_domain"),DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> shell = shader);
        } catch (IOException | RuntimeException error) {
            LogUtils.getLogger().warn("盖棺铁围山球壳材质加载失败，退回纯色结界",error);
        }
    }
    private JogoDomainShaders() {}
}
