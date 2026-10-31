package character.toji;

import cn.blockforge.generated.nnnnr1201forge2.*;
import character.CharacterService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 伏黑甚尔 · 纯 QTE 角色（天与暴君）。
 *
 * <p>核心循环：**怪物/弹幕/箭进入玩家半径 2 格 → 屏幕边缘红光 + 目标被红圈包住（0.8 秒）→ 空格完美闪避**
 * （闪身 5 格、速度 ×2、下一次攻击伤害翻倍 1 秒）→ 闪避后的 1.5 秒内按操作表接 1~20 连招。
 *
 * <p>输入：只在红光窗口与连招窗口内由客户端接管（空格/Shift+空格/Ctrl/E/F/G/R/1/2/左右键），
 * 窗口外完全不碰原版按键。
 */
public final class TojiQte {
    // ── 数值 ──────────────────────────────────────────────
    public static final double THREAT_RADIUS = 2.0;
    public static final int WINDOW_TICKS = 16;          // 红光窗口 0.8 秒
    public static final int PERFECT_TICKS = 20;         // 完美闪避后翻倍/加速持续 1 秒
    public static final int COMBO_TICKS = 100;          // 完美闪避后连招窗口 5 秒
    public static final double DODGE_DISTANCE = 5.0;    // 闪身 5 格
    public static final double BACKSTEP_DISTANCE = 2.0; // 后撤步 2 格
    public static final int IFRAMES = 12;               // 闪避无敌帧 0.6 秒
    public static final int PERFECT_PER_EXECUTE = 3;    // 暴君处决需要的完美闪避次数
    private record Stunned(LivingEntity target, long until) {}
    private static final java.util.List<Stunned> STUNNED = new java.util.ArrayList<>();
    public static final double NEXT_HIT_MULTIPLIER = 2.0;

    // ── 输入 token ────────────────────────────────────────
    public static final int DODGE = 0, BACKSTEP = 1, SLIDE = 2, ATTACK = 3, SHIFT_ATTACK = 4, USE = 5,
        WEAPON_YUYUN = 6, WEAPON_TENKA = 7, GRAB = 8, DISARM = 9, EXECUTE = 10, THROW = 11,
        AIR_DODGE = 12, CHARGE_ATTACK = 13, JUMP = 14;

    // ── 状态键 ────────────────────────────────────────────
    static final String K_WINDOW = "TojiQteUntil", K_TARGET = "TojiQteTarget", K_PERFECT = "TojiPerfectUntil",
        K_COUNT = "TojiPerfectCount", K_NEXT = "TojiNextHitUntil", K_MULT = "TojiNextHitMult",
        K_COMBO = "TojiComboUntil", K_SEQ = "TojiComboSeq", K_SEQ_AT = "TojiComboAt",
        K_IFRAME = "TojiIframesUntil", K_SLIDE = "TojiSlideUntil", K_AIR = "TojiAirDodgeUntil",
        K_POSE = "TojiPoseAt", K_STUN = "TojiStunUntil", K_PENDING = "TojiPendingAt";
    /** 输入后延迟几刻再结算，给"连点第二下/加修饰键"留出时间。 */
    public static final int COMMIT_TICKS = 5;

    public static long now(Player p) { return p.level().getGameTime(); }

    /** 生物状态数据：玩家走角色数据，其它生物走 persistentData。 */
    public static CompoundTag data(LivingEntity entity) {
        return entity instanceof Player player ? Curse.data(player) : entity.getPersistentData();
    }
    public static long now(Entity entity) { return entity.level().getGameTime(); }

    public static boolean inWindow(Player p) {
        return CharacterService.toji(p) && now(p) < Curse.data(p).getLong(K_WINDOW);
    }

    public static boolean inCombo(Player p) {
        return CharacterService.toji(p) && now(p) < Curse.data(p).getLong(K_COMBO);
    }

