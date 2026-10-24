package character.sukuna;
import character.CharacterDefinition;
import java.util.List;
import static character.CharacterDefinition.*;
public final class SukunaCharacter {
    public static CharacterDefinition create() { return new CharacterDefinition("sukuna", "宿傩", "伏魔御厨子", "保留宿傩手指、即身佛、九相图与后续传承的原有成长线和技能分支。", Family.SHRINE, Resource.CURSED_ENERGY, 0xFFE34D65,
        List.of(0,1,2,3,4,5),List.of(new Chapter("咒物受肉",0,"吞噬20根宿傩手指","解锁捌与反转术式"),new Chapter("肉体归还",0,"击败即身佛并融合肉体","世界斩与高级分支"),new Chapter("九相与传承",0,"完成九相试炼并继续原有个人线","赤血操术、灵魂与虚空传承"))); }
    private SukunaCharacter() {}
}
