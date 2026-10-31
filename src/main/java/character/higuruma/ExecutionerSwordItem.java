package character.higuruma;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 处刑人之剑：命中直接把目标生命设为 0（不走伤害事件），耐久 1。 */
public class ExecutionerSwordItem extends SwordItem {
    public ExecutionerSwordItem(Properties properties) { super(Tiers.NETHERITE, 0, -2.4f, properties); }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.setHealth(0f);
        if (target.isAlive()) target.hurt(attacker.damageSources().genericKill(), Float.MAX_VALUE);
        stack.shrink(1);   // 耐久 1：处刑一次即毁
        return true;
    }

    @Override public int getMaxDamage(ItemStack stack) { return 1; }
    @Override public boolean isDamageable(ItemStack stack) { return true; }

    @Override public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level,
                                          List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("处刑人之剑 · 一击处刑（耐久 1）").withStyle(ChatFormatting.DARK_RED));
    }
}
