package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** 创造专用测试入口，不走正常治疗/修复熔断的资格和费用限制。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class CreativeRestorer extends Item {
    public CreativeRestorer(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(!player.isCreative()){if(!level.isClientSide)player.displayClientMessage(Component.literal("测试恢复符仅限创造模式使用"),true);return InteractionResultHolder.fail(stack);}
        if(player instanceof ServerPlayer p)restore(p);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("创造专用：右键回满生命、咒力、饥饿和灵魂层数"));
        lines.add(Component.literal("清除熔断、冷却、禁疗、负面效果，停止当前技能"));
        lines.add(Component.literal("保留成长、物品、敌友设置；不消耗道具"));
    }
    /** 定身时也可以使用测试道具，但绝不放开普通物品的交互。 */
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
    public static void interact(PlayerInteractEvent event){
        if(!(event instanceof PlayerInteractEvent.RightClickItem||event instanceof PlayerInteractEvent.RightClickBlock||event instanceof PlayerInteractEvent.EntityInteract||event instanceof PlayerInteractEvent.EntityInteractSpecific))return;
        if(!event.getEntity().isCreative()||!(event.getItemStack().getItem() instanceof CreativeRestorer))return;
        if(event.getEntity() instanceof ServerPlayer p)restore(p);
        event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);
    }
    static void restore(ServerPlayer p){
        if(!p.isCreative()||!p.isAlive())return;
        // 先关闭仍会扣费/施加状态的时间轴，最后再清熔断和回满咒力。
        SixfoldFinale.testReset(p);DomainClash.testReset(p);Skills.testReset(p);GuardianPlayerSkills.testReset(p);VoidCombat.testReset(p);
        VoidJourney.clear(p);BloodArts.exit(p);DeathStar.testReset(p);Curse.data(p).remove("BloodCooldowns");
        var progress=Curse.data(p);for(String key:List.copyOf(progress.getAllKeys()))if(RecoveryRules.progressKey(key))progress.remove(key);
        var data=p.getPersistentData();for(String key:List.copyOf(data.getAllKeys()))if(RecoveryRules.entityKey(key))data.remove(key);
        SoulCombat.testReset(p);
        p.removeAllEffects();p.clearFire();p.setTicksFrozen(0);p.setAirSupply(p.getMaxAirSupply());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);p.hurtMarked=true;
        Curse.apply(p);p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(0);
        p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(20);p.getFoodData().setExhaustion(0);
        progress.putDouble("Energy",Curse.maximum(p));
        for(Item item:ForgeRegistries.ITEMS)if(p.getCooldowns().isOnCooldown(item))p.getCooldowns().removeCooldown(item);
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new Network.TestReset());
        Curse.sync(p);p.displayClientMessage(Component.literal("测试恢复完成：生命、咒力已满；熔断、冷却与负面状态已清除，成长保留"),true);
    }
}
