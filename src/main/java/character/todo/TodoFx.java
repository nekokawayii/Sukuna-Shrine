package character.todo;

/** 东堂葵（不义游戏）的特效号。 */
public final class TodoFx {
    /** 注入咒力：owner=实体 id（0=方块），position=标记位置，size=1 方块 / 2 实体，seed=标记键。 */
    public static final int MARK = 371;
    /** 换位消耗掉标记：参数同 MARK。 */
    public static final int UNMARK = 372;
    /** 换位完成：position=起点，direction=终点。 */
    public static final int SWAP = 373;
    /** 注入时的火花：position=命中点。 */
    public static final int INJECT = 374;
    public static final int FIRST = MARK, LAST = INJECT;
    private TodoFx() {}
}
