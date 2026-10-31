package character.ryu;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.List;

/** 石流龙：三章「超载」「光炮」「甜点」。 */
public final class RyuStory {
    public static List<Chapter> chapters() {
        return List.of(
            new Chapter("ryu_1", "第一章·超载", 3, "击败任意敌人 3 次", "咒力容量的扩张",
                new CharacterReward(new Stats(400, 10, 2, 6, 1), 60, 300, List.of())),
            new Chapter("ryu_2", "第二章·光炮", 10, "击败任意敌人累计 10 次", "放出的精度",
                new CharacterReward(new Stats(600, 12, 3, 8, 1), 90, 400, List.of())),
            new Chapter("ryu_3", "第三章·甜点", 20, "击败任意敌人累计 20 次", "超载的觉悟",
                new CharacterReward(new Stats(1000, 16, 4, 10, 2), 150, 600, List.of()))
        );
    }
    private RyuStory() {}
}
