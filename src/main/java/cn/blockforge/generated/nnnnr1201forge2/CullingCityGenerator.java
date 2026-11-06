package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.serialization.Codec;
import java.util.concurrent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/** 512×512都市按16×16区块生成，无整城主线程铺方块；边界为512格黑色半透明立方体。 */
public final class CullingCityGenerator extends FlatLevelSource {
    public static final Codec<CullingCityGenerator> CODEC=FlatLevelGeneratorSettings.CODEC.fieldOf("settings").xmap(CullingCityGenerator::new,CullingCityGenerator::settings).codec();
    public CullingCityGenerator(FlatLevelGeneratorSettings s){super(s);}
    @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public int getGenDepth(){return 512;}
    @Override public int getSpawnHeight(net.minecraft.world.level.LevelHeightAccessor l){return 65;}
    @Override public int getBaseHeight(int x,int z,Heightmap.Types t,net.minecraft.world.level.LevelHeightAccessor l,RandomState r){return 65;}
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState random,StructureManager manager,ChunkAccess c){return super.fillFromNoise(executor,blender,random,manager,c).thenApply(chunk->{generate(chunk);return chunk;});}
    private static void generate(ChunkAccess c){
        for(int x=c.getPos().getMinBlockX();x<=c.getPos().getMaxBlockX();x++)for(int z=c.getPos().getMinBlockZ();z<=c.getPos().getMaxBlockZ();z++){
            if(x< -256||x>255||z< -256||z>255)continue;
            put(c,x,0,z,ExpeditionContent.block("culling_barrier"));
            // r259：地面下的沙砾层全部改成基岩，防止玩家往下挖穿结界
            for(int y=1;y<=63;y++)put(c,x,y,z,y<60?Blocks.STONE:Blocks.BEDROCK);
            int lx=Math.floorMod(x+256,32),lz=Math.floorMod(z+256,32),bx=Math.floorDiv(x+256,32),bz=Math.floorDiv(z+256,32);
            boolean road=lx<7||lz<7;put(c,x,64,z,road?(lx==3||lz==3?Blocks.YELLOW_CONCRETE:Blocks.GRAY_CONCRETE):Blocks.SMOOTH_STONE);
            if(x==-256||x==255||z==-256||z==255)for(int y=1;y<=511;y++)put(c,x,y,z,ExpeditionContent.block("culling_barrier"));
            put(c,x,511,z,ExpeditionContent.block("culling_barrier"));
            boolean plaza=Math.abs(x)<23&&Math.abs(z)<23;
            if(!road&&!plaza&&lx>=10&&lx<=28&&lz>=10&&lz<=28){int floors=2+Math.floorMod(bx*31+bz*17,5),top=65+floors*5;for(int y=65;y<=top;y++){
                boolean wall=lx==10||lx==28||lz==10||lz==28;boolean door=lz==10&&lx>=17&&lx<=20&&y<69;
                if(door)continue;
                if(wall){boolean broken=Math.floorMod(x*53+z*97+y*13,37)<3;if(!broken)put(c,x,y,z,y%5>1&&((lx+lz)%4<3)?Blocks.GLASS:Blocks.LIGHT_GRAY_CONCRETE);}
                else if((y-65)%5==0&&!(lx>=13&&lx<=14&&lz>=12&&lz<=13))put(c,x,y,z,Blocks.SMOOTH_STONE);
                if(lx==12&&lz==12&&y<top)put(c,x,y,z,Blocks.IRON_BLOCK);
                if(lx==13&&lz==12&&y<top){c.setBlockState(new BlockPos(x,y,z),Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,net.minecraft.core.Direction.EAST),false);}
            }
            if(lx==12&&lz==15){put(c,x,66,z,Blocks.CHEST);CompoundTag n=be("minecraft:chest",x,66,z);n.putString("LootTable","sukuna:chests/culling_city");n.putLong("LootTableSeed",x*341873128712L+z*132897987541L);c.setBlockEntityNbt(n);}
            }
            if(Math.abs(x)<=5&&Math.abs(z)<=5){for(int y=65;y<=104;y++)if(Math.abs(x)==5&&Math.abs(z)==5||y%8==0)put(c,x,y,z,Blocks.IRON_BLOCK);}
            if(x==0&&z==-8)put(c,x,65,z,ExpeditionContent.block("culling_exchange"));
            if(x==2&&z==-8)put(c,x,65,z,ExpeditionContent.block("culling_exit"));
            if(x==0&&z==0){put(c,x,105,z,Blocks.BEACON);}
        }
    }
    private static CompoundTag be(String id,int x,int y,int z){CompoundTag n=new CompoundTag();n.putString("id",id);n.putInt("x",x);n.putInt("y",y);n.putInt("z",z);return n;}
    private static void put(ChunkAccess c,int x,int y,int z,Block b){c.setBlockState(new BlockPos(x,y,z),b.defaultBlockState(),false);}
    @Override public void buildSurface(net.minecraft.server.level.WorldGenRegion r,StructureManager m,RandomState s,ChunkAccess c){}
}
