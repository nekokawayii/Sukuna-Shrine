package cn.blockforge.generated.nnnnr1201forge2;

/** 调服边界的纯规则：参与范围64格，挑战范围500格，球形而不是方盒。 */
public final class MahoragaRitualRules {
    public static final double PARTICIPANT_RADIUS=64,CHALLENGE_RADIUS=500;
    public static boolean participant(double squaredDistance){return Double.isFinite(squaredDistance)&&squaredDistance<=PARTICIPANT_RADIUS*PARTICIPANT_RADIUS;}
    public static boolean escaped(boolean online,boolean alive,boolean sameLevel,double squaredDistance){return !online||!alive||!sameLevel||!Double.isFinite(squaredDistance)||squaredDistance>CHALLENGE_RADIUS*CHALLENGE_RADIUS;}
    public static boolean intruder(boolean maid,boolean player,boolean mutualEnemy,boolean attackingOwner){return maid||(player?!mutualEnemy:!attackingOwner);}
    public static boolean reward(int state,boolean ownerValid,boolean killerAllowed){return state==1&&ownerValid&&killerAllowed;}
    private MahoragaRitualRules(){}
}
