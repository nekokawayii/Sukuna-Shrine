package character.mahito;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 真人的轮盘配色与开局属性（灰色调，咒力资源）。 */
public final class MahitoStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("真人", "无为转变", 0xE0141418, 0xE02A2A30, 0xE0808A94, 0xB0454A52, 0xFFE8F0F5),
            new Stats(1200, 8, 2, 4, 12),
            Stats.ZERO,
            MahitoSkills.all(),
            Map.of(MahitoSkills.TRANSMUTE, 0, MahitoSkills.SELF_EMBRYO, 0),
            Map.of()
        );
    }
    private MahitoStyle() {}
}
