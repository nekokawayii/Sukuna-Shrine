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
            "由人类恐惧诞生的特级咒灵。灵魂触碰、改造人偶、淀月、肉体崩坏、地刺、灵魂闪避与重铸，领域自闭圆顿裹。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFF9FD7E8,
            List.of(MahitoSkills.TRANSMUTE,MahitoSkills.TAIL,MahitoSkills.COLLAPSE,MahitoSkills.ARMS,MahitoSkills.DODGE,MahitoSkills.REFORGE,MahitoSkills.SELF_EMBRYO),
            MahitoStory.chapters(), MahitoStyle.config());
    }
    private MahitoCharacter() {}
}
