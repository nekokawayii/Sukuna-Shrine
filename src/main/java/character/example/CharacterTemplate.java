package character.example;

import character.*;
import java.util.List;
import static character.CharacterDefinition.*;

/** 从零设计的完整示例，默认不登记；复制整个example文件夹后改包名和角色编号。 */
public final class CharacterTemplate {
    public static CharacterDefinition create() {
        return new CharacterDefinition(
            "example_ice", "霜行者", "霜纹术式", "以霜线、霜扇与霜环逐章成长的示例角色。",
            Family.CUSTOM, Resource.CURSED_ENERGY, 0xFF88CCFF,
            List.of(ExampleSkills.FROST_RAY, ExampleSkills.FROST_FAN, ExampleSkills.FROST_RING),
            ExampleStory.chapters(), ExampleStyle.config());
    }
    private CharacterTemplate() {}
}
