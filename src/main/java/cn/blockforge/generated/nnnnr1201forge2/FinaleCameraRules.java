package cn.blockforge.generated.nnnnr1201forge2;

/** 坐标以施术者为原点：侧向、竖直、面朝方向；不依赖玩家输入或代理实体。 */
public final class FinaleCameraRules {
    public record Shot(double side,double height,double forward,double focusHeight,double fov){}
    public static Shot shot(double age){
        if(age<FinaleRules.TRIGGERS[0])return new Shot(2.2,.15,-4.3,5,98);
        if(age<FinaleRules.TRIGGERS[0]+4)return new Shot(3,2,-28,21,98);
        if(age<FinaleRules.TRIGGERS[1])return new Shot(1.5,4.5,13,6,85);
        if(age<FinaleRules.STARTS[2])return new Shot(3.8,3,-5.5,1.1,75);
        if(age<FinaleRules.STARTS[2]+12)return new Shot(0,28,-.05,0,82);
        if(age<FinaleRules.TRIGGERS[3]-8){double t=clamp((age-(FinaleRules.STARTS[2]+12))/64);return new Shot(2+2*t,-9.2,-10-9*t,1.0,74);}
        if(age<FinaleRules.STARTS[4])return new Shot(0,24,-.05,.7,80);
        double t=clamp((age-FinaleRules.STARTS[4])/(FinaleRules.SHATTER-FinaleRules.STARTS[4]));
        return new Shot(0,1.55,2.8+3.8*t,1.2,62);
    }
    public static double shake(double age){
        double first=age-(FinaleRules.TRIGGERS[0]+12),second=age-FinaleRules.TRIGGERS[3];
        double t=second>=0?second:first;return t>=0&&t<8?(1-t/8)*Math.sin(t*7.3)*1.5:0;
    }
    private static double clamp(double n){return Math.max(0,Math.min(1,n));}
    private FinaleCameraRules(){}
}
