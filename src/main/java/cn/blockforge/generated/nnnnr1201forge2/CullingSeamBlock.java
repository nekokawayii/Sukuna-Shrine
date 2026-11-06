package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** 死灭洄游立方体结界上的「红色缝隙」（r256）：走进来就被拉进结界。 */
public final class CullingSeamBlock extends Block {
    public CullingSeamBlock(Properties properties) { super(properties); }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p)) return;
        if (p.level().dimension().equals(CullingGame.DIM)) return;
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "雪花噪点闪过，羂索的声音在耳边响起：「欢迎来到死灭回游，棋子。」")
            .withStyle(net.minecraft.ChatFormatting.DARK_RED));
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1f, .7f);
        CullingGame.enter(p);
    }
}
