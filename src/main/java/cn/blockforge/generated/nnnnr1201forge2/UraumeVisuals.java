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
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class UraumeVisuals {
    private record Visual(Network.Effect e, long born) { }
    private static final List<Visual> VISUALS = new ArrayList<>();

    public static void receive(Network.Effect e) {
        if (VISUALS.size() > 200) VISUALS.remove(0);
        VISUALS.add(new Visual(e, System.currentTimeMillis()));
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
                        Client.ribbon(b, ps, at, at.add(d.scale(size * (.5 + t))), .12, color, cam);
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
