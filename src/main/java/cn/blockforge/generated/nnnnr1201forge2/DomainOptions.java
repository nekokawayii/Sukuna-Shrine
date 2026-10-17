package cn.blockforge.generated.nnnnr1201forge2;

/** 伏魔御厨子（技能5）展开前的点数分配表：固定若干点在「领域半径 / 伤害倍率 / 斩击速度」三项之间分配。
 *
 *  <p>三项每点收益**线性相同**，所以"降低一项＝退还该点数"与"按最终收益的减少退点"是同一件事。
 *  这份换算给服务端与客户端共用：服务端在 {@link ShrineRules#DOMAIN_OPEN_TICKS} 刻真正展开时读"那一刻已保存的点数"，
 *  客户端界面 {@link DomainOptionsScreen} 用它显示数值。平衡值只改这一个文件。</p>
 *
 *  <p>默认分配 {@link #DEFAULT_RADIUS}/{@link #DEFAULT_DAMAGE}/{@link #DEFAULT_SPEED} 换算出来就是本功能加入前的现状：
 *  <b>半径 200 格 / 伤害 ×1.00 / 斩击 ×1.00</b>（基准值与每点增量都做成"默认点正好落在现状上"）。</p> */
public final class DomainOptions {
    /** 开着「纵向伤敌」时的可用总点数（三项之和不得超过它）。 */
    public static final int TOTAL=10;
    /** 关掉「纵向伤敌」的补偿：可用总点数变成 11（多给 1 点），代价是领域打不到地下生物。 */
    public static final int TOTAL_NO_VERTICAL=11;
    /** 关掉「纵向伤敌」时判"地下"的阈值（格）：目标 Y 比领域中心低这么多就跳过，不再对它结算斩击。 */
    public static final double VERTICAL_MARGIN=2.0;
    /** 没保存过点数时的默认分配：半径 4 点 / 伤害 3 点 / 速度 3 点 —— 正好等于现状（200 格 / ×1.00 / ×1.00）。 */
    public static final int DEFAULT_RADIUS=4,DEFAULT_DAMAGE=3,DEFAULT_SPEED=3;
    /** 领域半径：0 点 = {@link #RADIUS_BASE} 格，每点 +{@link #RADIUS_STEP} 格（默认 4 点 = 200 格 = 现状；10 点 = 320 格）。 */
    public static final int RADIUS_BASE=120,RADIUS_STEP=20;
    /** 伤害倍率：0 点 = {@link #DAMAGE_BASE}，每点 +{@link #DAMAGE_STEP}（默认 3 点 = ×1.00 = 现状；10 点 = ×1.56）。 */
    public static final double DAMAGE_BASE=0.76,DAMAGE_STEP=0.08;
    /** 斩击速度倍率：0 点 = {@link #SPEED_BASE}，每点 +{@link #SPEED_STEP}（默认 3 点 = ×1.00 = 现状；10 点 = ×1.56）。 */
    public static final double SPEED_BASE=0.76,SPEED_STEP=0.08;
    /** 领域斩击的基础间隔（刻）：速度倍率线性缩短它（×1.00 时就是原来每 2 刻一次斩击）。 */
    public static final double SLASH_INTERVAL=2;
    /** 可用总点数：开着「纵向伤敌」= {@link #TOTAL}，关掉（打不到地下生物）多给 1 点 = {@link #TOTAL_NO_VERTICAL}。
     *  界面显示与服务端校验都只走这一个入口，客户端多塞点也过不了 {@link #valid}。 */
    public static int total(boolean verticalKill){return verticalKill?TOTAL:TOTAL_NO_VERTICAL;}
    /** 半径（格）。 */
    public static int radius(int points){return RADIUS_BASE+clamp(points)*RADIUS_STEP;}
    /** 伤害倍率。 */
    public static double damage(int points){return DAMAGE_BASE+clamp(points)*DAMAGE_STEP;}
    /** 斩击速度倍率。 */
    public static double speed(int points){return SPEED_BASE+clamp(points)*SPEED_STEP;}
    /** 单项能分到的最大点数就是"总点数最大那一档"，所以按 {@link #TOTAL_NO_VERTICAL} 夹。 */
    public static int clamp(int points){return Math.max(0,Math.min(TOTAL_NO_VERTICAL,points));}
    /** 三项都不为负、且之和 ≤ {@link #total(boolean)} 才算合法（界面与服务端存档都按这一条校验；合法时每项必然 ≤ {@link #total(boolean)}）。 */
    public static boolean valid(int radiusPoints,int damagePoints,int speedPoints,boolean verticalKill){
        return radiusPoints>=0&&damagePoints>=0&&speedPoints>=0&&radiusPoints+damagePoints+speedPoints<=total(verticalKill);
    }
    /** 还能分配的点数（永远不会是负数：即使传进来一组越界的值，界面也只会显示 0）。 */
    public static int remaining(int radiusPoints,int damagePoints,int speedPoints,boolean verticalKill){
        return Math.max(0,total(verticalKill)-clamp(radiusPoints)-clamp(damagePoints)-clamp(speedPoints));
    }
    private DomainOptions(){}
}
