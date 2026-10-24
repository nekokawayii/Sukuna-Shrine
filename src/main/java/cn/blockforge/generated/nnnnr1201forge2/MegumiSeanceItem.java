package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="sukuna")
public final class MegumiSeanceItem extends Item {
    public MegumiSeanceItem(Properties properties){super(properties);}
    // 村民交易先于 Item.interactLivingEntity，必须在实体交互事件拦截才能真正降灵。
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof Villager villager)||!event.getItemStack().is(ShadowContent.SEANCE.get()))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(!(event.getEntity() instanceof ServerPlayer p)||event.getHand()!=InteractionHand.MAIN_HAND)return;
        if(villager.isBaby()){p.displayClientMessage(Component.literal("请对成年村民使用降灵"),true);return;}
        if(p.level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){p.displayClientMessage(Component.literal("请先改为简单、普通或困难，再开始伏黑惠挑战"),true);return;}
        if(!p.isAlive()||p.isSpectator())return;
        MegumiEntity megumi=SoulContent.MEGUMI.get().create(p.serverLevel());if(megumi==null)return;
        megumi.configure(p);megumi.moveTo(villager.getX(),villager.getY(),villager.getZ(),villager.getYRot(),0);
        if(!p.serverLevel().noCollision(megumi)||!p.serverLevel().addFreshEntity(megumi)){p.displayClientMessage(Component.literal("空间不足，无法降灵"),true);return;}
        villager.discard();if(!p.isCreative())event.getItemStack().shrink(1);
        p.sendSystemMessage(Component.literal("降灵完成 · 伏黑惠暂时保持中立，空手右键或攻击他开始首领战"));
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){lines.add(Component.literal("右键成年村民：转化为中立伏黑惠"));lines.add(Component.literal("再空手右键或攻击伏黑惠开始挑战"));lines.add(Component.literal("击败后右键倒地肉身，觉醒十种影法术"));}
}
