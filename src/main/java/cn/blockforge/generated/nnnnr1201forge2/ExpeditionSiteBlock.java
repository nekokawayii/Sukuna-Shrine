package cn.blockforge.generated.nnnnr1201forge2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
public final class ExpeditionSiteBlock extends BaseEntityBlock {
    public ExpeditionSiteBlock(){super(BlockBehaviour.Properties.copy(Blocks.CHISELED_DEEPSLATE).strength(8,1200));}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ExpeditionSiteEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,ExpeditionContent.SITE.get(),ExpeditionSiteEntity::tick);}
}
