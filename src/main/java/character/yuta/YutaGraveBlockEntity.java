package character.yuta;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 坟头插刀的方块实体。
 * <p>只承担"这个位置有一把插着的鬼妖村正"的标记；真正的模型由
 * {@code YutaGraveRenderer} 用 {@code oni_muramasa_sheathed.bbmodel} 运行时绘制。
 */
public class YutaGraveBlockEntity extends BlockEntity {
    /** 刀是否已被拔走（拔走后不再绘制）。 */
    private boolean taken;

    public YutaGraveBlockEntity(BlockPos pos, BlockState state) {
        super(cn.blockforge.generated.nnnnr1201forge2.Content.YUTA_GRAVE_BE.get(), pos, state);
    }

    public boolean taken() { return taken; }
    public void taken(boolean value) { this.taken = value; setChanged(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Taken", taken);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        taken = tag.getBoolean("Taken");
    }
}
