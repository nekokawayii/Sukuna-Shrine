package cn.blockforge.generated.nnnnr1201forge2;

/** 解动作的**自绘关键帧动作组**：只驱动原版玩家的肢体关节，不增加模型、贴图或前置依赖。
 *
 *  <p>设计：每个动作 = 一组 (时刻, 姿态) 关键帧，按 {@link DismantleMotionRules#phase} 做 smoothstep
 *  过渡；角度单位为度，正值含义与原版关节一致（{@code leftArm.xRot} 负值=抬臂向前，
 *  {@code leftLeg.xRot} 负值=抬腿向前，膝盖正值=向后弯，{@code spread}=双臂向外张开）。
 *  时刻与 {@link DismantleMotionRules} 的时间轴对齐（起手/击发/收招都在同一刻）。</p>
 *
 *  <p>整体旋转不在这里（后空翻翻转、旋转环斩自转、滑铲侧倾由姿态层矩阵完成，
 *  见 {@code DismantleMotionClient.MotionRenderer#setupRotations}）——刚体旋转用矩阵比骨骼更稳。</p> */
public final class DismantlePose {
    public record Pose(double sink,double body,double head,double leftArm,double rightArm,double spread,double leftLeg,double rightLeg,double legZ,double leftKnee,double rightKnee) {
        Pose mix(Pose b,double t){return new Pose(lerp(sink,b.sink,t),lerp(body,b.body,t),lerp(head,b.head,t),lerp(leftArm,b.leftArm,t),lerp(rightArm,b.rightArm,t),lerp(spread,b.spread,t),lerp(leftLeg,b.leftLeg,t),lerp(rightLeg,b.rightLeg,t),lerp(legZ,b.legZ,t),lerp(leftKnee,b.leftKnee,t),lerp(rightKnee,b.rightKnee,t));}
    }
    // ───────────────────────── 姿态基元 ─────────────────────────
    /** 站立（中立）。 */
    private static final Pose STAND=new Pose(0,0,0,0,0,0,0,0,0,0,0);
    /** 起手蹲：屈膝、双臂内收。 */
    private static final Pose CROUCH=new Pose(0.12,18,-12,-45,-45,12,-35,-35,0,70,70);
    /** 深蹲蓄力（起跳前）。 */
    private static final Pose DEEP=new Pose(0.28,26,-16,-70,-70,10,-55,-55,0,95,95);
    /** 团身：抱膝收腿（后翻空中用）。 */
    private static final Pose TUCK=new Pose(0,22,-22,-140,-140,16,-115,-100,0,135,125);
    /** 空中展身。 */
    private static final Pose SPREAD=new Pose(0,10,-10,-30,-30,80,-40,-40,0,45,45);
    /** 单膝跪地。 */
    private static final Pose KNEEL=new Pose(0.37,8,-8,-75,-75,8,-80,0,0,80,92);
    /** 跪地展开双臂（收招）。 */
    private static final Pose OPEN=new Pose(0.37,5,-5,-22,-22,86,-80,0,0,80,92);
    /** 落地缓冲。 */
    private static final Pose LAND=new Pose(0.22,16,-10,-30,-20,28,-60,-55,0,72,72);
    /** 飞踢：前腿踢直、后腿后摆、上身后仰。 */
    private static final Pose KICK=new Pose(0,-18,12,-70,35,25,-92,30,0,0,45);
    /** 飞踢起腿前的收势。 */
    private static final Pose KICK_WIND=new Pose(0,8,-6,-55,-45,20,-40,-30,0,80,60);
    /** 滑铲：后仰极低、前腿伸直、后腿压弯、双臂后摆。 */
    private static final Pose SLIDE_LOW=new Pose(0.62,-70,34,-25,25,24,-95,-25,0,15,115);
    /** 滑铲起身过渡。 */
    private static final Pose SLIDE_MID=new Pose(0.45,-45,22,-20,20,20,-70,-15,0,30,95);
    /** 冲拳：右拳前冲、左臂后引、上身微前倾。 */
    private static final Pose PUNCH=new Pose(0,-14,10,-42,-95,6,-88,12,0,105,25);
    /** 出拳前的收势。 */
    private static final Pose PUNCH_WIND=new Pose(0.10,14,-10,-55,-70,10,-60,-45,0,85,70);
    /** 双手高举（砸地起手 / 简易领域）。 */
    private static final Pose RAISE=new Pose(0.20,10,-8,-165,-165,8,-75,0,0,80,92);
    /** 砸地：双臂下砸、俯身屈膝。 */
    private static final Pose SLAM=new Pose(0.34,34,-20,25,25,14,-85,0,0,88,96);
    /** 旋转环斩：双臂平展、身体微沉。 */
    private static final Pose TURN=new Pose(0.04,6,-3,-12,-12,82,-16,-16,0,32,32);

    // ───────────────────────── 各动作的关键帧时间轴（刻） ─────────────────────────
    /** 取某个动作在"已过 age 刻"时的姿态。 */
    public static Pose sample(int kind,double age){
        if(kind==DismantleMotionRules.BACKFLIP)    // 后翻：蓄力 → 团身（翻转矩阵同时转）→ 展身 → 落地
            return frames(age,new double[]{0,4,9,16,22,27,33,39,46},
                new Pose[]{STAND,DEEP,TUCK,TUCK,SPREAD,OPEN,KNEEL,LAND,STAND});
        if(kind==DismantleMotionRules.FLYING_KICK) // 飞踢：收势 → 踢直（覆盖 5~16 判定窗口）→ 收腿团身 → 落地
            return frames(age,new double[]{0,4,7,12,17,21,25,29,34,38,44,50},
                new Pose[]{STAND,KICK_WIND,KICK,KICK,CROUCH,TUCK,TUCK,OPEN,KNEEL,LAND,STAND,STAND});
        if(kind==DismantleMotionRules.SLIDE)       // 滑铲：扑低 → 保持低姿滑行 → 起身
            return frames(age,new double[]{0,4,10,18,24,30,36},
                new Pose[]{STAND,SLIDE_LOW,SLIDE_LOW,SLIDE_LOW,SLIDE_MID,OPEN,STAND});
        if(kind==DismantleMotionRules.AIR_PUNCH)   // 冲拳：收势 → 冲拳 → 保持 → 收
            return frames(age,new double[]{0,4,9,14,20,26,32},
                new Pose[]{STAND,CROUCH,PUNCH,PUNCH,PUNCH,CROUCH,STAND});
        if(kind==DismantleMotionRules.KNEEL)       // 跪地：深蹲 → 单膝跪 → 展臂 → 起
            return frames(age,new double[]{0,5,9,14,19,24},
                new Pose[]{STAND,DEEP,KNEEL,KNEEL,OPEN,STAND});
        if(kind==DismantleMotionRules.GROUND_SMASH)// 砸地：举双手 → 下砸 → 跪姿 → 起
            return frames(age,new double[]{0,4,7,10,14,18,24},
                new Pose[]{STAND,RAISE,RAISE,SLAM,KNEEL,KNEEL,STAND});
        // 其余（旋转环斩等）：双臂平展，整体自转由姿态层矩阵完成
        return frames(age,new double[]{0,4,10,20,26,32},
            new Pose[]{STAND,CROUCH,TURN,TURN,OPEN,STAND});
    }
    private static Pose frames(double age,double[] times,Pose[] poses){for(int i=1;i<times.length;i++)if(age<=times[i])return poses[i-1].mix(poses[i],DismantleMotionRules.phase(age,times[i-1],times[i]));return poses[poses.length-1];}
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private DismantlePose(){}
}
