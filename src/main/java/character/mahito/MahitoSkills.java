package character.mahito;

import character.CharacterSkill;
import java.util.List;

/** 真人：无为转变 / 自闭圆盾裹。技能本体消耗：无为转变 300、自闭圆盾裹 1500（轮盘各形态另计）。 */
public final class MahitoSkills {
    public static final int TRANSMUTE = 2100;   // 无为转变
    public static final int SELF_EMBRYO = 2101; // 自闭圆盾裹

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(TRANSMUTE, "无为转变", "无为转变",
                "触碰灵魂的形态，任意改变生命与咒力的形状。（功能待定）", 0xFF9FD7E8, 300, 1,
                CharacterSkill.Shape.BURST, 8, 1, 180, 0, 0, 32, 0, 0, 0),
            new CharacterSkill(SELF_EMBRYO, "自闭圆盾裹", "自闭圆盾裹",
                "与领域王同款：半径 30 的黑混凝土穹顶 + 肉色网格，身后立着圆钝果实。再按一次收起。", 0xFF2A2A2E, 1500, 1,
                CharacterSkill.Shape.BURST, 20, 1, 180, 0, 0, 32, 0, 0, 0)
        );
    }
    private MahitoSkills() {}
}
