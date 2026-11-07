package cn.blockforge.generated.nnnnr1201forge2;

/** 五条悟技能分支（r302）：苍/赫/无量 六式。编号 3400–3405，输入由技能轮盘第二页发送。 */
public final class GojoBranches {
    public static final int SINGULARITY = 3400, TEAR = 3401, METEOR = 3402,
        HEAVY = 3403, TURBULENCE = 3404, BRAINDEAD = 3405;
    public static final int FX_SINGULARITY = 760, FX_TEAR = 761, FX_METEOR = 762,
        FX_HEAVY = 763, FX_TURBULENCE = 764, FX_BRAIN = 765;

    public static final String[] NAMES = {"苍·坍缩奇点", "苍·引力撕裂", "苍·微型流星",
        "赫·重压冲击", "赫·苍红乱流", "无量空处·局部脑死"};
    public static final String[] LABELS = {"奇点", "撕裂", "流星", "重压", "乱流", "脑死"};
    public static final int[] COSTS = {260, 200, 180, 150, 320, 400};
    public static final int[] COOLDOWNS = {200, 120, 100, 200, 260, 600};
    public static final String[] DESC = {
        "准星处制造引力奇点：10 格内敌人被吸到中心并定身 2 秒，坍缩造成高额真实伤害。",
        "向准星打出引力波：命中后缠绕引力线多段撕裂并附加流血。",
        "连弹 5 颗微型苍球，自动追踪 15 格内敌人，命中产生小范围引力爆炸。",
        "近战挥拳瞬间引爆赫：巨额物理伤害、强击退，并击碎护甲与护盾。",
        "双手交错连轰 5 发斥力炮弹，5 发全中同一目标时引发动能核爆。",
        "把无量空处压缩到指尖点中头部：目标瘫痪 5 秒（Boss 2 秒），期间受伤翻倍。"};

    public static int idx(int skill) { return skill - SINGULARITY; }
    public static boolean branch(int skill) { return skill >= SINGULARITY && skill <= BRAINDEAD; }
    public static String name(int skill) { return branch(skill) ? NAMES[idx(skill)] : "五条悟"; }
    public static String label(int skill) { return branch(skill) ? LABELS[idx(skill)] : ""; }
    public static String desc(int skill) { return branch(skill) ? DESC[idx(skill)] : ""; }
    public static int cost(int skill) { return branch(skill) ? COSTS[idx(skill)] : 0; }
    public static int cooldown(int skill) { return branch(skill) ? COOLDOWNS[idx(skill)] : 0; }

    private GojoBranches() { }
}
