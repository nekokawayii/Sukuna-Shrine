package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/** 咒术选择书：右键打开六个流派的选择界面；选定后消耗一本。 */
public class TechniqueBook extends Item {
    public TechniqueBook(){super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));}

    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide&&player instanceof ServerPlayer sp)
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->sp),new Network.OpenTechnique());
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }

    /** 界面上点选后由服务端调用：选了才扣书，冷却/非法编号由 {@link TechniqueRules#select} 拒绝。 */
    public static void choose(ServerPlayer p,int technique){
        if(!TechniqueRules.select(p,technique))return;
        if(p.isCreative())return;
        for(InteractionHand hand:InteractionHand.values()){
            ItemStack stack=p.getItemInHand(hand);
            if(stack.is(ProgressionContent.BOOK.get())){stack.shrink(1);return;}
        }
        for(ItemStack stack:p.getInventory().items)if(stack.is(ProgressionContent.BOOK.get())){stack.shrink(1);return;}
    }
}
