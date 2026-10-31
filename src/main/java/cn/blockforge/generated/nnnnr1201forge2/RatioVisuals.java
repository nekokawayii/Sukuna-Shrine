package cn.blockforge.generated.nnnnr1201forge2;

import character.nanami.NanamiFx;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 十划咒法的表现层：
 * ①敌人身上那条纵向 7:3 数轴（10 段、第 7 段金色、70% 处一条亮线，朝向镜头）；
 * ②弱点暴击的黑金十字火花 + 屏幕轻震；
 * ③强打的三道黑金刀光、隙的细线、瓦落瓦落的金色钟表盘、加班的黑金火焰与第一人称黑边。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class RatioVisuals {
    private static final int CORE = 0xFFFFFF, GOLD = 0xD8B24A, BLACK = 0x0B0A08;
    private static final String TEX_GLOW = "textures/effect/glow.png";

    private record Axis(int entityId, long until, long since, boolean stunned, boolean overtime) {}
    private record Arc(Vec3 pos, Vec3 dir, double range, long born, int life) {}
    private record Line(Vec3 from, Vec3 to, long born, int life) {}
    private record Clock(Vec3 pos, double radius, boolean counter, long born, int life) {}
    private record Crit(Vec3 pos, long born, int life) {}
    private record Overtime(int playerId, long until) {}

    private static final Map<Integer, Axis> AXES = new HashMap<>();
    private static final List<Arc> ARCS = new ArrayList<>();
    private static final List<Line> LINES = new ArrayList<>();
    private static final List<Clock> CLOCKS = new ArrayList<>();
    private static final List<Crit> CRITS = new ArrayList<>();
    private static final Map<Integer, Overtime> OVERTIMES = new HashMap<>();
    private static Object world;

    public static void receive(Network.Effect e) {
        var mc = Minecraft.getInstance();
        long now = now(mc);
        switch (e.kind()) {
            case NanamiFx.AXIS -> {
                if (e.size() > 0) {
                    boolean stunned = (e.seed() & 1) != 0, overtime = (e.seed() & 2) != 0;
                    Axis old = AXES.get(e.owner());
                    AXES.put(e.owner(), new Axis(e.owner(), now + Math.max(1, e.life()),
                        old == null ? now : old.since(), stunned, overtime));
                } else AXES.remove(e.owner());
            }
            case NanamiFx.SMASH -> ARCS.add(new Arc(e.position(), e.direction().normalize(), e.size(), now, 16));
            case NanamiFx.GAP -> LINES.add(new Line(e.position(), e.direction(), now, 12));
            case NanamiFx.CLOCK -> {
                CLOCKS.add(new Clock(e.position(), Math.abs(e.size()) <= 0 ? 1.6 : Math.abs(e.size()), e.size() < 0, now, Math.max(10, e.life())));
                if (PhotonFx.ready()) {
                    PhotonFx.ring(e.position().add(0, 1.0, 0), 1.8, 0.5, 0xE0FFE9A8,
                        e.size() < 0 ? 0xFFFFF0B0 : 0xFFD8B24A, 26, 6, 10);
                    if (e.size() < 0) Client.shake(4);
                }
            }
            case NanamiFx.CRIT -> {
                CRITS.add(new Crit(e.position(), now, 14));
                Client.shake(3);
                if (PhotonFx.ready()) {
                    PhotonFx.spark(e.position(), 1.5, 1.0, 0xFFFFF3C4, 0xFFD8B24A, TEX_GLOW, true, 6, 10, 36, 0.4);
                    PhotonFx.shockwave(e.position(), 0xFFD8B24A, 2.6, 8);
                }
            }
            case NanamiFx.OVERTIME -> {
                if (e.size() > 0) OVERTIMES.put(e.owner(), new Overtime(e.owner(), now + Math.max(1, e.life())));
                else OVERTIMES.remove(e.owner());
            }
            default -> { }
        }
    }

    private static long now(Minecraft mc) { return mc.level == null ? 0 : mc.level.getGameTime(); }

    @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (world != mc.level) {
            world = mc.level;
            clear();
            return;
        }
        long now = now(mc);
        AXES.values().removeIf(a -> a.until() <= now);
        ARCS.removeIf(a -> now - a.born() > a.life());
        LINES.removeIf(a -> now - a.born() > a.life());
        CLOCKS.removeIf(a -> now - a.born() > a.life());
        CRITS.removeIf(a -> now - a.born() > a.life());
        OVERTIMES.values().removeIf(o -> o.until() <= now);
        var p = mc.player;
        if (p == null || !PhotonFx.ready()) return;
        if (OVERTIMES.containsKey(p.getId()) && now % 4 == 0) {
            Vec3 pos = p.position().add(0, p.getBbHeight() * 0.55, 0);
            PhotonFx.lines(pos, 1.1, 0.05, 0.8, 0xFFFFE9A8, 0xFFB98A2E, 0, 0, 12, 5, 7, false);
            PhotonFx.spark(pos, 0.9, 0.6, 0xFFFFF3C4, 0xFF8A6A20, TEX_GLOW, true, 4, 6, 12, 0.3);
        }
    }

    public static void clear() {
        AXES.clear();
        ARCS.clear();
        LINES.clear();
        CLOCKS.clear();
        CRITS.clear();
        OVERTIMES.clear();
    }

    // ------------------------------------------------------------ 世界内绘制
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (AXES.isEmpty() && ARCS.isEmpty() && LINES.isEmpty() && CLOCKS.isEmpty() && CRITS.isEmpty()) return;
        long now = now(mc);
        PoseStack ps = e.getPoseStack();
        Vec3 cam = e.getCamera().getPosition();
        float partial = e.getPartialTick();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Arc arc : ARCS) drawArc(b, ps, arc, cam, now);
        for (Line line : LINES) drawLine(b, ps, line, cam, now);
        for (Clock clock : CLOCKS) drawClock(b, ps, clock, cam, now);
        for (Crit crit : CRITS) drawCrit(b, ps, crit, cam, now);
        BufferUploader.drawWithShader(b.end());
        // 7:3 数轴：画在最上层（关深度测试），直接盖过生物与地形，永远不会被身体挡掉
        if (!AXES.isEmpty()) {
            RenderSystem.disableDepthTest();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (Axis axis : AXES.values()) drawAxis(b, ps, mc, axis, cam, partial, now);
            BufferUploader.drawWithShader(b.end());
            RenderSystem.enableDepthTest();
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    /** 7:3 数轴：10 段刻度 + 第 7 段金色 + 70% 处一条亮线，整体朝向镜头。 */
    private static void drawAxis(BufferBuilder b, PoseStack ps, Minecraft mc, Axis axis, Vec3 cam,
                                 float partial, long now) {
        Entity entity = mc.level.getEntity(axis.entityId());
        if (!(entity instanceof LivingEntity living)) return;
        Vec3 feet = living.getPosition(partial);
        if (feet.distanceToSqr(cam) > 48 * 48) return;
        // 上下端严格取碰撞箱：最上端就是碰撞箱最上端，下端同理
        AABB box = living.getBoundingBox();
        Vec3 shift = feet.subtract(living.position());
        double bottom = box.minY + shift.y, top = box.maxY + shift.y;
        double height = Math.max(0.2, top - bottom);
        // 太近（相机几乎贴在身上）或已经跑到镜头后面就不画：否则近裁剪面会把面片撕成糊屏的光带
        if (feet.distanceToSqr(cam) < 2.0) return;
        Vec3 center0 = new Vec3(box.getCenter().x + shift.x, feet.y + height * 0.5, box.getCenter().z + shift.z);
        Vec3 cameraLook = new Vec3(mc.gameRenderer.getMainCamera().getLookVector());
        if (cameraLook.dot(center0.subtract(cam).normalize()) < 0.15) return;
        float appear = Math.min(1f, (now - axis.since() + partial) / 12f);   // 缓慢浮现
        int baseAlpha = (int) (235 * appear);
        // 朝向镜头：把整条轴往镜头方向推 0.55 格，避免埋进身体里
        Vec3 toward = cam.subtract(feet);
        Vec3 flat = new Vec3(toward.x, 0, toward.z);
        if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
        Vec3 center = new Vec3(box.getCenter().x + shift.x, bottom, box.getCenter().z + shift.z)
            .add(flat.normalize().scale(0.5));
        Vec3 right = new Vec3(-flat.z, 0, flat.x).normalize();
        if (right.lengthSqr() < 1e-4) right = new Vec3(1, 0, 0);
        Vec3 top_ = center.add(0, height, 0);
        // ① 先铺一层黑色描边（宽一圈），保证在白墙/天空/亮特效上也能看清
        Client.ribbon(b, ps, center, top_, 0.075, argb(BLACK, (int) (baseAlpha * 0.85)), cam);
        // ② 主竖线：金色 + 白色内芯
        Client.ribbon(b, ps, center, top_, 0.045, argb(GOLD, baseAlpha), cam);
        Client.ribbon(b, ps, center, top_, 0.016, argb(CORE, (int) (baseAlpha * 0.9)), cam);
        // ③ 第 7 段（60%~70%）金色区：两层，外面一层暗底
        double y0 = height * 0.60, y1 = height * 0.70;
        Vec3 a = center.add(0, y0, 0), c = center.add(0, y1, 0);
        double w = 0.21;
        Client.quad(b, ps, a.subtract(right.scale(w + 0.03)), a.add(right.scale(w + 0.03)),
            c.add(right.scale(w + 0.03)), c.subtract(right.scale(w + 0.03)), argb(BLACK, (int) (baseAlpha * 0.5)));
        Client.quad(b, ps, a.subtract(right.scale(w)), a.add(right.scale(w)),
            c.add(right.scale(w)), c.subtract(right.scale(w)), argb(GOLD, (int) (baseAlpha * 0.8)));
        // ④ 刻度：每 10% 一格，第 5、10 格更长；金色第 7 格再加长
        for (int i = 0; i <= 10; i++) {
            double y = height * (i / 10.0);
            double length = i == 7 ? 0.44 : i % 5 == 0 ? 0.34 : 0.22;
            Vec3 at = center.add(0, y, 0);
            Client.ribbon(b, ps, at, at.add(right.scale(length)), i == 7 ? 0.05 : 0.03,
                argb(BLACK, (int) (baseAlpha * 0.7)), cam);
            Client.ribbon(b, ps, at, at.add(right.scale(length)), i == 7 ? 0.03 : 0.016,
                argb(i == 7 ? GOLD : CORE, (int) (baseAlpha * (i == 7 ? 1.0 : 0.7))), cam);
        }
        // ⑤ 70% 那条分割线：最显眼的一条（黑底 → 金色光晕 → 白色芯，还带呼吸）
        Vec3 lineAt = center.add(0, height * 0.70, 0);
        double pulse = 0.9 + 0.1 * Math.sin(now * 0.35);
        Client.ribbon(b, ps, lineAt.subtract(right.scale(0.55)), lineAt.add(right.scale(0.55)),
            0.16, argb(BLACK, (int) (baseAlpha * 0.85)), cam);
        Client.ribbon(b, ps, lineAt.subtract(right.scale(0.52)), lineAt.add(right.scale(0.52)),
            (float) (0.11 * pulse), argb(GOLD, baseAlpha), cam);
        Client.ribbon(b, ps, lineAt.subtract(right.scale(0.5)), lineAt.add(right.scale(0.5)),
            (float) (0.045 * pulse), argb(0xFFF3C4, baseAlpha), cam);
        Client.ribbon(b, ps, lineAt.subtract(right.scale(0.48)), lineAt.add(right.scale(0.48)),
            (float) (0.018 * pulse), argb(CORE, baseAlpha), cam);
        // 眩晕 / 加班时把判定窗口画得更宽（提示弱点被放大）
        double widen = 0.0;
        if (axis.stunned()) widen += 0.22;
        if (axis.overtime()) widen += 0.11;
        if (widen > 0) {
            Client.ribbon(b, ps, lineAt.add(0, -widen, 0).subtract(right.scale(0.3)),
                lineAt.add(0, widen, 0).subtract(right.scale(0.3)), 0.02, argb(GOLD, (int) (baseAlpha * 0.5)), cam);
        }
    }

    /** 强打：三道黑金刀光交叉。 */
    private static void drawArc(BufferBuilder b, PoseStack ps, Arc arc, Vec3 cam, long now) {
        float t = (now - arc.born()) / (float) arc.life();
        if (t < 0 || t > 1) return;
        int alpha = (int) (230 * (1 - t));
        Vec3 dir = arc.dir();
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        for (int k = -1; k <= 1; k++) {
            double tilt = k * 0.55 + (k == 0 ? 0 : Math.sin(now * 0.5) * 0.05);
            Vec3 axis = dir.scale(Math.cos(tilt)).add(0, Math.sin(tilt), 0).normalize();
            Vec3 prev = arc.pos().add(0, 1.0, 0).subtract(axis.scale(arc.range() * 0.9));
            for (int i = 1; i <= 8; i++) {
                Vec3 next = arc.pos().add(0, 1.0, 0).subtract(axis.scale(arc.range() * (0.9 - i * 0.225)));
                Client.ribbon(b, ps, prev, next, 0.14, argb(BLACK, alpha), cam);
                Client.ribbon(b, ps, prev, next, 0.05, argb(GOLD, alpha), cam);
                prev = next;
            }
        }
    }

    /** 隙：一条极细的黑金直线。 */
    private static void drawLine(BufferBuilder b, PoseStack ps, Line line, Vec3 cam, long now) {
        float t = (now - line.born()) / (float) line.life();
        if (t < 0 || t > 1) return;
        int alpha = (int) (235 * (1 - t));
        Client.ribbon(b, ps, line.from(), line.to(), 0.03, argb(GOLD, alpha), cam);
        Client.ribbon(b, ps, line.from(), line.to(), 0.012, argb(CORE, alpha), cam);
    }

    /** 瓦落瓦落：金色钟表盘虚影（守护时慢慢转，反击时炸一圈）。 */
    private static void drawClock(BufferBuilder b, PoseStack ps, Clock clock, Vec3 cam, long now) {
        float t = (now - clock.born()) / (float) clock.life();
        if (t < 0 || t > 1) return;
        int alpha = (int) ((clock.counter() ? 230 : 170) * (1 - t));
        Vec3 center = clock.pos().add(0, 1.0, 0);
        double radius = clock.radius() * (clock.counter() ? 1.0 + t * 1.2 : 1.0);
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments, a1 = (i + 1) * Math.PI * 2 / segments;
            Vec3 p0 = center.add(Math.cos(a0) * radius, Math.sin(a0) * radius, 0);
            Vec3 p1 = center.add(Math.cos(a1) * radius, Math.sin(a1) * radius, 0);
            Client.ribbon(b, ps, p0, p1, 0.05, argb(GOLD, alpha), cam);
        }
        // 12 个刻度
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            Vec3 at = center.add(Math.cos(a) * radius, Math.sin(a) * radius, 0);
            Client.ribbon(b, ps, at, at.add(Math.cos(a) * 0.14, Math.sin(a) * 0.14, 0), 0.03, argb(CORE, alpha), cam);
        }
        // 两根指针
        double spin = (now - clock.born()) * (clock.counter() ? 0.55 : 0.12);
        Client.ribbon(b, ps, center, center.add(Math.cos(spin) * radius * 0.7, Math.sin(spin) * radius * 0.7, 0),
            0.045, argb(CORE, alpha), cam);
        Client.ribbon(b, ps, center, center.add(Math.cos(spin * 0.4 + 1.2) * radius * 0.45,
            Math.sin(spin * 0.4 + 1.2) * radius * 0.45, 0), 0.035, argb(GOLD, alpha), cam);
    }

    /** 弱点暴击：大量黑金十字火花。 */
    private static void drawCrit(BufferBuilder b, PoseStack ps, Crit crit, Vec3 cam, long now) {
        float t = (now - crit.born()) / (float) crit.life();
        if (t < 0 || t > 1) return;
        int alpha = (int) (255 * (1 - t) * (1 - t));
        Random rnd = new Random(crit.born() * 7919L + (long) (crit.pos().x * 100));
        for (int i = 0; i < 14; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = rnd.nextDouble() * 1.5 * (0.4 + t);
            double size = 0.16 + rnd.nextDouble() * 0.32;
            Vec3 at = crit.pos().add(Math.cos(a) * d, (rnd.nextDouble() - 0.5) * 0.8, Math.sin(a) * d);
            Vec3 armX = new Vec3(size, 0, 0), armZ = new Vec3(0, 0, size);
            Client.ribbon(b, ps, at.subtract(armX), at.add(armX), 0.035, argb(i % 2 == 0 ? GOLD : BLACK, alpha), cam);
            Client.ribbon(b, ps, at.subtract(armZ), at.add(armZ), 0.035, argb(i % 2 == 0 ? GOLD : BLACK, alpha), cam);
        }
        double radius = 0.5 + 2.0 * t;
        for (int i = 0; i < 24; i++) {
            double a0 = i * Math.PI / 12, a1 = (i + 1) * Math.PI / 12;
            Vec3 p0 = crit.pos().add(Math.cos(a0) * radius, Math.sin(a0) * radius, 0);
            Vec3 p1 = crit.pos().add(Math.cos(a1) * radius, Math.sin(a1) * radius, 0);
            Client.ribbon(b, ps, p0, p1, 0.05, argb(GOLD, alpha), cam);
        }
    }

    // ------------------------------------------------------------ HUD：加班时第一人称黑边
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void hud(RenderGuiEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.level == null) return;
        if (!OVERTIMES.containsKey(mc.player.getId())) return;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        GuiGraphics g = e.getGuiGraphics();
        int bands = 7;
        for (int i = 0; i < bands; i++) {
            int alpha = (44 - i * 5) << 24;
            int width = 6 + i * 4;
            g.fill(i * width, 0, (i + 1) * width, h, alpha);
            g.fill(w - (i + 1) * width, 0, w - i * width, h, alpha);
        }
        g.drawString(mc.font, "加班", 12, h - 140, 0xFFD8B24A);
    }

    private static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private RatioVisuals() {}
}
