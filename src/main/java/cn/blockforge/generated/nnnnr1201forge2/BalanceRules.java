package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;

/** 只缩放有量纲的强度；配置和存档始终保存原值，禁止反复乘小。 */
public final class BalanceRules {
    /** 定标原值：传承（最终阶段）一发「解」在开启模式（=配置原值）下的伤害。 */
    public static final double DISMANTLE_RAW=652.0560302734375;
    /** 定标目标：铁傀儡 100 点生命、0 点护甲（原版 1.20.1）。 */
    public static final double GOLEM_HEALTH=100;
    /** 关闭模式的定标比值 = 100 / 一发解原值：关闭模式一发解正好砍死铁傀儡。 */
    private static final double OFF=GOLEM_HEALTH/DISMANTLE_RAW;
    private static final double ON=1.0;
    /** 分类：同一分类在关闭模式共用一个比值，改一处即可覆盖同类数值。 */
    public enum Category {
        PLAYER_HEALTH("玩家生命成长"), PLAYER_ATTACK("玩家普攻成长"), DAMAGE("术式伤害"), HEALING("治疗"),
        MAID_SPELL("女仆术式"), BOSS("首领面板"), BOSS_ATTACK("首领伤害"), BOSS_ARMOR("首领护甲"), BOSS_HIT_CAP("首领单次受伤上限");
        private final String label;
        Category(String label){this.label=label;}
        public String label(){return label;}
    }
    /** 分类比值表：[分类, 开启模式, 关闭模式]。开启模式恒为 1.0，关闭模式按类定标。 */
    private static final Object[][] TABLE={
        {Category.PLAYER_HEALTH,ON,OFF},
        {Category.PLAYER_ATTACK,ON,OFF},
        {Category.DAMAGE,ON,OFF},
        {Category.HEALING,ON,OFF},
        {Category.MAID_SPELL,ON,OFF},
        {Category.BOSS,ON,OFF},
        {Category.BOSS_ATTACK,ON,OFF},
        {Category.BOSS_ARMOR,ON,OFF},
        {Category.BOSS_HIT_CAP,ON,OFF}};
    /** 关闭模式比值推导依据：每个分类一行，改标定时把这张表和 {@link #TABLE} 一起改。 */
    public static final String[] RATIONALE={
        "玩家生命成长：与术式同比值，生命/术式/首领三者比例完全沿用开启模式的固定原值",
        "玩家普攻成长：与术式同比值，摘掉附加解之后本体一刀仍是原比例的成长",
        "术式伤害：定标项——传承阶段一发解原值 "+number(DISMANTLE_RAW)+"，×该比值 = "+number(GOLEM_HEALTH)+"，正好砍死铁傀儡",
        "治疗：与生命成长同比值，回血占最大生命的比例不变（净土每秒治疗、术式反转、反转外放）",
        "女仆术式：玩家侧同类同值，保留女仆原有的整体减半",
        "首领面板：首领生命按同比值缩小，首领战时长与原比值一致，不会变成血牛",
        "首领伤害：对玩家的伤害同比值缩小，玩家的耐打程度与原比值一致",
        "首领护甲：同比值缩小，保留原比值的物理减伤",
        "首领单次受伤上限：与首领生命同比值，几刀砍死/砍不动的关系不变"};
    private static final Map<Category,Double> MODPACK=new EnumMap<>(Category.class),NORMAL=new EnumMap<>(Category.class);
    /** 表内因子全集：旧存档换算只认这些值，其余一律按原值处理。 */
    private static final Set<Double> KNOWN;
    static {
        for(Object[] row:TABLE){Category category=(Category)row[0];MODPACK.put(category,(double)row[1]);NORMAL.put(category,(double)row[2]);}
        if(MODPACK.size()!=Category.values().length||NORMAL.size()!=Category.values().length)throw new IllegalStateException("平衡表缺分类");
        Set<Double> known=new LinkedHashSet<>();
        known.add(1.0);
        known.add(0.1); // r249 及以前：关闭模式的统一因子（旧存档里可能写着它）
        known.addAll(MODPACK.values());
        known.addAll(NORMAL.values());
        KNOWN=Collections.unmodifiableSet(known);
    }
    private static volatile boolean modpackMode;
    public static void configure(boolean enabled){modpackMode=enabled;}
    public static boolean modpackMode(){return modpackMode;}
    public static double factor(){return factor(modpackMode);}
    /** 通用因子（=分类表里的术式伤害列）；开启模式恒 1.0。 */
    public static double factor(boolean enabled){return factor(enabled,Category.DAMAGE);}
    public static double factor(boolean enabled,Category category){return category==null?1.0:(enabled?MODPACK:NORMAL).get(category);}
    /** 关闭模式各分类比值（面板/文案展示用，顺序与 {@link Category} 一致）。 */
    public static String ratios(){StringJoiner joiner=new StringJoiner(" · ");for(Category category:Category.values())joiner.add(category.label()+"×"+ratio(factor(false,category)));return joiner.toString();}
    /** 比值展示：保留 3 位小数，避免 0.1534 被显示成 0.15。 */
    public static String ratio(double factor){return java.math.BigDecimal.valueOf(factor).setScale(3,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    /** 数值的百分数（文案展示用）。 */
    public static double percent(double factor){return factor*100;}
    /** 按分类缩放原值；开启模式严格 1.0，关闭模式查表；分类为 null（表外键）时保持原值。 */
    public static double strength(double raw,Category category){return category==null?raw:raw*factor(modpackMode,category);}
    public static float damage(double raw){return damage(raw,Category.DAMAGE);}
    public static float damage(double raw,Category category){return (float)Math.max(0,strength(raw,category));}
    public static double raw(double effective,Category category){double factor=factor(modpackMode,category);return factor==0?0:effective/factor;}
    /** 旧存档换算：先按写入时的比值还原原值，再按当前分类比值缩放；比值不在表内的一律视为原值。 */
    public static float storedDamage(float stored,double previousFactor){if(!Float.isFinite(stored)||stored<0)return 0;if(!KNOWN.contains(previousFactor)||previousFactor==1.0)return damage(stored);return damage(stored/previousFactor);}
    public static double health(double raw){return Math.max(1,strength(Math.max(1,Math.min(1000000,raw)),Category.BOSS));}
    public static double healthAfterResize(double health,double oldMax,double newMax){return oldMax<=0?newMax:Math.max(0,Math.min(newMax,health/oldMax*newMax));}
    /** 黑闪先按原值完成乘方，最后缩放伤害；其余伤害参数也在命中口统一处理。表外键保持原值。 */
    public static double guardianAttribute(String key,double raw){return strength(raw,guardianCategory(key));}
    private static Category guardianCategory(String key){return switch(key){case "Health"->Category.BOSS;case "Armor"->Category.BOSS_ARMOR;case "HitCap"->Category.BOSS_HIT_CAP;case "HealPerSecond"->Category.HEALING;default->null;};}
    public static String number(double value){return java.math.BigDecimal.valueOf(value).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    private BalanceRules(){}
}
