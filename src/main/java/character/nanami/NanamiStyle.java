package character.nanami;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 七海建人的轮盘配色（黑金）与开局属性。 */
public final class NanamiStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("七海建人", "十划咒法", 0xE0121010, 0xE02A2418, 0xE0D8B24A, 0xB0524526, 0xFFFFF4D6),
            new Stats(1750, 18, 5, 8, 12),
            Stats.ZERO,
            NanamiSkills.all(),
            Map.of(NanamiSkills.SMASH, 0),
            Map.of()
        );
    }
    private NanamiStyle() {}
}
