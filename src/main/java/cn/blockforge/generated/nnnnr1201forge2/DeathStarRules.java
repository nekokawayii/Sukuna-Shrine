package cn.blockforge.generated.nnnnr1201forge2;

/** 死胎星数值集中入口，纯规则可独立回归。生命值以点计，2点是一颗红心。 */
public final class DeathStarRules {
    public static final int SKILL=75,CHARGE_TICKS=40,ANEMIA_TICKS=1200,FLIGHT_TICKS=100;
    public static final double RADIUS=50,PULL_RADIUS=32,SPEED=.65,BALL_RADIUS=.8;
    public static final float FIXED_DAMAGE=1000;
    public static float remainingHealth(float current){return Math.max(1,current*.1f);}
    public static float damage(float maximum){return Math.max(0,maximum)*.5f+FIXED_DAMAGE;}
    public static boolean inside(double squared){return Double.isFinite(squared)&&squared<=RADIUS*RADIUS;}
    private DeathStarRules(){}
}
