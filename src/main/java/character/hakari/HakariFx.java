package character.hakari;

/** 秤金次的特效号段（540-549，客户端 HakariClient 分派）。 */
public final class HakariFx {
    /** 老虎机滚动：size=luck，life=持续刻数。 */
    public static final int SLOT = 540;
    /** 中奖：金色/绿色爆发，life=中奖持续刻数。 */
    public static final int JACKPOT = 541;
    /** 中奖结束。 */
    public static final int JACKPOT_END = 542;
    /** 车门砸击：position=命中点。 */
    public static final int DOOR = 543;
    /** 钢珠散射：direction=方向。 */
    public static final int SPARK = 544;
    /** 纯爱列车：direction=前进方向，size=长度。 */
    public static final int TRAIN = 545;
    /** 格挡火花。 */
    public static final int BLOCK = 546;
    public static final int FIRST = SLOT, LAST = 549;

    private HakariFx() {}
}
