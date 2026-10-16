package cn.blockforge.generated.nnnnr1201forge2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

/** 世界斩壁纸（**回归单文件版**）：斩击内部显示**本机桌面的实时画面**——
 *  开着的窗口、任务管理器、壁纸全都一起在里面（就是"斩击是桌面上的一扇窗"）。
 *
 *  <ul>
 *    <li>**不再分享**：没有任何上传/下载/中转。{@link #hasImage} 只认**本机玩家自己的 UUID**，
 *        所以别人的斩击永远是纯黑（画面里没有别人的桌面）。</li>
 *    <li>**采集**：{@code java.awt.Robot} 截取游戏窗口所在显示器的整块画面，等比缩到
 *        {@link #MAX_EDGE} 以内，每 {@link #REFRESH_TICKS} 刻刷新一次（后台线程采集，主线程上传纹理）。</li>
 *    <li>**采样**：着色器 {@code wallpaper_slash} 按**屏幕坐标**取样（DesktopRegion 把游戏窗口
 *        映射到桌面区域），所以斩击里出现的正好是它背后那块桌面，窗口/任务管理器都原样可见。</li>
 *    <li>关闭：{@code /sukuna_wallpaper off}（写进 config/sukuna-wallpaper.properties）；开启同名命令。</li>
 *  </ul> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class WallpaperClient {
    public record DesktopRect(float x,float y,float width,float height){}
    /** 刷新间隔（刻）与采集后的最大边长（越小越省显存与带宽，采样按屏幕坐标所以不影响构图）。 */
    private static final int REFRESH_TICKS=10,MAX_EDGE=1280;
    private static final ResourceLocation[] RING={new ResourceLocation("sukuna","wallpaper/live0"),
        new ResourceLocation("sukuna","wallpaper/live1"),new ResourceLocation("sukuna","wallpaper/live2")};
    private static final ExecutorService io=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(2),
        r->{Thread t=new Thread(r,"sukuna-wallpaper-live");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;},
        new ThreadPoolExecutor.AbortPolicy());
    private static final Properties config=new Properties();
    /** 当前帧用的纹理名（三张轮转：换新的时候把上一张释放掉，不积压 GL 纹理与像素内存）。 */
    private static ResourceLocation current;
    private static int ringIndex;
    private static UUID self;
    private static boolean enabled=true,initialized,announced,capturing,failed;
    private static long tick;

    private static Path configPath(){return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("sukuna-wallpaper.properties");}
    private static void load(){
        config.clear();
        try{if(Files.isRegularFile(configPath()))try(var reader=Files.newBufferedReader(configPath())){config.load(reader);}}catch(Exception ignored){}
        enabled=!"false".equalsIgnoreCase(config.getProperty("enabled","true"));
        if(!Files.exists(configPath()))save();
    }
    private static void save(){
        config.setProperty("enabled",Boolean.toString(enabled));
        try{Files.createDirectories(configPath().getParent());try(var writer=Files.newBufferedWriter(configPath())){config.store(writer,"enabled=true 时：斩击内部显示本机桌面实时画面（含窗口/任务管理器），只在本机显示、不上传分享");}}
        catch(Exception ex){tell("壁纸设置无法保存。");}
    }
    public static void tell(String text){var p=Minecraft.getInstance().player;if(p!=null)p.displayClientMessage(Component.literal(text),false);}
    public static void tellBar(String text){var p=Minecraft.getInstance().player;if(p!=null)p.displayClientMessage(Component.literal(text),true);}

    // ------------------------------------------------------------ 查询（渲染层用）
    /** 只有"本机玩家 + 已经采到过一帧"才算有图：别人的斩击因此保持纯黑。 */
    public static boolean hasImage(UUID id){
        return enabled&&current!=null&&id!=null&&id.equals(self);
    }
    public static ResourceLocation texture(UUID id){
        return hasImage(id)?current:null;
    }
    public static void clear(){
        self=null;announced=false;initialized=false;capturing=false;failed=false;
    }
    @SubscribeEvent public static void login(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn e){clear();}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){clear();}

    // ------------------------------------------------------------ 每刻：定时采一帧
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.getConnection()==null){return;}
        tick++;
        if(!initialized){
            initialized=true;load();self=mc.player.getUUID();
            if(enabled)announce();
        }
        if(mc.player!=null)self=mc.player.getUUID();
        if(!enabled||failed||capturing||tick%REFRESH_TICKS!=0)return;
        capturing=true;
        DesktopRect area=desktop();
        try{
            io.execute(()->{
                NativeImage frame=null;
                try{frame=capture(area);}catch(Throwable ex){capturing=false;Minecraft.getInstance().execute(()->fail(ex));return;}
                NativeImage ready=frame;
                Minecraft.getInstance().execute(()->{capturing=false;upload(ready);});
            });
        }catch(RejectedExecutionException busy){capturing=false;}
    }
    private static void announce(){
        tell("【斩击壁纸 · 本机版】斩击内部显示你**本机的实时桌面**（开着的窗口、任务管理器、壁纸都会出现），"
            +"画面只在本机生成、**不上传也不分享**；别人的斩击是纯黑。关闭：/sukuna_wallpaper off");
        tellBar("斩击壁纸：本机桌面实时画面 · 仅本地");
        LogUtils.getLogger().info("[斩击壁纸] 本机实时桌面模式 = 开（单文件版，无分享），配置 {}",configPath());
    }
    private static void fail(Throwable ex){
        failed=true;
        tell("无法截取本机桌面（"+ex.getClass().getSimpleName()+"），斩击保持纯黑。");
        LogUtils.getLogger().warn("斩击壁纸：本机桌面截取失败",ex);
    }
    /** 后台线程：截屏 → 等比缩小 → NativeImage（ABGR，DirectByteBuffer 之外的普通内存）。 */
    private static NativeImage capture(DesktopRect area)throws Exception{
        int sx=Math.max(1,Math.round(area.x())),sy=Math.max(1,Math.round(area.y()));
        int sw=Math.max(1,Math.round(area.width())),sh=Math.max(1,Math.round(area.height()));
        BufferedImage raw=new Robot().createScreenCapture(new Rectangle(sx,sy,sw,sh));
        double scale=Math.min(1.0,MAX_EDGE/(double)Math.max(sw,sh));
        int w=Math.max(1,(int)Math.round(sw*scale)),h=Math.max(1,(int)Math.round(sh*scale));
        BufferedImage scaled=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=scaled.createGraphics();
        try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(raw,0,0,w,h,null);}finally{g.dispose();raw.flush();}
        NativeImage image=new NativeImage(NativeImage.Format.RGBA,w,h,false);
        int[] row=new int[w];
        for(int y=0;y<h;y++){
            scaled.getRGB(0,y,w,1,row,0,w);
            for(int x=0;x<w;x++){
                int argb=row[x];
                image.setPixelRGBA(x,y,(argb&0xFF000000)|((argb&0xFF)<<16)|(argb&0xFF00)|((argb>>16)&0xFF));
            }
        }
        scaled.flush();
        return image;
    }
    /** 主线程：三张纹理轮转——注册新的一帧，随后释放上一张（GL 纹理与像素一起回收，不积压）。 */
    private static void upload(NativeImage image){
        if(!enabled){image.close();return;}
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){image.close();return;}
        try{
            var tm=mc.getTextureManager();
            ResourceLocation old=current;
            ResourceLocation next=RING[ringIndex=(ringIndex+1)%RING.length];
            tm.register(next,new DynamicTexture(image));   // DynamicTexture 接管这一帧的像素
            current=next;
            if(old!=null&&old!=next)tm.release(old);       // 释放上一张
        }catch(RuntimeException ex){
            image.close();fail(ex);
        }
    }

    // ------------------------------------------------------------ 桌面/窗口定位（shader 用）
    /** 游戏窗口所在显示器的范围（**只做定位**；采集出来的像素也只用于本机斩击显示）。 */
    public static DesktopRect desktop(){
        var window=Minecraft.getInstance().getWindow();var monitor=window.findBestMonitor();
        if(monitor==null)return new DesktopRect(window.getX(),window.getY(),Math.max(1,window.getScreenWidth()),Math.max(1,window.getScreenHeight()));
        float scaleX=1,scaleY=1;
        if(Minecraft.ON_OSX)try(var stack=MemoryStack.stackPush()){
            var x=stack.mallocFloat(1);var y=stack.mallocFloat(1);GLFW.glfwGetMonitorContentScale(monitor.getMonitor(),x,y);scaleX=Math.max(1,x.get(0));scaleY=Math.max(1,y.get(0));
        }
        return new DesktopRect(monitor.getX(),monitor.getY(),monitor.getCurrentMode().getWidth()/scaleX,monitor.getCurrentMode().getHeight()/scaleY);
    }
    public static DesktopRect window(){
        long handle=Minecraft.getInstance().getWindow().getWindow();try(var stack=MemoryStack.stackPush()){
            var x=stack.mallocInt(1);var y=stack.mallocInt(1);var w=stack.mallocInt(1);var h=stack.mallocInt(1);GLFW.glfwGetWindowPos(handle,x,y);GLFW.glfwGetWindowSize(handle,w,h);return new DesktopRect(x.get(0),y.get(0),Math.max(1,w.get(0)),Math.max(1,h.get(0)));
        }
    }

    // ------------------------------------------------------------ 命令
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){
        var root=Commands.literal("sukuna_wallpaper").executes(c->{toggle();return 1;});
        root.then(Commands.literal("on").executes(c->{set(true);return 1;}));
        root.then(Commands.literal("off").executes(c->{set(false);return 1;}));
        root.then(Commands.literal("refresh").executes(c->{tick=tick-tick%REFRESH_TICKS;tell("将立刻重新采集本机桌面。");return 1;}));
        root.then(Commands.literal("status").executes(c->{tell("斩击壁纸："+(enabled?"开启":"关闭")+" · 本机实时桌面（不上传分享）· "+(current==null?"尚未采到画面":"正在显示本机桌面")+" · 配置 config/sukuna-wallpaper.properties");return 1;}));
        e.getDispatcher().register(root);
    }
    private static void toggle(){set(!enabled);}
    private static void set(boolean on){
        enabled=on;failed=false;save();
        if(!on)tell("斩击壁纸已关闭：斩击内部保持纯黑（你自己的也是）。");
        else{tell("斩击壁纸已开启：斩击内部显示本机桌面实时画面（含窗口/任务管理器），只在本机显示、不上传分享。");tick=tick-tick%REFRESH_TICKS;}
    }
    private WallpaperClient(){}
}
