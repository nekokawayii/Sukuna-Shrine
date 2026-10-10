package cn.blockforge.generated.nnnnr1201forge2;

/** 新分支的两端常量与可独立测试的时间规则。所有时间均为游戏刻。 */
public final class CombatExpansionRules {
    public static final int LINGER=29, PURPLE_FURNACE=30, DRAGON_STAR=31, CLEAVE_LINGER=32;
    public static final int HOLD_DELAY=6, LINGER_LIFE=300, WORLD_COOLDOWN=2400, DRAGON_BURNOUT=1500;
    public static final int STAR_GAP=10, STAR_STROKES=6, STARS=6, METEORS_PER_STAR=20;
    public static final int FALL_CUTS=5, LANDING_TARGETS=5, RAIN_CUTS=10, WEB_BLOCKS=30;
    public static final int FX_SLASH=170, FX_DRAGON=171, FX_STAR=172, FX_DUST=173, FX_PURPLE=174, FX_BLADE_STOP=175;
    public static boolean branch(int skill){return skill>=LINGER&&skill<=CLEAVE_LINGER;}
    public static boolean dismantle(int skill){return skill==0||skill==18||skill==24||skill==26||skill==27||skill==28||skill==LINGER;}
    public static boolean cleave(int skill){return skill==2||skill==19||skill==20||skill==25||skill==CLEAVE_LINGER;}
    public static boolean linger(int skill){return skill==LINGER||skill==CLEAVE_LINGER;}
    public static String name(int skill){return switch(skill){case LINGER->"解 · 滞留";case CLEAVE_LINGER->"捌 · 滞留";case PURPLE_FURNACE->"灶 · 开";case DRAGON_STAR->"龙之昴星";default->"";};}
    public static double rate(int heldTicks,int maximum){return Math.min(Math.max(1,Math.min(30,maximum)),1+Math.max(0,heldTicks-HOLD_DELAY)*29.0/100);}
    public static double slashCost(double base,int previous){return base*(1+Math.max(0,previous)*0.01);}
    public static boolean strokeExpired(long now,long last){return now-last>STAR_GAP;}
    public static boolean webBlock(double squaredDistance){return squaredDistance>9&&squaredDistance<49;}
    public static int huntingCuts(double random){return 5+Math.min(10,(int)(Math.max(0,Math.min(1,random))*11));}
    private CombatExpansionRules(){}
}
