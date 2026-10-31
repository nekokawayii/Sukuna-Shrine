package character.todo;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.List;

/** 东堂葵：三章「挚友」「不义游戏」「兄贵」。 */
public final class TodoStory {
    public static List<Chapter> chapters() {
        return List.of(
            new Chapter("todo_1", "第一章·挚友", 3, "击败任意敌人 3 次", "挚友的认可",
                new CharacterReward(new Stats(400, 12, 3, 6, 1), 60, 300, List.of())),
            new Chapter("todo_2", "第二章·不义游戏", 10, "击败任意敌人累计 10 次", "术式的熟练",
                new CharacterReward(new Stats(500, 14, 3, 8, 1), 90, 400, List.of())),
            new Chapter("todo_3", "第三章·兄贵", 20, "击败任意敌人累计 20 次", "兄贵的觉悟",
                new CharacterReward(new Stats(800, 18, 4, 10, 2), 150, 600, List.of()))
        );
    }
    private TodoStory() {}
}
