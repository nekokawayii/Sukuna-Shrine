package character.mainline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** L 键：共同主线成就面板（红框黑底，按分支分组，显示条件与进度）。 */
public final class MainlineScreen extends Screen {
    public MainlineScreen() { super(Component.literal("共同主线")); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        var font = Minecraft.getInstance().font;
        int boxWidth = Math.min(420, width - 40);
        int rows = Mainline.NAMES.length;
        int boxHeight = 34 + rows * 34;
        int x = (width - boxWidth) / 2, y = (height - boxHeight) / 2;
        g.fill(x, y, x + boxWidth, y + boxHeight, 0xE6000000);
        int red = 0xFFFF3B3B;
        g.fill(x, y, x + boxWidth, y + 1, red);
        g.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, red);
        g.fill(x, y, x + 1, y + boxHeight, red);
        g.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, red);
        g.drawString(font, "共同主线 · 成就（L 关闭）", x + 10, y + 8, 0xFFFF8080, false);
        for (int i = 0; i < rows; i++) {
            boolean done = MainlineState.unlocked(i);
            int rowY = y + 26 + i * 34;
            g.drawString(font, (done ? "✔ " : "✖ ") + Mainline.NAMES[i], x + 12, rowY, done ? 0xFFFFD700 : 0xFFB0B0B0, false);
            g.drawString(font, Mainline.REQUIREMENTS[i], x + 12, rowY + 11, 0xFF9A9A9A, false);
            String progress = i == Mainline.SIMPLE ? MainlineState.simple + " / 15"
                : i == Mainline.DOMAIN ? MainlineState.domain + " / 10" : "";
            if (!progress.isEmpty())
                g.drawString(font, progress, x + boxWidth - 12 - font.width(progress), rowY + 11, 0xFFCFCFCF, false);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override public boolean isPauseScreen() { return false; }
}
