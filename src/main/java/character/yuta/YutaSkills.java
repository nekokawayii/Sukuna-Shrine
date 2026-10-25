package character.yuta;

import character.CharacterSkill;
import java.util.List;
import static character.CharacterSkill.Shape.*;

/**
 * 乙骨忧太的两格轮盘：里香召唤 与 复制术式入口。
 * <p>真正效果在 {@link YutaEffects}，用 {@code CharacterCasting.registerEffect} 挂到这两个编号上；
 * 这里的伤害/范围只是形状判定的占位（两个技能都不是伤害技能，damage 填 0）。
 */
public final class YutaSkills {
    /** 召唤里香：不造成伤害，纯自身施法。 */
    public static final int RIKA = 1000;
    /** 复制术式入口：悬停展开已复制列表（分支界面后续接入），本轮先做规则校验与提示。 */
    public static final int COPY = 1001;
    /** 领域展开·真赝相爱：第三章全部完成后解锁。 */
    public static final int DOMAIN = 1002;
    /** 反转术式（自疗），第四章解锁。 */
    public static final int SURGERY = 1003;
    /** 反转术式外放（治疗目标并修护甲），第四章解锁。 */
    public static final int RCT_OUT = 1004;

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(RIKA, "里香", "里香", "召唤里香战斗两分钟：期间咒力无上限，里香会自动撕咬或拳击最近的敌人。",
                0xFFB9E4FF, 0, 2400, BURST, 1, .5, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(COPY, "复制术式", "复制", "对着生物潜行右键掠夺其术式；未召唤里香时无法使用已复制的术式。",
                0xFF9AD8FF, 0, 20, BURST, 1, .5, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(SURGERY, "反转术式", "反转", "以反转术式治疗自身，消耗随生命成长提高。",
                0xFFFFE0E0, 0, 20, BURST, 1, .5, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(RCT_OUT, "反转术式·外放", "外放", "把反转术式打向准星目标：治疗并修复其护甲（复用守护者的反转外放实现）。",
                0xFFFFC8C8, 0, 20, RAY, 1, .5, 60, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(DOMAIN, "领域展开·真赝相爱", "领域", "外围漆黑、内部坟与刀的封闭领域；每秒消耗 30 咒力，耗尽即解除。",
                0xFFEAF6FF, 0, 2400, BURST, 1, .5, 180, 0, 0, 1, 0, 0, 0)
        );
    }
    private YutaSkills() {}
}
