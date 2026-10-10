package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** 只接收服务端进度；蓝色永远代表本机玩家。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class DomainClashClient {
    private static Network.ClashState state;
    private static Object world;
    private static net.minecraft.client.player.LocalPlayer player;
    private static long tick,lastPacket,resultUntil;
    private static boolean held;
    public static boolean active(){var mc=Minecraft.getInstance();return state!=null&&state.active()&&tick-lastPacket<100&&world==mc.level&&player!=null&&player==mc.player&&player.isAlive();}
    public static void receive(Network.ClashState next){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
        boolean start=next.active()&&(!active()||!next.id().equals(state.id()));
        state=next;lastPacket=tick;world=mc.level;player=mc.player;
        if(start){held=false;if(mc.screen instanceof SkillWheel)mc.setScreen(null);}
        if(!next.active()){held=false;resultUntil=tick+80;}
    }
    public static void clear(){state=null;held=false;resultUntil=0;player=null;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();world=null;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getInstance();tick++;
        if(world!=mc.level||mc.player==null||player!=null&&(player!=mc.player||!player.isAlive())){clear();world=mc.level;return;}
        if(!active())return;
        // 打开菜单或切出窗口时主动释放，防止丢失松开事件后无法继续计分。
        if(mc.screen!=null||!mc.isWindowActive())release();
        if(mc.player!=null){mc.player.setDeltaMovement(0,0,0);mc.player.fallDistance=0;}
    }
    private static void release(){if(held&&state!=null){Network.CHANNEL.sendToServer(new Network.ClashPress(state.id(),false));held=false;}}
    @SubscribeEvent public static void key(InputEvent.Key event){
        if(!active()||event.getKey()!=GLFW.GLFW_KEY_SPACE)return;
        if(event.getAction()==GLFW.GLFW_RELEASE){release();return;}
        // 忽略系统长按重复；必须松开再按，长按不算连点。
        if(event.getAction()!=GLFW.GLFW_PRESS||held||Minecraft.getInstance().screen!=null)return;
        held=true;Network.CHANNEL.sendToServer(new Network.ClashPress(state.id(),true));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void move(MovementInputUpdateEvent event){
        if(event.getEntity()!=Minecraft.getInstance().player||!active())return;var input=event.getInput();input.leftImpulse=input.forwardImpulse=0;
        input.up=input.down=input.left=input.right=input.jumping=input.shiftKeyDown=false;
        event.getEntity().setDeltaMovement(0,0,0);
    }
    @SubscribeEvent public static void interact(InputEvent.InteractionKeyMappingTriggered event){if(active()){event.setCanceled(true);event.setSwingHand(false);}}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void draw(RenderGuiEvent.Post event){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||state==null)return;
        var g=event.getGuiGraphics();int w=g.guiWidth(),h=g.guiHeight(),cx=w/2,y=Math.max(64,h/3);
        if(!active()){
            if(tick<resultUntil){String text=state.result()>0?"对拼获胜 · 领域延长20秒":state.result()<0?"对拼失败 · 熔断2分钟 · 治疗55%咒力":"对拼结束 · 双方领域解除";g.drawCenteredString(mc.font,text,cx,y,state.result()>0?0xFF67CEFF:0xFFFF8792);}return;
        }
        int width=Math.max(100,Math.min(300,w-36)),left=cx-width/2;
        g.fill(left-9,y-28,left+width+9,y+66,0xDB11131B);
        g.drawCenteredString(mc.font,"领 域 对 拼",cx,y-21,0xFFFFFFFF);
        int blue=width*Math.max(0,Math.min(100,state.percent()))/100;
        g.fill(left-3,y-3,left+width+3,y+19,0xFF000000);
        g.fill(left,y,left+width,y+16,0xFFD62F42);
        g.fill(left,y,left+blue,y+16,0xFF238DF2);
        for(int mark:new int[]{25,75}){int x=left+width*mark/100;g.fill(x,y,x+1,y+16,0xFF151515);}
        g.drawString(mc.font,"自己 "+state.percent()+"%",left,y+23,0xFF69C9FF);
        String enemy="对方 "+(100-state.percent())+"%";
        g.drawString(mc.font,enemy,left+width-mc.font.width(enemy),y+23,0xFFFF7582);
        String instruction=state.ready()>0?"准备…… "+((state.ready()+19)/20):"连续点按空格 · 蓝色达到75%获胜";
        g.drawCenteredString(mc.font,instruction,cx,y+37,0xFFFFFFFF);
        String detail=state.bossCps()>0?"对手手速 "+state.bossCps()+" 次/秒":"对手："+state.opponent();
        g.drawCenteredString(mc.font,detail+" · 剩余 "+((state.remaining()+19)/20)+"秒",cx,y+51,0xFFBBC5D9);
    }
    private DomainClashClient(){}
}
