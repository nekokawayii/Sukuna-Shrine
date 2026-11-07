package cn.blockforge.generated.nnnnr1201forge2;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 五条悟分支六式的表现层（r314）：顶点通路做形体（黑/实色）+ PhotonFx 加法层做光。 */
@Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths", value = Dist.CLIENT)
public final class GojoBranchClient {
    private static final int BLUE = 0xFF6FC8FF, RED = 0xFFFF6A5A, VIOLET = 0xFFB57BFF, WHITE = 0xFFEAF7FF;
    private static final int DARK = 0xF00A0618, DARK2 = 0xE6140B2E;
    private record V(Network.Effect e, long born) { }
    private static final List<V> VISUALS = new ArrayList<>();

    public static void receive(Network.Effect e) {
        if (VISUALS.size() > 128) VISUALS.remove(0);
        VISUALS.add(new V(e, System.currentTimeMillis()));
        burst(e);
        if (e.kind() == GojoBranches.FX_SINGULARITY && e.life() <= 25) Client.shake(7); // 坍缩瞬间：屏幕向内一扯
    }

    private static void burst(Network.Effect e) {
        Vec3 at = e.position();
        float size = Math.max(.4f, e.size());
        Vec3 dir = e.direction().lengthSqr() < .01 ? new Vec3(0, 0, 1) : e.direction().normalize();
        switch (e.kind()) {
            case GojoBranches.FX_SINGULARITY -> {
                PhotonFx.sphere(at, size * .5, .8, BLUE, 0xFFCFF3FF, "textures/effect/glow.png", true, e.life(), 18, 34, -.5);
                PhotonFx.ring(at, size * 1.15, .2, BLUE, 0xFFE6FAFF, 9, e.life(), 14);
            }
            case GojoBranches.FX_TEAR -> {
                for (int i = 0; i < 7; i++) {
                    double a = i * Math.PI * 2 / 7;
                    PhotonFx.spark(at, size * .9, .5, BLUE, 0xFFD6F4FF, "textures/effect/glow.png", true, e.life(), 10, 26,
                        a);
                }
            }
            case GojoBranches.FX_METEOR -> PhotonFx.spark(at, 1.5, .5, BLUE, 0xFFDCF7FF, "textures/effect/glow.png", true, e.life(), 9, 30, 0);
            case GojoBranches.FX_HEAVY -> {
                PhotonFx.shockwave(at, RED, size * 1.8, 14);
                PhotonFx.spark(at.add(dir), 2.6, 1.0, RED, 0xFFFFD8CE, "textures/effect/glow.png", true, e.life(), 10, 34, 0);
            }
            case GojoBranches.FX_TURBULENCE -> {
                PhotonFx.beam(at, at.add(dir.scale(size * 3)), 1.8, Math.min(24, e.life()));
                PhotonFx.ring(at, 2.4, .18, RED, 0xFFFFE0D0, 9, e.life(), 12);
            }
            case GojoBranches.FX_BRAIN -> {
                PhotonFx.sphere(at, .9, .5, VIOLET, WHITE, "textures/effect/glow.png", true, e.life(), 16, 34, .7);
                PhotonFx.ring(at.add(0, .8, 0), 1.2, .14, VIOLET, WHITE, -7, e.life(), 12);
            }
            default -> { }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || VISUALS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        Vec3 cam = e.getCamera().getPosition();
        com.mojang.blaze3d.vertex.PoseStack ps = e.getPoseStack();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        com.mojang.blaze3d.vertex.BufferBuilder b = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        b.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        int drawn = 0;
        for (var it = VISUALS.iterator(); it.hasNext(); ) {
            V v = it.next();
            Network.Effect fx = v.e;
            float age = (now - v.born) / 50f;
            float life = Math.max(1, fx.life());
            if (age > life) { it.remove(); continue; }
            drawn++;
            float t = age / life;
            Vec3 at = fx.position();
            Vec3 dir = fx.direction().lengthSqr() < .01 ? new Vec3(0, 0, 1) : fx.direction().normalize();
            float size = Math.max(.4f, fx.size());
            switch (fx.kind()) {
                case GojoBranches.FX_SINGULARITY -> blackHole(b, ps, at, size, t, cam);
                case GojoBranches.FX_TEAR -> web(b, ps, at, size, t, cam);
                case GojoBranches.FX_METEOR -> meteor(b, ps, at, dir, t, cam);
                case GojoBranches.FX_HEAVY -> blast(b, ps, at, dir, size, t, cam);
                case GojoBranches.FX_TURBULENCE -> turbulence(b, ps, at, dir, size, t, cam);
                case GojoBranches.FX_BRAIN -> brain(b, ps, at, size, t, cam);
                default -> { }
            }
        }
        ps.popPose();
        com.mojang.blaze3d.vertex.Tesselator.getInstance().end();
    }

    /** 黑蓝微型黑洞：三层黑环 + 向内收的螺旋吸积线 + 中心纯黑盘。 */
    private static void blackHole(com.mojang.blaze3d.vertex.BufferBuilder b,
                                  com.mojang.blaze3d.vertex.PoseStack ps, Vec3 at, float size, float t, Vec3 cam) {
        float grow = .6f + t * .6f;
        Client.ring(b, ps, at, size * .45 * grow, .30, DARK, cam);
        Client.ring(b, ps, at, size * .70 * grow, .16, DARK2, cam);
        Client.ring(b, ps, at, size * 1.05 * grow, .07, 0x90402080, cam);
        for (int i = 0; i < 10; i++) {
            double a0 = i * Math.PI / 5 + t * 5.0, a1 = a0 + 1.1;
            double r0 = size * 1.5, r1 = size * (.6 + .1 * Math.sin(t * 9 + i));
            Vec3 p0 = at.add(Math.cos(a0) * r0, Math.sin(t * 4 + i) * .25, Math.sin(a0) * r0);
            Vec3 p1 = at.add(Math.cos(a1) * r1, 0, Math.sin(a1) * r1);
            Client.ribbon(b, ps, p0, p1, .06, 0xCC6FC8FF, cam);
        }
        // 中心黑盘（俯视/侧视各一片，保证任何角度都是黑的）
        Client.quad(b, ps, at.add(-size * .35, 0, -size * .35), at.add(size * .35, 0, -size * .35),
            at.add(size * .35, 0, size * .35), at.add(-size * .35, 0, size * .35), 0xFF000000);
        Client.quad(b, ps, at.add(0, -size * .35, -size * .35), at.add(0, size * .35, -size * .35),
            at.add(0, size * .35, size * .35), at.add(0, -size * .35, size * .35), 0xFF000000);
    }

    /** 引力蛛网：从命中点放射 8 条蓝线，先缠住再向外猛撕。 */
    private static void web(com.mojang.blaze3d.vertex.BufferBuilder b,
                            com.mojang.blaze3d.vertex.PoseStack ps, Vec3 at, float size, float t, Vec3 cam) {
        double reach = size * (t < .45 ? .5 + t : .72 + (t - .45) * 2.2);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + t * 2.2;
            Vec3 edge = at.add(Math.cos(a) * reach, Math.sin(a * 3) * .5 * (1 - t), Math.sin(a) * reach);
            Client.ribbon(b, ps, at, edge, .05, 0xB06FC8FF, cam);
            Vec3 mid = at.add(Math.cos(a + .5) * reach * .8, .4, Math.sin(a + .5) * reach * .8);
            Client.ribbon(b, ps, edge, mid, .03, 0x80A8E8FF, cam);
        }
        Client.quad(b, ps, at.add(-.2, .6, -.2), at.add(.2, .6, -.2), at.add(.2, 1.0, .2), at.add(-.2, 1.0, .2), 0xFF000000);
    }

