package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** 生物完整存档留在服务端世界数据，不塞进物品或玩家同步包。 */
public final class LegionStore extends SavedData {
    private final CompoundTag armies;
    public LegionStore(){armies=new CompoundTag();}
    private LegionStore(CompoundTag tag){armies=tag.getCompound("Armies");}
    public static LegionStore get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(LegionStore::new,LegionStore::new,"sukuna_authority_legions");}
    public CompoundTag army(java.util.UUID owner){String key=owner.toString();if(!armies.contains(key)){armies.put(key,new CompoundTag());setDirty();}return armies.getCompound(key);}
    @Override public CompoundTag save(CompoundTag tag){tag.put("Armies",armies);return tag;}
}
