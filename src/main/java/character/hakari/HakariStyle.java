package character.hakari;

import character.CharacterConfig;
import character.CharacterConfig.Stats;
import character.CharacterConfig.Wheel;
import java.util.Map;

/** 秤金次的轮盘配色与开局属性（赌场金红）。 */
public final class HakariStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("秤金次", "坐杀博打", 0xE01A0F0C, 0xE03A1510, 0xE0FFCB3D, 0xB0502418, 0xFFFFF0D0),
            // 平时偏软：咒力一般、生命一般，全靠头奖爆发
            new Stats(3000, 20, 5, 5, 12),
            Stats.ZERO,
            HakariSkills.all(),
            Map.of(HakariSkills.DOOR_SMASH, 0),
            Map.of()
        );
    }

    private HakariStyle() {}
}
