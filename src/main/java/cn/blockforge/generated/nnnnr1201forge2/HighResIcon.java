package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/** 高像素物品贴图的**通用生成器**：把任意分辨率的 PNG（比如 1024×1024 的原画）
 *  等比缩放成适合当"物品图标"的尺寸，透明边裁掉、居中放进正方形画布。
 *
 *  <p>为什么需要它：把 1024×1024 的原画直接丢进 {@code textures/item/} 虽然能用，
 *  但物品贴图会按原尺寸进显存、而且 16×16 的物品格子里细节全糊；把长宽比 5:1 的武器
 *  硬压进 16×16 只剩 4 像素宽。所以这里按"目标最长边"缩放（默认 16 的整数倍，
 *  32/64 都被原版渲染器正常支持），并保留透明通道。</p>
 *
 *  <p>用法（客户端资源重载后调用一次即可，也可由指令触发）：<br>
 *  {@code HighResIcon.convert("hiten",64,320)} —— 把 {@code textures/item_src/hiten.png}
 *  转成 {@code textures/item/hiten.png}。</p> */
public final class HighResIcon {
    /** 默认目标最长边（16×16 的物品格，32 或 64 都能被原版正常渲染）。 */
    public static final int DEFAULT_MAX=64;
    /** 源图目录（模组资源内）：{@code assets/sukuna/textures/item_src/<name>.png}。 */
    public static String sourcePath(String name){return "textures/item_src/"+name+".png";}
    /** 目标路径：{@code assets/sukuna/textures/item/<name>.png}。 */
    public static String targetPath(String name){return "textures/item/"+name+".png";}
    /** 把源图等比缩放成 maxSide 内的正方形图标。返回是否成功。 */
    public static boolean convert(String name,int maxSide){
        try{
            var rm=Minecraft.getInstance().getResourceManager();
            var res=rm.getResource(new net.minecraft.resources.ResourceLocation("sukuna",sourcePath(name)));
            if(res.isEmpty())return false;
            NativeImage src;
            try(InputStream in=res.get().open()){src=NativeImage.read(in);}
            NativeImage out=scale(src,maxSide);
            src.close();
            // 写回开发目录（仅开发环境有效；正式包里贴图已预先生成好）
            Path dir=Path.of("src/main/resources/assets/sukuna/textures/item");
            Files.createDirectories(dir);
            out.writeToFile(dir.resolve(name+".png").toFile()); // 公开 API（writeToChannel 是私有的）
            out.close();
            return true;
        }catch(Exception e){
            System.err.println("[SUKUNA] 高像素图标生成失败 "+name+": "+e);
            return false;
        }
    }
    /** 等比缩放 + 裁掉透明边 + 居中放到 maxSide×maxSide 的透明画布上。 */
    public static NativeImage scale(NativeImage src,int maxSide){
        int w=src.getWidth(),h=src.getHeight();
        // 1) 非透明包围盒（用来裁掉大片空白，避免"内容只占画布一角"）
        int x0=w,x1=-1,y0=h,y1=-1;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int a=(src.getPixelRGBA(x,y)>>>24)&255;
            if(a>8){if(x<x0)x0=x;if(x>x1)x1=x;if(y<y0)y0=y;if(y>y1)y1=y;}
        }
        if(x1<0){x0=y0=0;x1=w-1;y1=h-1;} // 全透明：整张缩
        int cw=x1-x0+1,ch=y1-y0+1;
        double k=Math.min(maxSide/(double)cw,maxSide/(double)ch);
        int nw=Math.max(1,(int)Math.round(cw*k)),nh=Math.max(1,(int)Math.round(ch*k));
        NativeImage out=new NativeImage(NativeImage.Format.RGBA,maxSide,maxSide,false);
        out.fillRect(0,0,maxSide,maxSide,0); // 透明底
        int ox=(maxSide-nw)/2,oy=(maxSide-nh)/2;
        for(int y=0;y<nh;y++)for(int x=0;x<nw;x++){
            int sx=x0+(int)(x/k),sy=y0+(int)(y/k);
            sx=Math.min(x1,Math.max(x0,sx));sy=Math.min(y1,Math.max(y0,sy));
            int argb=src.getPixelRGBA(sx,sy);
            // ARGB → out 需要的 ABGR 顺序（NativeImage 用的是 ABGR）
            int a=(argb>>>24)&255,r=(argb>>16)&255,g=(argb>>8)&255,b=argb&255;
            out.setPixelRGBA(ox+x,oy+y,(a<<24)|(b<<16)|(g<<8)|r);
        }
        return out;
    }
    private HighResIcon(){}
}
