package character.culling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 死灭洄游规则面板（shift+P）。
 *
 * <p>服务端通过 {@code Network.Effect(620)} 下发：size=已制定条数、life=当前积分、seed=5 个候选下标（每个 7 位）。
 * 点卡片把下标回传（TechniqueInput skill=3200 phase=7 motion=下标）；制定满 5 条后顶部出现「脱离」。
 */
public final class CullingRuleScreen extends Screen {
    private static final int CARD_W = 132, CARD_H = 56, GAP = 8;
    private final int picked, points;
    private final int[] options = new int[5];
    private int hover = -1, escapeHover = -1, rerollHover = -1;

    private CullingRuleScreen(int picked, int points, long packed) {
        super(Component.literal("死灭洄游 · 规则"));
        this.picked = picked;
        this.points = points;
        // 服务端存的是 idx+1，0 = 这张卡没有候选
        for (int i = 0; i < 5; i++) options[i] = (int) ((packed >>> (7 * i)) & 0x7F) - 1;
    }

    public static void open(int picked, int points, long packed) {
        Minecraft.getInstance().setScreen(new CullingRuleScreen(picked, points, packed));
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        var font = Minecraft.getInstance().font;
        int width = Math.min(720, this.width - 30);
        int height = 150;
        int x = (this.width - width) / 2, y = (this.height - height) / 2;
        g.fill(x, y, x + width, y + height, 0xE6100A0A);
        int red = 0xFFFF3B3B;
        g.fill(x, y, x + width, y + 1, red);
        g.fill(x, y + height - 1, x + width, y + height, red);
        g.fill(x, y, x + 1, y + height, red);
        g.fill(x + width - 1, y, x + width, y + height, red);
        g.drawString(font, "死灭洄游 · 规则制定", x + 12, y + 8, 0xFFFF8080, false);
        String head = "积分 " + points + "   已制定 " + picked + " 条（可无限制定）   每条消耗 " + CullingRules.COST + " 分";
        g.drawString(font, head, x + width - 12 - font.width(head), y + 8, 0xFFE8D9B0, false);

        // 满 5 条：顶部出现「脱离」
        escapeHover = -1;
        if (picked >= CullingRules.ESCAPE_AT) {
            int bw = 96, bh = 22, bx = (this.width - bw) / 2, by = y - 30;
            boolean hot = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
            escapeHover = hot ? 1 : 0;
            g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, hot ? 0xFFFF6B6B : 0xFF7A2A2A);
            g.fill(bx, by, bx + bw, by + bh, hot ? 0xFF3A0A0A : 0xFF1A0E0E);
            g.drawCenteredString(font, "脱 离", bx + bw / 2, by + 7, hot ? 0xFFFFFFFF : 0xFFFFB0B0);
        } else {
            String tip = "还差 " + (CullingRules.ESCAPE_AT - picked) + " 条规则才能脱离";
            g.drawString(font, tip, (this.width - font.width(tip)) / 2, y - 22, 0xFF9A9A9A, false);
        }

        // 刷新卡池（25 分）
        rerollHover = -1;
        {
            int bw = 110, bh = 18, bx = (this.width - bw) / 2, by = y + height - 34;
            boolean hot = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
            rerollHover = hot ? 1 : 0;
            boolean can = points >= CullingRules.REROLL_COST;
            g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, hot && can ? 0xFF66CCFF : 0xFF2A4A5A);
            g.fill(bx, by, bx + bw, by + bh, hot && can ? 0xFF102A38 : 0xFF0C1418);
            g.drawCenteredString(font, "刷新卡池 " + CullingRules.REROLL_COST + " 分", bx + bw / 2, by + 5,
                can ? 0xFFBFE8FF : 0xFF5A6A70);
        }

        // 五张候选卡
        int total = 5 * CARD_W + 4 * GAP;
        int startX = (this.width - total) / 2, top = y + 34;
        hover = -1;
        for (int i = 0; i < 5; i++) {
            int cx = startX + i * (CARD_W + GAP);
            boolean hot = mouseX >= cx && mouseX <= cx + CARD_W && mouseY >= top && mouseY <= top + CARD_H;
            if (hot) hover = i;
            CullingRules.Rule rule = options[i] >= 0 && options[i] < CullingRules.ALL.size() ? CullingRules.ALL.get(options[i]) : null;
            boolean affordable = points >= CullingRules.COST;
            int border = hot && affordable ? 0xFFFFD700 : 0xFF7A2A2A;
            g.fill(cx - 1, top - 1, cx + CARD_W + 1, top + CARD_H + 1, border);
            g.fill(cx, top, cx + CARD_W, top + CARD_H, hot ? 0xFF2A1414 : 0xFF160C0C);
            if (rule == null) { g.drawCenteredString(font, "—", cx + CARD_W / 2, top + CARD_H / 2 - 4, 0xFF6A6A6A); continue; }
            g.drawString(font, rule.name(), cx + 6, top + 5, affordable ? 0xFFFFE8B0 : 0xFF8A7A6A, false);
            g.drawString(font, "【" + rule.group() + "】", cx + CARD_W - 6 - font.width("【" + rule.group() + "】"), top + 5, 0xFF8A9AA8, false);
            int lineY = top + 19;
            for (var line : font.split(Component.literal(rule.desc()), CARD_W - 12)) {
                if (lineY > top + CARD_H - 10) break;
                g.drawString(font, line, cx + 6, lineY, 0xFFC8C8C8, false);
                lineY += 10;
            }
        }
        String hint = "点击卡片制定规则（可无限制定）· 满 " + CullingRules.ESCAPE_AT + " 条后出现「脱离」";
        g.drawCenteredString(font, hint, this.width / 2, y + height - 14, 0xFF8A8A8A);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int y = (this.height - 150) / 2;
        if (rerollHover == 1 && points >= CullingRules.REROLL_COST) { send(CullingRules.REROLL); return true; }
        if (picked >= CullingRules.ESCAPE_AT && escapeHover == 1) { send(CullingRules.ESCAPE); return true; }
        if (hover >= 0 && points >= CullingRules.COST) { send(hover); return true; }
        return super.mouseClicked(mx, my, button);
    }

    private void send(int index) {
        cn.blockforge.generated.nnnnr1201forge2.Network.CHANNEL.sendToServer(
            new cn.blockforge.generated.nnnnr1201forge2.TechniqueInput.Input(CullingRules.SKILL, 7, false, false, false, false, index, -1));
        onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
}
