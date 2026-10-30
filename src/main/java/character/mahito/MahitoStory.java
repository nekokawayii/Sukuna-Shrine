package character.mahito;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.List;

/** 真人：一章「灵魂的形」。 */
public final class MahitoStory {
    public static List<Chapter> chapters() {
        return List.of(new Chapter("mahito_1", "第一章·灵魂的形", 1,
            "击败任意敌人 1 次", "灵魂的形",
            new CharacterReward(new Stats(400, 10, 2, 4, 2), 50, 200, List.of())));
    }
    private MahitoStory() {}
}
