package cn.blockforge.generated.nnnnr1201forge2;

import character.naoya.NaoyaFx;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 直哉投射咒法的客户端表现：
 * ① 半透明残影（起点位置、朝准星方向的简化人形）；
 * ② 投影节点（青白色小环）与帧线（节点之间拉出的直线）；
 * ③ 命中/截断闪光。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class NaoyaVisuals {
    private record Ghost(int owner, Vec3 at, Vec3 look, long until) {}
    private record Node(Vec3 at, long until) {}
    private record Trail(Vec3 from, Vec3 to, long born, int life) {}
    private static final List<Ghost> GHOSTS = new ArrayList<>();
    private static final List<Node> NODES = new ArrayList<>();
    private static final List<Trail> TRAILS = new ArrayList<>();
    private static long projectUntil;

    public static boolean projecting() {
        var mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getGameTime() < projectUntil;
    }

    public static void receive(Network.Effect e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        switch (e.kind()) {
            case NaoyaFx.PROJECT -> projectUntil = Math.max(projectUntil, now + Math.max(1, e.life()));
            case NaoyaFx.GHOST -> {
                GHOSTS.removeIf(g -> g.owner() == e.owner());
                GHOSTS.add(new Ghost(e.owner(), e.position(), e.direction(), now + Math.max(1, e.life())));
            }
            case NaoyaFx.NODE -> NODES.add(new Node(e.position(), now + Math.max(1, e.life())));
            case NaoyaFx.TRAIL -> TRAILS.add(new Trail(e.position(), e.position().add(e.direction()),
                now, Math.max(4, e.life())));
            case NaoyaFx.HIT, NaoyaFx.INTERCEPT -> {
                if (PhotonFx.ready()) {
                    PhotonFx.spark(e.position(), 1.4, 0.7, 0xFFBFF4FF, 0xFFFFFFFF,
                        "textures/effect/glow.png", true, 5, 8, 16, 0.3);
                }
            }
            default -> { }
        }
    }

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) { GHOSTS.clear(); NODES.clear(); TRAILS.clear(); projectUntil = 0; return; }
        long now = mc.level.getGameTime();
        GHOSTS.removeIf(g -> now >= g.until() || mc.level.getEntity(g.owner()) == null);
        NODES.removeIf(n -> now >= n.until());
        TRAILS.removeIf(t -> now - t.born() > t.life());
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || (GHOSTS.isEmpty() && NODES.isEmpty() && TRAILS.isEmpty())) return;
        long now = mc.level.getGameTime();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // 帧线
        for (Trail trail : TRAILS) {
            float t = (now - trail.born()) / (float) trail.life();
            int alpha = (int) (220 * (1 - t));
            Client.ribbon(builder, ps, trail.from(), trail.to(), 0.10, (alpha << 24) | 0x9FF0FF, cam);
            Client.ribbon(builder, ps, trail.from(), trail.to(), 0.04, (alpha << 24) | 0xFFFFFF, cam);
        }
        // 节点环
        for (Node node : NODES) {
            long left = node.until() - now;
            int alpha = (int) (210 * Math.min(1.0, left / 10.0));
            Client.ring(builder, ps, node.at(), 0.45, 0.07, (alpha << 24) | 0x7FD8FF, cam);
            Client.ring(builder, ps, node.at().add(0, 0.02, 0), 0.22, 0.04, (alpha << 24) | 0xEAF8FF, cam);
        }
        // 半透明残影：起点处的简化人形（头 + 躯干 + 双臂），朝准星方向
        for (Ghost ghost : GHOSTS) {
            long left = ghost.until() - now;
            int alpha = (int) (110 * Math.min(1.0, left / 20.0));
            drawGhost(builder, ps, ghost, (alpha << 24) | 0x9FF0FF);
        }
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    /** 简化人形：几块半透明方片拼出"玩家残影"。 */
    private static void drawGhost(BufferBuilder b, PoseStack ps, Ghost ghost, int color) {
        Vec3 at = ghost.at();
        Vec3 look = ghost.look().multiply(1, 0, 1);
        if (look.lengthSqr() < 0.01) look = new Vec3(0, 0, 1);
        look = look.normalize();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        // 躯干（两层交叉面片，保证从任何角度看都有体积）
        box(b, ps, at.add(0, 0.55, 0), 0.34, 0.62, side, look, color);
        // 头
        box(b, ps, at.add(0, 1.42, 0), 0.26, 0.26, side, look, color);
        // 两臂
        box(b, ps, at.add(side.x * 0.36, 1.05, side.z * 0.36), 0.14, 0.52, side, look, color);
        box(b, ps, at.add(-side.x * 0.36, 1.05, -side.z * 0.36), 0.14, 0.52, side, look, color);
        // 两腿
        box(b, ps, at.add(side.x * 0.16, 0.2, side.z * 0.16), 0.15, 0.5, side, look, color);
        box(b, ps, at.add(-side.x * 0.16, 0.2, -side.z * 0.16), 0.15, 0.5, side, look, color);
    }

    private static void box(BufferBuilder b, PoseStack ps, Vec3 center, double halfWidth, double height,
                            Vec3 side, Vec3 forward, int color) {
        Vec3 top = center.add(0, height / 2, 0), bottom = center.add(0, -height / 2, 0);
        Client.quad(b, ps, bottom.add(side.scale(halfWidth)), top.add(side.scale(halfWidth)),
            top.add(side.scale(-halfWidth)), bottom.add(side.scale(-halfWidth)), color);
        Client.quad(b, ps, bottom.add(forward.scale(halfWidth)), top.add(forward.scale(halfWidth)),
            top.add(forward.scale(-halfWidth)), bottom.add(forward.scale(-halfWidth)), color);
    }

    private NaoyaVisuals() {}
}
