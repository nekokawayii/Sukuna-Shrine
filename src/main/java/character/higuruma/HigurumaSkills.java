package character.higuruma;

import character.CharacterSkill;
import java.util.List;

/** 日车宽见：诛伏赐死。1 刻印 / 2 强化 / 3 领域展开·诛伏赐死。 */
public final class HigurumaSkills {
    public static final int SEAL = 2300, ENHANCE = 2301, DOMAIN = 2302,
        BONE = HigurumaExtras.BONE, VERDICT = HigurumaExtras.VERDICT, FREEZE = HigurumaExtras.FREEZE,
        COFFIN = HigurumaExtras.COFFIN, LIGHT = HigurumaExtras.LIGHT;

    public static List<CharacterSkill> all() {
        return List.of(
            new CharacterSkill(SEAL, "刻印", "刻印",
                "把副手武器的伤害、攻速与特殊 NBT 全部刻进法槌。", 0xFFC9A227, 150, 10,
                CharacterSkill.Shape.BURST, 8, 1, 180, 0, 0, 32, 0, 0, 0),
            new CharacterSkill(ENHANCE, "强化", "强化",
                "强化法槌：如忠诚三叉戟般掷出后自动飞回，无需蓄力。", 0xFFD9B24C, 300, 40,
                CharacterSkill.Shape.BURST, 8, 1, 180, 0, 0, 32, 0, 0, 0),
            new CharacterSkill(DOMAIN, "领域展开·诛伏赐死", "诛伏赐死",
                "外侧黑色混凝土，内部法庭，法庭四周环绕一圈 15 格高的断头台。", 0xFF2A2A2E, 1200, 600,
                CharacterSkill.Shape.BURST, 20, 1, 180, 0, 0, 32, 0, 0, 0),
            new CharacterSkill(BONE, "法槌·碎骨", "碎骨",
                "持法槌平A：对护盾与重甲目标额外 200% 破坏。", 0xFFC9A227, 0, 1,
                CharacterSkill.Shape.CONE, 3, 1, 40, 10, 0, 1, 0, 0, 0),
            new CharacterSkill(VERDICT, "法槌·一锤定音", "一锤定音",
                "长按蓄力 1 秒后跃起猛砸：强制打断并眩晕 2 秒。", 0xFFD9B24C, 200, 160,
                CharacterSkill.Shape.BURST, 4.5, 4.5, 180, 45, 0, 8, 0, 40, 0),
            new CharacterSkill(FREEZE, "起诉·证据冻结", "证据冻结",
                "准星目标被金色法庭锁链锁住 2 秒，期间无法位移或传送。", 0xFFE8D27A, 150, 120,
                CharacterSkill.Shape.RAY, 20, 1, 20, 0, 0, 1, 0, 40, 0),
            new CharacterSkill(COFFIN, "拘束·灵柩", "灵柩",
                "前方扇形：法庭长椅破土而出，把敌人卡在原地 1.5 秒。", 0xFFB99A3C, 220, 180,
                CharacterSkill.Shape.CONE, 8, 4, 60, 10, 0, 8, 0, 30, 0),
            new CharacterSkill(LIGHT, "审判之光", "审判之光",
                "对被起诉/标记的目标降下金色光柱，造成高额真实伤害。", 0xFFFFE49A, 260, 200,
                CharacterSkill.Shape.RAY, 30, 1, 25, 60, 0, 1, 0, 0, 0)
        );
    }
    private HigurumaSkills() {}
}
