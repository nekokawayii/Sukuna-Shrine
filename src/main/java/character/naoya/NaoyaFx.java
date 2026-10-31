package character.naoya;

/** 直哉投射咒法的特效号段（520-529，客户端 NaoyaVisuals 分派）。 */
public final class NaoyaFx {
    /** 开始设定投影：owner=玩家，life=持续刻数（客户端据此保留"投影模式"标记）。 */
    public static final int PROJECT = 520;
    /** 残影：owner=玩家，position=起始位置，direction=准星方向，life=持续刻数。 */
    public static final int GHOST = 521;
    /** 投影节点：position=节点位置。 */
    public static final int NODE = 522;
    /** 帧线：position=起点，direction=位移（终点-起点）。 */
    public static final int TRAIL = 523;
    /** 命中闪光：owner=目标实体 id。 */
    public static final int HIT = 524;
    /** 释放：在节点间穿梭完成。 */
    public static final int RELEASE = 525;
    /** 截断弹幕。 */
    public static final int INTERCEPT = 526;
    /** 被压成二次元平面：owner=目标 id，position=目标位置，size=身高，life=定身刻数。 */
    public static final int PLANE = 528;
    /** 平面破碎：一次高额伤害的玻璃炸裂。 */
    public static final int SHATTER = 529;
    /** 领域占位起手。 */
    public static final int DOMAIN = 527;
    public static final int FIRST = PROJECT, LAST = 529;

    private NaoyaFx() {}
}
