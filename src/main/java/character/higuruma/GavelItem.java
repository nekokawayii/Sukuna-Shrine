package character.higuruma;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 日车宽见的「法槌」：不可丢弃、无限耐久，刻印数据存在 NBT「GavelSeal」。 */
public class GavelItem extends SwordItem {
    public static final String SEAL = "GavelSeal";
    public static final String SEAL_DAMAGE = "GavelDamage";
    public static final String SEAL_SPEED = "GavelSpeed";

    public GavelItem(Properties properties) { super(Tiers.NETHERITE, 4, -2.4f, properties); }

    @Override public boolean onDroppedByPlayer(ItemStack item, Player player) { return false; }
    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isBarVisible(ItemStack stack) { return false; }
    @Override public boolean canBeDepleted() { return false; }

    @Override public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level,
                                          List<Component> lines, TooltipFlag flag) {
        CompoundTag tag = stack.getTagElement(SEAL);
        lines.add(Component.literal("不可丢弃 · 无限耐久").withStyle(ChatFormatting.DARK_GRAY));
        if (tag == null || tag.isEmpty()) {
            lines.add(Component.literal("刻印：无（副手持武器后用「刻印」）").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.literal("刻印：" + tag.getString("name")).withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("伤害：" + String.format("%.1f", stack.getOrCreateTag().getFloat(SEAL_DAMAGE))
            + "  攻速：" + String.format("%.2f", stack.getOrCreateTag().getFloat(SEAL_SPEED))).withStyle(ChatFormatting.GRAY));
    }
}
