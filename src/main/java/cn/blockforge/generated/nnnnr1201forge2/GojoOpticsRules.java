package cn.blockforge.generated.nnnnr1201forge2;

/** 光学数值规则：Java 与着色器共享参数，供无窗口回归验证。 */
public final class GojoOpticsRules {
    public static final float LENS_SCALE=1.05f;
    public static float radius(float projectionY,float size,float clipW){
        if(!Float.isFinite(projectionY)||!Float.isFinite(size)||!Float.isFinite(clipW)||clipW<=.05f||size<=0)return 0;
        return Math.min(1.8f,Math.abs(projectionY*size*LENS_SCALE/clipW)*.5f);
    }
    public static double bend(double radius){
        if(!Double.isFinite(radius)||radius<=.06||radius>=3.6)return 0;
        double t=Math.max(0,Math.min(1,(radius-2.4)/1.2));
        return 1.35*(1-t*t*(3-2*t))/(radius*radius+.12);
    }
    public static boolean background(double surface,double light,double size,boolean sky){
        return sky||surface>light-size*.35;
    }
    public static float shadowRange(double distance){return (float)Math.max(0,Math.min(24,Double.isFinite(distance)?distance:24));}
    private GojoOpticsRules(){}
}
