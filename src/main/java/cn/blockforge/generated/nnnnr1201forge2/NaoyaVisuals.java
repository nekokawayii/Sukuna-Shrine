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
    /** 被压成二次元平面的目标：entity id + 到期刻 + 身高。 */
    private record Plane(int entity, long until, double height) {}
    private record Shards(Vec3 at, long born, int life) {}
    private static final List<Ghost> GHOSTS = new ArrayList<>();
    private static final List<Node> NODES = new ArrayList<>();
    /** 设定投影的起点（节点的局部坐标参考点）。 */
    private static Vec3 anchor;
    /** 这份投影属于哪个玩家（平移时按他自己的位置算，多人下不会跟着别人走）。 */
    private static int anchorOwner = -1;
    private static final List<Trail> TRAILS = new ArrayList<>();
    private static final List<Plane> PLANES = new ArrayList<>();
    private static final List<Shards> SHARDS = new ArrayList<>();
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
            case NaoyaFx.PROJECT -> {
                if (e.size() <= 0 && e.life() <= 0) { NODES.clear(); anchor = null; anchorOwner = -1; projectUntil = 0; break; }
                projectUntil = Math.max(projectUntil, now + Math.max(1, e.life()));
                if (anchor == null) { anchor = e.position(); anchorOwner = e.owner(); NODES.clear(); }
            }
            case NaoyaFx.GHOST -> {
                GHOSTS.removeIf(g -> g.owner() == e.owner());
                GHOSTS.add(new Ghost(e.owner(), e.position(), e.direction(), now + Math.max(1, e.life())));
            }
            case NaoyaFx.NODE -> {
                if (anchor == null) anchor = e.position();
                NODES.add(new Node(e.position(), now + Math.max(1, e.life())));
            }
            case NaoyaFx.TRAIL -> TRAILS.add(new Trail(e.position(), e.position().add(e.direction()),
                now, Math.max(4, e.life())));
            case NaoyaFx.RELEASE -> { NODES.clear(); anchor = null; anchorOwner = -1; }
            case NaoyaFx.PLANE -> {
                PLANES.removeIf(plane -> plane.entity() == e.owner());
                PLANES.add(new Plane(e.owner(), now + Math.max(1, e.life()), Math.max(1.2, e.size())));
            }
            case NaoyaFx.SHATTER -> {
                PLANES.removeIf(plane -> plane.entity() == e.owner());
                SHARDS.add(new Shards(e.position(), now, 12));
            }
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
        if (mc.level == null) { GHOSTS.clear(); NODES.clear(); TRAILS.clear(); projectUntil = 0; anchor = null; anchorOwner = -1; return; }
        long now = mc.level.getGameTime();
        GHOSTS.removeIf(g -> now >= g.until() || mc.level.getEntity(g.owner()) == null);
        NODES.removeIf(n -> now >= n.until());
        TRAILS.removeIf(t -> now - t.born() > t.life());
        PLANES.removeIf(plane -> now >= plane.until() || mc.level.getEntity(plane.entity()) == null);
        SHARDS.removeIf(shard -> now - shard.born() > shard.life());
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || (GHOSTS.isEmpty() && NODES.isEmpty() && TRAILS.isEmpty()
            && PLANES.isEmpty() && SHARDS.isEmpty())) return;
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
        // 节点环：整条投影按"玩家当前位置 − 锚点"平移 → 设定完成后地上的投影跟着玩家走
        Vec3 shift = Vec3.ZERO;
        if (anchor != null) {
            var entity = anchorOwner >= 0 ? mc.level.getEntity(anchorOwner) : mc.player;
            if (entity != null) shift = entity.position().subtract(anchor);
        }
        Vec3 previous = null;
        for (Node node : NODES) {
            long left = node.until() - now;
            int alpha = (int) (210 * Math.min(1.0, left / 10.0));
            Vec3 at = node.at().add(shift);
            Client.ring(builder, ps, at, 0.45, 0.07, (alpha << 24) | 0x7FD8FF, cam);
            Client.ring(builder, ps, at.add(0, 0.02, 0), 0.22, 0.04, (alpha << 24) | 0xEAF8FF, cam);
            if (previous != null) {                                  // 地面连线：整条路径一目了然
                Client.ribbon(builder, ps, previous, at, 0.06, (alpha << 24) | 0x9FF0FF, cam);
            }
            previous = at;
        }
        // 半透明残影：起点处的简化人形（头 + 躯干 + 双臂），朝准星方向
        for (Ghost ghost : GHOSTS) {
            long left = ghost.until() - now;
            int alpha = (int) (110 * Math.min(1.0, left / 20.0));
            drawGhost(builder, ps, ghost, (alpha << 24) | 0x9FF0FF);
        }
        // 二次元平面：把目标画成一张朝相机的玻璃片 + 四周玻璃碎片
        for (Plane plane : PLANES) {
            var entity = mc.level.getEntity(plane.entity());
            if (entity == null) continue;
            drawPlane(builder, ps, entity.getBoundingBox().getCenter(), entity.getBbWidth(), plane.height(),
                cam, now);
        }
        for (Shards shard : SHARDS) {
            float t = (now - shard.born()) / (float) shard.life();
            int alpha = (int) (220 * (1 - t));
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                Vec3 dir = new Vec3(Math.cos(angle), 0.6 + (i % 3) * 0.35, Math.sin(angle)).normalize();
                Vec3 from = shard.at();
                Vec3 to = from.add(dir.scale(0.6 + 2.2 * t));
                Client.ribbon(builder, ps, from, to, 0.05, (alpha << 24) | 0xCFEFFF, cam);
            }
        }
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    /** 一张"二次元平面"：朝相机的玻璃片 + 边框 + 几片碎玻璃。 */
    private static void drawPlane(BufferBuilder b, PoseStack ps, Vec3 center, double width, double height,
                                  Vec3 cam, long now) {
        Vec3 look = center.subtract(cam);
        if (look.lengthSqr() < 0.0001) look = new Vec3(0, 0, 1);
        look = look.normalize();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 0.0001) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        double halfW = Math.max(0.45, width * 0.62), halfH = height * 0.5;
        int glass = (150 << 24) | 0xCFEFFF;
        int edge = (210 << 24) | 0x9FF0FF;
        Vec3 tl = center.add(right.scale(-halfW)).add(up.scale(halfH));
        Vec3 tr = center.add(right.scale(halfW)).add(up.scale(halfH));
        Vec3 bl = center.add(right.scale(-halfW)).subtract(up.scale(halfH));
        Vec3 br = center.add(right.scale(halfW)).subtract(up.scale(halfH));
        Client.quad(b, ps, bl, br, tr, tl, glass);
        Client.ribbon(b, ps, tl, tr, 0.045, edge, cam);
        Client.ribbon(b, ps, tr, br, 0.045, edge, cam);
        Client.ribbon(b, ps, br, bl, 0.045, edge, cam);
        Client.ribbon(b, ps, bl, tl, 0.045, edge, cam);
        // 几道斜向裂纹
        for (int i = 0; i < 3; i++) {
            double t = (i + 1) / 4.0;
            Vec3 a = bl.add(br.subtract(bl).scale(t)).add(up.scale(halfH * (0.35 + 0.25 * i)));
            Vec3 z = a.subtract(right.scale(halfW * (0.35 + 0.2 * i)));
            Client.ribbon(b, ps, z, a, 0.02, (170 << 24) | 0xEAF8FF, cam);
        }
        // 四周细碎玻璃刺
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3 + now * 0.02;
            Vec3 out = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))).normalize();
            Vec3 from = center.add(out.scale(Math.min(halfW, halfH) * 0.9));
            Client.ribbon(b, ps, from, from.add(out.scale(0.22)), 0.03, (190 << 24) | 0xFFFFFF, cam);
        }
    }

    /** 简化人形：几块半透明方片拼出"玩家残影"。 */
    private static void drawGhost(BufferBuilder b, PoseStack ps, Ghost ghost, int color) {
        Vec3 look = ghost.look().multiply(1, 0, 1);
        if (look.lengthSqr() < 0.01) look = new Vec3(0, 0, 1);
        look = look.normalize();
        Vec3 at = ghost.at().add(look.scale(1.2));   // 画在身前，朝准星方向，不糊在玩家身上
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
