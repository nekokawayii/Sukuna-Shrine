package cn.blockforge.generated.nnnnr1201forge2;

/** 91个伤害档位采用严格递减权重：+10有91份，+100仅1份。 */
public final class RevelationRules {
    public static final int MIN=10,MAX=100,TOTAL_WEIGHT=4186;
    public static int damage(int ticket){
        if(ticket<0||ticket>=TOTAL_WEIGHT)throw new IllegalArgumentException("抽签序号越界");
        for(int value=MIN;value<=MAX;value++){int weight=MAX-value+1;if(ticket<weight)return value;ticket-=weight;}
        throw new IllegalStateException("抽签权重错误");
    }
    public static int quantity(int base,boolean enhanced,int limit){return Math.min(limit,Math.max(0,base)*(enhanced?2:1));}
    private RevelationRules(){}
}
