package character.culling;

import cn.blockforge.generated.nnnnr1201forge2.Network;
import cn.blockforge.generated.nnnnr1201forge2.TechniqueInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 死灭洄游「初始登记」三选一面板（r250）。
 *
 * <p>服务端用 {@code Network.Effect(622)} 打开（只发给本人）；点卡片回传
 * {@code TechniqueInput(skill=3200, phase=7, motion=300+下标)}。
 */
public final class CullingStartScreen extends Screen {
    private static final int CARD_W = 150, CARD_H = 74, GAP = 12;
    private static final String[] TITLES = {"暴 力", "富 豪", "规 则"};
    private static final String[] LINES = {
        "开局祝福：伤害 +20%（永久）",
        "开局祝福：积分 +50",
        "开局祝福：免分随机获得一条规则"
    };
    private int hover = -1;

    private CullingStartScreen() { super(Component.literal("死灭洄游 · 登记")); }

    public static void open() { Minecraft.getInstance().setScreen(new CullingStartScreen()); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        var font = Minecraft.getInstance().font;
        int total = 3 * CARD_W + 2 * GAP;
        int startX = (this.width - total) / 2, top = this.height / 2 - CARD_H / 2;
        g.drawCenteredString(font, "死灭回游，已登记。选择一项开局祝福", this.width / 2, top - 34, 0xFFFF5050);
        g.drawCenteredString(font, "规则，已赋予。", this.width / 2, top - 20, 0xFF9A9A9A);
        hover = -1;
        for (int i = 0; i < 3; i++) {
            int x = startX + i * (CARD_W + GAP);
            boolean hot = mouseX >= x && mouseX <= x + CARD_W && mouseY >= top && mouseY <= top + CARD_H;
            if (hot) hover = i;
            g.fill(x - 1, top - 1, x + CARD_W + 1, top + CARD_H + 1, hot ? 0xFFFF6B6B : 0xFF7A2A2A);
            g.fill(x, top, x + CARD_W, top + CARD_H, hot ? 0xFF2A1414 : 0xFF160C0C);
            g.drawCenteredString(font, TITLES[i], x + CARD_W / 2, top + 8, hot ? 0xFFFFD700 : 0xFFFFE8B0);
            int lineY = top + 30;
            for (var line : font.split(Component.literal(LINES[i]), CARD_W - 16)) {
                g.drawCenteredString(font, line, x + CARD_W / 2, lineY, 0xFFC8C8C8);
                lineY += 10;
            }
        }
        g.drawCenteredString(font, "点击卡片确认（确认后才会播报登记完成）", this.width / 2, top + CARD_H + 16, 0xFF8A8A8A);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && hover >= 0) {
            Network.CHANNEL.sendToServer(new TechniqueInput.Input(CullingRules.SKILL, 7,
                false, false, false, false, 300 + hover, -1));
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean isPauseScreen() { return false; }
}
