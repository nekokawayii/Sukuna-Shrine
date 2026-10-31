package cn.blockforge.generated.nnnnr1201forge2;

import character.todo.TodoFx;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 不义游戏的表现层：被注入咒力的方块/实体上方挂一圈紫色咒力环（永久显示，直到被换位消耗），
 * 换位瞬间两端各炸一圈光环并连一道雷线。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class TodoVisuals {
    private static final int CORE = 0xFFFFFF, GLOW = 0xB58CFF;
    private static final String TEX_GLOW = "textures/effect/glow.png";
    /** 同时最多画多少个标记（防止有人把整片地都注满）。 */
    private static final int MAX_MARKS = 96;

    private record Flash(Vec3 from, Vec3 to, long born, int life) {}

    private static final Set<Integer> entityMarks = new HashSet<>();
    private static final Map<Long, Vec3> blockMarks = new LinkedHashMap<>();
    private static final List<Flash> flashes = new ArrayList<>();
    private static Object world;

    public static void receive(Network.Effect e) {
        var mc = Minecraft.getInstance();
        long now = now(mc);
        switch (e.kind()) {
            case TodoFx.MARK -> {
                if (e.size() >= 2) entityMarks.add(e.owner());
                else blockMarks.put(e.seed(), e.position());
            }
            case TodoFx.UNMARK -> {
                if (e.size() >= 2) entityMarks.remove(e.owner());
                else blockMarks.remove(e.seed());
            }
            case TodoFx.SWAP -> {
                flashes.add(new Flash(e.position(), e.direction(), now, 14));
                if (PhotonFx.ready()) {
                    PhotonFx.spark(e.position(), 1.4, 0.9, 0xFFF0E8FF, 0xFFB58CFF, TEX_GLOW, true, 6, 8, 26, 0.3);
                    PhotonFx.spark(e.direction(), 1.4, 0.9, 0xFFF0E8FF, 0xFFB58CFF, TEX_GLOW, true, 6, 8, 26, 0.3);
                    PhotonFx.ring(e.position().add(0, 0.4, 0), 2.2, 0.5, 0xC0E6D4FF, 0xFFB58CFF, 20, 6, 10);
                    PhotonFx.ring(e.direction().add(0, 0.4, 0), 2.2, 0.5, 0xC0E6D4FF, 0xFFB58CFF, 20, 6, 10);
                }
                Client.shake(3);
            }
            case TodoFx.INJECT -> {
                if (PhotonFx.ready()) {
                    PhotonFx.spark(e.position(), 1.1, 0.8, 0xFFEAD9FF, 0xFFB58CFF, TEX_GLOW, true, 5, 8, 20, 0.25);
                    PhotonFx.ring(e.position(), 1.3, 0.3, 0xA0D9C4FF, 0xFFB58CFF, 16, 5, 8);
                }
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
            entityMarks.clear();
            blockMarks.clear();
            flashes.clear();
            return;
        }
        long now = now(mc);
        flashes.removeIf(f -> now - f.born() > f.life());
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (entityMarks.isEmpty() && blockMarks.isEmpty() && flashes.isEmpty()) return;
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
        int drawn = 0;
        for (Map.Entry<Long, Vec3> entry : blockMarks.entrySet()) {
            if (drawn++ >= MAX_MARKS) break;
            Vec3 at = entry.getValue();
            if (at.distanceToSqr(cam) > 64 * 64) continue;
            mark(b, ps, at.subtract(0, 0.5, 0), 0.55, now, cam);
        }
        for (int id : entityMarks) {
            if (drawn++ >= MAX_MARKS) break;
            Entity entity = mc.level.getEntity(id);
            if (entity == null) continue;
            Vec3 at = entity.getPosition(e.getPartialTick());
            if (at.distanceToSqr(cam) > 64 * 64) continue;
            mark(b, ps, at, Math.max(0.45, entity.getBbWidth() * 0.75), now, cam);
        }
        for (Flash flash : flashes) drawFlash(b, ps, flash, cam, now);
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    /** 一个"有咒力"的标记：地面一圈环 + 一根竖直光柱，带呼吸。 */
    private static void mark(BufferBuilder b, PoseStack ps, Vec3 at, double radius, long now, Vec3 cam) {
        double pulse = 0.85 + 0.15 * Math.sin(now * 0.12 + at.x);
        Client.ring(b, ps, at.add(0, 0.03, 0), radius * pulse, 0.06, argb(GLOW, 150), cam);
        Client.ring(b, ps, at.add(0, 0.04, 0), radius * 0.55 * pulse, 0.04, argb(CORE, 90), cam);
        Client.ribbon(b, ps, at.add(0, 0.05, 0), at.add(0, 1.3, 0), 0.05, argb(GLOW, 130), cam);
        Client.ribbon(b, ps, at.add(0, 0.05, 0), at.add(0, 1.3, 0), 0.02, argb(CORE, 90), cam);
    }

    /** 换位瞬间：两端各一圈炸开的光环 + 一条终点→起点的雷线。 */
    private static void drawFlash(BufferBuilder b, PoseStack ps, Flash flash, Vec3 cam, long now) {
        float t = (now - flash.born()) / (float) Math.max(1, flash.life());
        if (t < 0 || t > 1) return;
        int alpha = (int) (220 * (1 - t));
        double radius = 0.8 + 2.4 * Math.min(1, t * 1.2);
        Client.ring(b, ps, flash.from().add(0, 0.5, 0), radius, 0.12, argb(GLOW, alpha), cam);
        Client.ring(b, ps, flash.to().add(0, 0.5, 0), radius, 0.12, argb(GLOW, alpha), cam);
        Random rnd = new Random(flash.born() * 977 + 31);
        Vec3 prev = flash.from().add(0, 1, 0);
        for (int i = 1; i <= 8; i++) {
            Vec3 next = flash.from().lerp(flash.to(), i / 8.0).add(0, 1, 0)
                .add((rnd.nextDouble() - 0.5) * 0.7, (rnd.nextDouble() - 0.5) * 0.7, (rnd.nextDouble() - 0.5) * 0.7);
            Client.ribbon(b, ps, prev, next, 0.06, argb(GLOW, (int) (alpha * 0.7)), cam);
            Client.ribbon(b, ps, prev, next, 0.02, argb(CORE, alpha), cam);
            prev = next;
        }
    }

    private static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    private TodoVisuals() {}
}
