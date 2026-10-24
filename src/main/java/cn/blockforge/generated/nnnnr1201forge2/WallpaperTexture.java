package cn.blockforge.generated.nnnnr1201forge2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

/** 仅客户端使用：壁纸解码、上传期间的原生资源必须有明确的所有者。 */
public final class WallpaperTexture {
    public static void register(TextureManager manager,ResourceLocation location,byte[] bytes)throws IOException{
        if(bytes==null||bytes.length<4||bytes.length>WallpaperRules.MAX_BYTES)throw new IOException("壁纸数据大小无效");
        NativeImage image=null;
        DynamicTexture texture=null;
        try{
            // 1.20.1 的 read(byte[]) 会把整张压缩图复制进 MemoryStack，普通壁纸也会撑爆。
            // InputStream 重载改用独立原生缓冲，并在成功或失败后自行释放输入缓冲。
            // 网络传输的是 JPEG；原生纹理读取在部分环境只接受 PNG，不能直接把 JPEG 当 PNG。
            // 复用有尺寸上限的解码器，明确转成游戏需要的 ABGR 像素，避免整张图回退黑色。
            var decoded=WallpaperImage.decode(bytes,WallpaperRules.MAX_EDGE,WallpaperRules.MAX_PIXELS);
            try{
                if(!WallpaperRules.validImage(decoded.getWidth(),decoded.getHeight()))throw new IOException("壁纸尺寸超过限制");
                image=new NativeImage(decoded.getWidth(),decoded.getHeight(),false);
                for(int y=0;y<decoded.getHeight();y++)for(int x=0;x<decoded.getWidth();x++){
                    int rgb=decoded.getRGB(x,y);image.setPixelRGBA(x,y,0xFF000000|((rgb&255)<<16)|(rgb&0xFF00)|((rgb>>16)&255));
                }
            }finally{decoded.flush();}
            texture=new DynamicTexture(image);
            texture.setFilter(true,false);texture.upload();
            manager.register(location,texture);
            // 成功后交由 TextureManager 负责缓存淘汰、撤回分享和离服时的释放。
            texture=null;
            image=null;
        }finally{
            // 解码后的尺寸校验、纹理构造或注册失败，也不能遗留原生像素内存。
            if(texture!=null)texture.close();
            else if(image!=null)image.close();
        }
    }
    private WallpaperTexture(){}
}
