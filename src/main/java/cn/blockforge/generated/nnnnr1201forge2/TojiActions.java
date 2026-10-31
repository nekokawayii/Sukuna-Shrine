package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 伏黑甚尔 20 条连招的**专属动作**（走项目自绘动作系统 {@link MotionFallbackClient}）。
 *
 * <p>每招三段姿态：起手 → 命中 → 收招；三段之间线性混合，其它玩家也能看到。
 * 动作名与 {@link character.toji.TojiCombos} 的连招编号一一对应：`sukuna:toji_c1` ~ `sukuna:toji_c20`。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TojiActions {
    private static final DismantlePose.Pose STAND = new DismantlePose.Pose(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        action(1, 16, pose(0.05, 12, -6, -50, 40, 8, -10, 10, 0, 20, 10), pose(0.02, 26, 4, -20, 95, 4, -6, 14, 0, 10, 8),
            pose(0.04, 6, 0, -35, 20, 6, -8, 8, 0, 12, 10));            // 基础闪避反击：侧身重拳
        action(2, 20, pose(0.10, 22, -8, -70, -60, 18, -30, -30, 0, 60, 60), pose(0.06, 30, 6, -95, -30, 20, -55, 10, 0, 90, 70),
            pose(0.03, 14, 0, -40, 10, 8, -12, 6, 0, 30, 20));          // 裂骨膝撞：扣肩 + 提膝
        action(3, 18, pose(0.04, -8, 6, -30, 30, 10, -40, 20, 0, 40, 60), pose(0.02, -14, 10, -20, 40, 12, -70, 30, 0, 20, 70),
            pose(0.06, 4, 0, -25, 10, 6, -10, 8, 0, 20, 14));           // 飞踢拉扯：后仰凌空踢
        action(4, 24, pose(0.12, 26, -6, -80, -80, 20, -30, -30, 0, 70, 70), pose(0.20, 40, 10, -110, -110, 26, -20, -20, 0, 80, 80),
            pose(0.08, 16, 4, -30, -10, 10, -40, -40, 0, 40, 40));      // 过肩重摔：弯腰抡砸
        action(5, 18, pose(0.30, -10, -4, -20, 20, 14, -80, 20, 0, 20, 30), pose(0.26, -6, 8, -10, 60, 12, -85, 40, 0, 10, 80),
            pose(0.16, 6, 0, -20, 20, 8, -40, 16, 0, 20, 30));          // 滑铲切足：贴地滑行砸膝
        action(6, 22, pose(0.04, 10, 0, -40, 60, 6, -8, 10, 0, 14, 10), pose(0.02, 18, 6, -30, 110, 4, -6, 12, 0, 10, 8),
            pose(0.04, 12, 2, -50, 80, 8, -8, 10, 0, 12, 10));          // 天逆鉾·破魔：反手斜划
        action(7, 22, pose(0.04, 8, 0, -30, -100, 16, -10, 12, 14, 12, 10), pose(0.02, 20, 6, -20, 70, 18, -8, 14, 20, 10, 8),
            pose(0.04, 8, 0, -40, 10, 8, -10, 10, 0, 12, 10));          // 游云破膝：甩棍砸膝
        action(8, 20, pose(0.04, -6, 0, -70, 80, 10, -10, 12, 0, 12, 10), pose(0.02, 12, 6, -50, 110, 6, -8, 14, 0, 10, 8),
            pose(0.04, 6, 0, -60, 30, 8, -10, 10, 0, 12, 10));          // 替身挡箭：拽人 + 贴地重拳
        action(9, 18, pose(0.10, 6, -6, -20, 20, 24, -70, -70, 0, 90, 90), pose(0.06, 10, 6, -10, 10, 28, -80, -80, 0, 100, 100),
            pose(0.04, 0, 0, -30, 30, 10, -12, -12, 0, 20, 20));        // 踩头处决：双腿夹压
        action(10, 26, pose(0.16, 18, -10, -120, -120, 14, -40, -40, 0, 70, 70), pose(0.04, 34, 8, -10, -10, 10, -10, -10, 0, 20, 20),
            pose(0.10, 12, 0, -30, -30, 8, -20, -20, 0, 30, 30));       // 蓄力破甲：过顶砸地
        action(11, 24, pose(0.06, 12, 0, -60, 80, 8, -10, 12, 0, 12, 10), pose(0.02, 22, 8, -30, 120, 6, -8, 14, 0, 10, 8),
            pose(0.06, 8, 0, -50, 40, 10, -12, 10, 0, 14, 10));         // 极速切武三连
        action(12, 20, pose(0.06, -10, 0, -40, 150, 12, -14, 14, 0, 14, 12), pose(0.02, 18, 6, -30, 120, 8, -10, 12, 0, 12, 10),
            pose(0.04, 8, 0, -30, 40, 8, -10, 10, 0, 12, 10));          // 先之先投掷
        action(13, 24, pose(0.06, 12, 0, -40, 90, 10, -10, 12, 0, 12, 10), pose(0.03, 24, 8, -20, 130, 8, -8, 14, 0, 10, 8),
            pose(0.06, 10, 0, -40, 30, 8, -12, 10, 0, 12, 10));         // 浮空乱打
        action(14, 22, pose(0.06, 16, 4, -100, -20, 14, -12, 14, 0, 14, 12), pose(0.03, 30, 10, -130, 10, 16, -10, 12, 0, 12, 10),
            pose(0.06, 10, 0, -60, 20, 8, -12, 10, 0, 12, 10));         // 墙壁反杀：按头撞墙
        action(15, 18, pose(0.04, -8, 8, -40, 40, 30, -60, -20, 0, 60, 30), pose(0.02, -12, 12, -30, 50, 32, -80, -10, 0, 40, 20),
            pose(0.06, 6, 0, -30, 20, 10, -14, 10, 0, 14, 12));         // 蹬墙膝撞
        action(16, 16, pose(0.04, -6, 0, -30, 60, 8, -10, 12, 0, 12, 10), pose(0.02, 4, 4, -20, 100, 6, -8, 14, 0, 10, 8),
            pose(0.04, 2, 0, -30, 20, 8, -10, 10, 0, 12, 10));          // 刀反：刀尖点前
        action(17, 20, pose(0.04, -4, 0, -80, -80, 10, -10, 12, 0, 12, 10), pose(0.02, 10, 6, -110, 90, 8, -8, 14, 0, 10, 8),
            pose(0.04, 6, 0, -50, 30, 8, -10, 10, 0, 12, 10));          // 夺刃反杀：扭腕反劈
        action(18, 20, pose(0.06, 10, -4, -40, -110, 20, -12, 14, 12, 12, 10), pose(0.02, 28, 6, -20, 80, 24, -10, 16, 22, 10, 8),
            pose(0.06, 8, 0, -40, 10, 10, -12, 10, 0, 12, 10));         // 打棒球：侧身抡棒
        action(19, 22, pose(0.04, -10, 8, -30, 40, 14, -60, 20, 0, 40, 60), pose(0.02, -16, 12, -20, 50, 16, -80, 30, 0, 20, 70),
            pose(0.04, 10, 0, -30, 30, 10, -14, 10, 0, 12, 10));        // 空中体操：飞踢 + 下劈
        action(20, 26, pose(0.18, 16, -8, -130, -130, 14, -40, -40, 0, 80, 80), pose(0.04, 40, 10, -20, -20, 12, -10, -10, 0, 20, 20),
            pose(0.12, 16, 0, -40, -40, 10, -20, -20, 0, 30, 30));      // 暴君处决：举棍砸落
    }

    private static void action(int combo, int duration, DismantlePose.Pose windup,
                               DismantlePose.Pose strike, DismantlePose.Pose recover) {
        MotionFallbackClient.register("sukuna:toji_c" + combo, duration, age -> {
            double t = Math.max(0, Math.min(1, age / duration));
            DismantlePose.Pose pose;
            if (t < 0.3) pose = STAND.mix(windup, t / 0.3);
            else if (t < 0.62) pose = windup.mix(strike, (t - 0.3) / 0.32);
            else pose = strike.mix(recover, (t - 0.62) / 0.38);
            return new MotionFallbackClient.Frame(pose, 0, 0, 0);
        });
    }

    private static DismantlePose.Pose pose(double sink, double body, double head, double leftArm, double rightArm,
                                           double spread, double leftLeg, double rightLeg, double legZ,
                                           double leftKnee, double rightKnee) {
        return new DismantlePose.Pose(sink, body, head, leftArm, rightArm, spread, leftLeg, rightLeg, legZ,
            leftKnee, rightKnee);
    }

    private TojiActions() {}
}
