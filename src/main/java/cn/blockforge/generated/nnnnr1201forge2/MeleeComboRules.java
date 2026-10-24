package cn.blockforge.generated.nnnnr1201forge2;

import java.util.ArrayDeque;

/** 纯动作：不包含伤害、位移、攻击冷却或咒力结算。角度为度，时间为游戏刻。 */
public final class MeleeComboRules {
    public static final int STAGES=8, RESET_GAP=14, MAX_BUFFER_TICKS=71;
    private static final int[] LENGTH={7,7,8,8,10,8,11,12};
    private static final double[] WIND_END={1.7,1.7,2.3,2.4,3.2,2.1,3.8,4.0};
    private static final double[] HIT={2.5,2.5,3.2,3.3,4.2,3.0,5.0,5.2};
    public record Step(int stage,long start) {}
    public static int duration(int stage){return valid(stage)?LENGTH[stage-1]:0;}
    public static double impact(int stage){return valid(stage)?HIT[stage-1]:0;}
    public static double windup(int stage){return valid(stage)?WIND_END[stage-1]:0;}
    public static final class Sequence {
        private int stage;
        private long lastClick=Long.MIN_VALUE,lastEnd=Long.MIN_VALUE;
        private final ArrayDeque<Long> pending=new ArrayDeque<>();
        public Step press(long now) {
            if(stage==0||now<lastClick||expired(now))reset();
            while(!pending.isEmpty()&&pending.peekFirst()<=now)pending.removeFirst();
            if(pending.size()>=STAGES)return null;
            long start=stage==0?now:Math.max(now,lastEnd);
            stage=stage%STAGES+1;lastClick=now;lastEnd=start+duration(stage);pending.addLast(lastEnd);
            return new Step(stage,start);
        }
        public boolean expired(long now){return stage!=0&&now>Math.max(lastClick,lastEnd)+RESET_GAP;}
        public void reset(){stage=0;lastClick=lastEnd=Long.MIN_VALUE;pending.clear();}
    }
    public static boolean valid(int stage){return stage>=1&&stage<=STAGES;}
    public static boolean kick(int stage){return stage==5||stage==7;}
    public record Pose(double body,double twist,double head,double leftArm,double rightArm,
                       double leftYaw,double rightYaw,double leftSpread,double rightSpread,
                       double leftLeg,double rightLeg,double leftKnee,double rightKnee,double sink,
                       double leftElbow,double rightElbow,double lean,double leftLegYaw,double rightLegYaw,
                       double leftLegSpread,double rightLegSpread) {
        public Pose mix(Pose b,double t){return new Pose(m(body,b.body,t),m(twist,b.twist,t),m(head,b.head,t),m(leftArm,b.leftArm,t),m(rightArm,b.rightArm,t),m(leftYaw,b.leftYaw,t),m(rightYaw,b.rightYaw,t),m(leftSpread,b.leftSpread,t),m(rightSpread,b.rightSpread,t),m(leftLeg,b.leftLeg,t),m(rightLeg,b.rightLeg,t),m(leftKnee,b.leftKnee,t),m(rightKnee,b.rightKnee,t),m(sink,b.sink,t),m(leftElbow,b.leftElbow,t),m(rightElbow,b.rightElbow,t),m(lean,b.lean,t),m(leftLegYaw,b.leftLegYaw,t),m(rightLegYaw,b.rightLegYaw,t),m(leftLegSpread,b.leftLegSpread,t),m(rightLegSpread,b.rightLegSpread,t));}
    }
    public static final Pose REST=new Pose(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
    // 屈肘护脸、前后脚站稳，不再把两条直胳膊平举当格斗准备姿势。
    public static final Pose GUARD=new Pose(7,-8,-5,-28,-22,10,-16,-8,10,-22,12,24,16,.025,-88,-100,0,-8,10,-5,5);
    private static final Pose[] WIND={
        new Pose(3,22,-3,5,-28,24,-18,-12,10,-24,10,28,12,.04,-112,-95,-3,-8,14,-6,6),
        new Pose(4,-32,-3,-24,16,12,-32,-10,14,-12,-18,18,28,.05,-96,-118,3,-10,18,-6,6),
        new Pose(8,40,-6,-30,-26,38,-20,-42,8,-28,14,34,16,.06,-95,-100,-5,-12,20,-8,8),
        new Pose(26,-28,-14,-32,28,16,-25,-10,18,-32,-14,44,32,.11,-90,-115,5,-8,18,-7,7),
        new Pose(-10,18,7,-32,-24,15,-20,-20,20,-102,8,124,18,.02,-80,-95,3,0,15,-3,8),
        new Pose(10,-38,-6,-30,-28,15,-38,-12,32,-24,6,30,18,.05,-96,-142,5,-10,18,-7,7),
        new Pose(-8,52,6,-25,-32,28,-25,-28,28,8,-92,18,124,.015,-85,-95,-7,-16,-35,-10,18),
        new Pose(-16,-10,10,-154,-150,8,-8,-18,18,-24,-20,32,30,.03,-45,-45,0,-12,12,-10,10)
    };
    private static final Pose[] STRIKES={
        new Pose(14,-35,-8,-94,-24,-9,-20,-4,10,-30,18,30,10,.025,-4,-102,-5,-14,20,-7,7),
        new Pose(19,42,-12,-28,-101,18,8,-10,4,14,-32,12,34,.035,-100,-3,7,-20,22,-8,8),
        new Pose(13,-62,-7,-78,-25,-64,-18,-38,12,-34,15,36,12,.035,-72,-100,-9,-24,28,-9,9),
        new Pose(-10,42,6,-25,-82,16,24,-8,14,16,-32,12,36,.025,-100,-108,7,-14,24,-8,8),
        new Pose(-23,-20,12,-35,10,24,-26,-24,26,-114,12,4,18,.015,-75,-80,4,-6,18,-4,9),
        new Pose(20,62,-12,-30,-72,20,68,-12,44,-26,15,30,12,.025,-96,-148,10,-20,32,-8,8),
        new Pose(-18,-76,9,-22,-38,34,-26,-36,32,8,-100,16,6,.01,-82,-90,-14,24,68,-12,52),
        new Pose(42,8,-25,-66,-70,-12,12,-10,10,-56,-42,72,58,.19,-16,-18,0,-18,18,-12,12)
    };
    public static Pose sample(int stage,double age){return sample(stage,age,REST,false);}
    /** 连点时在战斗架势中交接，只有最后一段才站回去；快速出招、短暂停势、较慢回收。 */
    public static Pose sample(int stage,double age,Pose entry,boolean continues){
        if(!valid(stage)||!Double.isFinite(age)||age<0)return REST;
        int i=stage-1;double wind=WIND_END[i],hit=HIT[i],hold=hit+(stage>=7?.85:.55),recover=duration(stage)-1.6;
        Pose end=continues?GUARD:REST;
        if(age>=duration(stage))return end;
        if(age<wind)return entry.mix(WIND[i],smooth(age/wind));
        if(age<hit){double t=(age-wind)/(hit-wind);return WIND[i].mix(STRIKES[i],1-Math.pow(1-t,3));}
        if(age<hold)return STRIKES[i];
        if(age<recover)return STRIKES[i].mix(GUARD,smooth((age-hold)/(recover-hold)));
        return GUARD.mix(end,smooth((age-recover)/(duration(stage)-recover)));
    }
    /** 第一人称与第三人称共用发力时间，而非匀速正弦摆手。负值表示回拉蓄力。 */
    public static double drive(int stage,double age){
        if(!valid(stage)||!Double.isFinite(age)||age<0||age>=duration(stage))return 0;
        int i=stage-1;double wind=WIND_END[i],hit=HIT[i],hold=hit+(stage>=7?.85:.55);
        if(age<wind)return -.18*smooth(age/wind);
        if(age<hit)return m(-.18,1,1-Math.pow(1-(age-wind)/(hit-wind),3));
        if(age<hold)return 1;
        return 1-smooth((age-hold)/(duration(stage)-hold));
    }
    public static double envelope(int stage,double age){return !valid(stage)||age<0||age>=duration(stage)?0:Math.min(smooth(age/.8),smooth((duration(stage)-age)/1.2));}
    private static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static double m(double a,double b,double t){return a+(b-a)*t;}
    private MeleeComboRules(){}
}
