package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 「咒灵操术之符」：正常游玩里召唤夏油杰的入口。
 * <p>右键消耗一张，在身前降下夏油杰，带一圈咒力粒子与音效；30 秒冷却防止连点刷屏。
 */
public class SummonTalismanItem extends Item {
    private static final int COOLDOWN_TICKS = 30 * 20;

    public SummonTalismanItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer p) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        // 站在方块上时在其前方 3 格落地，避免卡进墙里。
        BlockPos base = p.blockPosition().relative(p.getDirection(), 3);
        GetoEntity geto = Content.GETO.get().create(serverLevel);
        if (geto == null) return InteractionResultHolder.fail(stack);
        geto.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, p.getYRot() + 180f, 0f);
        serverLevel.addFreshEntity(geto);

        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            base.getX() + 0.5, base.getY() + 0.4, base.getZ() + 0.5, 60, 1.2, 0.6, 1.2, 0.02);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
            base.getX() + 0.5, base.getY() + 0.4, base.getZ() + 0.5, 40, 1.0, 0.8, 1.0, 0.03);
        serverLevel.playSound(null, base, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.2f, 0.7f);
        p.displayClientMessage(Component.literal("咒灵操术之符燃尽——夏油杰现身"), false);

        if (!p.getAbilities().instabuild) stack.shrink(1);
        p.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
