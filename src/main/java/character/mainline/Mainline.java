package character.mainline;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 共同主线（r381）：所有角色共享的成就线，在 L 键成就面板里以"成就"形式展示。
 *
 * <p>反转术式分支：咒术师 → 反转术式（血量 5% 以下 50% 领悟）→ 反转术式外放（友方 15% 以下 50% 领悟）。
 * <p>结界术分支：结界术（在敌方领域里存活 15 秒）→ 简易领域（击败 1 个有领域的敌人）→
 * 领域展开（累计用简易领域 15 次）→ 领域展延（累计用领域展开 10 次）。
 */
public final class Mainline {
    // ── 成就 id ──
    public static final int SORCERER = 0;          // 咒术师：使用术式选择书
    public static final int REVERSE = 1;           // 反转术式
    public static final int REVERSE_OUT = 2;       // 反转术式外放
    public static final int BARRIER = 3;           // 结界术
    public static final int SIMPLE = 4;            // 简易领域
    public static final int DOMAIN = 5;            // 领域展开
    public static final int AMPLIFY = 6;           // 领域展延

    public static final String[] NAMES = {"咒术师", "反转术式", "反转术式外放", "结界术", "简易领域",
        "领域展开", "领域展延"};
    public static final String[] REQUIREMENTS = {
        "使用一次《咒术选择书》",
        "血量降到 5% 以下时，50% 概率领悟",
        "附近友方血量降到 15% 以下时，50% 概率领悟",
        "在任意敌方的领域内累计存活 15 秒",
        "击败 1 个拥有领域的敌人",
        "累计使用简易领域 15 次",
        "累计使用领域展开 10 次"};

    private static final String K_PREFIX = "MainlineUnlocked", C_PREFIX = "MainlineCount";
    private static final int SURVIVE_TICKS = 300;      // 15 秒
    private static final int SIMPLE_NEED = 15, DOMAIN_NEED = 10;
    private static final double REVERSE_CHANCE = 0.5, OUT_CHANCE = 0.5;

    // ── 简易领域 / 结界术 的运行时状态 ──
    public static final int SIMPLE_RADIUS = 10, SIMPLE_TICKS = 200;
    public static final int BARRIER_RADIUS = 15, BARRIER_TICKS = 200;
    private static final String K_SIMPLE_UNTIL = "MainlineSimpleUntil", K_BARRIER_UNTIL = "MainlineBarrierUntil";
    private static long surviveTicks;                   // 当前连续待在敌方领域的刻数（全局，单服务器足够）

    public static boolean unlocked(Player p, int id) {
        return id >= 0 && id < NAMES.length && Curse.data(p).getBoolean(K_PREFIX + id);
    }

    public static int count(Player p, int id) { return Curse.data(p).getInt(C_PREFIX + id); }

    public static void unlock(ServerPlayer p, int id) {
        if (id < 0 || id >= NAMES.length || unlocked(p, id)) return;
        Curse.data(p).putBoolean(K_PREFIX + id, true);
        Network.sendTo(p, new Network.Effect(character.ryu.RyuFx.DOMAIN, p.getId(), p.position(),
            Vec3.ZERO, 0f, 0, 0));
        p.sendSystemMessage(Component.literal("【成就解锁】" + NAMES[id] + " —— " + REQUIREMENTS[id])
            .withStyle(ChatFormatting.GOLD));
        p.displayClientMessage(Component.literal("成就解锁：" + NAMES[id]).withStyle(ChatFormatting.GOLD), true);
        if (id == DOMAIN) unlockAllDomains(p);
    }

    private static void addCount(ServerPlayer p, int id, int amount) {
        Curse.data(p).putInt(C_PREFIX + id, count(p, id) + amount);
    }

    /** 咒术师：第一次选角色（吃下咒术选择书）时解锁。 */
    public static void onCharacterChosen(ServerPlayer p) {
        unlock(p, SORCERER);
    }

