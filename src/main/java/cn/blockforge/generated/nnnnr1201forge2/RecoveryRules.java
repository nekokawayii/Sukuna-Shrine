package cn.blockforge.generated.nnnnr1201forge2;

import java.util.Set;

/** 只删除战斗临时状态；成长、敌友、控制器参数和玩家选择均保留。 */
public final class RecoveryRules {
    private static final Set<String> TRANSIENT=Set.of(
        "TechniqueBurnout","TechniqueBurnoutUntil","TechniqueBurnoutAt","TechniqueRepairedUntil",
        "FinaleBurnout","FinaleNoRecovery","FinalePending","FinaleSavedGravity","DomainClashPenaltyUntil",
        "RecoveryDelay","Siphon","Disorder","Parasite","Rotation","Combat","MultiArmed","Fist",
        "SoulGap","SoulSuppressed","SoulStiffUntil","SoulStiffStacks","Iwate","SoulHealHalf","SoulExecution","SoulPeelUntil","PureLandNoHeal",
        "GuardianPoisonUntil","GuardianPoisonDamage","GuardianPoisonCooldown","GuardianPoisonSource",
        "PlayerGuardianPoisonUntil","PlayerGuardianPoisonCooldown");
    public static boolean progressKey(String key){return TRANSIENT.contains(key)||key.contains("Cooldown");}
    public static boolean entityKey(String key){
        return key.startsWith("SukunaSoul")||key.equals("SukunaNoHeal")||key.startsWith("SukunaVoidFlame")
            ||key.startsWith("SukunaVoidIgnition")||key.startsWith("SukunaSixfoldScar")||key.startsWith("ClashAnchor")
            ||key.equals("DomainClashStunUntil")||key.equals("SoulGapUntil")||key.equals("VoidCaptured")
            ||key.equals("VoidNoHeal")||key.equals("VoidContactCooldown")||key.equals("GuardianSkillNoHeal");
    }
    private RecoveryRules(){}
}
