package character.yuta;

import cn.blockforge.generated.nnnnr1201forge2.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 领域内的坟头：插着一把鬼妖村正，右键拔出。
 * <p>用 {@link EntityBlock}（而不是 BaseEntityBlock）避免 1.20.1 强制的 MapCodec 要求。
 */
public class YutaGraveBlock extends Block implements EntityBlock {
    public YutaGraveBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new YutaGraveBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer p)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof YutaGraveBlockEntity grave) || grave.taken()) {
            return InteractionResult.PASS;
        }
        grave.taken(true);
        // 拔出：给一把鬼妖村正，方块本身移除（领域还原时该格会按记录回到原状）。
        ItemStack katana = new ItemStack(Content.YUTA_KATANA.get());
        if (!p.getInventory().add(katana)) {
            p.drop(katana, false);   // 背包满就掉在脚下，避免"拔了却没拿到"
        }
        p.getInventory().setChanged();
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1f, 1.2f);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("拔出了太刀"), true);
        return InteractionResult.CONSUME;
    }
}
