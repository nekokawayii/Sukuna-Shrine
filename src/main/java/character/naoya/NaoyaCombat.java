package character.naoya;

import cn.blockforge.generated.nnnnr1201forge2.*;
import character.CharacterService;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 禅院直哉 · 投射咒法（服务端权威）。
 *
 * <p>**投影 = 一条真实的路线**：设定投影记录 10 秒内的移动节点；释放与各投射技都会真的
 * **沿着这些点一帧一帧走过去**（每刻按弧长插值传送、沿途结算伤害），不是瞬移到终点。
 *
 * <p>①设定投影：残影的起点**始终跟随玩家当前位置**，朝准星方向显示；
 * ②释放：沿记录到的节点真实跑一遍；
 * ③右半五个投射技：**自动生成自己的投影点**（不用手动留节点），同样真实跑一遍。
 */
public final class NaoyaCombat {
    // ── 数值 ─────────────────────────────────────────────
    public static final int PROJECT_TICKS = 200;        // 设定投影 10 秒
    public static final double NODE_STEP = 0.45;
    public static final int NODE_CAP = 128;             // 投影点在 10 秒里最多记 128 个（0.45 格一个）
    public static final int NODE_LIFE = 600;            // 节点活 30 秒，投影结束后仍能释放
    public static final double CHARGE_MIN = 5, CHARGE_MAX = 20;
    public static final int MUD_TICKS = 60;
    public static final double TRAVEL_SPEED = 2.2;      // 每刻走 2.2 格（残影冲刺）
    public static final int TRAVEL_MIN = 4;
    public static final double LINE_DAMAGE = 14, LINE_STEP = 10;
    public static final double ZIGZAG_DAMAGE = 9;
    public static final double ASSIMILATE_DAMAGE = 26;
    public static final int ASSIMILATE_LOCK = 40;
    public static final double RELEASE_DAMAGE = 16;
    public static final double SKYSTEP_UP = 1.5, SKYSTEP_FORWARD = 1.5;

    // ── 状态键 ───────────────────────────────────────────
    static final String K_UNTIL = "NaoyaProjectUntil", K_NODES = "NaoyaNodes", K_NODES_AT = "NaoyaNodesAt",
        K_SELECTED = "NaoyaSelected", K_CHARGE = "NaoyaChargeAt", K_MUD = "NaoyaMudUntil",
        K_GHOST = "NaoyaGhostAt", K_GHOST_TICK = "NaoyaGhostNext",
        K_TR_PATH = "NaoyaTravelPath", K_TR_START = "NaoyaTravelStart", K_TR_TICKS = "NaoyaTravelTicks",
        K_TR_END = "NaoyaTravelEnd", K_TR_TARGET = "NaoyaTravelTarget", K_TR_MUD = "NaoyaTravelMud",
        K_TR_DAMAGE = "NaoyaTravelDamage", K_TR_HIT = "NaoyaTravelHitAt", K_TR_DEALT = "NaoyaTravelDealt";

    /** 走完之后的收尾动作。 */
    private static final int END_NONE = 0, END_ASSIMILATE = 1, END_INTERCEPT = 2, END_SKYSTEP = 3;

    // ── 输入 token（客户端 → 服务端） ─────────────────────
    public static final int CHARGE = 0, PLACE = 1, EXECUTE = 2;

    public static long now(net.minecraft.world.entity.Entity e) { return e.level().getGameTime(); }

