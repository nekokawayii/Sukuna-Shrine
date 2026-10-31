package character.naoya;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.List;

/** 禅院直哉：三章「速度」「投射」「同化」。 */
public final class NaoyaStory {
    public static List<Chapter> chapters() {
        return List.of(
            new Chapter("naoya_1", "第一章·速度", 3, "击败任意敌人 3 次", "先手必胜",
                new CharacterReward(new Stats(400, 12, 4, 6, 1), 80, 300, List.of())),
            new Chapter("naoya_2", "第二章·投射", 10, "击败任意敌人累计 10 次", "把动作拆成一帧",
                new CharacterReward(new Stats(500, 14, 4, 8, 1), 120, 400, List.of())),
            new Chapter("naoya_3", "第三章·同化", 20, "击败任意敌人累计 20 次", "不可违抗的帧",
                new CharacterReward(new Stats(800, 18, 5, 10, 2), 180, 600, List.of())));
    }

    private NaoyaStory() {}
}
