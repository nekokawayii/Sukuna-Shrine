package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** 分支施法失败时优先显示真实剩余冷却，不把缺咒力等原因误报成冷却。 */
public final class CastFeedback {
    public static void tell(ServerPlayer p,String text){p.displayClientMessage(Component.literal(text),true);}
    public static boolean cooldown(ServerPlayer p,String key,String name){
        long ticks=Curse.data(p).getLong(key)-p.level().getGameTime();
        if(ticks<=0)return false;
        tell(p,name+"冷却中 · "+String.format(java.util.Locale.ROOT,"%.1f",ticks/20.0)+" 秒");return true;
    }
    public static void unavailable(ServerPlayer p,double cost){
        if(Curse.burnout(p))tell(p,"术式熔断中：只能使用术式反转或修复熔断");
        else if(SoulCombat.suppressed(p))tell(p,"灵魂压制中，无法使用术式");
        else if(!Curse.canSpend(p,cost))tell(p,"咒力不足：需要 "+BalanceRules.number(cost)+" 咒力");
        else tell(p,"正在受控或施法，请等待当前动作结束");
    }
    private CastFeedback(){}
}
