package character.mahito;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 无为转变轮盘：环形 6 格。
 * <p>mode=0：击打目标后选择「1 储存」；mode=1：手持贮存体右键选择「1 变成剑」。
 * 其余 5 格是留给后续形态的空位（显示「？」）。</p>
 */
public final class TransfigureScreen extends Screen {
    private static final int SLOTS = 6;
    private static final double RADIUS = 92;
    private static final int NODE = 52;

    private final int mode;

    private TransfigureScreen(int mode) {
        super(Component.literal("无为转变"));
        this.mode = mode;
    }

    public static void open(int mode) {
        com.mojang.logging.LogUtils.getLogger().info("[无为] 客户端打开轮盘 mode={}", mode);
        if (Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().setScreen(new TransfigureScreen(mode));
    }

    private String label(int i) {
        if (i == 0) return mode == 0 ? "1 储存" : "1 变成剑";
        return "？";
    }

    private int nodeX(int i) {
        return (int) (width / 2.0 + Math.cos(-Math.PI / 2 + i * Math.PI * 2 / SLOTS) * RADIUS);
    }

    private int nodeY(int i) {
        return (int) (height / 2.0 + Math.sin(-Math.PI / 2 + i * Math.PI * 2 / SLOTS) * RADIUS);
    }

    private int hovered(double mx, double my) {
        for (int i = 0; i < SLOTS; i++) {
            int dx = (int) (mx - nodeX(i)), dy = (int) (my - nodeY(i));
            if (dx * dx + dy * dy <= (NODE / 2) * (NODE / 2)) return i;
        }
        return -1;
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int cx = width / 2, cy = height / 2;
        g.fill(cx - 30, cy - 30, cx + 30, cy + 30, 0xCC14171C);
        g.drawCenteredString(font, "无为转变", cx, cy - 6, 0xFF9FD7E8);
        for (int i = 0; i < SLOTS; i++) {
            int x = nodeX(i), y = nodeY(i);
            boolean hot = hovered(mouseX, mouseY) == i && i == 0;
            int fill = hot ? 0xEE2C5661 : 0xCC1C2228;
            int border = i == 0 ? 0xFF9FD7E8 : 0xFF3A434B;
            g.fill(x - NODE / 2 - 1, y - NODE / 2 - 1, x + NODE / 2 + 1, y + NODE / 2 + 1, border);
            g.fill(x - NODE / 2, y - NODE / 2, x + NODE / 2, y + NODE / 2, fill);
            g.drawCenteredString(font, label(i), x, y - 4, i == 0 ? 0xFFE8F0F5 : 0xFF5D686F);
        }
        g.drawCenteredString(font, mode == 0 ? "选择要贮存的形态" : "选择要变化的形态", cx, cy + 120, 0xFFB9C6CF);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (hovered(mx, my) == 0) {
            int action = mode == 0 ? 0 : 1;
            cn.blockforge.generated.nnnnr1201forge2.Network.CHANNEL.sendToServer(
                new cn.blockforge.generated.nnnnr1201forge2.TechniqueInput.Input(
                    character.mahito.MahitoSkills.TRANSMUTE, 7, false, false, false, false, action, 0));
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean isPauseScreen() { return false; }
}
