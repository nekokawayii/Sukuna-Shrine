package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/** 虎杖重做（r316）表现层：全部走 PhotonFx 加法层，按招区分形状与颜色。 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class YujiFxClient {
    private static final int ORANGE = 0xFFFFAB85, RED = 0xFFFF6A4A, BLUE = 0xFF8FC8FF, BLACK = 0xFF2A0A0A;

    public static void receive(Network.Effect e) {
        Vec3 at = e.position();
        float size = Math.max(.3f, e.size());
        Vec3 dir = e.direction().lengthSqr() < .01 ? new Vec3(0, 0, 1) : e.direction().normalize();
        switch (e.kind()) {
            case YujiFx.IKKI -> { // 二段：敌人背后的蓝色咒力震荡圈
                PhotonFx.shockwave(at, BLUE, 1.8, 12);
                PhotonFx.spark(at, 1.6, .5, BLUE, 0xFFDCF0FF, "textures/effect/glow.png", true, e.life(), 10, 30, 0);
            }
            case YujiFx.BREAK -> {
                PhotonFx.shockwave(at, ORANGE, 2.2, 12);
                PhotonFx.spark(at, 2.0, .7, ORANGE, 0xFFFFE0CE, "textures/effect/glow.png", true, e.life(), 10, 34, 0);
            }
            case YujiFx.BARRAGE -> PhotonFx.spark(at, 1.2, .45, ORANGE, 0xFFFFE6D6, "textures/effect/glow.png", true, e.life(), 6, 30, 0);
            case YujiFx.CHARGE -> PhotonFx.spark(at, 1.0, .5, ORANGE, 0xFFFFF0D0, "textures/effect/glow.png", true, e.life(), 12, 22, -0.4);
            case YujiFx.CHARGE_HIT -> {
                PhotonFx.shockwave(at, RED, 3.2, 16);
                PhotonFx.ring(at, 2.4, .25, RED, 0xFFFFD8C8, 10, e.life(), 14);
            }
            case YujiFx.RISING -> { // 上勾：竖直光柱 + 旋风
                PhotonFx.beam(at, at.add(0, 5, 0), 1.1, 14);
                PhotonFx.ring(at, 1.6, .16, ORANGE, 0xFFFFE8D8, 8, e.life(), 12);
            }
            case YujiFx.HOOK -> PhotonFx.ring(at, 1.8, .2, ORANGE, 0xFFFFD8B8, 7, e.life(), 10);
            case YujiFx.KICK -> {
                PhotonFx.shockwave(at, ORANGE, 2.6, 14);
                PhotonFx.spark(at, 2.2, .6, ORANGE, 0xFFFFE8D8, "textures/effect/glow.png", true, e.life(), 8, 26, 0);
            }
            case YujiFx.DASH -> { // 残影线
                for (int i = 0; i < 8; i++) {
                    Vec3 p = at.add(dir.scale(i * 1.2));
                    PhotonFx.spark(p, .8, .4, ORANGE, 0xFF3A211A, "textures/effect/glow.png", true, e.life(), 6, 16, 0);
                }
            }
            case YujiFx.ROLL -> PhotonFx.ring(at, 1.4, .12, ORANGE, 0xFFFFF0E0, 6, e.life(), 10);
            case YujiFx.SWEEP -> { // 贴地气浪
                for (int i = 0; i < 3; i++) PhotonFx.ring(at.add(0, .1, 0), size * (.5 + i * .3), .18, ORANGE, 0xFFFFE0C0, 9, e.life(), 12);
            }
            case YujiFx.WALL -> PhotonFx.ring(at, 1.0, .14, ORANGE, 0xFFFFEFD8, 6, e.life(), 10);
            case YujiFx.WAVE -> PhotonFx.beam(at, at.add(dir.scale(size * 2.2)), 1.4, 16);
            case YujiFx.WAVE_HIT -> {
                PhotonFx.shockwave(at, ORANGE, 2.0, 12);
                PhotonFx.ring(at, 1.4, .18, ORANGE, 0xFFFFE8D8, 8, e.life(), 10);
            }
            case YujiFx.STOMP -> { // 地震：多重径向环 + 地面裂
                for (int i = 0; i < 3; i++) PhotonFx.ring(at.add(0, .05, 0), size * (.4 + i * .28), .22, ORANGE, 0xFFFFD8B0, 10, e.life(), 14);
                PhotonFx.shockwave(at, ORANGE, size * 1.4, 18);
            }
            case YujiFx.ELBOW -> PhotonFx.ring(at, 1.5, .16, ORANGE, 0xFFFFE6D6, 7, e.life(), 10);
            case YujiFx.KNEE -> {
                PhotonFx.spark(at, 1.4, .6, ORANGE, 0xFFFFF0E0, "textures/effect/glow.png", true, e.life(), 8, 24, 0);
                PhotonFx.ring(at, 1.2, .12, RED, 0xFFFFD0C0, 6, e.life(), 10);
            }
            case YujiFx.BLACK_FLASH -> { // 黑闪：黑芯 + 红边
                PhotonFx.spark(at, 2.6, .8, BLACK, 0xFFFF3A2A, "textures/effect/glow.png", true, e.life(), 10, 40, 0);
                PhotonFx.shockwave(at, RED, 3.0, 12);
                Client.shake(12);
            }
            default -> { }
        }
    }

    private YujiFxClient() { }
}