    /** 流星：小蓝黑弹体 + 后面拖一截尾迹。 */
    private static void meteor(com.mojang.blaze3d.vertex.BufferBuilder b,
                               com.mojang.blaze3d.vertex.PoseStack ps, Vec3 at, Vec3 dir, float t, Vec3 cam) {
        Client.quad(b, ps, at.add(-.22, -.22, -.22), at.add(-.22, .22, .22), at.add(.22, .22, .22), at.add(.22, -.22, -.22), 0xFF000000);
        for (int i = 1; i <= 5; i++) {
            Vec3 tail = at.subtract(dir.scale(i * .35));
            Client.ribbon(b, ps, at, tail, .07 * (1 - i / 6f), 0xB06FC8FF, cam);
        }
    }

    /** 重压：红色气浪三层 + 拳前竖直冲击 + 音爆圆。 */
    private static void blast(com.mojang.blaze3d.vertex.BufferBuilder b,
                              com.mojang.blaze3d.vertex.PoseStack ps, Vec3 at, Vec3 dir, float size, float t, Vec3 cam) {
        for (int i = 0; i < 3; i++) {
            float r = size * (.6f + t * (1.2f + i * .5f));
            Client.ring(b, ps, at, r, .18 - i * .04, i == 0 ? 0xFFFF5A4A : 0xA0FF8A70, cam);
        }
        Vec3 tip = at.add(dir.scale(size * .8));
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            Client.ribbon(b, ps, at, tip.add(Math.cos(a) * size * .5, Math.sin(a) * size * .3, Math.sin(a) * size * .5),
                .06, 0xB0FFD0C0, cam);
        }
    }

    /** 苍红乱流：五道赤红流星；最后一发（size 更大）额外升起红色蘑菇云。 */
    private static void turbulence(com.mojang.blaze3d.vertex.BufferBuilder b,
                                   com.mojang.blaze3d.vertex.PoseStack ps, Vec3 at, Vec3 dir, float size, float t, Vec3 cam) {
        for (int i = 0; i < 5; i++) {
            Vec3 p = at.add(dir.scale(i * 1.2)).add(0, Math.sin(t * 8 + i) * .2, 0);
            Client.ribbon(b, ps, p, p.add(dir.scale(1.4)), .10, 0xC0FF5A44, cam);
        }
        if (size >= 3.5f) {
            float r = size * (.5f + t * 1.4f);
            Client.ring(b, ps, at.add(0, size * (1 + t), 0), r, .22, 0xC0FF4436, cam);
            Client.ring(b, ps, at.add(0, size * (1.3 + t), 0), r * .7, .16, 0xA0FF7A60, cam);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                Vec3 base = at.add(Math.cos(a) * .3, 0, Math.sin(a) * .3);
                Client.ribbon(b, ps, base, base.add(0, size * (1 + t), 0), .18, 0x90FF6A52, cam);
            }
        }
    }

    /** 局部脑死：指尖微缩星空 + 头顶爆出的乱码方片与星光。 */
    private static void brain(com.mojang.blaze3d.vertex.BufferBuilder b,
                              com.mojang.blaze3d.vertex.PoseStack ps, Vec3 at, float size, float t, Vec3 cam) {
        Client.ring(b, ps, at, size * .8, .12, 0xFFB57BFF, cam);
        Client.ring(b, ps, at, size * 1.1, .06, 0x90EAF7FF, cam);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8, rr = size * (.4 + (i % 3) * .3);
            Vec3 p = at.add(Math.cos(a) * rr, .6 + Math.sin(t * 7 + i) * .6, Math.sin(a) * rr);
            Client.quad(b, ps, p.add(-.07, -.07, 0), p.add(-.07, .07, 0), p.add(.07, .07, 0), p.add(.07, -.07, 0),
                i % 2 == 0 ? 0xC0EAF7FF : 0xC0B57BFF);
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + t;
            Vec3 p = at.add(Math.cos(a) * size * .9, 1.4 + (i % 4) * .3, Math.sin(a) * size * .9);
            Client.quad(b, ps, p.add(-.12, -.04, 0), p.add(-.12, .04, 0), p.add(.12, .04, 0), p.add(.12, -.04, 0), 0xE0FF4A6E);
        }
    }

    private GojoBranchClient() { }
}
