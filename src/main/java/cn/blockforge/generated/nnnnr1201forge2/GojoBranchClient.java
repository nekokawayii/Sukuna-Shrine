package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.phys.Vec3;

/** 五条悟分支六式的发光层（r302）：收到服务端兜底时用 PhotonFx 加法混合打一层光。 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "sukuna", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class GojoBranchClient {
    private static final int BLUE = 0xFF6FC8FF, RED = 0xFFFF6A5A, VIOLET = 0xFFB57BFF, WHITE = 0xFFEAF7FF;

    public static void receive(Network.Effect e) {
        Vec3 at = e.position();
        float size = Math.max(.4f, e.size());
        Vec3 dir = e.direction().lengthSqr() < .01 ? new Vec3(0, 0, 1) : e.direction().normalize();
        switch (e.kind()) {
            case GojoBranches.FX_SINGULARITY -> {
                PhotonFx.sphere(at, size * .55, .8, BLUE, 0xFFCFF3FF, "textures/effect/glow.png", true, e.life(), 16, 30, -.4);
                PhotonFx.ring(at, size * 1.2, .22, BLUE, 0xFFE6FAFF, 8, e.life(), 12);
            }
            case GojoBranches.FX_TEAR -> {
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    PhotonFx.ring(at, size * (.5 + i * .08), .12, BLUE, 0xFFD6F4FF, -5 + i, e.life(), 10);
                }
            }
            case GojoBranches.FX_METEOR -> PhotonFx.spark(at, 1.4, .55, BLUE, 0xFFDCF7FF,
                "textures/effect/glow.png", true, e.life(), 8, 26, 0);
            case GojoBranches.FX_HEAVY -> {
                PhotonFx.shockwave(at, RED, size * 1.6, 12);
                PhotonFx.spark(at.add(dir), 2.2, .9, RED, 0xFFFFD8CE, "textures/effect/glow.png", true, e.life(), 10, 30, 0);
            }
            case GojoBranches.FX_TURBULENCE -> {
                PhotonFx.beam(at, at.add(dir.scale(size * 3)), 1.8, Math.min(24, e.life()));
                PhotonFx.ring(at, 2.2, .2, RED, 0xFFFFE0D0, 9, e.life(), 12);
            }
            case GojoBranches.FX_BRAIN -> {
                PhotonFx.sphere(at, .8, .5, VIOLET, WHITE, "textures/effect/glow.png", true, e.life(), 14, 30, .6);
                PhotonFx.ring(at.add(0, 1.2, 0), 1.1, .14, VIOLET, WHITE, -7, e.life(), 10);
            }
            default -> { }
        }
    }

    private GojoBranchClient() { }
}
