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
    /** 法轮生效：实体头盔槽真的戴着，或受肉玩家选择了十影的「不完全显现」。 */
    public static boolean wheelOnHead(LivingEntity entity){
        return entity!=null&&(isWheel(entity.getItemBySlot(EquipmentSlot.HEAD))||entity instanceof ServerPlayer p&&TenShadows.partial(p));
    }
    /** 「开 · 炎弓 / 灶 · 粉尘爆炸 / 灶 · 开」这一系对魔虚罗的**特攻倍率**：20 倍（其余目标不受影响）。 */
    public static final float FURNACE_MULTIPLIER=20f;
    /** 目标是不是魔虚罗（含**友方**魔虚罗）：只有它吃特攻倍率。 */
    public static boolean isMahoraga(LivingEntity target){
        return target instanceof MahoragaEntity;
    }
    /** 对魔虚罗的伤害倍率：是魔虚罗就返回 {@code multiplier}，否则恒为 1 —— 直接乘进原值即可，不影响其它目标。 */
    public static float bossDamage(LivingEntity target,float multiplier){
        return isMahoraga(target)?multiplier:1f;
    }
    private MahoragaRules(){}
}
