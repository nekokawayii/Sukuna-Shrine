package character.yuki;

import character.CharacterConfig.Stats;
import character.CharacterDefinition.Chapter;
import character.CharacterReward;
import java.util.ArrayList;
import java.util.List;

/**
 * 九十九由基：20 章成长线，全部是「累计击杀」条件，奖励堆属性（血量与伤害拉满）。
 * <p>章节数与击杀门槛逐级递增：1 → 210，后期一章要清不少怪。
 */
public final class YukiStory {
    private static final int[] KILLS = {
        1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78, 91, 105, 120, 136, 153, 171, 190, 210
    };

    private static final String[] TITLES = {
        "星之怒", "质量的自觉", "第一拳", "迦楼罗", "加重", "压碎",
        "山一般的重量", "不动的质量", "拳骨", "引力", "坍缩前夜", "奇点",
        "特级的余裕", "星之轨迹", "落锤", "千钧", "地鸣", "碎山",
        "黑洞边缘", "质量尽头"
    };

    public static List<Chapter> chapters() {
        var list = new ArrayList<Chapter>(KILLS.length);
        for (int i = 0; i < KILLS.length; i++) {
            int n = i + 1;
            // 数值怪：每章都塞大量咒力、生命与伤害
            Stats stats = new Stats(600 + n * 400, 10 + n * 3, 2 + n, 3 + n / 2, 2 + n / 2);
            list.add(new Chapter("yuki_" + n, "第" + n + "章·" + TITLES[i], KILLS[i],
                "累计击败 " + KILLS[i] + " 个敌人", "质量成长",
                new CharacterReward(stats, 40 + n * 10, 100 + n * 40, List.of())));
        }
        return List.copyOf(list);
    }

    private YukiStory() {}
}
