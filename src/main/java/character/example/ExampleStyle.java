package character.example;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 所有颜色写0xAARRGGBB；显示与实际技能共用ExampleSkills，不需要改界面类。 */
public final class ExampleStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("霜行者","选择霜纹",0xE6101C2A,0xD12C3B50,0xE04691BA,0xC0272930,0xFFF1FAFF),
            new Stats(1000,10,2,0,10), // 开局：咒力上限、额外生命、额外近战、术式加成、每秒回咒。
            Stats.ZERO, // 每章固定加成设0，所有奖励在ExampleStory逐章填写，不会偷偷再加400咒力。
            ExampleSkills.all(),
            Map.of(ExampleSkills.FROST_FAN,1, ExampleSkills.FROST_RING,2), // 完成第几章后解锁；不填=开局可用。
            Map.of() // 给旧技能改显示用：Map.of(22,new SkillText("新全名","短名","说明",0xFF88CCFF))。
        );
    }
    private ExampleStyle() {}
}
