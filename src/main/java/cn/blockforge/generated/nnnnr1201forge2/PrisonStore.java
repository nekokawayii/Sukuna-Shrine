package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** 全部封印的唯一服务端账本；玩家离线时仍保留库存和解封状态。 */
public final class PrisonStore extends SavedData {
    final Map<UUID,CompoundTag> realms=new LinkedHashMap<>();
    final Map<UUID,CompoundTag> prisoners=new LinkedHashMap<>();
    private int nextCell;
    public static PrisonStore get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(PrisonStore::load,PrisonStore::new,"sukuna_prison_realm");}
    public CompoundTag realm(UUID id){return realms.get(id);}
    public CompoundTag prisoner(UUID id){return prisoners.get(id);}
    public CompoundTag create(UUID id,UUID owner,String dimension,double x,double y,double z){
        CompoundTag r=realms.get(id);if(r!=null)return r;
        r=new CompoundTag();r.putUUID("Id",id);r.putUUID("Owner",owner);r.putString("Dimension",dimension);r.putDouble("X",x);r.putDouble("Y",y);r.putDouble("Z",z);
        int index=nextCell++;r.putInt("CellX",(index%4096)*64+8);r.putInt("CellZ",(index/4096)*64+8);realms.put(id,r);setDirty();return r;
    }
    public boolean occupied(UUID id){return prisoners.values().stream().anyMatch(t->t.getUUID("Realm").equals(id));}
    /** 完整快照真正写进物品；服务端账本继续维持在线/离线玩家和维度中的封印。 */
    public CompoundTag pack(UUID id){
        CompoundTag room=realm(id);if(room==null||room.getBoolean("Released")||room.getBoolean("Portable"))return null;
        room.putBoolean("Portable",true);room.putUUID("PortableToken",UUID.randomUUID());setDirty();return portableSnapshot(id);
    }
    public CompoundTag portableSnapshot(UUID id){
        CompoundTag room=realm(id);if(room==null)return null;
        CompoundTag payload=new CompoundTag();payload.putInt("Version",1);payload.put("Room",room.copy());ListTag contents=new ListTag();
        for(CompoundTag record:prisoners.values())if(record.getUUID("Realm").equals(id))contents.add(record.copy());
        payload.put("Prisoners",contents);return payload;
    }
    public boolean canPlace(UUID id,UUID token){
        CompoundTag room=realm(id);return room!=null&&!room.getBoolean("Released")&&room.getBoolean("Portable")&&room.hasUUID("PortableToken")&&room.getUUID("PortableToken").equals(token);
    }
    public void placed(UUID id,UUID owner,String dimension,double x,double y,double z){
        CompoundTag room=realm(id);if(room==null)return;
        room.putUUID("Owner",owner);room.putString("Dimension",dimension);room.putDouble("X",x);room.putDouble("Y",y);room.putDouble("Z",z);
        room.putBoolean("Portable",false);room.remove("PortableToken");setDirty();
    }
    static PrisonStore load(CompoundTag tag){
        PrisonStore s=new PrisonStore();s.nextCell=tag.getInt("NextCell");
        for(Tag value:tag.getList("Realms",Tag.TAG_COMPOUND)){CompoundTag t=(CompoundTag)value;s.realms.put(t.getUUID("Id"),t);}
        for(Tag value:tag.getList("Prisoners",Tag.TAG_COMPOUND)){CompoundTag t=(CompoundTag)value;s.prisoners.put(t.getUUID("Id"),t);}
        return s;
    }
    @Override public CompoundTag save(CompoundTag tag){
        tag.putInt("NextCell",nextCell);ListTag rs=new ListTag(),ps=new ListTag();realms.values().forEach(t->rs.add(t.copy()));prisoners.values().forEach(t->ps.add(t.copy()));tag.put("Realms",rs);tag.put("Prisoners",ps);return tag;
    }
}
