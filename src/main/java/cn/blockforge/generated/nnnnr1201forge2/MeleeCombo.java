package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

/** 服务端只批准和广播动作，不调用攻击、不修改伤害、击退或玩家位置。 */
public final class MeleeCombo {
    public record Press(boolean cancel){}
    public record State(int owner,UUID player,int stage,int delay){}
    public static Consumer<State> receiver=s->{};
    private static final class Chain {
        final ServerPlayer player;
        final MeleeComboRules.Sequence sequence=new MeleeComboRules.Sequence();
        final Object level;
        Chain(ServerPlayer p){player=p;level=p.level();}
    }
    private static final Map<UUID,Chain> chains=new HashMap<>();
    public static void register(){
        Network.CHANNEL.messageBuilder(Press.class,36,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->b.writeBoolean(m.cancel())).decoder(b->new Press(b.readBoolean()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null){if(m.cancel())cancel(p);else press(p);}c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(State.class,37,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeVarInt(m.owner());b.writeUUID(m.player());b.writeVarInt(m.stage());b.writeVarInt(m.delay());})
            .decoder(b->new State(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readVarInt()))
            .consumerMainThread((m,c)->{receiver.accept(m);c.get().setPacketHandled(true);}).add();
    }
    private static boolean allowed(ServerPlayer p){
        // 只要用过《咒术选择书》（身上有选中角色）就能用连击动作，不再限定宿傩/甚尔/虎杖
        return (Journey.unlocked(p)||character.CharacterService.toji(p)||character.CharacterService.yuji(p)||character.CharacterService.selected(p)!=null)&&p.isAlive()&&!p.isRemoved()&&!p.hasDisconnected()&&!p.isSpectator()
            &&!p.isUsingItem()&&!p.isPassenger()&&!p.isSleeping()&&!p.isFallFlying()&&!p.isSwimming()
            &&!DismantleMotion.active(p)&&!SixfoldFinale.active(p)&&!SixfoldFinale.controlled(p)
            &&!DomainClash.active(p)&&!NetBranches.locked(p)&&!Uraume.locked(p)&&!CombatExpansion.controlled(p)
            &&!GuardianPlayerSkills.locked(p)&&!SoulCombat.suppressed(p)&&!GojoEntity.mindLocked(p)
            &&!ObserverRitual.active(p)&&!TenShadows.locked(p)&&!Skills.sealing(p)&&!SimpleDomain.owns(p)
            &&!FurnaceMax.active(p)&&!EternalLove.settling(p);
    }
    private static void press(ServerPlayer p){
        if(!allowed(p)){cancel(p);return;}
        Chain chain=chains.get(p.getUUID());
        if(chain!=null&&(chain.player!=p||chain.level!=p.level())){cancel(p);chain=null;}
        if(chain==null){chain=new Chain(p);chains.put(p.getUUID(),chain);}
        long now=p.level().getGameTime();var step=chain.sequence.press(now);if(step==null)return;
        send(p,new State(p.getId(),p.getUUID(),step.stage(),(int)(step.start()-now)));
    }
    private static void send(ServerPlayer p,State s){Network.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),s);}
    public static void cancel(ServerPlayer p){if(chains.remove(p.getUUID())!=null)send(p,new State(p.getId(),p.getUUID(),0,0));}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(Chain chain:List.copyOf(chains.values())){
            ServerPlayer p=chain.player;
            if(chain.level!=p.level()||!allowed(p)||chain.sequence.expired(p.level().getGameTime()))cancel(p);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hurt(LivingDamageEvent e){if(e.getAmount()>0&&e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){chains.clear();}
    private MeleeCombo(){}
}
