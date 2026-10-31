package character.mahito;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 无为转变轮盘（多页）。
 * mode=0：贮存轮盘（只有「1 储存」）。
 * mode=1：变化轮盘——根页（工具/咒具/具象化/?/攻击/潜能激发）→ 各子页；
 * 具象化页用左右箭头翻页，列出全部生物（含模组生物）。
 */
public final class TransfigureScreen extends Screen {
    private static final int SLOTS = 6;
    private static final double RADIUS = 92;
    private static final int NODE = 52;

    private static final String[] ROOT = {"1 工具", "2 咒具", "3 具象化", "？", "5 攻击", "6 潜能激发"};
    private static final String[] TOOLS = {"剑", "斧", "镐", "锄", "铲", "返回"};
    private static final String[] CURSED = {"黑绳", "屠坐魔", "游云", "神武解", "飞天", "返回"};
    private static final String[] ATTACK = {"墙", "桥", "返回", "？", "？", "？"};

    private final int mode;
    private int page;      // 0=根 1=工具 2=咒具 3=攻击 4=具象化
    private int listPage;

    private TransfigureScreen(int mode) {
        super(Component.literal("无为转变"));
        this.mode = mode;
    }

    public static void open(int mode) {
        if (Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().setScreen(new TransfigureScreen(mode));
    }

    private String label(int i) {
        if (mode == 0) return i == 0 ? "1 储存" : i == 1 ? "2 改变" : i == 2 ? "3 治疗" : i == 3 ? "4 爆裂" : "？";
        if (mode == 3) return new String[]{"1 治疗", "2 分身", "3 遍杀即灵体", "4 肢体", "？", "？"}[i];
        return switch (page) {
            case 1 -> TOOLS[i];
            case 2 -> CURSED[i];
            case 3 -> ATTACK[i];
            case 4 -> entityLabel(i);
            default -> ROOT[i];
        };
    }

    private String entityLabel(int i) {
        List<ResourceLocation> keys = MahitoPowers.entityKeys();
        int index = listPage * SLOTS + i;
        if (index >= keys.size()) return "？";
        String path = keys.get(index).getPath();
        return path.length() > 10 ? path.substring(0, 10) : path;
    }

    private boolean selectable(int i) {
        if (mode == 0) return i <= 3;
        if (mode == 3) return i <= 3;
        return switch (page) {
            case 1, 2 -> true;
            case 3 -> i < 3;
            case 4 -> listPage * SLOTS + i < MahitoPowers.entityKeys().size();
            default -> i != 3;
        };
    }

    private int nodeX(int i) { return (int) (width / 2.0 + Math.cos(-Math.PI / 2 + i * Math.PI * 2 / SLOTS) * RADIUS); }
    private int nodeY(int i) { return (int) (height / 2.0 + Math.sin(-Math.PI / 2 + i * Math.PI * 2 / SLOTS) * RADIUS); }

    private int hovered(double mx, double my) {
        for (int i = 0; i < SLOTS; i++) {
            int dx = (int) (mx - nodeX(i)), dy = (int) (my - nodeY(i));
            if (dx * dx + dy * dy <= (NODE / 2) * (NODE / 2)) return i;
        }
        return -1;
    }

    private boolean leftArrow(double mx, double my) {
        return page == 4 && mx > width / 2.0 - 150 && mx < width / 2.0 - 118 && my > height / 2.0 - 14 && my < height / 2.0 + 14;
    }

    private boolean rightArrow(double mx, double my) {
        return page == 4 && mx > width / 2.0 + 118 && mx < width / 2.0 + 150 && my > height / 2.0 - 14 && my < height / 2.0 + 14;
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int cx = width / 2, cy = height / 2;
        g.fill(cx - 34, cy - 34, cx + 34, cy + 34, 0xCC14171C);
        g.drawCenteredString(font, page == 4 ? "具象化" : "无为转变", cx, cy - 6, 0xFF9FD7E8);
        for (int i = 0; i < SLOTS; i++) {
            int x = nodeX(i), y = nodeY(i);
            boolean hot = hovered(mouseX, mouseY) == i && selectable(i);
            int fill = hot ? 0xEE2C5661 : 0xCC1C2228;
            int border = selectable(i) ? 0xFF9FD7E8 : 0xFF3A434B;
            g.fill(x - NODE / 2 - 1, y - NODE / 2 - 1, x + NODE / 2 + 1, y + NODE / 2 + 1, border);
            g.fill(x - NODE / 2, y - NODE / 2, x + NODE / 2, y + NODE / 2, fill);
            g.drawCenteredString(font, label(i), x, y - 4, selectable(i) ? 0xFFE8F0F5 : 0xFF5D686F);
        }
        if (page == 4) {
            List<ResourceLocation> keys = MahitoPowers.entityKeys();
            int pages = Math.max(1, (keys.size() + SLOTS - 1) / SLOTS);
            boolean l = leftArrow(mouseX, mouseY), r = rightArrow(mouseX, mouseY);
            g.drawString(font, "<", (int) (cx - 134), (int) (cy - 4), l ? 0xFFFFFFFF : 0xFF8899A0);
            g.drawString(font, ">", (int) (cx + 130), (int) (cy - 4), r ? 0xFFFFFFFF : 0xFF8899A0);
            g.drawCenteredString(font, (listPage + 1) + " / " + pages, cx, cy + 96, 0xFF9AA7B0);
            g.drawCenteredString(font, "左右箭头翻页", cx, cy + 120, 0xFFB9C6CF);
        } else if (mode == 0) {
            g.drawCenteredString(font, "选择要贮存的形态", cx, cy + 120, 0xFFB9C6CF);
        } else if (mode == 3) {
            g.drawCenteredString(font, "自我改变", cx, cy + 120, 0xFFB9C6CF);
        } else if (page == 0) {
            g.drawCenteredString(font, "点格子进入子轮盘", cx, cy + 120, 0xFFB9C6CF);
        } else {
            g.drawCenteredString(font, "点「返回」回到上一层", cx, cy + 120, 0xFFB9C6CF);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (leftArrow(mx, my)) { listPage = Math.max(0, listPage - 1); return true; }
        if (rightArrow(mx, my)) {
            int pages = Math.max(1, (MahitoPowers.entityKeys().size() + SLOTS - 1) / SLOTS);
            listPage = Math.min(pages - 1, listPage + 1);
            return true;
        }
        int hit = hovered(mx, my);
        if (hit < 0 || !selectable(hit)) return super.mouseClicked(mx, my, button);
        if (mode == 0) { send(hit == 0 ? 0 : hit == 1 ? 400 : hit == 2 ? 500 : 501); return true; }
        if (mode == 3) { send(510 + hit); return true; }
        switch (page) {
            case 1 -> { if (hit == 5) { page = 0; return true; } send(hit + 1); return true; }
            case 2 -> { if (hit == 5) { page = 0; return true; } send(MahitoTransfigure.FORM_CURSED_BASE + hit); return true; }
            case 3 -> { if (hit == 2) { page = 0; return true; } send(200 + hit); return true; }
            case 4 -> { send(1000 + listPage * SLOTS + hit); return true; }
            default -> {
                if (hit == 0) { page = 1; return true; }
                if (hit == 1) { page = 2; return true; }
                if (hit == 2) { page = 4; listPage = 0; return true; }
                if (hit == 4) { page = 3; return true; }
                if (hit == 5) { send(300); return true; }
                return true;
            }
        }
    }

    private void send(int action) {
        cn.blockforge.generated.nnnnr1201forge2.Network.CHANNEL.sendToServer(
            new cn.blockforge.generated.nnnnr1201forge2.TechniqueInput.Input(
                MahitoSkills.TRANSMUTE, 7, false, false, false, false, action, 0));
        onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
}
