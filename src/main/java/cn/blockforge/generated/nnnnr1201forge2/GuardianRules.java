package cn.blockforge.generated.nnnnr1201forge2;

/** 不依赖客户端或伤害事件顺序的净土战斗边界。 */
public final class GuardianRules {
    public static final int DOMAIN_COOLDOWN_TICKS=60*20;
    public static final float DEFEAT_HEALTH=1;
    public static final double SPELL_REWARD=200;

    public static float cap(float damage,float limit){
        if(Float.isNaN(damage)||damage<=0)return 0;
        if(!Float.isFinite(limit)||limit<0)limit=0;
        return Math.min(damage,limit);
    }
    public static float healthAfter(float before,float requested,float limit,float floor){
        if(Float.isNaN(requested))return before;
        if(requested>=before)return requested;
        return Math.max(DEFEAT_HEALTH,Math.max(floor,before-cap(before-requested,limit)));
    }
    public static boolean domainReady(int remaining,long now,long next){return remaining<=0&&now>=next;}
    public static double spellMultiplier(double base,boolean victory){return base+(victory?SPELL_REWARD:0);}
    private GuardianRules(){}
}
