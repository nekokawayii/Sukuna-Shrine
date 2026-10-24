package character.jogo;

import character.CharacterDefinition.Chapter;
import character.CharacterConfig.Stats;
import character.CharacterReward;
import character.CharacterReward.Item;
import java.util.List;

/** 漏壶个人线：第一章靠"泡岩浆 30 秒"完成（进度由 {@link JogoCombat} 统计并给 rank）。 */
public final class JogoStory {
    public static List<Chapter> chapters() {
        return List.of(
            new Chapter("jogo_lava","第一章·于长夜重返大地",1,
                "自己收集 3 桶岩浆，并在岩浆里浸泡 30 秒","于长夜重返大地",
                new CharacterReward(new Stats(400,8,2,4,2),50,200,
                    List.of(new Item("minecraft:magma_block",8), new Item("minecraft:blaze_powder",4)))),
            new Chapter("jogo_siege","第二章·盖棺铁围山",2,
                "残血（生命低于30%）、周围至少 5 名敌人、且处于燃烧状态，坚持 60 秒","盖棺铁围山",
                new CharacterReward(new Stats(600,12,2,6,2),80,300,
                    List.of(new Item("minecraft:magma_cream",6), new Item("minecraft:blaze_rod",2))))
        );
    }
    private JogoStory() {}
}
