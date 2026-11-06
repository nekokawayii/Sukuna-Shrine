package character.uraume;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 里梅的轮盘配色（冰蓝）与开局属性。 */
public final class UraumeStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("里梅", "冰凝咒法", 0xE00A1620, 0xE0162836, 0xE0A8EEFF, 0xB03E5866, 0xFFEAF9FF),
            new Stats(1800, 12, 2, 6, 15),
            new Stats(400, 10, 2, 8, 0),
            UraumeSkills.all(),
            Map.of(UraumeSkills.FROST, 0),
            Map.of()
        );
    }
    private UraumeStyle() {}
}
