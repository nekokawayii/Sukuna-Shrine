package character.yuki;

import character.CharacterSkill;
import java.util.List;

/**
 * 九十九由基 · 星之怒。
 * <p>定位就是「数值怪」：招式少、机制简单，但每一项倍率与基础伤害都拉满。
 * <p>四招：质量拳（锥形重击）→ 迦楼罗（贯穿射线）→ 加重（自身增幅）→ 黑洞（大范围奇点）。
 */
public final class YukiSkills {
    public static final int MASS_FIST = 2000;
    public static final int GARUDA = 2001;
    public static final int STAR_RAGE = 2002;
    public static final int BLACK_HOLE = 2003;

    public static List<CharacterSkill> all() {
        return List.of(
            // 编号、全名、轮盘短名、说明、颜色、消耗、冷却、形状、距离、半径、半角、伤害、每章加伤、目标数、每章加目标、减速刻数、减速等级
            new CharacterSkill(MASS_FIST, "星之怒·质量拳", "质量拳",
                "把质量压进拳里：前方锥形重击，伤害与击退都极高。",
                0xFFFFD27F, 200, 30, CharacterSkill.Shape.CONE, 7, 3.5, 55, 300, 80, 6, 1, 20, 2),
            new CharacterSkill(GARUDA, "星之怒·迦楼罗", "迦楼罗",
                "召出式神迦楼罗沿准星贯穿，沿途所有敌人承受巨量伤害。",
                0xFFFFE9A8, 400, 90, CharacterSkill.Shape.RAY, 22, 2.6, 12, 450, 120, 8, 1, 0, 0),
            new CharacterSkill(STAR_RAGE, "星之怒·加重", "加重",
                "给自己叠加质量：攻击与抗性大幅提升，代价是移动变慢。",
                0xFFFFC46B, 500, 220, CharacterSkill.Shape.BURST, 1, 1, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(BLACK_HOLE, "星之怒·黑洞", "黑洞",
                "把全部质量压成奇点：大范围牵引并造成毁灭性伤害，自身陷入虚弱。",
                0xFF2B1B3D, 2500, 600, CharacterSkill.Shape.BURST, 14, 12, 180, 1200, 300, 40, 0, 60, 3)
        );
    }

    private YukiSkills() {}
}
