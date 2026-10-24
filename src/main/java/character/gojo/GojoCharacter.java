package character.gojo;
import character.CharacterDefinition;
import java.util.List;
import static character.CharacterDefinition.*;
/** 五条悟：无下限技能和个人线的唯一角色入口。 */
public final class GojoCharacter {
    public static CharacterDefinition create() { return new CharacterDefinition("gojo", "五条悟", "无下限", "苍与赫控制战场，茈贯穿敌人，无量空处压制敌人。", Family.LIMITLESS, Resource.CURSED_ENERGY, 0xFF72CFFF,
        List.of(34,35,36,37,38,39,40),
        List.of(new Chapter("六眼初醒",10,"击败10名敌人，练习苍与赫","咒力上限提高"),new Chapter("术式反转",30,"累计击败30名敌人，掌握茈与瞬移","生命与近战成长"),new Chapter("最强之路",60,"累计击败60名敌人，熟练无量空处","咒力上限再次提高"))); }
    private GojoCharacter() {}
}
