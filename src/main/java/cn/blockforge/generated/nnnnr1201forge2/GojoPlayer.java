package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** 六眼：吃下后获得五条悟技能组，屏幕最右侧出现独立小轮盘（与里梅同款）。
 *  玩家直接右键即可；女仆由主人潜行右键喂下，之后在女仆任务里选「六眼」。**没有任何阶段要求。** */
public final class GojoPlayer {
    /** 与首领完全同款：直接引用首领技能表，顺序、名称、冷却一一对应。 */
    public static final String[] NAMES=GojoEntity.NAMES;
    public static final String[] SHORT={"苍","赫","最大","茈","无限制","瞬移","体术","无量","苍极"};
    // 无量空处原来要 1200 咒力，比宿傩自己的领域（70）高出一大截，很多存档根本付不起而表现为"按了没反应"，降到 300。
    public static final int[] COST={120,150,400,600,900,60,40,300,200};
    public static final int[] COOLDOWN={160,200,600,400,3600,100,0,1200,240};
    public static final int COUNT=9;
    /** 轮盘 7 格 → 内部技能号（苍/赫/茈/无限制茈/瞬移/体术/无量空处）；最大功率两式改为长按触发，不占格。 */
    public static final int[] WHEEL={0,1,3,4,5,6,7};
    public static final int SLOTS=WHEEL.length;
    private static final String[] DESC={"引力拉扯+每秒10伤害 · 长按为苍·最大功率：光球每秒5格跟随准星，松手停留5秒",
        "斥力球 60 伤害与超级击退 · 长按为赫·最大功率：蓄力后射出圆柱激光",
        "双手合十对撞成银河紫球 · 命中 500 伤害",
        "念完四句咒词原地引爆 48 格 · 999 伤害 · 反噬自己 40% 生命",
        "向准星方向瞬移最多 8 格，遇墙停止",
        "近距离三连拳，命中附加缓慢",
        "领域展开：范围内敌人无法行动并持续受伤 · 关闭后术式熔断（可修复）"};
    public static String desc(int slot){return slot>=0&&slot<DESC.length?DESC[slot]:"";}
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
