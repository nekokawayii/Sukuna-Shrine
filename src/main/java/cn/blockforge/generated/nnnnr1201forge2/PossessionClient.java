package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 被宿傩夺舍期间：强制第三人称（r326，事件号 798）。 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class PossessionClient {
    private static long until;
    private static boolean wasForced;

    public static void receive(Network.Effect e) {
        until = Math.max(until, System.currentTimeMillis() + e.life() * 50L);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;
        boolean active = System.currentTimeMillis() < until;
        if (active) {
            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            wasForced = true;
        } else if (wasForced) {
            wasForced = false;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    private PossessionClient() { }
}
