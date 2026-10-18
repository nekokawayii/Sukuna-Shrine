package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

public final class Content {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"sukuna");
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"sukuna");
    public static final RegistryObject<EntityType<BuddhaEntity>> BUDDHA=ENTITIES.register("sokushinbutsu",()->EntityType.Builder.of(BuddhaEntity::new,MobCategory.MONSTER).sized(1.6f,2.8f).clientTrackingRange(16).updateInterval(1).build("sukuna:sokushinbutsu"));
    public static final RegistryObject<EntityType<ParasiteEntity>> PARASITE=ENTITIES.register("parasitic_buddha",()->EntityType.Builder.of(ParasiteEntity::new,MobCategory.MONSTER).sized(0.5f,0.65f).clientTrackingRange(10).build("sukuna:parasitic_buddha"));
    public static final RegistryObject<EntityType<ParasiteEntity>> MOTHER=ENTITIES.register("buddha_mother",()->EntityType.Builder.of(ParasiteEntity::new,MobCategory.MONSTER).sized(1.8f,3).clientTrackingRange(14).build("sukuna:buddha_mother"));
    public static final RegistryObject<EntityType<DaitianEntity>> DAITIAN=ENTITIES.register("daitian",()->EntityType.Builder.of(DaitianEntity::new,MobCategory.MONSTER).sized(1.1f,2.95f).clientTrackingRange(12).updateInterval(1).build("sukuna:daitian"));
    public static final RegistryObject<EntityType<BrokenMahitoEntity>> BROKEN_MAHITO=ENTITIES.register("broken_mahito",()->EntityType.Builder.of(BrokenMahitoEntity::new,MobCategory.MONSTER).sized(0.8f,1.95f).clientTrackingRange(16).updateInterval(1).build("sukuna:broken_mahito"));
    public static final RegistryObject<EntityType<MahitoLimbEntity>> MAHITO_LIMB=ENTITIES.register("mahito_limb",()->EntityType.Builder.of(MahitoLimbEntity::new,MobCategory.MISC).sized(0.7f,0.7f).clientTrackingRange(16).updateInterval(1).build("sukuna:mahito_limb"));
    /** 隐藏 Boss「观测者」：假玩家外观，不 AI、不受伤、只盯着镜头。 */
    public static final RegistryObject<EntityType<ObserverEntity>> OBSERVER=ENTITIES.register("observer",()->EntityType.Builder.of(ObserverEntity::new,MobCategory.MONSTER).sized(0.6f,1.8f).clientTrackingRange(24).updateInterval(1).build("sukuna:observer"));
    public static final RegistryObject<Item> IDLE_TRANSFIGURATION_CURSE=ITEMS.register("idle_transfiguration_curse",()->new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));
    public static final RegistryObject<Item> MAHITO_FINGER=ITEMS.register("mahito_finger",()->new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));
    public static final RegistryObject<Item> YUJI_FAMILY_PHOTO=ITEMS.register("yuji_family_photo",()->new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));
    /** 不属于这个世界的碎片：五条悟通关后从灵魂通道裂隙里掉出来的剧情道具，长得像一张网页截图/鼠标指针。 */
    public static final RegistryObject<Item> BEYOND_FRAGMENT=ITEMS.register("beyond_fragment",()->new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()){
        /** 在虚空竞技场对它右键：开启第三阶段（场地坍塌 → 纯白空间 → 观测者）。 */
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){
            ItemStack stack=p.getItemInHand(hand);
            if(p instanceof ServerPlayer sp&&ObserverRitual.start(sp))return InteractionResultHolder.success(stack);
            return InteractionResultHolder.pass(stack);
        }
    });
    public static final RegistryObject<Item> SOUL_SEPARATION_NEEDLE=ITEMS.register("soul_separation_needle",()->new PickaxeItem(Tiers.NETHERITE,1,-2.4f,new Item.Properties().rarity(Rarity.EPIC).fireResistant().durability(2031)){
        @Override public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state,Level level,net.minecraft.core.BlockPos pos,Player player){return state.is(ProgressionContent.TRANSFIGURED_FLESH.get());}
        @Override public float getDestroySpeed(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){return state.is(ProgressionContent.TRANSFIGURED_FLESH.get())?18:0;}
        @Override public boolean isCorrectToolForDrops(net.minecraft.world.level.block.state.BlockState state){return state.is(ProgressionContent.TRANSFIGURED_FLESH.get());}
    });
    public static final RegistryObject<Item> FINGER=ITEMS.register("sukuna_finger",()->new GrowthFood(false));
    public static final RegistryObject<Item> BODY=ITEMS.register("buddha_body",()->new GrowthFood(true));
    public static final RegistryObject<Item> ORB=ITEMS.register("curse_orb",()->new Item(new Item.Properties().rarity(Rarity.UNCOMMON)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){ItemStack stack=p.getItemInHand(hand);if(p instanceof ServerPlayer sp){Curse.addRaw(p,150);Curse.sync(sp);if(!p.isCreative())stack.shrink(1);}return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
    });
    public static final RegistryObject<Item> CREATIVE_RESTORER=ITEMS.register("creative_restorer",CreativeRestorer::new);
    public static final RegistryObject<Item> RELIC=ITEMS.register("golden_relic",()->new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<Item> SEAL=ITEMS.register("severance_seal",()->new Item(new Item.Properties().rarity(Rarity.UNCOMMON)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){ItemStack stack=p.getItemInHand(hand);if(!level.isClientSide){int count=0;for(BuddhaEntity boss:level.getEntitiesOfClass(BuddhaEntity.class,p.getBoundingBox().inflate(16)))if(boss.interruptSiphon())count++;if(count>0){if(!p.isCreative())stack.shrink(1);p.getCooldowns().addCooldown(this,60);}else p.displayClientMessage(Component.literal("附近没有正在虹吸的即身佛"),true);}return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
    });
    public static final RegistryObject<Item> INCENSE=ITEMS.register("buddha_incense",()->new Item(new Item.Properties().rarity(Rarity.EPIC)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){ItemStack stack=p.getItemInHand(hand);if(!level.isClientSide){
            if(level.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){p.displayClientMessage(Component.literal("请先把游戏难度改为简单、普通或困难"),true);return InteractionResultHolder.fail(stack);}
            if(!level.getEntitiesOfClass(BuddhaEntity.class,p.getBoundingBox().inflate(96)).isEmpty()){p.displayClientMessage(Component.literal("附近已有即身佛"),true);return InteractionResultHolder.fail(stack);}
            Vec3 forward=new Vec3(p.getLookAngle().x,0,p.getLookAngle().z).normalize();if(forward.lengthSqr()<0.1)forward=new Vec3(0,0,1);Vec3 q=p.position().add(forward.scale(8));BuddhaEntity boss=BUDDHA.get().create(level);if(boss!=null){boss.moveTo(q.x,q.y,q.z,p.getYRot()+180,0);if(!level.noCollision(boss)){p.displayClientMessage(Component.literal("前方8格需要留出空地"),true);return InteractionResultHolder.fail(stack);}level.addFreshEntity(boss);boss.setTarget(p);if(!p.isCreative())stack.shrink(1);p.getCooldowns().addCooldown(this,100);}}
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
    });
    public static final RegistryObject<Item> EGG=ITEMS.register("buddha_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(BUDDHA,0x493d2b,0x9a47b6,new Item.Properties()));
    public static void init(IEventBus bus){ITEMS.register(bus);ENTITIES.register(bus);bus.addListener(Content::attributes);}
    private static void attributes(EntityAttributeCreationEvent e){e.put(DAITIAN.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,200).add(Attributes.MOVEMENT_SPEED,0.26).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.FOLLOW_RANGE,32).add(Attributes.KNOCKBACK_RESISTANCE,0.4).build());e.put(BUDDHA.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,5000).add(Attributes.MOVEMENT_SPEED,0.24).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.FOLLOW_RANGE,64).add(Attributes.KNOCKBACK_RESISTANCE,0.9).build());e.put(PARASITE.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.MOVEMENT_SPEED,0.32).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.FOLLOW_RANGE,48).build());e.put(MOTHER.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,160).add(Attributes.MOVEMENT_SPEED,0.23).add(Attributes.ATTACK_DAMAGE,12).add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,0.8).build());e.put(BROKEN_MAHITO.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20000).add(Attributes.MOVEMENT_SPEED,0.34).add(Attributes.ATTACK_DAMAGE,40).add(Attributes.FOLLOW_RANGE,64).add(Attributes.ARMOR,20).add(Attributes.KNOCKBACK_RESISTANCE,0.55).build());e.put(MAHITO_LIMB.get(),MahitoLimbEntity.createAttributes());e.put(OBSERVER.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.FOLLOW_RANGE,64).add(Attributes.KNOCKBACK_RESISTANCE,1).build());}
    private static void tabs(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.COMBAT){e.accept(FINGER.get());e.accept(BODY.get());e.accept(ORB.get());e.accept(RELIC.get());e.accept(SEAL.get());e.accept(INCENSE.get());e.accept(EGG.get());e.accept(IDLE_TRANSFIGURATION_CURSE.get());e.accept(MAHITO_FINGER.get());e.accept(YUJI_FAMILY_PHOTO.get());e.accept(BEYOND_FRAGMENT.get());e.accept(SOUL_SEPARATION_NEEDLE.get());e.accept(EternalLove.TRUE_FALSE.get());}}
    private static class GrowthFood extends Item {
        final boolean body;GrowthFood(boolean body){super(new Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant().food(new FoodProperties.Builder().nutrition(1).saturationMod(0).alwaysEat().build()));this.body=body;}
        private boolean allowed(Player p){return !(Uraume.unlocked(p)&&!Journey.unlocked(p))&&(body?Curse.fingers(p)>=20&&!Curse.awakened(p):Curse.fingers(p)<20);}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){if(!level.isClientSide&&!allowed(p)){p.displayClientMessage(Component.literal(Uraume.unlocked(p)&&!Journey.unlocked(p)?"你已选择里梅，不能吸收宿傩成长物品":body?(Curse.awakened(p)?"已经融合过即身佛肉体":"先吞下20根宿傩手指，才能承受肉体融合"):"已经吸收20根手指，无法继续增加"),true);return InteractionResultHolder.fail(p.getItemInHand(hand));}return super.use(level,p,hand);}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){if(user instanceof ServerPlayer p){if(!allowed(p))return stack;Curse.consume(p,body);}return super.finishUsingItem(stack,level,user);}
    }
}
