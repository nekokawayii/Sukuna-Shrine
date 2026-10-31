package character.hakari;

import character.CharacterSkill;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 秤金次的战斗实现：咒力拳、连打、开盘抽奖、头奖状态。
 * <p>头奖（4 分 11 秒 = 5020 刻）期间：咒力每 0.5 秒回满、每秒回 10% 最大生命、伤害 ×2。
 */
public final class HakariCombat {
    /** 头奖持续时间（刻）：4 分 11 秒。 */
    public static final int JACKPOT_TICKS = 5020;
    /** 中奖率基础值 / 每次失败加成 / 上限。 */
    private static final double BASE_CHANCE = 0.10, PITY_STEP = 0.05, MAX_CHANCE = 0.95;

    private static final String JACKPOT_UNTIL = "HakariJackpotUntil";
    private static final String PITY = "HakariPity";

    private static net.minecraft.nbt.CompoundTag data(Player p) {
        return cn.blockforge.generated.nnnnr1201forge2.Curse.data(p);
    }

    /** 头奖是否生效。 */
    public static boolean jackpot(Player p) {
        return p != null && data(p).getLong(JACKPOT_UNTIL) > p.level().getGameTime();
    }

    private static int pity(Player p) { return data(p).getInt(PITY); }

    private static List<LivingEntity> around(ServerPlayer p, Vec3 center, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
            e -> e != p && e.isAlive() && !(e instanceof Player q && q.isCreative()));
    }

    /** 咒力拳。 */
    public static void punch(ServerPlayer p, CharacterSkill skill, int rank) {
        punchOnce(p, skill, rank, 1.0);
    }

    /** 连打：1 秒内五拳。 */
    public static void barrage(ServerPlayer p, CharacterSkill skill, int rank) {
        for (int i = 0; i < 5; i++) {
            final int index = i;
            p.server.tell(new TickTask(index * 4, () -> {
                if (p.isAlive()) punchOnce(p, skill, rank, 0.8);
            }));
        }
        p.displayClientMessage(Component.literal("必杀·连打"), true);
    }

    private static void punchOnce(ServerPlayer p, CharacterSkill skill, int rank, double scale) {
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        double range = skill.range();
        float damage = (float) (skill.damageAt(rank) * scale * (jackpot(p) ? 2.0 : 1.0));
        for (LivingEntity e : around(p, eye.add(dir.scale(range * 0.5)), range * 0.6 + skill.radius())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > range) continue;
            if (to.normalize().dot(dir) < Math.cos(Math.toRadians(skill.halfAngle()))) continue;
            e.hurt(p.damageSources().playerAttack(p), damage);
            Vec3 kb = to.normalize().scale(1.8);
            e.push(kb.x, 0.6, kb.z);
            e.hurtMarked = true;
        }
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(jackpot(p) ? ParticleTypes.FLAME : ParticleTypes.CRIT,
                eye.x + dir.x * 2, eye.y + dir.y * 2, eye.z + dir.z * 2, 10, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.9f, jackpot(p) ? 1.3f : 1.0f);
        }
    }

    /** 开盘：抽一次头奖。 */
    public static void spin(ServerPlayer p, CharacterSkill skill, int rank) {
        trySpin(p, true);
    }

    /** 抽奖核心；免费模式用于领域内自动开奖。 */
    public static void trySpin(ServerPlayer p, boolean paid) {
        double chance = Math.min(MAX_CHANCE, BASE_CHANCE + pity(p) * PITY_STEP);
        var data = data(p);
        if (p.getRandom().nextDouble() < chance) {
            data.putLong(JACKPOT_UNTIL, p.level().getGameTime() + JACKPOT_TICKS);
            data.putInt(PITY, 0);
            p.displayClientMessage(Component.literal("★ 中头奖！4 分 11 秒无限咒力 ★"), false);
            if (p.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1, p.getZ(), 80, 1.2, 1.0, 1.2, 0.12);
                level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundSource.PLAYERS, 1.5f, 1f);
            }
        } else {
            data.putInt(PITY, pity(p) + 1);
            p.displayClientMessage(Component.literal("未中奖 · 下次中奖率 "
                + Math.round(Math.min(MAX_CHANCE, BASE_CHANCE + pity(p) * PITY_STEP) * 100) + "%"), true);
            if (p.level() instanceof ServerLevel level) {
                level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.get(),
                    SoundSource.PLAYERS, 0.8f, 0.6f);
            }
            if (paid) {
                // 未中奖返还一部分咒力，避免开盘完全空转
                cn.blockforge.generated.nnnnr1201forge2.Curse.add(p, 200);
            }
        }
    }

    /** 领域展开。 */
    public static void domain(ServerPlayer p, CharacterSkill skill, int rank) {
        HakariDomain.open(p);
    }

    /** 头奖维持：回咒、回血、周期特效。 */
    public static void jackpotTick(ServerPlayer p) {
        if (!jackpot(p)) return;
        var data = data(p);
        long now = p.level().getGameTime();
        long left = data.getLong(JACKPOT_UNTIL) - now;
        if (now % 10 == 0) {
            cn.blockforge.generated.nnnnr1201forge2.Curse.add(p, p.getMaxHealth() * 1000);
        }
        if (now % 20 == 0) {
            p.heal(p.getMaxHealth() * 0.10f);
            cn.blockforge.generated.nnnnr1201forge2.Curse.sync(p);
            if (p.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(),
                    6, 0.6, 0.8, 0.6, 0.02);
            }
            if (left <= 200 && left > 0) {
                p.displayClientMessage(Component.literal("头奖剩余 " + (left / 20) + " 秒"), true);
            }
        }
    }

    /** 头奖维持（由 GeneratedMod 的玩家 tick 监听调用）。 */
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer p) || p.level().isClientSide) return;
        jackpotTick(p);
    }

    private HakariCombat() {}
}
