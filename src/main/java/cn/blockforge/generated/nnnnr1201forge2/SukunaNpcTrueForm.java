package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 宿傩 NPC「原身形态」的触发（r271）。
 *
 * <p>关键：**不能让这一击把它打死**。先把伤害压到"只掉到 1 点血"，等伤害结算完
 * （{@link LivingDamageEvent}）再变身回满；否则实体会进入死亡动画，
 * 我们中途 setHealth 只会把它变成"半死不死、一直抽搐的红色实体"。
 */
public final class SukunaNpcTrueForm {
    /** 咒胎受肉体：几条命（可调）。耗光最后一条命后才变成原身形态。 */
    public static final int LIVES = 5;

    private static int lives(SukunaNpcEntity npc) {
        var d = npc.getPersistentData();
        if (!d.contains("SukunaLives")) d.putInt("SukunaLives", LIVES);
        return d.getInt("SukunaLives");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clamp(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof SukunaNpcEntity npc) || npc.level().isClientSide) return;
        if (npc.getPersistentData().getBoolean("SukunaTrueForm")) return;
        float after = npc.getHealth() - e.getAmount();
        if (after <= 1.0f) e.setAmount(Math.max(0.0f, npc.getHealth() - 1.0f));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void transform(LivingDamageEvent e) {
        if (!(e.getEntity() instanceof SukunaNpcEntity npc) || npc.level().isClientSide) return;
        if (npc.getHealth() > 1.0f || npc.getPersistentData().getBoolean("SukunaTrueForm")) return;
        int left = lives(npc);
        if (left > 0) {
            // 咒胎受肉体：先烧掉一条命，回满继续打
            npc.getPersistentData().putInt("SukunaLives", left - 1);
            npc.deathTime = 0;
            npc.hurtTime = 0;
            npc.invulnerableTime = 10;
            npc.setHealth(npc.getMaxHealth());
            npc.level().playSound(null, npc.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_HURT,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, .8f);
            if (npc.getTarget() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("两面宿傩 · 咒胎受肉体：还剩 " + (left - 1) + " 条命")
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
            }
            return;
        }
        // 最后一条命也没了 → 原身形态
        npc.enterTrueForm();
        if (npc.getTarget() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("两面宿傩 · 原身显现")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
    }

    private SukunaNpcTrueForm() { }
}
