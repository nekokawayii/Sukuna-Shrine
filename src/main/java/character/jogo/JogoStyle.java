package character.jogo;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 漏壶的轮盘配色与开局属性：火焰橙红，咒力条走咒力。 */
public final class JogoStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("漏壶","灾厄火焰",0xE6140806,0xD13A1A10,0xE0C2410F,0xC02A1410,0xFFFFE6C8),
            new Stats(1000,10,2,0,10),   // 开局：咒力上限、额外生命、额外近战、术式加成、每秒回咒
            Stats.ZERO,                  // 每章固定成长设 0，全部写进 JogoStory 的章节奖励
            JogoSkills.all(),
            Map.of(),                    // 三招开局全可用；要逐章解锁就在这里写 招式,章数
            Map.of()                     // 旧技能改显示用；漏壶全部是自定义技能
        );
    }
    private JogoStyle() {}
}
