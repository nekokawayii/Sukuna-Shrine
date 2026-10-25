package character.yuta;

import cn.blockforge.generated.nnnnr1201forge2.Content;
import cn.blockforge.generated.nnnnr1201forge2.Curse;
import cn.blockforge.generated.nnnnr1201forge2.RikaEntity;
import cn.blockforge.generated.nnnnr1201forge2.RikaRules;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 领域展开 · 真赝相爱。
 * <p>本阶段只做「领域状态 + 音效时序 + 咒力持续消耗 + 里香常驻」，<b>不改动地形</b>；
 * 地形清场与还原按文档单独实现（那是唯一可能破坏存档的部分，需要世界级日志与崩溃兜底）。
 * <p>音效严格依次播放（实测时长）：start 1.19s → mid 1.12s → ost 25.71s，
 * 即 start 在 0 刻、mid 在 24 刻、ost 在 47 刻。
 */
@Mod.EventBusSubscriber(modid = "sukuna")
public final class YutaDomain {
    public static final int SKILL = 1002;
    /** 领域半径（方块）。 */
    private static final int RADIUS = 50;

    /** 每秒消耗咒力。 */
    private static final int DRAIN_PER_SECOND = 30;
    private static final int MID_TICK = 24;   // 1.19s 后
    private static final int OST_TICK = 47;   // 再 1.12s 后
    /** 里香在领域内不断续命，保证常驻。 */
    private static final int RIKA_KEEPALIVE = 40;

    private static final class Active {
        final long start;
        boolean mid, ost;
        Active(long start) { this.start = start; }
    }
    private static final Map<UUID, Active> ACTIVE = new HashMap<>();

    public static boolean active(net.minecraft.world.entity.player.Player p) {
        return p != null && ACTIVE.containsKey(p.getUUID());
    }

    public static void open(ServerPlayer p) {
        if (active(p)) {
            p.displayClientMessage(Component.literal("领域已展开"), true);
            return;
        }
        // 清场会覆盖方块，箱子内容目前还没做完整还原，所以附近有容器时直接拒绝，避免丢物品。
        String blocked = containerNear(p, RADIUS);
        if (blocked != null) {
            p.displayClientMessage(Component.literal("附近有容器（" + blocked + "），领域无法在此展开"), false);
            return;
        }
        ACTIVE.put(p.getUUID(), new Active(p.server.overworld().getGameTime()));
        YutaTerrain.begin(p, RADIUS);
        play(p, "sukuna:yuta_domain_start");
        ensureRika(p);
        p.displayClientMessage(Component.literal("领域展开 · 真赝相爱"), false);
    }

    public static void close(ServerPlayer p, String reason) {
        if (ACTIVE.remove(p.getUUID()) == null) return;
        YutaTerrain.restore(p);
        p.displayClientMessage(Component.literal("领域解除：" + reason), false);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        YutaTerrain.tick(e.getServer());
        if (ACTIVE.isEmpty()) return;
        long now = e.getServer().overworld().getGameTime();
        for (var it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p == null || !p.isAlive() || p.level().isClientSide) {
                YutaTerrain.restore(p);
                it.remove(); continue;
            }
            long age = now - entry.getValue().start;
            var state = entry.getValue();
            if (!state.mid && age >= MID_TICK) { play(p, "sukuna:yuta_domain_mid"); state.mid = true; }
            if (!state.ost && age >= OST_TICK) { play(p, "sukuna:yuta_domain_ost"); state.ost = true; }

            // 每秒扣一次咒力；不够就自动关闭。
            if (age > 0 && age % 20 == 0 && !Curse.spend(p, DRAIN_PER_SECOND)) {
                it.remove();
                YutaTerrain.restore(p);
                p.displayClientMessage(Component.literal("咒力耗尽，领域解除"), false);
                continue;
            }
            ensureRika(p);
        }
    }

    /** 扫描领域范围内的方块实体，返回第一个容器名字；没有则 null。 */
    private static String containerNear(ServerPlayer p, int radius) {
        if (!(p.level() instanceof ServerLevel level)) return null;
        int minCx = (p.blockPosition().getX() - radius) >> 4, maxCx = (p.blockPosition().getX() + radius) >> 4;
        int minCz = (p.blockPosition().getZ() - radius) >> 4, maxCz = (p.blockPosition().getZ() + radius) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                var chunk = level.getChunk(cx, cz);
                for (var be : chunk.getBlockEntities().values()) {
                    if (be instanceof net.minecraft.world.Container) {
                        return be.getBlockState().getBlock().getName().getString();
                    }
                }
            }
        }
        return null;
    }

    /** 领域期间里香常驻：不在就召一只，在就不断续期。 */
    private static void ensureRika(ServerPlayer p) {
        RikaEntity rika = RikaRules.find(p);
        if (rika == null) {
            if (!(p.level() instanceof ServerLevel level)) return;
            RikaEntity fresh = Content.RIKA.get().create(level);
            if (fresh == null) return;
            fresh.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
            fresh.setOwner(p.getUUID());
            fresh.setLifetime(RIKA_KEEPALIVE);
            level.addFreshEntity(fresh);
        } else {
            rika.setLifetime(RIKA_KEEPALIVE);
        }
    }

    private static void play(ServerPlayer p, String id) {
        var sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(id));
        if (sound != null) p.serverLevel().playSound(null, p.blockPosition(), sound, SoundSource.PLAYERS, 1.6f, 1f);
    }

    private YutaDomain() {}
}
