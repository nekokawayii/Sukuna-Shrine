package character.uraume;
import character.CharacterDefinition;
import java.util.List;
import static character.CharacterDefinition.*;
public final class UraumeCharacter {
    public static CharacterDefinition create() { return new CharacterDefinition("uraume", "里梅", "冰凝咒法",
        "霜凪冻结前方，直瀑降下冰柱；千手冰牢、急冻风暴、冰龙与大冰河时代由冰凝咒法延伸。", Family.ICE, Resource.CURSED_ENERGY, 0xFFA8EEFF,
        List.of(UraumeSkills.FROST, UraumeSkills.ICEFALL, UraumeSkills.PRISON, UraumeSkills.STORM,
            UraumeSkills.DRAGON, UraumeSkills.ICEAGE),
        List.of(new Chapter("凝霜",10,"击败10名敌人，练习霜凪","咒力上限提高"),
            new Chapter("冰瀑",30,"累计击败30名敌人，可继续食用冰鱼","生命与近战成长"),
            new Chapter("寒天",60,"累计击败60名敌人","咒力上限再次提高")),
        UraumeStyle.config()); }
    private UraumeCharacter() {}
}
