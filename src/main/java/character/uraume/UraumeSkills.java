package character.uraume;

import character.CharacterSkill;
import java.util.List;

/** 里梅 · 冰凝咒法（r289 迁到新角色框架）：6 格轮盘。 */
public final class UraumeSkills {
    public static final int FROST = 2198, ICEFALL = 2199, PRISON = 2200, STORM = 2201, DRAGON = 2202, ICEAGE = 2203, DOMAIN = 2204;

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(FROST, "冰凝咒法 · 霜凪", "霜凪",
                "前方锥形寒气持续 5 秒，命中叠霜寒；领域内范围扩大到整个领域。",
                0xFFA8EEFF, 200, 60, CharacterSkill.Shape.CONE, 14, 8, 40, 10, 2, 8, 0, 60, 1),
            new CharacterSkill(ICEFALL, "冰凝咒法 · 直瀑", "直瀑",
                "在准星位置降下冰柱，半径 5 格散射；领域内自动追踪必中。",
                0xFFCDF4FF, 260, 100, CharacterSkill.Shape.BURST, 30, 5, 45, 16, 3, 6, 0, 80, 2),
            new CharacterSkill(PRISON, "冰凝咒法 · 千手冰牢", "冰牢",
                "目标脚下冰手印合拢，范围内敌人被冻住并定身 2 秒（无视霸体）。",
                0xFFE6FBFF, 320, 240, CharacterSkill.Shape.BURST, 12, 4.5, 60, 14, 4, 8, 0, 40, 2),
            new CharacterSkill(STORM, "冰凝咒法 · 急冻风暴", "风暴",
                "以自身为中心 5 秒冰雪龙卷，持续把敌人吸向中心并造成切割伤害。",
                0xFF9FE4FF, 400, 360, CharacterSkill.Shape.BURST, 9, 8, 90, 4, 1, 32, 0, 20, 1),
            new CharacterSkill(DRAGON, "冰凝咒法 · 冰龙", "冰龙",
                "召唤冰霜巨龙向前贯穿 20 格，极高击退并叠两层霜寒。",
                0xFFBDEBFF, 500, 420, CharacterSkill.Shape.RAY, 20, 3.5, 45, 25, 5, 8, 0, 40, 2),
            new CharacterSkill(DOMAIN, "领域展开 · 霜天宫殿", "霜天宫殿",
                "展开深蓝黑冰宫殿：界内敌人无法跳跃/飞行、霜寒永久满层、冰系伤害翻倍。",
                0xFF7FC8FF, 1000, 1200, CharacterSkill.Shape.BURST, 26, 8, 90, 0, 0, 32, 0, 0, 0),
            new CharacterSkill(ICEAGE, "极之番 · 大冰河时代", "大冰河",
                "双手按地，50 格内所有生物与方块冻结成冰雕；自身 30 秒严重冻伤、无法施术。",
                0xFF8FD8FF, 1200, 1200, CharacterSkill.Shape.BURST, 32, 8, 90, 40, 10, 32, 0, 200, 3)
        );
    }

    private UraumeSkills() { }
}
