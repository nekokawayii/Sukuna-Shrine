package character.yuji;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 虎杖的轮盘配色（橙）与开局属性。 */
public final class YujiStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("虎杖", "体术", 0xE0180D0A, 0xE02A1510, 0xFFFFAB85, 0xB05A2A1E, 0xFFFFEDE4),
            new Stats(1600, 18, 6, 0, 12),
            new Stats(300, 14, 4, 0, 0),
            YujiRebuildSkills.wheelSkills(),
            Map.of(YujiRebuildSkills.BREAK, 0),
            Map.of()
        );
    }
    private YujiStyle() { }
}
