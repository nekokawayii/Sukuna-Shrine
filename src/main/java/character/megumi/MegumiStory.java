package character.megumi;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.List;

/** 伏黑惠：一章「影的继承」。 */
public final class MegumiStory {
    public static List<Chapter> chapters() {
        return List.of(new Chapter("megumi_1", "第一章·影的继承", 1,
            "击败任意敌人 1 次", "影的继承",
            new CharacterReward(new Stats(300, 8, 2, 3, 2), 50, 200, List.of())));
    }
    private MegumiStory() {}
}
