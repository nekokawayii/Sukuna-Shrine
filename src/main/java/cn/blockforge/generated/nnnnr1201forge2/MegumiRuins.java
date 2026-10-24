package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;

/** 每座高专遗迹补入一份灵魂；记录保存在世界，兼容已经探索过的旧遗迹。 */
public final class MegumiRuins {
    public static final class Found extends SavedData {
        final Map<Long,Long> chests=new HashMap<>();
        static Found load(CompoundTag tag){Found f=new Found();CompoundTag entries=tag.getCompound("Chests");for(String key:entries.getAllKeys())try{f.chests.put(Long.parseLong(key),entries.getLong(key));}catch(NumberFormatException ignored){}return f;}
        @Override public CompoundTag save(CompoundTag tag){CompoundTag entries=new CompoundTag();chests.forEach((k,v)->entries.putLong(k.toString(),v));tag.put("Chests",entries);return tag;}
    }
    public static void visit(ServerPlayer p,BlockPos origin,boolean enter){
        ServerLevel level=p.serverLevel();Found found=level.getDataStorage().computeIfAbsent(Found::load,Found::new,"sukuna_megumi_ruins");long key=origin.asLong();
        if(found.chests.containsKey(key)){if(enter)hint(p,BlockPos.of(found.chests.get(key)));return;}
        if(p.tickCount%40!=0&&!enter)return;
        List<ChestBlockEntity> chests=new ArrayList<>();
        for(int cx=origin.getX()>>4;cx<=(origin.getX()+65)>>4;cx++)for(int cz=origin.getZ()>>4;cz<=(origin.getZ()+65)>>4;cz++){
            if(!level.hasChunk(cx,cz))continue;
            for(var block:level.getChunk(cx,cz).getBlockEntities().values())if(block instanceof ChestBlockEntity chest){BlockPos pos=chest.getBlockPos();if(pos.getX()>=origin.getX()&&pos.getX()<=origin.getX()+65&&pos.getZ()>=origin.getZ()&&pos.getZ()<=origin.getZ()+65&&pos.getY()>=origin.getY()&&pos.getY()<origin.getY()+36)chests.add(chest);}
        }
        chests.sort(Comparator.comparingInt(c->c.getBlockPos().getY()));
        for(ChestBlockEntity chest:chests){chest.unpackLootTable(p);for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).isEmpty()){chest.setItem(slot,new ItemStack(ShadowContent.SOUL.get()));chest.setChanged();found.chests.put(key,chest.getBlockPos().asLong());found.setDirty();hint(p,chest.getBlockPos());return;}}
        // 现有高专模板只有建筑，没有箱子；在已加载的空地上补一只灵魂箱，不覆盖任何方块。
        for(int radius=2;radius<=8;radius++)for(int dy=-2;dy<=1;dy++)for(int side=0;side<16;side++){
            double angle=side*Math.PI/8;BlockPos pos=p.blockPosition().offset((int)Math.round(Math.cos(angle)*radius),dy,(int)Math.round(Math.sin(angle)*radius));
            if(pos.getX()<origin.getX()||pos.getX()>origin.getX()+64||pos.getZ()<origin.getZ()||pos.getZ()>origin.getZ()+64||pos.getY()<origin.getY()||pos.getY()>origin.getY()+27)continue;
            if(!level.hasChunkAt(pos)||!level.getBlockState(pos).isAir()||!level.getBlockState(pos.above()).isAir()||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP))continue;
            if(!level.getEntities(null,new net.minecraft.world.phys.AABB(pos)).isEmpty())continue;
            level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
            if(level.getBlockEntity(pos) instanceof ChestBlockEntity chest){chest.setItem(13,new ItemStack(ShadowContent.SOUL.get()));chest.setCustomName(Component.literal("高专封存 · 伏黑惠灵魂"));chest.setChanged();found.chests.put(key,pos.asLong());found.setDirty();hint(p,pos);return;}
        }
    }
    private static void hint(ServerPlayer p,BlockPos pos){p.sendSystemMessage(Component.literal("高专遗迹中曾封存伏黑惠灵魂 · 箱子位置："+pos.getX()+"，"+pos.getY()+"，"+pos.getZ()+"（每座遗迹一份，取走后不补发）"));}
    private MegumiRuins(){}
}
