package cn.blockforge.generated.nnnnr1201forge2;

/** 领域对拼的纯数值规则；不受整合包强度缩放影响。 */
public final class DomainClashRules {
    public static final int READY_TICKS=40, LIMIT_TICKS=1200, BURNOUT_TICKS=2400,
        STUN_TICKS=400, EXTEND_TICKS=400, WIN_LEAD=25, MAX_CPS=20;
    public static final double HEAL_FRACTION=0.55;
    public static int percent(int lead){return Math.max(0,Math.min(100,50+lead));}
    public static int winner(int lead){return lead>=WIN_LEAD?1:lead<=-WIN_LEAD?-1:0;}
    /** 净土守护者 5 次/秒；五条悟 3 次/秒；其余首领 1～7 次/秒。 */
    public static int bossCps(boolean guardian,boolean gojo,int roll){return guardian?5:gojo?3:1+Math.floorMod(roll,7);}
    public static double healCost(double maximum){return Math.max(0,maximum)*HEAL_FRACTION;}
    public static boolean overlaps(double squaredDistance,double a,double b){return squaredDistance<=(a+b)*(a+b);}
    private DomainClashRules(){}
}
