package cn.blockforge.generated.nnnnr1201forge2;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** 解码前读尺寸限制内存；只接受重新编码后的JPEG，绝不把路径和元数据转发给其他玩家。 */
public final class WallpaperImage {
    public static BufferedImage decode(byte[] bytes,int edge,long pixels)throws IOException{
        try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("无法识别壁纸图片");
            var reader=readers.next();try{reader.setInput(input,true,true);int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<=0||h<=0||w>edge||h>edge||(long)w*h>pixels)throw new IOException("壁纸尺寸超过限制");
                BufferedImage image=reader.read(0);if(image==null)throw new IOException("无法读取壁纸图片");return image;
            }finally{reader.dispose();}
        }
    }
    public static byte[] jpeg(BufferedImage image)throws IOException{
        var out=new ByteArrayOutputStream();if(!ImageIO.write(image,"jpg",out))throw new IOException("无法编码壁纸");return out.toByteArray();
    }
    public static byte[] sanitize(byte[] data)throws IOException{
        if(data.length<4||data.length>WallpaperRules.MAX_BYTES||(data[0]&255)!=255||(data[1]&255)!=216)throw new IOException("壁纸格式无效");
        BufferedImage image=decode(data,WallpaperRules.MAX_EDGE,WallpaperRules.MAX_PIXELS);
        try{byte[] clean=jpeg(image);if(clean.length>WallpaperRules.MAX_BYTES)throw new IOException("壁纸过大");return clean;}finally{image.flush();}
    }
    private WallpaperImage(){}
}
