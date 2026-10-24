package character.example;

import character.CharacterDefinition.Chapter;
import character.CharacterConfig.Stats;
import character.CharacterReward;
import character.CharacterReward.Item;
import java.util.List;

/** kills为累计有效击杀；每章有永久编号，奖励数量是真正执行的数据。 */
public final class ExampleStory {
    public static List<Chapter> chapters() {
        return List.of(
            new Chapter("first_frost","第一章·初霜",10,"累计击败10名敌人","开始练习扇形控制",
                new CharacterReward(new Stats(200,4,1,2,1),30,100,List.of(new Item("minecraft:iron_ingot",4)))),
            new Chapter("frost_bloom","第二章·散华",30,"累计击败30名敌人","掌握周身霜环",
                new CharacterReward(new Stats(300,6,1,3,1),60,150,List.of(new Item("minecraft:diamond",2)))),
            new Chapter("winter_master","第三章·寒天",60,"累计击败60名敌人","三招继续按章节成长",
                new CharacterReward(new Stats(500,10,2,5,2),100,250,List.of(new Item("minecraft:golden_apple",1))))
        );
    }
    private ExampleStory() {}
}
