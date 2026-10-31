package character.hakari;

import character.CharacterSkill;
import java.util.List;

/**
 * 秤金次 · 坐杀博徒（r371 大改）。
 *
 * <p>**未中奖**轮盘 7 格：车门猛砸 / 车门格挡 / 列车冲撞 / 钢珠散射 / 幸运硬币 / 出老千 / 领域展开；
 * **中奖**轮盘 3 格：无限平推（被动标记）/ 纯爱列车·全速前进 / 逆转反击（被动标记）。
 * 客户端按中奖状态自动切换（`HakariState.jackpot`）。
 */
public final class HakariSkills {
    // ── 未中奖轮盘 ──
    public static final int DOOR_SMASH = 3000;      // 1 车门猛砸
    public static final int DOOR_GUARD = 3001;      // 3 车门格挡
    public static final int TRAIN_RUSH = 3002;      // 4 列车冲撞
    public static final int STEEL_BALLS = 3003;     // 5 钢珠散射
    public static final int LUCKY_COIN = 3004;      // 6 幸运硬币
    public static final int CHEAT = 3005;           // 7 出老千
    public static final int DOMAIN = 3006;          // 领域展开·坐杀博徒
    // ── 中奖轮盘 ──
    public static final int ENDLESS = 3010;         // 9 无限咒力平推（被动标记）
    public static final int LOVE_TRAIN = 3011;      // 10 纯爱列车·全速前进
    public static final int REVERSAL = 3012;        // 11 逆转反击（被动标记）

    public static List<Integer> baseWheel() {
        return List.of(DOOR_SMASH, DOOR_GUARD, TRAIN_RUSH, STEEL_BALLS, LUCKY_COIN, CHEAT, DOMAIN);
    }

    public static List<Integer> jackpotWheel() {
        return List.of(ENDLESS, LOVE_TRAIN, REVERSAL);
    }

    /** 定义里放两份并集（轮盘实际显示哪份由 CharacterClient 按状态决定）。 */
    public static List<Integer> allIds() {
        return List.of(DOOR_SMASH, DOOR_GUARD, TRAIN_RUSH, STEEL_BALLS, LUCKY_COIN, CHEAT, DOMAIN,
            ENDLESS, LOVE_TRAIN, REVERSAL);
    }

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(DOOR_SMASH, "车门猛砸", "车门猛砸",
                "凭空召唤私铁纯爱列车的厚重车门向前猛砸：高额伤害并击飞，老虎机大幅拨动一格。",
                0xFFFFC44D, 200, 60, CharacterSkill.Shape.CONE, 6, 3, 60, 26, 5, 6, 1, 20, 1),
            new CharacterSkill(DOOR_GUARD, "车门格挡", "车门格挡",
                "把车门插在身前 3 秒：免疫正面伤害；格挡成功时拨动老虎机、敌人僵直，并接一记重拳。",
                0xFFB9C6D8, 120, 80, CharacterSkill.Shape.BURST, 2, 2, 180, 8, 2, 1, 0, 0, 0),
            new CharacterSkill(TRAIN_RUSH, "列车冲撞", "列车冲撞",
                "被幻影列车包裹向前高速冲撞 14 格，撞飞路径上所有敌人并拨动老虎机。",
                0xFF7CE38B, 220, 100, CharacterSkill.Shape.RAY, 14, 2, 30, 22, 4, 8, 1, 0, 0),
            new CharacterSkill(STEEL_BALLS, "钢珠散射", "钢珠散射",
                "双手像发射器一样抛出漫天的柏青哥钢珠：大范围弹幕伤害，每次命中都有概率拨动老虎机。",
                0xFFE0E0E0, 260, 120, CharacterSkill.Shape.CONE, 12, 5, 50, 12, 3, 10, 2, 0, 0),
            new CharacterSkill(LUCKY_COIN, "幸运硬币", "幸运硬币",
                "抛起一枚硬币：接下来 5 秒所有拨动老虎机的效率翻倍。",
                0xFFFFE066, 300, 300, CharacterSkill.Shape.BURST, 1, 1, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(CHEAT, "出老千", "出老千",
                "差一个图案就中奖时重掷：清空当前计数、短时间移速提升；代价是大量咒力。",
                0xFFFF7A7A, 800, 200, CharacterSkill.Shape.BURST, 1, 1, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(DOMAIN, "领域展开·坐杀博徒", "坐杀博徒",
                "展开柏青哥赌场领域：领域内攻击命中即拨动老虎机（敌方屏幕也会出现滚动图案），"
                    + "打满 30 次必定中奖；对拼必胜。",
                0xFFB02A1E, 1200, 2400, CharacterSkill.Shape.BURST, 8, 2, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(ENDLESS, "无限咒力平推", "无限平推",
                "中奖期间被动：咒力无限、平A与基础技能冷却 -90%，可以无限砸车门、扔钢珠。",
                0xFFFFD700, 0, 1, CharacterSkill.Shape.BURST, 1, 1, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(LOVE_TRAIN, "纯爱列车·全速前进", "纯爱列车",
                "双手拍地召唤巨大的私铁纯爱列车沿直线碾压，毁灭性伤害、沿途地形全毁；中奖期间可以无限释放。",
                0xFF35E06A, 0, 1, CharacterSkill.Shape.RAY, 32, 3, 20, 120, 30, 12, 2, 0, 0),
            new CharacterSkill(REVERSAL, "逆转反击", "逆转反击",
                "中奖期间被动：受到致命伤害不会被杀，反而强制再拨动三格并回复生命。",
                0xFF9BE8FF, 0, 1, CharacterSkill.Shape.BURST, 1, 1, 180, 0, 0, 1, 0, 0, 0));
    }

    private HakariSkills() {}
}
