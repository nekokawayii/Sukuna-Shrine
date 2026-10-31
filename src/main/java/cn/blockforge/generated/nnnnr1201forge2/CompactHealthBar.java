package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 超长血条的替代渲染。
 * <p>原版血条按 {@code maxHealth / 2} 逐颗画心：生命 24000 时每帧要画 12000 个心，
 * 生存模式直接掉到个位数帧。这里在生命上限超过 40 时取消原版渲染，改画一条紧凑血条 + 数字。
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class CompactHealthBar {
    /** 超过这个生命上限就改用紧凑血条（原版最多 20 颗心）。 */
    private static final float THRESHOLD = 40f;

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) return;
        float max = player.getMaxHealth();
        if (max <= THRESHOLD) return;   // 正常血量仍走原版

        event.setCanceled(true);

        var g = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int x = width / 2 - 91;
        int y = height - 39;
        int barWidth = 81;
        int barHeight = 5;

        // 底槽
        g.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xA0000000);
        // 血量比例
        float ratio = Math.max(0f, Math.min(1f, player.getHealth() / max));
        int fill = (int) (barWidth * ratio);
        int color = ratio > 0.5f ? 0xFFFF4040 : ratio > 0.25f ? 0xFFFFA040 : 0xFFFF2020;
        g.fill(x, y, x + Math.max(1, fill), y + barHeight, color);

        // 数字（心形符号不可用时用文字）
        String label = (int) Math.ceil(player.getHealth()) + " / " + (int) max;
        g.drawString(mc.font, label, x, y - 10, 0xFFFFE0E0, true);

        // 吸收伤害（金心）用一条细线表示，避免再画心
        float absorb = player.getAbsorptionAmount();
        if (absorb > 0) {
            g.fill(x, y + barHeight + 1, x + barWidth, y + barHeight + 3, 0xFFFFD257);
        }
    }

    private CompactHealthBar() {}
}