    public static boolean invulnerable(Player p) {
        long n = now(p);
        return n < Curse.data(p).getLong(K_IFRAME) || n < Curse.data(p).getLong(K_SLIDE);
    }

    // ── 主循环：威胁扫描 ──────────────────────────────────
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!CharacterService.toji(p)) return;
        long n = now(p);
        CompoundTag d = Curse.data(p);
        TojiCombat.tickBuffs(p);
        long pending = d.getLong(K_PENDING);
        if (pending != 0 && n - pending >= COMMIT_TICKS) {
            d.remove(K_PENDING);
            if (!TojiCombos.tryMatch(p)) {
                com.mojang.logging.LogUtils.getLogger().info("[甚尔] 序列 {} 没有匹配到连招",
                    Curse.data(p).getString(K_SEQ));
            }
        }
        if (n < d.getLong(K_WINDOW)) return;                       // 窗口未结束
        Entity threat = findThreat(p);
        if (threat != null) openWindow(p, threat, n);
    }

    /** 半径 2 格内的威胁：敌对生物（正在锁定玩家/怪物）、朝玩家飞的弹幕与箭。 */
    private static Entity findThreat(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return null;
        double best = Double.MAX_VALUE;
        Entity found = null;
        for (Entity entity : level.getEntitiesOfClass(Entity.class,
                new AABB(p.position(), p.position()).inflate(THREAT_RADIUS),
                t -> t != p && t.isAlive() && !(t instanceof ItemEntity))) {
            if (entity instanceof Projectile projectile) {
                Vec3 to = p.getEyePosition().subtract(projectile.position());
                if (to.lengthSqr() < 0.0001 || projectile.getDeltaMovement().dot(to.normalize()) <= 0.25) continue;
                double distance = projectile.distanceToSqr(p);
                if (distance < best) { best = distance; found = projectile; }
                continue;
            }
            if (!(entity instanceof LivingEntity living)) continue;
            boolean hostile = living instanceof Monster
                || (living instanceof Mob mob && mob.getTarget() == p)
                || living.getLastHurtByMob() == p;
            if (!hostile || !CombatSettings.canHit(p, living)) continue;
            double distance = living.distanceToSqr(p);
            if (distance < best) { best = distance; found = living; }
        }
        return found;
    }

    private static void openWindow(ServerPlayer p, Entity threat, long n) {
        CompoundTag d = Curse.data(p);
        d.putLong(K_WINDOW, n + WINDOW_TICKS);
        d.putInt(K_TARGET, threat.getId());
        ServerLevel level = p.serverLevel();
        // 只给本人发红色边缘光；标记广播给同维度所有人（别人也能看到"这东西被红圈锁住了"）
        Network.sendTo(p, new Network.Effect(TojiFx.QTE_ON, p.getId(), p.position(), Vec3.ZERO,
            threat.getId(), WINDOW_TICKS, 0));
        Network.send(level, new Network.Effect(TojiFx.MARK, threat.getId(), threat.position(), Vec3.ZERO,
            1f, WINDOW_TICKS, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.7f, 1.9f);
    }

    // ── 输入分派（客户端在窗口内接管后发来） ──────────────
    public static void input(ServerPlayer p, int token) {
        if (!CharacterService.toji(p) || !p.isAlive() || p.isSpectator()) return;
        long n = now(p);
        CompoundTag d = Curse.data(p);
        switch (token) {
            case DODGE -> {
                if (n < d.getLong(K_WINDOW)) dodge(p, n, false);
            }
            case BACKSTEP -> {
                if (n < d.getLong(K_WINDOW)) dodge(p, n, true);
            }
            case SLIDE -> {
                if (n < d.getLong(K_WINDOW)) slide(p, n);
            }
            case AIR_DODGE -> {
                if (n < d.getLong(K_WINDOW) && !p.onGround()) airDodge(p, n);
            }
            case JUMP -> airJump(p, n);
            case WEAPON_YUYUN -> equipWeapon(p, true);
            case WEAPON_TENKA -> equipWeapon(p, false);
            default -> { }
        }
        com.mojang.logging.LogUtils.getLogger().info("[甚尔] 输入 token={} 窗口={} 连招窗口={}",
            token, n < d.getLong(K_WINDOW), n < d.getLong(K_COMBO));
        if (token != JUMP) push(p, token, n);
    }

    /** 完美闪避：躲开 5 格（背对威胁方向）、无敌帧、速度 ×2、下一击翻倍。 */
    private static void dodge(ServerPlayer p, long n, boolean backstep) {
        CompoundTag d = Curse.data(p);
        Entity threat = p.serverLevel().getEntity(d.getInt(K_TARGET));
        Vec3 away = threat != null
            ? p.position().subtract(threat.position()).multiply(1, 0, 1).normalize()
            : p.getLookAngle().multiply(-1, 0, 1).normalize();
        if (away.lengthSqr() < 0.01) away = new Vec3(0, 0, 1);
        double distance = backstep ? BACKSTEP_DISTANCE : DODGE_DISTANCE;
        teleportAlong(p, away, distance);
        d.putLong(K_WINDOW, 0);
        // 保留 K_TARGET：闪身 5 格后目标可能在 4 格外，连招要靠这条记忆追回去
        if (backstep) {
            d.putLong(K_IFRAME, n + IFRAMES);
            p.displayClientMessage(Component.literal("后撤步").withStyle(net.minecraft.ChatFormatting.GRAY), true);
        } else {
            d.putLong(K_IFRAME, n + IFRAMES);
            d.putLong(K_PERFECT, n + PERFECT_TICKS);
            d.putLong(K_NEXT, n + PERFECT_TICKS);
            d.putDouble(K_MULT, NEXT_HIT_MULTIPLIER);
            d.putInt(K_COUNT, d.getInt(K_COUNT) + 1);
            applySpeed(p, n);
            ServerLevel level = p.serverLevel();
            Network.send(level, new Network.Effect(TojiFx.DODGE, p.getId(), p.position(), away, 1f, 10, 0));
            level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.7f);
            p.displayClientMessage(Component.literal("完美闪避 · 下一击 ×2").withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        d.putLong(K_COMBO, n + COMBO_TICKS);
        extendComboWindow(p);
    }

    private static void slide(ServerPlayer p, long n) {
        CompoundTag d = Curse.data(p);
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
        teleportAlong(p, dir, 3.0);
        d.putLong(K_SLIDE, n + 10);          // 0.5 秒无敌帧
        d.putLong(K_WINDOW, 0);
        d.putLong(K_COMBO, n + COMBO_TICKS);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.7f);
        extendComboWindow(p);
    }

    private static void airDodge(ServerPlayer p, long n) {
        CompoundTag d = Curse.data(p);
        Vec3 look = p.getLookAngle();
        p.setDeltaMovement(look.scale(1.4).add(0, 0.25, 0));
        p.hurtMarked = true;
        p.fallDistance = 0;
        d.putLong(K_IFRAME, n + IFRAMES);
        d.putLong(K_WINDOW, 0);
        d.putLong(K_COMBO, n + COMBO_TICKS);
        d.putBoolean("TojiAirDodged", true);
        extendComboWindow(p);
        p.displayClientMessage(Component.literal("空中闪避").withStyle(net.minecraft.ChatFormatting.RED), true);
    }

    /** 连招窗口：只给本人发一条 size=-1 的窗口包（客户端据此继续接管按键 1.5 秒）。 */
    private static void extendComboWindow(ServerPlayer p) {
        Network.sendTo(p, new Network.Effect(TojiFx.QTE_ON, p.getId(), p.position(), Vec3.ZERO,
            -1f, COMBO_TICKS, 0));
    }

    private static void airJump(ServerPlayer p, long n) {
        CompoundTag d = Curse.data(p);
        if (p.onGround() || !d.getBoolean("TojiAirDodged")) return;
        d.putBoolean("TojiAirDodged", false);
        p.setDeltaMovement(p.getDeltaMovement().x, 0.62, p.getDeltaMovement().z);
        p.hurtMarked = true;
        d.putLong(K_COMBO, n + COMBO_TICKS);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    /** 沿 direction 方向平移 distance 格，逐步试探避免卡墙。 */
    public static void teleportAlong(ServerPlayer p, Vec3 direction, double distance) {
        Vec3 start = p.position(), destination = start;
        Vec3 step = direction.normalize().scale(0.25);
        int steps = (int) Math.ceil(distance / 0.25);
        for (int i = 0; i < steps; i++) {
            Vec3 next = destination.add(step);
            var box = p.getBoundingBox().move(next.subtract(start));
            if (!p.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(next))
                || !p.level().getWorldBorder().isWithinBounds(box)
                || !p.level().noCollision(p, box)) break;
            destination = next;
        }
        p.teleportTo(destination.x, destination.y, destination.z);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
        p.hurtMarked = true;
    }

    private static void applySpeed(ServerPlayer p, long n) {
        var attribute = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;
        var id = java.util.UUID.fromString("480bb88f-333a-45aa-8bbb-001100220010");
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, "天与暴君·闪身",
            attribute.getBaseValue(), AttributeModifier.Operation.MULTIPLY_TOTAL));   // 等于当前速度 ×2
        Curse.data(p).putLong("TojiSpeedClear", n + PERFECT_TICKS);
    }

    /** 每刻清掉到期的速度加倍（由 TojiCombat.tick 调用）。 */
    public static void clearExpired(ServerPlayer p, long n) {
        var d = Curse.data(p);
        if (d.getLong("TojiSpeedClear") == 0 || n < d.getLong("TojiSpeedClear")) return;
        d.remove("TojiSpeedClear");
        var attribute = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) attribute.removeModifier(
            java.util.UUID.fromString("480bb88f-333a-45aa-8bbb-001100220010"));
    }

    private static void equipWeapon(ServerPlayer p, boolean yuyun) {
        // 连招里"切武器"只记录 token（真正的物品由玩家自己拿），这里给动作与提示
        p.displayClientMessage(Component.literal(yuyun ? "游云" : "天逆鉾")
            .withStyle(yuyun ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.AQUA), true);
    }

    // ── 输入缓冲 ──────────────────────────────────────────
    private static void push(ServerPlayer p, int token, long n) {
        CompoundTag d = Curse.data(p);
        long at = d.getLong(K_SEQ_AT);
        String seq = d.getString(K_SEQ);
        if (n - at > COMBO_TICKS || seq.isEmpty()) seq = "";
        d.putString(K_SEQ, seq.isEmpty() ? Integer.toString(token) : seq + "," + token);
        d.putLong(K_SEQ_AT, n);
        d.putLong(K_PENDING, n);
    }

    public static List<Integer> sequence(ServerPlayer p) {
        String raw = Curse.data(p).getString(K_SEQ);
        List<Integer> list = new ArrayList<>();
        if (raw.isEmpty()) return list;
        for (String part : raw.split(",")) {
            try { list.add(Integer.parseInt(part)); } catch (NumberFormatException ignored) { }
        }
        return list;
    }

    public static void resetSequence(ServerPlayer p) {
        Curse.data(p).remove(K_SEQ);
        Curse.data(p).remove(K_PENDING);
    }

    public static int perfectCount(Player p) { return Curse.data(p).getInt(K_COUNT); }

    /** 取这次攻击的翻倍倍率（用后清零）。 */
    public static double consumeNextHit(ServerPlayer p) {
        long n = now(p);
        CompoundTag d = Curse.data(p);
        if (n >= d.getLong(K_NEXT)) return 1;
        double mult = d.getDouble(K_MULT);
        d.remove(K_NEXT);
        d.remove(K_MULT);
        return mult <= 0 ? 1 : mult;
    }

    /** QTE 记住的目标（闪避前锁定那一个），12 格内仍有效。 */
    public static LivingEntity remembered(ServerPlayer p) {
        int id = Curse.data(p).getInt(K_TARGET);
        if (id == 0) return null;
        if (!(p.serverLevel().getEntity(id) instanceof LivingEntity living) || !living.isAlive()) return null;
        return living.distanceToSqr(p) <= 144 ? living : null;
    }

    /** 连招出手前把施术者拉到目标身边（闪身 5 格后够不着的问题）。 */
    public static LivingEntity approach(ServerPlayer p, double reach) {
        LivingEntity found = target(p, reach);
        if (found != null) return found;
        LivingEntity remembered = remembered(p);
        if (remembered == null) return null;
        Vec3 to = remembered.position().subtract(p.position()).multiply(1, 0, 1);
        double distance = to.length();
        if (distance > reach) {
            Vec3 step = to.normalize().scale(Math.max(0, distance - reach * 0.6));
            teleportAlong(p, step, step.length());
        }
        return remembered;
    }

    /** 连招要打的目标：先看准星前方 4 格，其次用 QTE 标记过的目标。 */
    public static LivingEntity target(ServerPlayer p, double reach) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null;
        double bestScore = -1;
        for (LivingEntity living : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(p.position(), p.position()).inflate(reach + 2),
                t -> t != p && t.isAlive() && !t.isSpectator() && CombatSettings.canHit(p, t))) {
            Vec3 delta = living.getBoundingBox().getCenter().subtract(eye);
            double along = delta.dot(look);
            if (along < 0 || along > reach + 2) continue;
            double score = along - delta.cross(look).length() * 0.5;
            if (score > bestScore) { bestScore = score; best = living; }
        }
        return best;
    }

    /** 眩晕/硬直：直接锁 AI。 */
    public static void stun(LivingEntity target, int ticks) {
        data(target).putLong(K_STUN, now(target) + ticks);
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
            STUNNED.removeIf(stunned -> stunned.target() == mob);
            STUNNED.add(new Stunned(mob, now(target) + ticks));
        }
    }

    /** 服务端每刻：解除到期的眩晕。 */
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || STUNNED.isEmpty()) return;
        STUNNED.removeIf(stunned -> {
            LivingEntity target = stunned.target();
            if (target == null || !target.isAlive() || now(target) >= stunned.until()) {
                if (target instanceof Mob mob && target.isAlive()) mob.setNoAi(false);
                return true;
            }
            return false;
        });
    }

    /** 闪避无敌帧 + 夺命挡箭牌：伤害转移给替死鬼。 */
    @SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !CharacterService.toji(p)) return;
        if (invulnerable(p)) { e.setCanceled(true); return; }
        int hostage = Curse.data(p).getInt("TojiHostage");
        if (hostage != -1 && e.getAmount() > 0 && p.serverLevel().getEntity(hostage) instanceof LivingEntity shield
            && shield.isAlive()) {
            Curse.data(p).putInt("TojiHostage", -1);
            CombatSettings.hurt(p, shield, e.getSource(), e.getAmount());
            e.setCanceled(true);
            Network.send(p.serverLevel(), new Network.Effect(TojiFx.BREAK, shield.getId(), shield.position(),
                Vec3.ZERO, 1f, 10, 0));
        }
    }

    /** 尘土/冲击波小特效。 */
    public static void dust(ServerLevel level, Vec3 at, double radius) {
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.2, at.z, 18, radius * 0.6, 0.1, radius * 0.6, 0.05);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y + 0.8, at.z, 24, radius * 0.5, 0.4, radius * 0.5, 0.2);
    }

    private TojiQte() {}
}
