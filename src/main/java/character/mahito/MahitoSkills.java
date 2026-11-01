package character.mahito;

import character.CharacterSkill;
import java.util.List;

/** 保留旧技能编号与贮存玩法，新增五个战斗轮盘槽位。 */
public final class MahitoSkills {
    public static final int TRANSMUTE=2100,SELF_EMBRYO=2101,TAIL=2102,COLLAPSE=2103,ARMS=2104,DODGE=2105,REFORGE=2106;
    private static CharacterSkill skill(int id,String name,String description,int cost,int cooldown){
        return new CharacterSkill(id,name,name,description,0xFFB67F91,cost,cooldown,CharacterSkill.Shape.BURST,8,1,180,0,0,32,0,0,0);
    }
    public static List<CharacterSkill> all(){return List.of(
        skill(TRANSMUTE,"无为转变","灵魂触碰绕过护甲与防御效果；伸长手臂近距离打击，剥离一层受肉或损坏重型防具。保留非战斗贮存玩法。",120,30),
        skill(TAIL,"淀月","具现人脸鲸尾，向前方半圆横扫，击飞并眩晕附近敌人。",220,140),
        skill(COLLAPSE,"肉体崩坏","锁定前方十格内单体，灵魂丝线定身两秒、护甲归零，每秒真实伤害。",260,180),
        skill(ARMS,"地刺·人类手臂","前方依次破土刺出五到八只手臂，将敌人挑飞并造成流血。",200,120),
        skill(DODGE,"极速灵魂闪避","向侧面或后方滑行最多八格，短暂免疫伤害；遇墙和悬崖停止。",140,140),
        skill(REFORGE,"灵魂重铸","为自己或改造人恢复百分之二十生命，并把面前玩家的一个增益变成减益。",280,260),
        skill(SELF_EMBRYO,"自闭圆顿裹","暗红肉体结界与倒悬手臂。无固定伤害；挥掌必中，每次削减一颗心上限，离场或击败真人后恢复。再按收回。",1500,1)
    );}
    private MahitoSkills(){}
}
