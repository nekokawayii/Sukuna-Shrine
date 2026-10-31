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
    private record Sky(Vec3 center, double radius, long until) {}
    private record Rise(Vec3 pos, double height, long born, int life) {}
    private record Burst2(Vec3 pos, int color, long born, int life) {}
    private record Spark(Vec3 pos, int color, long born, int life) {}

    /** 一条光束只保留最新的一条（施术者 id → 光束），持续喷的时候不会叠成一堆。 */
    private static final Map<Integer, Beam> BEAMS = new LinkedHashMap<>();
    private static final List<Trail> TRAILS = new ArrayList<>();
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static final Map<Integer, Charge> CHARGES = new HashMap<>();
    private static final Map<Integer, Dessert> DESSERTS = new HashMap<>();
    private static final List<Domain> DOMAINS = new ArrayList<>();
    private static final List<Rise> RISES = new ArrayList<>();
    private static final List<Burst2> BURSTS2 = new ArrayList<>();
    private static final List<Spark> SPARKS = new ArrayList<>();
    /** 烟花颜色表（特效的 size 里存的就是这个下标）。 */
    private static final int[] COLORS = {0xFFFFFF, 0xFF5A5A, 0xFFD24A, 0x7CFF6B, 0x6BE8FF, 0x6B8CFF, 0xFF6BE8, 0xFFA24A};
    /** 内部天际图（用户给的落日晚霞，原图原分辨率）。 */
    private static final net.minecraft.resources.ResourceLocation SKY_TEXTURE =
        new net.minecraft.resources.ResourceLocation("sukuna", "textures/effect/domain_sunset.png");
    private static Sky SKY;
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
            case RyuFx.FIREWORK_RISE -> RISES.add(new Rise(e.position(), Math.max(1, e.direction().y), now, 40));
            case RyuFx.FIREWORK_BURST -> {
                BURSTS2.add(new Burst2(e.position(), (int) e.size(), now, 18));
                if (PhotonFx.ready()) {
                    int color = COLORS[Math.floorMod((int) e.size(), COLORS.length)];
                    PhotonFx.spark(e.position(), 2.4, 1.2, 0xFFFFFFFF, 0xFF000000 | color, TEX_GLOW, true, 6, 10, 40, 0.4);
                }
                Client.shake(2);
            }
            case RyuFx.FIREWORK_SHELL -> {
                SPARKS.add(new Spark(e.position(), (int) e.size(), now, 5));
                TRAILS.add(new Trail(e.position().subtract(e.direction().normalize().scale(0.7)), e.position(), now, 4));
            }
            case RyuFx.DOMAIN -> {
                SKY = e.size() > 0 ? new Sky(e.position(), Math.max(2, e.size()), now + Math.max(1, e.life())) : null;
                if (e.size() <= 0) break;
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
        RISES.removeIf(r -> now - r.born() > r.life());
        BURSTS2.removeIf(b -> now - b.born() > b.life());
        SPARKS.removeIf(s2 -> now - s2.born() > s2.life());
        if (SKY != null && now >= SKY.until()) SKY = null;
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
        RISES.clear();
        BURSTS2.clear();
        SPARKS.clear();
        SKY = null;
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (BEAMS.isEmpty() && TRAILS.isEmpty() && BURSTS.isEmpty() && DOMAINS.isEmpty()
            && CHARGES.isEmpty() && DESSERTS.isEmpty() && RISES.isEmpty() && BURSTS2.isEmpty() && SPARKS.isEmpty()) return;
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
        for (Rise rise : RISES) drawRise(b, ps, rise, cam, now);
        for (Burst2 burst : BURSTS2) drawBurst2(b, ps, burst, cam, now);
        for (Spark spark : SPARKS) drawSpark(b, ps, spark, cam, now);
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

    /** 烟花火箭：一颗带尾迹的小亮点往上飞。 */
    private static void drawRise(BufferBuilder b, PoseStack ps, Rise rise, Vec3 cam, long now) {
        float t = (now - rise.born()) / (float) rise.life();
        if (t < 0 || t > 1) return;
        Vec3 at = rise.pos().add(0, rise.height() * t, 0);
        int alpha = (int) (230 * (1 - t * 0.4));
        Client.ribbon(b, ps, at.subtract(0, 0.6, 0), at, 0.16, argb(0xFFE7B0, (int) (alpha * 0.5)), cam);
        Client.ribbon(b, ps, at.subtract(0, 0.35, 0), at, 0.07, argb(0xFFFFFF, alpha), cam);
    }

    /** 烟花炸开：一圈该颜色的环 + 交叉火花。 */
    private static void drawBurst2(BufferBuilder b, PoseStack ps, Burst2 burst, Vec3 cam, long now) {
        float t = (now - burst.born()) / (float) burst.life();
        if (t < 0 || t > 1) return;
        int color = COLORS[Math.floorMod(burst.color(), COLORS.length)];
        int alpha = (int) (235 * (1 - t));
        double radius = 0.6 + 3.4 * Math.min(1, t * 1.25);
        Client.ring(b, ps, burst.pos(), radius, 0.12, argb(color, alpha), cam);
        Client.ring(b, ps, burst.pos(), radius * 0.6, 0.06, argb(0xFFFFFF, (int) (alpha * 0.8)), cam);
        Random rnd = new Random(burst.born() * 131 + burst.color());
        for (int i = 0; i < 12; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = radius * (0.4 + rnd.nextDouble() * 0.8);
            Vec3 at = burst.pos().add(Math.cos(a) * d, (rnd.nextDouble() - 0.5) * d, Math.sin(a) * d);
            Client.ribbon(b, ps, at.subtract(0.14, 0, 0), at.add(0.14, 0, 0), 0.03, argb(color, alpha), cam);
            Client.ribbon(b, ps, at.subtract(0, 0.14, 0), at.add(0, 0.14, 0), 0.03, argb(color, alpha), cam);
        }
    }

    /** 追踪弹幕：一颗小亮点。 */
    private static void drawSpark(BufferBuilder b, PoseStack ps, Spark spark, Vec3 cam, long now) {
        float t = (now - spark.born()) / (float) spark.life();
        if (t < 0 || t > 1) return;
        int color = COLORS[Math.floorMod(spark.color(), COLORS.length)];
        int alpha = (int) (245 * (1 - t));
        Client.ribbon(b, ps, spark.pos().subtract(0.1, 0, 0), spark.pos().add(0.1, 0, 0), 0.05, argb(color, alpha), cam);
        Client.ribbon(b, ps, spark.pos().subtract(0, 0.1, 0), spark.pos().add(0, 0.1, 0), 0.05, argb(color, alpha), cam);
        Client.ribbon(b, ps, spark.pos().subtract(0, 0, 0.1), spark.pos().add(0, 0, 0.1), 0.05, argb(color, alpha), cam);
    }

    /**
     * 领域内部天空：**无量空处同款**——先铺一层深紫黑的球壳底色，再把云图用加法混合贴在同一层内翻球上，
     * UV 随时间横向漂移 + 亮度呼吸，看起来就是一片缓慢翻涌的云海。
     * 画在方块之后、粒子之前，并保留深度测试：球壳 35 格在云层 33 格之外，所以云层只会盖住墙、不会盖住地板。
     */
    @SubscribeEvent public static void renderSky(RenderLevelStageEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || SKY == null) return;
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Vec3 cam = e.getCamera().getPosition();
        if (cam.distanceToSqr(SKY.center()) > (SKY.radius() + 8) * (SKY.radius() + 8)) return;
        PoseStack ps = e.getPoseStack();
        // 这一阶段的世界坐标绘制必须先平移 -cam（项目里 RuinClient / Client.render 都是这个约定）
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Vec3 center = SKY.center();
        double radius = SKY.radius() - 2.0;
        float time = (float) ((now(mc)) * 0.0016);
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        // ① 底色：深紫黑（云图是黑底不透明的，必须有这层压住，加法层才不会糊在空气里）
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        sphereQuads(b, ps, center, radius, 0xEE0C0716, 0f, 1f, false);
        BufferUploader.drawWithShader(b.end());
        // ② 图片层：正常 alpha 混合（照片才不会被加法冲白）+ UV 横向叠 3 次并随时间漂移
        float pulse = (float) (0.92 + 0.08 * Math.sin(now(mc) * 0.012));
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SKY_TEXTURE);
        RenderSystem.defaultBlendFunc();
        b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        sphereQuads(b, ps, center, radius * 0.985, ((int) (255 * pulse) << 24) | 0xFFFFFF, time * 0.15f, 1f, true);
        BufferUploader.drawWithShader(b.end());
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    /** 内翻球（经纬网格）；uvRepeat>0 时按经度把 UV 横向叠 uvRepeat 次并加漂移。 */
    private static void sphereQuads(BufferBuilder b, PoseStack ps, Vec3 center, double radius,
                                    int color, float uvShift, float uvRepeat, boolean textured) {
        int lon = 32, lat = 16;
        for (int i = 0; i < lat; i++) {
            double v0 = i / (double) lat, v1 = (i + 1) / (double) lat;
            double phi0 = Math.PI * (v0 - 0.5), phi1 = Math.PI * (v1 - 0.5);
            for (int j = 0; j < lon; j++) {
                double u0 = j / (double) lon, u1 = (j + 1) / (double) lon;
                double th0 = Math.PI * 2 * u0, th1 = Math.PI * 2 * u1;
                skyVertex(b, ps, center, radius, phi0, th0, u0 * uvRepeat + uvShift, v0, color, textured);
                skyVertex(b, ps, center, radius, phi1, th0, u0 * uvRepeat + uvShift, v1, color, textured);
                skyVertex(b, ps, center, radius, phi1, th1, u1 * uvRepeat + uvShift, v1, color, textured);
                skyVertex(b, ps, center, radius, phi0, th1, u1 * uvRepeat + uvShift, v0, color, textured);
            }
        }
    }

    private static void skyVertex(BufferBuilder b, PoseStack ps, Vec3 center, double radius,
                                  double phi, double theta, double u, double v, int color, boolean textured) {
        double cosPhi = Math.cos(phi);
        float x = (float) (center.x + radius * cosPhi * Math.cos(theta));
        float y = (float) (center.y + radius * Math.sin(phi));
        float z = (float) (center.z + radius * cosPhi * Math.sin(theta));
        var vertex = b.vertex(ps.last().pose(), x, y, z);
        if (textured) vertex.uv((float) u, (float) (1 - v));   // 贴图 v=0 在上，球的 v=0 在南极 → 翻转
        vertex.color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24) & 255).endVertex();
    }

    private static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private RyuVisuals() {}
}
