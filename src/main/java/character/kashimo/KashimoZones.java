package character.kashimo;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 鹿紫云一的「场上物件」：静电场、球形闪电、延迟落雷、被甩飞的敌人、雷霆万钧蓄力、雷影化折返。
 * <p>都由服务端每刻推进（{@link KashimoCombat#serverTick} 调 {@link #tick}），视觉一律走 Network.Effect。
 */
public final class KashimoZones {
    // 静电立场
    public static final double FIELD_RADIUS = 10;      // 半径 10 格
    public static final int FIELD_TICKS = 400;         // 20 秒
    // 球形闪电
    public static final int ORB_TICKS = 100;           // 5 秒后大爆炸
    public static final double ORB_SPEED = 0.45;
    public static final double ORB_HIT_RADIUS = 2.2;
    public static final double ORB_TICK_DAMAGE = 6;
    public static final double ORB_BLAST_RADIUS = 6;
    // 落雷
    public static final int BOLT_DELAY = 20;           // 延迟 1 秒
    public static final double BOLT_RADIUS = 5;
    // 电磁吸附·极
    public static final int FLING_TICKS = 90;
    public static final double FLING_RADIUS = 4;
    // 雷霆万钧
    public static final int CHAIN_CHARGE_TICKS = 40;   // 蓄力 2 秒
    public static final int CHAIN_HOPS = 5;
    public static final double CHAIN_FIRST_RANGE = 12, CHAIN_RANGE = 8, CHAIN_GROWTH = 1.35;
    // 雷影化
    public static final int SHADOW_HOPS = 4, SHADOW_STEP = 3;
    public static final double SHADOW_RANGE = 7;

    private record Field(ServerLevel level, Vec3 center, UUID owner, long until) {}
    private record Orb(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir, long born, boolean water) {}
    private record Warn(ServerLevel level, Vec3 pos, UUID owner, long at) {}
    private record Flung(ServerLevel level, UUID owner, UUID target, long born) {}
    private record Await(ServerLevel level, UUID owner, long at) {}
    private record Hop(ServerLevel level, UUID owner, List<Vec3> points, int step, long next) {}

    private static final List<Field> FIELDS = new ArrayList<>();
    private static final List<Orb> ORBS = new ArrayList<>();
    private static final List<Warn> WARNS = new ArrayList<>();
    private static final List<Flung> FLUNG = new ArrayList<>();
    private static final List<Await> AWAITS = new ArrayList<>();
    private static final List<Hop> HOPS = new ArrayList<>();

    // ------------------------------------------------------------ 对外：放下这些物件
    public static void field(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        FIELDS.add(new Field(level, owner.position(), owner.getUUID(), now + FIELD_TICKS));
        Network.send(level, new Network.Effect(KashimoFx.FIELD, owner.getId(), owner.position(),
            Vec3.ZERO, (float) FIELD_RADIUS, FIELD_TICKS, 0));
        level.playSound(null, owner.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1.6f);
        owner.displayClientMessage(Component.literal("静电立场：半径 " + (int) FIELD_RADIUS + " 格 · 20 秒"), true);
    }

    public static void orb(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        Vec3 dir = owner.getLookAngle();
        Vec3 pos = owner.getEyePosition().add(dir.scale(1.2));
        boolean water = inWater(level, pos);
        ORBS.add(new Orb(level, owner.getUUID(), pos, dir.normalize(), level.getGameTime(), water));
        Network.send(level, new Network.Effect(KashimoFx.ORB, owner.getId(), pos, dir, 1.1f, ORB_TICKS, 0));
        level.playSound(null, owner.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_RIPTIDE_2,
            net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.7f);
        owner.displayClientMessage(Component.literal(water ? "球形闪电（水中：范围×3、伤害减半）" : "球形闪电"), true);
    }

    public static void warn(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        Vec3 eye = owner.getEyePosition(), look = owner.getLookAngle();
        var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, eye.add(look.scale(48)),
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, owner));
        Vec3 at = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? eye.add(look.scale(48)) : hit.getLocation();
        WARNS.add(new Warn(level, at, owner.getUUID(), level.getGameTime() + BOLT_DELAY));
        Network.send(level, new Network.Effect(KashimoFx.BOLT_WARN, owner.getId(), at, Vec3.ZERO,
            (float) BOLT_RADIUS, BOLT_DELAY, 0));
        level.playSound(null, BlockPos.containing(at), net.minecraft.sounds.SoundEvents.TRIDENT_THUNDER,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.8f);
        owner.displayClientMessage(Component.literal("落雷：1 秒后降临"), true);
    }

    public static void fling(ServerPlayer owner, LivingEntity target, Vec3 dir) {
        if (!(target.level() instanceof ServerLevel level)) return;
        target.setDeltaMovement(dir.scale(2.1).add(0, 0.35, 0));
        target.hurtMarked = true;
        FLUNG.add(new Flung(level, owner.getUUID(), target.getUUID(), level.getGameTime()));
        Network.send(level, new Network.Effect(KashimoFx.ENVELOPE, target.getId(), target.position(),
            dir, 1f, FLING_TICKS, 0));
        level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_RETURN,
            net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.6f);
    }

    public static void chainCharge(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        AWAITS.add(new Await(level, owner.getUUID(), now + CHAIN_CHARGE_TICKS));
        Network.send(level, new Network.Effect(KashimoFx.CHAIN_DARK, owner.getId(), owner.position(),
            Vec3.ZERO, 1f, CHAIN_CHARGE_TICKS, 0));
        owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHAIN_CHARGE_TICKS, 9, false, false));
        level.playSound(null, owner.blockPosition(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.65f);
        owner.displayClientMessage(Component.literal("雷霆万钧：蓄力 2 秒"), true);
    }

    public static void shadow(ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        Vec3 look = owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        List<Vec3> points = new ArrayList<>();
        Vec3 from = owner.position();
        for (int i = 1; i <= SHADOW_HOPS; i++) {
            Vec3 want = from.add(flat.scale(SHADOW_RANGE)).add(side.scale((i % 2 == 0 ? 1 : -1) * 3.5));
            Wall wall = snap(level, owner, from, want);
            points.add(wall.pos());
            Network.send(level, new Network.Effect(KashimoFx.SHADOW, owner.getId(), from, wall.pos(), 1f, 12, 0));
            from = wall.pos();
        }
        HOPS.add(new Hop(level, owner.getUUID(), points, 0, level.getGameTime()));
        owner.displayClientMessage(Component.literal("雷影化：折返 " + SHADOW_HOPS + " 次"), true);
    }

    private record Wall(Vec3 pos) {}

    /** 落点：沿视线方向撞墙就贴在墙前；否则落地。 */
    private static Wall snap(ServerLevel level, ServerPlayer owner, Vec3 from, Vec3 want) {
        Vec3 eye = from.add(0, owner.getEyeHeight(), 0);
        var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, want.add(0, owner.getEyeHeight(), 0),
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, owner));
        Vec3 at = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? want
            : hit.getLocation().subtract(want.subtract(from).normalize().scale(0.6));
        BlockPos pos = BlockPos.containing(at);
        for (int dy = 1; dy >= -4; dy--) {
            BlockPos below = pos.offset(0, dy - 1, 0), self = pos.offset(0, dy, 0);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                && level.getBlockState(self).getCollisionShape(level, self).isEmpty())
                return new Wall(new Vec3(at.x, self.getY(), at.z));
        }
        return new Wall(new Vec3(at.x, from.y, at.z));
    }

    // ------------------------------------------------------------ 每刻推进
    public static void tick(MinecraftServer server) {
        if (server == null) return;
        long now = server.overworld().getGameTime();
        tickFields(server, now);
        tickOrbs(server, now);
        tickWarns(server, now);
        tickFlung(server, now);
        tickChain(server, now);
        tickHops(server, now);
    }

    private static void tickFields(MinecraftServer server, long now) {
        if (FIELDS.isEmpty()) return;
        List<Field> keep = new ArrayList<>();
        for (Field f : FIELDS) {
            ServerPlayer owner = server.getPlayerList().getPlayer(f.owner());
            if (owner == null || now >= f.until()) continue;
            if (now % 10 == 0) {
                for (var target : f.level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(f.center(), f.center()).inflate(FIELD_RADIUS + 1),
                        t -> t.isAlive() && !t.isSpectator() && CombatSettings.canHit(owner, t))) {
                    if (target.distanceToSqr(f.center()) > FIELD_RADIUS * FIELD_RADIUS) continue;
                    KashimoCharges.apply(owner, target, 1);
                    if (KashimoCharges.layers(target) >= KashimoCharges.MAX_LAYERS && !KashimoCombat.magnetized(owner))
                        KashimoCombat.magnetizeByField(owner);
                }
            }
            if (now % 20 == 0 && owner.level() == f.level()
                && owner.position().distanceToSqr(f.center()) <= FIELD_RADIUS * FIELD_RADIUS) {
                // 咒力回复翻倍：基础回复每秒结算一次，这里再补同样一份
                Curse.addRaw(owner, Math.max(1, character.CharacterService.stats(owner).recovery()));
            }
            keep.add(f);
        }
        FIELDS.clear();
        FIELDS.addAll(keep);
    }

    private static void tickOrbs(MinecraftServer server, long now) {
        if (ORBS.isEmpty()) return;
        List<Orb> keep = new ArrayList<>();
        for (Orb orb : ORBS) {
            ServerPlayer owner = server.getPlayerList().getPlayer(orb.owner());
            if (owner == null) continue;
            if (now - orb.born() >= ORB_TICKS) {
                blast(owner, orb.level(), orb.pos(), orb.water(), ORB_BLAST_RADIUS, KashimoSkills.ORB_DAMAGE);
                continue;
            }
            Vec3 pos = orb.pos(), dir = orb.dir();
            if (solid(orb.level(), pos.add(dir.x * ORB_SPEED, 0, 0))) dir = new Vec3(-dir.x, dir.y, dir.z);
            else pos = pos.add(dir.x * ORB_SPEED, 0, 0);
            if (solid(orb.level(), pos.add(0, dir.y * ORB_SPEED, 0))) dir = new Vec3(dir.x, -dir.y, dir.z);
            else pos = pos.add(0, dir.y * ORB_SPEED, 0);
            if (solid(orb.level(), pos.add(0, 0, dir.z * ORB_SPEED))) dir = new Vec3(dir.x, dir.y, -dir.z);
            else pos = pos.add(0, 0, dir.z * ORB_SPEED);
            Network.send(orb.level(), new Network.Effect(KashimoFx.ORB, owner.getId(), pos, dir, 1.1f,
                ORB_TICKS - (int) (now - orb.born()), 0));
            double radius = orb.water() ? ORB_HIT_RADIUS * 3 : ORB_HIT_RADIUS;
            if (now % 4 == 0) {
                for (var target : orb.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(radius),
                        t -> t.isAlive() && !t.isSpectator() && CombatSettings.canHit(owner, t))) {
                    if (target.distanceToSqr(pos) > radius * radius) continue;
                    KashimoCombat.zap(owner, target, orb.water() ? ORB_TICK_DAMAGE * 0.5 : ORB_TICK_DAMAGE);
                }
            }
            keep.add(new Orb(orb.level(), orb.owner(), pos, dir, orb.born(), orb.water()));
        }
        ORBS.clear();
        ORBS.addAll(keep);
    }

    private static void tickWarns(MinecraftServer server, long now) {
        if (WARNS.isEmpty()) return;
        List<Warn> keep = new ArrayList<>();
        for (Warn warn : WARNS) {
            if (now < warn.at()) { keep.add(warn); continue; }
            ServerPlayer owner = server.getPlayerList().getPlayer(warn.owner());
            if (owner == null) continue;
            List<LivingEntity> charged = new ArrayList<>();
            for (var target : warn.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(warn.pos(), warn.pos()).inflate(BOLT_RADIUS),
                    t -> t.isAlive() && !t.isSpectator() && CombatSettings.canHit(owner, t))) {
                if (target.distanceToSqr(warn.pos()) <= BOLT_RADIUS * BOLT_RADIUS && KashimoCharges.charged(target))
                    charged.add(target);
            }
            if (charged.isEmpty()) {
                // 没有带电荷的目标：就劈标记点最近的那个
                warn.level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(warn.pos(), warn.pos()).inflate(BOLT_RADIUS),
                        t -> t.isAlive() && !t.isSpectator() && CombatSettings.canHit(owner, t))
                    .stream().min(Comparator.comparingDouble(t -> t.distanceToSqr(warn.pos())))
                    .ifPresent(charged::add);
            }
            for (LivingEntity target : charged) {
                if (strike(owner, target, KashimoSkills.BOLT_DAMAGE)) target.setSecondsOnFire(3);
            }
            Network.send(warn.level(), new Network.Effect(KashimoFx.STRIKE, owner.getId(), warn.pos(),
                Vec3.ZERO, 2.2f, 20 * 15, owner.getId()));
            warn.level().playSound(null, BlockPos.containing(warn.pos()), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.8f);
        }
        WARNS.clear();
        WARNS.addAll(keep);
    }

    private static void tickFlung(MinecraftServer server, long now) {
        if (FLUNG.isEmpty()) return;
        List<Flung> keep = new ArrayList<>();
        for (Flung flung : FLUNG) {
            ServerPlayer owner = server.getPlayerList().getPlayer(flung.owner());
            if (owner == null) continue;
            var entity = flung.level().getEntity(flung.target());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) continue;
            long age = now - flung.born();
            boolean landed = age > 3 && (target.onGround() || target.getDeltaMovement().lengthSqr() < 0.02);
            if (!landed && age < FLING_TICKS) {
                Network.send(flung.level(), new Network.Effect(KashimoFx.ENVELOPE, target.getId(), target.position(),
                    Vec3.ZERO, 1f, 6, 0));
                keep.add(flung);
                continue;
            }
            blast(owner, flung.level(), target.position(), false, FLING_RADIUS, KashimoSkills.FLING_DAMAGE);
        }
        FLUNG.clear();
        FLUNG.addAll(keep);
    }

    private static void tickChain(MinecraftServer server, long now) {
        if (AWAITS.isEmpty()) return;
        List<Await> keep = new ArrayList<>();
        for (Await await : AWAITS) {
            if (now < await.at()) { keep.add(await); continue; }
            ServerPlayer owner = server.getPlayerList().getPlayer(await.owner());
            if (owner == null) continue;
            chain(owner, await.level());
        }
        AWAITS.clear();
        AWAITS.addAll(keep);
    }

    private static void chain(ServerPlayer owner, ServerLevel level) {
        LivingEntity current = nearest(level, owner, owner.position(), CHAIN_FIRST_RANGE, new HashSet<>());
        if (current == null) {
            owner.sendSystemMessage(Component.literal("雷霆万钧：附近没有敌人").withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }
        Set<UUID> hit = new HashSet<>();
        double damage = KashimoSkills.CHAIN_DAMAGE;
        Vec3 from = owner.getEyePosition();
        for (int hop = 0; hop < CHAIN_HOPS && current != null; hop++) {
            hit.add(current.getUUID());
            Vec3 at = current.position().add(0, current.getBbHeight() * 0.5, 0);
            Network.send(level, new Network.Effect(KashimoFx.CHAIN_LINK, owner.getId(), from, at, 1f, 10, 0));
            strike(owner, current, damage);
            from = at;
            damage *= CHAIN_GROWTH;
            current = nearest(level, owner, current.position(), CHAIN_RANGE, hit);
        }
        level.playSound(null, owner.blockPosition(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
            net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.1f);
    }

    private static void tickHops(MinecraftServer server, long now) {
        if (HOPS.isEmpty()) return;
        List<Hop> keep = new ArrayList<>();
        for (Hop hop : HOPS) {
            ServerPlayer owner = server.getPlayerList().getPlayer(hop.owner());
            if (owner == null) continue;
            if (now < hop.next()) { keep.add(hop); continue; }
            if (hop.step() >= hop.points().size()) continue;
            Vec3 to = hop.points().get(hop.step());
            Network.send(hop.level(), new Network.Effect(KashimoFx.DASH, owner.getId(), owner.position(), to, 1f, 12, 0));
            owner.teleportTo(to.x, to.y, to.z);
            owner.setDeltaMovement(Vec3.ZERO);
            owner.fallDistance = 0;
            Network.send(hop.level(), new Network.Effect(KashimoFx.DISCHARGE, owner.getId(), to, Vec3.ZERO, 2.5f, 10, 0));
            int step = hop.step() + 1;
            if (step < hop.points().size()) keep.add(new Hop(hop.level(), hop.owner(), hop.points(), step, now + SHADOW_STEP));
        }
        HOPS.clear();
        HOPS.addAll(keep);
    }

    // ------------------------------------------------------------ 工具
    private static LivingEntity nearest(ServerLevel level, ServerPlayer owner, Vec3 around, double range, Set<UUID> skip) {
        LivingEntity best = null;
        double bestDistance = range * range;
        for (var target : level.getEntitiesOfClass(LivingEntity.class, new AABB(around, around).inflate(range + 1),
                t -> t.isAlive() && !t.isSpectator() && !skip.contains(t.getUUID()) && CombatSettings.canHit(owner, t))) {
            double d = target.position().distanceToSqr(around);
            if (d < bestDistance) { bestDistance = d; best = target; }
        }
        return best;
    }

    /** 一次爆炸：范围内所有人吃一道雷（水中范围×3、伤害减半），并画出大面积焦痕。 */
    private static void blast(ServerPlayer owner, ServerLevel level, Vec3 at, boolean water, double radius, double damage) {
        double r = water ? radius * 3 : radius;
        double dmg = water ? damage * 0.5 : damage;
        Network.send(level, new Network.Effect(KashimoFx.ORB_BLAST, owner.getId(), at, Vec3.ZERO, (float) r, 20, 0));
        for (var target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
                t -> t.isAlive() && !t.isSpectator() && CombatSettings.canHit(owner, t))) {
            if (target.distanceToSqr(at) > r * r) continue;
            strike(owner, target, dmg);
        }
        level.playSound(null, BlockPos.containing(at), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
            net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.9f);
    }

    private static boolean strike(ServerPlayer owner, LivingEntity target, double damage) {
        return KashimoCombat.strike(owner, target, damage);
    }

    private static boolean solid(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    public static boolean inWater(ServerLevel level, Vec3 at) {
        return level.getFluidState(BlockPos.containing(at)).is(FluidTags.WATER);
    }

    /** 退出世界/关服：所有物件都是世界对象，直接清空。 */
    public static void reset() {
        FIELDS.clear();
        ORBS.clear();
        WARNS.clear();
        FLUNG.clear();
        AWAITS.clear();
        HOPS.clear();
    }

    private KashimoZones() {}
}
