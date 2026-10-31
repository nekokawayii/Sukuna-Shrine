package character.nanami;

import character.CharacterSkill;
import java.util.List;

/** 七海建人 · 十划咒法：1 强打 / 2 瓦落瓦落 / 3 隙 / 4 加班。 */
public final class NanamiSkills {
    public static final int SMASH = 2600, VALOR = 2601, GAP = 2602, OVERTIME = 2603;

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(SMASH, "十划咒法·强打", "强打",
                "向前挥刀，对前方扇形造成伤害，并强制在所有命中的敌人身上生成 7:3 弱点。",
                0xFFD8B24A, 180, 60, CharacterSkill.Shape.CONE,
                5, 2.5, 75, 20, 4, 8, 2, 0, 0),
            new CharacterSkill(VALOR, "瓦落瓦落", "瓦落瓦落",
                "双手持刀架在身前 1.5 秒：期间被打则完美格挡（不掉血）并反击，弹开并眩晕敌人 2 秒；眩晕期间弱点放大两倍。",
                0xFFFFE08A, 200, 300, CharacterSkill.Shape.BURST,
                4, 2, 180, 0, 0, 1, 0, 40, 0),
            new CharacterSkill(GAP, "十划咒法·隙", "隙",
                "射出一道极细的黑金咒力线：目标有弱点时自动微调轨迹直击弱点（暴击），否则只是小额伤害。",
                0xFFFFF2B8, 150, 100, CharacterSkill.Shape.RAY,
                32, 0.6, 8, 10, 2, 1, 0, 0, 0),
            new CharacterSkill(OVERTIME, "加班", "加班",
                "攻速 +50%、移速 +30%、弱点判定 ×1.5；代价是每秒掉 1% 最大生命（期间击杀一次即停止扣血）。",
                0xFFB98A2E, 400, 600, CharacterSkill.Shape.BURST,
                8, 1, 180, 0, 0, 1, 0, 0, 0)
        );
    }
    private NanamiSkills() {}
}
