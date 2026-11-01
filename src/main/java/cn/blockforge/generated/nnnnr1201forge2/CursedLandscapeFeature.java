package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** 只在本区块落笔，避免装饰时跨区块加载；不同群系有可采集的地表材料。 */
public final class CursedLandscapeFeature extends Feature<NoneFeatureConfiguration> {
    public CursedLandscapeFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        WorldGenLevel l=c.level();int ox=(c.origin().getX()>>4)<<4,oz=(c.origin().getZ()>>4)<<4;var r=c.random();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            BlockPos p=l.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG,new BlockPos(ox+x,0,oz+z)).below();
            var ground=l.getBlockState(p);
            if(!l.getFluidState(p).isEmpty()||p.getY()<60||!(ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.DIRT)||ground.is(Blocks.STONE)||ground.is(Blocks.GRAVEL)||ground.is(Blocks.PODZOL)||ground.is(Blocks.COARSE_DIRT)))continue;
            String b=ExpeditionWorldgen.biome(l,p);
            if(!ExpeditionWorldgen.BIOMES.contains(b))continue;
            Block top=switch(b){case "cursed_wasteland"->ExpeditionContent.block(r.nextInt(65)==0?"cursed_fissure":"cursed_soil");case "cursed_sakura_forest"->Blocks.PODZOL;case "scorched_volcanic_fields"->ExpeditionContent.block(r.nextInt(8)==0?"lava_obsidian":r.nextInt(7)==0?"volcanic_ash":"volcanic_rock");default->ExpeditionContent.block("blood_soil");};
            l.setBlock(p,top.defaultBlockState(),2);
            if(b.equals("scorched_volcanic_fields")&&x>1&&x<14&&z>1&&z<14&&r.nextInt(100)==0){l.setBlock(p,Blocks.LAVA.defaultBlockState(),2);for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(dx!=0||dz!=0){BlockPos rim=p.offset(dx,0,dz);BlockState existing=l.getBlockState(rim);if(existing.is(Blocks.STONE)||existing.is(Blocks.DIRT)||existing.is(Blocks.GRASS_BLOCK)||existing.is(Blocks.GRAVEL)||existing.is(ExpeditionContent.block("volcanic_rock"))||existing.is(ExpeditionContent.block("volcanic_ash")))l.setBlock(rim,ExpeditionContent.block("lava_obsidian").defaultBlockState(),2);}}
            if(x<4||x>11||z<4||z>11)continue;
            if(b.equals("cursed_wasteland")&&r.nextInt(75)==0){for(int y=1;y<6;y++)put(l,p.offset(0,y,0),Blocks.DARK_OAK_LOG);for(int d=-2;d<=2;d++){put(l,p.offset(d,5,0),Blocks.DARK_OAK_LOG);if(Math.abs(d)==2)put(l,p.offset(d,4,0),Blocks.DARK_OAK_LOG);}}
            if(b.equals("cursed_sakura_forest")&&r.nextInt(48)==0){for(int y=1;y<=5;y++)put(l,p.above(y),Blocks.CHERRY_LOG);for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(dx*dx+dz*dz<12)for(int y=4;y<7;y++){BlockPos leaf=p.offset(dx,y,dz);if(l.isEmptyBlock(leaf))l.setBlock(leaf,ExpeditionContent.block("blood_sakura_leaves").defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),2);}for(int y=1;y<=3;y++)put(l,p.offset(2,y,0),ExpeditionContent.block("black_vines"));}
            if(b.equals("ancient_battlefield")&&r.nextInt(80)==0){for(int d=-3;d<=3;d++){put(l,p.offset(d,1,0),Blocks.BONE_BLOCK);if(d%2==0){put(l,p.offset(d,2,1),Blocks.BONE_BLOCK);put(l,p.offset(d,3,2),Blocks.BONE_BLOCK);put(l,p.offset(d,2,-1),Blocks.BONE_BLOCK);}}}
        }
        return true;
    }
    private void put(WorldGenLevel l,BlockPos p,Block b){if(l.isEmptyBlock(p))l.setBlock(p,b.defaultBlockState(),2);}
}
