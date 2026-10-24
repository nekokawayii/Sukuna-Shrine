package character.yuji;
import character.CharacterDefinition;
import java.util.List;
import static character.CharacterDefinition.*;
public final class YujiCharacter {
    public static CharacterDefinition create() { return new CharacterDefinition("yuji", "虎杖", "体术", "咒力强化肉体，以咒力拳、径庭拳和连续拳近身作战。", Family.MARTIAL, Resource.CURSED_ENERGY, 0xFFFFAB85,
        List.of(7,8,13),List.of(new Chapter("拳之起点",10,"击败10名敌人，练习咒力拳","咒力上限提高"),new Chapter("灵魂节拍",30,"累计击败30名敌人，练习径庭拳","生命与近战成长"),new Chapter("不屈之拳",60,"累计击败60名敌人，熟练连续拳","咒力与肉体成长至最高阶段"))); }
    private YujiCharacter() {}
}
