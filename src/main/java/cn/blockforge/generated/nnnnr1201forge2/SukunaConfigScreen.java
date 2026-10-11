package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/** 模组列表里的本地配置编辑器；草稿只在明确保存后写入原有配置文件。 */
public final class SukunaConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 26;
    private final Screen parent;
    private final List<Entry<?>> entries = new ArrayList<>();
    private final List<EditBox> boxes = new ArrayList<>();
    private int page;
    private Button saveButton;
    private String status = "修改后点保存，再彻底重启游戏";
    private boolean error;

    private static final class Entry<T> {
        final String label;
        final ForgeConfigSpec.ConfigValue<T> value;
        final ForgeConfigSpec.ValueSpec spec;
        String draft;
        String saved;

        Entry(String label, ForgeConfigSpec.ConfigValue<T> value) {
            this.label = label;
            this.value = value;
            this.spec = GrowthConfig.SPEC.getSpec().get(value.getPath());
            this.draft = String.valueOf(GrowthConfig.SPEC.isLoaded() ? value.get() : value.getDefault());
            this.saved = draft;
        }

        boolean isBoolean() { return value instanceof ForgeConfigSpec.BooleanValue; }
        boolean changed() { return !draft.equals(saved); }
        Object parsed() {
            Object result = isBoolean() ? Boolean.valueOf(draft)
                : ConfigNumberInput.parse(draft, value instanceof ForgeConfigSpec.IntValue);
            if (!spec.test(result)) throw new IllegalArgumentException("超出允许范围");
            return result;
        }
        boolean valid() {
            try { parsed(); return true; }
            catch (IllegalArgumentException exception) { return false; }
        }
        @SuppressWarnings("unchecked")
        void apply(Object parsed) { value.set((T) parsed); }
        Component tooltip() {
            String comment = spec.getComment();
            return Component.literal(label + "\n" + (comment == null ? "" : comment)
                + (spec.getRange() == null ? "" : "\n允许范围：" + spec.getRange())
                + "\n默认值：" + value.getDefault() + "\n配置项：" + String.join(".", value.getPath()));
        }
    }

    public SukunaConfigScreen(Screen parent) {
        super(Component.literal("宿傩 · 伏魔御厨子 · 配置"));
        this.parent = parent;
        add("整合包模式（不影响咒力数值）", GrowthConfig.MODPACK_MODE);
        add("允许九个泥土合成控制器", GrowthConfig.CONTROLLER_CRAFTING);
        add("基础咒力上限", GrowthConfig.BASE_ENERGY);
        add("残魂凝练 · 每次普攻加成", GrowthConfig.RESIDUE_ATTACK);
        add("残魂凝练 · 每次术式加成", GrowthConfig.RESIDUE_SPELL);
        add("术式公开 · 概率分母", GrowthConfig.REVEAL_DENOMINATOR);
        stage("每根手指", GrowthConfig.FINGER);
        stage("即身佛融合", GrowthConfig.BODY);
        stage("九相图融合", GrowthConfig.NINE);
        stage("虎杖残影通关", GrowthConfig.SOUL);
        stage("五条悟通关", GrowthConfig.VOID);
        stage("净土通关", GrowthConfig.GUARDIAN);
    }

    private <T> void add(String label, ForgeConfigSpec.ConfigValue<T> value) {
        entries.add(new Entry<>(label, value));
    }
    private void stage(String label, GrowthConfig.Stage stage) {
        add(label + " · 生命加成", stage.health());
        add(label + " · 普攻加成", stage.attack());
        add(label + " · 术式加成", stage.spell());
        add(label + " · 咒力上限加成", stage.energy());
    }
    private boolean canEdit() {
        return minecraft != null && minecraft.level == null && minecraft.getConnection() == null
            && !minecraft.hasSingleplayerServer() && GrowthConfig.SPEC.isLoaded();
    }
    private boolean dirty() { return entries.stream().anyMatch(Entry::changed); }
    private boolean valid() { return entries.stream().allMatch(Entry::valid); }
    private int panelWidth() { return Math.min(480, width - 20); }
    private int left() { return (width - panelWidth()) / 2; }
    private int perPage() { return Math.max(1, Math.min(8, (height - 150) / ROW_HEIGHT)); }
    private int pages() { return (entries.size() + perPage() - 1) / perPage(); }
    private int start() { return page * perPage(); }
    private int end() { return Math.min(entries.size(), start() + perPage()); }

    @Override
    protected void init() {
        // 翻页、确认框返回和窗口缩放都只重建控件，不丢弃草稿。
        clearWidgets();
        boxes.clear();
        page = Math.min(page, pages() - 1);
        int left = left(), w = panelWidth(), fieldWidth = Math.min(115, w / 3);
        boolean editable = canEdit();
        for (int i = start(); i < end(); i++) {
            Entry<?> entry = entries.get(i);
            int x = left + w - fieldWidth, y = 76 + (i - start()) * ROW_HEIGHT;
            if (entry.isBoolean()) {
                Button toggle = addRenderableWidget(Button.builder(toggleText(entry), button -> {
                    if (!canEdit()) return;
                    entry.draft = Boolean.toString(!Boolean.parseBoolean(entry.draft));
                    button.setMessage(toggleText(entry));
                    edited();
                }).bounds(x, y, fieldWidth, 20).build());
                toggle.active = editable;
                toggle.setTooltip(Tooltip.create(entry.tooltip()));
            } else {
                EditBox box = new EditBox(font, x, y, fieldWidth, 20, Component.literal(entry.label));
                box.setMaxLength(32);
                box.setValue(entry.draft);
                box.setEditable(editable);
                box.setTextColor(entry.valid() ? 0xFFFFFF : 0xFF7777);
                box.setTooltip(Tooltip.create(entry.tooltip()));
                box.setResponder(text -> {
                    entry.draft = text;
                    box.setTextColor(entry.valid() ? 0xFFFFFF : 0xFF7777);
                    edited();
                });
                boxes.add(addRenderableWidget(box));
            }
        }
        Button previous = addRenderableWidget(Button.builder(Component.literal("上一页"), b -> {
            page--; init();
        }).bounds(left, height - 67, 70, 20).build());
        previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal("下一页"), b -> {
            page++; init();
        }).bounds(left + w - 70, height - 67, 70, 20).build());
        next.active = page + 1 < pages();
        int buttonWidth = (w - 12) / 3;
        Button reset = addRenderableWidget(Button.builder(Component.literal("本页恢复默认"), b -> confirmReset())
            .bounds(left, height - 41, buttonWidth, 20).build());
        reset.active = editable;
        saveButton = addRenderableWidget(Button.builder(Component.literal("保存配置"), b -> save())
            .bounds(left + buttonWidth + 6, height - 41, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("返回"), b -> onClose())
            .bounds(left + 2 * (buttonWidth + 6), height - 41, buttonWidth, 20).build());
        updateSaveButton();
    }

    private Component toggleText(Entry<?> entry) {
        return Component.literal(Boolean.parseBoolean(entry.draft) ? "开启" : "关闭");
    }
    private void updateSaveButton() {
        if (saveButton != null) saveButton.active = canEdit() && dirty() && valid();
    }
    private void edited() {
        error = !valid();
        status = error ? "红色输入无效：请按提示填写范围内的数字"
            : dirty() ? "有未保存的修改；保存后请重启游戏" : "没有未保存的修改";
        updateSaveButton();
    }
    private void confirmReset() {
        if (!canEdit()) return;
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed && canEdit()) {
                for (int i = start(); i < end(); i++) {
                    Entry<?> entry = entries.get(i);
                    entry.draft = String.valueOf(entry.value.getDefault());
                }
                edited();
            }
            minecraft.setScreen(this);
        }, Component.literal("恢复本页默认值？"), Component.literal("只修改当前页的草稿；点击保存后才会写入配置。"),
            Component.literal("恢复本页"), Component.literal("取消")));
    }
    private void save() {
        if (!canEdit() || !dirty()) return;
        List<Object> parsed = new ArrayList<>();
        // 所有页面一次性校验通过，才允许写入，避免只保存一部分。
        try {
            for (Entry<?> entry : entries) parsed.add(entry.parsed());
        } catch (IllegalArgumentException exception) {
            edited();
            return;
        }
        List<Object> original = new ArrayList<>();
        for (Entry<?> entry : entries) original.add(entry.value.get());
        try {
            for (int i = 0; i < entries.size(); i++) entries.get(i).apply(parsed.get(i));
            GrowthConfig.SPEC.save();
            GrowthConfig.applyCurrentValues();
            for (Entry<?> entry : entries) entry.saved = entry.draft;
            error = false;
            status = "已保存！请彻底退出并重新启动游戏";
        } catch (RuntimeException exception) {
            // 磁盘写入失败时尽量恢复旧值，不把失败伪装成保存成功。
            try {
                for (int i = 0; i < entries.size(); i++) entries.get(i).apply(original.get(i));
                GrowthConfig.SPEC.save();
                GrowthConfig.applyCurrentValues();
            } catch (RuntimeException rollback) {
                exception.addSuppressed(rollback);
            }
            LogUtils.getLogger().error("宿傩配置保存失败", exception);
            error = true;
            status = "保存失败，请检查配置文件权限和游戏日志";
        }
        updateSaveButton();
    }

    @Override
    public void onClose() {
        if (!dirty()) {
            minecraft.setScreen(parent);
            return;
        }
        minecraft.setScreen(new ConfirmScreen(discard -> minecraft.setScreen(discard ? parent : this),
            Component.literal("放弃未保存的修改？"), Component.literal("返回模组列表不会自动保存配置。"),
            Component.literal("放弃修改"), Component.literal("继续编辑")));
    }
    @Override
    public void tick() {
        for (EditBox box : boxes) box.tick();
        updateSaveButton();
    }
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int left = left(), w = panelWidth(), fieldWidth = Math.min(115, w / 3);
        g.fill(left - 6, 5, left + w + 6, height - 5, 0xDA17131D);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFD8E4);
        g.drawCenteredString(font, "本机文件：config/" + GrowthConfig.FILE, width / 2, 28, 0xFFBEB1CD);
        String notice = canEdit() ? "数值填写原始值；联机以服务器配置为准"
            : GrowthConfig.SPEC.isLoaded() ? "当前只读本机配置；请退出世界后再修改" : "配置尚未加载，请稍后重新打开";
        g.drawCenteredString(font, notice, width / 2, 43, 0xFFFFCE7C);
        g.drawCenteredString(font, "鼠标悬停查看说明；保存后需重启游戏", width / 2, 58, 0xFFBEB1CD);
        for (int i = start(); i < end(); i++) {
            Entry<?> entry = entries.get(i);
            int y = 76 + (i - start()) * ROW_HEIGHT;
            g.fill(left, y - 2, left + w, y + 22, 0x803B3045);
            String label = (entry.changed() ? "* " : "") + entry.label;
            g.drawString(font, font.plainSubstrByWidth(label, w - fieldWidth - 12), left + 4, y + 6,
                entry.valid() ? 0xFFE6DCEB : 0xFFFF7777);
            if (mouseX >= left && mouseX < left + w - fieldWidth - 4 && mouseY >= y && mouseY < y + 20)
                setTooltipForNextRenderPass(entry.tooltip());
        }
        g.drawCenteredString(font, "第 " + (page + 1) + " / " + pages() + " 页", width / 2, height - 61, 0xFFE6DCEB);
        g.drawCenteredString(font, font.plainSubstrByWidth(status, w), width / 2, height - 14,
            error ? 0xFFFF7777 : 0xFFA9DDB6);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
