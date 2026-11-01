package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 天逆蛑：把用户上传的 Blockbench 长矛模型做成的咒具。
 *
 * <ul>
 *   <li>模型与 display 全部来自 uploads/物品.bbmodel，原样转成物品模型（见
 *       {@code models/item/tenka_spear.json}），拿在手里、物品栏图标、展示框都按 Blockbench 里调好的摆法。</li>
 *   <li>右键「术式切断」：16 格内所有生物正在维持的术式（含领域、无下限）被强行终止，
 *       并且 3 秒内无法再使用任何咒术；自己也会被切断术式，但不会吃 3 秒封印。</li>
 *   <li>左键命中：只切断被打中那一个目标的术式，同样封 3 秒。</li>
 * </ul>
 *
 * <p>模型 display 数值不要改：用户明确要求原样保留。</p>
 */
public class TenkaSpearItem extends SwordItem {
    /** 术式封印时长：3 秒。 */
    public static final int SEAL_TICKS = 60;
    /** 右键「术式切断」冷却：30 秒。 */
    public static final int COOLDOWN_TICKS = 30 * 20;

    public TenkaSpearItem() {
        super(Tiers.NETHERITE, 6, -2.0f, new net.minecraft.world.item.Item.Properties()
            .stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);
        if (player.getCooldowns().isOnCooldown(this)) {
            player.displayClientMessage(Component.literal("天逆蛑冷却中"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        severAround(serverPlayer);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /**
     * 右键「术式切断」的实际效果（不含冷却判定）：切断附近一切术式与领域并封印 3 秒。
     * <p>r214 把这段抽出来，NPC 甚尔每 5 秒直接调这里（它拿的就是天逆蛑，但 FakePlayer 的
     * 物品栏/CD 是另一套，走 {@code use()} 会拿不到手上的矛）。
     */
    public static int severAround(net.minecraft.server.level.ServerPlayer serverPlayer) {
        int count = TechniqueSeverance.severAround(serverPlayer);
        Level level = serverPlayer.level();
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2f, 1.8f);
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
            SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.3f, 0.6f);
        serverPlayer.displayClientMessage(Component.literal(count > 0
            ? "天逆蛑 · 术式切断：" + count + " 处术式/领域被斩断，3 秒内无法使用咒术"
            : "天逆蛑 · 术式切断：附近没有可斩断的术式（自己的术式已终止）"), false);
        return count;
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) {
            TechniqueSeverance.sever(target, true, attacker);
            if (attacker instanceof Player p) {
                p.displayClientMessage(Component.literal("天逆蛑 · 破魔：命中目标的术式与领域已斩断，3 秒内无法施术").withStyle(ChatFormatting.AQUA), true);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines,
                                          TooltipFlag flag) {
        lines.add(Component.literal("能斩断术式的咒具：右键切断附近一切术式与领域，封印 3 秒").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("左键命中同样斩断该目标的术式（含无下限），冷却 30 秒").withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.literal("放在背包里会作为腰间挂件显示（按 F5 看自己）").withStyle(ChatFormatting.GRAY));
    }
}
