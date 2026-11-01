package character.mainline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** L 键：共同主线成就面板（红框黑底，按分支分组，显示条件与进度）。 */
public final class MainlineScreen extends Screen {
    private static final int ROW = 34, CELL_W = 34, CELL_H = 18, PER_ROW = 5;
    public MainlineScreen() { super(Component.literal("共同主线")); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        var font = Minecraft.getInstance().font;
        int boxWidth = Math.min(430, width - 40);
        int rows = Mainline.TRAIN_FIRST;                       // 上面 7 个主成就，下面是训练分支
        int gridRows = (Mainline.TRAIN_COUNT + PER_ROW - 1) / PER_ROW;
        int boxHeight = 34 + rows * ROW + 20 + gridRows * (CELL_H + 6) + 16;
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
            int rowY = y + 26 + i * ROW;
            g.drawString(font, (done ? "✔ " : "✖ ") + Mainline.NAMES[i], x + 12, rowY, done ? 0xFFFFD700 : 0xFFB0B0B0, false);
            g.drawString(font, Mainline.REQUIREMENTS[i], x + 12, rowY + 11, 0xFF9A9A9A, false);
            String progress = i == Mainline.SIMPLE ? MainlineState.simple + " / 15"
                : i == Mainline.DOMAIN ? MainlineState.domain + " / 10" : "";
            if (!progress.isEmpty())
                g.drawString(font, progress, x + boxWidth - 12 - font.width(progress), rowY + 11, 0xFFCFCFCF, false);
        }
        // ===== 咒力总量训练：1~10 级小格子 =====
        int gy = y + 26 + rows * ROW + 12;
        g.fill(x + 10, gy - 6, x + boxWidth - 10, gy - 5, 0xFF7A2A2A);
        g.drawString(font, "咒力总量训练（累计击杀咒灵 " + MainlineState.kills + "）", x + 12, gy, 0xFFFFC98A, false);
        int doneCount = 0;
        for (int n = 1; n <= Mainline.TRAIN_COUNT; n++)
            if (MainlineState.unlocked(Mainline.TRAIN_FIRST + n - 1)) doneCount = n;
        String next = doneCount >= Mainline.TRAIN_COUNT ? "已全部完成"
            : "下一级 " + Mainline.TRAIN_KILLS_PER * (doneCount + 1) + " 只 → 上限 +" + Mainline.TRAIN_ENERGY_PER * (doneCount + 1);
        g.drawString(font, next, x + boxWidth - 12 - font.width(next), gy, 0xFFCFCFCF, false);
        int cellTop = gy + 14;
        for (int n = 1; n <= Mainline.TRAIN_COUNT; n++) {
            int col = (n - 1) % PER_ROW, line = (n - 1) / PER_ROW;
            int cx = x + 12 + col * (CELL_W + 4), cy = cellTop + line * (CELL_H + 6);
            boolean done = MainlineState.unlocked(Mainline.TRAIN_FIRST + n - 1);
            boolean reachable = n == 1 || MainlineState.unlocked(Mainline.TRAIN_FIRST + n - 2);
            int border = done ? 0xFFFFD700 : reachable ? 0xFF8A5A5A : 0xFF444444;
            g.fill(cx - 1, cy - 1, cx + CELL_W + 1, cy + CELL_H + 1, border);
            g.fill(cx, cy, cx + CELL_W, cy + CELL_H, done ? 0xFF3A2C00 : 0xFF1A1A1A);
            g.drawCenteredString(font, done ? "✔" + n : "" + n, cx + CELL_W / 2, cy + 5,
                done ? 0xFFFFD700 : reachable ? 0xFFE6C9A8 : 0xFF6A6A6A);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override public boolean isPauseScreen() { return false; }
}
