package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.io.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

/** 先应用本人的共享选择；未设置时使用发行版默认值，询问版在答复前不读取。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class WallpaperClient {
    private static final class Incoming {
        final long revision,born;final int count,total;final byte[] data;int next,offset;
        Incoming(WallpaperPackets.Download p){revision=p.revision();count=p.count();total=p.total();data=new byte[total];born=tick;}
    }
    public record DesktopRect(float x,float y,float width,float height){}
    private static final ExecutorService io=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),r->{Thread t=new Thread(r,"sukuna-wallpaper-client");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static final Map<UUID,Incoming> incoming=new HashMap<>();
    private static final Map<UUID,Long> versions=new HashMap<>();
    private static final LinkedHashMap<UUID,ResourceLocation> textures=new LinkedHashMap<>(16,.75f,true);
    private static final Map<UUID,byte[]> encoded=new HashMap<>();
    private static final ArrayDeque<WallpaperPackets.Upload> uploads=new ArrayDeque<>();
    private static String preference="default",mode="ask",imagePath="",fit="fill";
    private static boolean initialized,allowed,promptPending;
    private static long session,generation,tick,nextUploadTick,decodeFrame=-1;
    private static final Properties config=new Properties();
    private static Path configPath(){return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("sukuna-wallpaper.properties");}
    private static void load(){
        boolean failed=false;
        config.clear();try{if(Files.isRegularFile(configPath()))try(var reader=Files.newBufferedReader(configPath())){config.load(reader);}}catch(IOException|IllegalArgumentException ex){failed=true;tell("壁纸配置读取失败，本次仍会询问。");}
        preference=failed?"ask":config.getProperty("consent","default");
        mode=WallpaperPolicy.resolve(preference,WallpaperPolicy.DEFAULT_MODE);
        imagePath=config.getProperty("wallpaperPath","").strip();fit=config.getProperty("fit","fill");
        if(!Files.exists(configPath()))save();
    }
    private static void save(){
        config.setProperty("consent",preference);config.setProperty("wallpaperPath",imagePath);config.setProperty("fit",fit);
        try{Files.createDirectories(configPath().getParent());try(var writer=Files.newBufferedWriter(configPath())){config.store(writer,"consent: default=跟随所装版本默认值, ask=每次询问, on=始终同意向同服玩家分享壁纸, off=始终关闭; wallpaperPath: 留空自动读取系统壁纸，仅支持本地静态图片; fit: fill/fit/stretch/center/tile");}}
        catch(IOException ex){tell("壁纸设置无法保存，重新进入游戏后可能再次询问。");}
    }
    public static void tell(String text){var p=Minecraft.getInstance().player;if(p!=null)p.displayClientMessage(Component.literal(text),false);}
    private static void revoke(){
        generation++;allowed=false;uploads.clear();if(Minecraft.getInstance().getConnection()!=null)Network.CHANNEL.sendToServer(new WallpaperPackets.Consent(false));
        var p=Minecraft.getInstance().player;if(p!=null)forget(p.getUUID());
    }
    public static void choose(boolean on,boolean remember){
        preference=remember?(on?"on":"off"):"ask";mode=preference;save();apply(on);
    }
    private static void apply(boolean on){
        promptPending=false;revoke();allowed=on;
        if(!on){tell("壁纸分享已关闭：你的斩击内部保持纯黑（含虚空、传承阶段的全部斩击）。");return;}
        Network.CHANNEL.sendToServer(new WallpaperPackets.Consent(true));long expectedSession=session,expectedGeneration=generation;
        DesktopRect desktop=desktop();String path=imagePath,style=fit;
        tell("壁纸共享已开启：将分享给同服全部玩家，只读取静态壁纸、不截屏。可用 /sukuna_wallpaper off 关闭。");
        try{io.execute(()->{
            byte[] bytes;try{bytes=WallpaperDesktop.read(path,style,(int)desktop.width,(int)desktop.height);}catch(Exception|LinkageError ex){Minecraft.getInstance().execute(()->{if(expectedSession==session&&expectedGeneration==generation&&allowed)tell("无法读取静态壁纸，暂时显示纯黑。可在配置中设置 wallpaperPath 后输入 /sukuna_wallpaper reload。");});return;}
            Minecraft.getInstance().execute(()->{
                var mc=Minecraft.getInstance();if(!allowed||session!=expectedSession||generation!=expectedGeneration||mc.player==null||mc.getConnection()==null)return;
                encoded.put(mc.player.getUUID(),bytes);releaseTexture(mc.player.getUUID());
                long token=java.util.concurrent.ThreadLocalRandom.current().nextLong();int count=(bytes.length+WallpaperRules.CHUNK-1)/WallpaperRules.CHUNK;
                for(int i=0;i<count;i++)uploads.add(new WallpaperPackets.Upload(token,i,count,bytes.length,Arrays.copyOfRange(bytes,i*WallpaperRules.CHUNK,Math.min(bytes.length,(i+1)*WallpaperRules.CHUNK))));
                tell("壁纸图片已读取，正在分片分享；其他玩家会在你的裂缝内看到这张壁纸。");
            });
        });}catch(RejectedExecutionException ex){tell("壁纸处理繁忙，暂时显示纯黑，请稍后重试。");}
    }
    private static void releaseTexture(UUID id){ResourceLocation texture=textures.remove(id);if(texture!=null)Minecraft.getInstance().getTextureManager().release(texture);}
    private static void forget(UUID id){releaseTexture(id);encoded.remove(id);incoming.remove(id);}
    public static void clear(){
        session++;generation++;allowed=false;initialized=false;promptPending=false;nextUploadTick=0;uploads.clear();incoming.clear();versions.clear();encoded.clear();
        for(UUID id:List.copyOf(textures.keySet()))releaseTexture(id);
    }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn e){clear();}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getConnection()==null)return;tick++;
        if(!initialized){initialized=true;load();if(mode.equals("on"))apply(true);else if(mode.equals("off"))apply(false);else promptPending=true;}
        if(promptPending&&mc.screen==null&&!FinaleClient.local()){promptPending=false;mc.setScreen(new WallpaperConsentScreen());}
        if(allowed)for(int i=0;i<2&&!uploads.isEmpty();i++){
            if(uploads.peek().index()==0){if(tick<nextUploadTick)break;nextUploadTick=tick+120;}
            Network.CHANNEL.sendToServer(uploads.remove());
        }
        incoming.values().removeIf(p->tick-p.born>1200);
    }
    public static void receive(WallpaperPackets.Download packet){
        var mc=Minecraft.getInstance();if(mc.getConnection()==null)return;UUID id=packet.owner();
        if(mc.player!=null&&id.equals(mc.player.getUUID())&&!allowed){forget(id);return;}
        if(versions.size()>=256&&!versions.containsKey(id))return;
        if(packet.revision()<versions.getOrDefault(id,Long.MIN_VALUE))return;
        if(packet.total()==0&&packet.count()==0&&packet.data().length==0){versions.put(id,packet.revision());forget(id);return;}
        if(!WallpaperRules.validPart(packet.index(),packet.count(),packet.total(),packet.data().length))return;
        Incoming in=incoming.get(id);
        if(packet.index()==0){if(incoming.size()>=128||encoded.size()>=128&&!encoded.containsKey(id))return;in=new Incoming(packet);incoming.put(id,in);versions.put(id,packet.revision());}
        if(in==null||in.revision!=packet.revision()||in.next!=packet.index()||in.total!=packet.total()||in.count!=packet.count()){incoming.remove(id);return;}
        System.arraycopy(packet.data(),0,in.data,in.offset,packet.data().length);in.offset+=packet.data().length;in.next++;
        if(in.next!=in.count)return;incoming.remove(id);byte[] data=in.data;long expected=session,rev=in.revision;
        try{io.execute(()->{byte[] clean;try{clean=WallpaperImage.sanitize(data);}catch(Exception ex){return;}
            Minecraft.getInstance().execute(()->{if(session!=expected||versions.getOrDefault(id,-1L)!=rev||Minecraft.getInstance().player!=null&&id.equals(Minecraft.getInstance().player.getUUID())&&!allowed)return;encoded.put(id,clean);releaseTexture(id);});
        });}catch(RejectedExecutionException ignored){}
    }
    /** GPU仅保留最近八张；压缩图只存在本次连接内存，退出就清除。 */
    public static boolean hasImage(UUID id){return id!=null&&encoded.containsKey(id);}
    public static ResourceLocation texture(UUID id){
        var mc=Minecraft.getInstance();if(mc.level==null||id==null)return null;
        ResourceLocation existing=textures.get(id);if(existing!=null)return existing;byte[] bytes=encoded.get(id);if(bytes==null||decodeFrame==tick)return null;decodeFrame=tick;
        try{
            // 先淘汰旧纹理，再解码新图，避免暂时同时保留九张完整像素图。
            while(textures.size()>=8)releaseTexture(textures.keySet().iterator().next());
            ResourceLocation location=new ResourceLocation("sukuna","wallpaper/"+id);
            WallpaperTexture.register(mc.getTextureManager(),location,bytes);textures.put(id,location);return location;
        }catch(IOException|RuntimeException ex){
            // 当前图片失败后不在每帧重试；重新授权或收到新图片时才恢复尝试。
            encoded.remove(id);LogUtils.getLogger().warn("世界斩壁纸加载失败，已回退纯黑（玩家 {}）",id,ex);return null;
        }
    }
    /** GLFW屏幕坐标使用窗口逻辑尺寸，不能把高DPI帧缓冲像素直接当桌面像素。 */
    public static DesktopRect desktop(){
        var window=Minecraft.getInstance().getWindow();var monitor=window.findBestMonitor();
        if(monitor==null)return new DesktopRect(window.getX(),window.getY(),Math.max(1,window.getScreenWidth()),Math.max(1,window.getScreenHeight()));
        // 只用完整显示器范围做定位；不读取桌面的任何像素。
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
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){
        var root=Commands.literal("sukuna_wallpaper").executes(c->{revoke();promptPending=true;return 1;});
        root.then(Commands.literal("on").executes(c->{revoke();promptPending=true;tell("请在弹窗中确认分享；选择始终开启可保存。");return 1;}));
        root.then(Commands.literal("off").executes(c->{choose(false,true);return 1;}));
        root.then(Commands.literal("ask").executes(c->{preference=mode="ask";save();revoke();promptPending=true;return 1;}));
        root.then(Commands.literal("reload").executes(c->{revoke();load();if(mode.equals("on"))apply(true);else if(mode.equals("off"))apply(false);else promptPending=true;return 1;}));
        root.then(Commands.literal("status").executes(c->{tell("壁纸分享："+(allowed?"本次开启":"本次关闭")+"；保存选项："+preference+"；本版默认："+WallpaperPolicy.DEFAULT_MODE+"；配置：config/sukuna-wallpaper.properties");return 1;}));
        e.getDispatcher().register(root);
    }
    private WallpaperClient(){}
}
