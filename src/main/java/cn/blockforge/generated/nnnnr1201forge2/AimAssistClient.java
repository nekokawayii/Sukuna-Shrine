package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class AimAssistClient {
    private static Network.AimState state=new Network.AimState(true,AimRules.DEFAULT_ANGLE,AimRules.DEFAULT_RANGE,false);
    private static double fov=70;
    public static void receive(Network.AimState value){state=value;}
    public static void clear(){state=new Network.AimState(true,AimRules.DEFAULT_ANGLE,AimRules.DEFAULT_RANGE,false);fov=70;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void fov(ViewportEvent.ComputeFov event){if(event.usedConfiguredFov())fov=event.getFOV();}
    @SubscribeEvent public static void overlay(RenderGuiOverlayEvent.Post event){
        var mc=Minecraft.getInstance();
        if(!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())||mc.player==null||mc.screen!=null||mc.options.hideGui||mc.player.isSpectator()||!mc.options.getCameraType().isFirstPerson()||!Client.progress.unlocked()||!state.enabled()||!state.circle()||mc.player.isScoping())return;
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        // 用当前视野角和界面缩放计算真实圆锥投影，默认3度只有准星附近一小圈。
        double radius=AimRules.radius(AimRules.angle(state.angle()),fov,h);if(!Double.isFinite(radius)||radius<1)return;
        var g=event.getGuiGraphics();int steps=Math.max(48,Math.min(720,(int)Math.ceil(radius*8)));
        int oldX=Integer.MIN_VALUE,oldY=Integer.MIN_VALUE;
        for(int i=0;i<steps;i++){
            double a=i*Math.PI*2/steps;int x=(int)Math.round(w/2.0+Math.cos(a)*radius),y=(int)Math.round(h/2.0+Math.sin(a)*radius);
            if(x==oldX&&y==oldY)continue;oldX=x;oldY=y;
            g.fill(x,y,x+1,y+1,0x90D5EEFF);
        }
    }
    private AimAssistClient(){}
}
