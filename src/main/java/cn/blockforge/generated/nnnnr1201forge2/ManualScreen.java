package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 没装帕秋莉时的自带手册 GUI：左侧分类、右侧条目，滚轮翻页。 */
public class ManualScreen extends Screen {
    private int index;

    public ManualScreen() { super(Component.literal("咒术公开玩法手册")); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        var font = Minecraft.getInstance().font;
        int w = Math.min(360, width - 40), h = Math.min(240, height - 40);
        int x = (width - w) / 2, y = (height - h) / 2;
        g.fill(x, y, x + w, y + h, 0xF0100C18);
        g.fill(x, y, x + w, y + 1, 0xFF7A5CFF);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF7A5CFF);
        g.drawString(font, "咒术公开玩法手册", x + 8, y + 8, 0xFFFFE6A8, false);
        int left = 104;
        for (int i = 0; i < ManualContent.SECTIONS.size(); i++) {
            boolean sel = i == index;
            g.fill(x + 4, y + 26 + i * 18, x + left - 4, y + 42 + i * 18, sel ? 0xFF3A2A66 : 0x00000000);
            g.drawString(font, ManualContent.SECTIONS.get(i).title(), x + 8, y + 28 + i * 18,
                sel ? 0xFFFFE6A8 : 0xFFB8A9E0, false);
        }
        g.fill(x + left - 2, y + 26, x + left - 1, y + h - 6, 0x66FFFFFF);
        int ty = y + 28;
        for (String line : ManualContent.SECTIONS.get(index).lines()) {
            for (var part : font.split(Component.literal(line), w - left - 10)) {
                g.drawString(font, part, x + left + 4, ty, 0xFFE8E0FF, false);
                ty += 11;
            }
            ty += 3;
        }
        g.drawString(font, (index + 1) + " / " + ManualContent.SECTIONS.size() + "   滚轮翻页 · ESC 关闭",
            x + 8, y + h - 14, 0xFF9E90C8, false);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta > 0) index = Math.max(0, index - 1);
        else index = Math.min(ManualContent.SECTIONS.size() - 1, index + 1);
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
