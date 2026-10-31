package character.toji;

/** 甚尔 QTE 的特效号段（480-489，客户端 TojiQteVisuals 分派）。 */
public final class TojiFx {
    /** 红光窗口：owner=玩家，position=玩家位置，size=威胁实体 id，life=窗口刻数。 */
    public static final int QTE_ON = 480;
    /** 红圈标记：owner=被标记实体 id，position=实体位置，life=持续刻数。 */
    public static final int MARK = 481;
    /** 完美闪避：direction=闪避方向。 */
    public static final int DODGE = 482;
    /** 连招命中：size=连招编号(1~20)，position=命中点。 */
    public static final int COMBO = 483;
    /** 破甲/破魔的暗红裂纹。 */
    public static final int BREAK = 484;
    public static final int FIRST = QTE_ON, LAST = 489;

    private TojiFx() {}
}
