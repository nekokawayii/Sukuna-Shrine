package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 实体球壳蒙版：方块本身负责碰撞和遮挡，图像按视线投影到可见表面。 */
public class DomainMaskBlock extends BaseEntityBlock {
    public DomainMaskBlock(Properties properties){super(properties);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new DomainMaskBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
}
