package cn.blockforge.generated.nnnnr1201forge2;

/** 只缩放有量纲的强度；配置和存档始终保存原值，禁止反复乘小。 */
public final class BalanceRules {
    private static volatile boolean modpackMode;
    public static void configure(boolean enabled){modpackMode=enabled;}
    public static boolean modpackMode(){return modpackMode;}
    public static double factor(){return factor(modpackMode);}
    public static double factor(boolean enabled){return enabled?1.0:0.1;}
    public static double strength(double raw){return raw*factor();}
    public static float damage(double raw){return (float)Math.max(0,strength(raw));}
    public static double raw(double effective){return effective/factor();}
    public static float storedDamage(float stored,double previousFactor){if(!Float.isFinite(stored)||stored<0)return 0;if(previousFactor!=0.1&&previousFactor!=1)previousFactor=1;return damage(stored/previousFactor);}
    public static double health(double raw){return Math.max(1,strength(Math.max(1,Math.min(1000000,raw))));}
    public static double healthAfterResize(double health,double oldMax,double newMax){return oldMax<=0?newMax:Math.max(0,Math.min(newMax,health/oldMax*newMax));}
    /** 黑闪先按原值完成乘方，最后缩放伤害；其余伤害参数也在命中口统一处理。 */
    public static double guardianAttribute(String key,double raw){return switch(key){case "Health","Armor","HitCap","HealPerSecond"->strength(raw);default->raw;};}
    public static String number(double value){return java.math.BigDecimal.valueOf(value).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    private BalanceRules(){}
}
