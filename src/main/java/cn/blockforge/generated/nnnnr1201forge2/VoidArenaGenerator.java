package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.block.Blocks;
import java.util.concurrent.*;

/** 竞技场位于 Y=-60 的基岩层，直径96格，独立空间避免破坏已有建筑。 */
public final class VoidArenaGenerator extends FlatLevelSource {
    public static final Codec<VoidArenaGenerator> CODEC=FlatLevelGeneratorSettings.CODEC.fieldOf("settings").xmap(VoidArenaGenerator::new,VoidArenaGenerator::settings).codec();
    public VoidArenaGenerator(FlatLevelGeneratorSettings s){super(s);}
    @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState random,StructureManager structures,ChunkAccess chunk){
        return super.fillFromNoise(executor,blender,random,structures,chunk).thenApply(c->{
            BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
            for(int x=c.getPos().getMinBlockX();x<=c.getPos().getMaxBlockX();x++)for(int z=c.getPos().getMinBlockZ();z<=c.getPos().getMaxBlockZ();z++){
                double r=Math.sqrt(x*x+z*z);if(r>50)continue;
                c.setBlockState(pos.set(x,-61,z),Blocks.BEDROCK.defaultBlockState(),false);
                c.setBlockState(pos.set(x,-60,z),(r>47?Blocks.CRYING_OBSIDIAN:Math.floorMod(x*3+z*7,29)==0?Blocks.CRYING_OBSIDIAN:Math.floorMod(x-z,17)==0?Blocks.PURPLE_CONCRETE:Blocks.BLACK_CONCRETE).defaultBlockState(),false);
                if(r>48)for(int y=-59;y<=-38;y++)c.setBlockState(pos.set(x,y,z),(y%6==0?Blocks.CRYING_OBSIDIAN:Blocks.OBSIDIAN).defaultBlockState(),false);
                if(r>33&&r<37&&Math.floorMod(x,16)==0&&Math.floorMod(z,16)==0)for(int y=-59;y<-52;y++)c.setBlockState(pos.set(x,y,z),Blocks.PURPLE_CONCRETE.defaultBlockState(),false);
            }return c;
        });
    }
    @Override public void buildSurface(net.minecraft.server.level.WorldGenRegion r,StructureManager s,RandomState n,ChunkAccess c){}
    @Override public void applyBiomeDecoration(net.minecraft.world.level.WorldGenLevel l,ChunkAccess c,StructureManager s){}
}
