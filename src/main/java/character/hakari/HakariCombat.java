package character.hakari;

import cn.blockforge.generated.nnnnr1201forge2.*;
import character.CharacterService;
import character.CharacterSkill;
import java.util.List;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 秤金次 · 坐杀博徒（r371 大改，用户逐条规格）。
 *
 * <p>**抽奖**：攻击命中一次拨动老虎机一格（粗粝打击被动），幸运硬币期间翻倍；
 * 每满 10 格掷一次骰子（1/8 中奖），满 **30 格必定中奖**。
 *
 * <p>**中奖 4 分 11 秒（5020 刻）**：无限咒力、每秒回 20% 最大生命、平A与技能冷却 -90%、
 * 领域冷却清零、伤害 ×2、逆转反击（致死保护，受致命伤反而 +3 格并回血）。
 */
public final class HakariCombat {
    public static final int JACKPOT_TICKS = 5020;            // 4 分 11 秒
    public static final int LUCK_MAX = 30;                   // 打满 30 次必中
    public static final int ROLL_EVERY = 10;                 // 每 10 格掷一次
    public static final double ROLL_CHANCE = 1.0 / 8.0;      // 单次掷骰中奖率
    public static final double HEAL_PER_SECOND = 0.20;       // 中奖每秒回 20% 最大生命
    public static final double JACKPOT_DAMAGE = 2.0;         // 中奖伤害 ×2
    public static final double JACKPOT_COOLDOWN = 0.10;      // 冷却 ×0.1（-90%）
    public static final int COIN_TICKS = 100;                // 幸运硬币 5 秒
    public static final int GUARD_TICKS = 60;                // 车门格挡 3 秒
    public static final int BLEED_TICKS = 40;                // 撕裂（流血）

    static final String K_LUCK = "HakariLuck", K_ROLLED = "HakariRolled", K_JACKPOT = "HakariJackpotUntil",
        K_TRAIN_USED = "HakariTrainUsed", K_COIN = "HakariCoinUntil", K_GUARD = "HakariGuardUntil",
        K_CHEAT = "HakariCheatUntil", K_GLOW = "HakariGlowTarget", K_SPEED = "HakariSpeedUntil";

    public static long now(ServerPlayer p) { return p.level().getGameTime(); }

    public static boolean jackpot(ServerPlayer p) {
        return p != null && Curse.data(p).getLong(K_JACKPOT) > now(p);
    }

    private static int luck(ServerPlayer p) { return Curse.data(p).getInt(K_LUCK); }

    // ── 抽奖 ────────────────────────────────────────────
    /** 拨动老虎机 amount 格（幸运硬币期间翻倍），满 10 格掷骰、满 30 格必中。 */
    public static void roll(ServerPlayer p, int amount) {
        if (!CharacterService.hakari(p) || amount <= 0 || jackpot(p)) return;
        CompoundTag data = Curse.data(p);
        long n = now(p);
        boolean coin = data.getLong(K_COIN) > n;
        int gain = amount * (coin ? 2 : 1);
        int value = Math.min(LUCK_MAX, data.getInt(K_LUCK) + gain);
        data.putInt(K_LUCK, value);
        int rolled = data.getInt(K_ROLLED);
        boolean win = value >= LUCK_MAX;
        while (!win && value - rolled >= ROLL_EVERY) {
            rolled += ROLL_EVERY;
            if (p.level().random.nextDouble() < ROLL_CHANCE) win = true;
        }
        data.putInt(K_ROLLED, rolled);
        broadcastSlot(p, value);
        if (win) startJackpot(p);
    }

    /** 把"当前幸运值/滚动"发给领域内所有人（敌方屏幕也会出现老虎机）。 */
    private static void broadcastSlot(ServerPlayer p, int value) {
        if (!(p.level() instanceof ServerLevel level)) return;
        Network.send(level, new Network.Effect(HakariFx.SLOT, p.getId(), p.position(), Vec3.ZERO, value, 30, 0));
    }

