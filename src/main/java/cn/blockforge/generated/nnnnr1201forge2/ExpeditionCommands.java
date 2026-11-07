package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 管理员测试入口：只建在面前的空地，不覆盖玩家已有方块。 */
public final class ExpeditionCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("sukuna_expedition")
            .then(Commands.literal("guide").executes(c->{c.getSource().sendSuccess(()->Component.literal("自然结构：/locate structure jujutsu_kaisen_cursed_paths:juvenile_center；群系：/locate biome jujutsu_kaisen_cursed_paths:cursed_wasteland；祭坛潜行右键切配方，普通右键投入背包材料。管理员测试：/sukuna_expedition altar 0 或 1 或 2"),false);return 1;}))
            .then(Commands.literal("hyakki").requires(c->c.hasPermission(2)).executes(c->{character.yuta.YutaProgress.teleportHyakki(c.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("points").executes(c->{var p=c.getSource().getPlayerOrException();c.getSource().sendSuccess(()->Component.literal("死灭回游积分："+ExpeditionProgress.data(p).getInt("CullingPoints")),false);return 1;}))
            .then(Commands.literal("altar").requires(c->c.hasPermission(2)).then(Commands.argument("tier",IntegerArgumentType.integer(0,2)).executes(c->build(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"tier"))))));
    }
    private static int build(ServerPlayer p,int tier){
        BlockPos center=p.blockPosition().relative(p.getDirection(),8).above(3);
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-4,-2,-4),center.offset(4,5,4))){if(p.level().isOutsideBuildHeight(pos)||!p.level().getWorldBorder().isWithinBounds(pos)||!p.level().hasChunkAt(pos)||!p.level().isEmptyBlock(pos)){p.sendSystemMessage(Component.literal("测试祭坛需要面前8格处、上方3格为中心的9×8×9空地；不会覆盖现有建筑。"));return 0;}}
        if(tier==0){
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)put(p,center.offset(x,-1,z),Blocks.OBSIDIAN);
            for(int[] d:RitualBlockEntity.FOUR){put(p,center.offset(d[0],-1,d[1]),Blocks.STONE_BRICKS);put(p,center.offset(d[0],0,d[1]),ExpeditionContent.block("simple_seal_pillar"));}
            for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})put(p,center.offset(x,0,z),Blocks.AMETHYST_BLOCK);
        }else if(tier==1){
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                if(Math.abs(x)<=1&&Math.abs(z)<=1){put(p,center.offset(x,-1,z),ExpeditionContent.block("alloy_block"));put(p,center.offset(x,0,z),Blocks.CAULDRON);}
                else put(p,center.offset(x,0,z),ExpeditionContent.block(Math.abs(x)==2&&Math.abs(z)==2?"curse_crystal_block":"alloy_bars"));
            }put(p,center.below(),ProgressionContent.TRANSFIGURED_FLESH.get());
        }else{
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)put(p,center.offset(x,-1,z),ExpeditionContent.block("alloy_block"));
            put(p,center.below(2),ExpeditionContent.block("spirit_stone"));
            for(int[] d:RitualBlockEntity.EIGHT){for(int y=0;y<3;y++)put(p,center.offset(d[0],y,d[1]),ExpeditionContent.block("special_seal_pillar"));p.level().setBlock(center.offset(d[0],2,d[1]),ExpeditionContent.block("special_seal_pillar").defaultBlockState().setValue(SealPillarBlock.INLAID,true),3);}
        }
        put(p,center,ExpeditionContent.block(tier==0?"refining_altar":tier==1?"gu_vat":"tengen_core"));
        p.sendSystemMessage(Component.literal("祭坛样板已建于 "+center.toShortString()+"。群系、时间、材料要求仍然有效。"));return 1;
    }
    private static void put(ServerPlayer p,BlockPos at,Block b){p.level().setBlock(at,b.defaultBlockState(),3);}
    private ExpeditionCommands(){}
}
