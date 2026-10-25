package character.yuta;

import character.*;
import java.util.List;
import static character.CharacterDefinition.*;

/**
 * 乙骨忧太 · 真赝相爱。
 * <p>与宿傩/五条等旧角色不同，他没有旧术式家族：两格轮盘都是 {@code CUSTOM} 技能，
 * 复制来的术式会以 1000+ 编号另行登记（见 {@link YutaSkills}）。
 */
public final class YutaCharacter {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "yuta",                       // id
            "乙骨忧太",                    // 角色名
            "真赝相爱",                    // 术式名：选择书里显示的就是这个
            "召唤里香获得无穷咒力，掠夺他人的术式化为己用。三章成长线全部是剧情条件。",
            Family.CUSTOM,
            Resource.CURSED_ENERGY,
            0xFF9AD8FF,                   // 天蓝
            List.of(YutaSkills.RIKA, YutaSkills.COPY, YutaSkills.DOMAIN),
            YutaStory.story(),
            YutaStyle.config(),
            YutaStory.events()            // 事件型章节
        );
    }
    private YutaCharacter() {}
}
