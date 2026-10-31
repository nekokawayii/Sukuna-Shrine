package character.higuruma;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/** 日车宽见：诛伏赐死，选中后获得不可丢弃、无限耐久的法槌。 */
public final class HigurumaCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "higuruma", "日车宽见", "诛伏赐死",
            "律师出身的咒术师。选中后获得法槌（不可丢弃、无限耐久），轮盘：刻印 / 强化 / 领域展开·诛伏赐死。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFFC9A227,
            List.of(HigurumaSkills.SEAL, HigurumaSkills.ENHANCE, HigurumaSkills.DOMAIN),
            HigurumaStory.chapters(), HigurumaStyle.config());
    }
    private HigurumaCharacter() {}
}
