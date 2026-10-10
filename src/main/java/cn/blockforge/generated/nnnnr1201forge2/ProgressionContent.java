package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

public final class ProgressionContent {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"sukuna");
    private static final List<RegistryObject<Item>> ALL=new ArrayList<>();
    private static RegistryObject<Item> item(String id,java.util.function.Supplier<Item> factory){var r=Content.ITEMS.register(id,factory);ALL.add(r);return r;}
    public static final RegistryObject<Item> MEMORY=item("sukuna_memory",()->new UnlockFood(false));
    public static final RegistryObject<Item> NINE=item("nine_death_paintings",()->new UnlockFood(true));
    public static final List<RegistryObject<Item>> PAGES=new ArrayList<>();
    static {for(int i=0;i<9;i++){final int index=i;PAGES.add(item("page_"+Journey.IDS[i],()->new Item(new Item.Properties().rarity(Rarity.RARE)){
        @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){lines.add(Component.literal(Journey.CONDITIONS[index]));lines.add(Component.literal("九种咒胎各一份，在工作台合成咒胎九相图"));}
    }));}}
    public static final RegistryObject<Item> CORE=item("nether_core",()->new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    public static final RegistryObject<Item> FETUS=item("forbidden_fetus",()->new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){ItemStack stack=p.getItemInHand(h);if(p instanceof ServerPlayer sp){if(!Curse.awakened(p)){sp.sendSystemMessage(Component.literal("先吞噬即身佛肉体，才能凝聚咒胎祭坛"));return InteractionResultHolder.fail(stack);}if(p.getInventory().countItem(Items.NETHERITE_INGOT)<6||p.getInventory().countItem(CORE.get())<3){sp.sendSystemMessage(Component.literal("祭坛合成需要：禁忌之胎×1、下界合金锭×6、下界核心×3；材料放在背包后右键禁忌之胎"));return InteractionResultHolder.fail(stack);}take(p,Items.NETHERITE_INGOT,6);take(p,CORE.get(),3);stack.shrink(1);Journey.give(sp,new ItemStack(ALTAR_ITEM.get()));sp.sendSystemMessage(Component.literal("咒胎祭坛已凝聚。放置后右键查看任务，持咒胎右键可合成九相图。"));}return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}
        @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){lines.add(Component.literal("觉醒后右键：消耗本物品、6下界合金锭、3下界核心合成祭坛"));}
    });
    public static final RegistryObject<Block> TRANSFIGURED_FLESH=BLOCKS.register("idle_transfigured_flesh",()->new Block(BlockBehaviour.Properties.copy(Blocks.NETHER_WART_BLOCK).strength(4,12).sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<Item> TRANSFIGURED_FLESH_ITEM=item("idle_transfigured_flesh",()->new BlockItem(TRANSFIGURED_FLESH.get(),new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Block> ALTAR=BLOCKS.register("cursed_altar",()->new Block(BlockBehaviour.Properties.copy(Blocks.CRYING_OBSIDIAN).strength(6,1200).lightLevel(s->10)){
        @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(p instanceof ServerPlayer sp){if(!Curse.awakened(p)){sp.sendSystemMessage(Component.literal("咒胎祭坛只回应融合即身佛肉体的人"));return InteractionResult.SUCCESS;}if(Journey.nine(p)&&p.getItemInHand(hand).is(Content.ORB.get())&&SoulCombat.layers(p)<9){p.getItemInHand(hand).shrink(1);Curse.data(p).putInt("SoulLayers",SoulCombat.layers(p)+1);SoulCombat.sync(sp);sp.sendSystemMessage(Component.literal("祭坛重凝一层咒胎受肉："+SoulCombat.layers(p)+"/9"));return InteractionResult.SUCCESS;}boolean pages=true;for(var r:PAGES)if(p.getInventory().countItem(r.get())<1)pages=false;
            if(pages&&!Journey.nine(p)){for(var r:PAGES)take(p,r.get(),1);Journey.give(sp,new ItemStack(NINE.get()));sp.sendSystemMessage(Component.literal("九相归一，咒胎九相图已合成。长按右键吞噬。"));}else sp.sendSystemMessage(Component.literal(Journey.guide(p)));}return InteractionResult.sidedSuccess(level.isClientSide);}
    });
    public static final RegistryObject<Item> ALTAR_ITEM=item("cursed_altar",()->new BlockItem(ALTAR.get(),new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> DAITIAN_EGG=item("daitian_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(Content.DAITIAN,0x756981,0xbf733c,new Item.Properties()));
    private static void take(Player p,Item item,int n){for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack s=p.getInventory().getItem(i);if(s.is(item)){int amount=Math.min(n,s.getCount());s.shrink(amount);n-=amount;if(n==0)return;}}}
    public static void init(IEventBus bus){BLOCKS.register(bus);}
    private static void tabs(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.COMBAT)for(var r:ALL)e.accept(r.get());}
    private static final class UnlockFood extends Item {
        private final boolean nine;
        UnlockFood(boolean nine){super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).food(new FoodProperties.Builder().nutrition(0).saturationMod(0).alwaysEat().build()));this.nine=nine;}
        private boolean allowed(Player p){return nine?Curse.awakened(p)&&Curse.data(p).getBoolean("BuddhaDefeated")&&!Journey.nine(p):BranchRules.acceptMemory(Journey.unlocked(p),Uraume.unlocked(p),MaidBridge.isCaster(p),false);}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand h){if(!level.isClientSide&&!allowed(p)){p.displayClientMessage(Component.literal(nine?"需先完成即身佛阶段，且九相图只能吞噬一次":Uraume.unlocked(p)?"先吃里梅后，不能再吃宿傩记忆":"已经觉醒宿傩记忆"),true);return InteractionResultHolder.fail(p.getItemInHand(h));}return super.use(level,p,h);}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){if(entity instanceof ServerPlayer p){if(!allowed(p))return stack;float oldHealth=p.getMaxHealth();Curse.data(p).putBoolean(nine?"Nine":"Memory",true);Curse.apply(p);p.heal(p.getMaxHealth()-oldHealth);Curse.add(p,Curse.maximum(p)*0.4);Curse.sync(p);if(nine)Network.send(p.serverLevel(),new Network.Effect(60,p.getId(),p.position(),net.minecraft.world.phys.Vec3.ZERO,9,80,0));p.sendSystemMessage(Component.literal(nine?"咒胎九相图融合："+GrowthConfig.NINE.description()+"；联合攻击延迟1秒；按 V 开启九相轮转；特效升为九相终式":"宿傩记忆苏醒：长按 N 选术式，R 释放，J 查看成长任务"));}return super.finishUsingItem(stack,level,entity);}
    }
}
