package cn.blockforge.generated.nnnnr1201forge2;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 天逆蛑腰间挂件的服务端同步。
 *
 * <p>挂件只在客户端画，但“谁背包里有这件咒具”只有服务端知道（别人的背包不会发给你的客户端），
 * 所以每隔 10 刻检查一次背包，状态变化时广播给周围能看见这个玩家的人；新玩家开始被追踪、
 * 登录、重生、换维度时补发一次。显示条件：主背包或副手任意一格有 天逆蛑。</p>
 */
public final class TenkaPendantSync {
    private static final Map<UUID, Boolean> LAST = new HashMap<>();

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        if (p.tickCount % 10 != 0) return;
        boolean has = has(p);
        Boolean old = LAST.put(p.getUUID(), has);
        if (old == null || old != has) broadcast(p, has);
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            boolean has = has(p);
            LAST.put(p.getUUID(), has);
            broadcast(p, has);
        }
    }

    @SubscribeEvent public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer
            && target != viewer) {
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new Network.PendantState(target.getId(), has(target)));
        }
    }

    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) LAST.remove(p.getUUID());
    }

    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) LAST.remove(p.getUUID());
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) LAST.remove(p.getUUID());
    }

    /** 背包里（含副手）有没有天逆蛑。 */
    public static boolean has(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Content.TENKA_SPEAR.get())) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(Content.TENKA_SPEAR.get())) return true;
        }
        return false;
    }

    private static void broadcast(ServerPlayer p, boolean visible) {
        Network.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p),
            new Network.PendantState(p.getId(), visible));
    }

    private TenkaPendantSync() { }
}
