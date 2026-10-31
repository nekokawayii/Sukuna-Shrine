package character.higuruma;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 日车宽见的轮盘配色（黑金）与开局属性。 */
public final class HigurumaStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("日车宽见", "诛伏赐死", 0xE0141310, 0xE0332C1E, 0xE0C9A227, 0xB05A4A28, 0xFFF2E6C0),
            new Stats(1600, 10, 3, 6, 14),
            Stats.ZERO,
            HigurumaSkills.all(),
            Map.of(HigurumaSkills.SEAL, 0),
            Map.of()
        );
    }
    private HigurumaStyle() {}
}
