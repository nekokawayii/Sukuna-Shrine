package character.ryu;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 石流龙的轮盘配色（蓝白）与开局属性：咒力 10000、每秒只回 5。 */
public final class RyuStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("石流龙", "咒力强化/放出", 0xE00A1016, 0xE0182A38, 0xE09FDFFF, 0xB03F6270, 0xFFEAF8FF),
            new Stats(10000, 20, 5, 10, 5),
            Stats.ZERO,
            RyuSkills.all(),
            Map.of(RyuSkills.BLAST, 0),
            Map.of()
        );
    }
    private RyuStyle() {}
}
