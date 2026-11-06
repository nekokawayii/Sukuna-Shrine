package character.culling;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 死灭洄游的「入场」逻辑（r250）：
 * ① 方式二·被迫卷入：主世界击杀敌对生物累计到阈值，夜晚被强制拉进结界，额外 50 积分；
 * ② 登记：第一次进结界时弹三选一（伤害 +20% / 开局 +50 积分 / 免分随机一条规则），选完才播报「已登记」。
 */
public final class CullingEntry {
    /** 被迫卷入需要的主世界击杀数。 */
    public static final int FORCED_KILLS = 660;
    /** 三选一带来的攻击加成（永久，固定 UUID）。 */
    private static final java.util.UUID ATK_UUID = java.util.UUID.fromString("7c1e0000-0000-4a00-9c00-00000000c001");

    /** 进结界后的登记流程（由 {@link CullingGame#enter} 调用）。 */
    public static void onEntered(ServerPlayer p) {
        CompoundTag d = ExpeditionProgress.data(p);
        if (d.getBoolean("CullingRegistered")) return;
        p.sendSystemMessage(Component.literal("死灭回游，已登记。请选择你的开局祝福。").withStyle(ChatFormatting.RED));
        Network.sendTo(p, new Network.Effect(622, p.getId(), p.position(), net.minecraft.world.phys.Vec3.ZERO,
            d.getInt("CullingPoints"), 3, 0));
    }

    /** 三选一：0 伤害 +20% / 1 +50 积分 / 2 免分随机一条规则。 */
    public static void pick(ServerPlayer p, int index) {
        CompoundTag d = ExpeditionProgress.data(p);
        if (d.getBoolean("CullingRegistered")) return;
        d.putBoolean("CullingRegistered", true);
        d.putInt("CullingStartBuff", index);
        switch (index) {
            case 0 -> {
                var atk = p.getAttribute(Attributes.ATTACK_DAMAGE);
                if (atk != null && atk.getModifier(ATK_UUID) == null)
                    atk.addPermanentModifier(new AttributeModifier(ATK_UUID, "死灭开局·暴力",
                        0.2, AttributeModifier.Operation.MULTIPLY_TOTAL));
                p.sendSystemMessage(Component.literal("【开局祝福】伤害 +20%。").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case 1 -> {
                d.putInt("CullingPoints", d.getInt("CullingPoints") + 50);
                p.sendSystemMessage(Component.literal("【开局祝福】积分 +50（现有 " + d.getInt("CullingPoints") + "）。")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            default -> {
                if (CullingRules.freeRule(p))
                    p.sendSystemMessage(Component.literal("【开局祝福】免分获得一条规则。").withStyle(ChatFormatting.LIGHT_PURPLE));
                else
                    p.sendSystemMessage(Component.literal("【开局祝福】卡池已空，改为 +50 积分。").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        p.sendSystemMessage(Component.literal("死灭回游，已登记。规则，已赋予。").withStyle(ChatFormatting.RED));
    }

    /** 方式二：主世界击杀敌对生物累计到阈值，夜里被强制拉入。 */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (p.level().dimension().equals(CullingGame.DIM)) return;
        if (!(e.getEntity() instanceof Monster || e.getEntity() instanceof Mob m && m.getPersistentData().getBoolean("CullingEnemy"))) return;
        CompoundTag d = ExpeditionProgress.data(p);
        int kills = d.getInt("CullingKillTally") + 1;
        d.putInt("CullingKillTally", kills);
        if (kills < FORCED_KILLS) return;
        ServerLevel level = p.serverLevel();
        if (!level.isNight()) { // 白天先攒着，天黑再拉
            if (kills == FORCED_KILLS) p.sendSystemMessage(Component.literal(
                "你杀戮的气息惊动了什么……入夜后，它们会来带你走。").withStyle(ChatFormatting.DARK_RED));
            return;
        }
        d.putInt("CullingKillTally", 0);
        if (PrisonSystem.locked(p)) return;
        p.sendSystemMessage(Component.literal("天空瞬间变成血红色——你被强制拉入了死灭回游。")
            .withStyle(ChatFormatting.DARK_RED));
        CullingGame.enter(p);
        CompoundTag pd = ExpeditionProgress.data(p);
        pd.putInt("CullingPoints", pd.getInt("CullingPoints") + 50);
        p.sendSystemMessage(Component.literal("【称号】不情愿的参赛者 · 补偿积分 +50").withStyle(ChatFormatting.GOLD));
    }

    /** 死亡重生后属性会重建：把「暴力」的 +20% 重新挂上（退出结界也保留）。 */
    @SubscribeEvent
    public static void clone(net.minecraftforge.event.entity.player.PlayerEvent.Clone e) {
        if (!e.isWasDeath() || !(e.getEntity() instanceof ServerPlayer p)) return;
        applyViolence(p);
    }

    @SubscribeEvent
    public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) applyViolence(p);
    }

    private static void applyViolence(ServerPlayer p) {
        var d = ExpeditionProgress.data(p);
        if (d.getInt("CullingStartBuff") != 0) return;   // 0 = 暴力
        if (!d.getBoolean("CullingRegistered")) return;
        var atk = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null && atk.getModifier(ATK_UUID) == null)
            atk.addPermanentModifier(new AttributeModifier(ATK_UUID, "死灭开局·暴力",
                0.2, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    // ===== 邀请函：原地展开黑色立方体，40 刻后把附近玩家吸进去（r257） =====
    private static final java.util.List<Object[]> PENDING = new java.util.ArrayList<>();

    public static void openInvitation(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Network.send(level, new Network.Effect(623, p.getId(), p.position(),
            net.minecraft.world.phys.Vec3.ZERO, 16f, 40, 0));
        PENDING.add(new Object[]{level, p.position(), level.getGameTime() + 40});
        p.sendSystemMessage(Component.literal("羂索的邀请函展开了——黑色立方体正在把你和周围的人吞进去。")
            .withStyle(ChatFormatting.DARK_RED));
    }

    @SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END || PENDING.isEmpty()) return;
        for (var it = PENDING.iterator(); it.hasNext(); ) {
            Object[] job = it.next();
            ServerLevel level = (ServerLevel) job[0];
            long at = (long) job[2];
            if (level.getGameTime() < at) continue;
            it.remove();
            net.minecraft.world.phys.Vec3 center = (net.minecraft.world.phys.Vec3) job[1];
            for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(16),
                o -> !o.isSpectator())) {
                CullingGame.enter(other);
            }
        }
    }

    private CullingEntry() { }
}
