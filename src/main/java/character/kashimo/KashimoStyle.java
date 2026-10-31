package character.kashimo;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 鹿紫云一的轮盘配色（雷光蓝白）与开局属性。 */
public final class KashimoStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("鹿紫云一", "幻兽琥珀", 0xE0081018, 0xE0182A38, 0xE07FD8FF, 0xB03A5060, 0xFFE8F8FF),
            new Stats(1700, 12, 4, 8, 12),
            Stats.ZERO,
            KashimoSkills.all(),
            Map.of(KashimoSkills.DASH, 0),
            Map.of()
        );
    }
    private KashimoStyle() {}
}
