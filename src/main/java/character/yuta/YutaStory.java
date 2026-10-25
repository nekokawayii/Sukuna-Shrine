package character.yuta;

import character.CharacterDefinition.Chapter;
import character.CharacterDefinition.ChapterKind;
import character.CharacterDefinition.EventChapter;
import character.CharacterConfig.Stats;
import character.CharacterReward;
import java.util.List;

/**
 * 真赝相爱的三章个人线。
 * <p>三章都不是「累计击杀」，而是事件判定，因此写在 {@code events} 里：
 * <ol>
 *   <li>目睹里香：100 格内有村民死亡后，120 秒内击败 3 只敌人。</li>
 *   <li>决战夏油杰：击败夏油杰（{@code sukuna:geto}）。</li>
 *   <li>百鬼夜行：在百鬼夜行维度内击败 100 只咒灵。</li>
 * </ol>
 * <p>{@code story} 仍保留三条占位章节：框架要求个人线非空，且 {@code rank()} 在有 events 时完全不读它。
 */
public final class YutaStory {
    public static List<Chapter> story() {
        return List.of(
            new Chapter("目睹里香", 1, "100格内有村民死亡后，120秒内击败3只敌人", "属性成长"),
            new Chapter("决战夏油杰", 2, "击败夏油杰", "属性成长"),
            new Chapter("百鬼夜行", 3, "在百鬼夜行维度内击败50只咒灵（每10只一波，共5波）", "属性成长")
        );
    }

    public static List<EventChapter> events() {
        return List.of(
            new EventChapter("witness_rika", "第一章·目睹里香",
                "100格内有村民死亡后，120秒内击败3只敌人", 3, ChapterKind.VILLAGER_THEN_KILLS, null,
                new CharacterReward(new Stats(0, 4, 1, 2, 1), 30, 100, List.of())),
            new EventChapter("geto_duel", "第二章·决战夏油杰",
                "击败夏油杰", 1, ChapterKind.BOSS_KILL, "sukuna:geto",
                new CharacterReward(new Stats(0, 6, 1, 3, 1), 60, 150, List.of())),
            new EventChapter("hyakki_yagyo", "第三章·百鬼夜行",
                "在百鬼夜行维度内击败50只咒灵（每10只一波，共5波）", 50, ChapterKind.DIMENSION_KILLS, null,
                new CharacterReward(new Stats(0, 10, 2, 5, 2), 100, 250, List.of()))
        );
    }
    private YutaStory() {}
}
