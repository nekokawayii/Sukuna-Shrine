package character.yuki;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/**
 * 九十九由基：特级咒术师，术式「星之怒」。
 * <p>玩法定位是数值怪——四招、机制直白，但单发伤害与属性成长都拉满。
 */
public final class YukiCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "yuki", "九十九由基", "星之怒",
            "特级咒术师。给自身与式神叠加质量换取毁灭性打击：质量拳、迦楼罗、加重、黑洞。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFFFFC46B,
            List.of(YukiSkills.MASS_FIST, YukiSkills.GARUDA, YukiSkills.STAR_RAGE, YukiSkills.BLACK_HOLE),
            YukiStory.chapters(), YukiStyle.config());
    }

    private YukiCharacter() {}
}
