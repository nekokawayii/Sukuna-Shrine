package cn.blockforge.generated.nnnnr1201forge2;

/** 对话按服务端游戏刻计时，区块卸载时暂停并保留进度。 */
public final class GuardianTimeline {
    public static final int DURATION=91*20;
    private static final int[] SECONDS={0,8,12,16,21,27,32,38,46,50,56,61,65,67,71,74,79,82,87};
    public static boolean due(int line,int tick){return line>=0&&line<SECONDS.length&&tick>=1+SECONDS[line]*20;}
    public static boolean finished(int line,int tick){return line>=SECONDS.length&&tick>=DURATION;}
    public static int lines(){return SECONDS.length;}
    private GuardianTimeline(){}
}
