package character.mahito;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 「改变」——类建模界面：调整待处理生物的身高/宽度/生命。
 * 点「完成」= 直接把这些改动应用到那只怪物身上（不储存、不生成贮存体）。
 */
public final class TransfigureModelScreen extends Screen {
    private static float width_ = 1f, height_ = 2f, health_ = 20f;

    public TransfigureModelScreen() { super(Component.literal("无为转变 · 改变")); }

    public static void open(float w, float h, float hp) {
        width_ = w;
        height_ = h;
        health_ = hp;
        if (Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().setScreen(new TransfigureModelScreen());
    }

    public static void refresh(float w, float h, float hp) {
        width_ = w;
        height_ = h;
        health_ = hp;
    }

    private void act(int action) {
        cn.blockforge.generated.nnnnr1201forge2.Network.CHANNEL.sendToServer(
            new cn.blockforge.generated.nnnnr1201forge2.TechniqueInput.Input(
                MahitoSkills.TRANSMUTE, 7, false, false, false, false, action, 0));
    }

    @Override protected void init() {
        int cx = width / 2;
        int y = height / 2 + 20;
        addRenderableWidget(Button.builder(Component.literal("身高 +0.2"), b -> act(401)).bounds(cx - 170, y, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("身高 -0.2"), b -> act(402)).bounds(cx - 55, y, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("宽度 +0.2"), b -> act(403)).bounds(cx + 60, y, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("宽度 -0.2"), b -> act(404)).bounds(cx - 170, y + 26, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("生命 +5"), b -> act(405)).bounds(cx - 55, y + 26, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("生命 -5"), b -> act(406)).bounds(cx + 60, y + 26, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("完成（直接改变形态）"), b -> { act(410); onClose(); })
            .bounds(cx - 90, y + 58, 180, 20).build());
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int cx = width / 2;
        g.drawCenteredString(font, "无为转变 · 改变（类建模）", cx, height / 2 - 60, 0xFF9FD7E8);
        g.drawCenteredString(font, "身高：" + String.format("%.1f", height_) + "  宽度：" + String.format("%.1f", width_)
            + "  生命：" + (int) health_, cx, height / 2 - 34, 0xFFE8F0F5);
        g.drawCenteredString(font, "点「完成」直接改到怪物身上（不储存）", cx, height / 2 - 16, 0xFF9AA7B0);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override public boolean isPauseScreen() { return false; }
}
