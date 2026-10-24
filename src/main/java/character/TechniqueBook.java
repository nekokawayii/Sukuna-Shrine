package character;

import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/** 保留 sukuna_memory 注册编号让旧物品直接升级成选择书；不再食用或消耗。 */
public final class TechniqueBook extends Item {
    public TechniqueBook() { super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer p)CharacterPackets.open(p);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
