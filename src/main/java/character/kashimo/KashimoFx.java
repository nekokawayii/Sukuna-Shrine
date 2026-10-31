package character.kashimo;

/** 鹿紫云一的特效号（服务端发 Network.Effect，客户端 KashimoVisuals 绘制）。 */
public final class KashimoFx {
    /** 正电荷：owner=目标实体 id，size=层数，life=剩余刻。 */
    public static final int POS_CHARGE = 350;
    /** 落雷：position=落点，size=雷柱粗细，life=焦痕持续时间（刻）。 */
    public static final int STRIKE = 351;
    /** 负电荷：owner=玩家实体 id，size=层数。 */
    public static final int NEG_CHARGE = 352;
    /** 雷走：position=起点，direction=终点。 */
    public static final int DASH = 353;
    /** 放电：position=中心，size=半径。 */
    public static final int DISCHARGE = 354;
    /** 电磁吸附：position=起点，direction=终点。 */
    public static final int ATTRACT = 355;
    /** 雷神·附着开/关：owner=玩家 id，size=1 开 / 0 关，life=剩余刻。 */
    public static final int ATTACH = 356;
    /** 幻兽·琥珀开/关：owner=玩家 id，size=1 开 / 0 关，life=剩余刻。 */
    public static final int ULT = 357;
    /** 肉体崩溃开/关：owner=玩家 id，size=1 开 / 0 关，life=剩余刻。 */
    public static final int CRASH = 358;
    public static final int FIRST = POS_CHARGE, LAST = CRASH;
    private KashimoFx() {}
}
