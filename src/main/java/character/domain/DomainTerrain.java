package character.domain;

import java.util.*;
import cn.blockforge.generated.nnnnr1201forge2.DomainClash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 共用地形事务。每次领域使用独立 Handle；方块按同一对拼平面选层，原地形不混入其它领域。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class DomainTerrain extends SavedData {
    private static final int BUDGET=4096;
    private static final class Cell {
        final DomainTerrainLayers<UUID,BlockState> layers;
        BlockState placed;
        Cell(BlockState before,BlockState placed){layers=new DomainTerrainLayers<>(before);this.placed=placed;}
    }
    private final Map<BlockPos,Cell> cells=new HashMap<>();
    private final Map<UUID,Handle> handles=new HashMap<>();
    private final ArrayDeque<BlockPos> pending=new ArrayDeque<>();
    private final Set<BlockPos> queued=new HashSet<>();
    private final ArrayDeque<BlockPos> urgent=new ArrayDeque<>();
    private final Set<BlockPos> urgentQueued=new HashSet<>();
    private void repaint(BlockPos pos){if(urgentQueued.add(pos))urgent.addLast(pos);}

    public static final class Handle {
        private final ServerLevel level;
        private final UUID id=UUID.randomUUID(),owner;
        private final Set<BlockPos> positions=new HashSet<>();
        private boolean closed;
        private Handle(ServerLevel level,UUID owner){this.level=level;this.owner=owner;}
        /** 可以逐批调用。覆盖其它领域时仍然保存最底层原状；容器和原生基岩保持不动。 */
        public boolean place(BlockPos pos,BlockState target){return !closed&&get(level).place(this,pos,target);}
        /** 已提交的可见结构是否落地；传送落座、生成装饰实体前调用，不等待未加载区块。 */
        public boolean ready(){
            if(closed)return false;var store=get(level);
            for(var pos:positions){var cell=store.cells.get(pos);if(cell!=null&&level.hasChunkAt(pos)&&!cell.placed.equals(store.desired(pos,cell)))return false;}
            return true;
        }
        /** 幂等关闭：立即停止施工，方块分批恢复，卸载区块等回来后继续。 */
        public void close(){
            if(closed)return;closed=true;var store=get(level);store.handles.remove(id);
            for(BlockPos pos:positions){var cell=store.cells.get(pos);if(cell!=null){cell.layers.remove(id);store.enqueue(pos);}}
            positions.clear();store.setDirty();
        }
    }
    public static Handle begin(ServerLevel level,UUID owner){
        var handle=new Handle(level,owner);get(level).handles.put(handle.id,handle);return handle;
    }
    public static DomainTerrain get(ServerLevel level){
        return level.getDataStorage().computeIfAbsent(tag->load(level,tag),DomainTerrain::new,"sukuna_domain_layers");
    }
    private void enqueue(BlockPos pos){if(queued.add(pos))pending.addLast(pos);}
    private boolean place(Handle handle,BlockPos pos,BlockState target){
        var level=handle.level;
        if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return false;
        Cell cell=cells.get(pos);var current=level.getBlockState(pos);
        if(cell==null){
            if(current.hasBlockEntity()||current.getDestroySpeed(level,pos)<0)return false;
            pos=pos.immutable();cell=new Cell(current,current);cells.put(pos,cell);
        }else if(!current.equals(cell.placed)){
            // 后来的人为改动成为新的原状；旧领域不强行覆盖，也不把它恢复成旧墙。
            forget(pos);return false;
        }
        pos=pos.immutable();cell.layers.put(handle.id,target);handle.positions.add(pos);enqueue(pos);setDirty();return true;
    }
    private void forget(BlockPos pos){
        cells.remove(pos);
        for(var handle:handles.values())handle.positions.remove(pos);
        setDirty();
    }
    /** 对拼起止及进度移动时调用；仅重排本方已提交的格子，不重新展开领域。 */
    public static void refresh(ServerLevel level,UUID owner){
        var store=get(level);
        for(var h:store.handles.values())if(h.owner.equals(owner))for(var pos:h.positions){
            var cell=store.cells.get(pos);
            // 只排入真正换边的格子，避免每次推进都把整颗十万格球体重新施工。
            if(cell!=null&&!cell.placed.equals(store.desired(pos,cell)))store.repaint(pos);
        }
    }
    public static void closeOwner(ServerLevel level,UUID owner){
        for(var h:List.copyOf(get(level).handles.values()))if(h.owner.equals(owner))h.close();
    }
    private BlockState desired(BlockPos pos,Cell cell){
        return cell.layers.visible(id->{var h=handles.get(id);return h!=null&&DomainClash.ownsTerrain(h.owner,Vec3.atCenterOf(pos));});
    }
    private void flush(ServerLevel level,int budget){
        int count=Math.min(budget,pending.size()+urgent.size());
        for(int i=0;i<count;i++){
            BlockPos pos;
            if(!urgent.isEmpty()){pos=urgent.removeFirst();urgentQueued.remove(pos);}
            else {pos=pending.removeFirst();queued.remove(pos);}
            var cell=cells.get(pos);if(cell==null)continue;
            if(!level.hasChunkAt(pos)){enqueue(pos);continue;}
            var current=level.getBlockState(pos);
            if(!current.equals(cell.placed)){forget(pos);continue;}
            BlockState desired=desired(pos,cell);
            if(!current.equals(desired)){
                if(!level.setBlock(pos,desired,2)){enqueue(pos);continue;}
                cell.placed=desired;setDirty();
            }
            if(cell.layers.empty()){cells.remove(pos);setDirty();}
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase==TickEvent.Phase.END)for(var level:event.getServer().getAllLevels())get(level).flush(level,BUDGET);
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event){
        for(var level:event.getServer().getAllLevels()){
            var store=get(level);for(var h:List.copyOf(store.handles.values()))h.close();
            store.flush(level,store.pending.size()+store.urgent.size()); // 未加载的记录仍留在 SavedData，正常重启接着还原。
        }
    }
    private static DomainTerrain load(ServerLevel level,CompoundTag tag){
        var store=new DomainTerrain();var blocks=level.holderLookup(Registries.BLOCK);
        var list=tag.getList("Cells",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){
            var t=list.getCompound(i);var pos=BlockPos.of(t.getLong("Pos"));
            store.cells.put(pos,new Cell(NbtUtils.readBlockState(blocks,t.getCompound("Before")),NbtUtils.readBlockState(blocks,t.getCompound("Placed"))));
            store.enqueue(pos);
        }
        return store;
    }
    @Override public CompoundTag save(CompoundTag tag){
        var list=new ListTag();
        for(var e:cells.entrySet()){
            var t=new CompoundTag();t.putLong("Pos",e.getKey().asLong());
            t.put("Before",NbtUtils.writeBlockState(e.getValue().layers.original()));
            t.put("Placed",NbtUtils.writeBlockState(e.getValue().placed));list.add(t);
        }
        tag.put("Cells",list);return tag;
    }
}
