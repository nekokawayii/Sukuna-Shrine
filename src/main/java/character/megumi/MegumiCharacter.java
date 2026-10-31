package character.megumi;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/** 伏黑惠：释放键直接打开十种影法术界面。 */
public final class MegumiCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "megumi", "伏黑惠", "十种影法术",
            "禅院家的影术师，以影子召唤式神。释放键直接打开十影界面。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFF5A6B7A,
            List.of(MegumiSkills.TEN_SHADOWS),
            MegumiStory.chapters(), MegumiStyle.config());
    }
    private MegumiCharacter() {}
}
