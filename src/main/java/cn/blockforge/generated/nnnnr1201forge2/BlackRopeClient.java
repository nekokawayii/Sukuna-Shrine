package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 黑绳：左键甩出（原版攻击键），命中判定交给服务端的 blackRopeSweep。 */
@Mod.EventBusSubscriber(modid="sukuna", value=Dist.CLIENT)
public final class BlackRopeClient {
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered e){
        if(!e.isAttack())return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.screen!=null)return;
        if(!mc.player.getMainHandItem().is(Content.BLACK_ROPE.get()))return;
        e.setCanceled(true);
        e.setSwingHand(true);
        Network.CHANNEL.sendToServer(new Network.Cast(SkillAction.BLACK_ROPE));
    }
    private BlackRopeClient(){}
}
