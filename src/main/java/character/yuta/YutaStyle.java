package character.yuta;

import character.CharacterConfig;
import character.CharacterConfig.*;
import java.util.Map;

/** 真赝相爱：天蓝主题、咒力路线、两格轮盘；开局两招都可用（里香与复制术式）。 */
public final class YutaStyle {
    public static CharacterConfig config() {
        return new CharacterConfig(
            new Wheel("真赝相爱", "选择术式", 0xE60B1A2B, 0xD11B2C44, 0xE03E86C8, 0xC0192233, 0xFFEAF6FF),
            new Stats(2000, 10, 2, 0, 10), // 咒力上限、额外生命、额外近战、术式加成、每秒回咒
            Stats.ZERO,                    // 每章固定加成留空，成长全部写在 YutaStory 里
            YutaSkills.all(),
            Map.of(YutaSkills.DOMAIN, 3),  // 领域在三章全部完成后解锁
            Map.of()                       // 不重命名旧术式
        );
    }
    private YutaStyle() {}
}
