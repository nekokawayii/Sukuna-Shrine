package character.npc;

import cn.blockforge.generated.nnnnr1201forge2.NpcBridge;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 有截止时间的硬直租约；保留原 NoAI 和目标，存档/卸载后也能自行解除。 */
public final class NpcControl {
    private static final String KEY = "SukunaControlLeases", PREVIOUS = "SukunaControlPreviousAI";
    public static void stun(Mob mob, String source, int ticks) {
        if (mob.level().isClientSide || ticks <= 0) return;
        var data = mob.getPersistentData();
        if (!data.contains(PREVIOUS)) data.putBoolean(PREVIOUS, mob.isNoAi());
        var leases = data.getCompound(KEY);
        leases.putLong(source, Math.max(leases.getLong(source), mob.level().getGameTime() + ticks));
        data.put(KEY, leases);
        // 不清空目标：否则 HurtByTargetGoal 的反击对象会永久丢失。
        mob.getNavigation().stop();
        mob.setNoAi(true);
    }
    public static boolean locked(Mob mob) {
        var leases = mob.getPersistentData().getCompound(KEY);
        long now = mob.level().getGameTime();
        return leases.getAllKeys().stream().anyMatch(k -> leases.getLong(k) > now);
    }
    public static void release(Mob mob, String source) {
        var leases = mob.getPersistentData().getCompound(KEY);
        leases.remove(source);
        mob.getPersistentData().put(KEY, leases);
        recover(mob);
    }
    private static void recover(Mob mob) {
        var data = mob.getPersistentData();
        if (!data.contains(PREVIOUS)) return;
        var leases = data.getCompound(KEY);
        long now = mob.level().getGameTime();
        for (String key : java.util.List.copyOf(leases.getAllKeys()))
            if (leases.getLong(key) <= now) leases.remove(key);
        if (!leases.isEmpty()) { mob.setNoAi(true); return; }
        mob.setNoAi(data.getBoolean(PREVIOUS));
        data.remove(PREVIOUS); data.remove(KEY);
        if (mob.getTarget() == null && NpcBridge.canHit(mob, mob.getLastHurtByMob())) mob.setTarget(mob.getLastHurtByMob());
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide) recover(mob);
    }
    private NpcControl() { }
}
