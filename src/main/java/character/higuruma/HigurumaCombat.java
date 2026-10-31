package character.higuruma;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static cn.blockforge.generated.nnnnr1201forge2.Content.GAVEL;

/** 诛伏赐死：法槌授予/回收、刻印、投掷回旋、领域展开。 */
public final class HigurumaCombat {
    private static final String DOMAIN_END = "HigurumaDomainEnd";
    private static final int RADIUS = 16, HEIGHT = 15, DOMAIN_TICKS = 600;

    /** 选中日车宽见时确保有法槌；换角色时移除。 */
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        boolean higuruma = character.CharacterService.selected(p) != null
            && "higuruma".equals(character.CharacterService.selected(p).id());
        if (higuruma) {
            if (!hasGavel(p)) giveGavel(p);
        } else if (hasGavel(p)) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).getItem() == GAVEL.get()) p.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static boolean hasGavel(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)
            if (p.getInventory().getItem(i).getItem() == GAVEL.get()) return true;
        return false;
    }

    public static void giveGavel(ServerPlayer p) {
        ItemStack gavel = new ItemStack(GAVEL.get());
        gavel.setHoverName(Component.literal("法槌").withStyle(ChatFormatting.GOLD));
        if (!p.getInventory().add(gavel)) p.drop(gavel, false);
        p.sendSystemMessage(Component.literal("诛伏赐死：法槌已授予（不可丢弃）").withStyle(ChatFormatting.GOLD));
    }

    /** 1 刻印：把副手武器的伤害/攻速/特殊 NBT 刻进法槌。 */
    public static void seal(ServerPlayer p) {
        ItemStack gavel = findGavel(p);
        if (gavel == null) { p.sendSystemMessage(Component.literal("需要先持有法槌").withStyle(ChatFormatting.GRAY)); return; }
        ItemStack off = p.getOffhandItem();
        if (off.isEmpty()) { p.sendSystemMessage(Component.literal("副手没有武器可刻印").withStyle(ChatFormatting.GRAY)); return; }
        CompoundTag seal = new CompoundTag();
        seal.putString("name", off.getHoverName().getString());
        seal.put("item", off.save(new CompoundTag()));
        ItemStack proxy = off.copy();
        double damage = 1.0, speed = 4.0;
        for (var entry : proxy.getAttributeModifiers(EquipmentSlot.MAINHAND).entries()) {
            if (entry.getKey() == Attributes.ATTACK_DAMAGE) damage += entry.getValue().getAmount();
            if (entry.getKey() == Attributes.ATTACK_SPEED) speed += entry.getValue().getAmount();
        }
        gavel.getOrCreateTag().put(GavelItem.SEAL, seal);
        gavel.getOrCreateTag().putFloat(GavelItem.SEAL_DAMAGE, (float) damage);
        gavel.getOrCreateTag().putFloat(GavelItem.SEAL_SPEED, (float) speed);
        p.sendSystemMessage(Component.literal("刻印完成：" + off.getHoverName().getString()
            + "（伤害 " + String.format("%.1f", damage) + "）").withStyle(ChatFormatting.GOLD));
    }

    /** 2 强化：把法槌像忠诚三叉戟一样丢出去再飞回来（无蓄力）。 */
    public static void enhanceThrow(ServerPlayer p) {
        ItemStack gavel = findGavel(p);
        if (gavel == null) { p.sendSystemMessage(Component.literal("需要先持有法槌").withStyle(ChatFormatting.GRAY)); return; }
        if (!(p.level() instanceof ServerLevel level)) return;
        ItemStack thrown = gavel.copy();
        thrown.enchant(Enchantments.LOYALTY, 3);
        ThrownTrident trident = new ThrownTrident(level, p, thrown);
        trident.setPos(p.getX(), p.getEyeY() - 0.2, p.getZ());
        trident.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, 2.2f, 0.5f);
        level.addFreshEntity(trident);
        gavel.shrink(1);
        p.sendSystemMessage(Component.literal("强化：法槌已掷出（忠诚 III，自动飞回）").withStyle(ChatFormatting.GOLD));
    }

    /** 3 领域展开·诛伏赐死：再按一次收回。 */
    public static void domain(ServerPlayer p) { HigurumaDomain.toggle(p); }

    @SubscribeEvent public static void hit(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getAmount() <= 0) return;
        ItemStack main = p.getMainHandItem();
        if (main.getItem() != GAVEL.get() || main.getTag() == null) return;
        if (main.getTag().contains(GavelItem.SEAL_DAMAGE)) e.setAmount(main.getTag().getFloat(GavelItem.SEAL_DAMAGE));
    }

    private static ItemStack findGavel(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.getItem() == GAVEL.get()) return st;
        }
        return null;
    }

    private HigurumaCombat() {}
}
