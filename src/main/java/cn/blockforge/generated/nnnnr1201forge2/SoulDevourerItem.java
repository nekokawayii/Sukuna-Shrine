package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** 旧释魂刀沿用同一个注册名，升级存档无需替换物品。 */
public final class SoulDevourerItem extends SwordItem {
    public SoulDevourerItem(){super(Tiers.NETHERITE,8,-2.4f,new Item.Properties().rarity(Rarity.EPIC).fireResistant());}
    public static long hits(ItemStack stack){return stack.hasTag()?Math.max(0,stack.getTag().getLong("SoulDevourerHits")):0;}
    public static float multiplier(ItemStack stack){return (float)(1d+hits(stack)*.05d);}
    public static void recordHit(ItemStack stack){long n=hits(stack);if(n<Long.MAX_VALUE)stack.getOrCreateTag().putLong("SoulDevourerHits",n+1);}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("无视护甲、盾牌、防御附魔和抗性提升"));
        lines.add(Component.literal("每次有效命中后永久累加基础伤害的5%，不按复利计算"));
        lines.add(Component.literal("已噬魂 "+hits(stack)+" 次 · 当前伤害 ×"+String.format(java.util.Locale.ROOT,"%.2f",multiplier(stack))));
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){SoulDevourerClient.item(consumer);}
}
