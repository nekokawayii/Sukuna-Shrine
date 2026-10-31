package character.megumi;

import character.CharacterSkill;
import java.util.List;

/** 伏黑惠：十种影法术。释放键直接打开十影界面（技能本体只负责开界面）。 */
public final class MegumiSkills {
    public static final int TEN_SHADOWS = 2200;

    public static List<CharacterSkill> all() {
        return List.of(new CharacterSkill(TEN_SHADOWS, "十种影法术", "十影",
            "以影子召唤十种式神；按释放键直接打开十影界面。", 0xFF5A6B7A, 200, 20,
            CharacterSkill.Shape.BURST, 8, 1, 180, 0, 0, 32, 0, 0, 0));
    }
    private MegumiSkills() {}
}
