package cn.blockforge.generated.nnnnr1201forge2;

/** 网分支的双方共用时间轴；一秒为20个游戏刻。 */
public final class NetBranchRules {
    public static final int CROSS_SKILL=17, QI_SKILL=18;
    public static final int CROSS_COST=650, QI_COST=300, CROSS_COOLDOWN=1000, QI_COOLDOWN=100;
    public static final int RISE_TICKS=20, WAVES=10, CORNERS=8, WAVE_INTERVAL=2, FLIGHT_TICKS=20;
    public static final int CROSS_TICKS=RISE_TICKS+(WAVES-1)*WAVE_INTERVAL+FLIGHT_TICKS;
    public static final int QI_LIFE=400;
    public static final double RISE=5, RADIUS=6, CROSS_FRACTION=1, QI_FRACTION=3, QI_SPEED=SlashFormRules.SPEED, QI_RADIUS=3;
    public static final int FX_CROSS=150, FX_QI=151, FX_HOLD=152, FX_STOP=153;
    public static boolean branch(int skill){return skill==CROSS_SKILL||skill==QI_SKILL;}
    public static int cost(int skill){return skill==CROSS_SKILL?CROSS_COST:QI_COST;}
    public static int cooldown(int skill){return skill==CROSS_SKILL?CROSS_COOLDOWN:QI_COOLDOWN;}
    public static String name(int skill){return skill==CROSS_SKILL?"十字斩":"剑气";}
    /** 上方全程能容纳目标才上浮，否则原地强控；不做部分上浮。 */
    public static double crossRise(boolean clearAbove){return clearAbove?RISE:0;}
    public static int launch(int wave){return RISE_TICKS+wave*WAVE_INTERVAL;}
    public static int arrival(int wave){return launch(wave)+FLIGHT_TICKS;}
    /** 到中心的二次加速进度；命中仍在发射后20刻。 */
    public static double flight(double age){double t=Math.max(0,Math.min(1,age/FLIGHT_TICKS));return t*t;}
    /** 穿过中心后继续飞到对角，抵达后留两刻淡出；不延长强控或追加伤害。 */
    public static final int CROSS_VISUAL_TICKS=(int)Math.ceil(FLIGHT_TICKS*Math.sqrt(2))+2;
    public static double crossTravel(double age){double t=Math.max(0,age/FLIGHT_TICKS);return Math.min(1,t*t/2);}
    /** 上下各四角，沿立方体体对角线穿过中心。 */
    public static int oppositeCorner(int corner){return corner^7;}
    public static double cornerY(int corner){return (corner&4)==0?-RADIUS:RADIUS;}
    public static double cornerX(int corner){return (corner&1)==0?-RADIUS:RADIUS;}
    public static double cornerZ(int corner){return (corner&2)==0?-RADIUS:RADIUS;}
    public static double qiSide(double angle){return Math.sin(angle)*QI_RADIUS;}
    public static double qiBend(double angle){return Math.cos(angle)*QI_RADIUS*0.55;}
    public static float domainLength(double random){return (float)(60+Math.max(0,Math.min(1,random))*60);}
    private NetBranchRules(){}
}
