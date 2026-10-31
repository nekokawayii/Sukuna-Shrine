package character.yuki;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.ArrayList;
import java.util.List;

/**
 * 九十九由基：100 章成长线，全部按「累计击杀」判定。
 * <p>门槛递增（最后一章约 1.6 万只），奖励持续堆属性；配合领域内自动刷怪，后期可以靠领域刷章。
 */
public final class YukiStory {
    /** 100 章的累计击杀门槛。 */
    private static final int[] KILLS = buildKills();

    private static final String[] WORDS = {
        "星之怒", "质量的自觉", "第一拳", "迦楼罗", "加重", "压碎",
        "山一般的重量", "不动的质量", "拳骨", "引力"
    };

    private static int[] buildKills() {
        int[] arr = new int[100];   // 框架上限 128，这里用 100 章
        double acc = 0;
        for (int i = 0; i < arr.length; i++) {
            acc += 3 + i * 3.2;      // 每章步长递增，100 章合计约 1.6 万
            arr[i] = (int) Math.round(acc);
        }
        return arr;
    }

    public static List<Chapter> chapters() {
        var list = new ArrayList<Chapter>(KILLS.length);
        for (int i = 0; i < KILLS.length; i++) {
            int n = i + 1;
            // 注意：CharacterDefinition 会把整条线的奖励累加后校验，每项必须 ≤ 100 万。
            Stats stats = new Stats(9000, 40 + n * 4, 2 + n * 2, 4 + n * 3, 2 + n);
            list.add(new Chapter("yuki_" + n, "第" + n + "章·" + WORDS[i % WORDS.length] + " " + ((i / WORDS.length) + 1),
                KILLS[i], "累计击败 " + KILLS[i] + " 个敌人", "质量成长",
                new CharacterReward(stats, 30 + n * 6, 80 + n * 25, List.of())));
        }
        return List.copyOf(list);
    }

    private YukiStory() {}
}
