package character.yuta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;

/**
 * 乙骨忧太「借用」他人术式时的临时放行标记。
 * <p>掠夺来的术式在原实现里会先检查「是否开启了对应家族」
 * （{@code GojoPlayer.unlocked} / {@code Uraume.unlocked} / {@code Journey.unlocked} / {@code BloodArts.unlocked}），
 * 乙骨是 {@code CUSTOM} 家族，永远不满足，所以会在 {@code AuthoritySkills.cast} 里被静默拒绝
 * （表现就是「尚未睁开六眼」这类提示）。
 * <p>转调期间把玩家 UUID 放进 {@link #ACTIVE}，各家族的 unlocked 判定额外放行一次；
 * 施法结束立刻移除，不影响该玩家自身的真实进度。
 */
public final class CopyBorrow {
    private static final Set<UUID> ACTIVE = new HashSet<>();

    public static boolean borrowing(Player p) {
        return p != null && ACTIVE.contains(p.getUUID());
    }

    public static void begin(Player p) {
        if (p != null) ACTIVE.add(p.getUUID());
    }

    public static void end(Player p) {
        if (p != null) ACTIVE.remove(p.getUUID());
    }

    private CopyBorrow() {}
}
