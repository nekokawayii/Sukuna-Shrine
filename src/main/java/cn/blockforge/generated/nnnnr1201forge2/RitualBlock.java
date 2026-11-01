package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public final class RitualBlock extends BaseEntityBlock {
    public final int tier;
    public RitualBlock(int tier){super(BlockBehaviour.Properties.copy(Blocks.CRYING_OBSIDIAN).strength(8,1200).lightLevel(s->6));this.tier=tier;}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new RitualBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,ExpeditionContent.RITUAL.get(),RitualBlockEntity::tick);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand h,BlockHitResult hit){if(h==InteractionHand.MAIN_HAND&&p instanceof ServerPlayer sp&&l.getBlockEntity(pos) instanceof RitualBlockEntity be)be.interact(sp);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void onRemove(BlockState s,Level l,BlockPos pos,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(pos) instanceof RitualBlockEntity be)be.destroy();super.onRemove(s,l,pos,next,moving);}
}