    // ── 施法入口 ─────────────────────────────────────────
    public static void cast(ServerPlayer p, int skill) {
        if (!CharacterService.naoya(p) || !p.isAlive() || p.isSpectator()) return;
        switch (skill) {
            case NaoyaSkills.PROJECT -> startProjection(p);
            case NaoyaSkills.RELEASE -> release(p);
            case NaoyaSkills.DOMAIN -> {
                Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.DOMAIN, p.getId(), p.position(),
                    Vec3.ZERO, 1f, 20, 0));
                p.sendSystemMessage(Component.literal("领域展开（占位）：直哉的领域还没做，本次只播放起手")
                    .withStyle(ChatFormatting.GRAY));
            }
            default -> {
                Curse.data(p).putInt(K_SELECTED, skill);
                technique(p, Curse.data(p), skill);      // 释放键直接释放：自动生成投影点并立刻冲过去
            }
        }
    }

    private static String name(int skill) {
        return switch (skill) {
            case NaoyaSkills.LINE -> "直线投射";
            case NaoyaSkills.ZIGZAG -> "Z 字折返";
            case NaoyaSkills.ASSIMILATE -> "强制同化";
            case NaoyaSkills.INTERCEPT -> "视距截断";
            case NaoyaSkills.SKYSTEP -> "空中走位";
            default -> "投射";
        };
    }

    /** 设定投影：10 秒记录移动节点；残影起点始终是玩家当前位置。 */
    private static void startProjection(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        long n = now(p);
        data.putLong(K_UNTIL, n + PROJECT_TICKS);
        data.putString(K_NODES, node(p.position()));
        data.putLong(K_NODES_AT, n + PROJECT_TICKS + NODE_LIFE);
        data.putLong(K_GHOST, n + PROJECT_TICKS);
        data.putLong(K_GHOST_TICK, 0);
        ServerLevel level = p.serverLevel();
        Network.send(level, new Network.Effect(NaoyaFx.GHOST, p.getId(), p.position(), p.getLookAngle(),
            1f, PROJECT_TICKS, 0));
        Network.sendTo(p, new Network.Effect(NaoyaFx.PROJECT, p.getId(), p.position(), Vec3.ZERO, 1f, PROJECT_TICKS, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
        p.sendSystemMessage(Component.literal("设定投影：10 秒内每移动一段就记录一个投影（右键也能手动补点）")
            .withStyle(ChatFormatting.AQUA));
    }

    /** 释放：沿记录到的节点真实跑一遍。 */
    private static void release(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        var nodes = nodes(data);
        if (nodes.size() < 2) {
            p.displayClientMessage(Component.literal("没有足够的投影：先放「设定投影」并移动").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        startTravel(p, nodes, RELEASE_DAMAGE, true, END_NONE, -1);
        data.remove(K_UNTIL);
        data.remove(K_GHOST);
        Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.RELEASE, p.getId(), p.position(), Vec3.ZERO,
            nodes.size(), 20, 0));
    }

    // ── 客户端输入 ───────────────────────────────────────
    public static void input(ServerPlayer p, int token) {
        if (!CharacterService.naoya(p) || !p.isAlive()) return;
        CompoundTag data = Curse.data(p);
        long n = now(p);
        switch (token) {
            case CHARGE -> data.putLong(K_CHARGE, n);
            case PLACE -> {
                long charged = n - data.getLong(K_CHARGE);
                double range = Math.max(CHARGE_MIN, Math.min(CHARGE_MAX, CHARGE_MIN + charged / 20.0 * 2.5));
                Vec3 hit = raycast(p, range);
                if (hit == null) {
                    p.displayClientMessage(Component.literal("准星没有落点").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                addNode(data, hit, n);
                Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.NODE, p.getId(), hit, Vec3.ZERO, 1f, NODE_LIFE, 0));
                p.displayClientMessage(Component.literal("投影节点 " + nodes(data).size() + "（"
                    + String.format(java.util.Locale.ROOT, "%.1f", range) + " 格）").withStyle(ChatFormatting.AQUA), true);
            }
            case EXECUTE -> execute(p, data);
            default -> { }
        }
    }

    /** 左键执行：跑当前选中的那个技能（保留给"先选再按左键"的旧习惯）。 */
    private static void execute(ServerPlayer p, CompoundTag data) {
        technique(p, data, data.getInt(K_SELECTED));
    }

    /** 五个投射技：自动生成各自的投影点，然后真实跑一遍。 */
    private static void technique(ServerPlayer p, CompoundTag data, int selected) {
        if (data.getLong(K_TR_START) != 0) return;                       // 正在穿梭中
        switch (selected) {
            case NaoyaSkills.LINE -> {
                Vec3 dir = flatLook(p);
                startTravel(p, path(p.position(), dir.scale(LINE_STEP)), LINE_DAMAGE, true, END_NONE, -1);
                say(p, "直线投射");
            }
            case NaoyaSkills.ZIGZAG -> {
                Vec3 look = flatLook(p), side = new Vec3(-look.z, 0, look.x);
                var route = new java.util.ArrayList<Vec3>();
                route.add(p.position().add(look.scale(4)).add(side.scale(3)));
                route.add(p.position().add(look.scale(8)).subtract(side.scale(3)));
                route.add(p.position().add(look.scale(12)).add(side.scale(3)));
                route.add(p.position().add(look.scale(16)));
                startTravel(p, route, ZIGZAG_DAMAGE, true, END_NONE, -1);
                say(p, "Z 字折返 · 4 点");
            }
            case NaoyaSkills.ASSIMILATE -> {
                LivingEntity target = muddedNear(p, 8);
                if (target == null) {
                    p.displayClientMessage(Component.literal("附近没有挂着「帧数泥沼」的目标").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                Vec3 center = target.position();
                var route = new java.util.ArrayList<Vec3>();
                for (int i = 0; i < 12; i++) {                           // 三圈
                    double angle = i * (Math.PI * 2 / 4);
                    route.add(center.add(Math.cos(angle) * 1.8, 0, Math.sin(angle) * 1.8));
                }
                startTravel(p, route, 0, false, END_ASSIMILATE, target.getId());
                say(p, "强制同化 · 三圈");
            }
            case NaoyaSkills.INTERCEPT -> {
                Projectile shot = incoming(p, 20);
                if (shot == null) {
                    p.displayClientMessage(Component.literal("附近没有可截断的弹幕").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                Vec3 front = shot.position().add(shot.getDeltaMovement().normalize().scale(2));
                startTravel(p, path(p.position(), front.subtract(p.position())), 0, false, END_INTERCEPT, shot.getId());
                say(p, "视距截断");
            }
            case NaoyaSkills.SKYSTEP -> {
                if (p.onGround()) {
                    p.displayClientMessage(Component.literal("空中走位要离地才能用").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                Vec3 dir = flatLook(p);
                var route = new java.util.ArrayList<Vec3>();
                Vec3 at = p.position();
                for (int i = 0; i < 5; i++) {
                    at = at.add(dir.scale(SKYSTEP_FORWARD)).add(0, SKYSTEP_UP, 0);
                    route.add(at);
                }
                startTravel(p, route, 4, false, END_SKYSTEP, -1);
                say(p, "空中走位 · 5 阶");
            }
            default -> p.displayClientMessage(Component.literal("先在轮盘右半选一个投射技").withStyle(ChatFormatting.GRAY), true);
        }
    }

    private static void say(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.AQUA), true);
    }

    // ── 沿投影真实穿梭 ───────────────────────────────────
    private static void startTravel(ServerPlayer p, java.util.List<Vec3> route, double damage, boolean mud,
                                    int endKind, int targetId) {
        if (route.size() < 2) return;
        CompoundTag data = Curse.data(p);
        long n = now(p);
        double length = 0;
        for (int i = 1; i < route.size(); i++) length += route.get(i).distanceTo(route.get(i - 1));
        int ticks = Math.max(TRAVEL_MIN, (int) Math.ceil(length / TRAVEL_SPEED));
        data.putString(K_TR_PATH, encode(route));
        data.putLong(K_TR_START, n);
        data.putLong(K_TR_TICKS, ticks);
        data.putLong(K_TR_END, endKind);
        data.putInt(K_TR_TARGET, targetId);
        data.putBoolean(K_TR_MUD, mud);
        data.putDouble(K_TR_DAMAGE, damage);
        data.putLong(K_TR_HIT, 0);
        data.putInt(K_TR_DEALT, 0);
        Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.RELEASE, p.getId(), p.position(), Vec3.ZERO,
            route.size(), ticks, 0));
        p.setDeltaMovement(0, 0, 0);
        p.hurtMarked = true;
    }

    /** 每刻沿路线推进（弧长插值），到达终点执行收尾。 */
    private static void tickTravel(ServerPlayer p, CompoundTag data, long n) {
        long start = data.getLong(K_TR_START);
        if (start == 0) return;
        long ticks = Math.max(1, data.getLong(K_TR_TICKS));
        var route = decode(data.getString(K_TR_PATH));
        if (route.size() < 2) { clearTravel(data); return; }
        double progress = Math.min(1, (n - start) / (double) ticks);
        Vec3 at = pointAt(route, progress);
        double damage = data.getDouble(K_TR_DAMAGE);
        if (damage > 0 && n - data.getLong(K_TR_HIT) >= 2) {
            data.putLong(K_TR_HIT, n);
            hurtNear(p, at, damage, data.getBoolean(K_TR_MUD), data);
        }
        Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.TRAIL, p.getId(), at, flatLook(p).scale(1.2),
            1.2f, 5, 0));
        p.teleportTo(at.x, at.y, at.z);
        p.setDeltaMovement(0, 0, 0);
        p.hurtMarked = true;
        p.fallDistance = 0;
        if (progress < 1) return;
        int end = (int) data.getLong(K_TR_END);
        int targetId = data.getInt(K_TR_TARGET);
        // 至少造成一次伤害：整条路上一个都没打到的话，收尾时补一次
        boolean dealt = data.getInt(K_TR_DEALT) > 0;
        double travelDamage = data.getDouble(K_TR_DAMAGE);
        clearTravel(data);
        if (!dealt) guaranteeHit(p, at, Math.max(4, travelDamage), end == END_ASSIMILATE);
        if (end == END_INTERCEPT) {
            if (p.serverLevel().getEntity(targetId) instanceof Projectile shot) {
                Vec3 shotAt = shot.position();
                shot.discard();
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.6f);
                Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.INTERCEPT, p.getId(), shotAt,
                    Vec3.ZERO, 1f, 12, 0));
            }
            return;
        }
        if (!(p.serverLevel().getEntity(targetId) instanceof LivingEntity target)) return;
        if (end == END_ASSIMILATE) {
            lock(target, ASSIMILATE_LOCK);
            CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), (float) ASSIMILATE_DAMAGE);
            Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.HIT, target.getId(), target.position(),
                Vec3.ZERO, 2f, 14, 0));
            p.displayClientMessage(Component.literal("强制同化 · 锁帧 " + (ASSIMILATE_LOCK / 20) + " 秒")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** 保底命中：附近 4 格内最近的可打目标，至少吃一下。 */
    private static void guaranteeHit(ServerPlayer p, Vec3 at, double damage, boolean mud) {
        LivingEntity best = null;
        double distance = 16;
        for (LivingEntity living : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(4), e -> e != p && e.isAlive() && CombatSettings.canHit(p, e))) {
            double d = living.distanceToSqr(at);
            if (d < distance) { distance = d; best = living; }
        }
        if (best == null) return;
        CombatSettings.hurt(p, best, p.damageSources().playerAttack(p), (float) damage);
        if (mud) mud(p, best);
        Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.HIT, best.getId(), best.position(),
            Vec3.ZERO, 1f, 8, 0));
    }

    private static void clearTravel(CompoundTag data) {
        data.remove(K_TR_PATH);
        data.remove(K_TR_START);
        data.remove(K_TR_TICKS);
        data.remove(K_TR_END);
        data.remove(K_TR_TARGET);
        data.remove(K_TR_DAMAGE);
        data.remove(K_TR_HIT);
        data.remove(K_TR_MUD);
        data.remove(K_TR_DEALT);
    }

    /** 弧长插值取点。 */
    private static Vec3 pointAt(java.util.List<Vec3> route, double progress) {
        double total = 0;
        for (int i = 1; i < route.size(); i++) total += route.get(i).distanceTo(route.get(i - 1));
        double want = total * Math.max(0, Math.min(1, progress));
        double walked = 0;
        for (int i = 1; i < route.size(); i++) {
            double segment = route.get(i).distanceTo(route.get(i - 1));
            if (walked + segment >= want || i == route.size() - 1) {
                double t = segment <= 0.0001 ? 1 : (want - walked) / segment;
                return route.get(i - 1).add(route.get(i).subtract(route.get(i - 1)).scale(Math.max(0, Math.min(1, t))));
            }
            walked += segment;
        }
        return route.get(route.size() - 1);
    }

    private static void hurtNear(ServerPlayer p, Vec3 at, double damage, boolean mud) {
        hurtNear(p, at, damage, mud, null);
    }

    private static void hurtNear(ServerPlayer p, Vec3 at, double damage, boolean mud, CompoundTag travel) {
        for (LivingEntity target : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(1.6),
                e -> e != p && e.isAlive() && !e.isSpectator() && CombatSettings.canHit(p, e)
                    && e.getBoundingBox().inflate(0.7).contains(at))) {
            CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), (float) damage);
            if (mud) mud(p, target);
            if (travel != null) travel.putInt(K_TR_DEALT, travel.getInt(K_TR_DEALT) + 1);
            Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.HIT, target.getId(), at, Vec3.ZERO, 1f, 6, 0));
        }
    }

    /** 帧数泥沼：移速 −50%、攻速 −50%，3 秒。 */
    public static void mud(ServerPlayer p, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MUD_TICKS, 1, false, false));
        AttributeInstance attackSpeed = target.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            var id = java.util.UUID.nameUUIDFromBytes(("NaoyaMud" + target.getUUID()).getBytes());
            attackSpeed.removeModifier(id);
            attackSpeed.addTransientModifier(new AttributeModifier(id, "帧数泥沼",
                -Math.max(0.01, attackSpeed.getValue()) * 0.5, AttributeModifier.Operation.ADDITION));
        }
        target.getPersistentData().putLong(K_MUD, now(target) + MUD_TICKS);
    }

    public static boolean mudded(LivingEntity target) {
        return target.getPersistentData().getLong(K_MUD) > now(target);
    }

    private static LivingEntity muddedNear(ServerPlayer p, double radius) {
        LivingEntity best = null;
        double distance = radius * radius;
        for (LivingEntity living : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(p.position(), p.position()).inflate(radius),
                e -> e != p && e.isAlive() && CombatSettings.canHit(p, e) && mudded(e))) {
            double d = living.distanceToSqr(p);
            if (d < distance) { distance = d; best = living; }
        }
        return best;
    }

    private static Projectile incoming(ServerPlayer p, double radius) {
        Projectile best = null;
        double distance = radius * radius;
        for (Projectile projectile : p.serverLevel().getEntitiesOfClass(Projectile.class,
                new AABB(p.position(), p.position()).inflate(radius), t -> t.isAlive() && t.getOwner() != p)) {
            double d = projectile.distanceToSqr(p);
            if (d < distance) { distance = d; best = projectile; }
        }
        return best;
    }

    /** 锁帧：定身（禁 AI + 清速度），到期自动恢复。 */
    public static void lock(LivingEntity target, int ticks) {
        target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
        target.getPersistentData().putLong("NaoyaLock", now(target) + ticks);
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
            LOCKS.removeIf(entry -> entry.target == mob);
            LOCKS.add(new Lock(mob, now(target) + ticks));
        }
    }

    private record Lock(Mob target, long until) {}
    private static final java.util.List<Lock> LOCKS = new java.util.ArrayList<>();

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || LOCKS.isEmpty()) return;
        LOCKS.removeIf(lock -> {
            if (!lock.target().isAlive() || now(lock.target()) >= lock.until()) {
                if (lock.target().isAlive()) lock.target().setNoAi(false);
                return true;
            }
            return false;
        });
    }

    // ── 每刻：推进穿梭 / 记录节点 / 残影跟随 ──────────────
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!CharacterService.naoya(p)) return;
        CompoundTag data = Curse.data(p);
        long n = now(p);
        tickTravel(p, data, n);
        long until = data.getLong(K_UNTIL);
        if (until == 0) return;
        if (n >= until) { data.remove(K_UNTIL); data.remove(K_GHOST); return; }
        var nodes = nodes(data);
        Vec3 last = nodes.isEmpty() ? p.position() : nodes.get(nodes.size() - 1);
        if (p.position().distanceTo(last) >= NODE_STEP) {
            addNode(data, p.position(), n);
            data.putLong(K_NODES_AT, until + NODE_LIFE);
            Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.NODE, p.getId(), p.position(), Vec3.ZERO,
                1f, NODE_LIFE, 0));
        }
        // 残影起点始终跟随玩家当前位置，朝准星方向
        if (n >= data.getLong(K_GHOST_TICK)) {
            data.putLong(K_GHOST_TICK, n + 8);
            Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.GHOST, p.getId(), p.position(),
                p.getLookAngle(), 1f, 12, 0));
        }
    }

    // ── 节点读写 ─────────────────────────────────────────
    private static String node(Vec3 at) {
        return String.format(java.util.Locale.ROOT, "%.3f,%.3f,%.3f", at.x, at.y, at.z);
    }

    private static String encode(java.util.List<Vec3> route) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < route.size(); i++) {
            if (i > 0) builder.append(';');
            builder.append(node(route.get(i)));
        }
        return builder.toString();
    }

    private static java.util.List<Vec3> decode(String raw) {
        java.util.List<Vec3> list = new java.util.ArrayList<>();
        if (raw == null || raw.isEmpty()) return list;
        for (String part : raw.split(";")) {
            String[] pieces = part.split(",");
            if (pieces.length != 3) continue;
            try {
                list.add(new Vec3(Double.parseDouble(pieces[0]), Double.parseDouble(pieces[1]),
                    Double.parseDouble(pieces[2])));
            } catch (NumberFormatException ignored) { }
        }
        return list;
    }

    private static java.util.List<Vec3> path(Vec3 from, Vec3 delta) {
        return java.util.List.of(from, from.add(delta));
    }

    private static void addNode(CompoundTag data, Vec3 at, long n) {
        var list = nodes(data);
        list.add(at);
        while (list.size() > NODE_CAP) list.remove(0);
        data.putString(K_NODES, encode(list));
        data.putLong(K_NODES_AT, Math.max(data.getLong(K_NODES_AT), n + NODE_LIFE));
    }

    public static java.util.List<Vec3> nodes(CompoundTag data) {
        return decode(data.getString(K_NODES));
    }

    private static Vec3 flatLook(ServerPlayer p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        return look.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : look.normalize();
    }

    private static Vec3 raycast(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        var hit = p.serverLevel().clip(new net.minecraft.world.level.ClipContext(eye, end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : hit.getLocation();
    }

    /** 创造恢复符 / 切角色：清掉投影状态与穿梭。 */
    public static void testReset(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        data.remove(K_UNTIL);
        data.remove(K_NODES);
        data.remove(K_NODES_AT);
        data.remove(K_GHOST);
        data.remove(K_GHOST_TICK);
        data.remove(K_SELECTED);
        data.remove(K_CHARGE);
        clearTravel(data);
        Network.sendTo(p, new Network.Effect(NaoyaFx.GHOST, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
        Network.sendTo(p, new Network.Effect(NaoyaFx.PROJECT, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
    }

    private NaoyaCombat() {}
}
