package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.registries.RegistryObject;

/** 每位施术者单独保存挚爱名单，目标端镜像用于离线持续伤害保护。 */
public final class EternalLove {
    public static final RegistryObject<Item> ITEM=Content.ITEMS.register("eternal_love",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> TRUE_FALSE=Content.ITEMS.register("true_false_love",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    private static final String KEY="EternalLoved",OWNERS="SukunaLovedBy";
    public static boolean settling(LivingEntity maid){return maid.getPersistentData().getLong("SukunaDuelEndUntil")>maid.level().getGameTime();}
    public static boolean duel(LivingEntity maid){return net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid")&&MaidBridge.isMaid(maid)&&MaidDuel.duel(maid);}
    public static boolean protects(Player owner,LivingEntity target){return Curse.data(owner).getCompound(KEY).contains(target.getUUID().toString())||protects(owner.getUUID(),target);}
    public static boolean protects(UUID owner,LivingEntity target){return target.getPersistentData().getCompound(OWNERS).contains(owner.toString());}
    /** 是否被任何一位玩家选为永恒挚爱：无量空处这类"绝对控制"据此免疫（非玩家施法者也要认这条）。 */
    public static boolean protectedByAnyone(LivingEntity target){return !target.getPersistentData().getCompound(OWNERS).isEmpty();}
    public static void init(){}
    private static void mark(ServerPlayer p,LivingEntity target){
        CompoundTag saved=Curse.data(p).getCompound(KEY),owners=target.getPersistentData().getCompound(OWNERS);
        saved.putBoolean(target.getUUID().toString(),true);owners.putBoolean(p.getUUID().toString(),true);
        Curse.data(p).put(KEY,saved);target.getPersistentData().put(OWNERS,owners);Skills.protectAlly(p,target.getUUID());SoulCombat.protect(p,target);
        var data=target.getPersistentData();
        if(data.hasUUID("SukunaSoulFireOwner")&&data.getUUID("SukunaSoulFireOwner").equals(p.getUUID())){data.remove("SukunaSoulFire");data.remove("SukunaSoulFireOwner");target.clearFire();}
        if(data.hasUUID("SukunaSoulSlashOwner")&&data.getUUID("SukunaSoulSlashOwner").equals(p.getUUID()))data.remove("SukunaSoulSlashOwner");
        if(!data.getBoolean("SukunaSoulFire")&&!data.hasUUID("SukunaSoulSlashOwner")&&data.getLong("SukunaSoulPeelUntil")<=target.level().getGameTime())data.remove("SukunaNoHeal");
        p.displayClientMessage(Component.literal("永恒挚爱：你的所有术式都不会再伤害或锁定 "+target.getName().getString()),false);
    }
    private static void clear(ServerPlayer p,LivingEntity target){
        CompoundTag saved=Curse.data(p).getCompound(KEY);saved.remove(target.getUUID().toString());Curse.data(p).put(KEY,saved);
        CompoundTag owners=target.getPersistentData().getCompound(OWNERS);owners.remove(p.getUUID().toString());target.getPersistentData().put(OWNERS,owners);
        p.displayClientMessage(Component.literal("已解除对 "+target.getName().getString()+" 的永恒挚爱保护"),false);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.EntityInteract event){
        if(!event.getItemStack().is(ITEM.get())||!(event.getTarget() instanceof LivingEntity target))return;
        if(event.getEntity() instanceof ServerPlayer p){if(p.isShiftKeyDown())clear(p,target);else mark(p,target);}
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void prevent(LivingAttackEvent event){
        ServerPlayer p=CombatSettings.caster(event.getSource());if(p!=null&&protects(p,event.getEntity()))event.setCanceled(true);
    }
    private EternalLove(){}
}
