package character.hakari;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.ArrayList;
import java.util.List;

/** 秤金次：30 章成长线，全部按累计击杀判定；奖励偏生命与近战（赌狗流前期偏软）。 */
public final class HakariStory {
    private static final String[] WORDS = {
        "第一注", "下注", "赌徒", "坐杀", "博打", "开奖", "头奖", "4分11秒", "赌场", "手气"
    };

    public static List<Chapter> chapters() {
        var list = new ArrayList<Chapter>(30);
        for (int i = 0; i < 30; i++) {
            int n = i + 1;
            int kills = (int) Math.round(n * (n + 1) * 1.5);   // 3, 9, 18 … 1395
            Stats stats = new Stats(3000, n <= 27 ? 10 : 0, 2 + n, 2 + n, 1 + n / 2);
            list.add(new Chapter("hakari_" + n, "第" + n + "章·" + WORDS[i % WORDS.length],
                kills, "累计击败 " + kills + " 个敌人", "赌运",
                new CharacterReward(stats, 30 + n * 8, 80 + n * 30, List.of())));
        }
        return List.copyOf(list);
    }

    private HakariStory() {}
}
