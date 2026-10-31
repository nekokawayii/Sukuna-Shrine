package character.megumi;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 伏黑惠的轮盘配色与开局属性（影色调）。 */
public final class MegumiStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("伏黑惠", "十种影法术", 0xE0101018, 0xE0222A38, 0xE0708AA0, 0xB03A4650, 0xFFDCE6F0),
            new Stats(1500, 12, 3, 6, 14),
            Stats.ZERO,
            MegumiSkills.all(),
            Map.of(MegumiSkills.TEN_SHADOWS, 0),
            Map.of()
        );
    }
    private MegumiStyle() {}
}
