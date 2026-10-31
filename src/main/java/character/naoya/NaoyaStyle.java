package character.naoya;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 禅院直哉的轮盘配色（青白）与开局属性。 */
public final class NaoyaStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("禅院直哉", "投射咒法", 0xE00C1218, 0xE0182630, 0xE07FD8FF, 0xB02C4552, 0xFFEAF8FF),
            new Stats(1900, 20, 5, 9, 13),
            Stats.ZERO,
            NaoyaSkills.all(),
            Map.of(NaoyaSkills.LINE, 0),
            Map.of());
    }

    private NaoyaStyle() {}
}
