package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ShadowContent {
    public static final RegistryObject<EntityType<ShadowEntity>> SHIKIGAMI=Content.ENTITIES.register("ten_shadow",()->EntityType.Builder.of(ShadowEntity::new,MobCategory.CREATURE).sized(.9f,1.2f).clientTrackingRange(16).updateInterval(1).build("sukuna:ten_shadow"));
    public static final RegistryObject<Item> SOUL=Content.ITEMS.register("megumi_soul",()->new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SEANCE=Content.ITEMS.register("megumi_seance",()->new MegumiSeanceItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));
    public static void init(IEventBus bus){bus.addListener(ShadowContent::attributes);}
    private static void attributes(EntityAttributeCreationEvent e){
        e.put(SHIKIGAMI.get(),Mob.createMobAttributes().add(Attributes.MAX_HEALTH,80).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.ARMOR,5).add(Attributes.MOVEMENT_SPEED,.35).add(Attributes.FOLLOW_RANGE,64).add(Attributes.KNOCKBACK_RESISTANCE,.3).build());
    }
    private ShadowContent(){}
}
