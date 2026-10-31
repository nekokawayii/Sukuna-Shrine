package character.mahito;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 肢体菜单（按 U 打开）：显示当前手臂数量与槽位占位。 */
public final class LimbScreen extends Screen {
    private static int arms = 0;
    public LimbScreen() { super(Component.literal("肢体")); }
    public static void setArms(int n) { arms = n; }
    public static int arms() { return arms; }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int cx = width / 2;
        g.drawCenteredString(font, "无为转变 · 肢体", cx, height / 2 - 60, 0xFF9FD7E8);
        g.drawCenteredString(font, "手臂数量：" + arms + " / 4", cx, height / 2 - 30, 0xFFE8F0F5);
        for (int i = 0; i < 4; i++) {
            int x = cx - 90 + i * 46;
            int color = i < arms ? 0xFF2C5661 : 0xFF1C2228;
            g.fill(x, height / 2, x + 40, height / 2 + 40, color);
        }
        g.drawCenteredString(font, "手臂可同时持物（渲染与槽位待接入）", cx, height / 2 + 60, 0xFF9AA7B0);
        super.render(g, mouseX, mouseY, partial);
    }
    @Override public boolean isPauseScreen() { return false; }
}
