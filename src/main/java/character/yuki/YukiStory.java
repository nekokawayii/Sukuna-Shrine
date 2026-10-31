package character.yuki;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.List;

/** 九十九由基：三章成长线，奖励全部堆属性（数值怪路线）。 */
public final class YukiStory {
    public static List<Chapter> chapters() {
        return List.of(
            new Chapter("yuki_1", "第一章·星之怒", 1,
                "击败任意敌人 1 次", "星之怒的自觉",
                new CharacterReward(new Stats(1000, 10, 3, 4, 4), 60, 200, List.of())),
            new Chapter("yuki_2", "第二章·迦楼罗", 20,
                "累计击败 20 个敌人", "式神·迦楼罗",
                new CharacterReward(new Stats(1500, 12, 4, 6, 5), 120, 400, List.of())),
            new Chapter("yuki_3", "第三章·奇点", 50,
                "累计击败 50 个敌人", "质量奇点",
                new CharacterReward(new Stats(2500, 20, 6, 10, 8), 240, 800, List.of()))
        );
    }

    private YukiStory() {}
}
