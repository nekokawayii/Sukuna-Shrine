package cn.blockforge.generated.nnnnr1201forge2;

/** 斩痕从起点到终点的单向时间曲线，单位为游戏刻。 */
public final class SlashMotion {
    public static final int SWEEP=2, LIFE=10, INTERVAL=4;
    private SlashMotion(){}
    public static float progress(float age){
        float t=Math.max(0,Math.min(1,age/SWEEP));
        return t*t*(3-2*t);
    }
    public static float recovery(float age){
        return Math.max(0,Math.min(1,(age-SWEEP)/(LIFE-SWEEP)));
    }
}
