package cn.blockforge.generated.nnnnr1201forge2;

/** 同一场高位战的唯一生命记录；护盾、弹幕与输入仍由各玩家会话隔离。 */
public final class ObserverPartyRules {
    private int hits;
    public int hits(){return hits;}
    public boolean won(){return hits>=ObserverBattleRules.HITS_TO_WIN;}
    public boolean hit(boolean accepted){if(!accepted||won())return false;hits++;return true;}
    public void apply(ObserverBattleRules battle){battle.hits=hits;if(won()&&battle.phase!=ObserverBattleRules.FAILED){battle.phase=ObserverBattleRules.SHATTER;battle.blades.clear();battle.attackUntil=0;battle.weakUntil=0;}}
}
