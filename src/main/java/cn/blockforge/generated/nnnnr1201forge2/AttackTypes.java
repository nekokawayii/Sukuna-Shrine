package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;

/** 魔虚罗的"攻击类型"语言：12 类，法轮与适应都按这个维度计算（不是按技能）。
 *
 *  <p>模组自己的术式用 {@link #mark(String)} 在施术前打标记；标记是**一次伤害一份**：
 *  {@code CombatSettings.hurt} 在结算时 {@link #consume()} 取走，服务端每刻开始再兜底清空，
 *  所以原版/其他模组的伤害永远不会被前一刻残留的标记污染。原版与其他模组的伤害按 DamageSource 归类。</p> */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="sukuna")
public final class AttackTypes {
    public static final String SLASH="slash",FIRE="fire",SOUL="soul",VOID="void",ARROW="arrow",TRIDENT="trident",
        UNARMED="unarmed",THROWN="thrown",EXPLOSION="explosion",MAGIC="magic",ICE="ice",LIGHTNING="lightning";
    /** 12 个基础类型：法轮格数与免疫光环都按这个顺序。 */
    public static final String[] BASE={SLASH,FIRE,SOUL,VOID,ARROW,TRIDENT,UNARMED,THROWN,EXPLOSION,MAGIC,ICE,LIGHTNING};
    public static final String[] BASE_NAMES={"斩击","火焰","灵魂","虚空","箭矢","三叉戟","徒手","投掷","爆炸","魔法","冰","雷电"};
    public static final int[] BASE_COLORS={0xFFFFFF,0xFF5A20,0x9B6BFF,0x5A3CC8,0xC8A46A,0x4AD0E0,
        0xCFE8FF,0x9ACD32,0xFFB020,0xE060FF,0x9BE8FF,0xFFFF66};

    // ---- 斩击细分：解 / 捌 / 解的分支 / 捌的分支 各自一条适应进度 ----
    public static final String DISMANTLE="dismantle",CLEAVE="cleave",
        NET_BRANCH="net_branch",SHAPE_BRANCH="shape_branch",WORLD_SLASH="world_slash",FINALE="finale";
    // ---- 魔法细分：苍 / 赫 / 茈 各自一条适应进度 ----
    public static final String MAGIC_BLUE="magic_blue",MAGIC_RED="magic_red",MAGIC_PURPLE="magic_purple",
        MAGIC_FURNACE="magic_furnace",MAGIC_SOUL="magic_soul",MAGIC_TELEPORT="magic_teleport";
    public static final String BLOOD="blood",TEN_SHADOWS="ten_shadows";
    /** 新类型追加在末尾，保留旧存档24个槽位的顺序。 */
    public static final String[] EXTRA={DISMANTLE,CLEAVE,NET_BRANCH,SHAPE_BRANCH,WORLD_SLASH,FINALE,
        MAGIC_BLUE,MAGIC_RED,MAGIC_PURPLE,MAGIC_FURNACE,MAGIC_SOUL,MAGIC_TELEPORT,BLOOD,TEN_SHADOWS};
    public static final String[] EXTRA_NAMES={"解","捌","解·网分支","解·形状分支","世界斩","终式",
        "苍","赫","茈","灶火","灵魂术式","瞬移","血液","十影"};
    public static final int[] EXTRA_COLORS={0xFFFFFF,0xFFE8E8,0xE8D8FF,0xB8E8FF,0xFFF0A0,0xFFD24A,
        0x4AA8FF,0xFF4A2A,0xC08CFF,0xFF7A20,0x9B6BFF,0x9FE8FF,0xDD123F,0x5D526D};
    /** 细分占位 → 所属基础类型（适应进度按细分算，免疫按基础类型算）。 */
    public static final String[] EXTRA_FAMILY={SLASH,SLASH,SLASH,SLASH,SLASH,SLASH,
        MAGIC,MAGIC,MAGIC,MAGIC,MAGIC,MAGIC,BLOOD,TEN_SHADOWS};
    /** 全部适应占位：基础 12 + 细分 12 = 24。 */
    public static final String[] ALL;
    public static final String[] NAMES;
    public static final int[] COLORS;
    /** 每个占位所属的基础类型（基础占位就是自己）。 */
    public static final String[] FAMILY;
    static{
        ALL=new String[BASE.length+EXTRA.length];
        NAMES=new String[ALL.length];
        COLORS=new int[ALL.length];
        FAMILY=new String[ALL.length];
        System.arraycopy(BASE,0,ALL,0,BASE.length);
        System.arraycopy(BASE_NAMES,0,NAMES,0,BASE.length);
        System.arraycopy(BASE_COLORS,0,COLORS,0,BASE.length);
        System.arraycopy(BASE,0,FAMILY,0,BASE.length);
        System.arraycopy(EXTRA,0,ALL,BASE.length,EXTRA.length);
        System.arraycopy(EXTRA_NAMES,0,NAMES,BASE.length,EXTRA.length);
        System.arraycopy(EXTRA_COLORS,0,COLORS,BASE.length,EXTRA.length);
        System.arraycopy(EXTRA_FAMILY,0,FAMILY,BASE.length,EXTRA.length);
    }
    /** 占位总数（24）：每条细分独立计数、独立免疫。 */
    public static final int SLOTS=ALL.length;
    /** 占位所属的基础类型名（只用于外观/文案区分，不参与免疫判定）。 */
    public static String family(int index){return index>=0&&index<FAMILY.length?FAMILY[index]:"未知";}
    public static int index(String type){for(int i=0;i<ALL.length;i++)if(ALL[i].equals(type))return i;return -1;}
    public static String name(int index){return index>=0&&index<NAMES.length?NAMES[index]:"未知";}
    public static int color(int index){return index>=0&&index<COLORS.length?COLORS[index]:0xFFFFFF;}
    public static String name(String type){return name(index(type));}

