package character.hakari;

import character.CharacterDefinition;
import character.CharacterDefinition.Family;
import character.CharacterDefinition.Resource;
import java.util.List;

/**
 * 秤金次：术式「坐杀博打」。
 * <p>赌狗流：平时只有糙拳脚，靠开盘抽头奖；中奖后 4 分 11 秒无限咒力与高速自愈。
 */
public final class HakariCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "hakari", "秤金次", "坐杀博打",
            "赌上性命的赌徒。抽中头奖即进入 4 分 11 秒无限咒力，没中奖时只能靠咒力强化的拳脚。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFFFFCB3D,
            HakariSkills.allIds(),
            HakariStory.chapters(), HakariStyle.config());
    }

    private HakariCharacter() {}
}
