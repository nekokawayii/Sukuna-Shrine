package cn.blockforge.generated.nnnnr1201forge2;

/** 咒力始终使用存档原始单位，不跟随伤害强度缩放；冰鱼按已食用总数计算。 */
public final class EnergyRules {
    public static final double ICE_FISH_BONUS=100;
    public static double fishBonus(int fish){return Math.max(0,fish)*ICE_FISH_BONUS;}
    public static double maximum(double stageMaximum,int fish){return Math.max(0,stageMaximum)+fishBonus(fish);}
    public static double current(double stored,double maximum){return Double.isFinite(stored)?Math.max(0,Math.min(maximum,stored)):0;}
    public static double add(double stored,double maximum,double amount){return Double.isFinite(amount)?current(current(stored,maximum)+amount,maximum):current(stored,maximum);}
    public static boolean canSpend(double current,double cost){return Double.isFinite(cost)&&cost>=0&&current+0.0001>=cost;}
    private EnergyRules(){}
}
