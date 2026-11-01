package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

/** 每种咒灵都有独立实体编号和刷怪蛋，旧low_curse仍保留并随机选外形。 */
public final class CurseRoster {
    public static final Map<String,RegistryObject<EntityType<LowCurseEntity>>> TYPES=new LinkedHashMap<>();
    public static final List<RegistryObject<Item>> EGGS=new ArrayList<>();
    static {
        for(var form:CurseCatalog.FORMS){
            String id="curse_"+form.id();
            var type=Content.ENTITIES.register(id,()->EntityType.Builder.of(LowCurseEntity::new,MobCategory.MONSTER).sized(form.width(),form.height()).clientTrackingRange(12).updateInterval(2).build("sukuna:"+id));
            TYPES.put(form.id(),type);
            EGGS.add(Content.ITEMS.register(id+"_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(type,0x302A38,form.grade()==0?0xA51949:0x9C859F,new Item.Properties())));
        }
    }
    public static void init(net.minecraftforge.eventbus.api.IEventBus bus){bus.addListener(CurseRoster::attributes);}
    private static void attributes(EntityAttributeCreationEvent event){for(var type:TYPES.values())event.put(type.get(),LowCurseEntity.createAttributes().build());}
    public static LowCurseEntity create(net.minecraft.world.level.Level level,int index){var f=CurseCatalog.form(index);var entity=TYPES.get(f.id()).get().create(level);if(entity!=null)entity.configure(index);return entity;}
    private CurseRoster(){}
}
