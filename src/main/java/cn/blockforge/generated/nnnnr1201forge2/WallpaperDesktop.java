package cn.blockforge.generated.nnnnr1201forge2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

/** 仅在本人授权后调用系统的壁纸路径查询；只打开壁纸图片，从不截取桌面。 */
public final class WallpaperDesktop {
    public interface WallpaperUser32 extends StdCallLibrary {boolean SystemParametersInfoW(int action,int count,char[] path,int flags);}
    private record Desktop(Path path,String fit){}
    private static Desktop discover()throws IOException{
        String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
        if(os.contains("win")){
            try{
                char[] path=new char[32768];WallpaperUser32 api=Native.load("user32",WallpaperUser32.class);
                if(!api.SystemParametersInfoW(0x0073,path.length,path,0))throw new IOException("系统未返回壁纸路径");
                int end=0;while(end<path.length&&path[end]!=0)end++;
                String fit="fill";
                try{String tile=Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER,"Control Panel\\Desktop","TileWallpaper"),style=Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER,"Control Panel\\Desktop","WallpaperStyle");
                    fit="1".equals(tile)?"tile":switch(style){case "0"->"center";case "2"->"stretch";case "6"->"fit";default->"fill";};
                }catch(RuntimeException ignored){}
                if(end==0)throw new IOException("桌面没有静态图片壁纸");return new Desktop(Path.of(new String(path,0,end)),fit);
            }catch(LinkageError|RuntimeException ex){throw new IOException("无法读取系统壁纸设置",ex);}
        }
        if(os.contains("mac"))return new Desktop(Path.of(query("osascript","-e","tell application \"System Events\" to get picture of current desktop")),"fill");
        String key="picture-uri";
        try{if(query("gsettings","get","org.gnome.desktop.interface","color-scheme").contains("prefer-dark"))key="picture-uri-dark";}catch(IOException ignored){}
        String value=query("gsettings","get","org.gnome.desktop.background",key);
        if(value.startsWith("'")&&value.endsWith("'"))value=value.substring(1,value.length()-1);
        URI uri=URI.create(value);if(!"file".equalsIgnoreCase(uri.getScheme()))throw new IOException("未找到本地静态壁纸");
        String options="fill";try{String setting=query("gsettings","get","org.gnome.desktop.background","picture-options");options=setting.contains("scaled")?"fit":setting.contains("stretched")?"stretch":setting.contains("centered")?"center":setting.contains("wallpaper")?"tile":"fill";}catch(IOException ignored){}
        return new Desktop(Path.of(uri),options);
    }
    private static String query(String... command)throws IOException{
        Process p=new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try{if(!p.waitFor(2,TimeUnit.SECONDS)){p.destroyForcibly();throw new IOException("读取壁纸设置超时");}
            if(p.exitValue()!=0)throw new IOException("此系统暂不支持自动读取壁纸");
            try(var in=p.getInputStream()){return new String(in.readNBytes(16384),StandardCharsets.UTF_8).strip();}
        }catch(InterruptedException ex){Thread.currentThread().interrupt();p.destroyForcibly();throw new IOException("壁纸读取已取消",ex);}
    }
    public static byte[] read(String override,String fitOverride,int desktopWidth,int desktopHeight)throws IOException{
        Desktop desktop=override.isBlank()?discover():new Desktop(Path.of(override),fitOverride);
        Path path=desktop.path();if(!Files.isRegularFile(path)||Files.size(path)>64L*1024*1024)throw new IOException("壁纸不存在或大于64兆");
        byte[] input;try(var stream=Files.newInputStream(path)){input=stream.readNBytes(64*1024*1024+1);}
        if(input.length>64*1024*1024)throw new IOException("壁纸文件过大");
        BufferedImage original=WallpaperImage.decode(input,16384,64L*1024*1024);
        try{
            double factor=Math.min(1,Math.min(1536.0/desktopWidth,864.0/desktopHeight));
            int w=Math.max(1,(int)(desktopWidth*factor)),h=Math.max(1,(int)(desktopHeight*factor));
            for(int attempt=0;attempt<3;attempt++){
                BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
                try{
                    g.setColor(Color.BLACK);g.fillRect(0,0,w,h);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    int iw=original.getWidth(),ih=original.getHeight();double sx=w/(double)iw,sy=h/(double)ih;
                    String fit=override.isBlank()?desktop.fit():fitOverride;
                    if("stretch".equals(fit))g.drawImage(original,0,0,w,h,null);
                    else if("tile".equals(fit)){int tw=Math.max(1,(int)(iw*w/(double)desktopWidth)),th=Math.max(1,(int)(ih*h/(double)desktopHeight));for(int y=0;y<h;y+=th)for(int x=0;x<w;x+=tw)g.drawImage(original,x,y,tw,th,null);}
                    else {double scale="center".equals(fit)?w/(double)desktopWidth:"fit".equals(fit)?Math.min(sx,sy):Math.max(sx,sy);int tw=Math.max(1,(int)(iw*scale)),th=Math.max(1,(int)(ih*scale));g.drawImage(original,(w-tw)/2,(h-th)/2,tw,th,null);}
                }finally{g.dispose();}
                byte[] bytes;try{bytes=WallpaperImage.jpeg(image);}finally{image.flush();}
                if(bytes.length<=WallpaperRules.MAX_BYTES)return bytes;w=Math.max(1,w*3/4);h=Math.max(1,h*3/4);
            }
            throw new IOException("壁纸压缩后仍然过大");
        }finally{original.flush();}
    }
    private WallpaperDesktop(){}
}
