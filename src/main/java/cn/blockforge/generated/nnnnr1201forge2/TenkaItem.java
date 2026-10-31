package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 天逆鉾：伏黑甚尔的咒具短刀，**能斩断术式**——命中时清掉目标身上所有增益（护盾/无敌/领域加持），
 * 配合甚尔连招 6「天逆鉾·破魔」使用。
 *
 * <p>占位说明：目前复用原版铁剑模型（`minecraft:item/iron_sword`），贴图等用户给了再换。
 */
public class TenkaItem extends SwordItem {
    public TenkaItem() {
        super(Tiers.NETHERITE, 5, -1.8f, new net.minecraft.world.item.Item.Properties().stacksTo(1)
            .rarity(net.minecraft.world.item.Rarity.EPIC));
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.getActiveEffects().stream()
            .filter(effect -> effect.getEffect().isBeneficial())
            .toList()
            .forEach(effect -> target.removeEffect(effect.getEffect()));
        target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines,
                                          TooltipFlag flag) {
        lines.add(Component.literal("能斩断术式的咒具：命中清除目标全部增益").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("甚尔连招：闪避 → [2] 切天逆鉾 → 左键 = 破魔").withStyle(ChatFormatting.DARK_AQUA));
    }
}
