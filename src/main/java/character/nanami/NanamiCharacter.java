package character.nanami;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/** 七海建人：十划咒法（7:3 比例的弱点）。 */
public final class NanamiCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "nanami", "七海建人", "十划咒法",
            "把对象的长度等分为十份，在 7:3 的比例处制造弱点：命中最靠近准星的敌人的那条分割线，"
                + "无视 70% 防御并打出 ×3 真实伤害。轮盘：强打 / 瓦落瓦落 / 隙 / 加班。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFFD8B24A,
            List.of(NanamiSkills.SMASH, NanamiSkills.VALOR, NanamiSkills.GAP, NanamiSkills.OVERTIME),
            NanamiStory.chapters(), NanamiStyle.config());
    }
    private NanamiCharacter() {}
}
