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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 禅院直哉 · 投射咒法（服务端权威）。
 *
 * <p>**投影节点**是整套玩法的核心：`设定投影` 开 10 秒模式 → 玩家每移动 0.45 格记一个节点；
 * 右键蓄力松开可手动补节点；`释放` 或各投射技的「左键执行」都沿这些节点结算。
 *
 * <p>投射技（右半轮盘）分两种：执行动作（直线投射 / Z 字折返 / 强制同化 / 视距截断 / 空中走位），
 * 都由"选格 → 留节点 → 左键执行"触发。
 */
public final class NaoyaCombat {
    // ── 数值 ─────────────────────────────────────────────
    public static final int PROJECT_TICKS = 200;        // 设定投影 10 秒
    public static final double NODE_STEP = 0.45;        // 每移动这么远记一个节点
    public static final int NODE_CAP = 24;
    public static final int NODE_LIFE = 200;            // 节点活 10 秒
    public static final double CHARGE_MIN = 5, CHARGE_MAX = 20;
    public static final int MUD_TICKS = 60;             // 帧数泥沼 3 秒
    public static final double LINE_DAMAGE = 14, LINE_STEP = 10;
    public static final double ZIGZAG_DAMAGE = 9, ZIGZAG_SECONDS = 0.5;
    public static final double ASSIMILATE_DAMAGE = 26;
    public static final int ASSIMILATE_LOCK = 40;       // 锁帧 2 秒
    public static final double RELEASE_DAMAGE = 16;
    public static final double SKYSTEP_UP = 1.5, SKYSTEP_FORWARD = 1.5;

