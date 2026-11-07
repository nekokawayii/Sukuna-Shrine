package character.yuji;

import character.CharacterSkill;
import java.util.List;

/** 虎杖重做（r316）：15 式体术，编号 1500–1513。 */
public final class YujiRebuildSkills {
    public static final int IKKI = 1500, BREAK = 1501, BARRAGE = 1502, CHARGE = 1503,
        RISING = 1504, HOOK = 1505, KICK = 1506, DASH = 1507, ROLL = 1508,
        SWEEP = 1509, WALL = 1510, WAVE = 1511, STOMP = 1512, ELBOW = 1513, KNEE = 1514;
    public static final int FIRST = IKKI, LAST = KNEE;

    /** 被动：径庭拳由左键命中触发，不占轮盘。 */
    public static final List<Integer> WHEEL = List.of(BREAK, BARRAGE, CHARGE, RISING, HOOK,
        KICK, DASH, ROLL, SWEEP, WALL, WAVE, STOMP, ELBOW, KNEE);

    private static CharacterSkill s(int id, String name, String desc, int cost, int cd, double range, double radius,
                                    int halfAngle, double damage, int slowTicks) {
        return new CharacterSkill(id, name, name, desc, 0xFFFFAB85, cost, cd, CharacterSkill.Shape.CONE,
            range, radius, halfAngle, damage, 0, 4, 0, slowTicks, 0);
    }

    public static List<CharacterSkill> all() {
        return List.of(
            s(IKKI, "径庭拳", "左键命中后，0.5 秒后咒力在敌人体内炸开，造成无视护甲的二段真实伤害。", 0, 1, 4, 1, 30, 10, 0),
            s(BREAK, "崩拳·破甲", "向前猛踏一步挥出重拳，打断敌人施法并削减护甲。", 120, 100, 4, 1, 40, 30, 0),
            s(BARRAGE, "连续重拳·乱打", "突进打出 5~8 段极速连击（霸体），最后一拳将敌人强力击飞。", 200, 160, 4, 2.5, 50, 9, 0),
            s(CHARGE, "蓄力重击·碎骨", "蓄力 1 秒后全力一击：破除防具并使敌人眩晕 1.5 秒。", 240, 220, 4, 1, 40, 60, 30),
            s(RISING, "升龙拳·浮空", "一记上勾拳把敌人打到浮空，方便接空中连段。", 150, 120, 3.5, 1, 45, 35, 0),
            s(HOOK, "勾拳·截击", "在敌人出手瞬间对拼：判定成功则打断动作并造成僵直。", 140, 140, 3.5, 1, 45, 25, 20),
            s(KICK, "飞踢·空战", "跃向空中凌空飞踢，命中空中目标则将其踹向地面。", 180, 140, 5, 1.5, 40, 40, 0),
            s(DASH, "瞬步·闪击", "化作残影向前突进 10 格，对沿途敌人造成撞击伤害。", 160, 120, 10, 1.5, 20, 22, 0),
            s(ROLL, "闪避·受身", "被击飞时按下：快速调整身位落地，获得无敌帧与移速加成。", 80, 200, 3, 2, 360, 0, 0),
            s(SWEEP, "旋风踢·扫堂", "贴地旋转扫踢，强制击倒周围所有敌人。", 180, 180, 4.5, 4.5, 360, 26, 40),
            s(WALL, "攀墙跃·地形战", "在墙面借力弹射，快速拉近高处敌人。", 100, 120, 3, 1, 360, 0, 0),
            s(WAVE, "拳风·真空波", "向前挥出全力一拳，打出极快的半月形真空拳风。", 200, 160, 22, 1.5, 12, 34, 0),
            s(STOMP, "重踏·地震", "高高跃起后单脚重踏，半径 8 格内震荡伤害并强制击飞。", 300, 240, 8, 8, 360, 45, 0),
            s(ELBOW, "肘击·破空", "近身肘击，附带极强击退（拉开身位用）。", 120, 80, 3, 1, 40, 28, 0),
            s(KNEE, "膝撞·碎盾", "跃起用膝盖猛撞，对护盾和装甲造成 300% 额外破坏。", 180, 140, 3, 1, 40, 35, 0));
    }

    private YujiRebuildSkills() { }
}
