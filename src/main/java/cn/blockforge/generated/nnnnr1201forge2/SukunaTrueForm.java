package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 两面宿傩「原身形态」（r268）：
 *
 * <p>被打到仅剩 1 点时自动开启：换成原身皮肤（客户端 {@link SukunaTrueFormClient}）、
 * 可以同时释放两个术式（{@link Skills} 里跳过结印互斥与通用冷却）、领域展开不再僵直。
 */
public final class SukunaTrueForm {
    private static final String KEY = "SukunaTrueForm";

    public static boolean active(net.minecraft.world.entity.player.Player p) {
        return Curse.data(p).getBoolean(KEY);
    }

    private static boolean isSukuna(ServerPlayer p) {
        var d = character.CharacterService.selected(p);
        return d == null ? Journey.unlocked(p) : "sukuna".equals(d.id());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
        if (!isSukuna(p) || active(p)) return;
        if (p.getHealth() > 1.0f) return;
        Curse.data(p).putBoolean(KEY, true);
        p.displayClientMessage(Component.literal("两面宿傩 · 原身显现").withStyle(net.minecraft.ChatFormatting.DARK_RED),
            false);
        p.sendSystemMessage(Component.literal("【原身形态】皮肤已变；术式可双发；领域展开不再僵直。")
            .withStyle(net.minecraft.ChatFormatting.RED));
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
            net.minecraft.sounds.SoundSource.PLAYERS, 1f, .6f);
        sync(p);
    }

    /** 把"已进入原身"同步给周围客户端（皮肤需要）。 */
    public static void sync(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        Network.send(level, new Network.Effect(640, p.getId(), p.position(), Vec3.ZERO, 1f, 0, 0));
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Curse.data(p).putBoolean(KEY, false);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        if (!e.isWasDeath() || !(e.getEntity() instanceof ServerPlayer p)) return;
        Curse.data(p).putBoolean(KEY, false);
        p.level().getServer().tell(new net.minecraft.server.TickTask(20,
            () -> Network.send(p.serverLevel(), new Network.Effect(640, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0))));
    }

    /** 周期性重发，保证重连/远处的玩家也能看到原身皮肤。 */
    @SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        var server = e.getServer();
        if (server.getTickCount() % 100 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (active(p)) sync(p);
        }
    }

    private SukunaTrueForm() { }
}
