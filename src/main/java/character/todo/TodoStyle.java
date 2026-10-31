package character.todo;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 东堂葵的轮盘配色（紫白）与开局属性。 */
public final class TodoStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("东堂葵", "不义游戏", 0xE0141018, 0xE02A2038, 0xE0B58CFF, 0xB0544060, 0xFFF2E9FF),
            new Stats(1800, 16, 4, 6, 13),
            Stats.ZERO,
            TodoSkills.all(),
            Map.of(TodoSkills.INJECT, 0),
            Map.of()
        );
    }
    private TodoStyle() {}
}
