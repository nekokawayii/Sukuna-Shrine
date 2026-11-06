package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 宿傩 NPC「咒胎受肉体」多条命（r273）。
 *
 * <p>不再用"锁一血"：让他**正常被打死**，在 {@link LivingDeathEvent} 里取消死亡并复活。
 * 每死一次烧一条命（{@link #LIVES} 条，可调），全部烧光后这次死亡改成**进入原身形态**。
 *
 * <p>Forge 的 {@code LivingEntity#die} 会先 fire 这个事件、被取消就直接 return，
 * 因此不会掉战利品、不会进死亡动画；随后手工把血量/死亡状态清干净即可。
 */
public final class SukunaNpcTrueForm {
    /** 咒胎受肉体：几条命（可调）。 */
    public static final int LIVES = 5;

    private static int lives(SukunaNpcEntity npc) {
        var d = npc.getPersistentData();
        if (!d.contains("SukunaLives")) d.putInt("SukunaLives", LIVES);
        return d.getInt("SukunaLives");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof SukunaNpcEntity npc) || npc.level().isClientSide) return;
        var d = npc.getPersistentData();
        if (d.getBoolean("SukunaTrueForm")) return; // 已经是原身，不再有命
        int left = lives(npc);
        if (left > 0) {
            d.putInt("SukunaLives", left - 1);
            e.setCanceled(true);
            // r282：每次复活只回一半血
            npc.reviveFromDeath(npc.getMaxHealth() * 0.5);
            npc.level().playSound(null, npc.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_HURT,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, .8f);
            if (npc.getTarget() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("两面宿傩 · 咒胎受肉体：还剩 " + (left - 1) + " 条命")
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
            }
            return;
        }
        // 最后一条命也没了 → 这次死亡变成原身显现
        e.setCanceled(true);
        npc.enterTrueForm();
        if (npc.getTarget() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("两面宿傩 · 原身显现")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
    }

    private SukunaNpcTrueForm() { }
}
