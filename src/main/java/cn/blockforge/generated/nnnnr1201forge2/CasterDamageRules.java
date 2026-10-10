package cn.blockforge.generated.nnnnr1201forge2;

/** 伤害结算规则：女仆在既有数值上减半，玩家保持不变；成长先相加再缩放。 */
public final class CasterDamageRules {
    public static double casterFactor(boolean maid){return maid?0.5:1.0;}
    public static float scale(double amount,boolean maid){return (float)Math.max(0,amount*casterFactor(maid));}
    public static float spell(double base,double multiplier,double bonus,double status,double residue,boolean domain,boolean maid){
        double amount=(base*multiplier+bonus)*0.6;
        if(domain)amount*=0.6;
        return scale(amount*status*residue,maid);
    }
    /** 虚空火焰沿用原有固定伤害规则，不额外叠加普通术式的60%倍率。 */
    public static float fixed(double base,double bonus,double status,double residue,boolean maid){
        return scale((base+bonus)*status*residue,maid);
    }
    /** 半径2.5的原版爆炸对应5格伤害范围；这里只计算伤害，不发出爆炸事件。 */
    public static float furnaceBlast(double distance,double visibility,boolean maid){
        if(!Double.isFinite(distance)||!Double.isFinite(visibility)||distance<0||distance>5||visibility<=0)return 0;
        double impact=(1-distance/5)*Math.min(1,visibility);
        return scale((int)((impact*impact+impact)*0.5*7*5+1),maid);
    }
    private CasterDamageRules(){}
}
