package character.higuruma;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.List;

/** 日车宽见：一章「审判者」。 */
public final class HigurumaStory {
    public static List<Chapter> chapters() {
        return List.of(new Chapter("higuruma_1", "第一章·审判者", 1,
            "击败任意敌人 1 次", "审判者",
            new CharacterReward(new Stats(500, 10, 3, 5, 2), 50, 200, List.of())));
    }
    private HigurumaStory() {}
}
