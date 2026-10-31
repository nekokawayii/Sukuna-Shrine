package character;

import java.util.*;
import character.gojo.GojoCharacter;
import character.uraume.UraumeCharacter;
import character.blood.BloodCharacter;
import character.yuji.YujiCharacter;
import character.sukuna.SukunaCharacter;
import character.toji.TojiCharacter;

/** 新角色在静态区 register 一次，选择书、轮盘和资源条自动读取。两端须使用相同模组。 */
public final class CharacterRegistry {
    private static final Map<String, CharacterDefinition> ENTRIES = new LinkedHashMap<>();
    static {
        register(GojoCharacter.create());
        register(UraumeCharacter.create());
        register(BloodCharacter.create());
        register(YujiCharacter.create());
        register(SukunaCharacter.create());
        register(TojiCharacter.create());
        register(character.mahito.MahitoCharacter.create());
        register(character.megumi.MegumiCharacter.create());
        register(character.higuruma.HigurumaCharacter.create());   // 真人：无为转变 / 自闭圆盾裹（功能待定）
        register(character.kashimo.KashimoCharacter.create());   // 鹿紫云一：幻兽琥珀（电荷与必中）
        register(character.todo.TodoCharacter.create());   // 东堂葵：不义游戏（注入咒力 / 换位）
        register(character.domainking.DomainKingCharacter.create());   // 领域王：万域（咒力无限）
        register(character.jogo.JogoCharacter.create());   // 漏壶·灾厄火焰（自定义个人线：泡岩浆 30 秒）
        register(character.yuta.YutaCharacter.create());
        // 启用完整示例：register(character.example.CharacterTemplate.create());
        // 复制到自己的文件夹后：register(character.my_ice.MyIceCharacter.create());
    }
    public static void register(CharacterDefinition definition) {
        if (ENTRIES.putIfAbsent(definition.id(), definition) != null)
            throw new IllegalArgumentException("重复的角色编号：" + definition.id());
    }
    public static CharacterDefinition get(String id) { return ENTRIES.get(id); }
    public static List<CharacterDefinition> all() { return List.copyOf(ENTRIES.values()); }
    private CharacterRegistry() {}
}
