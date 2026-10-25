package character.yuta;

import cn.blockforge.generated.nnnnr1201forge2.CombatSettings;
import character.CharacterCasting;
import character.CharacterService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

/**
 * 「鬼妖村正」——从领域坟头拔出的刀。
 * <p>左键：结算 20 点<b>魔法伤害</b>（魔法伤害不吃护甲），并经过 {@code CombatSettings.spellDamage}，
 * 因此能吃到术式加成与借用爆发窗口。
 * <p>右键：消耗这把刀，随机释放一条**已复制**的术式，伤害按 3 倍（{@link YutaBoost} 覆盖倍率），
 * 该术式随后从轮盘消失（需要重新去掠夺）。
 * <p>没有复制任何术式时右键不生效、刀不报废。
 */
public final class YutaKatanaItem extends SwordItem {
    /** 基础伤害（无视护甲）。 */
    private static final float BASE_DAMAGE = 20f;
    /** 右键释放时的伤害倍率。 */
    private static final double CAST_MULTIPLIER = 3.0;

    public YutaKatanaItem(Properties properties) {
        // 木剑基底：普通挥砍只贡献极低的物理伤害，真实伤害走下面的魔法结算。
        super(Tiers.WOOD, 0, -2.2f, properties);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(character.yuta.YutaClient.katanaExtensions());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer p) {
            float damage = CombatSettings.spellDamage(p, BASE_DAMAGE);
            // 魔法伤害类型：原版护甲不减免，符合「无视护甲」。
            target.hurt(p.damageSources().magic(), damage);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer p)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);

        // 只有领域展开时才允许用刀放术式（领域期间的里香常驻是它的前提）。
        if (!YutaDomain.active(p)) {
            p.displayClientMessage(Component.literal("只能在「真赝相爱」领域内使用"), true);
            return InteractionResultHolder.fail(stack);
        }
        Set<Integer> copies = YutaCopies.copies(p);
        if (copies.isEmpty()) {
            p.displayClientMessage(Component.literal("还没有复制任何术式"), true);
            return InteractionResultHolder.fail(stack);
        }
        List<Integer> pool = new ArrayList<>(copies);
        int picked = pool.get(p.getRandom().nextInt(pool.size()));

        // 剑释放同样要付咒力（借用路径原本跳过了扣费）。
        int cost = 300;
        if (!cn.blockforge.generated.nnnnr1201forge2.Curse.spend(p, cost)) {
            p.displayClientMessage(Component.literal("咒力不足（需要 " + cost + "）"), true);
            return InteractionResultHolder.fail(stack);
        }
        YutaBoost.activate(p, CharacterService.rank(p), CAST_MULTIPLIER);
        CharacterCasting.cast(p, picked);
        // 刀报废 + 该术式从轮盘消失（需要重新掠夺）。
        YutaCopies.remove(p, picked);
        YutaCopies.sync(p);
        // 直接清空所在槽位：只改 stack 数量会被客户端回滚（创造模式尤其明显）。
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == stack) { inv.setItem(i, ItemStack.EMPTY); break; }
        }
        stack.setCount(0);
        inv.setChanged();
        p.displayClientMessage(Component.literal("村正断裂 · 释放 " + YutaCopies.nameOf(picked) + "（×3）"), false);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
