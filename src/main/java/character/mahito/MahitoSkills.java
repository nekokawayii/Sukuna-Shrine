package character.mahito;

import character.CharacterSkill;
import java.util.List;

/** 保留旧技能编号与贮存玩法，新增五个战斗轮盘槽位。 */
public final class MahitoSkills {
    public static final int TRANSMUTE=2100,SELF_EMBRYO=2101,TAIL=2102,COLLAPSE=2103,ARMS=2104,DODGE=2105,REFORGE=2106,
        WALL=2107,BRIDGE=2108,HEAL=2109,BURST=2110,CLONE=2111,POTENTIAL=2112;
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
        skill(WALL,"改造肉墙","在身后立起 5×5 的改造肉块墙，可挡住攻击与视线。",500,200),
        skill(BRIDGE,"肉桥","脚下铺出 3×3 的改造肉块桥，持续 5 秒，可跨越沟壑。",500,200),
        skill(HEAL,"灵魂治疗","治疗自身 40% 最大生命并解除流血/凋零。",600,300),
        skill(BURST,"肉体爆裂","面前的改造体炸开：对 4 格内所有敌人造成 30 点伤害并击退。",700,240),
        skill(CLONE,"分身","分出一个持有同款装备的分身，血量对半；再次使用或死亡时收回。",900,400),
        skill(POTENTIAL,"潜能激发","让 8 格内最近的生物/玩家随机获得一种术式（宿傩/赤血/无下限/冰凝）。",800,600),
        skill(SELF_EMBRYO,"自闭圆顿裹","暗红肉体结界与倒悬手臂。无固定伤害；挥掌必中，每次削减一颗心上限，离场或击败真人后恢复。再按收回。",1500,1)
    );}
    private MahitoSkills(){}
}
