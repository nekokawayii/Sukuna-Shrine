package character.mahito;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 无为转变·贮存体菜单（第一版只有一个选项：变成剑）。 */
public final class TransfigureScreen extends Screen {
    private TransfigureScreen() { super(Component.literal("无为转变")); }

    public static void open() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        Minecraft.getInstance().setScreen(new TransfigureScreen());
    }

    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("1 变成剑"), b -> {
            cn.blockforge.generated.nnnnr1201forge2.Network.CHANNEL.sendToServer(
                new cn.blockforge.generated.nnnnr1201forge2.Network.Transfigure(1));
            onClose();
        }).bounds(width / 2 - 80, height / 2 + 46, 160, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
            .bounds(width / 2 - 80, height / 2 + 72, 160, 20).build());
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.drawCenteredString(font, "无为转变 · 贮存体", width / 2, height / 2 - 70, 0xFF9FD7E8);
        ItemStack held = Minecraft.getInstance().player == null ? ItemStack.EMPTY
            : Minecraft.getInstance().player.getMainHandItem();
        CompoundTag tag = held.getTagElement(MahitoTransfigure.BASE_TAG);
        int y = height / 2 - 44;
        if (tag == null) {
            g.drawCenteredString(font, "没有贮存数据", width / 2, y, 0xFFAAAAAA);
        } else {
            g.drawCenteredString(font, "来源：" + tag.getString("name"), width / 2, y, 0xFFE8F0F5);
            g.drawCenteredString(font, "生命：" + (int) tag.getFloat("health") + " / " + (int) tag.getFloat("maxHealth"),
                width / 2, y + 14, 0xFFE8F0F5);
            g.drawCenteredString(font, "体型：" + tag.getFloat("width") + " × " + tag.getFloat("height"),
                width / 2, y + 28, 0xFFE8F0F5);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override public boolean isPauseScreen() { return false; }
}
