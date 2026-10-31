package character.ryu;

import character.CharacterSkill;
import java.util.List;

/** 石流龙 · 咒力强化/放出：1 冰沙冲击波 / 2 追踪弹 / 3 咒力喷涌 / 4 甜品时间 / 5 领域展开（占位）。 */
public final class RyuSkills {
    public static final int BLAST = 2700, SCATTER = 2701, BURST = 2702, DESSERT = 2703, DOMAIN = 2704;

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(BLAST, "冰沙冲击波", "冲击波",
                "按住持续喷出 3D 咒力冲击波：宽度从 1 格起、每 2 秒 +1 格（无上限），持续期间每秒消耗 100 咒力。",
                0xFF9FDFFF, 0, 1, CharacterSkill.Shape.RAY,
                30, 1.5, 15, 45, 8, 32, 0, 0, 0),
            new CharacterSkill(SCATTER, "追踪弹", "追踪弹",
                "连续射出 3~5 发小型咒力弹，自动追踪附近敌人；单发伤害低但几乎不会落空。",
                0xFFCDEFFF, 300, 80, CharacterSkill.Shape.RAY,
                24, 1, 30, 18, 4, 5, 0, 0, 0),
            new CharacterSkill(BURST, "咒力喷涌", "喷涌",
                "不结印，全身爆开咒力把周围 3 格内的敌人震飞；不消耗咒力，是被近身时的解围技。",
                0xFFBFF4FF, 0, 200, CharacterSkill.Shape.BURST,
                3, 3, 180, 20, 4, 32, 0, 0, 0),
            new CharacterSkill(DESSERT, "甜品时间", "甜品",
                "掏出蛋糕吃掉：2 秒内不攻击，咒力回复速度翻倍；高风险高回报。",
                0xFFFFD9A0, 0, 400, CharacterSkill.Shape.BURST,
                4, 1, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(DOMAIN, "领域展开", "领域（占位）",
                "占位技能：目前只播放起手与提示，领域内容尚未制作。",
                0xFF7FB8E8, 4000, 2400, CharacterSkill.Shape.BURST,
                8, 8, 180, 0, 0, 1, 0, 0, 0)
        );
    }
    private RyuSkills() {}
}
