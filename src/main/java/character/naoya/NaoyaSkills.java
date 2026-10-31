package character.naoya;

import character.CharacterSkill;
import java.util.List;

/**
 * 禅院直哉 · 投射咒法：一圈 8 格，**左半 3 招（设定投影 / 领域展开 / 释放）**，
 * **右半 5 招（直线投射 / Z 字折返 / 强制同化 / 视距截断 / 空中走位）**。
 *
 * <p>轮盘是按"技能在列表里的下标"均分一圈的：n=8 时下标 4、5、6 落在左半圈（下方→左方），
 * 其余 5 个下标落在右半圈，所以这里的顺序就是为"左半 3 / 右半 5"排的。
 */
public final class NaoyaSkills {
    public static final int PROJECT = 2900;        // 设定投影（左 1）
    public static final int DOMAIN = 2901;         // 领域展开（左 2，占位）
    public static final int RELEASE = 2902;        // 释放（左 3）
    public static final int LINE = 2903;           // 直线投射（右 1）
    public static final int ZIGZAG = 2904;         // Z 字折返（右 2）
    public static final int ASSIMILATE = 2905;     // 强制同化（右 3）
    public static final int INTERCEPT = 2906;      // 视距截断（右 4）
    public static final int SKYSTEP = 2907;        // 空中走位（右 5）

    /** 轮盘顺序：下标 0~3 与 7 = 右半，下标 4~6 = 左半。 */
    public static List<Integer> wheel() {
        return List.of(LINE, ZIGZAG, ASSIMILATE, INTERCEPT, PROJECT, DOMAIN, RELEASE, SKYSTEP);
    }

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(LINE, "投射咒法·直线投射", "直线投射",
                "先选这一格，然后长按右键蓄力（越久越远）、松开留下节点，按左键沿节点方向突进 10 格，"
                    + "路径上的敌人受伤并附加「帧数泥沼」。",
                0xFF7FD8FF, 150, 40, CharacterSkill.Shape.RAY, 12, 1.5, 24, 14, 3, 4, 1, 0, 0),
            new CharacterSkill(ZIGZAG, "投射咒法·Z字折返", "Z字折返",
                "留下 4 个节点后按左键：0.5 秒内跑完 4 个点，对路径上的敌人多段物理切割。",
                0xFF9C6BFF, 250, 100, CharacterSkill.Shape.RAY, 16, 2, 30, 9, 2, 6, 1, 0, 0),
            new CharacterSkill(ASSIMILATE, "投射咒法·强制同化", "强制同化",
                "对着已经挂上「帧数泥沼」的敌人执行：在身边三圈高速穿梭，把敌人锁帧 2 秒，最后一击重击。",
                0xFFFF6B9C, 300, 160, CharacterSkill.Shape.BURST, 8, 2, 180, 26, 6, 1, 0, 0, 0),
            new CharacterSkill(INTERCEPT, "投射咒法·视距截断", "视距截断",
                "对着飞行中的弹幕执行：瞬移到弹道前方把弹幕拍碎。",
                0xFF6BFFC8, 100, 60, CharacterSkill.Shape.RAY, 20, 1, 20, 6, 1, 1, 0, 0, 0),
            new CharacterSkill(PROJECT, "投射咒法·设定投影", "设定投影",
                "接下来 10 秒：每移动一段距离就记录一个投影节点，并留下一个半透明残影（从开始位置朝准星方向）。",
                0xFFFFE36B, 200, 200, CharacterSkill.Shape.BURST, 4, 1, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(DOMAIN, "领域展开（占位）", "领域展开",
                "占位：直哉的领域还没做，暂时只播放起手提示。",
                0xFF808080, 4000, 2400, CharacterSkill.Shape.BURST, 8, 2, 180, 0, 0, 1, 0, 0, 0),
            new CharacterSkill(RELEASE, "投射咒法·释放", "释放",
                "按释放键：在已设定的投影节点之间自动穿梭，沿途敌人受伤（无击退）并挂上「帧数泥沼」。",
                0xFFFFFFFF, 0, 1, CharacterSkill.Shape.BURST, 8, 1, 180, 16, 3, 8, 1, 0, 0),
            new CharacterSkill(SKYSTEP, "投射咒法·空中走位", "空中走位",
                "空中留 2 个以上节点后执行：无视重力，以 45 度阶梯连续上跳。",
                0xFF7FA8FF, 150, 40, CharacterSkill.Shape.BURST, 6, 1, 180, 4, 1, 1, 0, 0, 0));
    }

    private NaoyaSkills() {}
}
