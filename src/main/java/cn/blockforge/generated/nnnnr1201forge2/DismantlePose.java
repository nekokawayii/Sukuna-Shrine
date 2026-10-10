package cn.blockforge.generated.nnnnr1201forge2;

/** 连续关键帧只驱动原版玩家肢体，不增加模型或贴图。 */
public final class DismantlePose {
    public record Pose(double sink,double body,double head,double leftArm,double rightArm,double spread,double leftLeg,double rightLeg,double legZ,double leftKnee,double rightKnee) {
        Pose mix(Pose b,double t){return new Pose(lerp(sink,b.sink,t),lerp(body,b.body,t),lerp(head,b.head,t),lerp(leftArm,b.leftArm,t),lerp(rightArm,b.rightArm,t),lerp(spread,b.spread,t),lerp(leftLeg,b.leftLeg,t),lerp(rightLeg,b.rightLeg,t),lerp(legZ,b.legZ,t),lerp(leftKnee,b.leftKnee,t),lerp(rightKnee,b.rightKnee,t));}
    }
    private static final Pose STAND=new Pose(0,0,0,0,0,0,0,0,0,0,0);
    private static final Pose CROUCH=new Pose(0.12,18,-12,-45,-45,12,-35,-35,0,70,70);
    private static final Pose TUCK=new Pose(0,20,-20,-135,-135,16,-110,-95,0,130,120);
    private static final Pose KNEEL=new Pose(0.37,8,-8,-75,-75,8,-80,0,0,80,92);
    private static final Pose OPEN=new Pose(0.37,5,-5,-22,-22,86,-80,0,0,80,92);
    private static final Pose KICK=new Pose(0,-18,12,-70,35,25,-92,30,0,0,45);
    private static final Pose TURN=new Pose(0.04,6,-3,-12,-12,82,-16,-16,0,32,32);
    public static Pose sample(int kind,double age){
        if(kind==DismantleMotionRules.KNEEL)return frames(age,new double[]{0,5,9,13,18,22},new Pose[]{STAND,KNEEL,KNEEL,OPEN,OPEN,STAND});
        if(kind==DismantleMotionRules.GROUND_SMASH)return frames(age,new double[]{0,4,8,14,20},new Pose[]{STAND,new Pose(.2,10,-8,-160,-160,8,-75,0,0,80,92),KNEEL,KNEEL,STAND});
        if(kind==DismantleMotionRules.AIR_PUNCH)return frames(age,new double[]{0,5,12,16,23,28},new Pose[]{STAND,CROUCH,new Pose(0,-12,10,-40,-65,8,-90,15,0,110,20),new Pose(0,-8,8,-45,-90,3,-90,15,0,110,20),new Pose(0,-8,8,-45,-90,3,-90,15,0,110,20),STAND});
        if(kind==DismantleMotionRules.SLIDE)return frames(age,new double[]{0,4,16,20,26,32},new Pose[]{STAND,new Pose(.55,-65,30,-30,20,18,-70,0,0,20,100),new Pose(.55,-65,30,-30,20,18,-70,0,0,20,100),OPEN,OPEN,STAND});
        if(kind==DismantleMotionRules.BACKFLIP)return frames(age,new double[]{0,4,10,18,24,27,32,38,DismantleMotionRules.duration(kind)},new Pose[]{STAND,CROUCH,TUCK,TUCK,KNEEL,KNEEL,OPEN,OPEN,STAND});
        if(kind==DismantleMotionRules.FLYING_KICK)return frames(age,new double[]{0,4,9,15,18,23,28,33,36,40,44,50},new Pose[]{STAND,CROUCH,KICK,KICK,CROUCH,TUCK,TUCK,KNEEL,KNEEL,OPEN,OPEN,STAND});
        return frames(age,new double[]{0,4,10,24,30},new Pose[]{STAND,CROUCH,TURN,TURN,STAND});
    }
    private static Pose frames(double age,double[] times,Pose[] poses){for(int i=1;i<times.length;i++)if(age<=times[i])return poses[i-1].mix(poses[i],DismantleMotionRules.phase(age,times[i-1],times[i]));return poses[poses.length-1];}
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private DismantlePose(){}
}
