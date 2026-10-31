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
    /** 本次窗口里玩家已经按过的键（用于筛选提示）。 */
    private static final java.util.List<Integer> PRESSED = new ArrayList<>();
    private static int markedEntity = -1;

    public static void receive(Network.Effect e) {
        var mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        switch (e.kind()) {
            case TojiFx.QTE_ON -> {
                windowUntil = Math.max(windowUntil, now + Math.max(1, e.life()));
                TojiQteClient.extend(Math.max(1, e.life()));
                markedEntity = (int) e.size();
                if (markedEntity != -1) PRESSED.clear();               // 新的红光窗口：重新开始筛选
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

    /** 客户端记录自己发出去的连招键，屏幕提示据此逐步筛选。 */
    static void noteToken(int token) {
        if (PRESSED.size() < 16) PRESSED.add(token);
    }

    /** 收到服务端的窗口/连招窗口包时延长本地接管时间。 */
    static void extend(long ticks) {
        var level = Minecraft.getInstance().level;
        if (level != null) windowUntil = Math.max(windowUntil, level.getGameTime() + ticks);
    }

    static void reset() { windowUntil = 0; PRESSED.clear(); }

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) { MARKS.clear(); BURSTS.clear(); windowUntil = 0; return; }
        long now = mc.level.getGameTime();
        MARKS.removeIf(mark -> now >= mark.until() || mc.level.getEntity(mark.entity()) == null);
        BURSTS.removeIf(burst -> now - burst.born() > burst.life());
    }

    /**
     * HUD 主入口（由 {@link TojiOverlay} 在模组总线上注册后调用）：
     * 屏幕边缘红光 + 连招提示面板。
     */
    public static void drawOverlay(net.minecraft.client.gui.GuiGraphics graphics, int width, int height) {
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
        hints(graphics, width, height);
    }

    /**
     * 连招提示面板：**红框 + 黑底**，按键序列 + 招式名；已按过的键会筛选掉不可能连招，
     * 下一键用红色标出来。
     */
    private static void hints(net.minecraft.client.gui.GuiGraphics graphics, int width, int height) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = mc.level.getGameTime();
        if (now >= windowUntil) { if (!PRESSED.isEmpty()) PRESSED.clear(); return; }
        var hints = new java.util.ArrayList<character.toji.TojiHints.Hint>();
        for (var hint : character.toji.TojiHints.ALL) {
            if (matches(hint.keys())) hints.add(hint);
        }
        if (hints.isEmpty()) return;
        var font = mc.font;
        int lineHeight = 10;
        int shown = Math.min(hints.size(), 14);
        int boxWidth = 150;
        int boxHeight = 18 + shown * lineHeight;
        int x = width - boxWidth - 10;
        int y = (height - boxHeight) / 2;
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xE6000000);            // 黑底
        int red = 0xFFFF3B3B;
        graphics.fill(x, y, x + boxWidth, y + 1, red);                           // 红框（四边）
        graphics.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, red);
        graphics.fill(x, y, x + 1, y + boxHeight, red);
        graphics.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, red);
        graphics.drawString(font, "完美闪避 · 连招（点键筛选）", x + 5, y + 4, 0xFFFF8080, false);
        for (int i = 0; i < shown; i++) {
            var hint = hints.get(i);
            int rowY = y + 15 + i * lineHeight;
            String keys = hint.keyText();
            graphics.drawString(font, keys, x + 5, rowY, 0xFFFFD0D0, false);
            graphics.drawString(font, hint.name(), x + 5 + Math.max(34, font.width(keys) + 6), rowY,
                0xFFE0E0E0, false);
        }
    }

    /** 已按过的键必须是这一招按键序列的子序列（按顺序出现过即可）。 */
    private static boolean matches(int[] keys) {
        int index = 0;
        for (int pressed : PRESSED) {
            if (index < keys.length && pressed == keys[index]) index++;
        }
        return index == PRESSED.size();
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
