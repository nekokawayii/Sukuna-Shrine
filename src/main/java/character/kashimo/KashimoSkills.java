package character.kashimo;

import character.CharacterSkill;
import java.util.List;

/**
 * 鹿紫云一：幻兽琥珀。
 * <p>轮盘 5 格：1 雷走（位移）/ 2 放电（AOE）/ 3 电磁吸附（控制）/ 4 雷神·附着（近战强化）/ 5 引爆（必中雷击）。
 * 终奥「幻兽·琥珀」不在轮盘格上，走轮盘中心（{@link KashimoCombat#ULT}），由客户端发 phase=7 请求。
 */
public final class KashimoSkills {
    public static final int DASH = 2400, DISCHARGE = 2401, ATTRACT = 2402, ATTACH = 2403, DETONATE = 2404;

    /** 能量数值都写在这里，方便按手感调。 */
    public static final double DASH_LAND_DAMAGE = 14;      // 雷走落点雷击
    public static final double DISCHARGE_DAMAGE = 16;      // 放电伤害
    public static final double ATTRACT_HIT_DAMAGE = 30;    // 吸附后的铁棒重击
    public static final double DETONATE_DAMAGE = 45;       // 引爆：每层正电荷一道雷的基础伤害
    public static final double DETONATE_PER_NEG = 2;       // 每层负电荷给每道雷加多少伤害
    public static final double ULT_BOLT_DAMAGE = 30;       // 琥珀期间每次普攻自带的那道雷

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(DASH, "雷走", "雷走",
                "化作一道闪电向前突进，落点炸开小范围雷击并挂正电荷；冷却短，是主要的起手与机动。",
                0xFF7FD8FF, 120, 50, CharacterSkill.Shape.BURST,
                10, 3, 180, DASH_LAND_DAMAGE, 3, 8, 4, 0, 0),
            new CharacterSkill(DISCHARGE, "放电", "放电",
                "以自身为中心释放环形电流，范围内所有敌人小额伤害并各挂一层正电荷。",
                0xFF5CC6FF, 180, 100, CharacterSkill.Shape.BURST,
                6, 6, 180, DISCHARGE_DAMAGE, 4, 32, 0, 0, 0),
            new CharacterSkill(ATTRACT, "电磁吸附", "吸附",
                "对单个带正电荷的敌人使用：异性相吸把它强行拉到面前，并接一记铁棒重击。",
                0xFFB9E8FF, 220, 140, CharacterSkill.Shape.RAY,
                24, 1.5, 40, ATTRACT_HIT_DAMAGE, 5, 1, 0, 0, 0),
            new CharacterSkill(ATTACH, "雷神·附着", "附着",
                "10 秒内武器缠绕高压电弧：普攻范围扩大，且每次普攻额外挂一层正电荷。",
                0xFFFFF3B0, 300, 400, CharacterSkill.Shape.BURST,
                8, 5, 120, 0, 0, 8, 0, 0, 0),
            new CharacterSkill(DETONATE, "引爆", "引爆",
                "无视距离、地形与闪避：所有带正电荷的敌人各按层数被落雷劈中。",
                0xFFFFFFFF, 200, 200, CharacterSkill.Shape.BURST,
                32, 1, 180, DETONATE_DAMAGE, 5, 32, 0, 0, 0)
        );
    }
    private KashimoSkills() {}
}
