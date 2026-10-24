package cn.blockforge.generated.nnnnr1201forge2;

/** 权限数值集中定义，供无游戏环境的回归检查使用。 */
public final class AuthorityRules {
    public static final double ENERGY_MULTIPLIER=20,LEGION_DAMAGE=.70,EDITOR_DAMAGE=10;
    public static final int FOLLOW_RADIUS=35,MAX_TARGETS=32,MAX_MEMBERS=128,EDITOR_INTERVAL=10;
    public static boolean canMark(boolean unlocked,boolean markerInUsedHand){return unlocked&&markerInUsedHand;}
    public static double maximum(double base,boolean unlocked){return Double.isFinite(base)?Math.max(0,base)*(unlocked?ENERGY_MULTIPLIER:1):0;}
    public static double spellScale(boolean legion,boolean editor){return (legion?LEGION_DAMAGE:1)*(editor?EDITOR_DAMAGE:1);}
    public static boolean shouldRecall(double distanceSquared,boolean hurt){return hurt||distanceSquared>FOLLOW_RADIUS*FOLLOW_RADIUS;}
    public static boolean cursor(float x,float y){return Float.isFinite(x)&&Float.isFinite(y)&&x>=-8&&x<=8&&y>=-8&&y<=8;}
    private AuthorityRules(){}
}
