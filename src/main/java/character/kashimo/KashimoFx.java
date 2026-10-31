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
    /** 磁化（3 层满电）：owner=玩家 id，size=1 开 / 0 关，life=剩余刻。 */
    public static final int MAGNET = 359;
    /** 静电立场：position=中心，size=半径，life=剩余刻。 */
    public static final int FIELD = 360;
    /** 同极相斥：position=玩家，size=半径，life=刻。 */
    public static final int GUARD = 361;
    /** 雷光连打上挑：position=落点，life=刻。 */
    public static final int UPPERCUT = 362;
    /** 电磁吸附·极：owner=被甩的实体 id，position=当前位置，life=剩余刻。 */
    public static final int ENVELOPE = 363;
    /** 落雷预警：position=目标点，size=半径，life=延迟刻。 */
    public static final int BOLT_WARN = 364;
    /** 球形闪电本体：position=当前位置，size=半径，life=剩余刻。 */
    public static final int ORB = 365;
    /** 球形闪电爆炸：position=中心，size=半径，life=刻。 */
    public static final int ORB_BLAST = 366;
    /** 雷霆万钧蓄力：owner=玩家 id，life=蓄力刻。 */
    public static final int CHAIN_DARK = 367;
    /** 连锁闪电的一段：position=起点，direction=终点。 */
    public static final int CHAIN_LINK = 368;
    /** 雷影化折返：position=起点，direction=终点。 */
    public static final int SHADOW = 369;
    /** 闪电反射：position=玩家，size=半径，life=刻。 */
    public static final int REFLECT = 370;
    public static final int FIRST = POS_CHARGE, LAST = REFLECT;
    private KashimoFx() {}
}
