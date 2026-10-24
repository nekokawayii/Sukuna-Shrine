package character.blood;
import character.CharacterDefinition;
import java.util.List;
import static character.CharacterDefinition.*;
public final class BloodCharacter {
    public static CharacterDefinition create() { return new CharacterDefinition("blood", "咒胎九相图", "赤血操术", "完整赤血双环轮盘；胎海展开时，死胎星出现在轮盘中央。", Family.BLOOD, Resource.CURSED_ENERGY, 0xFFD84362,
        List.of(60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,76,77,78,79),
        List.of(new Chapter("血脉苏醒",10,"击败10名敌人，练习百敛接穿血","血环与血块数量增加"),new Chapter("九相共鸣",30,"累计击败30名敌人，尝试血刃接千本","弹幕数量和持续时间成长"),new Chapter("胎海主宰",60,"累计击败60名敌人，掌握胎海与死胎星","赤血机制成长至最高阶段"))); }
    private BloodCharacter() {}
}
