package character.mahito;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 无为转变产出的物品基类：
 * · 名称标明「储存了谁」；tooltip 显示来源/生命；
 * · 以被贮存生物的最大生命作为耐久上限（存在 NBT 里，动态读取）。
 */
public class TransfiguredItem extends Item {
    public TransfiguredItem(Properties properties) { super(properties); }

    public static CompoundTag stored(ItemStack stack) { return stack.getTagElement(MahitoTransfigure.BASE_TAG); }

    @Override public int getMaxDamage(ItemStack stack) {
        CompoundTag tag = stored(stack);
        if (tag == null) return 1;
        return Math.max(1, Math.round(tag.getFloat("maxHealth")));
    }

    @Override public boolean isDamageable(ItemStack stack) { return stored(stack) != null; }

    @Override public boolean isBarVisible(ItemStack stack) { return stored(stack) != null; }

    @Override public int getBarWidth(ItemStack stack) {
        int max = Math.max(1, getMaxDamage(stack));
        return Math.round(13.0f * Math.max(0, max - stack.getDamageValue()) / max);
    }

    @Override public int getBarColor(ItemStack stack) { return 0xFF6FD3E8; }

    @Override public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level,
                                          List<Component> lines, TooltipFlag flag) {
        CompoundTag tag = stored(stack);
        if (tag == null) {
            lines.add(Component.literal("未贮存任何生物").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.literal("贮存：" + tag.getString("name")).withStyle(ChatFormatting.AQUA));
        if (tag.contains("attack")) lines.add(Component.literal("攻击：" + String.format("%.1f", tag.getFloat("attack"))).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("生命：" + (int) tag.getFloat("health") + " / " + (int) tag.getFloat("maxHealth"))
            .withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("耐久：" + Math.max(0, getMaxDamage(stack) - stack.getDamageValue()) + " / " + getMaxDamage(stack))
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** 耐久耗尽时：「判定死亡」并按掉落表掉在玩家脚下（在贮存时记录的位置生成尸体再击杀）。 */
    public static void releaseAndKill(Player player, ItemStack stack) {
        CompoundTag tag = stored(stack);
        if (tag == null || !(player.level() instanceof ServerLevel level)) return;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
        if (id == null) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        Entity entity = type.create(level);
        if (entity == null) return;
        entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0f);
        level.addFreshEntity(entity);
        if (entity instanceof LivingEntity living) {
            if (living instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
            living.hurt(level.damageSources().playerAttack(player), 1.0e9f);
        }
        if (!entity.isRemoved()) entity.discard();
        player.sendSystemMessage(Component.literal("无为转变：贮存物已耗尽并死亡，掉落物落在脚下")
            .withStyle(ChatFormatting.GRAY));
    }
}
