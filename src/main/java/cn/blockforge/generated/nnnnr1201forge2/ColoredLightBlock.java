package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 苍 / 赫 / 茈 的**不可见彩色光源方块**。
 *
 * <p>方块本身只声明"我是光源、亮度是 N"，**颜色不在方块里**：Colorful Lighting 会读
 * {@code assets/sukuna/light/emitters.json}，按方块 id 给光上色（bluish/reddish/purple 三块各一色）。
 * 这样光就交给**原版光照引擎**逐格传播 —— 真的照亮周围、被墙挡住就是被挡住（这就是"真实阴影"），
 * 而且装上 Oculus 光影时 Colorful Lighting 会自己给光影包打补丁，颜色照样成立。</p>
 *
 * <p>为什么不用 {@code minecraft:light}（原版光源方块）：那个方块只有亮度一个状态，
 * 三种颜色没法区分，而且它的 emitters 条目是别的资源包的地盘。自建三块互不干扰。</p>
 *
 * <p>玩家砸不掉（{@code strength(-1)}）、推不动（{@code PushReaction.BLOCK}）、不挡光、不挡视线，
 * 所以对玩法零干扰；万一服务器崩在光球还在的时候留下残块，{@link #randomTick} 会在没有任何
 * 活动光源指向它时自己消失。</p>
 */
public class ColoredLightBlock extends Block {
    public ColoredLightBlock(int light){
        super(BlockBehaviour.Properties.of()
            .lightLevel(state->light)
            .strength(-1.0F,3600000.0F)   // 不可破坏：这个方块只是光的载体，不该被当成建筑
            .noCollission()               // 不挡路
            .noOcclusion()                // 不挡光、不挡视线
            .randomTicks()                // 残块自愈（见 randomTick）
            .pushReaction(PushReaction.BLOCK)
            .sound(SoundType.STONE));
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos){return true;}
    /** 没有任何活动光球指向我 = 上一局崩在这里留下的残块，自己清掉。 */
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(GojoColoredLights.isReferenced(pos,this))return;
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        level.getLightEngine().checkBlock(pos);
    }
}
