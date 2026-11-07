package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * 「羂索的邀请函」演出（r257）：原地展开一个逐渐变大的黑色立方体，把周围的人吸进去。
 *
 * <p>服务端用 {@code Network.Effect(623)} 广播（position=展开点、life=时长刻）；客户端收到后
 * 强制切第三人称，画 40 刻的扩张黑立方（面 + 棱线），播完自动切回第一人称。
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "jujutsu_kaisen_cursed_paths", value = Dist.CLIENT)
public final class CullingCubeClient {
    private static final int LIFE = 40;
    private static Vec3 center;
    private static long start, until;
    private static boolean forcedThird;

    public static void receive(Network.Effect e) {
        center = e.position();
        start = System.currentTimeMillis();
        until = start + LIFE * 50L;
        var mc = Minecraft.getInstance();
        if (!forcedThird) {
            forcedThird = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !forcedThird) return;
        if (System.currentTimeMillis() > until + 400) {
            forcedThird = false;
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (center == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long age = System.currentTimeMillis() - start;
        if (age < 0) return;
        if (age > LIFE * 50L + 800) { center = null; return; }
        double t = Math.min(1.0, age / (LIFE * 50.0));
        double half = 0.4 + 5.6 * t;
        int alpha = (int) (200 * (1.0 - Math.pow(t, 4)));
        if (alpha <= 2) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Vec3 mn = center.add(-half, -half * .6, -half), mx = center.add(half, half * .9, half);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        // 六个面（半透明黑）
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int face = (Math.min(200, alpha / 2) << 24);
        Client.quad(b, ps, new Vec3(mn.x, mn.y, mn.z), new Vec3(mx.x, mn.y, mn.z), new Vec3(mx.x, mn.y, mx.z), new Vec3(mn.x, mn.y, mx.z), face);
        Client.quad(b, ps, new Vec3(mn.x, mx.y, mn.z), new Vec3(mn.x, mx.y, mx.z), new Vec3(mx.x, mx.y, mx.z), new Vec3(mx.x, mx.y, mn.z), face);
        Client.quad(b, ps, new Vec3(mn.x, mn.y, mn.z), new Vec3(mn.x, mn.y, mx.z), new Vec3(mn.x, mx.y, mx.z), new Vec3(mn.x, mx.y, mn.z), face);
        Client.quad(b, ps, new Vec3(mx.x, mn.y, mn.z), new Vec3(mx.x, mx.y, mn.z), new Vec3(mx.x, mx.y, mx.z), new Vec3(mx.x, mn.y, mx.z), face);
        Client.quad(b, ps, new Vec3(mn.x, mn.y, mn.z), new Vec3(mn.x, mx.y, mn.z), new Vec3(mx.x, mx.y, mn.z), new Vec3(mx.x, mn.y, mn.z), face);
        Client.quad(b, ps, new Vec3(mn.x, mn.y, mx.z), new Vec3(mx.x, mn.y, mx.z), new Vec3(mx.x, mx.y, mx.z), new Vec3(mn.x, mx.y, mx.z), face);
        BufferUploader.drawWithShader(b.end());
        // 十二条棱（暗红发光）
        b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int edge = (Math.min(255, alpha + 40) << 24) | 0x401018;
        Vec3[] v = {
            new Vec3(mn.x, mn.y, mn.z), new Vec3(mx.x, mn.y, mn.z), new Vec3(mx.x, mn.y, mx.z), new Vec3(mn.x, mn.y, mx.z),
            new Vec3(mn.x, mx.y, mn.z), new Vec3(mx.x, mx.y, mn.z), new Vec3(mx.x, mx.y, mx.z), new Vec3(mn.x, mx.y, mx.z)
        };
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] pair : edges) Client.ribbon(b, ps, v[pair[0]], v[pair[1]], 0.12, edge, cam);
        BufferUploader.drawWithShader(b.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    private CullingCubeClient() { }
}
