package cn.blockforge.generated.nnnnr1201forge2;

/** 伏魔御厨子（技能5）展开前的点数分配表：固定 {@link #TOTAL} 点在「领域半径 / 伤害倍率 / 斩击速度」三项之间分配。
 *
 *  <p>三项每点收益**线性相同**，所以"降低一项＝退还该点数"与"按最终收益的减少退点"是同一件事。
 *  这份换算给服务端与客户端共用：服务端在 {@link ShrineRules#DOMAIN_OPEN_TICKS} 刻真正展开时读"那一刻已保存的点数"，
 *  客户端界面 {@link DomainOptionsScreen} 用它显示数值。平衡值只改这一个文件。</p> */
public final class DomainOptions {
    /** 每次展开可以分配的总点数（三项之和不得超过它）。 */
    public static final int TOTAL=10;
    /** 没保存过点数时的默认分配：半径 4 点 / 伤害 3 点 / 速度 3 点。 */
    public static final int DEFAULT_RADIUS=4,DEFAULT_DAMAGE=3,DEFAULT_SPEED=3;
    /** 领域半径：0 点 = {@link #RADIUS_BASE} 格，每点 +{@link #RADIUS_STEP} 格（5 点 = 200 格 = 现状）。 */
    public static final int RADIUS_BASE=100,RADIUS_STEP=20;
    /** 伤害倍率：0 点 = {@link #DAMAGE_BASE}，每点 +{@link #DAMAGE_STEP}（5 点 = ×1.00 = 现状）。 */
    public static final double DAMAGE_BASE=0.60,DAMAGE_STEP=0.08;
    /** 斩击速度倍率：0 点 = {@link #SPEED_BASE}，每点 +{@link #SPEED_STEP}（5 点 = ×1.00 = 现状）。 */
    public static final double SPEED_BASE=0.60,SPEED_STEP=0.08;
    /** 领域斩击的基础间隔（刻）：速度倍率线性缩短它（×1.00 时就是原来每 2 刻一次斩击）。 */
    public static final double SLASH_INTERVAL=2;
    /** 半径（格）。 */
    public static int radius(int points){return RADIUS_BASE+clamp(points)*RADIUS_STEP;}
    /** 伤害倍率。 */
    public static double damage(int points){return DAMAGE_BASE+clamp(points)*DAMAGE_STEP;}
    /** 斩击速度倍率。 */
    public static double speed(int points){return SPEED_BASE+clamp(points)*SPEED_STEP;}
    public static int clamp(int points){return Math.max(0,Math.min(TOTAL,points));}
    /** 三项都不为负、且之和 ≤ {@link #TOTAL} 才算合法（界面与服务端存档都按这一条校验；合法时每项必然 ≤ {@link #TOTAL}）。 */
    public static boolean valid(int radiusPoints,int damagePoints,int speedPoints){
        return radiusPoints>=0&&damagePoints>=0&&speedPoints>=0&&radiusPoints+damagePoints+speedPoints<=TOTAL;
    }
    /** 还能分配的点数（永远不会是负数：即使传进来一组越界的值，界面也只会显示 0）。 */
    public static int remaining(int radiusPoints,int damagePoints,int speedPoints){
        return Math.max(0,TOTAL-clamp(radiusPoints)-clamp(damagePoints)-clamp(speedPoints));
    }
    private DomainOptions(){}
}
