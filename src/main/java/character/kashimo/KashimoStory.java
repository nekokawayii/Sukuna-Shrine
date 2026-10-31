package character.kashimo;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.List;

/** 鹿紫云一：三章「雷神」「电荷」「幻兽」。 */
public final class KashimoStory {
    public static List<Chapter> chapters() {
        return List.of(
            new Chapter("kashimo_1", "第一章·雷神", 3, "击败任意敌人 3 次", "雷神的体魄",
                new CharacterReward(new Stats(400, 10, 2, 8, 1), 60, 300, List.of())),
            new Chapter("kashimo_2", "第二章·电荷", 10, "击败任意敌人累计 10 次", "电荷的共鸣",
                new CharacterReward(new Stats(500, 12, 3, 10, 1), 90, 400, List.of())),
            new Chapter("kashimo_3", "第三章·幻兽", 20, "击败任意敌人累计 20 次", "幻兽的觉悟",
                new CharacterReward(new Stats(800, 16, 4, 12, 2), 150, 600, List.of()))
        );
    }
    private KashimoStory() {}
}
