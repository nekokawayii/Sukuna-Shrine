package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 魔虚罗的目标规则：**攻击所有人**，不分敌我（召唤者也不例外），但不打自己、不打另一只魔虚罗。
 *  另外提供「法轮」是否戴在头上的判定：法轮是**头盔**，戴在头上才有模型。 */
public final class MahoragaRules {
    public static boolean valid(LivingEntity boss,LivingEntity target){
        if(target==null||target==boss||!target.isAlive()||target.isRemoved()||target.isSpectator())return false;
        if(target.level()!=boss.level())return false;
        if(target instanceof MahoragaEntity)return false;
        if(target instanceof ServerPlayer p&&p.isCreative())return false;
        return !target.isInvulnerable();
    }
    /** 「法轮」作为头盔：手套类槽位的通用判定（玩家与女仆都适用）。 */
    public static boolean isWheel(ItemStack stack){
        return stack!=null&&!stack.isEmpty()&&stack.is(VoidContent.MAHORAGA_WHEEL.get());
    }
    /** 法轮戴在头上：只有这种情况才在头顶画法轮模型。 */
    public static boolean wheelOnHead(LivingEntity entity){
        return entity!=null&&isWheel(entity.getItemBySlot(EquipmentSlot.HEAD));
    }
    private MahoragaRules(){}
}
