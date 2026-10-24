package character.jogo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** 盖棺铁围山的部分地形变化：只在少数位置裂开、露出岩浆与岩浆石，并记录原方块以便还原。 */
public final class JogoTerrain {
    private static final int CRACKS=12, MAX_BLOCKS=140;
    private static final Map<UUID, LinkedHashMap<BlockPos,BlockState>> JOURNAL=new HashMap<>();

    /** 在领域内随机挑几处地面，沿短折线换成岩浆/岩浆石/黑曜石，原方块全部记进日志。 */
    public static void open(ServerPlayer owner,Vec3 center,double radius) {
        restore(owner);
        ServerLevel level=owner.serverLevel();
        var random=level.getRandom();
        var journal=new LinkedHashMap<BlockPos,BlockState>();
        for(int i=0;i<CRACKS&&journal.size()<MAX_BLOCKS;i++){
            double angle=random.nextDouble()*Math.PI*2;
            double dist=Math.sqrt(random.nextDouble())*radius*0.85;
            int x=(int)Math.floor(center.x+Math.cos(angle)*dist);
            int z=(int)Math.floor(center.z+Math.sin(angle)*dist);
            int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);
            double dir=random.nextDouble()*Math.PI*2;
            int length=3+random.nextInt(4);
            for(int step=0;step<length&&journal.size()<MAX_BLOCKS;step++){
                int bx=(int)Math.floor(x+Math.cos(dir)*step);
                int bz=(int)Math.floor(z+Math.sin(dir)*step);
                for(int dy=-1;dy<=0;dy++){
                    BlockPos pos=new BlockPos(bx,y+dy,bz);
                    BlockState old=level.getBlockState(pos);
                    if(old.isAir()||old.hasBlockEntity()||JOURNAL.containsKey(pos)||journal.containsKey(pos))continue;
                    if(old.getDestroySpeed(level,pos)<0)continue;   // 基岩等不可破坏方块不动
                    BlockState next=dy==0
                        ? (random.nextInt(4)==0?Blocks.LAVA.defaultBlockState():Blocks.MAGMA_BLOCK.defaultBlockState())
                        : (random.nextBoolean()?Blocks.OBSIDIAN.defaultBlockState():Blocks.MAGMA_BLOCK.defaultBlockState());
                    if(level.setBlock(pos,next,2))journal.put(pos,old);
                }
            }
        }
        if(!journal.isEmpty())JOURNAL.put(owner.getUUID(),journal);
    }

    /** 还原所有改过的方块；领域结束、玩家死亡或切换角色时都要调用。 */
    public static void restore(ServerPlayer owner) {
        var journal=JOURNAL.remove(owner.getUUID());
        if(journal==null||journal.isEmpty())return;
        ServerLevel level=owner.serverLevel();
        for(var entry:journal.entrySet())level.setBlock(entry.getKey(),entry.getValue(),2);
    }
    public static void restoreAll(ServerLevel level) {
        for(var owner:List.copyOf(JOURNAL.keySet()))JOURNAL.remove(owner);
    }
    public static boolean changed(ServerPlayer owner) { return JOURNAL.containsKey(owner.getUUID()); }
    private JogoTerrain() {}
}
