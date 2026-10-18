package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** 六眼：吃下后获得五条悟技能组，屏幕最右侧出现独立小轮盘（与里梅同款）。
 *  玩家直接右键即可；女仆由主人潜行右键喂下，之后在女仆任务里选「六眼」。**没有任何阶段要求。** */
public final class GojoPlayer {
    /** 与首领完全同款：直接引用首领技能表，顺序、名称、冷却一一对应。 */
    public static final String[] NAMES=GojoEntity.NAMES;
    /** 轮盘短名：**直接按技能号**取（苍/赫/最大/茈/咏唱/瞬移/体术/无量/苍极）。 */
    public static final String[] SHORT={"苍","赫","最大","茈","咏唱","瞬移","体术","无量","苍极"};
    // 无量空处原来要 1200 咒力，比宿傩自己的领域（70）高出一大截，很多存档根本付不起而表现为"按了没反应"，降到 300。
    public static final int[] COST={120,150,400,600,900,60,40,300,200};
    // 技能 4 = 咏唱·茈（原无限制茈）：蓄力 3 秒、200% 大小/伤害、冷却 **300 秒 = 6000 刻**。
    public static final int[] COOLDOWN={160,200,600,400,6000,100,0,1200,240};
    public static final int COUNT=9;
    /** 轮盘 7 格的显示名：**按下标与 WHEEL 一一对应**，不经过 shortName 的 switch，杜绝任何错位。 */
    public static final String[] WHEEL_LABELS={"苍","赫","茈","咏唱","瞬移","体术","无量"};
    /** 轮盘 7 格 → 技能号。**只放常规技能**：赫·最大功率（2）与苍·最大功率（8）是长按触发的，
     *  不占格；之前把它们当独立格摆出来，就是你看到的"废弃的苍极与最又出现了"。 */
    public static final int[] WHEEL={0,1,3,4,5,6,7};
    public static final int SLOTS=WHEEL.length;
    /** 技能号 → 轮盘短名（41/42 是长按专属，轮盘不显示，但名字仍要正确）。 */
    public static String shortName(int skill){
        return switch(skill){
            case 2->SHORT[2];   // 赫·最大功率
            case 7->SHORT[7];   // 无量空处
            case 8->SHORT[8];   // 苍·最大功率
            default->skill>=0&&skill<7?SHORT[skill]:"术式";
        };
    }
    /** 玩家侧显示名：技能 4 在轮盘里叫「咏唱·茈」（首领那边仍是原来的「无限制茈」，行为没变）。 */
    public static String nameOf(int skill){if(skill==4)return "咏唱·茈";return skill>=0&&skill<NAMES.length?NAMES[skill]:"五条悟";}
    /** 释放键选的这个技能号是不是「无量空处」。
     *  <b>输入侧的技能号一律是「34 + 轮盘格」</b>（{@link SkillWheel} 用 {@code 34+gojoSector}，服务端再用
     *  {@code WHEEL[skill-34]} 换成内部技能号）。无量空处 = 内部 7 = 轮盘第 6 格（{@link #WHEEL} 去掉了不占格
     *  的两式最大功率），所以客户端实际发来的是 <b>40</b>，不是 41。41 = 「34 + 内部技能号」的历史写法，
     *  这里一并认下，免得旧路径再踩同一个坑（曾导致"第二次按下既放不下也收不起"）。 */
    public static boolean voidKey(int skill){
        if(skill==41)return true;
        return skill>=34&&skill<34+SLOTS&&WHEEL[skill-34]==7;
    }
    public static int costOf(int skill){return skill>=0&&skill<COST.length?COST[skill]:0;}
    private static final String[] DESC={"引力拉扯+每秒10伤害 · 光球自带引力视界（黑洞同款扭曲）与拖尾 · 长按为苍·最大功率：光球每秒5格跟随准星，松手停留5秒",
        "斥力球 60 伤害与超级击退",
        "赫·最大功率：蓄力3秒后射出圆柱激光，沿直线贯穿并击飞（长按赫也能进）",
        "双手合十对撞成银河紫球 · 命中 500 伤害（打魔虚罗 10 倍） · 一下打不死就原地引爆：+25% 伤害并消散；一下打死则继续贯穿",
        "咏唱·茈：苍与赫旋转对撞蓄力 3 秒，打出 200% 大小 / 200% 伤害的茈（冷却 300 秒）",
        "向准星方向瞬移最多 8 格，遇墙停止",
        "近距离三连拳，命中附加缓慢",
        "领域展开：结印 4 秒后展开，范围内敌人无法行动并持续受伤 · 再按=放下(仅控制、无伤害) · 放下后再按一次=收起（关闭后术式熔断，可修复）",
        "苍·最大功率：长按引导的光球，松手后停留 5 秒（长按苍也能进）"};
    public static String desc(int skill){return skill>=0&&skill<DESC.length?DESC[skill]:"";}
    /** **轮盘格号 → 说明**：必须先用 {@link #WHEEL} 换成技能号再查 DESC。
     *  之前轮盘直接拿格号索引 DESC（格号 2 = 茈，却取到 DESC[2] 的赫·最大功率），
     *  所以悬停"茈"时显示的说明是"赫·最大功率"——这就是用户报的"普通茈在轮盘变成了赫·最大功率"。 */
    public static String descOfSlot(int slot){return slot>=0&&slot<SLOTS?desc(WHEEL[slot]):"";}
    public static boolean unlocked(Player p){return Curse.data(p).getBoolean("GojoMemory");}
    /** 吃下六眼：玩家与女仆共用同一条数据位；女仆不能走 displayClientMessage（没有连接会崩）。 */
    public static boolean consume(Player p){
        if(unlocked(p))return false;
        Curse.data(p).putBoolean("GojoMemory",true);
        if(p instanceof MaidBridge.Actor maid){
            ServerPlayer owner=MaidBridge.owner(maid);
            if(owner!=null)owner.sendSystemMessage(Component.literal("女仆已睁开六眼：请在女仆任务里选择「六眼」，她会自己放苍、赫、茈与无量空处。"));
        }else if(p instanceof ServerPlayer sp){
            sp.displayClientMessage(Component.literal("六眼与无下限的术式刻入身体：屏幕最右侧开启五条悟技能轮盘"),false);
        }
        return true;
    }
    private GojoPlayer(){}
}
