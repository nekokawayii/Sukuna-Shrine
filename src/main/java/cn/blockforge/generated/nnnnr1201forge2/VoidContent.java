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
    public static final RegistryObject<EntityType<GojoEntity>> GOJO=Content.ENTITIES.register("gojo",()->EntityType.Builder.of(GojoEntity::new,MobCategory.MONSTER).sized(0.6f,1.8f).fireImmune().clientTrackingRange(16).updateInterval(1).build("sukuna:gojo"));
    public static final RegistryObject<Item> KEY=Content.ITEMS.register("void_key",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(p instanceof ServerPlayer sp)VoidJourney.enter(sp);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static final RegistryObject<Item> FRAGMENT=Content.ITEMS.register("gojo_fragment",()->new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){ItemStack stack=p.getItemInHand(h);if(!l.isClientSide&&GojoPlayer.consume(p)&&!p.isCreative())stack.shrink(1);return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}
    });
    public static final RegistryObject<Item> EGG=Content.ITEMS.register("gojo_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(GOJO,0x0b0b12,0x8fd0ff,new Item.Properties()));
    public static final RegistryObject<Item> CONTROLLER=Content.ITEMS.register("void_controller",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(l.isClientSide)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->net.minecraft.client.Minecraft.getInstance().setScreen(new VoidControllerScreen()));else if(p instanceof ServerPlayer sp)Network.sendController(sp);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static void init(IEventBus bus){GENERATORS.register(bus);bus.addListener(VoidContent::attributes);}
    private static void attributes(EntityAttributeCreationEvent e){GojoEntity.attributes(e);}
    private static void tabs(net.minecraftforge.event.BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.COMBAT){e.accept(KEY.get());e.accept(CONTROLLER.get());e.accept(EGG.get());e.accept(FRAGMENT.get());}}
}
