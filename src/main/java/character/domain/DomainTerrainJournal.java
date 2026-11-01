package character.domain;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/** 领域施工日志：记录原状和实际写入状态，正常保存后重启会继续还原，绝不主动加载远处区块。 */
public final class DomainTerrainJournal extends SavedData {
    private record Entry(UUID owner, BlockPos pos, BlockState before, BlockState placed) { }
    private final Map<BlockPos, Entry> entries = new HashMap<>();
    private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
    public static DomainTerrainJournal get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(tag -> load(level,tag), DomainTerrainJournal::new, "sukuna_domain_terrain");
    }
    private static DomainTerrainJournal load(ServerLevel level, CompoundTag tag) {
        var journal = new DomainTerrainJournal();
        var blocks = level.holderLookup(Registries.BLOCK);
        var list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){
            var t=list.getCompound(i);if(!t.hasUUID("Owner"))continue;
            var pos=BlockPos.of(t.getLong("Pos"));
            var entry=new Entry(t.getUUID("Owner"),pos,NbtUtils.readBlockState(blocks,t.getCompound("Before")),NbtUtils.readBlockState(blocks,t.getCompound("Placed")));
            journal.entries.put(pos,entry);journal.queue.add(pos);
        }
        return journal;
    }
    public boolean place(ServerLevel level,UUID owner,BlockPos pos,BlockState placed) {
        if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos)||entries.containsKey(pos))return false;
        var before=level.getBlockState(pos);
        if(before.equals(placed)||before.hasBlockEntity()||before.getDestroySpeed(level,pos)<0)return false;
        var entry=new Entry(owner,pos.immutable(),before,placed);
        entries.put(entry.pos,entry);queue.add(entry.pos);setDirty();
        if(!level.setBlock(pos,placed,2)){entries.remove(pos);return false;}
        return true;
    }
    /** live 为当前仍在工作的领域；没有活动主人的记录一律进入恢复。扫描跳过项也计预算。 */
    public void restore(ServerLevel level,Set<UUID> live,int budget) {
        int count=Math.min(budget,queue.size());
        for(int i=0;i<count;i++){
            var pos=queue.removeFirst();var entry=entries.get(pos);if(entry==null)continue;
            if(live.contains(entry.owner)||!level.hasChunkAt(pos)){queue.addLast(pos);continue;}
            var current=level.getBlockState(pos);
            if(current.equals(entry.placed)&&!current.hasBlockEntity()){
                if(!level.setBlock(pos,entry.before,2)){queue.addLast(pos);continue;}
            }
            entries.remove(pos);setDirty();
        }
    }
    public int size(){return entries.size();}
    @Override public CompoundTag save(CompoundTag tag) {
        var list=new ListTag();
        for(var e:entries.values()){
            var t=new CompoundTag();t.putUUID("Owner",e.owner);t.putLong("Pos",e.pos.asLong());
            t.put("Before",NbtUtils.writeBlockState(e.before));t.put("Placed",NbtUtils.writeBlockState(e.placed));list.add(t);
        }
        tag.put("Entries",list);return tag;
    }
}
