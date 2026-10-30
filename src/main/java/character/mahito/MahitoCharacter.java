package character.mahito;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/** 真人：选择书里显示「无为转变」，轮盘只有两招（功能待定）。 */
public final class MahitoCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "mahito", "真人", "无为转变",
            "由人类恐惧诞生的特级咒灵，能随意改变灵魂的形态。轮盘：无为转变 / 自闭圆盾裹。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFF9FD7E8,
            List.of(MahitoSkills.TRANSMUTE, MahitoSkills.SELF_EMBRYO),
            MahitoStory.chapters(), MahitoStyle.config());
    }
    private MahitoCharacter() {}
}
