package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 第三阶段的客户端演出：白色空间、强制第三人称、隐藏全部 HUD、锁住输入、
 * 黑屏 + 系统错误窗口、字幕、以及"斩击切屏"的玻璃裂纹。
 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class ObserverClient {
    private static boolean active,dialogHold;
    private static long tick,blackFrom=Long.MIN_VALUE,blackTo=Long.MIN_VALUE,lineFrom=Long.MIN_VALUE,lineTo=Long.MIN_VALUE;
    private static long nextDeferred;
    private static final java.util.List<Network.Effect> deferred=new java.util.ArrayList<>();
    private static CameraType savedCamera;
    public static boolean active(){return active;}
    public static boolean black(){return dialogHold||tick<blackTo;}

    public static void receive(Network.Effect e){
        // 系统错误窗口是模态的：窗口没关之前到达的演出先排队，关掉后按间隔补播，不会漏台词。
        if(dialogHold&&e.kind()!=ObserverRitual.FX_END){deferred.add(e);return;}
        dispatch(e);
    }
    private static void dispatch(Network.Effect e){
        switch(e.kind()){
            case ObserverRitual.FX_START -> active=true;
            case ObserverRitual.FX_SHAKE -> Client.shake(Math.max(4,e.life()));
            case ObserverRitual.FX_BLACK -> {blackFrom=tick;blackTo=tick+Math.max(1,e.life());}
            case ObserverRitual.FX_DIALOG -> {dialogHold=true;ObserverDialog.show();}
            case ObserverRitual.FX_LINE -> {lineFrom=tick;lineTo=tick+Math.max(1,e.life());}
            case ObserverRitual.FX_ATTACK -> {ScreenCracks.trigger(e.seed());Client.shake(16);}
            case ObserverRitual.FX_FIGHT -> ObserverFight.start();
            case ObserverRitual.FX_END -> end();
            default -> {}
        }
    }
    private static void end(){
        active=false;dialogHold=false;blackTo=Long.MIN_VALUE;lineTo=Long.MIN_VALUE;blackFrom=lineFrom=Long.MIN_VALUE;
        deferred.clear();ScreenCracks.clear();ObserverFight.end();
        if(savedCamera!=null){Minecraft.getInstance().options.setCameraType(savedCamera);savedCamera=null;}
    }
    /** 进世界/退世界时清干净，别让"隐藏 HUD + 强制第三人称"漏进下一局。 */
    @SubscribeEvent public static void login(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn e){end();}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){end();}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        tick++;ScreenCracks.advance();
        if(dialogHold&&!ObserverDialog.pending()){dialogHold=false;blackTo=tick+14;}
        if(!dialogHold&&!deferred.isEmpty()&&tick>=nextDeferred){nextDeferred=tick+25;dispatch(deferred.remove(0));}
        var mc=Minecraft.getInstance();
        if(!active)return;
        if(mc.player==null){end();return;}
        if(mc.options.getCameraType()!=CameraType.THIRD_PERSON_BACK){
            if(savedCamera==null)savedCamera=mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if(mc.player.isSprinting())mc.player.setSprinting(false);
    }

    /** 白色空间：在天空阶段画一个巨大的白盒子（相机在原点，和既有领域天空同一套坐标约定）。 */
    public static void whiteSky(RenderLevelStageEvent e){
        PoseStack ps=e.getPoseStack();
        ps.pushPose();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        double r=180;
        int c=0xFFFFFFFF;
        Client.quad(b,ps,new Vec3(-r,-r,-r),new Vec3(r,-r,-r),new Vec3(r,r,-r),new Vec3(-r,r,-r),c);
        Client.quad(b,ps,new Vec3(-r,-r,r),new Vec3(-r,r,r),new Vec3(r,r,r),new Vec3(r,-r,r),c);
        Client.quad(b,ps,new Vec3(-r,-r,-r),new Vec3(-r,r,-r),new Vec3(-r,r,r),new Vec3(-r,-r,r),c);
        Client.quad(b,ps,new Vec3(r,-r,-r),new Vec3(r,-r,r),new Vec3(r,r,r),new Vec3(r,r,-r),c);
        Client.quad(b,ps,new Vec3(-r,r,-r),new Vec3(r,r,-r),new Vec3(r,r,r),new Vec3(-r,r,r),c);
        Client.quad(b,ps,new Vec3(-r,-r,-r),new Vec3(-r,-r,r),new Vec3(r,-r,r),new Vec3(r,-r,-r),c);
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();
        ps.popPose();
    }

    /** 整层 HUD 逐个取消：血条、物品栏、聊天、boss 条全部消失。 */
    @SubscribeEvent public static void overlays(RenderGuiOverlayEvent.Pre e){if(active)e.setCanceled(true);}

    /** 黑屏 / 裂屏 / 字幕都画在 GUI 最后。 */
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        if(!active)return;
        var mc=Minecraft.getInstance();
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        if(ScreenCracks.visible())ScreenCracks.render(w,h);
        GuiGraphics g=e.getGuiGraphics();
        if(black())g.fill(0,0,w,h,0xFF000000);
        if(tick>=lineFrom&&tick<lineTo){
            String text=ObserverRitual.LINE;
            int tw=mc.font.width(text);
            g.fill(w/2-tw/2-10,h-64,w/2+tw/2+10,h-40,0x99000000);
            g.drawCenteredString(mc.font,text,w/2,h-57,0xFFF2F2F2);
        }
    }

    @SubscribeEvent public static void input(MovementInputUpdateEvent event){
        if(!active||event.getEntity()!=Minecraft.getInstance().player)return;
        var input=event.getInput();
        input.forwardImpulse=input.leftImpulse=0;
        input.up=input.down=input.left=input.right=input.jumping=input.shiftKeyDown=false;
    }
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered event){if(active)event.setCanceled(true);}
    private ObserverClient(){}
}
