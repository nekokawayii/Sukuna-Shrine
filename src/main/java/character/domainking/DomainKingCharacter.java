package character.domainking;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/** 领域王：选择书里显示「万域」，轮盘全部是已有领域；咒力无限。 */
public final class DomainKingCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "domainking", "领域王", "万域",
            "把已出现过的所有领域都收进轮盘：盖棺铁围山、无量空处、伏魔御厨子、胎海、真赝相爱、简易领域·捌。咒力视为无限。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFFB57BFF,
            List.of(DomainKingSkills.JOGO_DOMAIN, DomainKingSkills.VOID, DomainKingSkills.SHRINE,
                DomainKingSkills.BLOOD, DomainKingSkills.YUTA, DomainKingSkills.SIMPLE),
            DomainKingStory.chapters(), DomainKingStyle.config());
    }
    private DomainKingCharacter() {}
}
