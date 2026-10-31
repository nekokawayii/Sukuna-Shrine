package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 领域方块的物品形态。
 *
 * <p>潜行 + 右键：切换**自己领域展开**要用的那张图（存 `RyuDomainImage`，领域展开时内壳与地板都用它）。
 * 普通右键照常放置方块。
 */
public class DomainScreenItem extends BlockItem {
    public static final int IMAGES = 8;
    public static final String KEY = "RyuDomainImage";
    private static final String[] NAMES = {
        "落日云海", "无量空处", "银河", "血海", "火焰", "虚空裂隙", "尘埃", "光斑"
    };

    public DomainScreenItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static String imageName(int index) {
        return NAMES[Math.floorMod(index, IMAGES)];
    }

    public static int imageOf(Player player) {
        CompoundTag data = Curse.data(player);
        return Math.floorMod(data.getInt(KEY), IMAGES);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                int next = Math.floorMod(imageOf(sp) + 1, IMAGES);
                Curse.data(sp).putInt(KEY, next);
                sp.displayClientMessage(Component.literal("领域方块外观：" + imageName(next)
                    + "（领域展开时使用）").withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines,
                                          net.minecraft.world.item.TooltipFlag flag) {
        lines.add(Component.literal("领域内壳材料：六个面显示同一张图").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("潜行右键：切换自己领域展开用的图片").withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.literal("放置后潜行右键某一格：单独切换那一格的图片").withStyle(ChatFormatting.DARK_AQUA));
    }
}
