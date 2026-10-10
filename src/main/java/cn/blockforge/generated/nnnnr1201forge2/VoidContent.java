package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

public final class VoidContent {
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,"sukuna");
    public static final RegistryObject<Codec<? extends ChunkGenerator>> ARENA=GENERATORS.register("void_arena",()->VoidArenaGenerator.CODEC);
    public static final RegistryObject<EntityType<VoidBossEntity>> BOSS=Content.ENTITIES.register("void_aggregate",()->EntityType.Builder.of(VoidBossEntity::new,MobCategory.MONSTER).sized(4.5f,5.4f).fireImmune().clientTrackingRange(16).updateInterval(1).build("sukuna:void_aggregate"));
    public static final RegistryObject<EntityType<VoidNodeEntity>> NODE=Content.ENTITIES.register("void_node",()->EntityType.Builder.of(VoidNodeEntity::new,MobCategory.MONSTER).sized(1.2f,1.8f).fireImmune().clientTrackingRange(16).updateInterval(1).build("sukuna:void_node"));
    public static final RegistryObject<Item> KEY=Content.ITEMS.register("void_key",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(p instanceof ServerPlayer sp)VoidJourney.enter(sp);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static final RegistryObject<Item> EGG=Content.ITEMS.register("void_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(BOSS,0x07030c,0x932aff,new Item.Properties()));
    public static final RegistryObject<Item> CONTROLLER=Content.ITEMS.register("void_controller",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(l.isClientSide)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->net.minecraft.client.Minecraft.getInstance().setScreen(new VoidControllerScreen()));else if(p instanceof ServerPlayer sp)Network.sendController(sp);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static void init(IEventBus bus){GENERATORS.register(bus);bus.addListener(VoidContent::attributes);}
    private static void attributes(EntityAttributeCreationEvent e){e.put(BOSS.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,VoidRules.HEALTH).add(Attributes.MOVEMENT_SPEED,0.24).add(Attributes.FOLLOW_RANGE,120).add(Attributes.KNOCKBACK_RESISTANCE,1).build());e.put(NODE.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,60).add(Attributes.MOVEMENT_SPEED,0.22).add(Attributes.KNOCKBACK_RESISTANCE,1).build());}
    private static void tabs(net.minecraftforge.event.BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.COMBAT){e.accept(KEY.get());e.accept(CONTROLLER.get());e.accept(EGG.get());}}
}
