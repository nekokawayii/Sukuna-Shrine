package character.toji;
import character.CharacterDefinition;
import java.util.List;
import static character.CharacterDefinition.*;
/** 甚尔没有术式轮盘。体术操作和战斗结算见 TojiCombat。 */
public final class TojiCharacter {
    public static CharacterDefinition create() { return new CharacterDefinition("toji", "伏黑甚尔", "天宇暴君", "零咒力的肉体路线：R突进、潜行+R重击、跳跃+R跃身。没有咒术轮盘，显示体术条。", Family.HEAVENLY, Resource.STAMINA, 0xFF79DEAA,
        List.of(),List.of(new Chapter("脱离禅院",10,"以近战或体术击败10名敌人","体术上限120，突进距离提高"),new Chapter("术师杀手",30,"累计以近战或体术击败30名敌人","体术上限140，重击范围扩大"),new Chapter("天宇暴君",60,"累计以近战或体术击败60名敌人","体术上限160，恢复与肉体全面强化"))); }
    private TojiCharacter() {}
}
