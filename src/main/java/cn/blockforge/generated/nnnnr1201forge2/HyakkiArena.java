package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 旧存档仍可能保存旧平地生成器：在远离旧场地的空中分批搭建教堂，不覆盖旧建筑。 */
public final class HyakkiArena {
    private static final int LEGACY_X=1600;
    private static final Map<ServerLevel,Build> BUILDING=new HashMap<>();
    private static final class Build {int cursor;boolean checked;final Set<UUID> waiting=new HashSet<>();}
    public static void enter(ServerLevel level,ServerPlayer player){
        if(level.getChunkSource().getGenerator() instanceof HyakkiCathedralGenerator){arrive(level,player,0);return;}
        if(level.getBlockState(new BlockPos(LEGACY_X,63,0)).is(Blocks.LODESTONE)){arrive(level,player,LEGACY_X);return;}
        var task=BUILDING.computeIfAbsent(level,l->new Build());task.waiting.add(player.getUUID());
        player.sendSystemMessage(Component.literal("正在为旧存档分批准备丧钟教堂，完成后会自动传送；不会覆盖原来的百鬼夜行场地。"));
    }
    private static void arrive(ServerLevel level,ServerPlayer player,int x){
        level.getChunk(new BlockPos(x,65,40));
        player.teleportTo(level,x+.5,65,40.5,180,0);player.fallDistance=0;
        player.sendSystemMessage(Component.literal("百鬼夜行 · 丧钟教堂：击败50只咒灵，共5波"));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(var entry:List.copyOf(BUILDING.entrySet())){
            ServerLevel level=entry.getKey();Build b=entry.getValue();int total=129*129*47;
            if(!b.checked){
                boolean obstructed=false;
                for(int n=0;n<16384&&b.cursor<total;n++,b.cursor++){
                    int x=b.cursor%129-64,z=(b.cursor/129)%129-64,y=b.cursor/(129*129)-2;
                    BlockPos pos=new BlockPos(LEGACY_X+x,64+y,z);
                    var state=level.getBlockState(pos);var expected=HyakkiCathedralGenerator.block(x,y,z);
                    // 允许继续上次中断的搭建，但拒绝覆盖其他建筑。
                    if(!state.isAir()&&!state.is(expected)){obstructed=true;break;}
                }
                if(obstructed){for(UUID id:b.waiting){var p=event.getServer().getPlayerList().getPlayer(id);if(p!=null)p.sendSystemMessage(Component.literal("旧存档教堂预留区域存在其他建筑，已停止搭建以免覆盖。请在新测试存档体验教堂维度。"));}BUILDING.remove(level);continue;}
                if(b.cursor>=total){b.checked=true;b.cursor=0;}continue;
            }
            for(int n=0;n<8192&&b.cursor<total;n++,b.cursor++){
                int x=b.cursor%129-64,z=(b.cursor/129)%129-64,y=b.cursor/(129*129)-2;
                var block=HyakkiCathedralGenerator.block(x,y,z);
                if(block!=Blocks.AIR)level.setBlock(new BlockPos(LEGACY_X+x,64+y,z),block.defaultBlockState(),2);
            }
            if(b.cursor>=total){
                level.setBlock(new BlockPos(LEGACY_X,63,0),Blocks.LODESTONE.defaultBlockState(),2);
                for(UUID id:b.waiting){ServerPlayer p=event.getServer().getPlayerList().getPlayer(id);if(p!=null&&p.isAlive())arrive(level,p,LEGACY_X);}
                BUILDING.remove(level);
            }
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){BUILDING.clear();}
    public static void ambience(ServerLevel level){
        if(level.getGameTime()%200!=0)return;
        for(var p:level.players())level.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.BELL_BLOCK,net.minecraft.sounds.SoundSource.AMBIENT,.65f,.5f);
    }
    private HyakkiArena(){}
}
