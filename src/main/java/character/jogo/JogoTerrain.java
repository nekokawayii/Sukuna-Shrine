package character.jogo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** 盖棺铁围山的地面变化：领域内随机几片 5–15 格的区域变成岩浆 / 岩浆块 / 原石，并逐块记录以便还原。 */
public final class JogoTerrain {
    private static final int PATCHES=5, MIN_RADIUS=5, MAX_RADIUS=15, MAX_BLOCKS=3000;
    private static final Map<UUID,LinkedHashMap<BlockPos,BlockState>> JOURNAL=new HashMap<>();

    public static void open(ServerPlayer owner,Vec3 center,double radius) {
        restore(owner);
        ServerLevel level=owner.serverLevel();
        var random=level.getRandom();
        var journal=new LinkedHashMap<BlockPos,BlockState>();
        for(int patch=0;patch<PATCHES&&journal.size()<MAX_BLOCKS;patch++){
            double angle=random.nextDouble()*Math.PI*2;
            double dist=Math.sqrt(random.nextDouble())*radius*0.80;
            int cx=(int)Math.floor(center.x+Math.cos(angle)*dist);
            int cz=(int)Math.floor(center.z+Math.sin(angle)*dist);
            double r=MIN_RADIUS+random.nextDouble()*(MAX_RADIUS-MIN_RADIUS);
            int ri=(int)Math.ceil(r);
            for(int dx=-ri;dx<=ri&&journal.size()<MAX_BLOCKS;dx++){
                for(int dz=-ri;dz<=ri&&journal.size()<MAX_BLOCKS;dz++){
                    if(dx*dx+dz*dz>r*r)continue;
                    int x=cx+dx,z=cz+dz;
                    int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
                    boolean core=dx*dx+dz*dz<=r*r*0.36;   // 中心 60% 半径更热
                    for(int dy=0;dy<=1&&journal.size()<MAX_BLOCKS;dy++){
                        BlockPos pos=new BlockPos(x,y-dy,z);
                        BlockState old=level.getBlockState(pos);
                        if(old.isAir()||old.hasBlockEntity()||journal.containsKey(pos))continue;
                        if(old.getDestroySpeed(level,pos)<0)continue;   // 基岩等不可破坏方块
                        double roll=random.nextDouble();
                        BlockState next;
                        if(dy==0)next=core?(roll<0.72?Blocks.LAVA.defaultBlockState():Blocks.MAGMA_BLOCK.defaultBlockState())
                                          :(roll<0.42?Blocks.MAGMA_BLOCK.defaultBlockState():roll<0.72?Blocks.LAVA.defaultBlockState():Blocks.COBBLESTONE.defaultBlockState());
                        else next=roll<0.55?Blocks.MAGMA_BLOCK.defaultBlockState():Blocks.COBBLESTONE.defaultBlockState();
                        if(level.setBlock(pos,next,2))journal.put(pos,old);
                    }
                }
            }
        }
        if(!journal.isEmpty())JOURNAL.put(owner.getUUID(),journal);
    }

    /** 还原所有改过的方块；领域结束、玩家死亡或切换角色时调用。 */
    public static void restore(ServerPlayer owner) {
        var journal=JOURNAL.remove(owner.getUUID());
        if(journal==null||journal.isEmpty())return;
        ServerLevel level=owner.serverLevel();
        for(var entry:journal.entrySet())level.setBlock(entry.getKey(),entry.getValue(),2);
    }
    public static boolean changed(ServerPlayer owner) { return JOURNAL.containsKey(owner.getUUID()); }
    private JogoTerrain() {}
}
