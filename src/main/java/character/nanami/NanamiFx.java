package character.nanami;

/** 七海建人的特效号（服务端发 Network.Effect，客户端 RatioVisuals 绘制）。 */
public final class NanamiFx {
    /** 7:3 数轴：owner=目标实体 id，size=1 开 / 0 关，life=剩余刻，seed=标记位（1 眩晕放大 / 2 加班放大）。 */
    public static final int AXIS = 380;
    /** 强打：position=起点，direction=朝向，size=半径。 */
    public static final int SMASH = 381;
    /** 隙：position=起点，direction=终点。 */
    public static final int GAP = 382;
    /** 瓦落瓦落：position=玩家，size=半径，life=守护刻；size<0 表示反击那一下。 */
    public static final int CLOCK = 383;
    /** 弱点暴击：position=命中点，size=强度。 */
    public static final int CRIT = 384;
    /** 加班：owner=玩家 id，size=1 开 / 0 关，life=剩余刻。 */
    public static final int OVERTIME = 385;
    public static final int FIRST = AXIS, LAST = OVERTIME;
    private NanamiFx() {}
}
