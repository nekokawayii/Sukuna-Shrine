package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 里香的召唤与状态判定。
 * <p>「无限咒力」不是把数值设成无穷大，而是在主人身上记一个到期时刻 {@code RikaUntil}：
 * {@link Curse#rawMaximum} 在场期间直接返回一个足够大的上限，召唤结束自动恢复原值。
 */
public final class RikaRules {
    /** 两分钟。 */
    public static final int DURATION = 2400;
    /** 里香在场时的咒力上限（“无限”的工程近似）。 */
    public static final double INFINITE = 1_000_000_000d;
    private static final String UNTIL = "RikaUntil";

    public static RikaEntity find(Player p) {
        if (p == null || p.level() == null) return null;
        List<RikaEntity> list = p.level().getEntitiesOfClass(RikaEntity.class,
            p.getBoundingBox().inflate(96), e -> e.isAlive() && p.getUUID().equals(e.ownerId()));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 里香是否仍在场（服务端权威；客户端只读同步出来的实体）。 */
    public static boolean active(Player p) { return find(p) != null; }

    /** 主人是否处于「无限咒力」窗口：必须是乙骨，且 {@link #UNTIL} 未到期。 */
    public static boolean infiniteEnergy(Player p) {
        if (p == null || p.level().isClientSide) return false;
        var d = character.CharacterService.selected(p);
        if (d == null || !"yuta".equals(d.id())) return false;
        return p.level().getGameTime() < p.getPersistentData().getLong(UNTIL);
    }

    public static void markUntil(Player p, long untilTick) { p.getPersistentData().putLong(UNTIL, untilTick); }
    public static void clear(Player p) { p.getPersistentData().remove(UNTIL); }

    /** 召唤：同一主人同时只能有一只里香。返回是否成功。 */
    public static boolean summon(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel server)) return false;
        RikaEntity old = find(p);
        long until = server.getGameTime() + DURATION;
        if (old != null) {
            // 重召顶掉旧里香：刷新时长而不是叠加两只。
            old.setLifetime(DURATION);
            markUntil(p, until);
            character.yuta.YutaCopies.resetUses(p);
            p.displayClientMessage(Component.literal("里香已重新展开（剩余 120 秒）"), true);
            return true;
        }
        RikaEntity rika = Content.RIKA.get().create(server);
        if (rika == null) return false;
        rika.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        rika.setOwner(p.getUUID());
        rika.setLifetime(DURATION);
        if (!server.addFreshEntity(rika)) return false;
        markUntil(p, until);
        server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.8f, 0.9f, 1f), 1.2f),
            p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
        server.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.4f, 0.7f);
        p.displayClientMessage(Component.literal("里香，来一下。"), true);
        return true;
    }

    /** 复制术式专用：里香只出现 3 秒，咬一口目标后消失，不占用召唤名额、也不给无限咒力。 */
    public static RikaEntity flashBite(ServerPlayer p, net.minecraft.world.entity.LivingEntity target) {
        if (!(p.level() instanceof ServerLevel server)) return null;
        RikaEntity rika = Content.RIKA.get().create(server);
        if (rika == null) return null;
        if (target != null) {
            double dx = target.getX() - p.getX(), dz = target.getZ() - p.getZ();
            double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
            rika.moveTo(target.getX() - dx / len * 1.2, p.getY(), target.getZ() - dz / len * 1.2, p.getYRot(), 0);
        } else {
            rika.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        }
        rika.setOwner(p.getUUID());
        rika.setLifetime(60);
        rika.forceBite(target);
        server.addFreshEntity(rika);
        server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.8f, 0.9f, 1f), 1.0f),
            target == null ? p.getX() : target.getX(), (target == null ? p.getY() : target.getY()) + 1,
            target == null ? p.getZ() : target.getZ(), 24, 0.4, 0.5, 0.4, 0.03);
        server.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EAT,
            net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.7f);
        return rika;
    }

    private RikaRules() {}
}
