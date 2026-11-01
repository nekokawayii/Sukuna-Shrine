package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.serialization.Codec;
import java.util.concurrent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/** 按区块生成的丧钟教堂群：尖顶、双钟楼、玫瑰彩窗、墓园及魂灯，全部使用原版方块。 */
public final class HyakkiCathedralGenerator extends FlatLevelSource {
    public static final int FLOOR=64,CELL=160;
    public static final Codec<HyakkiCathedralGenerator> CODEC=FlatLevelGeneratorSettings.CODEC.fieldOf("settings").xmap(HyakkiCathedralGenerator::new,HyakkiCathedralGenerator::settings).codec();
    public HyakkiCathedralGenerator(FlatLevelGeneratorSettings settings){super(settings);}
    @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public int getSpawnHeight(net.minecraft.world.level.LevelHeightAccessor level){return FLOOR+1;}
    @Override public int getGenDepth(){return 256;}
    @Override public int getMinY(){return -64;}
    @Override public int getSeaLevel(){return FLOOR-1;}
    @Override public net.minecraft.world.level.NoiseColumn getBaseColumn(int x,int z,net.minecraft.world.level.LevelHeightAccessor level,RandomState random){
        int min=level.getMinBuildHeight();var states=new net.minecraft.world.level.block.state.BlockState[level.getHeight()];
        for(int i=0;i<states.length;i++)states[i]=(i==0?Blocks.BEDROCK:block(x,min+i-FLOOR,z)).defaultBlockState();
        return new net.minecraft.world.level.NoiseColumn(min,states);
    }
    public static int local(int coordinate){return Math.floorMod(coordinate+CELL/2,CELL)-CELL/2;}
    public static Block block(int x,int y,int z){
        x=local(x);z=local(z);int ax=Math.abs(x),az=Math.abs(z);
        if(y<0)return y==-1?Blocks.DEEPSLATE_BRICKS:Blocks.DEEPSLATE;
        if(y==0){if(ax<=20&&z>=-36&&z<=30)return ax<=3?Blocks.POLISHED_BLACKSTONE:((x+z)&1)==0?Blocks.DEEPSLATE_TILES:Blocks.POLISHED_DEEPSLATE;return ax<=4||az<=4?Blocks.CRACKED_STONE_BRICKS:Math.floorMod(x*31+z*13,7)==0?Blocks.COARSE_DIRT:Blocks.SOUL_SOIL;}
        // 主殿：净空至少16格，容纳6格高的特级咒灵。
        if(ax<=18&&z>=-34&&z<=28){
            int roof=31-ax/2;
            if(y==roof||y==roof+1)return Blocks.DEEPSLATE_TILES;
            if((ax==18||z==-34||z==28)&&y<=roof){
                if(z==28&&ax<=4&&y<=10-ax/2)return Blocks.AIR;
                if(z==-34&&ax<=4&&y<=8)return Blocks.AIR;
                if(ax==18&&Math.floorMod(z+30,12)>=4&&Math.floorMod(z+30,12)<=8&&y>=7&&y<=17-Math.abs(Math.floorMod(z+30,12)-6))return (y+z)%3==0?Blocks.RED_STAINED_GLASS:Blocks.PURPLE_STAINED_GLASS;
                if(z==28&&x*x+(y-19)*(y-19)<=20)return x==0||y==19?Blocks.GILDED_BLACKSTONE:Blocks.RED_STAINED_GLASS;
                return y%6==0?Blocks.CHISELED_DEEPSLATE:Blocks.DEEPSLATE_BRICKS;
            }
            if(ax==13&&Math.floorMod(z+28,12)==0&&y<=20)return y==20?Blocks.CHISELED_POLISHED_BLACKSTONE:Blocks.POLISHED_BASALT;
            if(Math.floorMod(z+28,12)==0&&y==22-(13-ax)/2&&ax>=5&&ax<=13)return Blocks.POLISHED_BLACKSTONE_BRICKS;
            // 两侧长椅，中间保留7格宽通道。
            if(ax>=6&&ax<=10&&z>=-16&&z<=17&&Math.floorMod(z+16,7)==0&&y<=2)return Blocks.DARK_OAK_PLANKS;
            if(ax<=5&&z>=-29&&z<=-24&&y<=2)return Blocks.POLISHED_BLACKSTONE;
            if(ax<=3&&z==-27&&y==3)return Blocks.SOUL_LANTERN;
            if(ax==12&&Math.floorMod(z+28,12)==0&&y==7)return Blocks.SOUL_LANTERN;
            if(x==0&&Math.floorMod(z+24,16)==0&&y>=23&&y<roof)return Blocks.CHAIN;
            if(Math.floorMod(z+24,16)==0&&y==22&&ax<=2)return ax==2?Blocks.SOUL_LANTERN:Blocks.IRON_BARS;
        }
        // 双钟塔及可看见的青铜色丧钟。
        for(int side:new int[]{-24,24}){
            int dx=Math.abs(x-side),dz=Math.abs(z-25);
            if(dx<=5&&dz<=5){
                if(y<=30&&(dx==5||dz==5)){
                    if(y>=23&&dx<=2||y>=23&&dz<=2)return Blocks.AIR;
                    return y%7==0?Blocks.CHISELED_DEEPSLATE:Blocks.DEEPSLATE_BRICKS;
                }
                if(dx==0&&dz==0&&y==22)return Blocks.BELL;
                if(y==22||y==31)return Blocks.POLISHED_BLACKSTONE;
                if(y>=32&&y<=42&&Math.max(dx,dz)==5-(y-32)/2)return Blocks.DEEPSLATE_TILES;
                if(dx==0&&dz==0&&y>=27&&y<=30)return Blocks.CHAIN;
                if(dx<=1&&dz<=1&&y>=24&&y<=26)return Blocks.WAXED_WEATHERED_COPPER;
                if(dx<=2&&dz<=2&&y==23)return Blocks.WAXED_WEATHERED_COPPER;
                if(dx==0&&dz==0&&y==22)return Blocks.BELL;
            }
        }
        // 主殿外侧扶壁。
        if(ax>=19&&ax<=23&&z>=-34&&z<=20&&Math.floorMod(z+34,12)==0&&y<=20-(ax-18)*3)return Blocks.POLISHED_BLACKSTONE_BRICKS;
        // 前庭围墙，入口留在中轴。
        if((ax==62&&az<=62||az==62&&ax<=62)&&!(ax<=5||az<=5)&&y<=3)return y==3?Blocks.IRON_BARS:Blocks.STONE_BRICKS;
        // 墓园不挡主通道，墓碑、白骨、魂灯交错。
        if(ax>=34&&ax<=54&&az<=48&&Math.floorMod(x,8)==0&&Math.floorMod(z,9)==0){
            if(y==1)return Blocks.STONE_BRICKS;if(y==2)return Blocks.CHISELED_STONE_BRICKS;if(y==3)return Blocks.STONE_BRICK_WALL;
        }
        if(ax==6&&Math.floorMod(z,12)==0&&z>=34&&y<=4)return y==4?Blocks.SOUL_LANTERN:Blocks.COBBLESTONE_WALL;
        return Blocks.AIR;
    }
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,net.minecraft.world.level.LevelHeightAccessor level,RandomState state){
        for(int y=44;y>=0;y--)if(type.isOpaque().test(block(x,y,z).defaultBlockState()))return FLOOR+y+1;return FLOOR+1;
    }
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState random,StructureManager manager,ChunkAccess input){
        return super.fillFromNoise(executor,blender,random,manager,input).thenApply(chunk->{
            for(int x=chunk.getPos().getMinBlockX();x<=chunk.getPos().getMaxBlockX();x++)for(int z=chunk.getPos().getMinBlockZ();z<=chunk.getPos().getMaxBlockZ();z++){
                for(int y=chunk.getMinBuildHeight();y<=FLOOR+44;y++){
                    Block b=y==chunk.getMinBuildHeight()?Blocks.BEDROCK:block(x,y-FLOOR,z);
                    if(b!=Blocks.AIR)chunk.setBlockState(new BlockPos(x,y,z),b.defaultBlockState(),false);
                }
            }return chunk;
        });
    }
    @Override public void buildSurface(net.minecraft.server.level.WorldGenRegion region,StructureManager manager,RandomState state,ChunkAccess chunk){}
}
