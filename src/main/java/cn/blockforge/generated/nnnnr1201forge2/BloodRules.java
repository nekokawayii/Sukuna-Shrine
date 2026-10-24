package cn.blockforge.generated.nnnnr1201forge2;

/** 赤血操术编号保持存档兼容；成长优先增加弹幕与机制。 */
public final class BloodRules {
    public static final int FIRST=60,BIND=60,RINGS=61,COMPRESS=62,PIERCE=63,FLOW=64,BLADE=65,METEOR=66,ARMOR=67,NOVA=68,CANAL=69,WINGS=70,MIST=71,THORNS=72,MIRROR=73,DOMAIN=74,DEATH_STAR=75,OCEAN=76,TRAP=77,VISION=78,SENBON=79,LAST=79;
    public static final int CHARGE_TICKS=40,MARK_TICKS=100,DOMAIN_TICKS=600,OCEAN_TICKS=400,TRAP_DWELL=40;
    public static final double DOMAIN_RADIUS=100;
    public static final String[] NAMES={"赤缚","苅祓","百敛","穿血","赤鳞跃动","血刃","血星磊","血铠","超新星","运河","翅王","血雾","血棘","血镜","血之领域·胎海","极之番·赤血操术「死胎星」","血洋","血爆","赤鳞跃动·载","血刃·千本"};
    public static final int[] COST={100,130,100,180,120,100,160,180,260,100,140,130,160,140,1200,0,900,110,80,200};
    public static final int[] COOLDOWN={100,80,40,60,10,240,80,300,200,100,300,200,100,160,1200,1200,1000,40,20,100};
    public static final String[] DESC={"前方血网定身；成长扩大覆盖，毒血目标束缚翻倍","3至9枚环刃；成长增加穿透并附流血","按住2秒蓄满；穿血或超新星翻倍，成长增加储存次数","1至4束穿血；半甲穿透，命中黑闪红圈翻倍","强化移速攻速；成长延长持续，可另选载模式","武器血刃；成长增加追击，命中回咒力","3至9枚实体感血块向前连射，命中真伤","吸收血盾免击退；成长增加碎裂反击波","360度密集血弹与冲击环；成长追加连锁爆发","准星附近追踪最多10敌人；下招在全部标记处回响","腾空滑翔并留减速血痕；成长延长滑翔与血痕宽度","持续血雾致盲与毒血；成长扩大范围并延长持续","面前120度扇形地刺；成长增加排数与密度","身前反弹敌方弹道；成长扩大镜面并延长持续","100格胎海30秒；伤害基准降低90%，随施术者生命上限提高","仅胎海内：献祭90%当前生命及全部咒力；50格爆炸，贫血60秒","血海每秒扩散5格，20秒后逐格超新星索敌；接触仅减速","向准星地面吐血水；敌人连续停留2秒触发血刺爆炸","须先开赤鳞；每秒额外15咒力，红色预测轨迹，黑闪圈延长2秒","须先开血刃；剥离附魔，前方扇射36至72针，同目标伤害递减"};
    public static boolean skill(int id){return id>=FIRST&&id<=LAST;}
    public static String name(int id){return skill(id)?NAMES[id-FIRST]:"赤血操术";}
    public static int cost(int id){return skill(id)?COST[id-FIRST]:0;}
    public static int cooldown(int id){return skill(id)?COOLDOWN[id-FIRST]:0;}
    public static int bindTicks(boolean poisoned){return poisoned?80:40;}
    public static int penetration(boolean compressed){return compressed?4:2;}
    public static float multiplier(boolean compressed,boolean weak){return (compressed?2f:1f)*(weak?2f:1f);}
    public static boolean inside(double distanceSquared){return Double.isFinite(distanceSquared)&&distanceSquared<=DOMAIN_RADIUS*DOMAIN_RADIUS;}
    public static int rank(int stage){return Math.max(0,Math.min(3,stage-7));}
    public static int rings(int rank){return 3+2*clamp(rank);}
    public static int meteors(int rank){return 3+2*clamp(rank);}
    public static int needles(int rank){return 36+12*clamp(rank);}
    public static int canalTargets(int rank){return 10;}
    public static int novaRays(int rank){return 64+32*clamp(rank);}
    public static int duration(int base,int rank){return base+clamp(rank)*40;}
    public static float damageScale(int rank){return 1+.08f*clamp(rank);}
    public static float domainBase(float maxHealth){return 3.2f*(float)Math.sqrt(Math.max(1,maxHealth/20.0));}
    public static double oceanRadius(int age){return Math.min(100,Math.max(0,age)/4.0);}
    public static float needleFalloff(int hits){return (float)Math.max(.08,Math.pow(.82,Math.max(0,hits)));}
    private static int clamp(int rank){return Math.max(0,Math.min(3,rank));}
    public static int[] pages(boolean guardian,boolean nine){return nine?(guardian?new int[]{0,1,2}:new int[]{0,2}):(guardian?new int[]{0,1}:new int[]{0});}
    public static int pageOf(int skill){return skill(skill)?2:skill>=7&&skill<17?1:0;}
    public static int count(int page){return page==2?19:0;}
    public static int global(int page,int slot){int id=FIRST+slot;return id>=DEATH_STAR?id+1:id;}
    public static int wheelSlot(double x,double y,double outer){int offset=x*x+y*y<outer*outer*.64*.64?0:8,n=offset==0?8:11;double angle=Math.atan2(y,x)+Math.PI/2+Math.PI/n;return offset+Math.floorMod((int)Math.floor(angle/(2*Math.PI)*n),n);}
    public static int markTicks(boolean vision){return BlackFlashDuration.BASE+(vision?40:0);}
    /** 保持独立规则测试不依赖客户端类。 */
    private static final class BlackFlashDuration {static final int BASE=20;}
    private BloodRules(){}
}
