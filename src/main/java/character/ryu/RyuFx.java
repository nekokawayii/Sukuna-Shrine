package character.ryu;

/** 石流龙的特效号（服务端发 Network.Effect，客户端 RyuVisuals 绘制）。 */
public final class RyuFx {
    /** 冰沙冲击波：position=起点，direction=终点，size=光束宽度。 */
    public static final int BEAM = 390;
    /** 蓄力提示：owner=玩家 id，size=蓄力进度 0~1，life=剩余刻。 */
    public static final int CHARGE = 391;
    /** 追踪弹：position=当前位置，direction=朝向，size=1。 */
    public static final int MISSILE = 392;
    /** 咒力喷涌：position=玩家，size=半径。 */
    public static final int BURST = 393;
    /** 甜品时间：owner=玩家 id，size=1 开始 / 0 结束，life=刻。 */
    public static final int DESSERT = 394;
    /** 领域（占位）：position=玩家，size=半径。 */
    public static final int DOMAIN = 395;
    /** 烟花火箭上升：position=起点，direction=(0,上升高度,0)。 */
    public static final int FIREWORK_RISE = 396;
    /** 烟花炸开：position=炸点，size=颜色索引。 */
    public static final int FIREWORK_BURST = 397;
    /** 烟花弹幕：position=当前位置，direction=朝向，size=颜色索引。 */
    public static final int FIREWORK_SHELL = 398;
    public static final int FIRST = BEAM, LAST = FIREWORK_SHELL;
    private RyuFx() {}
}
