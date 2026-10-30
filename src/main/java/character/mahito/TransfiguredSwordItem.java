package character.mahito;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * 无为转变·剑：以被贮存生物的最大生命为耐久。
 * 左键命中目标 → 目标立刻消失（不判定死亡、不掉落），耐久 -1；
 * 耐久耗尽时 → 贮存物「判定死亡」，掉落物落在玩家脚下。
 */
public class TransfiguredSwordItem extends SwordItem {
    public TransfiguredSwordItem(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!(attacker instanceof Player player) || target instanceof Player) return true;
        if (TransfiguredItem.stored(stack) == null) return true;
        // 目标立刻消失，但不判定死亡（不触发掉落）
        target.discard();
        int next = stack.getDamageValue() + 1;
        int max = Math.max(1, getMaxDamage(stack));
        if (next >= max) {
            TransfiguredItem.releaseAndKill(player, stack);
            stack.shrink(1);
        } else {
            stack.setDamageValue(next);
        }
        return true;
    }
}
