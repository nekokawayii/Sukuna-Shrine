package character;

import cn.blockforge.generated.nnnnr1201forge2.Network;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 咒术选择书（r285 美化版）：按「高专 / 诅咒师 / 咒灵 / 其他」四类分组，
 * 每个角色是一张带主题色竖条的卡片，选中后卡片描边并高亮，底部显示详情。
 */
public final class CharacterSelectionScreen extends Screen {
    /** 分组：0 高专 / 1 诅咒师 / 2 咒灵 / 3 其他。 */
    private static final String[] GROUPS = {"高专", "诅咒师", "咒灵", "其他"};
    private static final int[] GROUP_COLORS = {0xFF6FB7FF, 0xFFB07CFF, 0xFF6FE09B, 0xFFD9DEE8};
    private static final Map<String, Integer> CATEGORY = new LinkedHashMap<>();

    static {
        // 高专
        for (String id : new String[]{"gojo", "yuji", "megumi", "yuta", "nanami", "todo", "hakari", "yuki"}) {
            CATEGORY.put(id, 0);
        }
        // 诅咒师（r286：宿傩也归这里）
        for (String id : new String[]{"uraume", "jujutsu_kaisen_cursed_paths"}) {
            CATEGORY.put(id, 1);
        }
        // 咒灵
        for (String id : new String[]{"jogo", "mahito", "blood"}) {
            CATEGORY.put(id, 2);
        }
        // 其他（r286：石流龙/日车/直哉/甚尔/鹿紫云一 都移到其他）
        for (String id : new String[]{"ryu", "higuruma", "naoya", "toji", "kashimo", "domainking"}) {
            CATEGORY.put(id, 3);
        }
    }

    private static final int CARD_H = 26, CARD_GAP = 4, HEADER_H = 16, COLS = 2;

    private CharacterDefinition choice = CharacterClient.selected();
    private Button confirm;
    /** 卡片点击区：x, y, w, h, 定义。 */
    private final List<Object[]> cards = new ArrayList<>();

    public CharacterSelectionScreen() { super(Component.literal("咒术选择书")); }

    private static int groupOf(String id) { return CATEGORY.getOrDefault(id, 3); }

