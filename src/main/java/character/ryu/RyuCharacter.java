package character.ryu;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/** 石流龙：咒力强化/放出（咒力超载：上限极高、回复极慢）。 */
public final class RyuCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "ryu", "石流龙", "咒力强化/放出",
            "咒力超载：咒力上限极高（10000）但回复极慢（每秒 5）。轮盘：冰沙冲击波（可蓄力光炮）/ 追踪弹 / 咒力喷涌 / 甜品时间 / 领域展开（占位）。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFF9FDFFF,
            List.of(RyuSkills.BLAST, RyuSkills.SCATTER, RyuSkills.BURST, RyuSkills.DESSERT, RyuSkills.DOMAIN),
            RyuStory.chapters(), RyuStyle.config());
    }
    private RyuCharacter() {}
}
