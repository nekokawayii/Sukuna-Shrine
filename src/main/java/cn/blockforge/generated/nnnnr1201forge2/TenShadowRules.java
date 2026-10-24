package cn.blockforge.generated.nnnnr1201forge2;

/** 十影的纯数值规则；索引同时用于存档、网络和界面，不随显示顺序变化。 */
public final class TenShadowRules {
    public static final int BLACK=0,WHITE=1,TOTALITY=2,NUE=3,TOAD=4,SERPENT=5,ELEPHANT=6,RABBIT=7,DEER=8,OX=9,TIGER=10,COUNT=11;
    public static final int INITIAL=(1<<BLACK)|(1<<WHITE),ALL=(1<<COUNT)-1;
    public static final int BALANCE_VERSION=131,MEGUMI_KINDS=2,OX_AIM_TICKS=20,OX_CHARGE_TICKS=50,OX_RECOVERY_TICKS=50;
    public static final String[] IDS={"dog_black","dog_white","dog_totality","nue","toad","serpent","elephant","rabbit","deer","ox","tiger"};
    public static final String[] NAMES={"玉犬黑","玉犬白","玉犬浑","鵺","虾蟇","大蛇","满象","脱兔","円鹿","贯牛","虎葬"};
    public static final String[] DETAILS={
        "近身撕咬，攻击强；黑白玉犬可以同时存在",
        "高速游击；标记自身55格内所有已加载生物",
        "继承黑犬强攻与白犬游击侦察，生命攻击速度翻倍，忽视65%护甲",
        "空中盘旋、带电俯冲；高空雷击周围敌人",
        "吐舌缠绕，双方定身2秒；最多同时5只",
        "高生命、高护甲、低伤害；嘲讽周围敌人",
        "高咒力消耗；水浪击退与高压水枪交替",
        "不攻击，薄血嘲讽；每批1至30只，无总数上限",
        "原地停留；每秒自疗2点并治疗50格内主人2点；调服时8格伤害圈",
        "自动瞄准敌人，蓄力1秒后直线冲撞；伤害随距离增加，最高3倍",
        "高攻高速厚血；每秒消耗300咒力，耗尽自动收回"};
    private static final double[] HEALTH={40,30,80,60,36,160,140,2,80,100,150};
    private static final double[] DAMAGE={5,3.5,10,6,2,3,12,0,0,7,14};
    private static final double[] SPEED={.32,.36,.72,.32,.25,.20,.20,.36,0,.38,.38};
    private static final double[] ARMOR={3,2,6,3,2,14,8,0,5,8,8};
    private static final int[] DURATION={240,240,300,240,180,300,180,180,240,180,180};
    private static final int[] COST={60,60,120,180,100,250,1200,8,350,300,600};
    public static boolean valid(int kind){return kind>=0&&kind<COUNT;}
    public static boolean dog(int kind){return kind==BLACK||kind==WHITE;}
    public static boolean canSummon(int mask,int kind,int active){return valid(kind)&&(mask&(1<<kind))!=0&&active<limit(kind);}
    public static boolean megumiCanAdd(int activeMask,int kind){return valid(kind)&&((activeMask&(1<<kind))!=0||Integer.bitCount(activeMask&ALL)<MEGUMI_KINDS);}
    public static int lethalMegumiPhase(int phase){return phase<2?2:3;}
    public static boolean megumiChant(int phase,float health,float maximum){return phase==1&&maximum>0&&health<=maximum*.05f;}
    public static float megumiRecovery(float maximum){return maximum*.10f;}
    public static int chantAfter(int ticks){return Math.max(0,ticks-1);}
    public static int mask(int saved,boolean fused){return fused?((saved|INITIAL|(1<<TOTALITY))&~INITIAL):(saved|INITIAL)&~(1<<TOTALITY);}
    public static int limit(int kind){return kind==RABBIT?Integer.MAX_VALUE:kind==TOAD?5:1;}
    public static int batch(int kind,int count){return kind==RABBIT?Math.max(1,Math.min(30,count)):1;}
    public static int duration(int kind){return DURATION[kind]*20;}
    public static int cost(int kind){return COST[kind];}
    public static double growth(int stage){return 1+Math.max(0,Math.min(10,stage))*.10;}
    public static double health(int kind,int stage,boolean pack){return HEALTH[kind]*growth(stage)*(pack?30:1);}
    public static double damage(int kind,int stage,boolean pack){return DAMAGE[kind]*growth(stage)*(pack?8:1);}
    public static double speed(int kind,int stage){return SPEED[kind]*(1+Math.max(0,Math.min(10,stage))*.015);}
    public static double armor(int kind){return ARMOR[kind];}
    public static double oxMultiplier(double distance){return 1+Math.min(2,Math.max(0,distance)*.05);}
    public static boolean oxFinished(int ticks,double distance,boolean collision){return ticks<=0||distance>=40||collision;}
    public static float deerHeal(){return 2;}
    public static boolean deerAuraRange(double distanceSqr){return distanceSqr>=0&&distanceSqr<=64;}
    public static float deerAuraDamage(int stage,boolean pack){return (float)(2*growth(stage)*(pack?8:1));}
    public static double megumiHealth(boolean pack){return pack?60000:750;}
    private TenShadowRules(){}
}
