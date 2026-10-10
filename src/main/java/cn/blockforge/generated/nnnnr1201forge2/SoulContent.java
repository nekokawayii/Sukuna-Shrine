package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

public final class SoulContent {
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,"sukuna");
    public static final RegistryObject<Codec<? extends ChunkGenerator>> CORRIDOR=GENERATORS.register("soul_corridor",()->SoulCorridorGenerator.CODEC);
    public static final RegistryObject<EntityType<YujiEntity>> YUJI=Content.ENTITIES.register("yuji_echo",()->EntityType.Builder.of(YujiEntity::new,MobCategory.MONSTER).sized(0.6f,1.8f).clientTrackingRange(16).updateInterval(1).build("sukuna:yuji_echo"));
    public static final RegistryObject<EntityType<GuardianEntity>> GUARDIAN=Content.ENTITIES.register("yuji_guardian",()->EntityType.Builder.of(GuardianEntity::new,MobCategory.MONSTER).sized(0.6f,1.8f).clientTrackingRange(16).updateInterval(1).build("sukuna:yuji_guardian"));
    public static final RegistryObject<Item> GUARDIAN_EGG=Content.ITEMS.register("yuji_guardian_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(GUARDIAN,0x191919,0xc88484,new Item.Properties()));
    /** 黑色、通往地下的灵魂通道传送门；右键在遗迹入口和灵魂通道之间切换。 */
    public static final RegistryObject<Block> GATE=ProgressionContent.BLOCKS.register("soul_gate",()->new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).strength(-1,3600000).noCollission().noOcclusion().lightLevel(s->2)){
        @Override public void entityInside(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,Entity entity){
            if(entity instanceof ServerPlayer p&&level.getGameTime()>=p.getPersistentData().getLong("SukunaGateCooldown")){p.getPersistentData().putLong("SukunaGateCooldown",level.getGameTime()+80);SoulJourney.enter(p);}
        }
        @Override public InteractionResult use(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
            if(player instanceof ServerPlayer p)SoulJourney.enter(p);return InteractionResult.sidedSuccess(level.isClientSide);
        }
    });
    public static final RegistryObject<Block> PASSAGE=ProgressionContent.BLOCKS.register("soul_passage",()->new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).strength(-1,3600000).lightLevel(s->8)){
        @Override public InteractionResult use(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){if(player instanceof ServerPlayer p)SoulJourney.passage(p,pos);return InteractionResult.sidedSuccess(level.isClientSide);}
    });
    public static final RegistryObject<Block> RICE=ProgressionContent.BLOCKS.register("iwate_rice",()->new Block(BlockBehaviour.Properties.copy(Blocks.GRASS).noCollission().noOcclusion().strength(0)));
    public static final RegistryObject<Block> THREAD=ProgressionContent.BLOCKS.register("soul_thread",()->new Block(BlockBehaviour.Properties.copy(Blocks.COBWEB).noCollission().strength(0.2f).lightLevel(s->3)){
        @Override public void entityInside(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,Entity entity){
            if(!level.isClientSide&&entity instanceof LivingEntity living)living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,30,0,false,false));
        }
    });
    public static final RegistryObject<Block> PHOTO_SLOT=ProgressionContent.BLOCKS.register("yuji_photo_slot",()->new Block(BlockBehaviour.Properties.copy(Blocks.POLISHED_DEEPSLATE).strength(-1,3600000).lightLevel(s->5)){
        @Override public InteractionResult use(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
            ItemStack stack=player.getItemInHand(hand);if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
            if(!stack.is(Content.YUJI_FAMILY_PHOTO.get())){p.displayClientMessage(net.minecraft.network.chat.Component.literal("照片槽需要放入“虎杖家双人合照”"),true);return InteractionResult.CONSUME;}
            if(level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){p.displayClientMessage(net.minecraft.network.chat.Component.literal("请先把难度改为简单、普通或困难"),true);return InteractionResult.CONSUME;}
            SoulWorldState world=SoulWorldState.get(p.serverLevel());if(world.guardianSummoned()){p.displayClientMessage(net.minecraft.network.chat.Component.literal("净土守护者已经被合照唤醒"),true);return InteractionResult.CONSUME;}
            GuardianEntity yuji=GUARDIAN.get().create(level);if(yuji==null)return InteractionResult.FAIL;yuji.configure(p);yuji.moveTo(pos.getX()+0.5,pos.getY()+2,pos.getZ()+4.5,180,0);yuji.setTarget(p);if(!level.noCollision(yuji)||!level.addFreshEntity(yuji))return InteractionResult.FAIL;world.markGuardianSummoned();for(YujiEntity old:level.getEntitiesOfClass(YujiEntity.class,yuji.getBoundingBox().inflate(128),YujiEntity::familyMember))old.discard();if(!p.isCreative())stack.shrink(1);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("合照泛起微光，虎杖悠仁·净土守护者降临。"));return InteractionResult.CONSUME;
        }
    });
    public static final RegistryObject<Item> EGG=Content.ITEMS.register("yuji_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(YUJI,0x171d33,0xda8a8a,new Item.Properties()));
    public static final RegistryObject<Item> GATE_ITEM=Content.ITEMS.register("soul_gate",()->new BlockItem(GATE.get(),new Item.Properties().rarity(Rarity.EPIC)));
    public static void init(IEventBus bus){GENERATORS.register(bus);bus.addListener(SoulContent::attributes);}
    private static void attributes(EntityAttributeCreationEvent event){event.put(GUARDIAN.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,800000).add(Attributes.ARMOR,80).add(Attributes.MOVEMENT_SPEED,0.42).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.FOLLOW_RANGE,128).add(Attributes.KNOCKBACK_RESISTANCE,1).build());event.put(YUJI.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30000).add(Attributes.MOVEMENT_SPEED,0.34).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.FOLLOW_RANGE,96).add(Attributes.KNOCKBACK_RESISTANCE,0.8).build());}
    private static void tabs(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event){if(event.getTabKey()==CreativeModeTabs.COMBAT){event.accept(EGG.get());event.accept(GATE_ITEM.get());}}
}
