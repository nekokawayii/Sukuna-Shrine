package cn.blockforge.generated.nnnnr1201forge2;

/** 「简易领域 · 捌」的姿态：**两手上举**（长按维持期间由客户端姿态层读取）。
 *
 *  <p>不新增一套骨架：直接复用动作姿态层（{@link DismantleMotionClient} 的渲染钩子）
 *  里"举手"的关节值，服务端只负责告诉客户端"当前有谁在维持简易领域"。</p> */
public final class SimpleDomainPose {
    /** 两手上举：上臂前抬约 160°、内收一点，小臂顺势上举。 */
    public static final float ARM_X=-2.8f,ARM_Z=0.18f,HEAD_X=-0.12f;
    private SimpleDomainPose(){}
}
