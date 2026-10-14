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
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

public final class VoidContent {
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,"sukuna");
    public static final RegistryObject<Codec<? extends ChunkGenerator>> ARENA=GENERATORS.register("void_arena",()->VoidArenaGenerator.CODEC);
    public static final RegistryObject<EntityType<GojoEntity>> GOJO=Content.ENTITIES.register("gojo",()->EntityType.Builder.of(GojoEntity::new,MobCategory.MONSTER).sized(0.6f,1.8f).fireImmune().clientTrackingRange(16).updateInterval(1).build("sukuna:gojo"));
    public static final RegistryObject<EntityType<MahoragaEntity>> MAHORAGA=Content.ENTITIES.register("mahoraga",()->EntityType.Builder.of(MahoragaEntity::new,MobCategory.MONSTER).sized(0.95f,3.6f).fireImmune().clientTrackingRange(16).updateInterval(1).build("sukuna:mahoraga"));
    public static final RegistryObject<Item> KEY=Content.ITEMS.register("void_key",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(p instanceof ServerPlayer sp)VoidJourney.enter(sp);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static final RegistryObject<Item> FRAGMENT=Content.ITEMS.register("gojo_fragment",()->new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){ItemStack stack=p.getItemInHand(h);if(!l.isClientSide&&GojoPlayer.consume(p)&&!p.isCreative())stack.shrink(1);return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}
    });
    /** 调伏仪式召唤物：右键在脚下展开仪式法阵并召出魔虚罗（无阶段要求）。 */
    public static final RegistryObject<Item> RITUAL=Content.ITEMS.register("mahoraga_ritual",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
            ItemStack stack=p.getItemInHand(h);
            if(!l.isClientSide&&p instanceof ServerPlayer sp){
                if(MahoragaRitual.summon(sp)){if(!sp.isCreative())stack.shrink(1);}
            }
            return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);
        }
    });
    /** 法轮：魔虚罗掉落物，同时也是**头盔**——戴在头上才会在头顶显示法轮模型。
     *  普通右键：直接戴到头上（原版防具逻辑）；潜行右键（含对着空气）：召唤/叫回友方魔虚罗
     *  （召唤后这件法轮会变成「法轮 · 收回」，所以召唤期间它不能戴在头上）；
     *  右键生物：把法轮戴到它头上。 */
    public static final RegistryObject<Item> MAHORAGA_WHEEL=Content.ITEMS.register("mahoraga_wheel",() -> new ArmorItem(MahoragaArmor.material(),ArmorItem.Type.HELMET,
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
            ItemStack stack=p.getItemInHand(h);
            if(!l.isClientSide&&p instanceof ServerPlayer sp&&sp.isShiftKeyDown()){
                // 潜行右键（含对着空气）：召唤/叫回**同一只**友方魔虚罗，继承已有适应、血量回满
                MahoragaAdaptStore.summonAlly(sp);
                return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);
            }
            // 潜行右键：不戴上（哪怕头盔是空的），只做召唤
            if(p.isShiftKeyDown())return InteractionResultHolder.pass(stack);
            // 普通右键：交给原版防具逻辑**直接戴到头上**（头盔为空时原版会自己装备）
            return super.use(l,p,h);
        }
        @Override public InteractionResult interactLivingEntity(ItemStack stack,Player p,LivingEntity target,InteractionHand h){
            if(p.isShiftKeyDown())return InteractionResult.PASS; // 潜行右键留给其他交互
            if(MahoragaRules.wheelOnHead(target))return InteractionResult.PASS; // 已经戴着
            if(p.level().isClientSide)return InteractionResult.SUCCESS;
            ItemStack worn=stack.copy();worn.setCount(1);worn.setDamageValue(0);
            ItemStack replaced=target.getItemBySlot(EquipmentSlot.HEAD).copy();
            target.setItemSlot(EquipmentSlot.HEAD,worn);
            stack.shrink(1);
            if(!replaced.isEmpty())p.getInventory().placeItemBackInInventory(replaced);
            if(p instanceof ServerPlayer sp)sp.displayClientMessage(Component.literal("法轮已戴在"+target.getDisplayName().getString()+"头上"),true);
            if(target instanceof Mob mob)mob.setPersistenceRequired();
            return InteractionResult.SUCCESS;
        }
        /** 返回一张**真实存在**的全透明防具贴图，避免日志里那条
         *  {@code Failed to load texture: sukuna:mahoraga_wheel}（材质名被当贴图路径去查了）。
         *  法轮的外形完全由 {@link MahoragaWheelRenderer} 画，原版防具层画不出东西。 */
        @Override public String getArmorTexture(ItemStack stack,net.minecraft.world.entity.Entity entity,net.minecraft.world.entity.EquipmentSlot slot,String type){
            return "sukuna:textures/models/armor/mahoraga_wheel_layer_"+(slot==net.minecraft.world.entity.EquipmentSlot.LEGS?2:1)+".png";
        }
    });
    /** 法轮 · 收回：召唤出友方魔虚罗后，手里的法轮会变成它；再右键就把那只召回来并变回法轮。 */
    public static final RegistryObject<Item> MAHORAGA_WHEEL_RECALL=Content.ITEMS.register("mahoraga_wheel_recall",() -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
            ItemStack stack=p.getItemInHand(h);
            if(!l.isClientSide&&p instanceof ServerPlayer sp)MahoragaAdaptStore.recallAlly(sp,h);
            return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);
        }
        @Override public void appendHoverText(ItemStack stack,Level l,List<Component> lines,TooltipFlag flags){
            lines.add(Component.literal("右键：召回场上的友方魔虚罗，并变回「法轮」").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("召唤期间法轮不存在，所以这段时间戴不到头上；收回后即变回法轮可继续佩戴").withStyle(ChatFormatting.GRAY));
        }
    });
    /** 武器「飞天」：伤害 20、范围 15 格；手持时斩击速度 −70%；右键把周围 10 格+面前扇形 40 格的敌人吹到斩击上。 */
    public static final RegistryObject<Item> HITEN=Content.ITEMS.register("hiten",() -> new net.minecraft.world.item.SwordItem(
        net.minecraft.world.item.Tiers.NETHERITE,(int)Hiten.DAMAGE-1,-2.2f, // 基础 1 + 19 = 20
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
            if(!l.isClientSide&&p instanceof ServerPlayer sp)Hiten.pull(sp);
            return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);
        }
        @Override public void appendHoverText(ItemStack stack,Level l,List<Component> lines,TooltipFlag flags){
            lines.add(Component.literal("近战 "+BalanceRules.number(Hiten.DAMAGE)+" 伤害 · 攻击范围 "+BalanceRules.number(Hiten.RANGE)+" 格").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("手持时所有斩击速度 −70%").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("右键：周围 "+BalanceRules.number(Hiten.AURA_RADIUS)+" 格 + 面前扇形 "+BalanceRules.number(Hiten.CONE_RANGE)
                +" 格的敌人被风直接吹到斩击上").withStyle(ChatFormatting.AQUA));
        }
    });
    /** 咒具「神武解」：左键 15 伤害；右键面前扇形 30 格内所有怪物各吃一道彩色雷击（15×20 原值）。 */
    public static final RegistryObject<Item> SHINBUKE=Content.ITEMS.register("shinbuke",() -> new net.minecraft.world.item.SwordItem(
        net.minecraft.world.item.Tiers.NETHERITE,(int)Shinbuke.MELEE_DAMAGE-1,-2.4f, // 基础 1 + 14 = 15
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
            if(!l.isClientSide&&p instanceof ServerPlayer sp)Shinbuke.strike(sp);
            return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);
        }
        @Override public void appendHoverText(ItemStack stack,Level l,List<Component> lines,TooltipFlag flags){
            lines.add(Component.literal("左键：近战 "+BalanceRules.number(Shinbuke.MELEE_DAMAGE)+" 伤害").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("右键：面前扇形 "+BalanceRules.number(Shinbuke.RANGE)+" 格内所有怪物各吃一道彩色雷击（"
                +BalanceRules.number(Shinbuke.MELEE_DAMAGE)+" × "+BalanceRules.number(Shinbuke.STRIKE_MULTIPLIER)+" = "
                +BalanceRules.number(BalanceRules.damage(Shinbuke.STRIKE_DAMAGE))+" 当前模式）").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("冷却 "+BalanceRules.number(Shinbuke.COOLDOWN/20)+" 秒 · 雷电伤害（魔虚罗会按「雷电」适应）").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("手持时无法使用术式 · 按住释放键充能 "+Shinbuke.CHARGE_PER_SECOND+"/秒").withStyle(ChatFormatting.DARK_PURPLE));
            lines.add(Component.literal("每 "+Shinbuke.CHARGE_STEP+" 点充能：范围 +"+BalanceRules.number(Shinbuke.RANGE_PER_STEP*100)+"%、伤害 +"
                +BalanceRules.number(Shinbuke.DAMAGE_PER_STEP*100)+"%，且本体近战永久 +"+BalanceRules.number(Shinbuke.MELEE_PERMANENT_PER_STEP*100)+"%").withStyle(ChatFormatting.DARK_PURPLE));
        }
    });
    public static final RegistryObject<Item> EGG=Content.ITEMS.register("gojo_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(GOJO,0x0b0b12,0x8fd0ff,new Item.Properties()));
    public static final RegistryObject<Item> MAHORAGA_EGG=Content.ITEMS.register("mahoraga_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(MAHORAGA,0x2b2118,0xffd24a,new Item.Properties()));
    public static final RegistryObject<Item> CONTROLLER=Content.ITEMS.register("void_controller",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(l.isClientSide)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->net.minecraft.client.Minecraft.getInstance().setScreen(new VoidControllerScreen()));else if(p instanceof ServerPlayer sp)Network.sendController(sp);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static void init(IEventBus bus){GENERATORS.register(bus);bus.addListener(VoidContent::attributes);}
    private static void attributes(EntityAttributeCreationEvent e){GojoEntity.attributes(e);MahoragaEntity.attributes(e);}
    private static void tabs(net.minecraftforge.event.BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.COMBAT){e.accept(KEY.get());e.accept(CONTROLLER.get());e.accept(EGG.get());e.accept(FRAGMENT.get());e.accept(RITUAL.get());e.accept(MAHORAGA_WHEEL.get());e.accept(MAHORAGA_WHEEL_RECALL.get());e.accept(MAHORAGA_EGG.get());e.accept(SHINBUKE.get());e.accept(HITEN.get());}}
}
