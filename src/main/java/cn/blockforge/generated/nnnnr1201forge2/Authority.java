package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/** 通关奖励与永久权限；不向未通关者泄露路线。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class Authority {
    public static final RegistryObject<Item> PERMISSION=Content.ITEMS.register("permission",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().food(new FoodProperties.Builder().nutrition(0).saturationMod(0).alwaysEat().build())){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){return unlocked(p)?InteractionResultHolder.fail(p.getItemInHand(h)):super.use(l,p,h);}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity user){if(user instanceof ServerPlayer p){if(unlocked(p))return stack;unlock(p);}return super.finishUsingItem(stack,l,user);}
    });
    public static final RegistryObject<Item> CURSE=Content.ITEMS.register("authority_curse",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)){
        @Override public InteractionResult interactLivingEntity(ItemStack stack,Player p,LivingEntity target,InteractionHand hand){if(p instanceof ServerPlayer sp)Legion.recruit(sp,target);return InteractionResult.sidedSuccess(p.level().isClientSide);}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(p instanceof ServerPlayer sp&&unlocked(p)&&p.isShiftKeyDown())Legion.clearTargets(sp);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static final RegistryObject<Item> MARKER=Content.ITEMS.register("authority_marker",AuthorityMarkerItem::new);
    public static final RegistryObject<Item> BANNER=Content.ITEMS.register("soul_banner",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(p instanceof ServerPlayer sp&&unlocked(p)&&!p.getCooldowns().isOnCooldown(this)){Legion.toggleStorage(sp);p.getCooldowns().addCooldown(this,10);}return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static final RegistryObject<Item> EDITOR=Content.ITEMS.register("world_authority_editor",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()){
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(p instanceof ServerPlayer sp&&unlocked(p))AuthorityPackets.open(sp);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
    });
    public static final RegistryObject<Item> MEDIUM=Content.ITEMS.register("technique_medium",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)){
        @Override public Component getName(ItemStack stack){int id=medium(stack);return Component.literal("术式媒介 · "+AuthoritySkills.name(id));}
    });
    public static void init(){} // 在 Content.ITEMS 注册前触发字段初始化。
    public static boolean hidden(Item i){return i==PERMISSION.get()||i==CURSE.get()||i==MARKER.get()||i==BANNER.get()||i==EDITOR.get()||i==MEDIUM.get();}
    public static boolean unlocked(Player p){return !character.CharacterService.toji(p)&&Curse.data(p).getBoolean("WorldAuthority");}
    public static boolean instant(Player p){return unlocked(p)||p instanceof MaidBridge.Actor a&&owner(a)!=null;}
    public static boolean countedAsMaid(Player p){return MaidBridge.isCaster(p)&&damageOwner(p)==null;}
    public static ServerPlayer owner(MaidBridge.Actor a){var tag=a.maid.getPersistentData();if(tag.hasUUID("AuthorityEchoOwner"))return a.server.getPlayerList().getPlayer(tag.getUUID("AuthorityEchoOwner"));ServerPlayer p=Legion.owner(a.maid);return p!=null&&unlocked(p)?p:null;}
    public static ServerPlayer pool(Player p){if(p instanceof MaidBridge.Actor a){ServerPlayer owner=owner(a);if(owner==null&&MaidBridge.isMaid(a.maid)){owner=MaidBridge.owner(a);if(owner!=null&&!unlocked(owner))owner=null;}return owner;}return null;}
    public static ServerPlayer damageOwner(Player p){return p instanceof MaidBridge.Actor a?owner(a):null;}
    public static double damageScale(Player p){if(!(p instanceof MaidBridge.Actor a)||owner(a)==null)return 1;return a.maid.getPersistentData().hasUUID("AuthorityEchoOwner")?a.maid.getPersistentData().getDouble("AuthorityEchoScale"):AuthorityRules.LEGION_DAMAGE;}
    public static boolean echo(LivingEntity e){return e!=null&&e.getPersistentData().hasUUID("AuthorityEchoOwner");}
    public static int medium(ItemStack stack){return stack.is(MEDIUM.get())&&stack.hasTag()&&stack.getTag().contains("AuthoritySkill")?stack.getTag().getInt("AuthoritySkill"):-1;}
    public static boolean editorHeld(Player p){return p.getMainHandItem().is(EDITOR.get())||p.getOffhandItem().is(EDITOR.get());}
    /** 高位通关奖励：击碎当刻就放进背包；背包满则每秒重试，绝不把奖励丢在高位回廊。 */
    public static void reward(ServerPlayer p){
        if(p==null||p.isRemoved())return;
        Curse.data(p).putBoolean("ObserverCleared",true);
        settleReward(p);
    }
    /**
     * 权限物品的交付状态机。只有真正落进背包才算「已交付」，
     * 因此旧存档里背包满而被丢回虚空、或结算没跑完的通关，都会在下一次巡检时补回。
     */
    public static boolean settleReward(ServerPlayer p){
        CompoundTag d=Curse.data(p);
        boolean has=p.getInventory().countItem(PERMISSION.get())>0;
        if(AuthorityRewardRules.shouldMarkDelivered(unlocked(p),has)){
            if(!d.getBoolean("AuthorityRewardDelivered")){d.putBoolean("AuthorityRewardDelivered",true);d.putBoolean("AuthorityRewardGiven",true);}
            return true;
        }
        if(!AuthorityRewardRules.shouldPlace(d.getBoolean("ObserverCleared"),false,d.getBoolean("AuthorityRewardDelivered"),has))return false;
        if(!p.isAlive()||p.isSpectator())return false; // 死亡或旁观时装不下，等下一轮
        ItemStack give=new ItemStack(PERMISSION.get());
        if(!p.getInventory().add(give))return false;   // 背包满：保持未交付，下一秒继续补
        d.putBoolean("AuthorityRewardDelivered",true);d.putBoolean("AuthorityRewardGiven",true);
        p.displayClientMessage(Component.literal("高位战奖励「权限」已放入背包：吃下它"),true);
        Curse.sync(p);
        return true;
    }
    /** 已通关但「权限」确实丢了（被虚空缝隙吞掉、误删）时的补救；不会重复发放。 */
    public static boolean restoreReward(ServerPlayer p){
        CompoundTag d=Curse.data(p);
        if(unlocked(p)){p.displayClientMessage(Component.literal("权限已经吃下，越权者不需要第二份"),true);return false;}
        if(!d.getBoolean("ObserverCleared")&&d.getBoolean("ObserverShattered"))d.putBoolean("ObserverCleared",true);
        if(!d.getBoolean("ObserverCleared")){p.displayClientMessage(Component.literal("还没有击碎高位，没有可补发的权限"),true);return false;}
        if(p.getInventory().countItem(PERMISSION.get())>0){p.displayClientMessage(Component.literal("「权限」就在背包里：按住右键吃下"),true);return true;}
        d.putBoolean("AuthorityRewardDelivered",false);d.putBoolean("AuthorityRewardGiven",false);
        boolean ok=settleReward(p);
        p.displayClientMessage(Component.literal(ok?"「权限」已放入背包。":"背包已满，腾空一格后会自动放入，不会掉进高位回廊。"),true);
        return ok;
    }
    private static void unlock(ServerPlayer p){
        remember(p);Curse.data(p).putBoolean("WorldAuthority",true);Curse.add(p,Curse.maximum(p));
        Journey.give(p,new ItemStack(CURSE.get()));Journey.give(p,new ItemStack(BANNER.get()));upgradeFragment(p);settleMarker(p);
        if(!p.getInventory().contains(new ItemStack(EDITOR.get())))Journey.give(p,new ItemStack(EDITOR.get()));
        var advancement=p.server.getAdvancements().getAdvancement(new ResourceLocation("sukuna","transgressor"));
        if(advancement!=null)for(String criterion:advancement.getCriteria().keySet())p.getAdvancements().award(advancement,criterion);
        Curse.sync(p);AuthorityPackets.sync(p);
    }
    private static void upgradeFragment(ServerPlayer p){for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(Content.BEYOND_FRAGMENT.get()))p.getInventory().setItem(i,new ItemStack(EDITOR.get()));}
    /** 给新旧权限角色各补一份；背包满时保留待领取状态，不掉在地上也不覆盖物品。 */
    public static void settleMarker(ServerPlayer p){
        if(!unlocked(p)||p instanceof net.minecraftforge.common.util.FakePlayer)return;
        CompoundTag d=Curse.data(p);
        if(p.getInventory().countItem(MARKER.get())>0){d.putBoolean("AuthorityMarkerDelivered",true);return;}
        if(d.getBoolean("AuthorityMarkerDelivered")||!p.isAlive()||p.isSpectator()||p.getInventory().getFreeSlot()<0)return;
        if(p.getInventory().add(new ItemStack(MARKER.get()))){d.putBoolean("AuthorityMarkerDelivered",true);p.inventoryMenu.broadcastChanges();}
    }
    public static void remember(ServerPlayer p){
        CompoundTag saved=Curse.data(p).getCompound("AuthorityHistory"),current=Curse.data(p);
        for(String k:List.of("Memory","Awakened","Nine","SoulShrine","SoulPowerDouble","VoidShrine","GojoMemory","UraumeMemory","PureLandVictory"))if(current.getBoolean(k))saved.putBoolean(k,true);
        for(String k:List.of("Fingers","SoulLayers","SoulFeasts","ResidueUpgrades","IceFish"))saved.putInt(k,Math.max(saved.getInt(k),current.getInt(k)));
        // 由技能目录保存历史技能编号，避免阶段标记命名差异导致遗忘。
        var skills=new java.util.TreeSet<Integer>();for(int id:saved.getIntArray("Skills"))skills.add(id);skills.addAll(AuthoritySkills.current(p));
        saved.putIntArray("Skills",skills.stream().mapToInt(Integer::intValue).toArray());Curse.data(p).put("AuthorityHistory",saved);
    }
    public static void inscribe(ServerPlayer p,int skill,int hand){
        if(!unlocked(p)||!editorHeld(p)||!AuthoritySkills.owned(p,skill)||hand<0||hand>1)return;
        ItemStack item=new ItemStack(MEDIUM.get());item.getOrCreateTag().putInt("AuthoritySkill",skill);
        InteractionHand slot=hand==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        ItemStack old=p.getItemInHand(slot).copy();p.setItemInHand(slot,item);if(!old.isEmpty())Journey.give(p,old);AuthorityPackets.sync(p);
    }
    @SubscribeEvent public static void automatic(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p)AuthoritySkills.tickAutomatic(p);}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&!(p instanceof MaidBridge.Actor)&&p.tickCount%AuthorityRewardRules.RETRY_TICKS==0){remember(p);if(unlocked(p)){upgradeFragment(p);settleMarker(p);}settleReward(p);AuthorityPackets.sync(p);}}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){if(Curse.data(p).getBoolean("ObserverCleared"))reward(p);AuthorityPackets.sync(p);}}
    private Authority(){}
}
