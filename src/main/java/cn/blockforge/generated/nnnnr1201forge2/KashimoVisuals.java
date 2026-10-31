package cn.blockforge.generated.nnnnr1201forge2;

import character.kashimo.KashimoFx;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * 鹿紫云一的表现层：落雷、雷走残影、放电环、吸附拉线、头顶正电荷图标、屏幕边缘电弧。
 * <p>分工按项目既有约束：形体（雷柱折线 / 焦痕 / 环）走 {@code Client.ribbon/quad} 顶点通路，
 * 发光（火花、光晕、冲击环）走 PhotonFx 加法混合层。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class KashimoVisuals {
    private static final int CORE = 0xFFFFFF, GLOW = 0x7FD8FF, DEEP = 0x2E9BE0;
    private static final String TEX_GLOW = "textures/effect/glow.png";
    private static final int MAX_NEG = 20;

    private record Charge(int layers, long until) {}
    private record Bolt(Vec3 from, Vec3 to, long seed, long born, int life) {}
    private record Scorch(Vec3 pos, long born, int life) {}
    private record Trail(Vec3 from, Vec3 to, long born, int life) {}
    private record Shock(Vec3 pos, double radius, long born, int life) {}
    private record Pull(Vec3 from, Vec3 to, long born, int life) {}
    private record Overhead(int x, int y, int layers) {}

    private static final Map<Integer, Charge> charges = new HashMap<>();
    private static final List<Bolt> bolts = new ArrayList<>();
    private static final List<Scorch> scorches = new ArrayList<>();
    private static final List<Trail> trails = new ArrayList<>();
    private static final List<Shock> shocks = new ArrayList<>();
    private static final List<Pull> pulls = new ArrayList<>();
    private static final List<Overhead> overheads = new ArrayList<>();
    /** 按实体记录的状态：别人身上的电弧也要画出来，不能只有自己看得见。 */
    private static final Map<Integer, Long> attachUntil = new HashMap<>();
    private static final Map<Integer, Long> ultUntil = new HashMap<>();
    private static final Map<Integer, Long> crashUntil = new HashMap<>();
    private static final Map<Integer, Integer> negStacks = new HashMap<>();
    private static final Map<Integer, Long> magnetUntil = new HashMap<>();
    private record FieldVis(Vec3 pos, double radius, long until) {}
    private record WarnVis(Vec3 pos, double radius, long until) {}
    private record OrbVis(Vec3 pos, double radius, long until) {}
    private record Ring3(Vec3 pos, double radius, long born, int life) {}
    private static final List<FieldVis> fields = new ArrayList<>();
    private static final List<WarnVis> warns = new ArrayList<>();
    private static final List<OrbVis> orbs = new ArrayList<>();
    private static final List<Ring3> rings = new ArrayList<>();
    private static final Map<Integer, Long> nets = new HashMap<>();
    private static long chainDarkUntil;
    private static long tick, strikeTick = -1;
    private static final Matrix4f projection = new Matrix4f();
    private static final org.joml.Quaternionf cameraRotation = new org.joml.Quaternionf();
    private static int strikeBurst;
    private static Object world;

    // ------------------------------------------------------------ 收包
    public static void receive(Network.Effect e) {
        var mc = Minecraft.getInstance();
        long now = now(mc);
        switch (e.kind()) {
            case KashimoFx.POS_CHARGE -> {
                if (e.size() <= 0) charges.remove(e.owner());
                else charges.put(e.owner(), new Charge((int) e.size(), now + Math.max(1, e.life())));
            }
            case KashimoFx.NEG_CHARGE -> {
                if (e.size() > 0) negStacks.put(e.owner(), (int) e.size());
                else negStacks.remove(e.owner());
            }
            case KashimoFx.STRIKE -> {
                Vec3 at = e.position();
                bolts.add(new Bolt(at.add(0, 26, 0), at.add(0, 0.15, 0), e.seed() * 131L + now, now, 12));
                scorches.add(new Scorch(at, now, Math.max(20, e.life())));
                // 引爆时同一刻会有十几道落雷：火花/冲击环只给前 8 道，避免瞬间堆几十个发射器
                if (strikeTick != now) { strikeTick = now; strikeBurst = 0; }
                strikeBurst++;
                if (PhotonFx.ready() && strikeBurst <= 8) {
                    PhotonFx.spark(at, 2.4, 1.35, 0xFFD9F6FF, 0xFF5FCBFF, TEX_GLOW, true, 6, 12, 46, 0.5);
                    PhotonFx.ring(at.add(0, 0.06, 0), 2.8, 0.7, 0xB0CFEFFF, 0xFF7FDFFF, 22, 6, 10);
                    PhotonFx.shockwave(at, 0xFF9FE8FF, 3.2, 8);
                }
                Client.shake(4);
            }
            case KashimoFx.DASH -> {
                Vec3 from = e.position(), to = e.direction();
                trails.add(new Trail(from, to, now, 14));
                shocks.add(new Shock(to, 3, now, 12));
                if (PhotonFx.ready()) {
                    PhotonFx.spark(from, 1.1, 0.9, 0xFFD9F6FF, 0xFF5FCBFF, TEX_GLOW, true, 6, 10, 30, 0.2);
                    PhotonFx.spark(to, 1.3, 1.0, 0xFFD9F6FF, 0xFF5FCBFF, TEX_GLOW, true, 8, 10, 34, 0.3);
                    PhotonFx.ring(to.add(0, 0.12, 0), 2.6, 0.5, 0x90BFF4FF, 0xFF7FDFFF, 18, 6, 8);
                }
            }
            case KashimoFx.DISCHARGE -> {
                shocks.add(new Shock(e.position(), Math.max(1, e.size()), now, 14));
                if (PhotonFx.ready()) {
                    PhotonFx.ring(e.position().add(0, 0.25, 0), Math.max(1, e.size()), 0.85, 0x9CCFF3FF,
                        0xFF5FCBFF, Math.max(20, e.size() * 5), 8, 12);
                    PhotonFx.spark(e.position(), Math.max(1, e.size()) * 0.55, 0.8, 0xFFE4FAFF, 0xFF7FDDFF,
                        TEX_GLOW, true, 6, 8, 26, 0.35);
                }
            }
            case KashimoFx.ATTRACT -> {
                pulls.add(new Pull(e.position(), e.direction(), now, 14));
                if (PhotonFx.ready()) {
                    PhotonFx.spark(e.position(), 1.0, 0.7, 0xFFE4FAFF, 0xFF7FDDFF, TEX_GLOW, true, 5, 8, 20, 0.2);
                    PhotonFx.spark(e.direction(), 0.9, 0.7, 0xFFE4FAFF, 0xFF7FDDFF, TEX_GLOW, true, 5, 8, 20, 0.2);
                }
            }
            case KashimoFx.ATTACH -> {
                if (e.size() > 0) attachUntil.put(e.owner(), now + Math.max(1, e.life()));
                else attachUntil.remove(e.owner());
            }
            case KashimoFx.ULT -> {
                if (e.size() > 0) ultUntil.put(e.owner(), now + Math.max(1, e.life()));
                else ultUntil.remove(e.owner());
                if (e.size() > 0 && PhotonFx.ready()) {
                    PhotonFx.ring(e.position().add(0, 0.2, 0), 4.5, 1.1, 0xC0CFF3FF, 0xFF7FDFFF, 40, 8, 12);
                    PhotonFx.shockwave(e.position(), 0xFFBFF0FF, 5.5, 12);
                }
            }
            case KashimoFx.CRASH -> {
                if (e.size() > 0) crashUntil.put(e.owner(), now + Math.max(1, e.life()));
                else crashUntil.remove(e.owner());
            }
            case KashimoFx.MAGNET -> {
                if (e.size() > 0) magnetUntil.put(e.owner(), now + Math.max(1, e.life()));
                else magnetUntil.remove(e.owner());
                if (e.size() > 0 && PhotonFx.ready())
                    PhotonFx.ring(e.position().add(0, 0.3, 0), 3.2, 0.7, 0xC0E0A8FF, 0xFFB15AFF, 30, 8, 12);
            }
            case KashimoFx.FIELD -> {
                fields.removeIf(f -> f.until() <= now);
                fields.add(new FieldVis(e.position(), Math.max(1, e.size()), now + Math.max(1, e.life())));
            }
            case KashimoFx.GUARD -> {
                rings.add(new Ring3(e.position(), Math.max(1, e.size()), now, Math.max(8, e.life())));
                if (PhotonFx.ready()) {
                    PhotonFx.shockwave(e.position(), 0xFFDFF6FF, Math.max(2, e.size()), 8);
                    PhotonFx.spark(e.position().add(0, 0.6, 0), Math.max(1.5, e.size() * 0.6), 1.1,
                        0xFFE8FBFF, 0xFF5FCBFF, TEX_GLOW, true, 6, 10, 30, 0.5);
                }
                Client.shake(3);
            }
            case KashimoFx.UPPERCUT -> {
                scorches.add(new Scorch(e.position(), now, 200));
                shocks.add(new Shock(e.position(), 2.6, now, 10));
                if (PhotonFx.ready())
                    PhotonFx.spark(e.position().add(0, 0.6, 0), 1.2, 0.8, 0xFFE8FBFF, 0xFF5FCBFF, TEX_GLOW, true, 5, 8, 24, 0.4);
            }
            case KashimoFx.ENVELOPE -> {
                if (e.life() > 7) nets.put(e.owner(), now + Math.max(1, e.life()));
                else nets.remove(e.owner());
            }
            case KashimoFx.BOLT_WARN -> {
                warns.removeIf(w -> w.until() <= now);
                warns.add(new WarnVis(e.position(), Math.max(1, e.size()), now + Math.max(1, e.life())));
                if (PhotonFx.ready())
                    PhotonFx.ring(e.position().add(0, 0.12, 0), Math.max(1, e.size()), 0.22,
                        0x66BFE9FF, 0xFF5FCBFF, 26, Math.max(4, e.life()), 8);
            }
            case KashimoFx.ORB -> {
                orbs.removeIf(o -> o.until() <= now);
                orbs.add(new OrbVis(e.position(), Math.max(0.5, e.size()), now + 3));
                if (PhotonFx.ready())
                    PhotonFx.spark(e.position(), 1.15, 0.9, 0xFFE8FBFF, 0xFF5FCBFF, TEX_GLOW, true, 4, 6, 16, 0.35);
            }
            case KashimoFx.ORB_BLAST -> {
                orbs.clear();
                rings.add(new Ring3(e.position(), Math.max(1, e.size()), now, 16));
                scorches.add(new Scorch(e.position(), now, 20 * 15));
                if (PhotonFx.ready()) {
                    PhotonFx.spark(e.position(), Math.max(2, e.size() * 0.5), 1.6, 0xFFD9F6FF, 0xFF5FCBFF, TEX_GLOW, true, 8, 12, 52, 0.6);
                    PhotonFx.shockwave(e.position(), 0xFFBFF0FF, Math.max(2, e.size()), 12);
                }
                Client.shake(6);
            }
            case KashimoFx.CHAIN_DARK -> chainDarkUntil = e.size() > 0 ? now + Math.max(1, e.life()) : 0;
            case KashimoFx.CHAIN_LINK -> pulls.add(new Pull(e.position(), e.direction(), now, 10));
            case KashimoFx.SHADOW -> {
                trails.add(new Trail(e.position(), e.direction(), now, 12));
                shocks.add(new Shock(e.direction(), 2.2, now, 8));
            }
            case KashimoFx.REFLECT -> {
                rings.add(new Ring3(e.position(), Math.max(1, e.size()), now, 10));
                if (PhotonFx.ready())
                    PhotonFx.spark(e.position().add(0, 1, 0), 1.0, 0.7, 0xFFE8FBFF, 0xFF5FCBFF, TEX_GLOW, true, 4, 6, 18, 0.2);
            }
            default -> { }
        }
    }

    private static long now(Minecraft mc) { return mc.level == null ? 0 : mc.level.getGameTime(); }

    // ------------------------------------------------------------ 每刻：持续时间与光效
    @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (world != mc.level) {
            world = mc.level;
            clear();
            return;
        }
        long now = now(mc);
        tick++;
        charges.entrySet().removeIf(entry -> now >= entry.getValue().until());
        bolts.removeIf(v -> now - v.born() > v.life());
        scorches.removeIf(v -> now - v.born() > v.life());
        trails.removeIf(v -> now - v.born() > v.life());
        shocks.removeIf(v -> now - v.born() > v.life());
        pulls.removeIf(v -> now - v.born() > v.life());
        attachUntil.values().removeIf(until -> now >= until);
        ultUntil.values().removeIf(until -> now >= until);
        crashUntil.values().removeIf(until -> now >= until);
        negStacks.entrySet().removeIf(entry -> entry.getValue() <= 0);
        magnetUntil.values().removeIf(until -> now >= until);
        fields.removeIf(f -> f.until() <= now);
        warns.removeIf(w -> w.until() <= now);
        orbs.removeIf(o -> o.until() <= now);
        rings.removeIf(r -> now - r.born() > r.life());
        nets.values().removeIf(until -> now >= until);
        if (chainDarkUntil != 0 && now >= chainDarkUntil) chainDarkUntil = 0;
        var self = mc.player;
        if (self == null || mc.level == null || !PhotonFx.ready()) return;
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        // 幻兽·琥珀：每 5 刻在身上炸一圈电弧；附着：每 6 刻在手上缠电；
        // 负电荷：每 4 刻身上的细碎电弧。全都按"这个实体"生成——第一人称只跳过自己，别人照常看得见。
        if (now % 5 == 0) for (int id : ultUntil.keySet()) aura(mc, id, firstPerson, true);
        if (now % 6 == 0) for (int id : attachUntil.keySet()) aura(mc, id, firstPerson, false);
        if (now % 4 == 0) {
            for (var entry : negStacks.entrySet()) {
                if (hideSelf(mc, entry.getKey(), firstPerson)) continue;
                if (!(mc.level.getEntity(entry.getKey()) instanceof net.minecraft.world.entity.LivingEntity living)) continue;
                if (self.distanceToSqr(living) > 64 * 64) continue;
                PhotonFx.spark(living.position().add(0, living.getBbHeight() * 0.6, 0), 0.85, 0.45,
                    0xFFCFEFFF, 0xFF5FCBFF, TEX_GLOW, true, 4, 6, 8, 0.2);
            }
        }
    }

    /** 第一人称下不画"自己身上"的那一层（相机就在身体里，光斑会糊满屏幕）；别人一律照常。 */
    private static boolean hideSelf(Minecraft mc, int id, boolean firstPerson) {
        return firstPerson && mc.player != null && mc.player.getId() == id;
    }

    /** 一个实体身上的持续电弧：body=true 全身（琥珀），false 手部（附着）。 */
    private static void aura(Minecraft mc, int id, boolean firstPerson, boolean body) {
        if (hideSelf(mc, id, firstPerson)) return;
        if (!(mc.level.getEntity(id) instanceof net.minecraft.world.entity.LivingEntity living)) return;
        if (mc.player != null && mc.player.distanceToSqr(living) > 64 * 64) return;
        Vec3 pos = body
            ? living.position().add(0, living.getBbHeight() * 0.5, 0)
            : living.getEyePosition().add(living.getLookAngle().scale(0.8)).add(0, -0.35, 0);
        if (body) {
            PhotonFx.lines(pos, 1.35, 0.05, 0.95, 0xFFBFF4FF, 0xFF5FCBFF, 0, 0, 16, 6, 8, false);
            PhotonFx.spark(pos, 1.15, 0.85, 0xFFE8FBFF, 0xFF7FDDFF, TEX_GLOW, true, 5, 8, 18, 0.4);
        } else {
            PhotonFx.lines(pos, 0.55, 0.04, 0.55, 0xFFCFF6FF, 0xFF7FDDFF, 0, 0, 10, 5, 6, false);
        }
    }

    /** 本机玩家自己身上的状态（HUD 用）。 */
    private static long local(Map<Integer, Long> map, long now) {
        var p = Minecraft.getInstance().player;
        if (p == null) return 0;
        long until = map.getOrDefault(p.getId(), 0L);
        return until > now ? until : 0;
    }

    private static int localNeg() {
        var p = Minecraft.getInstance().player;
        return p == null ? 0 : negStacks.getOrDefault(p.getId(), 0);
    }

    public static void clear() {
        charges.clear();
        bolts.clear();
        scorches.clear();
        trails.clear();
        shocks.clear();
        pulls.clear();
        overheads.clear();
        negStacks.clear();
        attachUntil.clear();
        ultUntil.clear();
        crashUntil.clear();
    }

    // ------------------------------------------------------------ 世界内绘制
    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        long now = now(mc);
        PoseStack ps = e.getPoseStack();
        Vec3 cam = e.getCamera().getPosition();
        float partial = e.getPartialTick();
        projectOverheads(mc, e, cam, partial, now);
        if (bolts.isEmpty() && scorches.isEmpty() && trails.isEmpty() && shocks.isEmpty() && pulls.isEmpty()
            && fields.isEmpty() && warns.isEmpty() && orbs.isEmpty() && rings.isEmpty() && nets.isEmpty()) return;
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
        for (Scorch s : scorches) drawScorch(b, ps, s, cam, now);
        for (Shock s : shocks) drawShock(b, ps, s, cam, now);
        for (Trail t : trails) drawTrail(b, ps, t, cam, now);
        for (Pull p : pulls) drawPull(b, ps, p, cam, now);
        for (FieldVis f : fields) drawField(b, ps, f, cam, now);
        for (WarnVis w : warns) drawWarn(b, ps, w, cam, now);
        for (OrbVis o : orbs) drawOrb(b, ps, o, cam, now);
        for (var entry : nets.entrySet()) drawNet(b, ps, mc, entry.getKey(), cam);
        for (Ring3 r : rings) drawRing3(b, ps, r, cam, now);
        for (Bolt v : bolts) drawBolt(b, ps, v, cam, now);
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    /**
     * 头顶图标要在 HUD 里画文字，所以每帧把实体头顶投影到屏幕坐标缓存一次。
     * <p>用「相机旋转的逆 × 世界投影」自己算，不依赖 PoseStack 在该阶段是否已被平移——
     * 那个约定在 Forge 各阶段不一致，自己乘最稳。
     */
    private static void projectOverheads(Minecraft mc, RenderLevelStageEvent e, Vec3 cam,
                                         float partial, long now) {
        overheads.clear();
        if (charges.isEmpty()) return;
        projection.set(e.getProjectionMatrix());
        cameraRotation.set(mc.gameRenderer.getMainCamera().rotation());
        Matrix4f view = new Matrix4f().rotation(cameraRotation).invert();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        for (var entry : charges.entrySet()) {
            if (now >= entry.getValue().until()) continue;
            Entity entity = mc.level.getEntity(entry.getKey());
            if (entity == null) continue;
            Vec3 at = entity.getPosition(partial).add(0, entity.getBbHeight() + 0.55, 0).subtract(cam);
            if (at.lengthSqr() > 96 * 96) continue;
            Vector4f v = new Vector4f((float) at.x, (float) at.y, (float) at.z, 1f).mul(view).mul(projection);
            if (v.w <= 0.01f) continue;
            int x = Math.round((v.x / v.w * 0.5f + 0.5f) * w);
            int y = Math.round((0.5f - v.y / v.w * 0.5f) * h);
            overheads.add(new Overhead(x, y, entry.getValue().layers()));
        }
    }

    private static void drawBolt(BufferBuilder b, PoseStack ps, Bolt bolt, Vec3 cam, long now) {
        float life = Math.max(1, bolt.life());
        float t = (now - bolt.born()) / life;
        if (t < 0 || t > 1) return;
        int alpha = (int) (255 * (1 - t));
        Random rnd = new Random(bolt.seed());
        Vec3 delta = bolt.to().subtract(bolt.from());
        Vec3 side = delta.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = delta.cross(side).normalize();
        int segs = 9;
        Vec3 prev = bolt.from();
        for (int i = 1; i <= segs; i++) {
            double f = i / (double) segs;
            Vec3 next = bolt.from().add(delta.scale(f));
            if (i < segs) {
                double jitter = (1 - Math.abs(f - 0.5) * 1.3) * 1.5;
                next = next.add(side.scale((rnd.nextDouble() - 0.5) * jitter))
                    .add(up.scale((rnd.nextDouble() - 0.5) * jitter));
            }
            Client.ribbon(b, ps, prev, next, 0.30, argb(GLOW, (int) (alpha * 0.45)), cam);
            Client.ribbon(b, ps, prev, next, 0.09, argb(CORE, (int) (alpha * 0.95)), cam);
            prev = next;
        }
        // 两道分叉，让雷柱不是一根直线
        for (int k = 0; k < 2; k++) {
            Vec3 start = bolt.from().add(delta.scale(0.25 + 0.25 * k));
            Vec3 end = start.add(delta.scale(0.18)).add(side.scale((k == 0 ? 1 : -1) * 2.2));
            Client.ribbon(b, ps, start, end, 0.05, argb(GLOW, (int) (alpha * 0.6)), cam);
        }
    }

    private static void drawScorch(BufferBuilder b, PoseStack ps, Scorch s, Vec3 cam, long now) {
        float t = (now - s.born()) / (float) Math.max(1, s.life());
        if (t < 0 || t > 1) return;
        int alpha = (int) (150 * (1 - t) * (1 - t));
        if (alpha <= 2) return;
        int color = argb(0x0A0A0C, alpha);
        double r = 0.85 + 0.55 * Math.min(1, t * 3);
        Vec3 c = s.pos().add(0, 0.04, 0);
        for (int i = 0; i < 14; i++) {
            double a0 = i * Math.PI / 7, a1 = (i + 1) * Math.PI / 7;
            double r0 = r * (0.72 + 0.28 * Math.sin(i * 2.7));
            double r1 = r * (0.72 + 0.28 * Math.sin((i + 1) * 2.7));
            Vec3 p0 = s.pos().add(Math.cos(a0) * r0, 0.04, Math.sin(a0) * r0);
            Vec3 p1 = s.pos().add(Math.cos(a1) * r1, 0.04, Math.sin(a1) * r1);
            Client.quad(b, ps, c, p0, p1, p1, color);
        }
    }

    private static void drawShock(BufferBuilder b, PoseStack ps, Shock s, Vec3 cam, long now) {
        float t = (now - s.born()) / (float) Math.max(1, s.life());
        if (t < 0 || t > 1) return;
        int alpha = (int) (210 * (1 - t));
        double radius = 0.6 + s.radius() * Math.min(1, t * 1.15);
        Client.ring(b, ps, s.pos().add(0, 0.16, 0), radius, 0.16 + 0.1 * (1 - t), argb(GLOW, alpha), cam);
        Client.ring(b, ps, s.pos().add(0, 0.2, 0), radius * 0.72, 0.06, argb(CORE, alpha), cam);
    }

    private static void drawTrail(BufferBuilder b, PoseStack ps, Trail t, Vec3 cam, long now) {
        float f = (now - t.born()) / (float) Math.max(1, t.life());
        if (f < 0 || f > 1) return;
        int alpha = (int) (230 * (1 - f));
        Vec3 delta = t.to().subtract(t.from());
        Vec3 side = delta.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Random rnd = new Random(t.born() * 977 + (long) (delta.lengthSqr() * 1000));
        for (int k = -1; k <= 1; k++) {
            Vec3 offset = side.scale(k * 0.32);
            Vec3 prev = t.from().add(offset);
            for (int i = 1; i <= 7; i++) {
                double p = i / 7.0;
                Vec3 next = t.from().add(delta.scale(p)).add(offset)
                    .add(0, (rnd.nextDouble() - 0.5) * 0.35, 0);
                Client.ribbon(b, ps, prev, next, k == 0 ? 0.10 : 0.05,
                    argb(k == 0 ? CORE : GLOW, (int) (alpha * (k == 0 ? 0.9 : 0.55))), cam);
                prev = next;
            }
        }
    }

    private static void drawPull(BufferBuilder b, PoseStack ps, Pull p, Vec3 cam, long now) {
        float f = (now - p.born()) / (float) Math.max(1, p.life());
        if (f < 0 || f > 1) return;
        int alpha = (int) (240 * (1 - f));
        Vec3 delta = p.to().subtract(p.from());
        Random rnd = new Random(p.born() * 131 + 7);
        Vec3 side = delta.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 prev = p.from().add(0, 1, 0);
        for (int i = 1; i <= 8; i++) {
            double q = i / 8.0;
            Vec3 next = p.from().lerp(p.to(), q).add(0, 1, 0)
                .add(side.scale((rnd.nextDouble() - 0.5) * 0.5));
            Client.ribbon(b, ps, prev, next, 0.12, argb(GLOW, (int) (alpha * 0.5)), cam);
            Client.ribbon(b, ps, prev, next, 0.04, argb(CORE, alpha), cam);
            prev = next;
        }
    }

    /** 幻兽·琥珀：周围空气的"高温扭曲"用 FOV 脉冲 + 极轻微的镜头抖动来近似。 */
    @SubscribeEvent public static void computeFov(net.minecraftforge.client.event.ViewportEvent.ComputeFov e) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !e.usedConfiguredFov() || mc.level == null) return;
        if (local(ultUntil, now(mc)) > 0) e.setFOV(e.getFOV() * (1.03f + 0.02f * (float) Math.sin(tick * 0.35)));
    }

    @SubscribeEvent public static void cameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || local(ultUntil, now(mc)) <= 0) return;
        e.setRoll(e.getRoll() + 0.55f * (float) Math.sin(tick * 1.7));
        e.setPitch(e.getPitch() + 0.35f * (float) Math.cos(tick * 1.3));
    }

    /** 静电场：地面电网纹路 + 中心那根"铁棒"的光柱。 */
    private static void drawField(BufferBuilder b, PoseStack ps, FieldVis f, Vec3 cam, long now) {
        float fade = (float) Math.min(1, (f.until() - now) / 20.0);
        int glow = argb(GLOW, (int) (120 * fade));
        int core = argb(CORE, (int) (80 * fade));
        Client.ring(b, ps, f.pos().add(0, 0.05, 0), f.radius(), 0.10, glow, cam);
        Client.ring(b, ps, f.pos().add(0, 0.06, 0), f.radius() * 0.66, 0.06, core, cam);
        Client.ring(b, ps, f.pos().add(0, 0.07, 0), f.radius() * 0.33, 0.05, core, cam);
        // 八条辐条 + 折线，像电网的纹路
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + now * 0.01;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            Vec3 prev = f.pos().add(dir.scale(f.radius() * 0.25));
            for (int k = 1; k <= 3; k++) {
                Vec3 next = f.pos().add(dir.scale(f.radius() * (0.25 + k * 0.25)))
                    .add(new Vec3(Math.cos(a + 1.5), 0, Math.sin(a + 1.5)).scale(0.22 * (k % 2)));
                Client.ribbon(b, ps, prev, next, 0.045, glow, cam);
                prev = next;
            }
        }
        // 中心铁棒：一根细光柱
        Client.ribbon(b, ps, f.pos(), f.pos().add(0, 2.4, 0), 0.14, argb(CORE, (int) (200 * fade)), cam);
    }

    /** 落雷预警：地上一个收缩的圈，1 秒后劈下来。 */
    private static void drawWarn(BufferBuilder b, PoseStack ps, WarnVis w, Vec3 cam, long now) {
        float left = (w.until() - now) / 20f;
        float pulse = 0.65f + 0.35f * (float) Math.sin(now * 0.9);
        Client.ring(b, ps, w.pos().add(0, 0.08, 0), w.radius(), 0.09,
            argb(GLOW, (int) (170 * pulse)), cam);
        Client.ring(b, ps, w.pos().add(0, 0.09, 0), w.radius() * Math.max(0.15, left * 0.9), 0.06,
            argb(CORE, (int) (190 * pulse)), cam);
    }

    /** 球形闪电：三圈互相垂直的光环拼出一个电球（外加一点刺）。 */
    private static void drawOrb(BufferBuilder b, PoseStack ps, OrbVis o, Vec3 cam, long now) {
        double r = o.radius();
        int glow = argb(GLOW, 210), core = argb(CORE, 235);
        Client.ring(b, ps, o.pos(), r, 0.06, glow, cam);
        Client.ring(b, ps, o.pos().add(0, r * 0.55, 0), r * 0.82, 0.05, glow, cam);
        Client.ring(b, ps, o.pos().add(0, -r * 0.55, 0), r * 0.82, 0.05, glow, cam);
        // 竖切面：两圈竖直的环，做出"球体"的轮廓
        for (int k = 0; k < 2; k++) {
            double rot = now * 0.12 + k * Math.PI / 2;
            Vec3 side = new Vec3(Math.cos(rot), 0, Math.sin(rot));
            Vec3 prev = o.pos().add(side.scale(r));
            for (int i = 1; i <= 16; i++) {
                double a = i * Math.PI * 2 / 16;
                Vec3 next = o.pos().add(side.scale(Math.cos(a) * r)).add(0, Math.sin(a) * r, 0);
                Client.ribbon(b, ps, prev, next, 0.05, glow, cam);
                prev = next;
            }
        }
        Client.ribbon(b, ps, o.pos().subtract(0, 0.02, 0), o.pos().add(0, 0.02, 0), r * 0.9, core, cam);
    }

    /** 电磁吸附·极：被甩飞的敌人身上罩一层电磁网。 */
    private static void drawNet(BufferBuilder b, PoseStack ps, Minecraft mc, int id, Vec3 cam) {
        if (!(mc.level.getEntity(id) instanceof net.minecraft.world.entity.LivingEntity living)) return;
        double h = living.getBbHeight();
        Vec3 c = living.position().add(0, h * 0.5, 0);
        double r = Math.max(0.6, living.getBbWidth() * 0.9);
        int glow = argb(GLOW, 190);
        Client.ring(b, ps, c.add(0, h * 0.3, 0), r, 0.05, glow, cam);
        Client.ring(b, ps, c, r * 1.05, 0.05, glow, cam);
        Client.ring(b, ps, c.subtract(0, h * 0.3, 0), r, 0.05, glow, cam);
    }

    /** 一次性扩散环（格挡罩 / 球雷爆炸 / 反弹 / 上挑）。 */
    private static void drawRing3(BufferBuilder b, PoseStack ps, Ring3 r, Vec3 cam, long now) {
        float t = (now - r.born()) / (float) Math.max(1, r.life());
        if (t < 0 || t > 1) return;
        int alpha = (int) (210 * (1 - t));
        double radius = Math.max(0.6, r.radius() * Math.min(1, t * 1.25));
        Client.ring(b, ps, r.pos().add(0, 0.12, 0), radius, 0.14, argb(GLOW, alpha), cam);
        Client.ring(b, ps, r.pos().add(0, 0.2, 0), radius * 0.7, 0.06, argb(CORE, alpha), cam);
        // 半球罩：再叠几层抬高的环
        for (int k = 1; k <= 3; k++) {
            double f = k / 4.0;
            Client.ring(b, ps, r.pos().add(0, f * radius * 0.9, 0), radius * Math.sqrt(Math.max(0, 1 - f * f)),
                0.05, argb(CORE, (int) (alpha * 0.5)), cam);
        }
    }

    /** 供客户端"防御键"门禁用：自己当前的负电荷层数。 */
    public static int negativeStacks() { return localNeg(); }

    // ------------------------------------------------------------ HUD
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void hud(RenderGuiEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.level == null) return;
        if (ObserverClient.active()) return;
        long now = now(mc);
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        GuiGraphics g = e.getGuiGraphics();
        // 头顶正电荷：蓝色闪电 + 层数
        for (Overhead o : overheads) {
            if (o.x() < -20 || o.y() < -20 || o.x() > w + 20 || o.y() > h + 20) continue;
            boltIcon(g, o.x(), o.y(), 0xFF8FE6FF);
            g.drawString(mc.font, Integer.toString(o.layers()), o.x() + 9, o.y() - 5, 0xFFE8FBFF);
        }
        long ultEnd = local(ultUntil, now), crashEnd = local(crashUntil, now), attachEnd = local(attachUntil, now);
        int neg = localNeg();
        boolean ult = ultEnd > 0, crash = crashEnd > 0, attach = attachEnd > 0;
        boolean magnet = local(magnetUntil, now) > 0;
        // 雷霆万钧蓄力：屏幕先暗下来（电光在蓄完时才会炸开）
        if (chainDarkUntil > now) {
            float left = (chainDarkUntil - now) / 40f;
            int alpha = (int) (150 * Math.min(1, left));
            g.fill(0, 0, w, h, (alpha << 24) | 0x05070C);
        }
        // 屏幕边缘的蓝白电弧：负电荷越多越亮
        if (neg > 0 && !ult) {
            int alpha = Math.min(210, 30 + neg * 9);
            Random rnd = new Random(tick * 1103515245L + 12345);
            int count = 10 + neg * 2;
            int arcGlow = magnet ? 0xB15AFF : GLOW, arcCore = magnet ? 0xE070FF : CORE;
            for (int i = 0; i < count; i++) {
                int len = 10 + rnd.nextInt(26);
                int inset = rnd.nextInt(4);
                int pos = rnd.nextInt(Math.max(1, w + h));
                boolean horizontal = rnd.nextBoolean();
                int a = Math.max(24, alpha - rnd.nextInt(60));
                if (horizontal) {
                    int x = Math.max(0, Math.min(w - len, pos - len / 2));
                    int y = rnd.nextBoolean() ? inset : h - inset - 2;
                    g.fill(x, y, x + len, y + 2, argb(arcGlow, a));
                    g.fill(x + len / 3, y + (y < h / 2 ? 2 : -3), x + len / 3 + 5, y + (y < h / 2 ? 4 : -1), argb(arcCore, a));
                } else {
                    int y = Math.max(0, Math.min(h - len, pos - len / 2));
                    int x = rnd.nextBoolean() ? inset : w - inset - 2;
                    g.fill(x, y, x + 2, y + len, argb(arcGlow, a));
                    g.fill(x + (x < w / 2 ? 2 : -5), y + len / 3, x + (x < w / 2 ? 6 : -1), y + len / 3 + 4, argb(arcCore, a));
                }
            }
        }
        if (ult) {
            int remain = (int) ((ultEnd - now) / 20);
            for (int i = 0; i < 5; i++) {
                int color = ((30 - i * 5) << 24) | (i % 2 == 0 ? 0x5FCBFF : 0xE8FBFF);
                g.fill(i * 4, 0, i * 4 + 4, h, color);
                g.fill(w - i * 4 - 4, 0, w - i * 4, h, color);
            }
            g.drawCenteredString(mc.font, "幻兽·琥珀 " + remain + " 秒 · 免疫物理 · 必中落雷", w / 2, 16, 0xFFBFF4FF);
        } else if (attach) {
            g.drawCenteredString(mc.font, "雷神·附着 · 普攻挂 2 层正电荷", w / 2, 16, 0xFFFFF3B0);
        }
        if (crash) {
            int remain = (int) ((crashEnd - now) / 20);
            g.drawCenteredString(mc.font, "肉体崩溃 " + remain + " 秒 · 生命锁定 1 · 无法攻击与施术", w / 2, ult ? 28 : 16, 0xFFFF7A85);
        }
        if (neg > 0) {
            g.drawString(mc.font, "负电荷 " + neg + " / " + MAX_NEG, 12, h - 118,
                magnet ? 0xFFE070FF : 0xFFBFF4FF);
        }
        if (magnet) g.drawString(mc.font, "磁化 · 移速+20% · 下次雷走免咒力", 12, h - 129, 0xFFE070FF);
    }

    /** 小闪电图标（6×10 像素，用几块填充拼出来，不额外加贴图）。 */
    private static void boltIcon(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 3, y, x + 7, y + 2, color);
        g.fill(x + 2, y + 2, x + 6, y + 4, color);
        g.fill(x + 1, y + 4, x + 5, y + 6, color);
        g.fill(x + 1, y + 5, x + 6, y + 7, color);
        g.fill(x + 3, y + 7, x + 5, y + 10, color);
    }

    private static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private KashimoVisuals() {}
}
