package cn.blockforge.generated.nnnnr1201forge2;

import character.ryu.RyuFx;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 石流龙的表现层：蓝白高密度光炮、蓄力、追踪弹、咒力喷涌环、甜品、领域起手。 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class RyuVisuals {
    private static final int CORE = 0xFFFFFF, GLOW = 0x9FDFFF, DEEP = 0x3E93C8;
    private static final String TEX_GLOW = "textures/effect/glow.png";

    private record Beam(Vec3 from, Vec3 to, double width, long born, int life) {}
    private record Trail(Vec3 from, Vec3 to, long born, int life) {}
    private record Burst(Vec3 pos, double radius, long born, int life) {}
    private record Charge(int owner, double ratio, long until) {}
    private record Dessert(int owner, long until) {}
    private record Domain(Vec3 pos, double radius, long born, int life) {}

    /** 一条光束只保留最新的一条（施术者 id → 光束），持续喷的时候不会叠成一堆。 */
    private static final Map<Integer, Beam> BEAMS = new LinkedHashMap<>();
    private static final List<Trail> TRAILS = new ArrayList<>();
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static final Map<Integer, Charge> CHARGES = new HashMap<>();
    private static final Map<Integer, Dessert> DESSERTS = new HashMap<>();
    private static final List<Domain> DOMAINS = new ArrayList<>();
    private static Object world;

    public static void receive(Network.Effect e) {
        var mc = Minecraft.getInstance();
        long now = now(mc);
        switch (e.kind()) {
            case RyuFx.BEAM -> {
                BEAMS.put(e.owner(), new Beam(e.position(), e.direction(), Math.max(0.5, e.size()), now, 8));
                if (PhotonFx.ready()) {
                    Vec3 mid = e.position().lerp(e.direction(), 0.5);
                    PhotonFx.beam(e.position(), e.direction(), Math.max(0.8, e.size() * 0.9), 8);
                    PhotonFx.spark(mid, Math.max(1.5, e.size()), 1.4, 0xFFEAF9FF, 0xFF5FC8FF, TEX_GLOW,
                        true, 6, 10, 46, 0.4);
                    PhotonFx.ring(e.direction(), Math.max(1.5, e.size() * 1.2), 0.6, 0xA0CFEFFF, 0xFF7FDDFF, 26, 6, 10);
                }
                Client.shake(6);
            }
            case RyuFx.CHARGE -> {
                if (e.size() <= 0) CHARGES.remove(e.owner());
                else CHARGES.put(e.owner(), new Charge(e.owner(), e.size(), now + Math.max(2, e.life())));
            }
            case RyuFx.MISSILE -> {
                TRAILS.add(new Trail(e.position().subtract(e.direction().normalize().scale(0.8)), e.position(), now, 5));
                if (PhotonFx.ready())
                    PhotonFx.spark(e.position(), 0.5, 0.4, 0xFFF0FBFF, 0xFF5FC8FF, TEX_GLOW, true, 3, 5, 8, 0.2);
            }
            case RyuFx.BURST -> {
                BURSTS.add(new Burst(e.position().add(0, 0.6, 0), Math.max(1, e.size()), now, 14));
                if (PhotonFx.ready()) {
                    PhotonFx.ring(e.position().add(0, 0.3, 0), Math.max(1, e.size()), 0.8, 0xA8D8F7FF, 0xFF7FDDFF, 30, 8, 10);
                    PhotonFx.shockwave(e.position().add(0, 0.4, 0), 0xFFBFF0FF, Math.max(1.5, e.size()), 8);
                }
                Client.shake(3);
            }
            case RyuFx.DESSERT -> {
                if (e.size() > 0) DESSERTS.put(e.owner(), new Dessert(e.owner(), now + Math.max(1, e.life())));
                else DESSERTS.remove(e.owner());
            }
            case RyuFx.DOMAIN -> {
                DOMAINS.add(new Domain(e.position(), Math.max(2, e.size()), now, 20));
                if (PhotonFx.ready()) {
                    PhotonFx.ring(e.position().add(0, 0.2, 0), Math.max(2, e.size()), 1.2, 0xA8D8F7FF, 0xFF9FDFFF, 44, 10, 14);
                    PhotonFx.shockwave(e.position(), 0xFFBFF0FF, Math.max(2, e.size()), 12);
                }
                Client.shake(8);
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
        BEAMS.values().removeIf(b -> now - b.born() > b.life());
        TRAILS.removeIf(t -> now - t.born() > t.life());
        BURSTS.removeIf(b -> now - b.born() > b.life());
        CHARGES.values().removeIf(c -> c.until() <= now);
        DESSERTS.values().removeIf(d -> d.until() <= now);
        DOMAINS.removeIf(d -> now - d.born() > d.life());
        // 甜品：吃蛋糕的小表现
        var p = mc.player;
        if (p == null || !PhotonFx.ready()) return;
        if (DESSERTS.containsKey(p.getId()) && now % 6 == 0) {
            Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(0.5)).add(0, -0.15, 0);
            PhotonFx.spark(at, 0.35, 0.3, 0xFFFFE7B8, 0xFFD8A05A, TEX_GLOW, true, 4, 6, 6, 0.1);
        }
    }

    public static void clear() {
        BEAMS.clear();
        TRAILS.clear();
        BURSTS.clear();
        CHARGES.clear();
        DESSERTS.clear();
        DOMAINS.clear();
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (BEAMS.isEmpty() && TRAILS.isEmpty() && BURSTS.isEmpty() && DOMAINS.isEmpty()
            && CHARGES.isEmpty() && DESSERTS.isEmpty()) return;
        long now = now(mc);
        PoseStack ps = e.getPoseStack();
        Vec3 cam = e.getCamera().getPosition();
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
        for (Beam beam : BEAMS.values()) drawBeam(b, ps, beam, cam, now);
        for (Trail trail : TRAILS) drawTrail(b, ps, trail, cam, now);
        for (Burst burst : BURSTS) drawBurst(b, ps, burst, cam, now);
        for (Domain domain : DOMAINS) drawDomain(b, ps, domain, cam, now);
        for (var entry : CHARGES.entrySet()) drawCharge(b, ps, mc, entry.getValue(), cam, now);
        for (var entry : DESSERTS.entrySet()) drawDessert(b, ps, mc, entry.getKey(), cam, now);
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    /**
     * 3D 冲击波：沿轴线一圈圈环 + 多股偏移光束 + 枪口/末端球壳，
     * 从任何角度看都是"有体积的一团"，而不是一张贴在屏幕上的面片。
     */
    private static void drawBeam(BufferBuilder b, PoseStack ps, Beam beam, Vec3 cam, long now) {
        float t = (now - beam.born()) / (float) beam.life();
        if (t < 0 || t > 1) return;
        int alpha = (int) (235 * (1 - t * 0.6));
        double width = beam.width();
        Vec3 from = beam.from(), to = beam.to();
        Vec3 axis = to.subtract(from);
        double length = axis.length();
        if (length < 0.3) return;
        axis = axis.normalize();
        Vec3 u = axis.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1e-4) u = new Vec3(1, 0, 0);
        u = u.normalize();
        Vec3 v = axis.cross(u).normalize();
        // ① 中心的粗芯（billboard 面片，负责亮度）
        Client.ribbon(b, ps, from, to, width * 1.5, argb(DEEP, (int) (alpha * 0.40)), cam);
        Client.ribbon(b, ps, from, to, width * 0.95, argb(GLOW, (int) (alpha * 0.75)), cam);
        Client.ribbon(b, ps, from, to, width * 0.45, argb(CORE, alpha), cam);
        // ② 四股绕轴偏置的光束：给出体积感
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + now * 0.15;
            Vec3 offset = u.scale(Math.cos(a) * width * 0.32).add(v.scale(Math.sin(a) * width * 0.32));
            double w = width * (0.22 + 0.06 * Math.sin(now * 0.3 + k));
            Client.ribbon(b, ps, from.add(offset), to.add(offset), w, argb(GLOW, (int) (alpha * 0.55)), cam);
        }
        // ③ 一圈圈横截面（真正让它看起来是 3D 的关键）
        int rings = 7;
        for (int i = 0; i <= rings; i++) {
            double f = i / (double) rings;
            Vec3 center = from.add(axis.scale(length * f));
            double radius = width * 0.5 * (1 + f * 0.9);
            double spin = now * 0.05 + i;
            circle(b, ps, center, u, v, radius, 0.05 + width * 0.02, spin,
                argb(i % 2 == 0 ? CORE : GLOW, (int) (alpha * (0.35 + 0.4 * (1 - f)))), cam);
        }
        // ④ 枪口与末端的球壳
        shells(b, ps, from, axis, width * 0.62, u, v, (int) (alpha * 0.9), cam);
        shells(b, ps, to, axis, width * 0.95, u, v, (int) (alpha * 0.75), cam);
    }

    /** 平面上的一个圆（横截面）。 */
    private static void circle(BufferBuilder b, PoseStack ps, Vec3 center, Vec3 u, Vec3 v, double radius,
                               double width, double phase, int color, Vec3 cam) {
        int segments = 20;
        Vec3 prev = center.add(u.scale(Math.cos(phase) * radius)).add(v.scale(Math.sin(phase) * radius));
        for (int i = 1; i <= segments; i++) {
            double a = phase + i * Math.PI * 2 / segments;
            Vec3 next = center.add(u.scale(Math.cos(a) * radius)).add(v.scale(Math.sin(a) * radius));
            Client.ribbon(b, ps, prev, next, width, color, cam);
            prev = next;
        }
    }

    /** 三圈互相垂直的环，拼出一个"球壳"的轮廓。 */
    private static void shells(BufferBuilder b, PoseStack ps, Vec3 center, Vec3 axis, double radius,
                               Vec3 u, Vec3 v, int alpha, Vec3 cam) {
        int color = argb(CORE, alpha);
        circle(b, ps, center, u, v, radius, 0.06, 0, color, cam);
        circle(b, ps, center, u, axis, radius, 0.06, 0, color, cam);
        circle(b, ps, center, v, axis, radius, 0.06, 0, color, cam);
    }

    private static void drawTrail(BufferBuilder b, PoseStack ps, Trail trail, Vec3 cam, long now) {
        float t = (now - trail.born()) / (float) trail.life();
        if (t < 0 || t > 1) return;
        int alpha = (int) (220 * (1 - t));
        Client.ribbon(b, ps, trail.from(), trail.to(), 0.22, argb(GLOW, (int) (alpha * 0.6)), cam);
        Client.ribbon(b, ps, trail.from(), trail.to(), 0.09, argb(CORE, alpha), cam);
    }

    private static void drawBurst(BufferBuilder b, PoseStack ps, Burst burst, Vec3 cam, long now) {
        float t = (now - burst.born()) / (float) burst.life();
        if (t < 0 || t > 1) return;
        int alpha = (int) (220 * (1 - t));
        double radius = 0.6 + burst.radius() * Math.min(1, t * 1.3);
        Client.ring(b, ps, burst.pos().add(0, 0.1, 0), radius, 0.14, argb(GLOW, alpha), cam);
        Client.ring(b, ps, burst.pos().add(0, 0.25, 0), radius * 0.7, 0.06, argb(CORE, alpha), cam);
    }

    private static void drawDomain(BufferBuilder b, PoseStack ps, Domain domain, Vec3 cam, long now) {
        float t = (now - domain.born()) / (float) domain.life();
        if (t < 0 || t > 1) return;
        int alpha = (int) (200 * (1 - t));
        Client.ring(b, ps, domain.pos().add(0, 0.1, 0), domain.radius() * Math.min(1, t * 1.4), 0.2,
            argb(GLOW, alpha), cam);
        Client.ring(b, ps, domain.pos().add(0, 0.3, 0), domain.radius() * 0.6 * Math.min(1, t * 1.4), 0.08,
            argb(CORE, alpha), cam);
    }

    /** 蓄力：飞机头前方一颗越涨越亮的球 + 一圈收缩的瞄准环。 */
    private static void drawCharge(BufferBuilder b, PoseStack ps, Minecraft mc, Charge charge, Vec3 cam, long now) {
        Entity entity = mc.level.getEntity(charge.owner());
        if (!(entity instanceof LivingEntity living)) return;
        Vec3 at = living.getEyePosition().add(living.getLookAngle().scale(1.0));
        // 传进来的是当前宽度（1 格起、无上限），前 4 格宽度换算成 0~1 的蓄力表现
        double ratio = Math.max(0, Math.min(1, (charge.ratio() - 1) / 4));
        double radius = 0.25 + ratio * 0.8;
        int alpha = (int) (120 + 120 * ratio);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + now * 0.4;
            Vec3 dir = new Vec3(Math.cos(a), Math.sin(a), 0);
            Client.ribbon(b, ps, at.add(dir.scale(radius)), at.add(dir.scale(radius * 1.25)), 0.05,
                argb(GLOW, alpha), cam);
        }
        Client.ribbon(b, ps, at.subtract(radius * 0.4, 0, 0), at.add(radius * 0.4, 0, 0), 0.12,
            argb(CORE, alpha), cam);
        Client.ribbon(b, ps, at.subtract(0, radius * 0.4, 0), at.add(0, radius * 0.4, 0), 0.12,
            argb(CORE, alpha), cam);
    }

    /** 甜品：手里一块蛋糕（几块色块拼的）+ 飘出的碎屑。 */
    private static void drawDessert(BufferBuilder b, PoseStack ps, Minecraft mc, int owner, Vec3 cam, long now) {
        Entity entity = mc.level.getEntity(owner);
        if (!(entity instanceof LivingEntity living)) return;
        Vec3 at = living.getEyePosition().add(living.getLookAngle().scale(0.55)).add(0, -0.3, 0);
        double sway = Math.sin(now * 0.5) * 0.05;
        at = at.add(sway, 0, sway);
        int body = 0xFFF3E3C0, cream = 0xFFFFF6E0, jam = 0xFFD8637B;
        Client.ribbon(b, ps, at, at.add(0.34, 0.02, 0), 0.16, argb(body, 235), cam);
        Client.ribbon(b, ps, at.add(0, 0.1, 0), at.add(0.34, 0.12, 0), 0.07, argb(cream, 235), cam);
        Client.ribbon(b, ps, at.add(0.02, 0.16, 0), at.add(0.2, 0.17, 0), 0.05, argb(jam, 235), cam);
        Client.ribbon(b, ps, at.add(0, 0.04, 0), at.add(0, 0.16, 0), 0.05, argb(body, 200), cam);
    }

    private static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private RyuVisuals() {}
}
