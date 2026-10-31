package character.todo;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/** 东堂葵：不义游戏（注入咒力 / 换位）。 */
public final class TodoCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "todo", "东堂葵", "不义游戏",
            "术式是「不义游戏」：生物默认自带咒力；给方块、弹幕注入咒力（永久有效），再与准星最近的有咒力的东西换位；"
                + "换位无冷却，前后各 0.3 秒免疫伤害。轮盘：1 注入咒力 / 2 换位。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFFB58CFF,
            List.of(TodoSkills.INJECT, TodoSkills.SWAP),
            TodoStory.chapters(), TodoStyle.config());
    }
    private TodoCharacter() {}
}
