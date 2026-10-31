package character.ryu;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 石流龙 · 咒力强化/放出。
 *
 * <p>被动「咒力超载」：上限 10000、每秒只回 5（写在 {@link RyuStyle} 的 Stats 里）。
 * 轮盘：1 冰沙冲击波（持续放出的 3D 冲击波）/ 2 追踪弹 / 3 咒力喷涌 / 4 甜品时间 / 5 领域展开（占位）。
 *
 * <p>冰沙冲击波：按住就持续喷，**每秒扣 100 咒力**；宽度从 1 格起、**每 2 秒 +1 格、无上限**；
 * 判定是圆锥体积（枪口半径 = 宽度/2，随距离缓慢扩张）→ 真正有体积的 3D 冲击波。
 */
public final class RyuCombat {
    // 冰沙冲击波（持续型）
    public static final double BLAST_LENGTH = 30;
    public static final double BLAST_BASE_WIDTH = 1.0;      // 起手 1 格宽
    public static final double BLAST_WIDTH_PER_2S = 1.0;    // 每 2 秒 +1 格，无上限
    public static final double BLAST_SPREAD = 0.06;         // 每格距离再扩张的半径
    public static final double BLAST_TICK_COST = 100;       // 每秒扣 100 咒力
    public static final double BLAST_DAMAGE_BASE = 8, BLAST_DAMAGE_PER_WIDTH = 2;
    public static final double BLAST_KNOCKBACK = 1.2;
    public static final int BLAST_DAMAGE_INTERVAL = 4, BLAST_PUSH_INTERVAL = 12, BLAST_EFFECT_INTERVAL = 4;
    // 追踪弹
    public static final int SCATTER_MIN = 3, SCATTER_MAX = 5, SCATTER_LIFE = 70;
    public static final double SCATTER_SPEED = 0.62, SCATTER_TURN = 0.30;
    public static final double SCATTER_DAMAGE = 18, SCATTER_TRACK = 20;
    // 咒力喷涌
    public static final double BURST_RADIUS = 3, BURST_DAMAGE = 20, BURST_KNOCKBACK = 1.7;
    // 甜品时间
    public static final int DESSERT_TICKS = 40;
    // 领域占位
    public static final double DOMAIN_RADIUS = 8;

    private static final String CHANNEL_START = "RyuBlastStart", DESSERT_UNTIL = "RyuDessertUntil";
    private static final String TOGGLE_AT = "RyuBlastToggleAt";
    private static final String NPC_ENERGY = "RyuNpcEnergy", NPC_CHANNEL_START = "RyuNpcBlastStart";
    private static final String NPC_CHANNEL_END = "RyuNpcBlastEnd";
    public static final double NPC_MAX_ENERGY = 10000, NPC_REGEN = 5;
    public static final int NPC_CHANNEL_TICKS = 80;

    /** 追踪弹：玩家 / NPC 共用（owner 与 caster 二选一）。 */
    private record Missile(ServerLevel level, ServerPlayer owner, Mob caster, Vec3 pos, Vec3 dir, long born, double damage) {}
    private static final List<Missile> MISSILES = new ArrayList<>();

    public static boolean isRyu(Player p) {
        var d = character.CharacterService.selected(p);
        return d != null && "ryu".equals(d.id());
    }

    private static long now(LivingEntity e) { return e.level().getGameTime(); }