    // ── 状态键 ───────────────────────────────────────────
    static final String K_UNTIL = "NaoyaProjectUntil", K_START = "NaoyaStart", K_NODES = "NaoyaNodes",
        K_NODES_AT = "NaoyaNodesAt", K_SELECTED = "NaoyaSelected", K_CHARGE = "NaoyaChargeAt",
        K_MUD = "NaoyaMudUntil", K_GHOST = "NaoyaGhostAt";

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
                String name = switch (skill) {
                    case NaoyaSkills.LINE -> "直线投射";
                    case NaoyaSkills.ZIGZAG -> "Z 字折返";
                    case NaoyaSkills.ASSIMILATE -> "强制同化";
                    case NaoyaSkills.INTERCEPT -> "视距截断";
                    case NaoyaSkills.SKYSTEP -> "空中走位";
                    default -> "投射";
                };
                p.displayClientMessage(Component.literal("已选：" + name + " · 右键蓄力留节点，左键执行")
                    .withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    private static void startProjection(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        long n = now(p);
        data.putLong(K_UNTIL, n + PROJECT_TICKS);
        data.putDouble(K_START + "X", p.getX());
        data.putDouble(K_START + "Y", p.getY());
        data.putDouble(K_START + "Z", p.getZ());
        data.putString(K_NODES, node(p.position()));
        data.putLong(K_NODES_AT, n + PROJECT_TICKS + NODE_LIFE);
        data.putLong(K_GHOST, n + PROJECT_TICKS);
        ServerLevel level = p.serverLevel();
        Network.send(level, new Network.Effect(NaoyaFx.GHOST, p.getId(), p.position(), p.getLookAngle(),
            1f, PROJECT_TICKS, 0));
        Network.sendTo(p, new Network.Effect(NaoyaFx.PROJECT, p.getId(), p.position(), Vec3.ZERO,
            1f, PROJECT_TICKS, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
        p.sendSystemMessage(Component.literal("设定投影：10 秒内每移动一段就记录一个投影（右键可手动补点）")
            .withStyle(ChatFormatting.AQUA));
    }

    private static void release(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        var nodes = nodes(data);
        if (nodes.size() < 2) {
            p.displayClientMessage(Component.literal("没有足够的投影：先放「设定投影」并移动").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        travel(p, nodes, RELEASE_DAMAGE, true);
        data.remove(K_UNTIL);
        data.remove(K_GHOST);
        Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.RELEASE, p.getId(), p.position(), Vec3.ZERO,
            nodes.size(), 20, 0));
    }

    /** 沿节点路径瞬移穿梭，路径上的敌人受伤并挂泥沼。 */
    private static void travel(ServerPlayer p, java.util.List<Vec3> nodes, double damage, boolean mud) {
        ServerLevel level = p.serverLevel();
        for (int i = 1; i < nodes.size(); i++) {
            Vec3 from = nodes.get(i - 1), to = nodes.get(i);
            hurtAlong(p, from, to, damage, mud);
            Network.send(level, new Network.Effect(NaoyaFx.TRAIL, p.getId(), from,
                to.subtract(from), 1f, 8, 0));
        }
        Vec3 last = nodes.get(nodes.size() - 1);
        p.teleportTo(last.x, last.y, last.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    private static void hurtAlong(ServerPlayer p, Vec3 from, Vec3 to, double damage, boolean mud) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.01) return;
        Vec3 step = delta.normalize();
        for (double t = 0; t <= length; t += 0.8) {
            Vec3 at = from.add(step.scale(t));
            for (LivingEntity target : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    new AABB(at, at).inflate(1.4),
                    e -> e != p && e.isAlive() && !e.isSpectator() && CombatSettings.canHit(p, e)
                        && e.getBoundingBox().inflate(0.6).contains(at))) {
                CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), (float) damage);
                if (mud) mud(p, target);
                Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.HIT, target.getId(), at, Vec3.ZERO,
                    1f, 6, 0));
            }
        }
    }

    /** 帧数泥沼：移速 -50%、攻速 -50%，3 秒。 */
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
                if (hit == null) { p.displayClientMessage(Component.literal("准星没有落点").withStyle(ChatFormatting.GRAY), true); return; }
                addNode(data, hit, n);
                Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.NODE, p.getId(), hit, Vec3.ZERO,
                    1f, NODE_LIFE, 0));
                p.displayClientMessage(Component.literal("投影节点 " + nodes(data).size() + "（" + String.format("%.1f", range) + " 格）")
                    .withStyle(ChatFormatting.AQUA), true);
            }
            case EXECUTE -> execute(p, data, n);
            default -> { }
        }
    }

    private static void execute(ServerPlayer p, CompoundTag data, long n) {
        int selected = data.getInt(K_SELECTED);
        var nodes = nodes(data);
        switch (selected) {
            case NaoyaSkills.LINE -> line(p, nodes);
            case NaoyaSkills.ZIGZAG -> zigzag(p, nodes);
            case NaoyaSkills.ASSIMILATE -> assimilate(p, nodes);
            case NaoyaSkills.INTERCEPT -> intercept(p);
            case NaoyaSkills.SKYSTEP -> skystep(p, nodes);
            default -> p.displayClientMessage(Component.literal("先在轮盘右半选一个投射技").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** 1. 直线投射：沿节点方向突进 10 格，路径伤害 + 泥沼。 */
    private static void line(ServerPlayer p, java.util.List<Vec3> nodes) {
        Vec3 origin = p.position();
        Vec3 dir = nodes.isEmpty() ? p.getLookAngle() : nodes.get(nodes.size() - 1).subtract(origin).multiply(1, 0, 1);
        if (dir.lengthSqr() < 0.01) dir = p.getLookAngle().multiply(1, 0, 1);
        dir = dir.normalize();
        Vec3 to = origin.add(dir.scale(LINE_STEP));
        hurtAlong(p, origin, to, LINE_DAMAGE, true);
        p.teleportTo(to.x, to.y, to.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.TRAIL, p.getId(), origin, to.subtract(origin), 2f, 10, 0));
        p.displayClientMessage(Component.literal("直线投射").withStyle(ChatFormatting.AQUA), true);
    }

    /** 2. Z 字折返：4 个节点、0.5 秒内跑完，多段切割。 */
    private static void zigzag(ServerPlayer p, java.util.List<Vec3> nodes) {
        if (nodes.size() < 4) {
            p.displayClientMessage(Component.literal("Z 字折返需要 4 个节点（现在 " + nodes.size() + "）")
                .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        var path = nodes.subList(Math.max(0, nodes.size() - 4), nodes.size());
        for (int i = 1; i < path.size(); i++) {
            hurtAlong(p, path.get(i - 1), path.get(i), ZIGZAG_DAMAGE, true);
            Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.TRAIL, p.getId(), path.get(i - 1),
                path.get(i).subtract(path.get(i - 1)), 1.6f, 12, 0));
        }
        Vec3 last = path.get(path.size() - 1);
        p.teleportTo(last.x, last.y, last.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.displayClientMessage(Component.literal("Z 字折返 · " + path.size() + " 点").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** 3. 强制同化：对带泥沼的目标绕三圈，锁帧 2 秒，最后一击重击。 */
    private static void assimilate(ServerPlayer p, java.util.List<Vec3> nodes) {
        LivingEntity target = null;
        double best = 64;
        for (LivingEntity living : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(p.position(), p.position()).inflate(8),
                e -> e != p && e.isAlive() && CombatSettings.canHit(p, e) && mudded(e))) {
            double d = living.distanceToSqr(p);
            if (d < best) { best = d; target = living; }
        }
        if (target == null) {
            p.displayClientMessage(Component.literal("附近没有挂着「帧数泥沼」的目标").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ServerLevel level = p.serverLevel();
        Vec3 center = target.position();
        for (int i = 0; i < 12; i++) {                       // 三圈 × 4 个采样位
            double angle = i * (Math.PI * 2 / 4);
            Vec3 at = center.add(Math.cos(angle) * 1.8, 0, Math.sin(angle) * 1.8);
            Network.send(level, new Network.Effect(NaoyaFx.TRAIL, p.getId(), at, center.subtract(at).normalize(), 1f, 6, 0));
        }
        p.teleportTo(center.x + 1.6, center.y, center.z + 1.6);
        p.hurtMarked = true;
        lock(target, ASSIMILATE_LOCK);
        CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), (float) ASSIMILATE_DAMAGE);
        Network.send(level, new Network.Effect(NaoyaFx.HIT, target.getId(), center, Vec3.ZERO, 2f, 14, 0));
        p.displayClientMessage(Component.literal("强制同化 · 锁帧 " + (ASSIMILATE_LOCK / 20) + " 秒")
            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** 4. 视距截断：瞬移到弹道前方，把弹幕拍碎。 */
    private static void intercept(ServerPlayer p) {
        Projectile nearest = null;
        double best = 400;
        for (Projectile projectile : p.serverLevel().getEntitiesOfClass(Projectile.class,
                new AABB(p.position(), p.position()).inflate(20), t -> t.isAlive() && t.getOwner() != p)) {
            double d = projectile.distanceToSqr(p);
            if (d < best) { best = d; nearest = projectile; }
        }
        if (nearest == null) {
            p.displayClientMessage(Component.literal("附近没有可截断的弹幕").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Vec3 shot = nearest.position();
        Vec3 front = shot.add(nearest.getDeltaMovement().normalize().scale(2));
        p.teleportTo(front.x, front.y, front.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        nearest.discard();
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.6f);
        Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.INTERCEPT, p.getId(), shot, Vec3.ZERO, 1f, 12, 0));
        p.displayClientMessage(Component.literal("视距截断").withStyle(ChatFormatting.AQUA), true);
    }

    /** 5. 空中走位：空中以 45 度阶梯连续上跳。 */
    private static void skystep(ServerPlayer p, java.util.List<Vec3> nodes) {
        if (p.onGround()) {
            p.displayClientMessage(Component.literal("空中走位要离地才能用").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int steps = Math.min(6, Math.max(2, nodes.size()));
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 at = p.position();
        for (int i = 0; i < steps; i++) {
            at = at.add(dir.scale(SKYSTEP_FORWARD)).add(0, SKYSTEP_UP, 0);
            if (!p.level().noCollision(p, p.getBoundingBox().move(at.subtract(p.position())))) break;
            Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.NODE, p.getId(), at, Vec3.ZERO, 1f, 10, 0));
        }
        p.teleportTo(at.x, at.y, at.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        Network.send(p.serverLevel(), new Network.Effect(NaoyaFx.TRAIL, p.getId(), p.position(), Vec3.ZERO, 2f, 10, 0));
        p.displayClientMessage(Component.literal("空中走位 · " + steps + " 阶").withStyle(ChatFormatting.AQUA), true);
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

    // ── 每刻：设定投影记录节点 ────────────────────────────
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!CharacterService.naoya(p)) return;
        CompoundTag data = Curse.data(p);
        long n = now(p);
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
    }

    // ── 节点读写 ─────────────────────────────────────────
    private static String node(Vec3 at) {
        return String.format(java.util.Locale.ROOT, "%.3f,%.3f,%.3f", at.x, at.y, at.z);
    }

    private static void addNode(CompoundTag data, Vec3 at, long n) {
        var list = nodes(data);
        list.add(at);
        while (list.size() > NODE_CAP) list.remove(0);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) builder.append(';');
            builder.append(node(list.get(i)));
        }
        data.putString(K_NODES, builder.toString());
        data.putLong(K_NODES_AT, Math.max(data.getLong(K_NODES_AT), n + NODE_LIFE));
    }

    public static java.util.List<Vec3> nodes(CompoundTag data) {
        java.util.List<Vec3> list = new java.util.ArrayList<>();
        String raw = data.getString(K_NODES);
        if (raw.isEmpty()) return list;
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

    private static Vec3 raycast(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        var hit = p.serverLevel().clip(new net.minecraft.world.level.ClipContext(eye, end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : hit.getLocation();
    }

    /** 创造恢复符 / 切角色：清掉投影状态与节点。 */
    public static void testReset(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        data.remove(K_UNTIL);
        data.remove(K_NODES);
        data.remove(K_NODES_AT);
        data.remove(K_GHOST);
        data.remove(K_SELECTED);
        data.remove(K_CHARGE);
        data.remove(K_START + "X");
        data.remove(K_START + "Y");
        data.remove(K_START + "Z");
        Network.sendTo(p, new Network.Effect(NaoyaFx.GHOST, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
        Network.sendTo(p, new Network.Effect(NaoyaFx.PROJECT, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
    }

    private NaoyaCombat() {}
}