    @Override
    protected void init() {
        cards.clear();
        int panel = Math.min(640, width - 16);
        int left = (width - panel) / 2;
        var all = CharacterRegistry.all();
        // 先统计每类人数 → 算出可用高度，必要时压缩卡片
        int[] counts = new int[GROUPS.length];
        for (var d : all) counts[groupOf(d.id())]++;
        int rows = 0;
        for (int c = 0; c < GROUPS.length; c++) if (counts[c] > 0) rows += (counts[c] + COLS - 1) / COLS;
        int space = height - 40 - 62 - GROUPS.length * HEADER_H;
        int cardH = Math.max(18, Math.min(CARD_H, rows == 0 ? CARD_H : space / Math.max(1, rows)) - CARD_GAP);
        int cardW = (panel - (COLS - 1) * 6) / COLS;
        int y = 34;
        for (int c = 0; c < GROUPS.length; c++) {
            if (counts[c] == 0) continue;
            y += HEADER_H;
            int slot = 0;
            for (var d : all) {
                if (groupOf(d.id()) != c) continue;
                int cx = left + (slot % COLS) * (cardW + 6);
                int cy = y + (slot / COLS) * (cardH + CARD_GAP);
                cards.add(new Object[]{cx, cy, cardW, cardH, d});
                slot++;
            }
            y += ((counts[c] + COLS - 1) / COLS) * (cardH + CARD_GAP);
        }
        confirm = addRenderableWidget(Button.builder(Component.literal("确认选择"), b -> {
            if (choice != null) {
                Network.CHANNEL.sendToServer(new CharacterPackets.Choose(choice.id()));
                confirm.active = false;
            }
        }).bounds(width / 2 - 102, height - 26, 98, 20).build());
        confirm.active = choice != null;
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
            .bounds(width / 2 + 4, height - 26, 98, 20).build());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Object[] card : cards) {
            int x = (int) card[0], y = (int) card[1], w = (int) card[2], h = (int) card[3];
            if (mx >= x && mx < x + w && my >= y && my < y + h) {
                choice = (CharacterDefinition) card[4];
                if (confirm != null) confirm.active = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void tick() {
        if (minecraft.player == null || !minecraft.player.isAlive()) { onClose(); return; }
        if (confirm != null) confirm.active = choice != null;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xF010141D);
        // 标题栏
        g.fill(0, 0, width, 28, 0xFF161C28);
        g.fill(0, 27, width, 28, 0xFF2C3A52);
        g.drawCenteredString(font, "咒 术 选 择 书", width / 2, 9, 0xFFFFE4B2);

        int panel = Math.min(640, width - 16);
        int left = (width - panel) / 2;
        int[] counts = new int[GROUPS.length];
        for (Object[] card : cards) counts[groupOf(((CharacterDefinition) card[4]).id())]++;

        int y = 34;
        int cardH = 0;
        for (Object[] card : cards) { cardH = (int) card[3]; break; }
        for (int c = 0; c < GROUPS.length; c++) {
            if (counts[c] == 0) continue;
            int color = GROUP_COLORS[c];
            g.fill(left, y + 5, left + 3, y + 13, color);
            g.drawString(font, GROUPS[c], left + 8, y + 5, color, false);
            g.drawString(font, "· " + counts[c], left + 8 + font.width(GROUPS[c]) + 4, y + 5, 0xFF8A93A6, false);
            g.fill(left, y + 14, left + panel, y + 15, 0x332C3A52);
            y += HEADER_H;
            y += ((counts[c] + COLS - 1) / COLS) * (cardH + CARD_GAP);
        }

        // 卡片
        for (Object[] card : cards) {
            int x = (int) card[0], yy = (int) card[1], w = (int) card[2], h = (int) card[3];
            CharacterDefinition d = (CharacterDefinition) card[4];
            boolean hover = mx >= x && mx < x + w && my >= yy && my < yy + h;
            boolean selected = choice != null && choice.id().equals(d.id());
            int bg = selected ? 0xFF232E42 : hover ? 0xFF1B2333 : 0xFF151B26;
            g.fill(x, yy, x + w, yy + h, bg);
            g.fill(x, yy, x + 3, yy + h, d.color());
            if (selected) {
                g.fill(x, yy, x + w, yy + 1, 0xFFF3D9A0);
                g.fill(x, yy + h - 1, x + w, yy + h, 0xFFF3D9A0);
                g.fill(x, yy, x + 1, yy + h, 0xFFF3D9A0);
                g.fill(x + w - 1, yy, x + w, yy + h, 0xFFF3D9A0);
            }
            g.drawString(font, d.name(), x + 8, yy + 4, 0xFFFFFFFF, false);
            String tech = d.technique();
            if (font.width(tech) > w - 14) tech = font.plainSubstrByWidth(tech, w - 16) + "…";
            g.drawString(font, tech, x + 8, yy + h - 12, 0xFF9FB3CC, false);
        }

        // 底部详情
        int dy = height - 58;
        g.fill(0, dy - 6, width, height - 32, 0xE8131925);
        if (choice != null) {
            g.drawString(font, "已选：" + choice.name() + " · " + choice.technique() + " · " + choice.resourceName(),
                left, dy, choice.color(), false);
            String desc = choice.description();
            int maxW = width - 24;
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(desc), maxW);
            int ly = dy + 12;
            for (int i = 0; i < lines.size() && i < 2; i++) {
                g.drawString(font, lines.get(i), 12, ly, 0xFFC9D2DE, false);
                ly += 10;
            }
        } else {
            g.drawCenteredString(font, "点击卡片选择角色，再点「确认选择」", width / 2, dy + 6, 0xFFD1D7E2);
        }
        g.drawString(font, "各角色进度分开保存 · 切换不回血 · 战斗中不能切换", 12, height - 21, 0xFF7F8899, false);
        super.render(g, mx, my, partial);
    }
}
