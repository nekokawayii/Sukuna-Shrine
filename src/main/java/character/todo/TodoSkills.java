package character.todo;

import character.CharacterSkill;
import java.util.List;

/** 东堂葵 · 不义游戏：1 注入咒力 / 2 换位（无 CD）。 */
public final class TodoSkills {
    public static final int INJECT = 2500, SWAP = 2501;

    /** 换位免疫窗口已经写在 {@link TodoCombat#DODGE_TICKS}。 */
    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(INJECT, "注入咒力", "注入",
                "对准星指向的方块、弹幕或生物注入咒力（永久有效、换位时消耗）。生物本来就自带咒力，注入主要用于标记和布置方块。",
                0xFFB58CFF, 60, 10, CharacterSkill.Shape.RAY,
                24, 1, 20, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(SWAP, "换位", "换位",
                "与准星最近的有咒力的东西互换位置（生物默认自带咒力，没被注入过也能换）：实体真互换，方块搬到脚下；换位前后各 0.3 秒免疫伤害。无冷却。",
                0xFFE6D4FF, 0, 1, CharacterSkill.Shape.BURST,
                32, 1, 180, 0, 0, 1, 0, 0, 0)
        );
    }
    private TodoSkills() {}
}
