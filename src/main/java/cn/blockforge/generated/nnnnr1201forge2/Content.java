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
    /** 夏油杰：第二章人形 Boss（HD 皮肤 geto.png + 原版玩家骨架）。 */
    public static final RegistryObject<EntityType<GetoEntity>> GETO=ENTITIES.register("geto",()->EntityType.Builder.of(GetoEntity::new,MobCategory.MONSTER).sized(0.6f,1.8f).clientTrackingRange(16).updateInterval(1).build("sukuna:geto"));
    /** 低级咒灵：夏油杰「咒灵操术·群袭」的召唤物，运行时解析 basement_wanderer.bbmodel。 */
    public static final RegistryObject<EntityType<LowCurseEntity>> LOW_CURSE=ENTITIES.register("low_curse",()->EntityType.Builder.of(LowCurseEntity::new,MobCategory.MONSTER).sized(0.7f,2.0f).clientTrackingRange(12).updateInterval(2).build("sukuna:low_curse"));
    /** 里香：乙骨忧太的召唤物（无限血、两分钟、自动攻击）。 */
    public static final RegistryObject<EntityType<RikaEntity>> RIKA=ENTITIES.register("rika",()->EntityType.Builder.of(RikaEntity::new,MobCategory.CREATURE).sized(0.9f,2.2f).clientTrackingRange(16).updateInterval(1).build("sukuna:rika"));
    public static final RegistryObject<Item> IDLE_TRANSFIGURATION_CURSE=ITEMS.register("idle_transfiguration_curse",()->new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));
    public static final RegistryObject<Item> MAHITO_FINGER=ITEMS.register("mahito_finger",()->new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));
    public static final RegistryObject<Item> YUJI_FAMILY_PHOTO=ITEMS.register("yuji_family_photo",()->new Item(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));
    /** 不属于这个世界的碎片：五条悟通关后从灵魂通道裂隙里掉出来的剧情道具，长得像一张网页截图/鼠标指针。 */
    /** 诛伏赐死：审判者 + 处刑人之剑。 */
    public static final RegistryObject<EntityType<character.higuruma.HigurumaJudgeEntity>> HIGURUMA_JUDGE =
        ENTITIES.register("higuruma_judge", () -> EntityType.Builder.<character.higuruma.HigurumaJudgeEntity>of(
            character.higuruma.HigurumaJudgeEntity::new, MobCategory.CREATURE).sized(0.8f, 2.2f)
            .clientTrackingRange(10).updateInterval(2).build("sukuna:higuruma_judge"));
    public static final RegistryObject<Item> EXECUTIONER_SWORD=ITEMS.register("executioner_sword",
        ()->new character.higuruma.ExecutionerSwordItem(new Item.Properties().stacksTo(1).durability(1).rarity(Rarity.EPIC)));
    /** 无为转变·分身 / 尾巴：玩家模型、友方。 */
    public static final RegistryObject<EntityType<character.mahito.MahitoCloneEntity>> MAHITO_CLONE =
        ENTITIES.register("mahito_clone", () -> EntityType.Builder.<character.mahito.MahitoCloneEntity>of(
            character.mahito.MahitoCloneEntity::new, MobCategory.CREATURE).sized(0.6f, 1.8f)
            .clientTrackingRange(10).updateInterval(2).build("sukuna:mahito_clone"));
    /** 圆钝果实：真人领域里的不可移动实体（渲染 一堆手.bbmodel）。 */
    public static final RegistryObject<EntityType<YiduiShouEntity>> YIDUI = ENTITIES.register("yidui",
        () -> EntityType.Builder.of(YiduiShouEntity::new, MobCategory.MONSTER).sized(2.0f, 2.0f).clientTrackingRange(10).updateInterval(20).build("sukuna:yidui"));
    /** 漏壶（NPC）：敌对，使用漏壶技能；见到玩家开领域（冷却 300 秒）。 */
    public static final RegistryObject<EntityType<JogoNpcEntity>> JOGO_NPC = ENTITIES.register("jogo_npc",
        () -> EntityType.Builder.of(JogoNpcEntity::new, MobCategory.MONSTER).sized(0.6f, 1.9f).fireImmune().clientTrackingRange(16).updateInterval(1).build("sukuna:jogo_npc"));
    public static final RegistryObject<Item> JOGO_NPC_EGG = ITEMS.register("jogo_npc_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(JOGO_NPC, 0xE6400A, 0x2A1410, new Item.Properties()));
    /** 里梅（NPC）：敌对，玩家模型 + 冰凝咒法。 */
    public static final RegistryObject<EntityType<UraumeNpcEntity>> URAUME_NPC = ENTITIES.register("uraume_npc",
        () -> EntityType.Builder.of(UraumeNpcEntity::new, MobCategory.MONSTER).sized(0.6f, 1.8f).clientTrackingRange(16).updateInterval(1).build("sukuna:uraume_npc"));
    public static final RegistryObject<Item> URAUME_NPC_EGG = ITEMS.register("uraume_npc_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(URAUME_NPC, 0xE8EEF2, 0x6FA8C8, new Item.Properties()));
    /** 胀相：中立角色，会赤血操术。 */
    public static final RegistryObject<EntityType<ChosoEntity>> CHOSO=ENTITIES.register("choso",
        ()->EntityType.Builder.of(ChosoEntity::new,MobCategory.CREATURE).sized(0.6f,1.8f).clientTrackingRange(16).updateInterval(1).build("sukuna:choso"));
    public static final RegistryObject<Item> CHOSO_EGG=ITEMS.register("choso_spawn_egg",
        ()->new net.minecraftforge.common.ForgeSpawnEggItem(CHOSO,0x2A1030,0x8C1E3A,new Item.Properties()));
    /** 东堂葵：玩家模型 NPC，用「不义游戏」（注入咒力 + 换位）。皮肤等用户给图，先用默认 Steve。 */
    public static final RegistryObject<EntityType<TodoNpcEntity>> TODO_NPC=ENTITIES.register("todo_npc",
        ()->EntityType.Builder.of(TodoNpcEntity::new,MobCategory.CREATURE).sized(0.6f,1.8f).clientTrackingRange(16).updateInterval(1).build("sukuna:todo_npc"));
    public static final RegistryObject<Item> TODO_NPC_EGG=ITEMS.register("todo_npc_spawn_egg",
        ()->new net.minecraftforge.common.ForgeSpawnEggItem(TODO_NPC,0x2A2038,0xB58CFF,new Item.Properties()));
    /** 石流龙：玩家模型 NPC，用「咒力强化/放出」（光炮 / 追踪弹 / 喷涌 / 甜品）。皮肤等用户给图，先用默认 Steve。 */
    public static final RegistryObject<EntityType<RyuNpcEntity>> RYU_NPC=ENTITIES.register("ryu_npc",
        ()->EntityType.Builder.of(RyuNpcEntity::new,MobCategory.CREATURE).sized(0.6f,1.8f).clientTrackingRange(16).updateInterval(1).build("sukuna:ryu_npc"));
    public static final RegistryObject<Item> RYU_NPC_EGG=ITEMS.register("ryu_npc_spawn_egg",
        ()->new net.minecraftforge.common.ForgeSpawnEggItem(RYU_NPC,0x0A1016,0x9FDFFF,new Item.Properties()));
    /** 不义游戏：纯客户端特效被注入的一瞬间就地生成的锚点实体（不渲染、不可攻击）。 */
    public static final RegistryObject<EntityType<TodoAnchorEntity>> TODO_ANCHOR=ENTITIES.register("todo_anchor",
        ()->EntityType.Builder.of(TodoAnchorEntity::new,MobCategory.MISC).sized(0.25f,0.25f)
            .clientTrackingRange(10).updateInterval(20).build("sukuna:todo_anchor"));
    /** 咒具·游云：三节棍，伤害 18，右键横扫 10 格。 */
    public static final RegistryObject<Item> YUYUN=ITEMS.register("yuyun",()->new net.minecraft.world.item.SwordItem(
            net.minecraft.world.item.Tiers.IRON,15,-2.4f,new Item.Properties().stacksTo(1).rarity(Rarity.RARE)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            ItemStack stack=player.getItemInHand(hand);
            if(!level.isClientSide&&player instanceof ServerPlayer sp)yuyunSweep(sp);
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
    });
    /** 游云横扫：正前方 10 格内敌人，各 18 点伤害。 */
    public static void yuyunSweep(ServerPlayer sp){
        Vec3 look=sp.getLookAngle();
        Network.send(sp.serverLevel(),new Network.Effect(0,sp.getId(),sp.getEyePosition(),look,10,10,0));
        int hits=0;
        for(var target:sp.serverLevel().getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(10),e->e!=sp&&e.isAlive()&&!e.isSpectator()&&CombatSettings.canHit(sp,e))){
            if(target.position().distanceToSqr(sp.position())>100)continue;
            if(look.dot(target.position().add(0,target.getBbHeight()*0.5,0).subtract(sp.getEyePosition()).normalize())<0.15)continue;
            AttackTypes.mark(AttackTypes.MAGIC);
            try{CombatSettings.hurt(sp,target,sp.damageSources().playerAttack(sp),18f);}
            finally{AttackTypes.clear();}
            hits++;
        }
        sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(hits>0?"游云 · 抡中 "+hits+" 个目标":"游云 · 10 格内前方没有目标"));
        sp.serverLevel().playSound(null,sp.blockPosition(),net.minecraft.sounds.SoundEvents.TRIDENT_HIT,net.minecraft.sounds.SoundSource.PLAYERS,1.1f,1.1f);
    }

    /** 剑「屠坐魔」：基础伤害 5，对亡灵造成双倍伤害。 */
    public static final RegistryObject<Item> SLAUGHTER_DEMON=ITEMS.register("slaughter_demon",()->new net.minecraft.world.item.SwordItem(
            net.minecraft.world.item.Tiers.IRON,2,-2.4f,new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    /** 屠坐魔：手持此剑攻击亡灵时伤害翻倍。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void slaughterDemonUndead(net.minecraftforge.event.entity.living.LivingDamageEvent e){
        if(!(e.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer p))return;
        if(!(p.getMainHandItem().is(SLAUGHTER_DEMON.get())))return;
        if(e.getEntity().getMobType()!=net.minecraft.world.entity.MobType.UNDEAD)return;
        e.setAmount(e.getAmount()*2f);
    }
    /** 咒具第一条分支：黑绳。左键甩出，抽打 10 格内正前方的敌人。 */
    public static final RegistryObject<Item> BLACK_ROPE=ITEMS.register("black_rope",()->new net.minecraft.world.item.SwordItem(
            net.minecraft.world.item.Tiers.NETHERITE,5,-2.4f,new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            if(!level.isClientSide&&player instanceof ServerPlayer sp)blackRopeSweep(sp);
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        }
    });
    /** 黑绳横扫：10 格内正前方的敌人，10 点伤害、僵直 0.1 秒、抹除术式，并不需要领域展延就能命中五条悟。 */
    public static void blackRopeSweep(ServerPlayer sp){
        Vec3 look=sp.getLookAngle();
        Network.send(sp.serverLevel(),new Network.Effect(265,sp.getId(),sp.getEyePosition(),look,10,12,0));
        int hits=0;
        for(var target:sp.serverLevel().getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(10),e->e!=sp&&e.isAlive()&&!e.isSpectator()&&CombatSettings.canHit(sp,e))){
            if(target.position().distanceToSqr(sp.position())>100)continue;
            if(look.dot(target.position().add(0,target.getBbHeight()*0.5,0).subtract(sp.getEyePosition()).normalize())<0.2)continue;
            boolean amplified=false;
            if(target instanceof GojoEntity gojo){
                amplified=!gojo.getPersistentData().getBoolean("GojoAmplified");
                gojo.getPersistentData().putBoolean("GojoAmplified",true);
            }
            AttackTypes.mark(AttackTypes.MAGIC);
            // 视为武器：基础 10 + 玩家攻击力加成（力量、属性、附魔等都能吃到）
            float dmg=10f+Math.max(0f,(float)sp.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)-1f);
            try{CombatSettings.hurt(sp,target,sp.damageSources().playerAttack(sp),dmg);}
            finally{
                AttackTypes.clear();
                if(amplified&&target instanceof GojoEntity gojo)gojo.getPersistentData().remove("GojoAmplified");
            }
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,2,10,false,false));
            target.setDeltaMovement(0,Math.min(0,target.getDeltaMovement().y),0);
            target.hurtMarked=true;
            clearTechniques(target);
            hits++;
        }
        sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(hits>0?"黑绳 · 抽中了 "+hits+" 个目标（僵直+术式抹除）":"黑绳 · 10 格内前方没有目标"));
        sp.serverLevel().playSound(null,sp.blockPosition(),net.minecraft.sounds.SoundEvents.TRIDENT_HIT,net.minecraft.sounds.SoundSource.PLAYERS,1.2f,0.8f);
    }

    /** 作弊道具：仅创造模式可得，右键立刻完成当前角色章节（漏壶按章节旗标推进，其他角色按击杀数推进）。 */
    public static final RegistryObject<Item> TRANSFIGURED_BODY=ITEMS.register("transfigured_body",
        ()->new character.mahito.TransfiguredItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)){
            @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
                    net.minecraft.world.entity.player.Player p, net.minecraft.world.InteractionHand hand){
                if(level.isClientSide) net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT, ()->()->character.mahito.TransfigureScreen.open(1));
                return net.minecraft.world.InteractionResultHolder.success(p.getItemInHand(hand));
            }
        });
    /** 无为转变工具：4 种工具 × 6 个材质，材质由贮存生物血量决定。 */
    public static final String[] TRANSFIGURE_TIERS={"wood","stone","gold","iron","diamond","netherite"};
    public static final net.minecraft.world.item.Tier[] TRANSFIGURE_TIER_OBJ={
        net.minecraft.world.item.Tiers.WOOD, net.minecraft.world.item.Tiers.STONE,
        net.minecraft.world.item.Tiers.GOLD, net.minecraft.world.item.Tiers.IRON,
        net.minecraft.world.item.Tiers.DIAMOND, net.minecraft.world.item.Tiers.NETHERITE};
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] TRANSFIGURED_AXES=new RegistryObject[6];
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] TRANSFIGURED_PICKAXES=new RegistryObject[6];
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] TRANSFIGURED_HOES=new RegistryObject[6];
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] TRANSFIGURED_SHOVELS=new RegistryObject[6];
    static {
        for(int i=0;i<6;i++){
            final int k=i;
            TRANSFIGURED_AXES[i]=ITEMS.register("transfigured_axe_"+TRANSFIGURE_TIERS[i],
                ()->new net.minecraft.world.item.AxeItem(TRANSFIGURE_TIER_OBJ[k],6.0f,-3.0f,new Item.Properties().rarity(Rarity.EPIC)));
            TRANSFIGURED_PICKAXES[i]=ITEMS.register("transfigured_pickaxe_"+TRANSFIGURE_TIERS[i],
                ()->new net.minecraft.world.item.PickaxeItem(TRANSFIGURE_TIER_OBJ[k],1,-2.8f,new Item.Properties().rarity(Rarity.EPIC)));
            TRANSFIGURED_HOES[i]=ITEMS.register("transfigured_hoe_"+TRANSFIGURE_TIERS[i],
                ()->new net.minecraft.world.item.HoeItem(TRANSFIGURE_TIER_OBJ[k],-2,-3.0f,new Item.Properties().rarity(Rarity.EPIC)));
            TRANSFIGURED_SHOVELS[i]=ITEMS.register("transfigured_shovel_"+TRANSFIGURE_TIERS[i],
                ()->new net.minecraft.world.item.ShovelItem(TRANSFIGURE_TIER_OBJ[k],1.5f,-3.0f,new Item.Properties().rarity(Rarity.EPIC)));
        }
    }
    /** 日车宽见的法槌：不可丢弃、无限耐久。 */
    public static final RegistryObject<Item> GAVEL=ITEMS.register("gavel",
        ()->new character.higuruma.GavelItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> TRANSFIGURED_SWORD=ITEMS.register("transfigured_sword",
        ()->new character.mahito.TransfiguredSwordItem(net.minecraft.world.item.Tiers.NETHERITE,5,-2.4f,
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CHAPTER_CHEAT=ITEMS.register("chapter_cheat",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            ItemStack stack=player.getItemInHand(hand);
            if(!level.isClientSide&&player instanceof ServerPlayer sp){
                var d=character.CharacterService.selected(sp);
                boolean done=false;
                if(d==null){
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("作弊：还没有选择角色，先吃《咒术选择书》"));
                }else if(d.id().equals("jogo")){
                    done=character.jogo.JogoCombat.completeNextChapter(sp);
                }else if(d.hasEvents()){
                    // 乙骨这类事件型个人线：直接写入下一章的事件标记（story 里的击杀数对它无效）。
                    int eRank=character.CharacterService.rank(sp);
                    if(eRank<d.events().size()){
                        character.CharacterService.markChapter(sp,d.events().get(eRank).id());
                        done=true;
                    }
                }else{
                    int rank=character.CharacterService.rank(sp);
                    var story=d.story();
                    if(rank<story.size()){
                        Curse.data(sp).putInt("CharacterKills",story.get(rank).kills());
                        character.CharacterRewards.grant(sp);
                        sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("作弊："+story.get(rank).title()+" 已跳过"));
                        done=true;
                    }
                }
                if(!done&&d!=null)sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("作弊：当前角色章节已经全部完成"));
            }
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
    });
    public static final RegistryObject<Item> BEYOND_FRAGMENT=ITEMS.register("beyond_fragment",()->new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()){
        /** 在虚空竞技场对它右键：开启第三阶段（场地坍塌 → 纯白空间 → 观测者）。 */
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){
            if(p instanceof ServerPlayer sp)BeyondRift.activateFragment(sp,hand);
            return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);
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
    /** 咒灵操术之符：右键召唤夏油杰（正常游玩入口）。 */
    public static final RegistryObject<Item> SUMMON_TALISMAN=ITEMS.register("curse_summon_talisman",
        ()->new SummonTalismanItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    /** 领域内的坟头（插着鬼妖村正，右键拔出）。 */
    public static final net.minecraftforge.registries.DeferredRegister<net.minecraft.world.level.block.Block> YUTA_BLOCKS=
        net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.BLOCKS,"sukuna");
    public static final RegistryObject<net.minecraft.world.level.block.Block> YUTA_GRAVE=YUTA_BLOCKS.register("yuta_grave",
        ()->new character.yuta.YutaGraveBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            .noCollission().strength(0.4f).noOcclusion().lightLevel(st->7)));
    public static final net.minecraftforge.registries.DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> YUTA_BES=
        net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES,"sukuna");
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<character.yuta.YutaGraveBlockEntity>> YUTA_GRAVE_BE=
        YUTA_BES.register("yuta_grave",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder
            .of(character.yuta.YutaGraveBlockEntity::new,YUTA_GRAVE.get()).build(null));
    public static final RegistryObject<Item> YUTA_GRAVE_ITEM=ITEMS.register("yuta_grave",
        ()->new net.minecraft.world.item.BlockItem(YUTA_GRAVE.get(),new Item.Properties()));

    /** 领域方块（石流龙打上花火的内壳材料）：六个面显示同一张图，image 0~7 切不同外观。 */
    public static final RegistryObject<net.minecraft.world.level.block.Block> DOMAIN_SCREEN =
        YUTA_BLOCKS.register("domain_screen", () -> new DomainScreenBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .strength(-1.0f, 3600000.0f).noOcclusion().lightLevel(st -> 0)));
    public static final RegistryObject<Item> DOMAIN_SCREEN_ITEM = ITEMS.register("domain_screen",
        () -> new DomainScreenItem(DOMAIN_SCREEN.get(), new Item.Properties()));

    /** 鬼妖村正：领域内从坟头拔出的刀（左键 20 无视护甲，右键放复制术式×3 后报废）。 */
    public static final RegistryObject<Item> YUTA_KATANA=ITEMS.register("yuta_katana",()->new character.yuta.YutaKatanaItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> GETO_EGG=ITEMS.register("geto_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(GETO,0x2b2b3d,0x6b7fd7,new Item.Properties()));
    public static final RegistryObject<Item> LOW_CURSE_EGG=ITEMS.register("low_curse_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(LOW_CURSE,0x141414,0x4a4a55,new Item.Properties()));
    public static void init(IEventBus bus){ITEMS.register(bus);ENTITIES.register(bus);YUTA_BLOCKS.register(bus);YUTA_BES.register(bus);bus.addListener(Content::attributes);}
    private static void attributes(EntityAttributeCreationEvent e){e.put(DAITIAN.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,200).add(Attributes.MOVEMENT_SPEED,0.26).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.FOLLOW_RANGE,32).add(Attributes.KNOCKBACK_RESISTANCE,0.4).build());e.put(BUDDHA.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,5000).add(Attributes.MOVEMENT_SPEED,0.24).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.FOLLOW_RANGE,64).add(Attributes.KNOCKBACK_RESISTANCE,0.9).build());e.put(PARASITE.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.MOVEMENT_SPEED,0.32).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.FOLLOW_RANGE,48).build());e.put(MOTHER.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,160).add(Attributes.MOVEMENT_SPEED,0.23).add(Attributes.ATTACK_DAMAGE,12).add(Attributes.FOLLOW_RANGE,48).add(Attributes.KNOCKBACK_RESISTANCE,0.8).build());e.put(BROKEN_MAHITO.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20000).add(Attributes.MOVEMENT_SPEED,0.34).add(Attributes.ATTACK_DAMAGE,40).add(Attributes.FOLLOW_RANGE,64).add(Attributes.ARMOR,20).add(Attributes.KNOCKBACK_RESISTANCE,0.55).build());e.put(MAHITO_LIMB.get(),MahitoLimbEntity.createAttributes());e.put(CHOSO.get(),ChosoEntity.createAttributes().build());e.put(TODO_NPC.get(),TodoNpcEntity.createAttributes().build());e.put(RYU_NPC.get(),RyuNpcEntity.createAttributes().build());e.put(URAUME_NPC.get(),UraumeNpcEntity.createAttributes().build());e.put(JOGO_NPC.get(),JogoNpcEntity.createAttributes().build());e.put(MAHITO_CLONE.get(),character.mahito.MahitoCloneEntity.createAttributes().build());e.put(HIGURUMA_JUDGE.get(),character.higuruma.HigurumaJudgeEntity.createAttributes().build());e.put(YIDUI.get(),YiduiShouEntity.createAttributes().build());e.put(GETO.get(),GetoEntity.createAttributes().build());e.put(LOW_CURSE.get(),LowCurseEntity.createAttributes().build());e.put(RIKA.get(),RikaEntity.createAttributes().build());e.put(OBSERVER.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.FOLLOW_RANGE,64).add(Attributes.KNOCKBACK_RESISTANCE,1).build());}
    private static void tabs(BuildCreativeModeTabContentsEvent e){
        // 无为转变的产物（贮存体/剑/24 件工具）只能由技能生成，已全部不 accept，创造栏里不会再出现
        if(e.getTabKey()==CreativeModeTabs.COMBAT){e.accept(FINGER.get());e.accept(BODY.get());e.accept(ORB.get());e.accept(RELIC.get());e.accept(SEAL.get());e.accept(INCENSE.get());e.accept(EGG.get());e.accept(IDLE_TRANSFIGURATION_CURSE.get());e.accept(MAHITO_FINGER.get());e.accept(YUJI_FAMILY_PHOTO.get());e.accept(CHAPTER_CHEAT.get());e.accept(BLACK_ROPE.get());e.accept(SLAUGHTER_DEMON.get());e.accept(YUYUN.get());e.accept(CHOSO_EGG.get());e.accept(TODO_NPC_EGG.get());e.accept(RYU_NPC_EGG.get());e.accept(DOMAIN_SCREEN_ITEM.get());e.accept(URAUME_NPC_EGG.get());e.accept(JOGO_NPC_EGG.get());e.accept(SOUL_SEPARATION_NEEDLE.get());e.accept(EternalLove.TRUE_FALSE.get());}}

    private static class GrowthFood extends Item {
        final boolean body;GrowthFood(boolean body){super(new Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant().food(new FoodProperties.Builder().nutrition(1).saturationMod(0).alwaysEat().build()));this.body=body;}
        private boolean allowed(Player p){return character.CharacterService.permits(p,character.CharacterDefinition.Family.SHRINE)&&!(Uraume.unlocked(p)&&!Journey.unlocked(p))&&(body?Curse.fingers(p)>=20&&!Curse.awakened(p):Curse.fingers(p)<20);}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){if(!level.isClientSide&&!allowed(p)){p.displayClientMessage(Component.literal(!character.CharacterService.permits(p,character.CharacterDefinition.Family.SHRINE)?"请先用选择书切回宿傩，才能吸收宿傩成长物品":Uraume.unlocked(p)&&!Journey.unlocked(p)?"你已选择里梅，不能吸收宿傩成长物品":body?(Curse.awakened(p)?"已经融合过即身佛肉体":"先吞下20根宿傩手指，才能承受肉体融合"):"已经吸收20根手指，无法继续增加"),true);return InteractionResultHolder.fail(p.getItemInHand(hand));}return super.use(level,p,hand);}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){if(user instanceof ServerPlayer p){if(!allowed(p))return stack;Curse.consume(p,body);}return super.finishUsingItem(stack,level,user);}
    }
    private static void clearTechniques(net.minecraft.world.entity.LivingEntity target){
        if(target instanceof ServerPlayer sp){
            try{Skills.cancelCharge(sp);}catch(Throwable ignored){}
            try{Skills.closeDomain(sp);}catch(Throwable ignored){}
            try{GojoCombat.closeDomain(sp);}catch(Throwable ignored){}
            try{GuardianPlayerSkills.closeOwnedDomain(sp,false);}catch(Throwable ignored){}
            try{BloodDomain.close(sp);}catch(Throwable ignored){}
            try{character.jogo.JogoCombat.closeDomain(sp);}catch(Throwable ignored){}
        }else if(target instanceof GojoEntity gojo){
            try{gojo.clashCloseDomain();}catch(Throwable ignored){}
        }
    }
}