    private static String hint;
    /** 当前施法者的"这一招属于哪个细分"：施法开始时由 {@link #prepare} 写入，伤害落点时取用。
     *  与 {@link #hint} 的区别是它跨刻有效——茈要 5 秒后才命中，靠 hint 是来不及的。 */
    private static String pending;
    /** 模组术式施法前调用；每次伤害结算后立刻消费掉，服务端每刻开始再兜底清一次。 */
    public static void mark(String type){hint=type;}
    /** 一次施法的开始：记下这一招的细分类型（解/捌/苍/赫/茈…），供延迟命中的伤害取用。 */
    public static void prepare(String type){pending=type;hint=type;}
    /** 施法者离线/死亡时清掉，避免残留到下一次无关的伤害上。 */
    public static void clearPending(){pending=null;}
    /** 只有本刻还没被标记时才写入默认值：让"虚空裂隙""灶火"这类调用点能覆盖通用斩击默认。 */
    public static void markDefault(String type){if(hint==null)hint=type;}
    /** 伤害落点用的默认类型：优先用这一招的细分（{@link #prepare}），没有就用传入的基础类型。 */
    public static void markPendingDefault(String fallback){if(hint==null)hint=pending!=null?pending:fallback;}
    public static String pending(){return pending;}
    /** 技能号 → 适应细分：给 {@code TechniqueInput.castRaw} 兜底用（各技能内部更精确的 prepare 优先）。 */
    public static String pendingFor(int skill){
        if(BloodRules.skill(skill))return BLOOD;
        if(skill>=34)return switch(skill-34){
            case 0,8->MAGIC_BLUE;      // 苍 / 苍·最大功率
            case 1,2->MAGIC_RED;       // 赫 / 赫·最大功率
            case 3,4->MAGIC_PURPLE;    // 茈 / 无限制茈
            case 5->MAGIC_TELEPORT;    // 瞬移
            case 6->UNARMED;           // 体术连击
            case 7->MAGIC;             // 无量空处
            default->MAGIC;
        };
        return switch(skill){
            case 0->DISMANTLE;                     // 解
            case 2->CLEAVE;                        // 捌
            case 4->MAGIC_FURNACE;                 // 灶·开
            case 6->MAGIC_SOUL;                    // 灵魂剥离
            case 5->MAGIC;                         // 伏魔御厨子
            case 17,18->NET_BRANCH;                // 十字斩 / 剑气
            case 19,20,21->NET_BRANCH;             // 捌的分支：蜘蛛网 / 多重斩 / 墙
            case 22,23->ICE;                       // 冰凝咒法
            case 24,25,26,27,28,29->SHAPE_BRANCH;  // 解的形状分支
            case 30->MAGIC_FURNACE;                // 灶·开（虚空版）
            case 31->MAGIC;                        // 龙之昴星
            case 32->CLEAVE;                       // 捌·滞留
            case 33->FINALE;                       // 终式
            default->SLASH;
        };
    }
    public static boolean marked(){return hint!=null;}
    public static void clear(){hint=null;}
    public static String hint(){return hint;}
    /** 只看不取：目标挨打时先按施法者刚打的标记分类，再由伤害口取走。 */
    public static String peek(){return hint;}
    /** 取走本刻的标记（取走即失效）。一次伤害只允许影响一个目标：
     *  以前标记会活到下一刻，导致"打怪 A 的一刀"把同一刻打怪 B 的原版伤害也算成斩击。 */
    public static String consume(){
        String type=hint;hint=null;return type;
    }
    /** 每刻开始清空：即使没有任何魔虚罗在场（旧实现把清空挂在魔虚罗的 tick 事件里）也不会残留。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void serverTick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase==net.minecraftforge.event.TickEvent.Phase.START)clear();
    }

    /** 归类一次伤害：先看术式标记，再按 DamageSource。 */
    public static String of(DamageSource source){
        String marked=peek();
        if(marked!=null)return marked;
        return bySource(source);
    }
    /** 只看 DamageSource 的归类（原版/其他模组的伤害都走这里）。 */
    public static String bySource(DamageSource source){
        if(source==null)return UNARMED;
        if(source.is(DeathStar.DAMAGE)||source.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new net.minecraft.resources.ResourceLocation("sukuna","blood")))||source.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new net.minecraft.resources.ResourceLocation("sukuna","blood_true"))))return BLOOD;
        if(source.getEntity() instanceof ShadowEntity||source.getDirectEntity() instanceof ShadowEntity||source.getEntity() instanceof MahoragaEntity||source.getDirectEntity() instanceof MahoragaEntity)return TEN_SHADOWS;
        if(source.is(DamageTypeTags.IS_EXPLOSION))return EXPLOSION;
        if(source.is(DamageTypeTags.IS_LIGHTNING))return LIGHTNING;
        if(source.is(DamageTypeTags.IS_FIRE))return FIRE;
        if(source.is(DamageTypeTags.IS_PROJECTILE)){
            Entity direct=source.getDirectEntity();
            if(direct instanceof ThrownTrident)return TRIDENT;
            if(direct instanceof AbstractArrow)return ARROW;
            return THROWN; // 雪球、鸡蛋、末影珍珠等
        }
        if(source.is(net.minecraft.world.damagesource.DamageTypes.THROWN))return THROWN;
        if(source.is(DamageTypeTags.WITCH_RESISTANT_TO)||source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
            ||source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC))return MAGIC;
        if(source.is(net.minecraft.world.damagesource.DamageTypes.ARROW)||source.is(net.minecraft.world.damagesource.DamageTypes.MOB_PROJECTILE))return ARROW;
        if(source.is(net.minecraft.world.damagesource.DamageTypes.TRIDENT))return TRIDENT;
        Entity attacker=source.getEntity();
        if(attacker instanceof LivingEntity living){
            var weapon=living.getMainHandItem().getItem();
            // 表里把剑与斧也算进斩击；空手/体术算徒手。
            if(weapon instanceof SwordItem||weapon instanceof AxeItem)return SLASH;
            if(living.isUsingItem()&&living.getUseItem().getItem() instanceof SwordItem)return SLASH;
            return UNARMED;
        }
        return UNARMED;
    }
    private AttackTypes(){}
}
