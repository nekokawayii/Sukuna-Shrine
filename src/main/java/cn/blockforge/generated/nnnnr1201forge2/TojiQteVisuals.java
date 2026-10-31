package cn.blockforge.generated.nnnnr1201forge2;

import character.toji.TojiFx;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 甚尔 QTE 的客户端表现：
 * ① 屏幕四边红光（窗口 0.8 秒内呼吸闪烁）；
 * ② 被锁定的生物/弹幕身上包一圈会收缩的红环；
 * ③ 连招命中时播对应的自绘动作 + 红色冲击波。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class TojiQteVisuals {
    private record Mark(int entity, long until) {}
    private record Burst(Vec3 at, int combo, long born, int life) {}
    private static final List<Mark> MARKS = new ArrayList<>();
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static long windowUntil;
    private static int markedEntity = -1;

    public static void receive(Network.Effect e) {
        var mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        switch (e.kind()) {
            case TojiFx.QTE_ON -> {
                windowUntil = Math.max(windowUntil, now + Math.max(1, e.life()));
                TojiQteClient.extend(Math.max(1, e.life()));
                markedEntity = (int) e.size();
                if (markedEntity == -1) { Client.shake(1); break; }   // 只延长连招输入窗口
                if (mc.level != null && mc.level.getEntity(markedEntity) != null) {
                    MARKS.add(new Mark(markedEntity, now + Math.max(1, e.life())));
                }
                Client.shake(1);
            }
            case TojiFx.MARK -> MARKS.add(new Mark(e.owner(), now + Math.max(1, e.life())));
            case TojiFx.DODGE -> {
                if (PhotonFx.ready()) {
                    PhotonFx.spark(e.position(), 1.6, 0.8, 0xFFFF3B3B, 0xFFFF8A8A,
                        "textures/effect/glow.png", true, 6, 8, 20, 0.35);
                }
            }
            case TojiFx.COMBO -> {
                int combo = (int) e.size();
                BURSTS.add(new Burst(e.position(), combo, now, 14));
                if (mc.level != null && mc.level.getEntity(e.owner()) instanceof AbstractClientPlayer player) {
                    MotionFallbackClient.play(player, "sukuna:toji_c" + combo,
                        new java.util.UUID(e.owner(), Math.max(1L, (long) e.size())), 0);
                }
                Client.shake(2);
            }
            default -> { }
        }
    }

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) { MARKS.clear(); BURSTS.clear(); windowUntil = 0; return; }
        long now = mc.level.getGameTime();
        MARKS.removeIf(mark -> now >= mark.until() || mc.level.getEntity(mark.entity()) == null);
        BURSTS.removeIf(burst -> now - burst.born() > burst.life());
    }

    /** 屏幕边缘红光：窗口内才画，用四段渐变条。 */
    @SubscribeEvent public static void overlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("sukuna_toji_qte", (gui, graphics, partialTick, width, height) -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            long now = mc.level.getGameTime();
            if (now >= windowUntil) return;
            float pulse = 0.55f + 0.45f * (float) Math.sin(now * 0.9);
            int alpha = (int) (120 * pulse);
            int color = (alpha << 24) | 0xFF2020;
            int edge = Math.max(8, width / 14);
            int vertical = Math.max(8, height / 9);
            graphics.fillGradient(0, 0, width, vertical, color, 0x00000000);
            graphics.fillGradient(0, height - vertical, width, height, 0x00000000, color);
            graphics.fillGradient(0, 0, edge, height, color, 0x00FF2020);
            graphics.fillGradient(width - edge, 0, width, height, 0x00FF2020, color);
        });
    }

    /** 红环 + 连招冲击波（世界内，画在粒子之后）。 */
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || (MARKS.isEmpty() && BURSTS.isEmpty())) return;
        long now = mc.level.getGameTime();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Mark mark : MARKS) {
            Entity entity = mc.level.getEntity(mark.entity());
            if (entity == null) continue;
            Vec3 center = entity.getBoundingBox().getCenter();
            double radius = Math.max(0.8, entity.getBbWidth() * 0.85);
            long left = mark.until() - now;
            double shrink = 1.0 + 0.25 * Math.sin(now * 0.6 + mark.entity());
            int alpha = (int) (200 * Math.min(1.0, left / 6.0));
            Client.ring(builder, ps, center, radius * shrink, 0.09, (alpha << 24) | 0xFF3B3B, cam);
            Client.ring(builder, ps, center, radius * shrink * 0.72, 0.05, (alpha << 24) | 0xFF8C8C, cam);
            if (entity instanceof LivingEntity living) {
                double top = living.getBoundingBox().maxY + 0.35 + 0.08 * Math.sin(now * 0.4);
                Client.ring(builder, ps, new Vec3(center.x, top, center.z), radius * 0.55, 0.05,
                    (alpha << 24) | 0xFF5555, cam);
            }
        }
        for (Burst burst : BURSTS) {
            float t = (now - burst.born()) / (float) burst.life();
            int alpha = (int) (220 * (1 - t));
            double radius = 0.5 + 3.2 * Math.min(1, t * 1.4);
            Client.ring(builder, ps, burst.at(), radius, 0.14, (alpha << 24) | 0xFF3030, cam);
            Client.ring(builder, ps, burst.at().add(0, 0.55, 0), radius * 0.6, 0.08, (alpha << 24) | 0xFFD0D0, cam);
        }
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(builder.end());
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableCull();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        ps.popPose();
    }

    private TojiQteVisuals() {}
}
