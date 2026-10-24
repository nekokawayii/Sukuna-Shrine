package character.jogo;

import character.*;
import java.util.List;
import static character.CharacterDefinition.*;

/** 漏壶：选择书里显示「灾厄火焰」，三招全自定义，被动免疫火焰与岩浆。 */
public final class JogoCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "jogo", "漏壶", "灾厄火焰",
            "火山咒灵：火砾虫、陨石与火焰喷发，完全免疫火焰与岩浆伤害。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFFFF6A1E,
            List.of(JogoSkills.FIRE_BUG, JogoSkills.METEOR, JogoSkills.LAVA_BURST, JogoSkills.DOMAIN,
                JogoSkills.VOLCANO_BEAM, JogoSkills.MAGMA_TRAP, JogoSkills.ASH_VEIL),
            JogoStory.chapters(), JogoStyle.config());
    }
    private JogoCharacter() {}
}
