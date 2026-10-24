package cn.blockforge.generated.nnnnr1201forge2;

import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;

/** 可选 Curios 1.20.x 兼容，不把饰品模组变成必装依赖。 */
public final class PrisonCurios {
    private static Method inventory,curios,stacks,cosmetics;
    private static boolean checked,failed;
    private static void init(){
        if(checked)return;checked=true;if(!ModList.get().isLoaded("curios"))return;
        try{
            inventory=Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosInventory",LivingEntity.class);
            curios=Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler").getMethod("getCurios");
            Class<?> slots=Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");stacks=slots.getMethod("getStacks");cosmetics=slots.getMethod("getCosmeticStacks");
        }catch(ReflectiveOperationException ex){failed=true;com.mojang.logging.LogUtils.getLogger().error("无法接入Curios封印库存，为避免物品丢失将拒绝封印该玩家",ex);}
    }
    private static Map<String,IItemHandlerModifiable> handlers(LivingEntity entity)throws ReflectiveOperationException{
        init();Map<String,IItemHandlerModifiable> result=new LinkedHashMap<>();if(failed)throw new IllegalStateException("Curios接口不可用");if(inventory==null)return result;
        Object value=((LazyOptional<?>)inventory.invoke(null,entity)).orElse(null);if(value==null)return result;
        Map<?,?> entries=(Map<?,?>)curios.invoke(value);
        for(var entry:entries.entrySet()){
            result.put(entry.getKey()+"/equipped",(IItemHandlerModifiable)stacks.invoke(entry.getValue()));
            result.put(entry.getKey()+"/cosmetic",(IItemHandlerModifiable)cosmetics.invoke(entry.getValue()));
        }
        return result;
    }
    public static boolean available(LivingEntity entity){try{handlers(entity);return true;}catch(ReflectiveOperationException|RuntimeException ex){return false;}}
    public static void take(LivingEntity entity,CompoundTag record){
        try{
            ListTag saved=record.getList("Curios",Tag.TAG_COMPOUND);
            for(var entry:handlers(entity).entrySet()){
                IItemHandlerModifiable h=entry.getValue();
                for(int slot=0;slot<h.getSlots();slot++){
                    ItemStack stack=h.getStackInSlot(slot);if(stack.isEmpty())continue;
                    if(entity instanceof ServerPlayer){CompoundTag item=new CompoundTag();item.putString("Handler",entry.getKey());item.putInt("Slot",slot);item.put("Stack",stack.save(new CompoundTag()));saved.add(item);record.put("Curios",saved);}
                    h.setStackInSlot(slot,ItemStack.EMPTY);
                }
            }
            record.put("Curios",saved);
        }catch(ReflectiveOperationException|RuntimeException ex){com.mojang.logging.LogUtils.getLogger().error("封印饰品库存时发生兼容错误，已保存的物品仍留在账本",ex);}
    }
    public static void restore(ServerPlayer player,CompoundTag record){
        Map<String,IItemHandlerModifiable> handlers;
        try{handlers=handlers(player);}catch(ReflectiveOperationException|RuntimeException ex){handlers=Map.of();}
        for(Tag value:record.getList("Curios",Tag.TAG_COMPOUND)){
            CompoundTag item=(CompoundTag)value;ItemStack stack=ItemStack.of(item.getCompound("Stack"));IItemHandlerModifiable h=handlers.get(item.getString("Handler"));int slot=item.getInt("Slot");
            if(h!=null&&slot>=0&&slot<h.getSlots()&&h.getStackInSlot(slot).isEmpty())h.setStackInSlot(slot,stack);else player.getInventory().placeItemBackInInventory(stack);
        }
    }
    private PrisonCurios(){}
}
