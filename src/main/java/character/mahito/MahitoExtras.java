package character.mahito;

import cn.blockforge.generated.nnnnr1201forge2.Curse;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 真人战斗轮盘 2107–2112：无为转变自带的六个术式（改造肉墙／肉桥／灵魂治疗／肉体爆裂／分身／潜能激发）。
 * <p>全部转调 {@link MahitoPowers} 与 {@link MahitoSelf} 的既有实现（与无为转变多页轮盘同一份逻辑），
 * 不在这里重写数值；轮盘放不出来（没目标、内部冷却、目标已炸过）时把 {@link Curse} 扣掉的咒力退回。
 */
public final class MahitoExtras {
    public static void cast(ServerPlayer p, int id) {
        switch (id) {
            case MahitoSkills.WALL -> MahitoPowers.wall(p);
            case MahitoSkills.BRIDGE -> MahitoPowers.bridge(p);
            case MahitoSkills.HEAL -> {
                long now = p.level().getGameTime();
                long ready = Curse.data(p).getLong("MahitoHealReady");
                if (now < ready) {
                    p.sendSystemMessage(Component.literal("灵魂治疗冷却中 · "
                        + String.format("%.1f", (ready - now) / 20.0) + " 秒").withStyle(ChatFormatting.GRAY));
                    refund(p, id);
                    return;
                }
                MahitoSelf.heal(p);
            }
            case MahitoSkills.BURST -> {
                LivingEntity target = MahitoCombat.aimed(p, 10);
                if (target == null) {
                    p.sendSystemMessage(Component.literal("肉体爆裂：准星前方十格内没有目标").withStyle(ChatFormatting.GRAY));
                    refund(p, id);
                    return;
                }
                if (Curse.data(p).getCompound(MahitoSelf.BURST_USED).getBoolean(target.getUUID().toString())) {
                    p.sendSystemMessage(Component.literal("肉体爆裂：这个目标已经炸过一次了").withStyle(ChatFormatting.GRAY));
                    refund(p, id);
                    return;
                }
                MahitoSelf.burst(p, target);
            }
            case MahitoSkills.CLONE -> {
                boolean had = Curse.data(p).hasUUID("MahitoCloneId");
                MahitoSelf.clone(p);
                if (had) refund(p, id);   // 收回分身不花咒力
            }
            case MahitoSkills.POTENTIAL -> MahitoPowers.potential(p);
            default -> { }
        }
    }

    /** 把 {@link character.CharacterCasting} 已经扣掉的咒力退回去。 */
    private static void refund(ServerPlayer p, int id) {
        var definition = character.CharacterRegistry.get("mahito");
        var skill = definition == null ? null : definition.config().skill(id);
        if (skill != null) Curse.add(p, skill.cost());
    }

    private MahitoExtras() {}
}
