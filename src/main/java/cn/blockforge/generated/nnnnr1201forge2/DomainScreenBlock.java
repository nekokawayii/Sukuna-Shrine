package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 领域方块（石流龙「打上花火」的内壳材料）。
 *
 * <p>本质是"绿幕"：六个面都贴同一张图，`image` 属性 0~7 决定贴哪一张，
 * 所以不同领域可以长出完全不同的外观（`assets/sukuna/textures/effect/domain_*.png` 或复用的特效图）。
 * 不可破坏、无掉落；领域收起时由 {@code RyuDomain} 逐格还原。
 */
public class DomainScreenBlock extends Block {
    /** 0=落日云海 1=无量空处 2=银河 3=血海 4=火焰 5=虚空裂隙 6=尘埃 7=光斑 */
    public static final IntegerProperty IMAGE = IntegerProperty.create("image", 0, DomainScreenItem.IMAGES - 1);

    public DomainScreenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(IMAGE, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IMAGE);
    }

    /** 潜行 + 右键：切换这一格显示的图片（每格独立）。 */
    @Override public InteractionResult use(BlockState state, Level level, net.minecraft.core.BlockPos pos,
                                           Player player, InteractionHand hand,
                                           net.minecraft.world.phys.BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            int next = (state.getValue(IMAGE) + 1) % DomainScreenItem.IMAGES;
            level.setBlock(pos, state.setValue(IMAGE, next), 3);
            player.displayClientMessage(Component.literal("领域方块：" + DomainScreenItem.imageName(next))
                .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