    // ------------------------------------------------------------ 冰沙冲击波：持续放出
    /**
     * 冰沙冲击波：**按一下开、再按一下关**（咒力见底也会自动关）。
     * <p>做成开关而不是"按住"是因为输入侧的按住心跳在某些情况下会丢，
     * 表现成"只喷一下"。开关模式下哪怕只收到一次 phase 0 也能一直喷。
     */
    public static void startCharge(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        long now = now(p);
        // 防抖：4 刻内的重复按下忽略，避免输入侧抖动把开关连点两次（表现成"只喷一下"）
        if (now - data.getLong(TOGGLE_AT) < 4) return;
        data.putLong(TOGGLE_AT, now);
        if (data.getLong(CHANNEL_START) != 0) {
            // 已经开着 → 这一次按下当作"关"
            data.remove(CHANNEL_START);
            Network.sendTo(p, new Network.Effect(RyuFx.CHARGE, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
            com.mojang.logging.LogUtils.getLogger().info("[石流龙] 冲击波关闭 {}", p.getScoreboardName());
            p.displayClientMessage(Component.literal("冰沙冲击波 · 关闭"), true);
            return;
        }
        data.putLong(CHANNEL_START, now);
        // 起手立刻结算一发：这样"点一下"也一定看得到光（以前要等 4 刻的取模才发第一跳）
        pulse(p, p, BLAST_BASE_WIDTH, BLAST_DAMAGE_BASE + BLAST_BASE_WIDTH * BLAST_DAMAGE_PER_WIDTH, true, true);
        com.mojang.logging.LogUtils.getLogger().info("[石流龙] 冲击波开始 {}（起手已开火）", p.getScoreboardName());
        // 起手那一下后坐力（约退 2 格）
        Vec3 back = p.getLookAngle().normalize().scale(-1.25);
        p.setDeltaMovement(p.getDeltaMovement().add(back.x, 0.18, back.z));
        p.hurtMarked = true;
        if (p.level() instanceof ServerLevel level) {
            level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.9f, 1.3f);
        }
    }

    public static void holdCharge(ServerPlayer p) {
        if (Curse.data(p).getLong(CHANNEL_START) == 0) startCharge(p);
    }

    /** 松手/取消：**不关**冲击波（开关模式下靠再按一次开关键关闭）。只把蓄力提示收掉。 */
    public static void releaseCharge(ServerPlayer p, boolean fire) {
        Network.sendTo(p, new Network.Effect(RyuFx.CHARGE, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
    }

    /** 当前宽度：1 格起，每 2 秒 +1 格，无上限。 */
    public static double channelWidth(long elapsedTicks) {
        return BLAST_BASE_WIDTH + (elapsedTicks / 20.0) / 2.0 * BLAST_WIDTH_PER_2S;
    }

    /**
     * 一次冲击波结算：圆锥体积（枪口半径 = 宽度/2，随距离扩张），
     * 伤害与击退都按"是否在体积内"判定。
     */
    private static void pulse(LivingEntity caster, ServerPlayer owner, double width, double damage,
                              boolean knockback, boolean effect) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        Vec3 origin = origin(caster), dir = caster.getLookAngle().normalize();
        Vec3 end = origin.add(dir.scale(BLAST_LENGTH));
        var blockHit = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();
        if (effect) {
            Network.send(level, new Network.Effect(RyuFx.BEAM, caster.getId(), origin, end, (float) width, 8, 0));
        }
        double length = end.distanceTo(origin);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, end).inflate(width + 2),
                t -> t != caster && t.isAlive() && !t.isSpectator())) {
            Vec3 delta = target.getBoundingBox().getCenter().subtract(origin);
            double along = delta.dot(dir);
            if (along < -0.5 || along > length + 1) continue;
            double radius = width * 0.5 + Math.max(0, along) * BLAST_SPREAD;
            double perpendicular = delta.subtract(dir.scale(along)).length();
            if (perpendicular > radius + Math.max(target.getBbWidth(), 0.6) * 0.5) continue;
            if (damage > 0) hurt(level, caster, owner, target, damage);
            if (knockback) {
                Vec3 push = dir.scale(BLAST_KNOCKBACK).add(0, 0.3, 0);
                target.setDeltaMovement(target.getDeltaMovement().add(push));
                target.hurtMarked = true;
            }
        }
    }

    /** 飞机头前端：眼睛高度再往前 0.8 格。 */
    private static Vec3 origin(LivingEntity caster) {
        return caster.getEyePosition().add(caster.getLookAngle().scale(0.8));
    }

    private static void hurt(ServerLevel level, LivingEntity caster, ServerPlayer owner, LivingEntity target, double damage) {
        if (owner != null) {
            AttackTypes.mark(AttackTypes.MAGIC);
            try {
                CombatSettings.hurt(owner, target, owner.damageSources().indirectMagic(owner, owner),
                    CombatSettings.spellDamage(owner, damage));
            } finally {
                AttackTypes.clear();
            }
        } else if (caster instanceof Mob mob) {
            NpcCombat.hurt(mob, target, (float) damage);
        }
    }

    // ------------------------------------------------------------ 追踪弹
    public static void scatter(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        int count = Math.min(SCATTER_MAX, SCATTER_MIN + character.CharacterService.rank(p) / 2);
        Vec3 dir = p.getLookAngle().normalize();
        for (int i = 0; i < count; i++) {
            Vec3 side = dir.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
            side = side.normalize();
            Vec3 offset = side.scale((i - (count - 1) / 2.0) * 0.35);
            MISSILES.add(new Missile(level, p, null, origin(p).add(offset), dir, level.getGameTime(), SCATTER_DAMAGE));
        }
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.9f, 1.5f);
        p.displayClientMessage(Component.literal("追踪弹 ×" + count), true);
    }

    private static void npcScatterInternal(Mob mob, LivingEntity target) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin(mob)).normalize();
        for (int i = 0; i < SCATTER_MIN; i++) {
            MISSILES.add(new Missile(level, null, mob, origin(mob), dir, level.getGameTime(), SCATTER_DAMAGE));
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 0.9f, 1.4f);
    }

    public static void npcScatter(Mob mob, LivingEntity target) {
        if (!(mob.level() instanceof ServerLevel level) || !npcSpend(mob, 300)) return;
        npcScatterInternal(mob, target);
    }

    /** 追踪弹每刻推进（服务端 tick 调）：朝最近敌人转向，撞到就炸。 */
    public static void tickMissiles(long now) {
        if (MISSILES.isEmpty()) return;
        List<Missile> keep = new ArrayList<>();
        for (Missile missile : MISSILES) {
            if (now - missile.born() > SCATTER_LIFE) continue;
            LivingEntity ownerEntity = missile.owner() != null ? missile.owner() : missile.caster();
            if (ownerEntity == null || !ownerEntity.isAlive()) continue;
            ServerLevel level = missile.level();
            LivingEntity target = nearest(level, ownerEntity, missile.pos(), SCATTER_TRACK);
            Vec3 dir = missile.dir();
            if (target != null) {
                Vec3 want = target.getBoundingBox().getCenter().subtract(missile.pos()).normalize();
                dir = dir.scale(1 - SCATTER_TURN).add(want.scale(SCATTER_TURN)).normalize();
            }
            Vec3 pos = missile.pos().add(dir.scale(SCATTER_SPEED));
            Network.send(level, new Network.Effect(RyuFx.MISSILE, ownerEntity.getId(), pos, dir, 1f, 4, 0));
            LivingEntity hit = null;
            for (var candidate : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(1.0),
                    t -> t != ownerEntity && t.isAlive() && !t.isSpectator() && t.getBoundingBox().inflate(0.4).contains(pos))) {
                hit = candidate;
                break;
            }
            if (hit != null) {
                hurt(level, ownerEntity, missile.owner(), hit, missile.damage());
                level.playSound(null, hit.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 1.8f);
                continue;
            }
            var pos0 = net.minecraft.core.BlockPos.containing(pos);
            if (!level.getBlockState(pos0).getCollisionShape(level, pos0).isEmpty()) continue;
            keep.add(new Missile(level, missile.owner(), missile.caster(), pos, dir, missile.born(), missile.damage()));
        }
        MISSILES.clear();
        MISSILES.addAll(keep);
    }

    private static LivingEntity nearest(ServerLevel level, LivingEntity caster, Vec3 around, double range) {
        LivingEntity best = null;
        double bestDistance = range * range;
        for (var candidate : level.getEntitiesOfClass(LivingEntity.class, new AABB(around, around).inflate(range),
                t -> t != caster && t.isAlive() && !t.isSpectator())) {
            double d = candidate.position().distanceToSqr(around);
            if (d < bestDistance) { bestDistance = d; best = candidate; }
        }
        return best;
    }

    // ------------------------------------------------------------ 咒力喷涌
    public static void burst(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        area(level, p, p, BURST_RADIUS, BURST_DAMAGE, BURST_KNOCKBACK);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.6f);
        p.displayClientMessage(Component.literal("咒力喷涌"), true);
    }

    private static void area(ServerLevel level, LivingEntity caster, ServerPlayer owner, double radius,
                             double damage, double knockback) {
        Network.send(level, new Network.Effect(RyuFx.BURST, caster.getId(), caster.position(), Vec3.ZERO,
            (float) radius, 12, 0));
        for (var target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius),
                t -> t != caster && t.isAlive() && !t.isSpectator())) {
            if (target.distanceToSqr(caster) > radius * radius) continue;
            hurt(level, caster, owner, target, damage);
            Vec3 push = target.position().subtract(caster.position());
            if (push.lengthSqr() < 1e-4) push = new Vec3(1, 0, 0);
            push = push.normalize().scale(knockback);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.45, push.z));
            target.hurtMarked = true;
        }
    }

    // ------------------------------------------------------------ 甜品时间
    public static void dessert(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        data.putLong(DESSERT_UNTIL, now(p) + DESSERT_TICKS);
        if (p.level() instanceof ServerLevel level) {
            Network.send(level, new Network.Effect(RyuFx.DESSERT, p.getId(), p.position(), Vec3.ZERO,
                1f, DESSERT_TICKS, 0));
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1f, 1f);
        }
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DESSERT_TICKS, 9, false, false));
        p.sendSystemMessage(Component.literal("甜品时间（2 秒）：不攻击、咒力回复翻倍").withStyle(ChatFormatting.AQUA));
        p.displayClientMessage(Component.literal("甜品时间 · 回蓝中"), true);
    }

    public static boolean dessertActive(Player p) {
        return Curse.data(p).getLong(DESSERT_UNTIL) > p.level().getGameTime();
    }

    // ------------------------------------------------------------ 领域（占位）
    public static void domain(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        Network.send(level, new Network.Effect(RyuFx.DOMAIN, p.getId(), p.position(), Vec3.ZERO,
            (float) DOMAIN_RADIUS, 20, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 0.7f);
        p.sendSystemMessage(Component.literal("领域展开·（占位）：内容尚未制作，本次只播放了起手").withStyle(ChatFormatting.GRAY));
        p.displayClientMessage(Component.literal("领域展开（占位）"), true);
    }

    // ------------------------------------------------------------ 每刻
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!isRyu(p)) return;
        long now = now(p);
        CompoundTag data = Curse.data(p);

        // ① 冰沙冲击波：持续喷，每秒 100 咒力，宽度每 2 秒 +1 格
        long start = data.getLong(CHANNEL_START);
        if (start != 0) {
            long elapsed = Math.max(0, now - start);
            double width = channelWidth(elapsed);
            if (elapsed > 20 * 120) {
                // 兜底：切角色/长时间挂着回来不会变成无限宽的怪东西
                data.remove(CHANNEL_START);
                Network.sendTo(p, new Network.Effect(RyuFx.CHARGE, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
                com.mojang.logging.LogUtils.getLogger().info("[石流龙] 冲击波超时自动关闭 {}", p.getScoreboardName());
                p.displayClientMessage(Component.literal("冰沙冲击波 · 超时自动关闭"), true);
                return;
            }
            if (elapsed % 20 == 0 && elapsed > 0 && !Curse.spend(p, BLAST_TICK_COST)) {
                data.remove(CHANNEL_START);
                Network.sendTo(p, new Network.Effect(RyuFx.CHARGE, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
                com.mojang.logging.LogUtils.getLogger().info("[石流龙] 冲击波因咒力不足中断 {}", p.getScoreboardName());
                p.sendSystemMessage(Component.literal("咒力不足：冰沙冲击波中断").withStyle(ChatFormatting.GRAY));
                p.displayClientMessage(Component.literal("咒力不足 · 中断"), true);
            } else {
                if (elapsed % BLAST_DAMAGE_INTERVAL == 0) {
                    boolean effect = (elapsed / BLAST_DAMAGE_INTERVAL) % 2 == 0;
                    pulse(p, p, width, BLAST_DAMAGE_BASE + width * BLAST_DAMAGE_PER_WIDTH, false, effect);
                }
                if (elapsed % BLAST_PUSH_INTERVAL == 0) pulse(p, p, width, 0, true, false);
                if (elapsed % 10 == 0) {
                    Network.sendTo(p, new Network.Effect(RyuFx.CHARGE, p.getId(), p.position(), Vec3.ZERO,
                        (float) width, 12, 0));
                }
            }
        }

        // ② 甜品时间：回蓝翻倍
        long dessertUntil = data.getLong(DESSERT_UNTIL);
        if (dessertUntil != 0) {
            if (now >= dessertUntil) {
                data.remove(DESSERT_UNTIL);
                Network.send(p.serverLevel(), new Network.Effect(RyuFx.DESSERT, p.getId(), p.position(),
                    Vec3.ZERO, 0f, 0, 0));
            } else if (now % 20 == 0) {
                Curse.addRaw(p, Math.max(1, character.CharacterService.stats(p).recovery()));
                p.displayClientMessage(Component.literal("甜品时间：咒力回复 ×2"), true);
            }
        }
    }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.getServer() != null) {
            tickMissiles(e.getServer().overworld().getGameTime());
        }
    }

    @SubscribeEvent public static void attack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && isRyu(p) && dessertActive(p)) {
            e.setCanceled(true);
            p.displayClientMessage(Component.literal("甜品时间：正在吃，不攻击"), true);
        }
    }

    @SubscribeEvent public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && isRyu(p)) {
            Curse.data(p).remove(DESSERT_UNTIL);
            Curse.data(p).remove(CHANNEL_START);
        }
    }

    /** 创造恢复符：清蓄力/甜品状态。 */
    public static void testReset(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        data.remove(CHANNEL_START);
        data.remove(DESSERT_UNTIL);
    }

    // ------------------------------------------------------------ NPC
    public static double npcEnergy(Mob mob) {
        CompoundTag tag = mob.getPersistentData();
        if (!tag.contains(NPC_ENERGY)) tag.putDouble(NPC_ENERGY, NPC_MAX_ENERGY);
        return tag.getDouble(NPC_ENERGY);
    }

    public static boolean npcSpend(Mob mob, double cost) {
        double energy = npcEnergy(mob);
        if (energy < cost) return false;
        mob.getPersistentData().putDouble(NPC_ENERGY, energy - cost);
        return true;
    }

    public static void npcRegen(Mob mob, double multiplier) {
        CompoundTag tag = mob.getPersistentData();
        tag.putDouble(NPC_ENERGY, Math.min(NPC_MAX_ENERGY, npcEnergy(mob) + NPC_REGEN * multiplier));
    }

    /** NPC 开始喷冲击波：持续 4 秒、每秒 100 咒力、宽度同样每 2 秒 +1。 */
    public static void npcStartBlast(Mob mob) {
        CompoundTag tag = mob.getPersistentData();
        long now = now(mob);
        tag.putLong(NPC_CHANNEL_START, now);
        tag.putLong(NPC_CHANNEL_END, now + NPC_CHANNEL_TICKS);
        pulse(mob, null, BLAST_BASE_WIDTH, BLAST_DAMAGE_BASE + BLAST_BASE_WIDTH * BLAST_DAMAGE_PER_WIDTH, true, true);
        if (mob.level() instanceof ServerLevel level) {
            level.playSound(null, mob.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.9f, 1.2f);
        }
    }

    public static boolean npcBlastActive(Mob mob) {
        CompoundTag tag = mob.getPersistentData();
        long start = tag.getLong(NPC_CHANNEL_START);
        return start != 0 && tag.getLong(NPC_CHANNEL_END) > now(mob);
    }

    /** NPC 的持续冲击波推进（每刻由实体调）。 */
    public static void npcBlastTick(Mob mob) {
        CompoundTag tag = mob.getPersistentData();
        long start = tag.getLong(NPC_CHANNEL_START);
        if (start == 0) return;
        long now = now(mob);
        if (now >= tag.getLong(NPC_CHANNEL_END) || npcEnergy(mob) < 5) {
            tag.remove(NPC_CHANNEL_START);
            tag.remove(NPC_CHANNEL_END);
            return;
        }
        double width = channelWidth(now - start);
        if (!npcSpend(mob, 5)) {
            tag.remove(NPC_CHANNEL_START);
            tag.remove(NPC_CHANNEL_END);
            return;
        }
        long elapsed = now - start;
        if (elapsed % BLAST_DAMAGE_INTERVAL == 0) {
            boolean effect = (elapsed / BLAST_DAMAGE_INTERVAL) % 2 == 0;
            pulse(mob, null, width, BLAST_DAMAGE_BASE + width * BLAST_DAMAGE_PER_WIDTH, false, effect);
        }
        if (elapsed % BLAST_PUSH_INTERVAL == 0) pulse(mob, null, width, 0, true, false);
    }

    public static void npcBurst(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        area(level, mob, null, BURST_RADIUS, BURST_DAMAGE, BURST_KNOCKBACK);
    }

    public static void npcDessert(Mob mob) {
        CompoundTag tag = mob.getPersistentData();
        tag.putLong(DESSERT_UNTIL, now(mob) + DESSERT_TICKS);
        if (mob.level() instanceof ServerLevel level) {
            Network.send(level, new Network.Effect(RyuFx.DESSERT, mob.getId(), mob.position(), Vec3.ZERO,
                1f, DESSERT_TICKS, 0));
            level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 1f, 1f);
        }
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DESSERT_TICKS, 9, false, false));
    }

    public static boolean npcDessertActive(Mob mob) {
        return mob.getPersistentData().getLong(DESSERT_UNTIL) > now(mob);
    }

    public static void reset() {
        MISSILES.clear();
    }

    private RyuCombat() {}
}
