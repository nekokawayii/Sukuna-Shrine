package cn.blockforge.generated.nnnnr1201forge2;

/** 残秽满百凝炼一次，每级永久加伤3点；计算先用long，避免大数溢出。 */
public final class ResidueProgress {
    public static final int XP_PER_LEVEL=100, PERCENT_PER_LEVEL=3;
    public static final int MAX_LEVEL=Integer.MAX_VALUE/PERCENT_PER_LEVEL;
    public record State(int xp,int level){}
    private ResidueProgress(){}
    public static int clampLevel(int level){return Math.max(0,Math.min(MAX_LEVEL,level));}
    public static State normalize(long xp,int level){
        long amount=Math.max(0,xp);
        int nextLevel=(int)Math.min(MAX_LEVEL,clampLevel(level)+amount/XP_PER_LEVEL);
        return new State((int)(amount%XP_PER_LEVEL),nextLevel);
    }
    public static int percent(int level){return clampLevel(level)*PERCENT_PER_LEVEL;}
    public static double bonus(int level){return 1+percent(level)/100.0;}
}
