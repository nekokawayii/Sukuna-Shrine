package character.domainking;

import character.CharacterSkill;
import java.util.List;

/** 领域王：轮盘里全是「领域」。cost 全 0、咒力视为无限。 */
public final class DomainKingSkills {
    public static final int JOGO_DOMAIN = 2000;
    public static final int VOID = 2001;
    public static final int SHRINE = 2002;
    public static final int BLOOD = 2003;
    public static final int YUTA = 2004;
    public static final int SIMPLE = 2005;

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(JOGO_DOMAIN, "盖棺铁围山", "盖棺铁围山",
                "化出岩浆海与黑石穹顶，界内敌人持续被灼烧。", 0xFFFF6A1E, 0, 1,
                CharacterSkill.Shape.BURST, 20, 1, 180, 12, 2, 32, 0, 20, 1),
            new CharacterSkill(VOID, "无量空处", "无量空处",
                "灌入无限信息，界内敌人被定身并缓慢失去意识。", 0xFF8F7BFF, 0, 1,
                CharacterSkill.Shape.BURST, 20, 1, 180, 10, 1, 32, 0, 60, 2),
            new CharacterSkill(SHRINE, "伏魔御厨子", "伏魔御厨子",
                "无边界斩击领域，界内一切持续被「解」与「捌」切割。", 0xFFFF3A3A, 0, 1,
                CharacterSkill.Shape.BURST, 20, 1, 180, 14, 2, 32, 0, 0, 0),
            new CharacterSkill(BLOOD, "血之领域·胎海", "胎海",
                "血色海洋吞没场地，界内敌人被减速并持续失血。", 0xFFB01030, 0, 1,
                CharacterSkill.Shape.BURST, 20, 1, 180, 9, 1, 32, 0, 40, 1),
            new CharacterSkill(YUTA, "领域展开·真赝相爱", "真赝相爱",
                "赝品与真品交错，界内敌人承受到复制术式的连续追击。", 0xFF66D2FF, 0, 1,
                CharacterSkill.Shape.BURST, 20, 1, 180, 11, 1, 32, 0, 0, 0),
            new CharacterSkill(SIMPLE, "简易领域·捌", "简易领域·捌",
                "以自身为中心张开简易领域，持续撕裂靠近的敌人。", 0xFFFFD36A, 0, 1,
                CharacterSkill.Shape.BURST, 8, 1, 180, 8, 1, 32, 0, 10, 0)
        );
    }
    private DomainKingSkills() {}
}
