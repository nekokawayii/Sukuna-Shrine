package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.phys.Vec3;

/** 魔虚罗系的**客户端表现**（重做版）。
 *
 *  <p>按需求：**除了"法轮转动"之外，所有 Photon 特效全部删除**（免疫光环、适应、真伤、
 *  仪式失败、震地冲击等一律不再绘制）；**出场**也不再有自己的粒子，只保留模型自带的
 *  {@code emerge} 动画。{@link #WHEEL_BEAT} / {@link #WHEEL_END} / {@link #SPIN} / {@link #SPIN_END}
 *  是"谁戴着法轮 + 法轮要不要转"的网络与本地信号，本身不产生任何粒子。</p>
 *
 *  <p>法轮转动的特效已经**重做**：不再走 Photon，而是在 {@link MahoragaWheelRenderer} 里
 *  用模组自己的顶点通路画一圈扩散的金色光环（跟着头顶法轮一起转），见 {@link #SPIN}。</p> */
public final class MahoragaVisuals {
    /** 服务端 → 客户端：法轮转了一格（不再画粒子，只播法轮本身的自转/闪光）。 */
    public static final int WHEEL_ROTATE=213;
    /** 服务端 → 客户端：出场（已无粒子，仅保留编号以免旧包错位）。 */
    public static final int EMERGE=214;
    /** 客户端本地：头顶法轮转动的光环（由 {@link MahoragaWheelClient} 触发）。 */
    public static final int SPIN=215;
    /** 客户端本地：法轮闪光结束。 */
    public static final int SPIN_END=216;
    /** 服务端 → 客户端：法轮心跳（谁戴着法轮），无粒子。 */
    public static final int WHEEL_BEAT=217;
    /** 服务端 → 客户端：法轮收起，无粒子。 */
    public static final int WHEEL_END=218;
    /** 编号区间（{@code Client.receive} 用）。 */
    public static final int FIRST=WHEEL_ROTATE,LAST=WHEEL_END;
    public static void receive(Network.Effect e){
        switch(e.kind()){
            case WHEEL_BEAT->MahoragaWheelClient.beat(e);
            case WHEEL_END->MahoragaWheelClient.stop(e.owner());
            case WHEEL_ROTATE->MahoragaWheelClient.rotate(e);
            case SPIN->MahoragaWheelClient.spin(e.owner());
            default->{} // EMERGE / SPIN_END：没有粒子可播
        }
    }
    /** 兼容入口：适应时**不再播放任何特效**（保留空实现，调用点无需到处判断）。 */
    public static void adaptAt(Vec3 at,double level){}
    private MahoragaVisuals(){}
}
