package character.jogo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** 盖棺铁围山的地面变化：领域内**所有**地面列换成岩浆 / 岩浆块 / 原石，逐块记录原状态以便还原。 */
public final class JogoTerrain {
    private static final int MAX_BLOCKS=90000;   // 半径 100 的两层地面约 6.3 万块
    private static final int PER_TICK=8000;      // 分批施工，避免单刻卡死
    private record Change(BlockPos pos,BlockState state) {}
    private static final Map<UUID,LinkedHashMap<BlockPos,BlockState>> JOURNAL=new HashMap<>();
    private static final Map<UUID,ArrayDeque<Change>> QUEUE=new HashMap<>();

    public static void open(ServerPlayer owner,Vec3 center,double radius) {
        restore(owner);
        ServerLevel level=owner.serverLevel();
        var random=level.getRandom();
        var queue=new ArrayDeque<Change>();
        int ri=(int)Math.ceil(radius);
        int cxi=(int)Math.floor(center.x),czi=(int)Math.floor(center.z);
        for(int dx=-ri;dx<=ri&&queue.size()<MAX_BLOCKS;dx++){
            for(int dz=-ri;dz<=ri&&queue.size()<MAX_BLOCKS;dz++){
                double d2=dx*dx+dz*dz;
                if(d2>radius*radius)continue;
                int x=cxi+dx,z=czi+dz;
                int surface=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
                // 先把这列球体内的方块清成空气（A 方案：舞台内原地形临时替换）
                for(int yy=surface+1;yy<=(int)center.y+(int)radius&&queue.size()<MAX_BLOCKS;yy++){
                    var p2=new BlockPos(x,yy,z);
                    if(p2.distSqr(new BlockPos((int)center.x,(int)center.y,(int)center.z))>radius*radius)continue;
                    BlockState st=level.getBlockState(p2);
                    if(st.isAir()||st.getDestroySpeed(level,p2)<0)continue;
                    queue.add(new Change(p2,Blocks.AIR.defaultBlockState()));
                }
                double core=1.0-Math.sqrt(d2)/radius;
                double roll=random.nextDouble();
                BlockState top=roll<0.18+core*0.62?Blocks.LAVA.defaultBlockState()
                              :roll<0.55+core*0.25?Blocks.MAGMA_BLOCK.defaultBlockState()
                              :Blocks.COBBLESTONE.defaultBlockState();
                queue.add(new Change(new BlockPos(x,surface,z),top));
                queue.add(new Change(new BlockPos(x,surface-1,z),
                        roll<0.62?Blocks.MAGMA_BLOCK.defaultBlockState():Blocks.COBBLESTONE.defaultBlockState()));
            }
        }
        // ---- A 方案：真实方块外壳（1 格厚球壳），彻底挡住天空与山体 ----
        int latSteps=Math.max(24,(int)(Math.PI*radius));
        for(int i=0;i<=latSteps&&queue.size()<MAX_BLOCKS;i++){
            double theta=Math.PI*i/latSteps;
            int lonSteps=Math.max(12,(int)(2*Math.PI*radius*Math.sin(theta)));
            for(int j=0;j<lonSteps&&queue.size()<MAX_BLOCKS;j++){
                double phi=2*Math.PI*j/Math.max(1,lonSteps);
                int x=(int)Math.round(center.x+radius*Math.sin(theta)*Math.cos(phi));
                int y=(int)Math.round(center.y+radius*Math.cos(theta));
                int z=(int)Math.round(center.z+radius*Math.sin(theta)*Math.sin(phi));
                if(y<level.getMinBuildHeight()+1||y>=level.getMaxBuildHeight()-1)continue;
                // 主题：黑石为主，每 7 个点夹一格岩浆块做暗红发热纹
                BlockState wall=((queue.size()%7)==0)?Blocks.MAGMA_BLOCK.defaultBlockState()
                    :((queue.size()%3)==0)?Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                    :Blocks.DEEPSLATE_TILES.defaultBlockState();
                queue.add(new Change(new BlockPos(x,y,z),wall));
            }
        }
        QUEUE.put(owner.getUUID(),queue);
        owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "盖棺铁围山：地面化为岩浆海（"+queue.size()+" 个方块），分批施工，收起/结束时全部还原"));
    }

    /** 每刻推进施工；由 JogoCombat 的服务器 tick 调用。 */
    public static void tick() {
        if(QUEUE.isEmpty())return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null)return;
        for(var it=QUEUE.entrySet().iterator();it.hasNext();){
            var entry=it.next();
            var owner=server.getPlayerList().getPlayer(entry.getKey());
            if(owner==null){it.remove();continue;}
            ServerLevel level=owner.serverLevel();
            var journal=JOURNAL.computeIfAbsent(entry.getKey(),k->new LinkedHashMap<>());
            var queue=entry.getValue();
            int done=0;
            while(done<PER_TICK&&!queue.isEmpty()){
                Change change=queue.poll();
                BlockPos pos=change.pos();
                if(pos.getY()<level.getMinBuildHeight()+1)continue;
                BlockState old=level.getBlockState(pos);
                if(old.hasBlockEntity()||old.getDestroySpeed(level,pos)<0)continue;   // 只跳过容器与基岩类
                if(old==change.state())continue;
                if(level.setBlock(pos,change.state(),2))journal.put(pos,old);
                done++;
            }
            if(queue.isEmpty()){
                it.remove();
                owner.sendSystemMessage(net.minecraft.network.chat.Component.literal("盖棺铁围山：地面改造完成（"+journal.size()+" 块）"));
            }
        }
    }

    /** 还原所有改过的方块；收起、结束、死亡、关服都调用。 */
    public static void restore(ServerPlayer owner) {
        QUEUE.remove(owner.getUUID());
        var journal=JOURNAL.remove(owner.getUUID());
        if(journal==null||journal.isEmpty())return;
        ServerLevel level=owner.serverLevel();
        for(var entry:journal.entrySet())level.setBlock(entry.getKey(),entry.getValue(),2);
        owner.sendSystemMessage(net.minecraft.network.chat.Component.literal("盖棺铁围山：地面已还原（"+journal.size()+" 块）"));
    }
    public static boolean changed(ServerPlayer owner) { return JOURNAL.containsKey(owner.getUUID()); }
    private JogoTerrain() {}
}
