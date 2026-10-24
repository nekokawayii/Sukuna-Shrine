package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** 标记只由手持道具的右键触发；普通攻击不参与军团选目标。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class AuthorityMarkerItem extends Item {
    public AuthorityMarkerItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());}

    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer p)mark(p,hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }

    /** 同时接住空气、方块、普通生物和有专用交互的生物，避免标记时误开门、交易或骑乘。 */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent event){
        if(!(event instanceof PlayerInteractEvent.RightClickItem||event instanceof PlayerInteractEvent.RightClickBlock
            ||event instanceof PlayerInteractEvent.EntityInteract||event instanceof PlayerInteractEvent.EntityInteractSpecific))return;
        if(!event.getItemStack().is(Authority.MARKER.get()))return;
        if(event.getEntity() instanceof ServerPlayer p)mark(p,event.getHand());
        event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);
    }

    private static void mark(ServerPlayer p,InteractionHand hand){
        if(!AuthorityRules.canMark(Authority.unlocked(p),p.getItemInHand(hand).is(Authority.MARKER.get()))
            ||!p.isAlive()||p.isSpectator()||ObserverRitual.active(p)||p.getCooldowns().isOnCooldown(Authority.MARKER.get()))return;
        // 两手与不同右键事件共用该玩家的冷却，不会一次点击选中后又立刻取消。
        p.getCooldowns().addCooldown(Authority.MARKER.get(),5);
        if(p.isShiftKeyDown()){Legion.clearTargets(p);return;}
        LivingEntity target=aimed(p);
        if(target!=null)Legion.select(p,target);
    }

    private static LivingEntity aimed(ServerPlayer p){
        Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(96));
        var block=p.serverLevel().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
        LivingEntity nearest=null;double best=eye.distanceToSqr(end);
        for(LivingEntity target:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(eye,end).inflate(1),
            t->t!=p&&t.isAlive()&&!t.isInvisible()&&!t.isSpectator())){
            var hit=target.getBoundingBox().inflate(.1).clip(eye,end);
            if(hit.isEmpty())continue;
            double distance=eye.distanceToSqr(hit.get());
            if(distance<best&&p.hasLineOfSight(target)){best=distance;nearest=target;}
        }
        return nearest;
    }
}
