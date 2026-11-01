package character.npc;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.world.entity.Mob;

/** 保留五条/虎杖首领原有动作状态机，只统一资源和领域规则。 */
public final class NpcLegacyCombat {
    public static void tick(Mob mob,boolean fighting){
        var d=mob.getPersistentData();long now=mob.level().getGameTime();
        d.putInt("NpcCombatTicks",fighting?Math.min(12000,d.getInt("NpcCombatTicks")+1):0);
        if(d.getLong("NpcLegacyBurnout")>now&&d.getLong("NpcLegacyRepairAt")<=now
            &&d.getLong("NpcLegacyPenaltyUntil")<=now&&!DomainClash.stunned(mob)&&!DomainClash.active(mob)){
            if(NpcCurse.spend(mob,NpcCurse.max(mob)*.15))d.remove("NpcLegacyBurnout");
            d.putLong("NpcLegacyRepairAt",now+40);
        }
    }
    public static boolean domainReady(Mob mob){
        var d=mob.getPersistentData();
        return NpcBattleRules.domainReady(mob.level().getGameTime(),d.getLong("NpcDomainReadyAt"),
            d.getInt("NpcCombatTicks"),mob.getHealth()/mob.getMaxHealth(),DomainClash.inEnemyDomain(mob));
    }
    public static boolean pay(Mob mob,double cost,boolean domain){
        if(DomainClash.active(mob)||DomainClash.stunned(mob)
            ||mob.getPersistentData().getLong("NpcLegacyPenaltyUntil")>mob.level().getGameTime()
            ||mob.getPersistentData().getLong("NpcLegacyBurnout")>mob.level().getGameTime()
            ||domain&&!domainReady(mob))return false;
        if(!NpcCurse.spend(mob,cost))return false;
        if(domain)mob.getPersistentData().putLong("NpcDomainReadyAt",mob.level().getGameTime()+NpcBattleRules.DOMAIN_COOLDOWN);
        return true;
    }
    public static void burnout(Mob mob){
        mob.getPersistentData().putLong("NpcLegacyBurnout",mob.level().getGameTime()+2400);
        mob.getPersistentData().putLong("NpcLegacyRepairAt",mob.level().getGameTime()+NpcBattleRules.REPAIR_DELAY);
    }
    private NpcLegacyCombat() { }
}
