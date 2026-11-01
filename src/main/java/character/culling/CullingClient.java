package character.culling;

/** 客户端侧的规则 HUD 标志（服务端用 Effect(621) 同步；不用任何 MC 类，双端安全）。 */
public final class CullingClient {
    private static int mask;
    public static void set(int value){mask=value;}
    public static boolean hideHealth(){return (mask&CullingExtras.UI_HIDE_HEALTH)!=0;}
    public static boolean fog(){return (mask&CullingExtras.UI_FOG)!=0;}
    public static void clear(){mask=0;}
    private CullingClient(){}
}
