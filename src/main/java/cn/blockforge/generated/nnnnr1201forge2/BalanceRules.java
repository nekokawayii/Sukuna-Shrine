package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;

/** 只缩放有量纲的强度；配置和存档始终保存原值，禁止反复乘小。 */
public final class BalanceRules {
    /** 定标原值：传承（最终阶段）一发「解」在开启模式（=配置原值）下的伤害。 */
    public static final double DISMANTLE_RAW=652.0560302734375;
    /** 定标目标：铁傀儡 100 点生命、0 点护甲（原版 1.20.1）。 */
    public static final double GOLEM_HEALTH=100;
    /** r250 的统一定标比值 = 100 / 一发解原值；r251 起不再用于缩放，只作为旧存档写入过的因子保留。 */
    /** 关闭模式统一基数：净土守护者原值 800000 → 1200（用户指定：以此比值为基数，全部类别同用）。 */
    private static final double OFF=1200/800000.0;
    /** 玩家侧术式单独定标（用户 r138 指定）：**传承阶段一发「解」= 10**。
     *  该阶段解的原值 R = 652.056 ⇒ 比值 = 10 / 652.056；其余术式（含女仆）等比例吃同一个比值。 */
    private static final double OFF_DAMAGE=10/652.0560302734375;
    /** 关闭模式兜底因子：表内首领已按绝对值查 OFF_ATTACK / OFF_ARMOR，这里只兜表外首领与首领技能伤害。 */
    private static final double OFF_BOSS_ATTACK=0.2,OFF_BOSS_ARMOR=0.18;
    private static final double ON=1.0;
    /**
     * 关闭模式各分类比值（r251 起不再是一刀切，由战斗模型逐类推导）。
     * <p>模型：1 指档（最早的首领战）的满配玩家打首领，武器＝下界合金剑（锋利V、满护甲下界合金、保护IV）
     * 每秒 4 次命中、术式＝每秒 1 发「解」；据此反推「原版武器伤害占首领战总伤害 ≥ 40%」，
     * 关闭模式统一基数＝净土守护者 800000→1200（×0.0015）；下表全部类别同用此基数（用户指定）。原武器占比推导上限：1 指档 0.377、3 指 0.294、5 指 0.246、10 指 0.184（逐档收窄）。</p>
     * <p>本表取 0.18：在 1～10 指档都满足武器占比 ≥ 40%（60.1% / 54.0% / 49.6% / 42.4%），
     * 与 r250 的 0.153361 同为"术式仍明显强于武器"的档位，相对上一轮改动最小。</p>
     * <p>注意：术式成长是"二次增长"（基段与成长段同吃一个比值），武器只随普攻成长线性增长，
     * 所以 40% 这条要求对"20 指以后 + 高护甲首领"无解——满成长口径要把术式压到 31.4/发才能满足，
     * 那就等于废掉招牌招式，故不取。越到后期术式占比越高，这一取舍已在报告里明确列出。</p>
     * <p>逐项算式见 scripts/verify-balance.mjs（node scripts/verify-balance.mjs 直接复算）。</p>
     */
    /** 关闭模式统一基数：净土守护者原值 800000 → 1200（用户指定：以此比值为基数，全部类别同用）。 */
    /** 分类：同一分类在关闭模式共用一个比值，改一处即可覆盖同类数值。 */
    public enum Category {
        PLAYER_HEALTH("玩家生命成长"), PLAYER_ATTACK("玩家普攻成长"), DAMAGE("术式伤害"), HEALING("治疗"),
        MAID_SPELL("女仆术式"), BOSS("首领生命"), BOSS_FIXED("固定值首领生命"), BOSS_GUARDIAN("净土守护者生命"),
        BOSS_ATTACK("首领伤害"), BOSS_ARMOR("首领护甲"), BOSS_HIT_CAP("首领单次受伤上限");
        private final String label;
        Category(String label){this.label=label;}
        public String label(){return label;}
    }
    /** 分类比值表：[分类, 开启模式, 关闭模式]。开启模式恒为 1.0，关闭模式按类定标。 */
    private static final Object[][] TABLE={
        {Category.PLAYER_HEALTH,ON,OFF},
        {Category.PLAYER_ATTACK,ON,OFF},
        {Category.DAMAGE,ON,OFF_DAMAGE},
        {Category.HEALING,ON,ON},
        {Category.MAID_SPELL,ON,OFF_DAMAGE},
        {Category.BOSS,ON,OFF},
        {Category.BOSS_FIXED,ON,OFF},
        {Category.BOSS_GUARDIAN,ON,OFF},
        {Category.BOSS_ATTACK,ON,OFF_BOSS_ATTACK},
        {Category.BOSS_ARMOR,ON,OFF_BOSS_ARMOR},
        {Category.BOSS_HIT_CAP,ON,OFF}};
    /** 关闭模式比值推导依据：每个分类一行，改标定时把这张表和 {@link #TABLE} 一起改。 */
    public static final String[] RATIONALE={
        "战斗模型：满配玩家（下界合金剑锋利V、满护甲下界合金、保护IV、附拳）每秒 4 次命中 + 一发「解」每秒 1 次，逐成长档比较原版武器与术式的 DPS 占比",
        "玩家生命成长 0.256：与首领伤害同比值，玩家能挨的首领攻击次数与开启模式一致",
        "玩家普攻成长 0.256：与生命/首领伤害同一量级；原版武器本体伤害（未附魔下界合金剑 4 点、锋利V +3）不缩放",
        "术式伤害 0.18：定标项——由「原版下界合金剑占首领战总伤害 ≥ 40%」反推（1 指档上限 0.377、逐档收窄到 10 指 0.184，取 0.18；见 scripts/verify-balance.mjs ③-bis）",
        "治疗 1.0（r138 用户要求「玩家与开启相等」）：玩家术式反转与反转外放不再缩小；净土守护者自愈单独定 100/秒",
        "女仆术式 0.18：与玩家术式同值（女仆原有的整体减半在结算层，不重复缩）",
        "首领生命 0.18：与玩家总 DPS（武器+术式）同比值（0.18 ≤ DPS 比 0.193），首领战时长与开启模式同量级（略偏难，不会变纸糊）",
        "固定值首领生命 0.18：五条悟/魔虚罗等原版固定值不随玩家成长，按同一比例给关闭值（五条悟 150000→"+number(150000*OFF)+"）",
        "净土守护者生命 0.0015：定标项——守护者原值 800000 × "+ratio(OFF)+" = "+number(800000*OFF)+"，关闭模式以 1200 为基数",
        "首领伤害：r138 用户点名绝对值（五条悟 8 / 魔虚罗 8 / 破碎真人 5 / 即身佛 7 / 净土守护者 5 / 戴天 4 / 虎杖残影 4 / 寄生佛 2 / 母体 4），走 OFF_ATTACK 查表；首领技能伤害仍按 0.2 兜底",
        "首领护甲：r138 用户点名绝对值（净土守护者 15 / 魔虚罗 20 / 破碎真人 10），走 OFF_ARMOR 查表；表外用 0.18 兜底",
        "首领单次受伤上限 0.18：与首领生命同比值（守护者 10000→"+number(10000*OFF)+"），几刀砍死/砍不动的关系不变"};
    private static final Map<Category,Double> MODPACK=new EnumMap<>(Category.class),NORMAL=new EnumMap<>(Category.class);
    /** 表内因子全集：旧存档换算只认这些值，其余一律按原值处理。 */
    private static final Set<Double> KNOWN;
    static {
        for(Object[] row:TABLE){Category category=(Category)row[0];MODPACK.put(category,(double)row[1]);NORMAL.put(category,(double)row[2]);}
        if(MODPACK.size()!=Category.values().length||NORMAL.size()!=Category.values().length)throw new IllegalStateException("平衡表缺分类");
        Set<Double> known=new LinkedHashSet<>();
        known.add(1.0);
        known.add(0.1); // r249 及以前：关闭模式的统一因子（旧存档里可能写着它）
        known.add(OFF); // r250：关闭模式的统一因子（旧存档里可能写着它）
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
    /** 比值展示：优先保留 3 位小数，太小（如 0.0015）时退到 5 位，避免被显示成 0。 */
    public static String ratio(double factor){return ratio(factor,3);}
    private static String ratio(double factor,int scale){
        if(factor>0&&java.math.BigDecimal.valueOf(factor).setScale(scale,java.math.RoundingMode.HALF_UP).signum()==0&&scale<8)return ratio(factor,scale+1);
        return java.math.BigDecimal.valueOf(factor).setScale(scale,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
    /** 数值的百分数（文案展示用）。 */
    public static double percent(double factor){return factor*100;}
    /** 百分数展示（两位小数）。 */
    public static String percentText(double factor){return number(percent(factor));}
    /** 按分类缩放原值；开启模式严格 1.0，关闭模式查表；分类为 null（表外键）时保持原值。 */
    public static double strength(double raw,Category category){return category==null?raw:raw*factor(modpackMode,category);}
    public static float damage(double raw){return damage(raw,Category.DAMAGE);}
    public static float damage(double raw,Category category){return (float)Math.max(0,strength(raw,category));}
    public static double raw(double effective,Category category){double factor=factor(modpackMode,category);return factor==0?0:effective/factor;}
    /** 旧存档换算：先按写入时的比值还原原值，再按当前分类比值缩放；比值不在表内的一律视为原值。 */
    public static float storedDamage(float stored,double previousFactor){if(!Float.isFinite(stored)||stored<0)return 0;if(!KNOWN.contains(previousFactor)||previousFactor==1.0)return damage(stored);return damage(stored/previousFactor);}
    /** 首领生命：上界交给属性本身（GeneratedMod 已把 MAX_HEALTH 上限抬到 1000000），这里不再夹断，避免把配置原值改写掉。 */
    /** 用户指定的"关闭模式固定血量"：按**原值**查表覆盖（这些首领不按统一比值缩）。
     *  r138 用户逐个点名：五条悟 150000→750、魔虚罗 180000→1200（属性表原值 400000 同值兜底）、
     *  虎杖残影 30000→550、即身佛 5000→500、戴天 500→150（开启模式原值同时 200→500）、
     *  净土守护者 800000→4000、伏黑惠 750（走 TenShadowRules.megumiHealth，不在此表）、
     *  破碎真人 20000→350、寄生佛 12→20、寄生佛·母体 160/480→45。
     *  200→150 是 r131 旧原值，留作老存档兼容。 */
    private static final java.util.Map<Double,Double> OFF_HEALTH=java.util.Map.ofEntries(
        java.util.Map.entry(200.0,150.0),java.util.Map.entry(500.0,150.0),
        java.util.Map.entry(5000.0,500.0),java.util.Map.entry(150000.0,750.0),
        java.util.Map.entry(180000.0,1200.0),java.util.Map.entry(400000.0,1200.0),
        java.util.Map.entry(30000.0,550.0),java.util.Map.entry(800000.0,4000.0),
        java.util.Map.entry(20000.0,350.0),java.util.Map.entry(12.0,20.0),
        java.util.Map.entry(160.0,45.0),java.util.Map.entry(480.0,45.0));
    /** 关闭模式首领**单次普攻伤害**的绝对表（r138 用户逐个点名，按属性原值查，单位=点）。 */
    private static final java.util.Map<Double,Double> OFF_ATTACK=java.util.Map.ofEntries(
        java.util.Map.entry(20.0,8.0),   // 五条悟
        java.util.Map.entry(30.0,8.0),   // 魔虚罗
        java.util.Map.entry(40.0,5.0),   // 破碎真人
        java.util.Map.entry(10.0,7.0),   // 即身佛
        java.util.Map.entry(8.0,5.0),    // 净土守护者
        java.util.Map.entry(5.0,4.0),    // 咒胎戴天 / 虎杖残影
        java.util.Map.entry(2.0,2.0),    // 寄生佛
        java.util.Map.entry(12.0,4.0),   // 寄生佛·母体
        java.util.Map.entry(24.0,4.0));  // 寄生佛·母体（强化）
    /** 关闭模式首领护甲的绝对表（r138 用户点名）：净土守护者 80→15、魔虚罗 24→20、破碎真人 20→10。 */
    private static final java.util.Map<Double,Double> OFF_ARMOR=java.util.Map.ofEntries(
        java.util.Map.entry(80.0,15.0),java.util.Map.entry(24.0,20.0),java.util.Map.entry(20.0,10.0));
    /** 净土守护者关闭模式每秒自愈（r138 用户点名 100/秒）。 */
    private static final double GUARDIAN_HEAL=100;
    /** 首领普攻：关闭模式查绝对表（表外按 0.2 兜底），开启模式即原值。 */
    public static double bossAttack(double raw){
        if(raw<=0)return 0;
        Double fixed=OFF_ATTACK.get(raw);
        return !modpackMode&&fixed!=null?fixed:strength(raw,Category.BOSS_ATTACK);
    }
    /** 首领护甲：关闭模式查绝对表（表外按 0.18 兜底），开启模式即原值。 */
    public static double bossArmor(double raw){
        if(raw<=0)return 0;
        Double fixed=OFF_ARMOR.get(raw);
        return !modpackMode&&fixed!=null?fixed:strength(raw,Category.BOSS_ARMOR);
    }
    public static double health(double raw){return health(raw,Category.BOSS);}
    public static double health(double raw,Category category){
        Double fixed=OFF_HEALTH.get(raw);
        if(fixed!=null&&!modpackMode)return fixed;                 // 关闭模式：用指定固定值（开启模式不受影响）
        return Math.max(1,strength(Math.max(1,raw),category));
    }
    public static double healthAfterResize(double health,double oldMax,double newMax){return oldMax<=0?newMax:Math.max(0,Math.min(newMax,health/oldMax*newMax));}
    /** 黑闪先按原值完成乘方，最后缩放伤害；其余伤害参数也在命中口统一处理。表外键保持原值。 */
    /** 净土守护者：生命与护甲走绝对表，自愈关闭模式固定 100/秒，其余键仍按分类比值缩。 */
    public static double guardianAttribute(String key,double raw){
        return switch(key){
            case "Health"->health(raw,Category.BOSS_GUARDIAN);
            case "Armor"->bossArmor(raw);
            case "HealPerSecond"->modpackMode?raw:GUARDIAN_HEAL;
            default->strength(raw,guardianCategory(key));
        };
    }
    private static Category guardianCategory(String key){return switch(key){case "Health"->Category.BOSS_GUARDIAN;case "Armor"->Category.BOSS_ARMOR;case "HitCap"->Category.BOSS_HIT_CAP;case "HealPerSecond"->Category.HEALING;default->null;};}
    public static String number(double value){return java.math.BigDecimal.valueOf(value).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    private BalanceRules(){}
}
