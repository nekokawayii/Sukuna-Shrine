package character.domainking;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.List;

/** 领域王：只有一章（开局即满），奖励给巨额咒力上限。 */
public final class DomainKingStory {
    public static List<Chapter> chapters() {
        return List.of(new Chapter("dk_awake", "第一章·万域之主", 1,
            "用任一领域击败一名敌人", "万域之主",
            new CharacterReward(new Stats(0.0, 20, 2, 6, 9999), 0, 0, List.of())));
    }
    private DomainKingStory() {}
}
