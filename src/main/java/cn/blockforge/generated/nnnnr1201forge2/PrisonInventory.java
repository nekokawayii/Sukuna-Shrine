package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/** 封印库存独立保管，背包内嵌的容器NBT随物品一起保存。 */
public final class PrisonInventory {
    public static void capture(ServerPlayer p,CompoundTag record){
        // 先暂存光标与自身2×2合成格，关界面时不会把满背包物品丢在外面。
        ListTag cursor=new ListTag();stash(cursor,p.containerMenu.getCarried());p.containerMenu.setCarried(ItemStack.EMPTY);
        var craft=p.inventoryMenu.getCraftSlots();for(int i=0;i<craft.getContainerSize();i++){stash(cursor,craft.getItem(i));craft.setItem(i,ItemStack.EMPTY);}record.put("Extras",cursor);
        p.closeContainer();
        record.put("Inventory",p.getInventory().save(new ListTag()));record.putInt("Selected",p.getInventory().selected);
        record.put("EnderItems",p.getEnderChestInventory().createTag());
        record.putInt("XpLevel",p.experienceLevel);record.putInt("XpTotal",p.totalExperience);record.putFloat("XpProgress",p.experienceProgress);
        p.getInventory().clearContent();p.getEnderChestInventory().clearContent();p.setExperienceLevels(0);p.setExperiencePoints(0);p.totalExperience=0;
        confiscate(p,record);sync(p);
    }
    public static void confiscate(LivingEntity entity,CompoundTag record){
        // 首次与封印中得到的新物品都保管，绝不直接删除。
        ListTag extras=record.getList("Extras",Tag.TAG_COMPOUND);
        if(entity instanceof ServerPlayer p){
            for(int i=0;i<p.getInventory().getContainerSize();i++){stash(extras,p.getInventory().getItem(i));p.getInventory().setItem(i,ItemStack.EMPTY);}
            for(int i=0;i<p.getEnderChestInventory().getContainerSize();i++){stash(extras,p.getEnderChestInventory().getItem(i));p.getEnderChestInventory().setItem(i,ItemStack.EMPTY);}
            if(!p.containerMenu.getCarried().isEmpty()){stash(extras,p.containerMenu.getCarried());p.containerMenu.setCarried(ItemStack.EMPTY);}
            var craft=p.inventoryMenu.getCraftSlots();for(int i=0;i<craft.getContainerSize();i++){stash(extras,craft.getItem(i));craft.setItem(i,ItemStack.EMPTY);}
        }else{
            for(EquipmentSlot slot:EquipmentSlot.values())entity.setItemSlot(slot,ItemStack.EMPTY);
            if(entity instanceof Container container)container.clearContent();
            if(entity instanceof net.minecraft.world.entity.npc.InventoryCarrier carrier)carrier.getInventory().clearContent();
        }
        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler->clear(handler,entity instanceof ServerPlayer?extras:null));
        record.put("Extras",extras);PrisonCurios.take(entity,record);
    }
    private static void clear(IItemHandler h,ListTag extras){
        for(int slot=0;slot<h.getSlots();slot++){
            ItemStack stack=h.getStackInSlot(slot);if(stack.isEmpty())continue;
            if(h instanceof IItemHandlerModifiable m){if(extras!=null)stash(extras,stack);m.setStackInSlot(slot,ItemStack.EMPTY);}
            else {ItemStack taken=h.extractItem(slot,Integer.MAX_VALUE,false);if(extras!=null)stash(extras,taken);}
        }
    }
    private static void stash(ListTag list,ItemStack stack){if(!stack.isEmpty())list.add(stack.save(new CompoundTag()));}
    public static void restore(ServerPlayer p,CompoundTag record){
        p.closeContainer();confiscate(p,record);
        p.getInventory().load(record.getList("Inventory",Tag.TAG_COMPOUND));p.getInventory().selected=record.getInt("Selected");
        p.getEnderChestInventory().fromTag(record.getList("EnderItems",Tag.TAG_COMPOUND));
        p.setExperienceLevels(record.getInt("XpLevel"));p.experienceProgress=record.getFloat("XpProgress");p.totalExperience=record.getInt("XpTotal");
        for(Tag t:record.getList("Extras",Tag.TAG_COMPOUND))p.getInventory().placeItemBackInInventory(ItemStack.of((CompoundTag)t));
        PrisonCurios.restore(p,record);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(p.experienceProgress,p.totalExperience,p.experienceLevel));sync(p);
    }
    public static void sync(ServerPlayer p){p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();p.containerMenu.broadcastChanges();}
    private PrisonInventory(){}
}
