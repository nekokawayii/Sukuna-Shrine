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
    /** 第二条抓取通道：AWT Robot（与 GDI **并行**跑，谁抓到有效画面就用谁）。
     *  全屏/GL 窗口下 GDI 常常全黑，这时 Robot 往往还能拿到画面，两条一起跑最稳。 */
    private static final ExecutorService robot=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),
        r->{Thread t=new Thread(r,"sukuna-wallpaper-robot");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;},
        new ThreadPoolExecutor.AbortPolicy());
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
    private static boolean enabled=true,initialized,announced,capturing,failed,captureLogged,statsLogged,debugOverlay,blankWarned,robotBroken,robotNoted;
    /** 上一帧是哪条通道给的（GDI / Robot），排查时看日志。 */
    private static String lastSource="-";
    /** 过期内容探针：GDI 帧的指纹与连续未变次数。 */
    private static long gdiHash;private static int gdiSameCount;private static boolean gdiStaleNoted;
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
    /** 调试预览：把采到的这一帧直接画在左下角（`/sukuna_wallpaper debug`）——
     *  用来一眼区分"纹理是黑的"还是"纹理没问题、只是斩击那边没画出来"。 */
    public static void drawDebug(net.minecraft.client.gui.GuiGraphics g){
        if(!debugOverlay||current==null)return;
        var mc=Minecraft.getInstance();
        int w=Math.min(420,Math.max(160,g.guiWidth()/3)),h=w*9/16;
        int x=10,y=g.guiHeight()-h-14;
        try{g.blit(current,x,y,0,0,w,h,w,h);}catch(RuntimeException ignored){return;}
        g.drawString(mc.font,"斩击壁纸调试·纹理内容（应能看到你本机的窗口）",x,y-11,0xFFFF9A6A);
    }
    /** 这条通道抓到的画面"有没有内容"：亮像素占比（每 64 像素抽查一个）。 */
    private static double score(int[] pixels){
        if(pixels==null||pixels.length==0)return -1;
        long lit=0;int n=0;
        for(int i=0;i<pixels.length;i+=64){
            int v=pixels[i];
            if((((v>>16)&0xFF)+((v>>8)&0xFF)+(v&0xFF))/3>8)lit++;
            n++;
        }
        return n==0?-1:lit/(double)n;
    }
    private static void noteRobotBroken(){
        if(robotNoted)return;
        robotNoted=true;
        LogUtils.getLogger().info("[斩击壁纸] Robot 通道不可用（本轮由 GDI 提供画面）——多为 AWT 处于 headless；已在客户端初始化时尝试关闭 headless");
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
        tell("无法截取本机桌面（"+ex.getMessage()+"），斩击保持纯黑。");
        LogUtils.getLogger().warn("斩击壁纸：本机桌面截取失败",ex);
    }
    /** 后台线程截屏：Windows 走 **GDI BitBlt**（不依赖 AWT，headless 环境下也能用），
     *  其它平台退回 {@code java.awt.Robot}；之后等比缩到 {@link #MAX_EDGE} 以内并转成 NativeImage(ABGR)。 */
    private static NativeImage capture(DesktopRect area)throws Exception{
        int sx=Math.max(0,Math.round(area.x())),sy=Math.max(0,Math.round(area.y()));
        int sw=Math.max(1,Math.round(area.width())),sh=Math.max(1,Math.round(area.height()));
        // 两条通道并行：Robot 在另一条线程上同时抓，GDI 在本线程抓，最后按"亮像素占比"择优
        Future<int[]> robotTask=null;
        try{if(!robotBroken)robotTask=robot.submit(()->captureRobot(sx,sy,sw,sh));}catch(RejectedExecutionException busy){}
        int[] gdi=null;String gdiError=null;
        try{gdi=captureGdi(sx,sy,sw,sh);}catch(Throwable ex){gdiError=ex.getClass().getSimpleName();}
        int[] shot=null;
        if(robotTask!=null){
            try{shot=robotTask.get(700,TimeUnit.MILLISECONDS);}
            catch(Throwable ex){robotTask.cancel(true);if(ex instanceof ExecutionException)robotBroken=true;}
        }
        double gdiScore=score(gdi),robotScore=score(shot);
        if(gdi!=null){   // 过期内容探针：GDI 帧连续很多帧完全不变 → 说明拿到的是"卡住的旧画面"
            long h=0;for(int i=0;i<gdi.length;i+=4096)h=h*31+gdi[i];
            if(h==gdiHash){if(++gdiSameCount==40&&!gdiStaleNoted){gdiStaleNoted=true;
                LogUtils.getLogger().info("[斩击壁纸] GDI 通道连续 40 帧画面完全未变：确认拿到的是过期内容（GL 窗口下 GDI/PrintWindow 的通病）");}}
            else{gdiSameCount=0;gdiHash=h;}
        }
        if(robotScore<0&&robotBroken&&robotTask!=null)noteRobotBroken();
        int[] pixels;String from;
        if(gdiScore<0&&robotScore<0){
            if(gdiError!=null)throw new IllegalStateException("两条抓取都失败（GDI:"+gdiError+"）");
            pixels=gdi==null?(shot==null?new int[sw*sh]:shot):gdi;from="空白";
        }else if(robotScore>=0&&(gdiScore<0||robotScore>=gdiScore-0.05)){
            // **Robot 优先**：GDI/PrintWindow 对 GL 窗口只能拿到"过期内容"（比如启动时的 Mojang 画面），
            // 而"非黑占比"评分判不出过期，所以不能只比分数——有 Robot 就用 Robot。
            pixels=shot;from="Robot";
        }else if(gdiScore>=0){
            pixels=gdi;from="GDI(可能过期)";
        }else{
            pixels=shot;from="Robot";
        }
        lastSource=from;
        double scale=Math.min(1.0,MAX_EDGE/(double)Math.max(sw,sh));
        int w=Math.max(1,(int)Math.round(sw*scale)),h=Math.max(1,(int)Math.round(sh*scale));
        NativeImage image=new NativeImage(NativeImage.Format.RGBA,w,h,false);
        long lum=0;int bright=0;long nonBlack=0;
        for(int y=0;y<h;y++){
            int srcY=(int)((long)y*sh/h)*sw;
            for(int x=0;x<w;x++){
                int argb=pixels[srcY+(int)((long)x*sw/w)];
                image.setPixelRGBA(x,y,(0xFF000000)|((argb&0xFF)<<16)|(argb&0xFF00)|((argb>>16)&0xFF));
                int v=(((argb>>16)&0xFF)+((argb>>8)&0xFF)+(argb&0xFF))/3;
                lum+=v;if(v>bright)bright=v;if(v>8)nonBlack++;
            }
        }
        if(!statsLogged){
            statsLogged=true;
            LogUtils.getLogger().info("[斩击壁纸] 本帧内容统计：{}x{} 来源={} 平均亮度 {} 最亮 {} 非黑占比 {}%（GDI {} / Robot {}）",
                w,h,from,lum/(double)(w*h),bright,String.format("%.1f",100.0*nonBlack/(w*h)),
                String.format("%.2f",gdiScore),String.format("%.2f",robotScore));
        }
        return image;
    }
    /** Windows：GetDC → 兼容 DC/位图 → BitBlt → GetDIBits（32bpp 顶向下的 BGRA）。 */
    private static int[] captureGdi(int x,int y,int w,int h){
        var user32=com.sun.jna.platform.win32.User32.INSTANCE;
        var gdi32=com.sun.jna.platform.win32.GDI32.INSTANCE;
        com.sun.jna.platform.win32.WinDef.HDC screen=user32.GetDC(null);
        if(screen==null)throw new IllegalStateException("GetDC 失败");
        com.sun.jna.platform.win32.WinDef.HDC mem=null;
        com.sun.jna.platform.win32.WinDef.HBITMAP bmp=null;        try{
            mem=gdi32.CreateCompatibleDC(screen);
            bmp=gdi32.CreateCompatibleBitmap(screen,w,h);
            if(mem==null||bmp==null)throw new IllegalStateException("创建位图失败");
            gdi32.SelectObject(mem,bmp);   // 位图选进临时 DC；DC 随后整体删掉，不需要恢复原对象
            if(!gdi32.BitBlt(mem,0,0,w,h,screen,x,y,SRCCOPY|CAPTUREBLT))throw new IllegalStateException("BitBlt 失败");
            var info=new com.sun.jna.platform.win32.WinGDI.BITMAPINFO();
            info.bmiHeader.biWidth=w;
            info.bmiHeader.biHeight=-h;                 // 负数 = 顶向下，取出来的行序和屏幕一致
            info.bmiHeader.biPlanes=1;
            info.bmiHeader.biBitCount=(short)32;
            info.bmiHeader.biCompression=com.sun.jna.platform.win32.WinGDI.BI_RGB;
            info.bmiHeader.write();
            com.sun.jna.Memory buffer=new com.sun.jna.Memory((long)w*h*4);
            byte[] raw=readDib(gdi32,mem,bmp,w,h,buffer,info);
            // 全屏 / GL 窗口下 BitBlt 常常整屏全黑：补一次 DWM 合成抓取（PW_RENDERFULLCONTENT=2）
            blankFrame=false;
            if(isBlank(raw)){
                if(user32.PrintWindow(user32.GetDesktopWindow(),mem,2))raw=readDib(gdi32,mem,bmp,w,h,buffer,info);
                blankFrame=isBlank(raw);
            }
            int[] out=new int[w*h];
            for(int i=0,o=0;i<out.length;i++,o+=4)
                out[i]=((raw[o+2]&0xFF)<<16)|((raw[o+1]&0xFF)<<8)|(raw[o]&0xFF);   // BGRA → RGB
            return out;
        }finally{
            if(bmp!=null)gdi32.DeleteObject(bmp);
            if(mem!=null)gdi32.DeleteDC(mem);
            user32.ReleaseDC(null,screen);
        }
    }
    /** 这一帧是不是"全黑"：每 64 个像素抽查一次，够快也能识别全屏黑屏那种情况。 */
    private static boolean isBlank(byte[] raw){
        long lit=0;int samples=0;
        for(int o=0;o+3<raw.length;o+=64*4){
            int v=((raw[o+2]&0xFF)+((raw[o+1]&0xFF)+(raw[o]&0xFF)))/3;
            if(v>8)lit++;
            samples++;
        }
        return samples==0||lit*100L/samples<1;   // 亮像素 <1% 视为全黑
    }
    private static byte[] readDib(com.sun.jna.platform.win32.GDI32 gdi32,com.sun.jna.platform.win32.WinDef.HDC mem,
                                  com.sun.jna.platform.win32.WinDef.HBITMAP bmp,int w,int h,com.sun.jna.Memory buffer,
                                  com.sun.jna.platform.win32.WinGDI.BITMAPINFO info){
        if(gdi32.GetDIBits(mem,bmp,0,h,buffer,info,com.sun.jna.platform.win32.WinGDI.DIB_RGB_COLORS)==0)
            throw new IllegalStateException("GetDIBits 失败");
        return buffer.getByteArray(0,w*h*4);
    }
    /** 上一帧是否全黑（全屏模式下 GDI 抓不到游戏画面时会是 true）。 */
    public static boolean blankFrame(){return blankFrame;}
    private static boolean blankFrame;
    private static final int SRCCOPY=0x00CC0020,CAPTUREBLT=0x40000000;
    /** 非 Windows 兜底：AWT Robot（需要非 headless 环境）。 */
    private static int[] captureRobot(int x,int y,int w,int h){
        try{
            BufferedImage raw=new Robot().createScreenCapture(new Rectangle(x,y,w,h));
            int[] out=raw.getRGB(0,0,w,h,null,0,w);
            raw.flush();
            return out;
        }catch(AWTException|HeadlessException ex){
            throw new IllegalStateException("系统拒绝了截屏（headless）",ex);
        }
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
            if(blankFrame()&&!blankWarned){
                blankWarned=true;
                tell("斩击壁纸：系统抓到的是**全黑画面**——全屏模式下 GDI 取不到游戏与桌面画面。请把游戏切成**窗口模式**"
                    +"（视频设置 → 全屏：关，或按 F11）再试；窗口模式下桌面、任务管理器、其他窗口都会出现在斩击里。");
                LogUtils.getLogger().warn("[斩击壁纸] 抓到全黑帧：BitBlt 与 PrintWindow 都没取到画面，通常是全屏模式所致");
            }
            if(!captureLogged){
                captureLogged=true;
                LogUtils.getLogger().info("[斩击壁纸] 本机桌面采集成功：显示器 {}x{} → 贴图 {}x{}（本机玩家 {}）",
                    Math.round(desktop().width()),Math.round(desktop().height()),image.getWidth(),image.getHeight(),self);
            }
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
        root.then(Commands.literal("debug").executes(c->{debugOverlay=!debugOverlay;tell("调试预览："+(debugOverlay?"开（左下角显示采到的本机画面，用来确认纹理内容）":"关"));return 1;}));
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
