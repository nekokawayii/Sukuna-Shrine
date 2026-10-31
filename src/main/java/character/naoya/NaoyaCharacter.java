package character.naoya;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;

/** 禅院直哉：投射咒法（把动作拆成帧、在帧之间移动）。 */
public final class NaoyaCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "naoya", "禅院直哉", "投射咒法",
            "把一秒切成二十四帧、在帧与帧之间移动：设定投影（记录 10 秒内的移动节点）→ 释放（在节点间穿梭，"
                + "沿途敌人受伤并挂上「帧数泥沼」）。右半轮盘是五个投射技：直线投射 / Z 字折返 / 强制同化 / 视距截断 / 空中走位。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFF7FD8FF,
            NaoyaSkills.wheel(), NaoyaStory.chapters(), NaoyaStyle.config());
    }

    private NaoyaCharacter() {}
}
