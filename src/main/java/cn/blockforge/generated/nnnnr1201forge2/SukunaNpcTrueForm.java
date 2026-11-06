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
        npc.enterTrueForm();
        if (npc.getTarget() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("两面宿傩 · 原身显现")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
    }

    private SukunaNpcTrueForm() { }
}
