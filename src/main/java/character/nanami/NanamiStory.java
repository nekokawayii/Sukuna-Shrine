package character.nanami;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.List;

/** 七海建人：三章「社畜」「七三」「加班」。 */
public final class NanamiStory {
    public static List<Chapter> chapters() {
        return List.of(
            new Chapter("nanami_1", "第一章·社畜", 3, "击败任意敌人 3 次", "劳动的回报",
                new CharacterReward(new Stats(400, 12, 4, 6, 1), 60, 300, List.of())),
            new Chapter("nanami_2", "第二章·七三", 10, "击败任意敌人累计 10 次", "比例的眼力",
                new CharacterReward(new Stats(500, 14, 4, 8, 1), 90, 400, List.of())),
            new Chapter("nanami_3", "第三章·加班", 20, "击败任意敌人累计 20 次", "过劳的觉悟",
                new CharacterReward(new Stats(800, 18, 5, 10, 2), 150, 600, List.of()))
        );
    }
    private NanamiStory() {}
}
