package character.kashimo;

import cn.blockforge.generated.nnnnr1201forge2.Curse;
import cn.blockforge.generated.nnnnr1201forge2.Network;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 鹿紫云一的电荷记账。
 * <p>正电荷挂在敌人自己的持久数据里（不占角色存档），另用一张 UUID→世界 的索引表支持「引爆无视距离」；
 * 负电荷挂在玩家自己的角色存档里（切角色自动快照）。
 */
public final class KashimoCharges {
    /** 正电荷：最多 3 层，10 秒（200 刻）刷新。 */
    public static final int MAX_LAYERS = 3, CHARGE_TICKS = 200;
    /** 负电荷上限：20 层。 */
    public static final int MAX_NEG = 20;
    /** 超过这么多刻没命中，负电荷每 20 刻掉一层。 */
    public static final int NEG_IDLE_TICKS = 160;

    public static final String NEG = "KashimoNeg", NEG_AT = "KashimoNegAt";
    private static final String LAYERS = "KashimoLayers", UNTIL = "KashimoUntil", BY = "KashimoBy";
    private static final Map<UUID, ServerLevel> INDEX = new HashMap<>();

    /** 给目标挂正电荷（叠加，最多 3 层），并把"谁挂的"记进索引。 */
    public static void apply(ServerPlayer owner, LivingEntity target, int layers) {
        if (layers <= 0 || !(target.level() instanceof ServerLevel level)) return;
        CompoundTag tag = target.getPersistentData();
        long now = level.getGameTime();
        int current = now < tag.getLong(UNTIL) ? tag.getInt(LAYERS) : 0;
        int next = Math.min(MAX_LAYERS, current + layers);
        tag.putInt(LAYERS, next);
        tag.putLong(UNTIL, now + CHARGE_TICKS);
        tag.putString(BY, owner.getUUID().toString());
        INDEX.put(target.getUUID(), level);
        sync(level, target, next, CHARGE_TICKS);
    }

    /** 当前层数（过期即 0）。 */
    public static int layers(LivingEntity target) {
        CompoundTag tag = target.getPersistentData();
        if (target.level().getGameTime() >= tag.getLong(UNTIL)) return 0;
        return Math.max(0, Math.min(MAX_LAYERS, tag.getInt(LAYERS)));
    }

    public static boolean charged(LivingEntity target) { return layers(target) > 0; }

    /** 引爆/消散：清空层数并通知客户端收起头顶图标。 */
    public static void clear(LivingEntity target) {
        CompoundTag tag = target.getPersistentData();
        tag.remove(LAYERS);
        tag.remove(UNTIL);
        tag.remove(BY);
        if (target.level() instanceof ServerLevel level) sync(level, target, 0, 1);
    }

    /** 这名玩家挂上的、仍然带着正电荷的敌人（遍历时顺手清掉过期条目）。 */
    public static List<LivingEntity> chargedBy(ServerPlayer owner) {
        List<LivingEntity> out = new ArrayList<>();
        ServerLevel level = owner.serverLevel();
        if (level == null) return out;
        String by = owner.getUUID().toString();
        var it = INDEX.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue() != level) continue;
            if (!(level.getEntity(entry.getKey()) instanceof LivingEntity living) || !living.isAlive()
                || layers(living) <= 0 || !by.equals(living.getPersistentData().getString(BY))) {
                it.remove();
                continue;
            }
            out.add(living);
        }
        out.sort(Comparator.comparingDouble(living -> living.distanceToSqr(owner)));
        return out;
    }

    /** 每 20 刻清一次过期条目，避免索引表无限增长。 */
    public static void prune() {
        var it = INDEX.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (!(entry.getValue().getEntity(entry.getKey()) instanceof LivingEntity living)
                || !living.isAlive() || layers(living) <= 0) {
                it.remove();
            }
        }
    }

    /** 断开连接/关服时清空：索引里都是世界对象，不能跨存档留着。 */
    public static void reset() { INDEX.clear(); }

    // ------------------------------------------------------------ 负电荷
    public static int negative(ServerPlayer p) {
        return Math.max(0, Math.min(MAX_NEG, Curse.data(p).getInt(NEG)));
    }

    public static void setNegative(ServerPlayer p, int value) {
        int v = Math.max(0, Math.min(MAX_NEG, value));
        CompoundTag data = Curse.data(p);
        data.putInt(NEG, v);
        data.putLong(NEG_AT, p.level().getGameTime());
        syncNegative(p);
    }

    public static void addNegative(ServerPlayer p, int amount) {
        setNegative(p, negative(p) + amount);
    }

    /** 负电荷要广播：别的玩家也能看见"身上电弧越积越亮"，只是自己第一人称时本地不画。 */
    public static void syncNegative(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        Network.send(level, new Network.Effect(KashimoFx.NEG_CHARGE, p.getId(), p.position(),
            Vec3.ZERO, negative(p), 0, 0));
    }

    private static void sync(ServerLevel level, LivingEntity target, int layers, int life) {
        Network.send(level, new Network.Effect(KashimoFx.POS_CHARGE, target.getId(), target.position(),
            Vec3.ZERO, layers, life, 0));
    }

    /** 让被拉动的实体客户端立刻跟上位置（teleportTo 之后补一次速度包）。 */
    public static void syncMotion(LivingEntity target) {
        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        } else {
            target.hurtMarked = true;
        }
    }

    private KashimoCharges() {}
}
