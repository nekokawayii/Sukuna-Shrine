package character.yuki;

import character.CharacterConfig;
import character.CharacterConfig.Stats;
import character.CharacterConfig.Wheel;
import java.util.List;
import java.util.Map;

/** 九十九由基的轮盘配色与开局属性（琥珀 + 深紫，对应星之怒的「质量」）。 */
public final class YukiStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("九十九由基", "星之怒", 0xE01A1420, 0xE02C2138, 0xE0FFC46B, 0xB03A2E44, 0xFFFFF1DC),
            // 数值怪：高咒力、高生命、高近战与术式加成，回咒也快。
            new Stats(6000, 10, 8, 10, 20),
            Stats.ZERO,
            YukiSkills.all(),
            Map.of(),
            Map.of()
        );
    }

    public static List<Integer> skills() { return YukiSkills.all().stream().map(character.CharacterSkill::id).toList(); }

    private YukiStyle() {}
}
