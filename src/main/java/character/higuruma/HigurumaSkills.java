package character.higuruma;

import character.CharacterSkill;
import java.util.List;

/** 日车宽见：诛伏赐死。1 刻印 / 2 强化 / 3 领域展开·诛伏赐死。 */
public final class HigurumaSkills {
    public static final int SEAL = 2300, ENHANCE = 2301, DOMAIN = 2302;

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(SEAL, "刻印", "刻印",
                "把副手武器的伤害、攻速与特殊 NBT 全部刻进法槌。", 0xFFC9A227, 150, 10,
                CharacterSkill.Shape.BURST, 8, 1, 180, 0, 0, 32, 0, 0, 0),
            new CharacterSkill(ENHANCE, "强化", "强化",
                "强化法槌：如忠诚三叉戟般掷出后自动飞回，无需蓄力。", 0xFFD9B24C, 300, 40,
                CharacterSkill.Shape.BURST, 8, 1, 180, 0, 0, 32, 0, 0, 0),
            new CharacterSkill(DOMAIN, "领域展开·诛伏赐死", "诛伏赐死",
                "外侧黑色混凝土，内部法庭，法庭四周环绕一圈 15 格高的断头台。", 0xFF2A2A2E, 1200, 600,
                CharacterSkill.Shape.BURST, 20, 1, 180, 0, 0, 32, 0, 0, 0)
        );
    }
    private HigurumaSkills() {}
}
