package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 仅修复附近已载入区块中的天然水系，不重建建筑、不重新召唤首领。 */
public final class IwateWaterRepair {
    private static boolean natural(BlockState s){return s.isAir()||s.is(Blocks.STONE)||s.is(Blocks.DIRT)||s.is(Blocks.GRASS_BLOCK)||s.is(Blocks.GRAVEL)||s.is(Blocks.CLAY)||s.is(Blocks.WATER)||s.is(Blocks.DIRT_PATH)||s.is(Blocks.GRASS)||s.is(Blocks.TALL_GRASS)||s.is(Blocks.FERN)||s.is(SoulContent.RICE.get())||s.is(Blocks.MOSSY_COBBLESTONE);}
    private static int repair(ServerPlayer player){
        var level=player.serverLevel();if(level.dimension()!=SoulJourney.CORRIDOR){player.sendSystemMessage(Component.literal("请到岩手地区后使用这条修复指令。"));return 0;}
        int count=0,skipped=0;var origin=player.chunkPosition();
        for(int cx=origin.x-6;cx<=origin.x+6;cx++)for(int cz=origin.z-6;cz<=origin.z+6;cz++){
            var chunk=level.getChunkSource().getChunkNow(cx,cz);if(chunk==null)continue;
            for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){
                if(!IwateTerrain.wetland(x,z))continue;
                int h=IwateTerrain.height(x,z),top=1330;boolean safe=true;
                for(int y=h-1;y<=top;y++)if(!natural(chunk.getBlockState(new BlockPos(x,y,z)))){safe=false;break;}
                if(!safe){skipped++;continue;}
                for(int y=h+1;y<=top;y++){BlockPos pos=new BlockPos(x,y,z);if(!level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
                for(int y=SoulCorridorGenerator.IWATE_FLOOR;y<h;y++)level.setBlock(new BlockPos(x,y,z),(y==h-1?Blocks.CLAY:Blocks.STONE).defaultBlockState(),2);
                if(IwateTerrain.river(x,z)){
                    level.setBlock(new BlockPos(x,h,z),Blocks.GRAVEL.defaultBlockState(),2);
                    for(int y=h+1;y<=IwateTerrain.RIVER_LEVEL;y++)level.setBlock(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState(),2);
                }else{
                    boolean bank=IwateTerrain.terraceBank(x,z);level.setBlock(new BlockPos(x,h,z),(bank?Blocks.MOSSY_COBBLESTONE:Blocks.WATER).defaultBlockState(),2);
                    if(!bank&&Math.floorMod(x+z,3)!=0)level.setBlock(new BlockPos(x,h+1,z),SoulContent.RICE.get().defaultBlockState(),2);
                }
                count++;
            }
        }
        player.sendSystemMessage(Component.literal("岩手水系修复完成：恢复 "+count+" 列天然河流/梯田；跳过 "+skipped+" 列建筑或非天然方块。没有重建房屋或首领。"));return count;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("sukuna").then(Commands.literal("iwate").then(Commands.literal("repair_water").requires(s->s.hasPermission(2)).executes(c->repair(c.getSource().getPlayerOrException())))));}
    private IwateWaterRepair(){}
}
