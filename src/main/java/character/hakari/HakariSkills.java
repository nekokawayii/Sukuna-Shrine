package character.hakari;

import character.CharacterSkill;
import java.util.List;

/**
 * 秤金次 · 坐杀博打。
 * <p>玩法：平时只有咒力强化的拳脚（软），靠「开盘」赌中头奖，
 * 中奖后 4 分 11 秒无限咒力 + 高速自愈 + 双倍伤害。
 * <p>抽奖带保底：每失败一次概率 +5%，最多 95%。
 */
public final class HakariSkills {
    /** 赌拳：CE 强化直拳。 */
    public static final int PUNCH = 3000;
    /** 坐杀博打·开盘：主动抽奖。 */
    public static final int SPIN = 3001;
    /** 领域展开·坐杀博打。 */
    public static final int DOMAIN = 3002;
    /** 必杀·连打：短时间多段重拳。 */
    public static final int BARRAGE = 3003;

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(PUNCH, "咒力拳", "咒力拳",
                "把咒力压进拳骨：前方锥形重击，中头奖期间伤害翻倍。",
                0xFFFFD86B, 150, 8, CharacterSkill.Shape.CONE, 5, 3, 55, 200, 60, 6, 1, 10, 1),
            new CharacterSkill(SPIN, "坐杀博打·开盘", "开盘",
                "押上咒力抽一次：中奖率 10%，每失败一次 +5%，命中即进入 4 分 11 秒头奖。",
                0xFFFF7A3D, 600, 40, CharacterSkill.Shape.BURST, 1, 1, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(DOMAIN, "领域展开·坐杀博打", "坐杀博打",
                "展开赌场领域：每 2 秒自动开奖一次，中奖后无限咒力。",
                0xFFB02A1E, 1500, 2400, CharacterSkill.Shape.BURST, 1, 1, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(BARRAGE, "必杀·连打", "连打",
                "连续挥出五记重拳，每拳都吃头奖加成；冷却 20 秒。",
                0xFFFFB35C, 400, 400, CharacterSkill.Shape.CONE, 5, 3, 55, 150, 40, 8, 1, 0, 0)
        );
    }

    private HakariSkills() {}
}
