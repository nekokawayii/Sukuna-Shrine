package character.mahito;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * 「改变」后的体型视觉缩放。
 * 服务端在改完形态后广播 Effect(303)（owner=实体 id，size=宽比例，life=高比例×1000），
 * 客户端登记到 map，渲染时按比例缩放（不改碰撞箱）。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class TransformedScaleClient {
    private record Scale(float w, float h, long at) {}
    private static final Map<Integer, Scale> SCALES = new HashMap<>();

    public static void put(int entityId, float w, float h) {
        SCALES.put(entityId, new Scale(w, h, System.currentTimeMillis()));
    }

    public static void clear() { SCALES.clear(); }

    @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public static void pre(RenderLivingEvent.Pre event) {
        Scale scale = SCALES.get(event.getEntity().getId());
        if (scale == null) return;
        // 60 秒未刷新则失效（防止残留）
        if (System.currentTimeMillis() - scale.at() > 60_000L) { SCALES.remove(event.getEntity().getId()); return; }
        event.getPoseStack().scale(scale.w(), scale.h(), scale.w());
    }

    private TransformedScaleClient() {}
}
