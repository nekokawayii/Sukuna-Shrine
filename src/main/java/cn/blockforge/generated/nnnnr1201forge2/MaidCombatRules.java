package cn.blockforge.generated.nnnnr1201forge2;

/** 玩家目标必须由对决或明确敌对授权，不能把旧攻击记忆当作授权。 */
public final class MaidCombatRules {
    public static boolean playerTarget(boolean duel,boolean owner,boolean creative,boolean spectator,boolean pvp,boolean enemy){
        if(creative||spectator)return false;
        // 真赝相爱只对饰品佩戴者的主人开放，不波及其他玩家。
        if(duel)return owner;
        return !owner&&pvp&&enemy;
    }
    private MaidCombatRules(){}
}
