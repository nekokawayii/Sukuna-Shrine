package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.client.Minecraft;

/** 每个表现对象开始绘制时绑定其服务端快照；不使用旁观者的阶段或图片。 */
public final class SlashAppearance {
    private static UUID source;
    private static boolean desktop;
    public static void use(Network.Effect effect){source=effect.wallpaperOwner();desktop=effect.desktopSlashes();}
    public static void preview(){
        var player=Minecraft.getInstance().player;
        source=player==null?null:player.getUUID();
        desktop=WallpaperPolicy.allSlashes(Client.progress.voidShrine(),Client.progress.pureLandVictory());
    }
    public static void reset(){source=null;desktop=false;}
    public static UUID source(){return source;}
    public static boolean desktop(){return desktop;}
    private SlashAppearance(){}
}
