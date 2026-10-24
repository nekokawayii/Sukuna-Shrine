package cn.blockforge.generated.nnnnr1201forge2;

/** 菜单敌友的最终授权：标敌可覆盖同队，但绝不能覆盖服务器关闭 PvP。 */
public final class CombatRelationRules {
    public static boolean player(int relation,boolean creative,boolean spectator,boolean pvp){return relation==2&&!creative&&!spectator&&pvp;}
    public static boolean creature(int relation,boolean allied){return relation!=1&&(relation==2||!allied);}
    public static boolean summon(boolean authorized,boolean markedEnemy,boolean monster,boolean ownerAttacking,boolean ownerAttacked,boolean threat){return authorized&&(markedEnemy||monster||ownerAttacking||ownerAttacked||threat);}
    private CombatRelationRules(){}
}
