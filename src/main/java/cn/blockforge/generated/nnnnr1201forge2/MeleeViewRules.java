package cn.blockforge.generated.nnnnr1201forge2;

/** 第一人称专用姿态与拳风时间轴；只有画面数值，不改攻击判定。 */
public final class MeleeViewRules {
    // 人物模型：+X 向左、+Y 向下；镜头：+X 向右、+Y 向上。两轴一起反转，不镜像皮肤。
    public static final float VIEW_X=-1,VIEW_Y=-1,VIEW_Z=1;
    public record Arm(double x,double y,double z,double pitch,double yaw,double roll,double elbow) {}
    public record Point(double x,double y,double z) {}
    public static boolean striking(int stage,int side){
        return MeleeComboRules.valid(stage)&&!MeleeComboRules.kick(stage)&&(stage==8||side==(stage%2==0?1:-1));
    }
    /** side 为镜头左右：-1 左臂，+1 右臂；关节角保持人物模型本来的屈肘方向。 */
    public static Arm arm(int stage,int side,double age,MeleeComboRules.Pose pose){
        double drive=MeleeComboRules.drive(stage,age),hit=Math.max(0,drive);
        double env=MeleeComboRules.envelope(stage,age),wind=Math.max(0,-drive)/.18;
        boolean strike=striking(stage,side);
        double x=side*.36,y=-.32,z=-.12,pitch=-38,yaw=-side*8,roll=-side*5,elbow=-72;
        if(strike){
            x-=side*.08*hit;y+=.05*hit;z+=.09*wind-.16*hit;
            pitch-=46*hit;yaw-=side*4*hit;
            elbow=(side<0?pose.leftElbow():pose.rightElbow())*.72;
            // 起手也保留护拳，不让整条直臂忽然竖到镜头前。
            elbow=Math.min(elbow,-72*(1-env));
            if(stage==3){x-=.07*env;x+=.21*hit;pitch=-38-16*hit;yaw=8+48*hit;roll=5-17*hit;}
            if(stage==4){y-=.17*env;y+=.17*hit;pitch=-38-32*hit;elbow=-72-23*hit;}
            if(stage==6){x+=.06*env-.18*hit;pitch=-38-28*hit;yaw=-8-46*hit;roll=-5-23*hit;elbow=-72-64*hit;}
            if(stage==8){y+=.12*env-.20*hit+.04*wind;pitch=-38-87*env+55*hit;}
        }
        return new Arm(x,y,z,pitch,yaw,roll,elbow);
    }
    public static double windAlpha(int stage,double age){
        if(!MeleeComboRules.valid(stage)||!Double.isFinite(age))return 0;
        double start=MeleeComboRules.windup(stage),hit=MeleeComboRules.impact(stage),end=hit+2.2;
        if(age<=start||age>=end)return 0;
        if(age<hit)return smooth((age-start)/(hit-start));
        return 1-smooth((age-hit)/(end-hit));
    }
    public static double windProgress(int stage,double age){
        if(!MeleeComboRules.valid(stage)||!Double.isFinite(age))return 0;
        double start=MeleeComboRules.windup(stage);
        return Math.max(0,Math.min(1,(age-start)/(MeleeComboRules.impact(stage)+2.2-start)));
    }
    /** 与 ModelPart 的 Z/Y/X 旋转顺序一致，用于回归验证拳头端点及皮肤面朝向。 */
    public static Point viewPoint(Arm arm,double x,double y,double z,boolean forearm){
        if(forearm){double a=Math.toRadians(arm.elbow()),ny=y*Math.cos(a)-z*Math.sin(a);z=y*Math.sin(a)+z*Math.cos(a);y=ny+.25;}
        double a=Math.toRadians(arm.pitch()),ny=y*Math.cos(a)-z*Math.sin(a);z=y*Math.sin(a)+z*Math.cos(a);y=ny;
        a=Math.toRadians(arm.yaw());double nx=x*Math.cos(a)+z*Math.sin(a);z=-x*Math.sin(a)+z*Math.cos(a);x=nx;
        a=Math.toRadians(arm.roll());nx=x*Math.cos(a)-y*Math.sin(a);y=x*Math.sin(a)+y*Math.cos(a);x=nx;
        return new Point(arm.x()+VIEW_X*x,arm.y()+VIEW_Y*y,arm.z()+VIEW_Z*z);
    }
    private static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private MeleeViewRules(){}
}
