package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 《咒术公开玩法手册》：**一个物品，两个显示后端**。
 * <ul>
 *   <li>装了帕秋莉：右键打开帕秋莉版（书 id <code>sukuna:guide</code>）；</li>
 *   <li>没装：右键打开本模组自带的目录式 GUI。</li>
 * </ul>
 * 帕秋莉是可选依赖，这里用反射调用，不引入编译期依赖。
 */
public class ManualItem extends Item {
    public ManualItem() {
        super(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer p)) return InteractionResultHolder.fail(stack);
        if (!ManualOpen.openPatchouli(p)) {
            // 没装帕秋莉：让客户端打开自带 GUI（借既有 Effect 通道，kind 用手册专用号）
            Network.sendTo(p, new Network.Effect(ManualContent.EFFECT_OPEN, p.getId(), p.position(),
                net.minecraft.world.phys.Vec3.ZERO, 0f, 0, 0));
        }
        return InteractionResultHolder.sidedSuccess(stack, true);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<net.minecraft.network.chat.Component> lines,
                                          TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.literal("右键打开玩法手册（帕秋莉已装则用帕秋莉版）")
            .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
