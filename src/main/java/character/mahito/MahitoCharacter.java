package character.mahito;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/** 真人：选择书里显示「无为转变」，轮盘 13 格（含无为转变的肉墙／肉桥／治疗／爆裂／分身／潜能激发）。 */
public final class MahitoCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "mahito", "真人", "无为转变",
            "由人类恐惧诞生的特级咒灵。灵魂触碰、改造人偶、淀月、肉体崩坏、地刺、灵魂闪避与重铸、改造肉墙与肉桥、灵魂治疗、肉体爆裂、分身、潜能激发，领域自闭圆顿裹。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFF9FD7E8,
            List.of(MahitoSkills.TRANSMUTE,MahitoSkills.TAIL,MahitoSkills.COLLAPSE,MahitoSkills.ARMS,MahitoSkills.DODGE,
                MahitoSkills.REFORGE,MahitoSkills.WALL,MahitoSkills.BRIDGE,MahitoSkills.HEAL,MahitoSkills.BURST,
                MahitoSkills.CLONE,MahitoSkills.POTENTIAL,MahitoSkills.SELF_EMBRYO),
            MahitoStory.chapters(), MahitoStyle.config());
    }
    private MahitoCharacter() {}
}
