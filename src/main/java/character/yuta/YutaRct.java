package character.yuta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;

/**
 * 「反转术式·外放」的门禁放行开关。
 * <p>模组里守护者的 {@code ReverseOutput.cast} 会检查 {@code GuardianCombat.awakened}
 * （蓝色觉醒，来自净土通关），乙骨不满足。和复制术式一样，用一次性放行标记绕过去，
 * 这样外放的治疗量、护甲修复、冷却、咒力消耗全部复用原实现。
 */
public final class YutaRct {
    private static final Set<UUID> ACTIVE = new HashSet<>();

    public static void begin(Player p) { if (p != null) ACTIVE.add(p.getUUID()); }
    public static void end(Player p) { if (p != null) ACTIVE.remove(p.getUUID()); }
    public static boolean active(Player p) { return p != null && ACTIVE.contains(p.getUUID()); }

    private YutaRct() {}
}
