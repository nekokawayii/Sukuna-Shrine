package cn.blockforge.generated.nnnnr1201forge2;

/** 自疗随永久生命成长增加；每恢复1点基础生命需要30咒力。 */
public final class ReverseRules {
    public static final int COOLDOWN_TICKS=20;
    public static double healing(double permanentHealthBonus){return 10+Math.max(0,permanentHealthBonus)*0.5;}
    public static double cost(double permanentHealthBonus){return Math.ceil(healing(permanentHealthBonus)*30);}
    private ReverseRules(){}
}
