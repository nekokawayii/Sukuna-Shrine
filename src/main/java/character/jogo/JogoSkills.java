package character.jogo;

import character.CharacterSkill;
import java.util.List;
import static character.CharacterSkill.Shape.*;

/** 漏壶·灾厄火焰：三招都是自定义效果（见 {@link JogoCombat}），这里只声明参数与轮盘文字。 */
public final class JogoSkills {
    public static final int FIRE_BUG=1000, METEOR=1001, LAVA_BURST=1002, DOMAIN=1003;

    public static List<CharacterSkill> all() {
        return List.of(
            // 编号、全名、轮盘短名、说明、颜色、消耗、冷却、形状、距离、半径、半角、伤害、每章加伤、目标数、每章加目标、减速刻数、减速等级
            new CharacterSkill(FIRE_BUG,"灾厄·火砾虫","火砾虫",
                "召唤 1~10 只火砾虫向前飞去，飞行 20 秒或碰到敌人时爆炸。",
                0xFFFF7A18,60,80,RAY,20,.5,35,8,2,1,1,0,0),
            new CharacterSkill(METEOR,"灾厄·陨石","陨石",
                "召唤 2~6 颗陨石（黑曜石带火焰拖尾）从天空砸向准星位置，落地造成范围伤害。",
                0xFFFF4A12,150,160,RAY,32,1,35,26,4,1,1,0,0),
            new CharacterSkill(LAVA_BURST,"灾厄·火焰喷发","火焰喷发",
                "单手前指，向前方扇形喷出 1~15 条岩浆柱，灼烧范围内的敌人。",
                0xFFFFB024,220,160,CONE,18,2,22,14,3,15,3,40,1),
            new CharacterSkill(DOMAIN,"领域展开·盖棺铁围山","盖棺铁围山",
                "封闭式火山领域：暗红火山岩壁围住战场，地面岩浆翻滚，界内敌人被高温持续烘烤；漏壶在领域内技能冷却减半。完成第二章解锁。",
                0xFFFF3A0A,800,6000,BURST,20,1,180,0,0,1,0,0,0)
        );
    }
    private JogoSkills() {}
}
