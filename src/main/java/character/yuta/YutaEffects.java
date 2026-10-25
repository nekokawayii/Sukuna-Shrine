package character.yuta;

import character.CharacterSkill;
import cn.blockforge.generated.nnnnr1201forge2.RikaRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 乙骨忧太两格轮盘的服务端效果。
 * <p>由 {@code GeneratedMod} 在 {@code CharacterPackets.register()} 之后用
 * {@code CharacterCasting.registerEffect("yuta", 1000/1001, ...)} 登记，只登记一次。
 * <p>资格、存活、冷却、扣费、控制状态已由 {@code CharacterCasting.cast} 统一校验，这里不重复扣费。
 */
public final class YutaEffects {
    /** 1000 里香：召唤两分钟，期间咒力无限。 */
    public static void summonRika(ServerPlayer p, CharacterSkill skill, int rank) {
        RikaRules.summon(p);
    }

    /** 1001 复制术式入口：未召唤里香时不可用；分支列表与施放在下一步接入。 */
    public static void copyMenu(ServerPlayer p, CharacterSkill skill, int rank) {
        if (!RikaRules.active(p)) {
            p.displayClientMessage(Component.literal("未召唤里香，无法使用复制术式"), true);
            return;
        }
        p.displayClientMessage(Component.literal("已复制的术式：" + YutaCopies.describe(p)), true);
    }
    /** 1003 反转术式：直接复用 Curse.reverse（含冷却、治疗量、咒力消耗与提示）。 */
    public static void surgery(ServerPlayer p, CharacterSkill skill, int rank) {
        cn.blockforge.generated.nnnnr1201forge2.Curse.reverse(p);
    }

    /** 1004 反转术式·外放：复用守护者的 ReverseOutput，临时放行「蓝色觉醒」门禁。 */
    public static void rctOut(ServerPlayer p, CharacterSkill skill, int rank) {
        YutaRct.begin(p);
        try { cn.blockforge.generated.nnnnr1201forge2.ReverseOutput.cast(p); }
        finally { YutaRct.end(p); }
    }

    /** 1002 领域展开·真赝相爱。 */
    public static void domain(ServerPlayer p, CharacterSkill skill, int rank) {
        YutaDomain.open(p);
    }

    private YutaEffects() {}
}
