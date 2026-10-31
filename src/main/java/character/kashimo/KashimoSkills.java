package character.kashimo;

import character.CharacterSkill;
import java.util.List;

/**
 * 鹿紫云一：幻兽琥珀。
 * <p>轮盘 10 格：1 雷走 / 2 放电 / 3 电磁吸附 / 4 雷神·附着 / 5 引爆 /
 * 6 静电立场 / 7 落雷 / 8 球形闪电 / 9 雷霆万钧 / 10 雷影化。
 * 终奥「幻兽·琥珀」不在格子上，走轮盘中心（{@link KashimoCombat#ULT}）；
 * 同极相斥是防御键的即时反应（{@link KashimoCombat#GUARD}），同样不在轮盘里。
 */
public final class KashimoSkills {
    public static final int DASH = 2400, DISCHARGE = 2401, ATTRACT = 2402, ATTACH = 2403, DETONATE = 2404;
    public static final int FIELD = 2406, BOLT = 2407, ORB = 2408, CHAIN = 2409, SHADOW = 2410;

    /** 能量数值都写在这里，方便按手感调。 */
    public static final double DASH_LAND_DAMAGE = 14;      // 雷走落点雷击
    public static final double DISCHARGE_DAMAGE = 16;      // 放电伤害
    public static final double ATTRACT_HIT_DAMAGE = 30;    // 吸附后的铁棒重击
    public static final double DETONATE_DAMAGE = 45;       // 引爆：每层正电荷一道雷的基础伤害
    public static final double DETONATE_PER_NEG = 2;       // 每层负电荷给每道雷加多少伤害
    public static final double ULT_BOLT_DAMAGE = 30;       // 琥珀期间每次普攻自带的那道雷
    public static final double BOLT_DAMAGE = 70;           // 落雷（延迟 1 秒）
    public static final double ORB_DAMAGE = 60;            // 球形闪电爆炸
    public static final double CHAIN_DAMAGE = 30;          // 雷霆万钧第一跳，之后每跳 ×1.35
    public static final double FLING_DAMAGE = 40;          // 电磁吸附·极落点爆炸

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(DASH, "雷走", "雷走",
                "化作一道闪电向前突进，落点炸开小范围雷击并挂正电荷；冷却短，是主要的起手与机动。",
                0xFF7FD8FF, 120, 50, CharacterSkill.Shape.BURST,
                10, 3, 180, DASH_LAND_DAMAGE, 3, 8, 4, 0, 0),
            new CharacterSkill(DISCHARGE, "放电", "放电",
                "以自身为中心释放环形电流，范围内所有敌人小额伤害并各挂一层正电荷；在水里范围×3、伤害减半。",
                0xFF5CC6FF, 180, 100, CharacterSkill.Shape.BURST,
                6, 8, 180, DISCHARGE_DAMAGE, 4, 32, 0, 0, 0),
            new CharacterSkill(ATTRACT, "电磁吸附", "吸附",
                "对单个带正电荷的敌人使用：异性相吸把它拉到面前重击；满 3 层时改为甩向准星并落地爆炸。",
                0xFFB9E8FF, 220, 140, CharacterSkill.Shape.RAY,
                24, 1.5, 40, ATTRACT_HIT_DAMAGE, 5, 1, 0, 0, 0),
            new CharacterSkill(ATTACH, "雷神·附着", "附着",
                "10 秒内武器缠绕高压电弧：普攻范围扩大、每次普攻挂 2 层，第三下变成上挑把敌人打飞。",
                0xFFFFF3B0, 300, 400, CharacterSkill.Shape.BURST,
                8, 5, 120, 0, 0, 8, 0, 0, 0),
            new CharacterSkill(DETONATE, "引爆", "引爆",
                "无视距离、地形与闪避：所有带正电荷的敌人各按层数被落雷劈中。",
                0xFFFFFFFF, 200, 200, CharacterSkill.Shape.BURST,
                32, 1, 180, DETONATE_DAMAGE, 5, 32, 0, 0, 0),
            new CharacterSkill(FIELD, "静电立场", "静电场",
                "插下一根铁棒，形成半径 10 格的静电场：场内敌人持续挂正电荷，自己站在里面咒力回复翻倍。",
                0xFFA8E6FF, 350, 480, CharacterSkill.Shape.BURST,
                8, 8, 180, 0, 0, 32, 0, 0, 0),
            new CharacterSkill(BOLT, "落雷", "落雷",
                "向准星所指处发射电磁波，1 秒后一道粗雷落下；区域内带正电荷的敌人会被自动追踪。",
                0xFFBFF4FF, 400, 200, CharacterSkill.Shape.RAY,
                32, 5, 20, BOLT_DAMAGE, 6, 8, 0, 0, 0),
            new CharacterSkill(ORB, "球形闪电", "球雷",
                "放出一颗缓慢移动的巨大电球：碰到方块反弹、碰到敌人持续电击，5 秒后大爆炸。",
                0xFF9FDFFF, 500, 300, CharacterSkill.Shape.RAY,
                32, 6, 15, ORB_DAMAGE, 6, 32, 0, 0, 0),
            new CharacterSkill(CHAIN, "雷霆万钧", "雷霆万钧",
                "原地蓄力 2 秒，随后向四周爆出连锁闪电：最多弹射 5 次，每跳伤害递增。",
                0xFFFFFFFF, 800, 400, CharacterSkill.Shape.BURST,
                8, 8, 180, CHAIN_DAMAGE, 8, 32, 0, 0, 0),
            new CharacterSkill(SHADOW, "雷影化", "雷影化",
                "化作一道雷光在墙壁与树干之间折返跳跃，移动期间无法被瞄准。",
                0xFF8FE6FF, 300, 300, CharacterSkill.Shape.BURST,
                24, 1, 180, 0, 0, 1, 0, 0, 0)
        );
    }
    private KashimoSkills() {}
}
