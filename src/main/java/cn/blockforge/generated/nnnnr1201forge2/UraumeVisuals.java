package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 里梅冰系六招的自绘特效（r293）：纯顶点通路，白色/冰蓝为主。 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths", value = Dist.CLIENT)
public final class UraumeVisuals {
    private record Visual(Network.Effect e, long born) { }
    private static final List<Visual> VISUALS = new ArrayList<>();

    /** 冰蓝发光层（r301）：PhotonFx 加法混合只做"光"，几何形体仍由下面的顶点通路负责。 */
    private static final int ICE = 0xFFA8EEFF, DEEP = 0xFF3FA8E0;
    private static final int BLOOM = 0xFFCFF6FF;

    public static void receive(Network.Effect e) {
        if (VISUALS.size() > 200) VISUALS.remove(0);
        VISUALS.add(new Visual(e, System.currentTimeMillis()));
        burst(e);
    }

    private static void burst(Network.Effect e) {
        Vec3 at = e.position();
        float size = Math.max(.5f, e.size());
        Vec3 dir = e.direction().lengthSqr() < .01 ? new Vec3(0, 0, 1) : e.direction().normalize();
        switch (e.kind()) {
            case UraumeFx.FROST -> {          // 霜凪：向前喷的冰雾团 + 地面结霜光
                for (int i = 0; i < 6; i++) {
                    Vec3 p = at.add(dir.scale(size * (.25 + i * .14)));
                    PhotonFx.spark(p, size * .55, .55, ICE, BLOOM,
                        "textures/effect/glow.png", true, e.life(), 10, 34, -0.6);
                }
                PhotonFx.ring(at.add(0, .05, 0), size * .8, .16, ICE, BLOOM, 6, e.life(), 12);
            }
            case UraumeFx.ICEFALL -> {        // 直瀑：天降冰柱 + 落地冲击环
                PhotonFx.beam(at.add(0, 14, 0), at, 1.4 * size, Math.min(20, e.life()));
                PhotonFx.shockwave(at, ICE, size * 1.5, 10);
                PhotonFx.spark(at, size * .8, .7, ICE, BLOOM, "textures/effect/glow.png", true, e.life(), 10, 30, 0);
            }
            case UraumeFx.PRISON -> {         // 千手冰牢：地面法阵环 + 竖起的光柱
                PhotonFx.ring(at, size * 1.1, .22, ICE, BLOOM, 5, e.life(), 14);
                PhotonFx.ring(at, size * .6, .16, ICE, BLOOM, 3, e.life(), 12);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    Vec3 base = at.add(Math.cos(a) * size * .6, 0, Math.sin(a) * size * .6);
                    PhotonFx.spark(base, .5, .9, ICE, BLOOM, "textures/effect/glow.png", true, e.life(), 12, 20, 0);
                }
            }
            case UraumeFx.STORM -> {          // 急冻风暴：发光外壳 + 双层旋转环
                PhotonFx.sphere(at, size * .95, .55, ICE, BLOOM, "textures/effect/glow.png", true, e.life(), 20, 6, .9);
                PhotonFx.ring(at.add(0, 1.2, 0), size * .8, .2, ICE, BLOOM, 8, e.life(), 16);
                PhotonFx.ring(at.add(0, 3.4, 0), size * .55, .16, ICE, BLOOM, -6, e.life(), 16);
            }
            case UraumeFx.DRAGON -> {         // 冰龙：贯穿光带 + 沿途冰晶爆散
                PhotonFx.beam(at, at.add(dir.scale(size)), 1.6, Math.min(30, e.life()));
                for (int i = 1; i <= 6; i++) {
                    Vec3 p = at.add(dir.scale(size * i / 7.0));
                    PhotonFx.spark(p, 1.4, .8, ICE, BLOOM, "textures/effect/glow.png", true, e.life(), 12, 18, 0);
                }
            }
            case UraumeFx.ICEAGE -> {         // 大冰河时代：白蓝色球壳扩张 + 多重冲击环
                PhotonFx.domain(at, size * .95, Math.min(40, e.life()));
                PhotonFx.shockwave(at, ICE, size * 1.15, 18);
                PhotonFx.shockwave(at, 0xFFEAFBFF, size * .7, 12);
            }
            default -> { }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (VISUALS.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = System.currentTimeMillis();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (var it = VISUALS.iterator(); it.hasNext(); ) {
            Visual v = it.next();
            long age = now - v.born;
            long life = v.e.life() * 50L;
            if (age > life) { it.remove(); continue; }
            int kind = v.e.kind();
            float t = Math.min(1f, age / (float) life);
            Vec3 at = v.e.position();
            float size = Math.max(.4f, v.e.size());
            int alpha = (int) (200 * (1 - t));
            int color = (Math.min(200, alpha) << 24) | 0xBFE8FF;
            int deep = (Math.min(220, alpha) << 24) | 0x6FC8FF;
            switch (kind) {
                case UraumeFx.FROST -> {          // 霜凪：扇形白蓝雾墙 + 冰刺
                    Vec3 dir = v.e.direction().lengthSqr() < .01 ? new Vec3(0, 0, 1) : v.e.direction().normalize();
                    for (int i = 0; i < 26; i++) {
                        double a = (i - 12.5) / 12.5 * .7;
                        Vec3 d = dir.yRot((float) a);
                        Vec3 tip = at.add(d.scale(size * (.5 + t)));
                        Client.ribbon(b, ps, at, tip, .26, deep, cam);
                        Client.ribbon(b, ps, at, tip, .12, color, cam);
                    }
                }
                case UraumeFx.ICEFALL -> {        // 直瀑：高处垂直光柱 + 落点爆散
                    Client.ribbon(b, ps, at.add(0, 12, 0), at, .5 * size, color, cam);
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4;
                        Client.ribbon(b, ps, at, at.add(Math.cos(a) * size, .6, Math.sin(a) * size), .08, deep, cam);
                    }
                }
                case UraumeFx.PRISON -> {         // 千手冰牢：地面六芒环 + 竖起的冰手
                    Client.ring(b, ps, at, size * (0.6 + .4 * t), .18, color, cam);
                    for (int i = 0; i < 6; i++) {
                        double a = i * Math.PI / 3;
                        Vec3 base = at.add(Math.cos(a) * size * .6, 0, Math.sin(a) * size * .6);
                        Client.ribbon(b, ps, base, base.add(Math.cos(a) * .3, size * .8, Math.sin(a) * .3), .14, deep, cam);
                    }
                }
                case UraumeFx.STORM -> {          // 急冻风暴：双层螺旋
                    for (int i = 0; i < 60; i++) {
                        double a = i * .45 + t * 12, r = size * (.3 + .7 * i / 60.0);
                        Vec3 p0 = at.add(Math.cos(a) * r, i * .12, Math.sin(a) * r);
                        Vec3 p1 = at.add(Math.cos(a + .35) * r, (i + 1) * .12, Math.sin(a + .35) * r);
                        Client.ribbon(b, ps, p0, p1, .07, color, cam);
                    }
                }
                case UraumeFx.DRAGON -> {         // 冰龙：蛇形长条
                    Vec3 dir = v.e.direction().lengthSqr() < .01 ? new Vec3(0, 0, 1) : v.e.direction().normalize();
                    Vec3 side = new Vec3(-dir.z, 0, dir.x);
                    for (int i = 0; i < 40; i++) {
                        double tt = i / 40.0;
                        Vec3 p0 = at.add(dir.scale(tt * size)).add(side.scale(Math.sin(tt * 6 + t * 8) * .8)).add(0, .5 + Math.cos(tt * 6) * .3, 0);
                        Vec3 p1 = at.add(dir.scale((i + 1) / 40.0 * size)).add(side.scale(Math.sin((i + 1) / 40.0 * 6 + t * 8) * .8)).add(0, .5 + Math.cos((i + 1) / 40.0 * 6) * .3, 0);
                        Client.ribbon(b, ps, p0, p1, .22, color, cam);
                    }
                }
                case UraumeFx.ICEAGE -> {         // 大冰河：扩张白球环
                    for (int i = 0; i < 5; i++) {
                        double r = size * Math.min(1, t * 1.2) * (1 - i * .12);
                        Client.ring(b, ps, at.add(0, i * .3, 0), r, .3, color, cam);
                    }
                }
                default -> { }
            }
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    private UraumeVisuals() { }
}
