package cn.blockforge.generated.nnnnr1201forge2;

/** 玩家、女仆及回归测试共用的数值边界。 */
public final class ShrineRules {
    public static final int ACTION_INTERVAL=20,MAID_DOMAIN_TICKS=200,PLAYER_DOMAIN_TICKS=1200;
    public static final int VOLUNTARY_BURNOUT_TICKS=900,FORCED_BURNOUT_TICKS=1200,REPAIR_BUFF_TICKS=100;
    public static final int RESIDUE_DROP_DENOMINATOR=1000,RESIDUE_MAX_ITEMS=34;
    public static final int MAID_DOMAIN_COOLDOWN_TICKS=2400;
    /** 御厨子（技能5）重做后的时间轴：按下起 {@link #DOMAIN_FREEZE_TICKS} 刻不能移动，{@link #DOMAIN_OPEN_TICKS} 刻领域出现。 */
    public static final int DOMAIN_FREEZE_TICKS=60,DOMAIN_OPEN_TICKS=80;
    /** 无量空处专用：**按下释放键立刻播 gojo_domain_start（1.445 秒 ≈ 29 刻）**，放完这一刻领域出现。 */
    public static final int VOID_OPEN_TICKS=30;
    /** 起手音效长度（刻）：mid 紧随其后无缝衔接。 */
    public static final int VOID_START_SOUND_TICKS=29;
    /** 领域自己的冷却：只在领域结束时使用（不改全局熔断，否则等于禁用所有术式）。 */
    public static final int DOMAIN_COOLDOWN_TICKS=600;
    /** 结印"放下"后御厨子的伤害倍率，以及跳神龛的站立高度与飞行刻数、三击空格的最大间隔。 */
    public static final float DOMAIN_RELEASE_SCALE=0.5f;
    public static final double SHRINE_STAND_Y=4.0;
    /** 跳神龛的飞行刻数、以及"三击空格"三击之间的总窗口（刻）。 */
    public static final int SHRINE_LEAP_TICKS=16,TRIPLE_JUMP_TICKS=12;
    public static boolean recovering(boolean recovering,double energy,double maximum){return recovering&&energy<=maximum*0.25;}
    public static boolean needsOrb(double energy,double maximum){return maximum>0&&energy<maximum*0.5;}
    public static double skillWeight(int cost){return 1.0/Math.sqrt(Math.max(1,cost));}
    public static int burnoutTicks(boolean voluntary){return voluntary?VOLUNTARY_BURNOUT_TICKS:FORCED_BURNOUT_TICKS;}
    public static int domainTicks(boolean maid){return maid?MAID_DOMAIN_TICKS:PLAYER_DOMAIN_TICKS;}
    public static double repairCost(double maximum){return Math.max(0,maximum)*0.35;}
    public static int residuePercent(int items){return Math.max(0,Math.min(RESIDUE_MAX_ITEMS,items))*3;}
    public static boolean needsReverse(float health,float maximum){return maximum>0&&(double)health*5<(double)maximum*3;}
    public static boolean allows(int stage,boolean nine,int residue,boolean soul,boolean voidShrine,boolean legacy){return switch(stage){
        case 0->nine&&!soul&&residuePercent(residue)<100;
        case 1->nine&&residuePercent(residue)>=100&&!soul;
        case 2->soul&&!voidShrine;
        case 3->voidShrine&&!legacy;
        default->false;
    };}
    private ShrineRules(){}
}
