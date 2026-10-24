package cn.blockforge.generated.nnnnr1201forge2;

/** 封印边界与时间规则，服务端和回归共用。 */
public final class PrisonRules {
    public static final double RADIUS=5, LIGHT_RADIUS=5, LIGHT_HEIGHT=16;
    public static final int OPEN_TICKS=92, LIGHT_TICKS=120, PULSE_TICKS=10;
    public static boolean inRange(double squared){return squared<=RADIUS*RADIUS;}
    public static boolean lightPulse(int age){return age>0&&age<=LIGHT_TICKS&&age%PULSE_TICKS==0;}
    public static boolean inside(double x,double y,double z,double cx,double cz){return Math.abs(x-cx)<=14&&Math.abs(z-cz)<=14&&y>=64&&y<86;}
    public static String phrase(String raw){return raw.replace(" ","").replace("　","").replace(",","，").trim();}
    private PrisonRules(){}
}
