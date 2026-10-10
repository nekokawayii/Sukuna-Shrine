package cn.blockforge.generated.nnnnr1201forge2;

/** O 键关系：0 默认、1 盟友、2 敌人。任何一方标敌优先于共享。 */
public final class GuardianProgressRules {
    public static boolean shares(int from,int to,boolean sameTeam){return from!=2&&to!=2&&(from==1||to==1||sameTeam);}
    private GuardianProgressRules(){}
}