    public static void startJackpot(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        long n = now(p);
        data.putLong(K_JACKPOT, n + JACKPOT_TICKS);
        data.putInt(K_LUCK, 0);
        data.putInt(K_ROLLED, 0);
        data.putBoolean(K_TRAIN_USED, false);
        data.remove("CharacterSkillCooldown_" + HakariSkills.DOMAIN);   // 术式熔断解除
        ServerLevel level = p.serverLevel();
        Network.send(level, new Network.Effect(HakariFx.JACKPOT, p.getId(), p.position(), Vec3.ZERO,
            1f, JACKPOT_TICKS, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2f, 1.0f);
        p.displayClientMessage(Component.literal("中奖！坐杀博徒 · 4 分 11 秒").withStyle(ChatFormatting.GOLD), false);
        p.sendSystemMessage(Component.literal("老虎机摇出三个一样的图案：无限咒力、每秒回 20% 生命、冷却 -90%")
            .withStyle(ChatFormatting.GOLD));
        heal(p, 1.0);
    }

    public static void endJackpot(ServerPlayer p) {
        Curse.data(p).remove(K_JACKPOT);
        Network.sendTo(p, new Network.Effect(HakariFx.JACKPOT_END, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
        p.displayClientMessage(Component.literal("中奖结束 · 再来一局").withStyle(ChatFormatting.GRAY), false);
    }

    private static void heal(ServerPlayer p, double fraction) {
        p.setHealth((float) Math.min(p.getMaxHealth(), p.getHealth() + p.getMaxHealth() * fraction));
    }

    // ── 每刻维护 ────────────────────────────────────────
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!CharacterService.hakari(p)) return;
        CompoundTag data = Curse.data(p);
        long n = now(p);
        long until = data.getLong(K_JACKPOT);
        if (until != 0) {
            if (n >= until) endJackpot(p);
            else {
                Curse.addRaw(p, 4000);                               // 无限咒力
                if (n % 20 == 0) heal(p, HEAL_PER_SECOND);           // 自动反转术式
                if (n % 20 == 0) Network.sendTo(p, new Network.Effect(HakariFx.JACKPOT, p.getId(), p.position(),
                    Vec3.ZERO, (float) (until - n) / JACKPOT_TICKS, Math.max(1, (int) (until - n)), 0));
            }
        } else if (n % 20 == 0) {
            // 赌徒的嗅觉：未中奖时移速 +20%，并高亮 20 格内血量最低的敌人
            speed(p, n);
            highlight(p);
        }
        if (data.getLong(K_SPEED) != 0 && n >= data.getLong(K_SPEED)) clearSpeed(p);
    }

    private static void speed(ServerPlayer p, long n) {
        var attribute = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;
        var id = java.util.UUID.fromString("7a1c5f10-1111-4a2b-9c00-0000aaaabbbb");
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, "赌徒的嗅觉",
            attribute.getBaseValue() * 0.2, AttributeModifier.Operation.ADDITION));
    }

    private static void clearSpeed(ServerPlayer p) {
        var attribute = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) attribute.removeModifier(
            java.util.UUID.fromString("7a1c5f10-1111-4a2b-9c00-0000aaaabbbb"));
    }

    private static void highlight(ServerPlayer p) {
        LivingEntity best = null;
        double health = Double.MAX_VALUE;
        for (LivingEntity living : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(p.position(), p.position()).inflate(20),
                t -> t != p && t.isAlive() && CombatSettings.canHit(p, t))) {
            if (living.getHealth() < health) { health = living.getHealth(); best = living; }
        }
        CompoundTag data = Curse.data(p);
        int previous = data.getInt(K_GLOW);
        if (previous != 0 && (best == null || previous != best.getId())
            && p.serverLevel().getEntity(previous) instanceof LivingEntity old) old.setGlowingTag(false);
        if (best != null) { best.setGlowingTag(true); data.putInt(K_GLOW, best.getId()); }
    }

    // ── 受击/命中钩子 ────────────────────────────────────
    @SubscribeEvent public static void hurt(LivingHurtEvent e) {
        // ① 粗粝打击：秤金次的每次命中都撕裂目标并拨动老虎机
        if (e.getSource().getEntity() instanceof ServerPlayer p && CharacterService.hakari(p)
            && e.getSource().getDirectEntity() == p && e.getEntity() != p) {
            if (e.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) e.getEntity();
                CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), e.getAmount() * 0.5f);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, BLEED_TICKS, 0, false, false));
                roll(p, 1);
            }
            return;
        }
        if (!(e.getEntity() instanceof ServerPlayer p) || !CharacterService.hakari(p)) return;
        CompoundTag data = Curse.data(p);
        long n = now(p);
        // ② 车门格挡：正面来袭免伤 + 拨动 + 敌人僵直 + 反手重拳
        if (data.getLong(K_GUARD) > n && e.getSource().getEntity() instanceof LivingEntity attacker
            && frontal(p, attacker)) {
            e.setCanceled(true);
            roll(p, 2);
            if (attacker instanceof Mob mob) NaoyaStun.stun(mob, 30);
            CombatSettings.hurt(p, attacker, p.damageSources().playerAttack(p), 14);
            Network.send(p.serverLevel(), new Network.Effect(HakariFx.BLOCK, p.getId(), p.position(),
                p.getLookAngle(), 1f, 10, 0));
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1.2f);
            return;
        }
        // ③ 逆转反击：中奖期间致命伤不死，反而 +3 格并回满
        if (jackpot(p) && e.getAmount() >= p.getHealth()) {
            e.setCanceled(true);
            heal(p, 1.0);
            roll(p, 3);
            Network.send(p.serverLevel(), new Network.Effect(HakariFx.JACKPOT, p.getId(), p.position(),
                Vec3.ZERO, 0.4f, 20, 0));
            p.displayClientMessage(Component.literal("逆转反击！").withStyle(ChatFormatting.GREEN), false);
        }
    }

    private static boolean frontal(ServerPlayer p, LivingEntity attacker) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 to = attacker.position().subtract(p.position()).multiply(1, 0, 1).normalize();
        return look.dot(to) > 0.25;
    }

    /** 极简眩晕（只锁 AI，30 刻后由服务端恢复）。 */
    private static final class NaoyaStun {
        static void stun(Mob mob, int ticks) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
            STUNNED.add(new Object[]{mob, mob.level().getGameTime() + ticks});
        }
    }
    private static final List<Object[]> STUNNED = new java.util.ArrayList<>();

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || STUNNED.isEmpty()) return;
        STUNNED.removeIf(pair -> {
            Mob mob = (Mob) pair[0];
            long until = (Long) pair[1];
            if (mob == null || !mob.isAlive() || mob.level().getGameTime() >= until) {
                if (mob != null && mob.isAlive()) mob.setNoAi(false);
                return true;
            }
            return false;
        });
    }

    // ── 技能 ────────────────────────────────────────────
    public static void cast(ServerPlayer p, CharacterSkill skill) {
        if (!CharacterService.hakari(p) || !p.isAlive()) return;
        switch (skill.id()) {
            case HakariSkills.DOOR_SMASH -> doorSmash(p, skill);
            case HakariSkills.DOOR_GUARD -> doorGuard(p);
            case HakariSkills.TRAIN_RUSH -> trainRush(p, skill);
            case HakariSkills.STEEL_BALLS -> steelBalls(p, skill);
            case HakariSkills.LUCKY_COIN -> luckyCoin(p);
            case HakariSkills.CHEAT -> cheat(p);
            case HakariSkills.DOMAIN -> HakariDomain.toggle(p);
            case HakariSkills.LOVE_TRAIN -> loveTrain(p, skill);
            case HakariSkills.ENDLESS -> p.displayClientMessage(
                Component.literal(jackpot(p) ? "无限咒力平推：生效中" : "中奖后才生效").withStyle(ChatFormatting.GOLD), true);
            case HakariSkills.REVERSAL -> p.displayClientMessage(
                Component.literal(jackpot(p) ? "逆转反击：待机中" : "中奖后才生效").withStyle(ChatFormatting.GREEN), true);
            default -> { }
        }
    }

    private static void doorSmash(ServerPlayer p, CharacterSkill skill) {
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        Vec3 center = eye.add(dir.scale(skill.range() * 0.6));
        for (LivingEntity target : around(p, center, skill.radius() + 1.5)) {
            CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), damage(p, skill.damage()));
            Vec3 push = target.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(2.2);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.55, push.z));
            target.hurtMarked = true;
        }
        Network.send(p.serverLevel(), new Network.Effect(HakariFx.DOOR, p.getId(), center, dir, 2.4f, 12, 0));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1f, 0.6f);
        roll(p, 2);
    }

    private static void doorGuard(ServerPlayer p) {
        Curse.data(p).putLong(K_GUARD, now(p) + GUARD_TICKS);
        Network.send(p.serverLevel(), new Network.Effect(HakariFx.BLOCK, p.getId(), p.position(),
            p.getLookAngle(), 1.6f, GUARD_TICKS, 0));
        p.displayClientMessage(Component.literal("车门格挡 · 3 秒").withStyle(ChatFormatting.GRAY), true);
    }

    private static void trainRush(ServerPlayer p, CharacterSkill skill) {
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 at = p.position();
        for (int step = 0; step < 56; step++) {                       // 14 格，每 0.25 格
            Vec3 next = at.add(dir.scale(0.25));
            if (!p.level().noCollision(p, p.getBoundingBox().move(next.subtract(p.position())))) break;
            at = next;
        }
        p.teleportTo(at.x, at.y, at.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        for (LivingEntity target : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(p.position().add(dir.scale(-1.5)), p.position().add(dir.scale(15))).inflate(3),
                t -> t != p && t.isAlive() && CombatSettings.canHit(p, t))) {
            CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), damage(p, skill.damage()));
            Vec3 push = dir.scale(2.4);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.5, push.z));
            target.hurtMarked = true;
        }
        Network.send(p.serverLevel(), new Network.Effect(HakariFx.TRAIN, p.getId(), p.position(), dir, 14f, 14, 0));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.MINECART_INSIDE, SoundSource.PLAYERS, 1.5f, 0.7f);
        roll(p, 2);
    }

    private static void steelBalls(ServerPlayer p, CharacterSkill skill) {
        Vec3 eye = p.getEyePosition(), dir = p.getLookAngle();
        for (LivingEntity target : around(p, eye.add(dir.scale(skill.range() * 0.5)), skill.range() * 0.75)) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > skill.range() + 2) continue;
            if (to.normalize().dot(dir) < Math.cos(Math.toRadians(60))) continue;
            CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), damage(p, skill.damage() * 0.6));
            if (p.level().random.nextDouble() < 0.5) roll(p, 1);
        }
        Network.send(p.serverLevel(), new Network.Effect(HakariFx.SPARK, p.getId(), eye, dir, 1.6f, 12, 0));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    private static void luckyCoin(ServerPlayer p) {
        Curse.data(p).putLong(K_COIN, now(p) + COIN_TICKS);
        Network.send(p.serverLevel(), new Network.Effect(HakariFx.SLOT, p.getId(), p.position(), Vec3.ZERO, -1f, COIN_TICKS, 0));
        p.displayClientMessage(Component.literal("幸运硬币 · 5 秒内拨动翻倍").withStyle(ChatFormatting.GOLD), true);
    }

    private static void cheat(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        data.putInt(K_LUCK, 0);
        data.putInt(K_ROLLED, 0);
        data.putLong(K_SPEED, now(p) + 100);
        Network.send(p.serverLevel(), new Network.Effect(HakariFx.SLOT, p.getId(), p.position(), Vec3.ZERO, 0f, 20, 0));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1f, 0.6f);
        p.displayClientMessage(Component.literal("出老千：计数清空，移速提升").withStyle(ChatFormatting.RED), true);
    }

    private static void loveTrain(ServerPlayer p, CharacterSkill skill) {
        CompoundTag data = Curse.data(p);
        if (!jackpot(p)) { p.displayClientMessage(Component.literal("只能在中奖的 4 分 11 秒内使用").withStyle(ChatFormatting.GRAY), true); return; }
        data.putBoolean(K_TRAIN_USED, true);   // 只做记录，不再限制次数：中奖期间可以无限开
        Vec3 from = p.position(), dir = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 to = from.add(dir.scale(skill.range()));
        for (LivingEntity target : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(from, to).inflate(4), t -> t != p && t.isAlive() && CombatSettings.canHit(p, t))) {
            CombatSettings.hurt(p, target, p.damageSources().playerAttack(p), damage(p, skill.damage()));
            Vec3 push = dir.scale(3.2);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.9, push.z));
            target.hurtMarked = true;
        }
        SkillDestruction.immediate(p, from, to, 3);
        Network.send(p.serverLevel(), new Network.Effect(HakariFx.TRAIN, p.getId(), from, dir, (float) skill.range(), 30, 0));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2f, 0.7f);
        p.displayClientMessage(Component.literal("纯爱列车 · 全速前进").withStyle(ChatFormatting.GREEN), true);
    }

    // ── 工具 ────────────────────────────────────────────
    private static List<LivingEntity> around(ServerPlayer p, Vec3 center, double radius) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
            t -> t != p && t.isAlive() && !t.isSpectator() && CombatSettings.canHit(p, t));
    }

    private static float damage(ServerPlayer p, double raw) {
        return (float) (raw * (jackpot(p) ? JACKPOT_DAMAGE : 1.0));
    }

    /** 中奖期间冷却 ×0.1（注册给 CharacterCasting）。 */
    public static double cooldownScale(ServerPlayer p) {
        return jackpot(p) ? JACKPOT_COOLDOWN : 1.0;
    }

    public static void testReset(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        data.remove(K_LUCK);
        data.remove(K_ROLLED);
        data.remove(K_JACKPOT);
        data.remove(K_TRAIN_USED);
        data.remove(K_COIN);
        data.remove(K_GUARD);
        data.remove(K_CHEAT);
        data.remove(K_SPEED);
        data.remove(K_GLOW);
        clearSpeed(p);
        Network.sendTo(p, new Network.Effect(HakariFx.JACKPOT_END, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
    }

    private HakariCombat() {}
}
