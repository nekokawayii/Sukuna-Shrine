package character.yuki;

import character.CharacterSkill;
import java.util.List;

/**
 * 九十九由基 · 星之怒。
 * <p>定位「数值怪」：招式少但要叠层。
 * <p>三招：质量拳（锥形重击）→ 迦楼罗（贯穿射线）→ 加重（可叠 10 层，第 11 次触发黑洞）。
 */
public final class YukiSkills {
    public static final int MASS_FIST = 2000;
    public static final int GARUDA = 2001;
    /** 加重：叠满 10 层后，第 11 次点击触发黑洞（不再是独立技能）。 */
    public static final int STAR_RAGE = 2002;
    /** 星崩拳：质量拳的五倍伤害、无视防御、保底 20% 目标生命，冷却 30 秒。 */
    public static final int STAR_CRASH = 2003;
    /** 领域展开·星轨坠亡。 */
    public static final int DOMAIN = 2004;

    public static List<CharacterSkill> all() {
        return List.of(
            // 编号、全名、轮盘短名、说明、颜色、消耗、冷却、形状、距离、半径、半角、伤害、每章加伤、目标数、每章加目标、减速刻数、减速等级
            new CharacterSkill(STAR_CRASH, "星之怒·星崩拳", "星崩拳",
                "质量拳的五倍重击：无视防御与一切限伤，至少造成目标 20% 生命上限的伤害；冷却 30 秒。",
                0xFFFFB35C, 800, 600, CharacterSkill.Shape.CONE, 8, 4, 60, 1500, 400, 10, 1, 40, 3),
            new CharacterSkill(MASS_FIST, "星之怒·质量拳", "质量拳",
                "把质量压进拳里：前方锥形重击，伤害随「加重」层数暴涨。",
                0xFFFFD27F, 200, 10, CharacterSkill.Shape.CONE, 7, 3.5, 55, 300, 80, 8, 1, 20, 2),
            new CharacterSkill(GARUDA, "星之怒·迦楼罗", "迦楼罗",
                "召出式神迦楼罗沿准星贯穿，沿途所有敌人承受巨量伤害，同样吃加重层数。",
                0xFFFFE9A8, 400, 60, CharacterSkill.Shape.RAY, 22, 2.6, 12, 450, 120, 10, 1, 0, 0),
            new CharacterSkill(STAR_RAGE, "星之怒·加重", "加重",
                "给自己叠加质量，最多 10 层，每层大幅提高伤害；第 11 次会把质量压成黑洞。",
                0xFFFFC46B, 500, 20, CharacterSkill.Shape.BURST, 1, 1, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(DOMAIN, "领域展开·星轨坠亡", "星轨坠亡",
                "展开星空领域（内外两层星轨壁 + 星空地板）；领域内敌人无法移动且虚弱，生命低于你的会被压扁致死。",
                0xFF1B2A6B, 1000, 1200, CharacterSkill.Shape.BURST, 1, 1, 180, 0, 0, 1, 0, 0, 0)
        );
    }

    private YukiSkills() {}
}
