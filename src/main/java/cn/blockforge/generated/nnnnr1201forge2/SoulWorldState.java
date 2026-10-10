package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** 灵魂通道通关记录；真人通关后不再自动补刷，旧版再战时间仅保留读写兼容。 */
public final class SoulWorldState extends SavedData {
    private boolean mahitoSpawned,mahitoDefeated,yujiSummoned,photoChestRepaired;
    private long mahitoRespawnAt;
    public long mahitoRespawnAt(){return mahitoRespawnAt;}
    private CompoundTag condensedMaidOwners=new CompoundTag();
    public boolean maidCondensed(java.util.UUID owner){return condensedMaidOwners.getBoolean(owner.toString());}
    public void markMaidCondensed(java.util.UUID owner){if(!maidCondensed(owner)){condensedMaidOwners.putBoolean(owner.toString(),true);setDirty();}}

    public static SoulWorldState get(ServerLevel level){
        return level.getDataStorage().computeIfAbsent(SoulWorldState::load,SoulWorldState::new,"sukuna_soul_corridor");
    }
    public static SoulWorldState load(CompoundTag tag){
        SoulWorldState state=new SoulWorldState();
        state.condensedMaidOwners=tag.getCompound("CondensedMaidOwners").copy();
        state.mahitoSpawned=tag.getBoolean("MahitoSpawned");
        state.mahitoDefeated=tag.getBoolean("MahitoDefeated");
        state.mahitoRespawnAt=tag.getLong("MahitoRespawnAt");
        state.yujiSummoned=tag.getBoolean("YujiSummoned");state.guardianSummoned=tag.getBoolean("GuardianSummoned");state.photoChestRepaired=tag.getBoolean("PhotoChestRepaired");
        return state;
    }
    public boolean mahitoSpawned(){return mahitoSpawned;}
    public boolean mahitoDefeated(){return mahitoDefeated;}
    private boolean guardianSummoned;
    public boolean guardianSummoned(){return guardianSummoned;}
    public void markGuardianSummoned(){guardianSummoned=true;setDirty();}
    public boolean photoChestRepaired(){return photoChestRepaired;}
    public void markPhotoChestRepaired(){photoChestRepaired=true;setDirty();}
    public boolean yujiSummoned(){return yujiSummoned;}
    public void markMahitoSpawned(){mahitoSpawned=true;setDirty();}
    public void markMahitoDefeated(long now){mahitoSpawned=true;mahitoDefeated=true;mahitoRespawnAt=now+1200;setDirty();}
    public void markYujiSummoned(){yujiSummoned=true;setDirty();}
    public void reset(){mahitoSpawned=mahitoDefeated;yujiSummoned=false;guardianSummoned=false;setDirty();}
    @Override public CompoundTag save(CompoundTag tag){
        tag.put("CondensedMaidOwners",condensedMaidOwners.copy());
        tag.putBoolean("MahitoSpawned",mahitoSpawned);
        tag.putBoolean("MahitoDefeated",mahitoDefeated);
        tag.putLong("MahitoRespawnAt",mahitoRespawnAt);
        tag.putBoolean("YujiSummoned",yujiSummoned);tag.putBoolean("GuardianSummoned",guardianSummoned);tag.putBoolean("PhotoChestRepaired",photoChestRepaired);
        return tag;
    }
}
