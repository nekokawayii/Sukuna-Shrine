package character.yuta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;

/**
 * 乙骨「真赝相爱」借用释放的爆发窗口。
 * <p>复制来的术式每次召唤只能用一次、且没有原角色的成长加成，单发很弱。这里给一段
 * <b>3 秒（60 刻）</b>的增伤窗口，倍率 = {@code 1.5 + 0.5 × 已完成章节}（1.5 / 2.0 / 2.5）。
 * <p>窗口存在的意义：旧术式有延迟与持续伤害（苍的持续光球、飞的茈），只加成"施法那一帧"
 * 会全部漏掉，所以按时间窗口算。
 * <p>读取点在 {@code CombatSettings.spellDamage}——所有术式伤害的共同入口。
 */
public final class YutaBoost {
    private static final int WINDOW_TICKS = 60;

    private record Boost(long until, double multiplier) {}
    private static final Map<UUID, Boost> ACTIVE = new HashMap<>();

    /** 释放一次复制术式后调用：按当前章节开一段增伤窗口。 */
    public static void activate(Player p, int rank) {
        activate(p, rank, 0);
    }

    /** @param override 大于 0 时用该倍率替代「1.5 + 0.5×章节」（村正右键的 ×3）。 */
    public static void activate(Player p, int rank, double override) {
        if (p == null) return;
        double mult = override > 0 ? override : 1.5 + 0.5 * Math.max(0, rank);
        ACTIVE.put(p.getUUID(), new Boost(p.level().getGameTime() + WINDOW_TICKS, mult));
    }

    /** 当前增伤倍率；不在窗口内返回 1。 */
    public static double multiplier(Player p) {
        if (p == null) return 1;
        Boost b = ACTIVE.get(p.getUUID());
        if (b == null) return 1;
        if (p.level().getGameTime() > b.until()) {
            ACTIVE.remove(p.getUUID());
            return 1;
        }
        return b.multiplier();
    }

    public static double scale(Player p, double base) {
        return base * multiplier(p);
    }

    private YutaBoost() {}
}
