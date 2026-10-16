package cn.blockforge.generated.nnnnr1201forge2;

/** 两端共用动作时间轴；位置单位为格，角度为度，时间单位为刻。 */
public final class DismantleMotionRules {
    public static final int BACKFLIP=1, FLYING_KICK=2, SPIN=3, KNEEL=4, AIR_PUNCH=5, SLIDE=6, GROUND_SMASH=7, RING_SKILL=28;
    public static final int RING_SEGMENTS=48, RING_LIFE=24, RING_FX=163;
    public static final double BACK_DISTANCE=10, KICK_DISTANCE=6, RETURN_DISTANCE=3;
    /** 《使用说明》四(三)：飞踢接触敌人时**轻推开敌人，不额外增加独立踢击伤害**。
     *  所以这里只有推力与抬升，没有伤害常量。 */
    public static final double KICK_DAMAGE=0, KICK_KNOCKBACK=1.5, KICK_LIFT=0.45;
    public static boolean kickActive(int kind,int age){return kind==FLYING_KICK&&age>=5&&age<=16;}
    public static double kickLift(double resistance){return KICK_LIFT*(1-Math.max(0,Math.min(1,resistance)));}
    public static int trigger(boolean crouching,boolean forward,boolean backward){return !crouching||forward==backward?0:backward?BACKFLIP:FLYING_KICK;}
    /** 由按键 + 服务端的冲刺/离地状态挑动作；返回 0 表示"这不是动作，走普通解"。
     *  与《使用说明》四(三)(四)一致：蹲下后退/后退跳 = 后翻；蹲下前进 = 飞踢；蹲下不动 = 跪地；
     *  疾跑前进 = 滑铲（起跳或上升中 = 冲拳）。 */
    public static int choose(boolean crouch,boolean forward,boolean backward,boolean jump,boolean sprint,boolean ground,double vertical){
        if(forward&&backward){forward=false;backward=false;}
        if(backward&&jump)return BACKFLIP;
        if(crouch)return backward?BACKFLIP:forward?FLYING_KICK:KNEEL;
        if(sprint&&forward)return jump||vertical>0?AIR_PUNCH:SLIDE;
        return 0;
    }
    public static boolean valid(int kind){return kind>=BACKFLIP&&kind<=GROUND_SMASH;}
    public static int release(int kind){return kind==BACKFLIP?30:kind==FLYING_KICK?38:kind==KNEEL?10:kind==AIR_PUNCH?16:kind==SLIDE?20:kind==GROUND_SMASH?8:18;}
    public static int duration(int kind){return Math.max(release(kind)+12,lastRelease(kind)+6);}
    public static int blades(int kind){return kind==BACKFLIP?5:kind==FLYING_KICK?3:0;}
    /** 每道剑气独立抽样，不再按条数把角度固定排开。 */
    public static double bladeAngle(double random){return 165+30*Math.max(0,Math.min(1,random));}
    public static int bladeDelay(int index){return Math.max(0,index)*SlashFormRules.GAP;}
    public static int lastRelease(int kind){return release(kind)+bladeDelay(Math.max(0,blades(kind)-1));}
    public static boolean bladeReady(int kind,int age,int emitted){return emitted>=0&&emitted<blades(kind)&&age>=release(kind)+bladeDelay(emitted);}
    public static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    public static double phase(double age,double start,double end){return smooth((age-start)/(end-start));}
    public static double horizontal(int kind,double age){
        if(kind==SLIDE)return 10*phase(age,2,20);
        if(kind==AIR_PUNCH)return 2*phase(age,4,16);
        if(kind==BACKFLIP)return -BACK_DISTANCE*phase(age,4,22);
        if(kind==FLYING_KICK)return KICK_DISTANCE*phase(age,4,16)-RETURN_DISTANCE*phase(age,18,30);
        return 0;
    }
    public static double height(int kind,double age){
        if(kind==AIR_PUNCH)return 5*phase(age,0,14);
        if(kind==BACKFLIP)return arc(age,4,22,2.6);
        if(kind==FLYING_KICK)return age<=18?1.1*phase(age,4,10):1.1*(1-phase(age,18,30))+arc(age,18,30,1.7);
        return 0;
    }
    private static double arc(double age,double from,double to,double height){double t=phase(age,from,to);return height*Math.sin(Math.PI*t);}
    public static double flip(int kind,double age){return kind==BACKFLIP?360*phase(age,4,22):kind==FLYING_KICK?360*phase(age,18,30):0;}
    public static double sideways(int kind,double age){return kind==SLIDE?85*(phase(age,16,20)-phase(age,26,32)):0;}
    public static double spin(double age){return 360*phase(age,4,26);}
    public static double ringRadius(double age){return 2+Math.max(0,Math.min(RING_LIFE,age))*SlashFormRules.SPEED;}
    private DismantleMotionRules(){}
}
