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
 * <p>右键放置；潜行右键已放置的方块，可以单独切换那一格。
 * 石流龙的领域固定使用落日晚霞，不读取旧存档中的 RyuDomainImage。
 */
public class DomainScreenItem extends BlockItem {
    public static final int IMAGES = 9;
    public static final String KEY = "RyuDomainImage";
    private static final String[] NAMES = {
        "落日云海", "落日云海（旧外观兼容）", "银河", "血海", "火焰", "虚空裂隙", "尘埃", "光斑", "霜月雪云"
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
                sp.displayClientMessage(Component.literal("石流龙领域固定使用落日晚霞；潜行右键已放置的方块可单独换图。")
                    .withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines,
                                          net.minecraft.world.item.TooltipFlag flag) {
        lines.add(Component.literal("领域内壳材料：六个面显示同一张图").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("石流龙领域固定使用落日晚霞").withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.literal("放置后潜行右键某一格：单独切换那一格的图片").withStyle(ChatFormatting.DARK_AQUA));
    }
}
