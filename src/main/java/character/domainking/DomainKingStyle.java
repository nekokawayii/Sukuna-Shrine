package character.domainking;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 领域王的轮盘配色与开局属性：咒力上限给到近乎无限（HUD 显示 ∞）。 */
public final class DomainKingStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("领域王", "万域", 0xE0160A24, 0xE02A0F3C, 0xE06A2FD0, 0xB03A1370, 0xFFF0D0FF),
            new Stats(999999999.0, 0, 0, 0, 9999),   // 咒力上限近乎无限 + 每秒回咒
            Stats.ZERO,
            DomainKingSkills.all(),
            Map.of(DomainKingSkills.JOGO_DOMAIN, 0, DomainKingSkills.VOID, 0,
                   DomainKingSkills.SHRINE, 0, DomainKingSkills.BLOOD, 0,
                   DomainKingSkills.YUTA, 0, DomainKingSkills.SIMPLE, 0),
            Map.of()
        );
    }
    private DomainKingStyle() {}
}
