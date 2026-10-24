package character.example;

import character.CharacterSkill;
import java.util.List;
import static character.CharacterSkill.Shape.*;

/** 自定义技能效果，非旧冰系技能换名。20刻=1秒，伤害还会经过模组统一倍率与防御结算。 */
public final class ExampleSkills {
    public static final int FROST_RAY=1000, FROST_FAN=1001, FROST_RING=1002;
    public static List<CharacterSkill> all() {
        return List.of(
            // 编号、全名、轮盘短名、说明、颜色、消耗、冷却、形状、距离、半径、半角、伤害、每章加伤、目标数、每章加目标、减速刻数、减速等级参数。
            new CharacterSkill(FROST_RAY,"霜纹·一线","霜线","沿准星瞬间划出霜线，遇墙停止，每章增加一个可命中目标。",
                0xFF88CCFF,40,20,RAY,16,.4,35,12,2,1,1,40,0),
            new CharacterSkill(FROST_FAN,"霜纹·散华","霜扇","前方70度扇形，命中后短暂减速；完成第一章解锁。",
                0xFFB8EFFF,80,60,CONE,7,1,35,18,3,3,1,60,1),
            new CharacterSkill(FROST_RING,"霜纹·回响","霜环","向周围释放一次霜环，不穿墙；完成第二章解锁。",
                0xFFE0F6FF,150,120,BURST,6,1,180,24,4,5,1,80,1)
        );
    }
    private ExampleSkills() {}
}
