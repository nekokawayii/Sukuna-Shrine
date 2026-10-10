package cn.blockforge.generated.nnnnr1201forge2;

/** 世界斩额外百分比不再乘成长或旧版统一削弱，解项沿用一道解的最终数值。 */
public final class WorldSlashRules {
    public static final double WIDTH=3,HALF_WIDTH=WIDTH/2;
    public static float damage(double maximumHealth,double dismantle){return (float)(Math.max(0,maximumHealth)*.20+Math.max(0,dismantle)*5);}
    private WorldSlashRules(){}
}
