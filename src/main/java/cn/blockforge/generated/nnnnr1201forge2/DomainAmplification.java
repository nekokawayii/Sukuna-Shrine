package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 领域展延：持续消耗咒力，把简易领域覆在身体表面，使术式可以穿透五条悟的无下限。 */
public final class DomainAmplification {
    public static final double COST_PER_SECOND=40;
    private static final int COST_PER_TICK=(int)(COST_PER_SECOND/20);
    public static boolean active(LivingEntity e){
        if(e instanceof Player p)return Curse.data(p).getBoolean("DomainAmplification");
        return e.getPersistentData().getBoolean("SukunaDomainAmplification");
    }
    public static boolean unlocked(Player p){return p instanceof net.minecraftforge.common.util.FakePlayer||character.mainline.Mainline.unlocked(p,character.mainline.Mainline.AMPLIFY);}
    public static void toggle(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator())return;
        if(!unlocked(p)){p.displayClientMessage(Component.literal("领域展延尚未解锁：累计展开领域 10 次（L 查看进度）"),true);return;}
        boolean on=!active(p);
        if(on&&Curse.severed(p)){p.displayClientMessage(Component.literal("术式被天逆蛑封锁，无法开启领域展延"),true);return;}
        if(on&&!Curse.canSpend(p,COST_PER_TICK)){p.displayClientMessage(Component.literal("咒力不足，无法展开领域展延"),true);return;}
        Curse.data(p).putBoolean("DomainAmplification",on);Curse.sync(p);
        p.displayClientMessage(Component.literal(on?"领域展延：开启 · 每秒 "+BalanceRules.number(COST_PER_SECOND)+" 咒力 · 术式可穿透无下限":"领域展延：关闭"),true);
        p.level().playSound(null,p.blockPosition(),on?SoundEvents.BEACON_ACTIVATE:SoundEvents.BEACON_DEACTIVATE,SoundSource.PLAYERS,0.8f,on?1.4f:0.8f);
    }
    public static void stop(ServerPlayer p){if(active(p)){Curse.data(p).putBoolean("DomainAmplification",false);Curse.sync(p);}}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        if(!active(p))return;
        if(!p.isAlive()||p.isSpectator()||Curse.severed(p)||SoulCombat.suppressed(p)||DomainClash.active(p)){stop(p);return;}
        if(!Curse.spend(p,COST_PER_TICK)){stop(p);p.displayClientMessage(Component.literal("咒力耗尽，领域展延已解除"),true);return;}
    }
    private DomainAmplification(){}
}
