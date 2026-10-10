package cn.blockforge.generated.nnnnr1201forge2;

/** 同一角度同时用于服务端圆锥筛选和客户端透视圆圈。 */
public final class AimRules {
    public static final double DEFAULT_ANGLE=3,DEFAULT_RANGE=64,MIN_ANGLE=.5,MAX_ANGLE=15,MIN_RANGE=4,MAX_RANGE=256;
    public static double angle(double n){return Double.isFinite(n)?Math.max(MIN_ANGLE,Math.min(MAX_ANGLE,n)):DEFAULT_ANGLE;}
    public static double range(double n){return Double.isFinite(n)?Math.max(MIN_RANGE,Math.min(MAX_RANGE,n)):DEFAULT_RANGE;}
    public static boolean inside(double dot,double distance,double angle,double range){return Double.isFinite(dot)&&Double.isFinite(distance)&&distance>0&&distance<=range&&dot>=Math.cos(Math.toRadians(angle))-1e-10;}
    public static double radius(double angle,double fov,int height){return height*.5*Math.tan(Math.toRadians(angle))/Math.tan(Math.toRadians(Math.max(1,Math.min(179,fov)))/2);}
    private AimRules(){}
}
