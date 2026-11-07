package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/** 虎杖重做（r321）表现层：改用 Photon 粒子 + 简单贴图（PhotonBridge），不再用 PhotonFx 光球/光环。 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class YujiFxClient {
    private static final int BLUE = 0xFF6FB6FF, RED = 0xFFFF4A3C, WHITE = 0xFFEAF4FF, BLACK = 0xFF1A0E0E;

    public static void receive(Network.Effect e) {
        Vec3 at = e.position();
        float size = Math.max(.3f, e.size());
        switch (e.kind()) {
            case YujiFx.IKKI -> {          // 二段：蓝色圈从命中点逐渐变大
                RINGS.add(new Ring(at, System.currentTimeMillis(), 14));
                PhotonBridge.smoke(at, BLUE, 1.2, .45, 8, 10);
            }
            case YujiFx.BREAK -> {          // 崩拳：橙色爆尘 + 白闪
                PhotonBridge.smoke(at, BLUE, 1.8, .7, 22, 14);
                PhotonBridge.spawn(at, WHITE, .7, 8, 8);
            }
            case YujiFx.BARRAGE -> PhotonBridge.smoke(at, BLUE, .3, .45, 10, 7);
            case YujiFx.CHARGE -> PhotonBridge.smoke(at, BLUE, 1.2, .5, 12, 16);   // 聚气：尘土回卷
            case YujiFx.CHARGE_HIT -> {
                PhotonBridge.cloud(at, RED, 2.4, .9, 30, 18);
                PhotonBridge.spawn(at, WHITE, .9, 10, 8);
            }
            case YujiFx.RISING -> {          // 上勾：沿竖直方向连串火星
                for (int i = 0; i < 6; i++) PhotonBridge.smoke(at.add(0, i * .7, 0), BLUE, .25, .4, 5, 8 + i);
            }
            case YujiFx.HOOK -> PhotonBridge.smoke(at, BLUE, .3, .5, 8, 8);
            case YujiFx.KICK -> {
                PhotonBridge.smoke(at, BLUE, 1.6, .6, 18, 12);
                PhotonBridge.spawn(at, WHITE, .6, 6, 8);
            }
            case YujiFx.DASH -> {            // 残影：沿路径一串小粒子
                for (int i = 0; i < 8; i++) PhotonBridge.smoke(at.add(e.direction().normalize().scale(i * 1.2)), BLUE, .2, .35, 4, 8);
            }
            case YujiFx.ROLL -> PhotonBridge.smoke(at, BLUE, .8, .45, 8, 10);
            case YujiFx.SWEEP -> {           // 扫堂：贴地一圈尘土
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    PhotonBridge.smoke(at.add(Math.cos(a) * size * .6, .15, Math.sin(a) * size * .6), BLUE, .6, .45, 4, 10);
                }
            }
            case YujiFx.WALL -> PhotonBridge.smoke(at, BLUE, 1.0, .5, 10, 10);
            case YujiFx.WAVE -> {            // 拳风：沿飞行方向拉细线贴图
                for (int i = 1; i <= 5; i++) PhotonBridge.thread(at.add(e.direction().normalize().scale(i * .9)), WHITE, 1.2, 8, 0);
            }
            case YujiFx.WAVE_HIT -> PhotonBridge.smoke(at, BLUE, 1.6, .6, 16, 12);
            case YujiFx.STOMP -> {           // 重踏：地面一圈圈尘土
                for (int i = 0; i < 10; i++) {
                    double a = i * Math.PI / 5;
                    PhotonBridge.smoke(at.add(Math.cos(a) * size * .7, .1, Math.sin(a) * size * .7), BLUE, .9, .6, 6, 14);
                }
                PhotonBridge.smoke(at, BLUE, 2.0, .8, 20, 14);
            }
            case YujiFx.ELBOW -> PhotonBridge.smoke(at, BLUE, .3, .55, 8, 8);
            case YujiFx.KNEE -> PhotonBridge.cloud(at, WHITE, 1.2, .5, 14, 10);
            case YujiFx.BLACK_FLASH -> {     // 黑闪：黑红贴图粒子 + 白闪 + 震屏
                PhotonBridge.cloud(at, BLACK, 2.2, .9, 26, 14);
                PhotonBridge.cloud(at, RED, 1.6, .7, 18, 12);
                PhotonBridge.spawn(at, WHITE, .8, 8, 6);
                Client.shake(12);
            }
            default -> { }
        }
    }

    private record Ring(Vec3 at, long born, int life) { }
    private static final java.util.List<Ring> RINGS = new java.util.ArrayList<>();

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void render(net.minecraftforge.client.event.RenderLevelStageEvent e) {
        if (e.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES || RINGS.isEmpty()) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        long now = System.currentTimeMillis();
        com.mojang.blaze3d.vertex.PoseStack ps = e.getPoseStack();
        Vec3 cam = e.getCamera().getPosition();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        var b = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        b.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        for (var it = RINGS.iterator(); it.hasNext(); ) {
            Ring r = it.next();
            float t = (now - r.born) / (r.life * 50f);
            if (t >= 1f) { it.remove(); continue; }
            int alpha = (int) (220 * (1 - t));
            int color = (alpha << 24) | (BLUE & 0xFFFFFF);
            Client.ring(b, ps, r.at.add(0, .8, 0), .5 + t * 2.6, .10, color, cam);
            Client.ring(b, ps, r.at.add(0, .8, 0), .5 + t * 2.0, .05, (alpha / 2 << 24) | 0xFFFFFF, cam);
        }
        ps.popPose();
        com.mojang.blaze3d.vertex.Tesselator.getInstance().end();
    }

    private YujiFxClient() { }
}
