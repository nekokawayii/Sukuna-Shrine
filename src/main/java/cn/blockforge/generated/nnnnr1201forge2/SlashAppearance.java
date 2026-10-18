package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.client.Minecraft;

/** 每个表现对象开始绘制时绑定其服务端快照；不使用旁观者的阶段或图片。 */
public final class SlashAppearance {
    private static UUID source;
    private static boolean desktop;
    /** 当前斩击的睁眼进度：1 = 已完全睁开（没有时间轴的一次性斩击默认全开）。 */
    private static float open=1f;
    public static void use(Network.Effect effect){source=effect.wallpaperOwner();desktop=effect.desktopSlashes();open=1f;}
    /** 绑定该斩击已存在的刻数：只在"全部斩击露壁纸"的阶段生效，其余阶段保持原样。 */
    public static void age(double ticks){open=desktop?SlashEye.open(ticks):1f;}
    public static void preview(){
        var player=Minecraft.getInstance().player;
        source=player==null?null:player.getUUID();
        desktop=WallpaperPolicy.allSlashes(Client.progress.voidShrine(),Client.progress.pureLandVictory());
        open=1f;
    }
    public static void reset(){source=null;desktop=false;open=1f;}
    public static UUID source(){return source;}
    public static boolean desktop(){return desktop;}
    public static float open(){return open;}
    private SlashAppearance(){}
}
