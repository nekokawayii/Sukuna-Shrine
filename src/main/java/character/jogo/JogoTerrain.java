package character.jogo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/**
 * 盖棺铁围山的地形（教程 A 方案 / Cursed Fate 清场模型）：
 * 以施术瞬间的整数锚点为中心，按「地板 / 外部 / 地下填充 / 墙与穹顶 / 活动空间」五类分层施工：
 * · dy=-1 且 h²≤R² → 地板（深板岩砖 + 岩浆块纹）
 * · d²>R² → 不动
 * · dy≥0 且 (R-1)²<d²≤R² → 墙与穹顶
 * · dy≥0 且 d²≤(R-1)² → 清成空气（把山体/树/建筑临时替换掉）
 * dy<-1 的地下不动（只补地板）。
 * 每块改动前记录原状态，收起/超时/死亡/关服时按日志整批还原。
 */
public final class JogoTerrain {
    private static final int MAX_BLOCKS=140000;
    private static final int PER_TICK=512;
    private record Change(BlockPos pos,BlockState state) {}
    private static final Map<UUID,character.domain.DomainTerrain.Handle> TERRAIN=new HashMap<>();
    private static final Map<UUID,ArrayDeque<Change>> QUEUE=new HashMap<>();
    private static final Map<UUID,Integer> RATE=new HashMap<>();
    private static final Map<UUID,ServerLevel> LEVELS=new HashMap<>();

    public static void open(ServerPlayer owner,Vec3 center,double radius) {
        restore(owner);
        ServerLevel level=owner.serverLevel();
        LEVELS.put(owner.getUUID(),level);
        TERRAIN.put(owner.getUUID(),character.domain.DomainTerrain.begin(level,owner.getUUID()));
        int R=(int)Math.round(radius);
        int bx=(int)Math.floor(center.x),by=(int)Math.floor(center.y),bz=(int)Math.floor(center.z);
        var queue=new ArrayDeque<Change>();
        var random=level.getRandom();

        // ① 地板：dy=-1，二维圆盘
        for(int dx=-R;dx<=R;dx++){
            for(int dz=-R;dz<=R;dz++){
                int h2=dx*dx+dz*dz;
                if(h2>R*R)continue;
                BlockPos pos=new BlockPos(bx+dx,by-1,bz+dz);
                if(!inWorld(level,pos))continue;
                int roll=random.nextInt(100);
                BlockState floor=roll<62?Blocks.COBBLESTONE.defaultBlockState():Blocks.MAGMA_BLOCK.defaultBlockState();
                queue.add(new Change(pos,floor));
            }
        }
        // ② 球体上半部：外墙 + 内部清空
        int inner=R-1;
        for(int dy=0;dy<=R;dy++){
            int ring=R*R-dy*dy;if(ring<0)continue;
            int hmax=(int)Math.floor(Math.sqrt(ring));
            for(int dx=-hmax;dx<=hmax;dx++){
                int rz2=ring-dx*dx;if(rz2<0)continue;
                int zmax=(int)Math.floor(Math.sqrt(rz2));
                for(int dz=-zmax;dz<=zmax;dz++){
                    if(queue.size()>=MAX_BLOCKS)break;
                    int d2=dx*dx+dy*dy+dz*dz;
                    BlockPos pos=new BlockPos(bx+dx,by+dy,bz+dz);
                    if(!inWorld(level,pos))continue;
                    boolean wall=(inner*inner)<d2;
                    if(wall){
                        int roll=random.nextInt(9);
                        BlockState state=roll==0?Blocks.MAGMA_BLOCK.defaultBlockState()
                                :roll==1?Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                                :Blocks.DEEPSLATE_TILES.defaultBlockState();
                        queue.add(new Change(pos,state));
                    }else{
                        // 空气也要声明占位：否则对手的球壳进入本方半区时无法清除。
                        queue.add(new Change(pos,Blocks.AIR.defaultBlockState()));
                    }
                }
            }
        }
        // 逐环展开：地板按半径由内向外，再按球壳半径由内向外
        var ordered=new ArrayList<Change>(queue.size());
        var floorPart=new ArrayList<Change>();
        var shellPart=new ArrayList<Change>();
        var centerPos=new BlockPos(bx,by,bz);
        for(var c:queue)(c.state().isAir()?shellPart:(c.pos().getY()==by-1?floorPart:shellPart)).add(c);
        floorPart.sort((a,b)->Integer.compare(a.pos().distManhattan(centerPos),b.pos().distManhattan(centerPos)));
        shellPart.sort((a,b)->Integer.compare(a.pos().distManhattan(centerPos),b.pos().distManhattan(centerPos)));
        ordered.addAll(floorPart);ordered.addAll(shellPart);
        queue.clear();queue.addAll(ordered);
        RATE.put(owner.getUUID(),Math.max(1,queue.size()/60));   // 3 秒（60 刻）铺完
        QUEUE.put(owner.getUUID(),queue);
        owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "盖棺铁围山 · 清场：地板 + 穹顶 + 内部清空，共 "+queue.size()+" 个方块，分批施工，结束还原"));
    }

    private static boolean inWorld(ServerLevel level,BlockPos pos){
        return level.hasChunkAt(pos)&&pos.getY()>level.getMinBuildHeight() && pos.getY()<level.getMaxBuildHeight()-1;
    }

    /** 每刻推进施工；由 JogoCombat 的服务器 tick 调用。 */
    public static void tick() {
        if(QUEUE.isEmpty())return;
        int budget=1024;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null)return;
        for(var it=QUEUE.entrySet().iterator();it.hasNext();){
            var entry=it.next();
            var owner=cn.blockforge.generated.nnnnr1201forge2.NpcBridge.find(server,entry.getKey());
            ServerLevel level=LEVELS.get(entry.getKey());
            if(owner==null||owner.level()!=level){it.remove();restoreJournal(entry.getKey());continue;}
            var terrain=TERRAIN.get(entry.getKey());if(terrain==null){it.remove();continue;}
            var queue=entry.getValue();
            int perTick=Math.max(1,Math.min(512,RATE.getOrDefault(entry.getKey(),PER_TICK)));
            int done=0;
            while(done<perTick&&budget>0&&!queue.isEmpty()){
                done++;budget--;
                Change change=queue.poll();
                BlockPos pos=change.pos();
                terrain.place(pos,change.state());
            }
            if(queue.isEmpty()){
                it.remove();
                owner.sendSystemMessage(net.minecraft.network.chat.Component.literal("盖棺铁围山 · 地形已交给共用分区与恢复队列"));
            }
        }
    }

    /** 还原所有改过的方块；收起、结束、死亡、关服都调用。 */
    public static void restore(ServerPlayer owner) {
        QUEUE.remove(owner.getUUID());
        restoreJournal(owner.getUUID());
    }
    private static void restoreJournal(UUID id){
        RATE.remove(id);LEVELS.remove(id);var terrain=TERRAIN.remove(id);if(terrain!=null)terrain.close();
    }
    public static boolean sameLevel(ServerPlayer owner){return LEVELS.get(owner.getUUID())==owner.level();}
    public static void reset(){for(UUID id:List.copyOf(LEVELS.keySet()))restoreJournal(id);QUEUE.clear();RATE.clear();}
    public static boolean changed(ServerPlayer owner) { return TERRAIN.containsKey(owner.getUUID()); }
    private JogoTerrain() {}
}