    /** 领域展开：解锁各角色轮盘里自己的领域技能（这类技能都在角色技能表里，标记位用于放行）。 */
    private static void unlockAllDomains(ServerPlayer p) {
        Curse.data(p).putBoolean("MainlineDomainUnlocked", true);
        p.sendSystemMessage(Component.literal("所有术式的领域展开已解锁（轮盘里的领域格可用）")
            .withStyle(ChatFormatting.AQUA));
    }

    public static boolean domainUnlocked(Player p) { return unlocked(p, DOMAIN); }

    // ── 成就条件：受击时判定反转术式分支 ──
    @SubscribeEvent public static void hurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            if (!unlocked(p, REVERSE) && p.getHealth() - e.getAmount() <= p.getMaxHealth() * 0.05f
                && p.level().random.nextDouble() < REVERSE_CHANCE) unlock(p, REVERSE);
            return;
        }
        // 反转术式外放：附近友方（其他玩家/自己的召唤物）血量掉到 15% 以下
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) e.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.getPlayers(player -> player.isAlive()
            && player.distanceToSqr(victim) <= 12 * 12)) {
            if (p == victim || unlocked(p, REVERSE_OUT) || !unlocked(p, REVERSE)) continue;
            if (victim.getHealth() - e.getAmount() > victim.getMaxHealth() * 0.15f) continue;
            if (p.level().random.nextDouble() < OUT_CHANCE) unlock(p, REVERSE_OUT);
        }
    }

    // ── 每刻：结界术（在敌方领域里存活 15 秒）与结界持续时间 ──
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        CompoundTag data = Curse.data(p);
        long now = p.level().getGameTime();
        // 咒术师：只要已经选了角色（吃了咒术选择书）就解锁
        if(!unlocked(p,SORCERER)&&character.CharacterService.active(p))unlock(p,SORCERER);
        // 敌方的领域：用现有的领域状态判断（自己开的除外）
        boolean insideEnemy = false;
        for (ServerPlayer other : p.serverLevel().getPlayers(o -> o != p && o.isAlive())) {
            if (character.CharacterService.hakari(other)) continue;
            if (character.ryu.RyuDomain.active(other) || character.naoya.NaoyaDomain.active(other)
                || character.yuta.YutaDomain.active(other) || character.yuki.YukiDomain.active(other)
                || character.higuruma.HigurumaDomain.active(other)
                || character.domainking.SelfEmbryoDomain.active(other)) {
                if (p.distanceToSqr(other) <= 30 * 30) { insideEnemy = true; break; }
            }
        }
        if (insideEnemy && !unlocked(p, BARRIER)) {
            if (++surviveTicks >= SURVIVE_TICKS) unlock(p, BARRIER);
        } else surviveTicks = 0;
        // 结界术球壳结束
        long barrier = data.getLong(K_BARRIER_UNTIL);
        if (barrier != 0 && now >= barrier) {
            data.remove(K_BARRIER_UNTIL);
            Network.sendTo(p, new Network.Effect(character.ryu.RyuFx.DOMAIN, p.getId(), p.position(), Vec3.ZERO, 0f, 0, 0));
        }
    }

    // ── 术式 ──
    /** 反转术式：治疗自己。 */
    public static void reverse(ServerPlayer p) {
        if (!unlocked(p, REVERSE)) return;
        p.setHealth((float) Math.min(p.getMaxHealth(), p.getHealth() + p.getMaxHealth() * 0.4));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, false));
        Network.send(p.serverLevel(), new Network.Effect(character.ryu.RyuFx.BURST, p.getId(), p.position(),
            Vec3.ZERO, 1.5f, 20, 0));
        p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
            net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    /** 反转术式外放：治疗准星对准的友方。 */
    public static void reverseOut(ServerPlayer p) {
        if (!unlocked(p, REVERSE_OUT)) return;
        LivingEntity target = null;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double best = 0.9;
        for (LivingEntity living : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(p.position(), p.position()).inflate(16), t -> t != p && t.isAlive())) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > 16) continue;
            double dot = to.normalize().dot(look);
            if (dot > best) { best = dot; target = living; }
        }
        if (target == null) { p.displayClientMessage(Component.literal("准星没有对准目标").withStyle(ChatFormatting.GRAY), true); return; }
        target.setHealth((float) Math.min(target.getMaxHealth(), target.getHealth() + target.getMaxHealth() * 0.35));
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));
        Network.send(p.serverLevel(), new Network.Effect(character.ryu.RyuFx.BURST, target.getId(),
            target.position(), Vec3.ZERO, 1.2f, 18, 0));
    }

    /** 结界术：释放一个纯领域球壳（10 秒、半径 15）。 */
    public static void barrier(ServerPlayer p) {
        if (!unlocked(p, BARRIER)) return;
        long now = p.level().getGameTime();
        Curse.data(p).putLong(K_BARRIER_UNTIL, now + BARRIER_TICKS);
        Network.send(p.serverLevel(), new Network.Effect(character.ryu.RyuFx.DOMAIN, p.getId(), p.position(),
            Vec3.ZERO, BARRIER_RADIUS, BARRIER_TICKS, 0));
        p.displayClientMessage(Component.literal("结界术 · 半径 15 的球壳 10 秒").withStyle(ChatFormatting.AQUA), true);
    }

    /** 简易领域：脚下蓝色圆圈（半径 10），圈内免疫一切领域效果。 */
    public static void simple(ServerPlayer p) {
        if (!unlocked(p, SIMPLE)) return;
        Curse.data(p).putLong(K_SIMPLE_UNTIL, p.level().getGameTime() + SIMPLE_TICKS);
        addCount(p, SIMPLE, 1);
        if (count(p, SIMPLE) >= SIMPLE_NEED) unlock(p, DOMAIN);
        Network.send(p.serverLevel(), new Network.Effect(character.naoya.NaoyaFx.NODE, p.getId(), p.position(),
            Vec3.ZERO, SIMPLE_RADIUS, 20, 0));
        p.displayClientMessage(Component.literal("简易领域 · 半径 10 的蓝色圆（圈内免疫领域）")
            .withStyle(ChatFormatting.AQUA), true);
    }

    /** 玩家此刻是否被简易领域保护（圈内免疫领域效果）。 */
    public static boolean simpleProtected(Player p) {
        long until = Curse.data(p).getLong(K_SIMPLE_UNTIL);
        return until > p.level().getGameTime();
    }

    /** 领域展开计数（各角色领域成功展开时调用）。 */
    public static void onDomainOpened(ServerPlayer p) {
        if (!unlocked(p, DOMAIN)) return;
        addCount(p, DOMAIN, 1);
        if (count(p, DOMAIN) >= DOMAIN_NEED && !unlocked(p, AMPLIFY)) unlock(p, AMPLIFY);
    }

    /** 击败有领域的敌人 → 简易领域。 */
    public static void onKillDomainOwner(ServerPlayer p) { unlock(p, SIMPLE); }

    /** 成就面板数据（客户端只读）。 */
    public static Map<String, Object> snapshot(Player p) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < NAMES.length; i++) {
            map.put(NAMES[i], new Object[]{unlocked(p, i), i == SIMPLE ? count(p, SIMPLE) + "/" + SIMPLE_NEED
                : i == DOMAIN ? count(p, DOMAIN) + "/" + DOMAIN_NEED : ""});
        }
        return map;
    }

    /** 客户端用：已解锁位掩码 + 计数（走 Progress 包之外的简易同步）。 */
    public static int mask(Player p) {
        int mask = 0;
        for (int i = 0; i < NAMES.length; i++) if (unlocked(p, i)) mask |= 1 << i;
        return mask;
    }

    private Mainline() {}
}
