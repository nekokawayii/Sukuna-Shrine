package character.toji;

/** 无游戏依赖的体术规则，便于构建时回归测试。 */
public final class TojiRules {
    public static int rank(int value) { return Math.max(0,Math.min(3,value)); }
    public static double maximum(int rank) { return 100+20*rank(rank); }
    public static double recover(int rank) { return 8+2*rank(rank); }
    public static int cost(int kind) { return switch(kind){case 0->20;case 1->30;case 2->25;default->Integer.MAX_VALUE;}; }
    public static int cooldown(int kind) { return kind==1?30:20; }
    public static double current(double value,int rank) { return Double.isFinite(value)?Math.max(0,Math.min(maximum(rank),value)):0; }
    public static double dash(int rank) { return 4+rank(rank); }
    public static double reach(int rank) { return 3+0.4*rank(rank); }
    public static boolean canSpend(double value,int rank,int kind) { return kind>=0&&kind<=2&&current(value,rank)>=cost(kind); }
    private TojiRules() {}
}
