package cn.blockforge.generated.nnnnr1201forge2;

import java.util.Arrays;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** 白色空间、独立高位机位和输入隔离；所有设置退出后归还。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class ObserverClient {
    private static boolean active;
    private static CameraType savedCamera;
    private static float dragX,dragY;
    private static final java.lang.reflect.Method CAMERA_POSITION=Arrays.stream(Camera.class.getDeclaredMethods())
        .filter(m->m.getReturnType()==void.class&&Arrays.equals(m.getParameterTypes(),new Class<?>[]{Vec3.class}))
        .findFirst().map(m->{m.setAccessible(true);return m;}).orElse(null);
    public static boolean active(){return active;}
    public static boolean white(){var s=ObserverFight.state();return active&&s!=null&&s.age()>=30;}
    public static boolean black(){var s=ObserverFight.state();return s!=null&&(s.age()>=160&&s.age()<180||s.phase()==2);}
    public static void begin(){
        if(active)return;active=true;var mc=Minecraft.getInstance();savedCamera=mc.options.getCameraType();
        DismantleMotionClient.clear();FinaleClient.clear();dragX=dragY=0;
    }
    public static void receive(Network.Effect e){
        switch(e.kind()){
            case ObserverRitual.FX_START -> begin();
            case ObserverRitual.FX_SHAKE -> Client.shake(Math.min(20,e.life()));
            case ObserverRitual.FX_DIALOG -> ObserverDialog.show();
            case ObserverRitual.FX_LINE -> ObserverDialog.say("高位",ObserverRitual.LINE);
            case ObserverRitual.FX_ATTACK -> {ScreenCracks.trigger(e.seed());ObserverFight.slash();}
            case ObserverRitual.FX_END -> {
                boolean victory=e.life()==1,close=ObserverFight.closeOnVictory();end();
                if(victory&&close)Minecraft.getInstance().stop(); // 服务端结算确认后正常保存退出，不使用 halt。
                else if(victory)Minecraft.getInstance().setScreen(new VictoryScreen());
            }
            default -> {}
        }
    }
    public static void end(){
        active=false;ObserverFight.end();ScreenCracks.clear();
        if(savedCamera!=null){Minecraft.getInstance().options.setCameraType(savedCamera);savedCamera=null;}
    }
    public static void drag(double dx,double dy){dragX=Math.max(-2,Math.min(2,dragX+(float)dx*.012f));dragY=Math.max(-1,Math.min(1,dragY+(float)dy*.012f));}
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn e){end();}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){end();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;ScreenCracks.advance();if(!active)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){end();return;}
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);mc.player.setSprinting(false);dragX*=.9f;dragY*=.9f;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void camera(ViewportEvent.ComputeCameraAngles e){
        if(!white())return;var mc=Minecraft.getInstance();var boss=ObserverFight.boss();if(boss==null)return;
        var state=ObserverFight.state();boolean god=state.age()>=180;
        Vec3 focus=(god?state.arena():boss.getPosition((float)e.getPartialTick())).add(0,1,0);
        // 机位永远固定在身体上方；鼠标只带来小幅观察偏移，不能重新控制身体。
        double mouseX=(mc.mouseHandler.xpos()/Math.max(1,mc.getWindow().getScreenWidth())-.5)*1.2;
        Vec3 at=focus.add(god?mouseX+dragX:1.5,god?8+dragY:1.3,god?8:4);
        e.getCamera().setup(mc.level,mc.player,true,false,(float)e.getPartialTick());
        if(CAMERA_POSITION!=null)try{CAMERA_POSITION.invoke(e.getCamera(),at);}catch(ReflectiveOperationException ignored){at=e.getCamera().getPosition();}
        Vec3 direction=focus.subtract(at);
        e.setYaw((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));
        e.setPitch((float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z))));e.setRoll(0);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hidePlayer(RenderPlayerEvent.Pre e){
        if(white()&&e.getEntity()==Minecraft.getInstance().player)e.setCanceled(true);
    }
    public static void whiteSky(RenderLevelStageEvent e){
        if(!white())return;PoseStack ps=e.getPoseStack();ps.pushPose();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        double r=180;int c=0xFFFFFFFF;
        Client.quad(b,ps,new Vec3(-r,-r,-r),new Vec3(r,-r,-r),new Vec3(r,r,-r),new Vec3(-r,r,-r),c);
        Client.quad(b,ps,new Vec3(-r,-r,r),new Vec3(-r,r,r),new Vec3(r,r,r),new Vec3(r,-r,r),c);
        Client.quad(b,ps,new Vec3(-r,-r,-r),new Vec3(-r,r,-r),new Vec3(-r,r,r),new Vec3(-r,-r,r),c);
        Client.quad(b,ps,new Vec3(r,-r,-r),new Vec3(r,-r,r),new Vec3(r,r,r),new Vec3(r,r,-r),c);
        Client.quad(b,ps,new Vec3(-r,r,-r),new Vec3(r,r,-r),new Vec3(r,r,r),new Vec3(-r,r,r),c);
        Client.quad(b,ps,new Vec3(-r,-r,-r),new Vec3(-r,-r,r),new Vec3(r,-r,r),new Vec3(r,-r,-r),c);
        BufferUploader.drawWithShader(b.end());RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();ps.popPose();
    }
    @SubscribeEvent public static void overlays(RenderGuiOverlayEvent.Pre e){if(active)e.setCanceled(true);}
    @SubscribeEvent public static void input(MovementInputUpdateEvent e){
        if(!active||e.getEntity()!=Minecraft.getInstance().player)return;var i=e.getInput();i.forwardImpulse=i.leftImpulse=0;i.up=i.down=i.left=i.right=i.jumping=i.shiftKeyDown=false;
    }
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered e){if(active){e.setCanceled(true);e.setSwingHand(false);}}
    private static final class VictoryScreen extends net.minecraft.client.gui.screens.Screen {
        VictoryScreen(){super(net.minecraft.network.chat.Component.literal("高位 · 控制权已收回"));}
        @Override public void render(net.minecraft.client.gui.GuiGraphics g,int x,int y,float partial){
            renderBackground(g);ObserverDialog.frame(g,Math.max(8,width/2-140),height/2-45,Math.min(280,width-16),90,"连接已恢复","连接已恢复。");
        }
    }
    private ObserverClient(){}
}
