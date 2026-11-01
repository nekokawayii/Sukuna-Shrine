package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 沿用旧物品编号和配方；附件完整保存在物品内，不复制、不吞掉原咒具的属性。 */
public final class ThousandMileChainItem extends Item {
    private static final String ATTACHMENT = "ThousandMileSpear";

    public ThousandMileChainItem() { super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()); }
    public static boolean isChain(ItemStack stack) { return stack.getItem() instanceof ThousandMileChainItem; }
    public static ItemStack attachment(ItemStack stack) {
        return stack.hasTag() ? ItemStack.of(stack.getTag().getCompound(ATTACHMENT)) : ItemStack.EMPTY;
    }
    public static boolean armed(ItemStack stack) { return isSpear(attachment(stack)); }
    public static boolean isSpear(ItemStack stack) { return stack.is(Content.TENKA_SPEAR.get()) || stack.is(Content.TENKA.get()); }

    public static void configure(ServerPlayer p) {
        ItemStack chain = p.getMainHandItem();
        if (!isChain(chain) || !p.isAlive() || p.isSpectator() || PrisonSystem.locked(p)
            || ThousandMileChain.bound(p) || p.getCooldowns().isOnCooldown(chain.getItem())) return;
        p.getCooldowns().addCooldown(chain.getItem(), 5);
        if (chain.getCount() != 1) {
            tell(p, "旧存档中叠在一起的万里之锁，请先拆出一条再使用");
            return;
        }
        if (ThousandMileChain.active(p)) {
            tell(p, "先收回锁链，再装卸天逆蛑");
            return;
        }
        ItemStack installed = attachment(chain);
        if (!installed.isEmpty()) {
            if (!p.isShiftKeyDown()) {
                tell(p, "已装上天逆蛑：缠绕期间持续封锁咒术；潜行右键可拆下");
                return;
            }
            if (!p.getOffhandItem().isEmpty()) {
                tell(p, "请先空出副手，再潜行右键拆下天逆蛑");
                return;
            }
            chain.getOrCreateTag().remove(ATTACHMENT);
            p.setItemInHand(InteractionHand.OFF_HAND, installed);
            tell(p, "天逆蛑已完整退回副手");
        } else {
            ItemStack source = p.getOffhandItem();
            if (!isSpear(source)) {
                tell(p, "主手拿万里之锁，副手拿天逆蛑，再右键装上");
                return;
            }
            ItemStack saved = source.copy();
            saved.setCount(1);
            chain.getOrCreateTag().put(ATTACHMENT, saved.save(new CompoundTag()));
            // 创造模式也转移这一件实体物品，拆装不能凭空复制附件。
            source.shrink(1);
            tell(p, "已装上天逆蛑：命中切断术式，缠绕期间持续封锁咒术");
        }
        p.getInventory().setChanged();
        p.containerMenu.broadcastChanges();
    }
    private static void tell(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text), true); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (player instanceof ServerPlayer p) configure(p);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext c) {
        if (c.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (c.getPlayer() instanceof ServerPlayer p) configure(p);
        return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer p) configure(p);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @Override public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) { return false; }
    @Override public boolean onLeftClickEntity(ItemStack stack, Player player, net.minecraft.world.entity.Entity entity) { return true; }
    @Override public boolean isFoil(ItemStack stack) { return armed(stack) || super.isFoil(stack); }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("左键① 抛下有重力、会碰撞的锁链一端").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("左键② 只有自己看不见首端时，沿准星直线无限延伸").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("命中缠绕定身；左键③ 收回，并将目标拉到面前安全处").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("潜行左键随时收回；切换主手也会解除缠绕").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.literal("副手拿天逆蛑后右键装上；空副手潜行右键拆下").withStyle(ChatFormatting.GRAY));
        if (armed(stack)) lines.add(Component.literal("已装天逆蛑：缠绕期间持续封锁咒术").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
