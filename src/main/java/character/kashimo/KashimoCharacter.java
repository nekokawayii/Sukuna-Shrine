package character.kashimo;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/**
 * 鹿紫云一：幻兽琥珀。电荷与必中。
 * <p>任何攻击命中都会给敌人挂「正电荷」（最多 3 层、10 秒），自身积累「负电荷」；
 * 轮盘 5 格 + 轮盘中心终奥「幻兽·琥珀」。
 */
public final class KashimoCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "kashimo", "鹿紫云一", "幻兽琥珀",
            "雷电系的格斗家。任何攻击命中都给敌人挂正电荷（最多3层·10秒），自身积累负电荷（移速提升、屏幕电弧变亮）。"
                + "轮盘：雷走 / 放电 / 电磁吸附 / 雷神·附着 / 引爆；轮盘中心：幻兽·琥珀。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFF7FD8FF,
            List.of(KashimoSkills.DASH, KashimoSkills.DISCHARGE, KashimoSkills.ATTRACT,
                    KashimoSkills.ATTACH, KashimoSkills.DETONATE),
            KashimoStory.chapters(), KashimoStyle.config());
    }
    private KashimoCharacter() {}
}
