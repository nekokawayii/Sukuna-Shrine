package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.serialization.Codec;
import java.util.concurrent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/** 三层互不覆盖：低层灵魂通道、真人肉墙、露天岩手。 */
public final class SoulCorridorGenerator extends FlatLevelSource {
    public static final Codec<SoulCorridorGenerator> CODEC=FlatLevelGeneratorSettings.CODEC.fieldOf("settings").xmap(SoulCorridorGenerator::new,SoulCorridorGenerator::settings).codec();
    public static final int MAHITO_Y=1000,IWATE_Y=1300,IWATE_FLOOR=1288,SEPARATOR_FLOOR=1280;
    public SoulCorridorGenerator(FlatLevelGeneratorSettings settings){super(settings);}
    @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public int getGenDepth(){return 1536;}
    @Override public int getSpawnHeight(net.minecraft.world.level.LevelHeightAccessor level){return IWATE_Y+2;}
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,net.minecraft.world.level.LevelHeightAccessor level,RandomState random){return IwateTerrain.surface(x,z)+1;}
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState random,StructureManager structures,ChunkAccess chunk){
        return super.fillFromNoise(executor,blender,random,structures,chunk).thenApply(c->{generate(c,false);return c;});
    }
    public static void rebuild(LevelChunk chunk){generate(chunk,true);if(chunk.getLevel() instanceof net.minecraft.server.level.ServerLevel level)IwateStructures.place(level,chunk);chunk.setUnsaved(true);chunk.initializeLightSources();}
    @Override public CompletableFuture<ChunkAccess> createBiomes(Executor executor,RandomState random,Blender blender,StructureManager structures,ChunkAccess chunk){
        var registry=structures.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var corridor=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,new net.minecraft.resources.ResourceLocation("sukuna","soul_corridor")));
        var forest=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,new net.minecraft.resources.ResourceLocation("sukuna","iwate_forest")));
        var village=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,new net.minecraft.resources.ResourceLocation("sukuna","iwate_village")));
        chunk.fillBiomesFromNoise((x,y,z,sampler)->y*4<1288?corridor:IwateTerrain.forest(x*4,z*4)?forest:village,random.sampler());return CompletableFuture.completedFuture(chunk);
    }
    private static void generate(ChunkAccess c,boolean clear){
        BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
        for(int x=c.getPos().getMinBlockX();x<=c.getPos().getMaxBlockX();x++)for(int z=c.getPos().getMinBlockZ();z<=c.getPos().getMaxBlockZ();z++){
            if(clear){for(int y=1;y<=96;y++){p.set(x,y,z);c.setBlockState(p,Blocks.AIR.defaultBlockState(),false);c.removeBlockEntity(p);}for(int y=988;y<1400;y++){p.set(x,y,z);c.setBlockState(p,Blocks.AIR.defaultBlockState(),false);c.removeBlockEntity(p);}}
            put(c,p,x,0,z,Blocks.BEDROCK);
            int ax=Math.abs(x),az=Math.abs(z);
            if(ax<=34&&az<=50)for(int y=62;y<=86;y++){
                Block b=ax==34||az==50||y==62||y==86?Blocks.BEDROCK:ax==33||az==49||y==63||y==85?((x+z)%9==0?Blocks.PURPLE_CONCRETE:Blocks.BLACK_CONCRETE):Blocks.AIR;
                put(c,p,x,y,z,b);
                if(y==64&&ax<32&&az<49&&(ax==30||az==46))put(c,p,x,y,z,Blocks.SEA_LANTERN);
            }
            if(ax<=26&&az<=26)for(int y=998;y<=1020;y++){
                Block b=ax==26||az==26||y==998||y==1020?Blocks.BEDROCK:ax==25||az==25||y==999||y==1019?((x-z)%7==0?Blocks.PURPLE_CONCRETE:Blocks.BLACK_CONCRETE):Blocks.AIR;
                // 肉墙是可挖通的真正墙体，不再埋在基岩夹层内部。
                if(z==12&&ax<25&&y>=1000&&y<1019)b=ProgressionContent.TRANSFIGURED_FLESH.get();
                if(y==1000&&ax==23&&az<24)b=Blocks.SEA_LANTERN;
                put(c,p,x,y,z,b);
            }
            put(c,p,x,SEPARATOR_FLOOR,z,Blocks.BEDROCK);
            int h=IwateVillage.height(x,z);
            for(int y=IWATE_FLOOR;y<=h;y++)put(c,p,x,y,z,y==h?Blocks.GRASS_BLOCK:y>=h-3?Blocks.DIRT:Blocks.STONE);
        }
        IwateVillage.generate(c);
        portal(c,0,64,-43,0,"返回现世");portal(c,0,64,43,1,"前往真人区域");
        portal(c,-18,1000,-18,2,"返回灵魂通道");portal(c,0,1000,20,3,"穿过肉墙 · 前往岩手");
        portal(c,0,1301,-8,4,"返回真人区域");
    }
    private static void put(ChunkAccess c,BlockPos.MutableBlockPos p,int x,int y,int z,Block b){c.setBlockState(p.set(x,y,z),b.defaultBlockState(),false);}
    private static void portal(ChunkAccess c,int x,int y,int z,int route,String text){
        IwateVillage.box(c,x-2,y-1,z-2,x+2,y-1,z+2,Blocks.POLISHED_BLACKSTONE);
        IwateVillage.set(c,x,y,z,route==0?SoulContent.GATE.get():SoulContent.PASSAGE.get());
        IwateVillage.sign(c,x+2,y,z,text,"右键通行","","",0);
    }
    @Override public void buildSurface(net.minecraft.server.level.WorldGenRegion region,StructureManager structures,RandomState random,ChunkAccess chunk){}
    @Override public void applyBiomeDecoration(net.minecraft.world.level.WorldGenLevel level,ChunkAccess chunk,StructureManager structures){IwateStructures.place(level,chunk);}
}
