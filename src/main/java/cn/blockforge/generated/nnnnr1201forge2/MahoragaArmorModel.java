package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** 「法轮」戴在头上时**不画任何原版防具层**：它自己就是一个横着的法轮模型
 *  （见 {@link MahoragaWheelRenderer}），所以不能再用方盒子挡住玩家的头部建模。
 *
 *  <p>做法：返回一个"所有部件都为空"的人形模型（HumanoidModel 的根 ModelPart 没有任何子部件），
 *  同时 {@code getArmorTexture} 给一张全透明贴图兜底——原版防具层什么也画不出来，
 *  头部模型与皮肤完全不受影响。</p> */
public final class MahoragaArmorModel implements IClientItemExtensions {
    private static HumanoidModel<LivingEntity> emptyModel;
    @SuppressWarnings("unchecked")
    private static HumanoidModel<LivingEntity> empty(){
        if(emptyModel==null){
            ModelPart empty=new ModelPart(java.util.List.of(),java.util.Map.of());
            emptyModel=(HumanoidModel<LivingEntity>)(HumanoidModel<?>)new HumanoidModel<>(empty);
        }
        return emptyModel;
    }
    /** 直接返回 **null 的通用模型**：Forge 的 getArmorModel 会把它交给原版防具层，
     *  原版拿到 null 就整套跳过（不设模型、不绑贴图），所以既不会画方盒子，
     *  也不会出现"头部建模被换成紫色/缺失贴图"这种染色问题。 */
    @Override public net.minecraft.client.model.Model getGenericArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){
        return null;
    }
    /** 客户端初始化时把本类绑到「法轮」上（1.20.1 的入口是 Item.initializeClient）。 */
    public static void register(){
        java.util.function.Consumer<IClientItemExtensions> consumer=ext->{};
        ((net.minecraft.world.item.Item)VoidContent.MAHORAGA_WHEEL.get()).<IClientItemExtensions>initializeClient(consumer);
    }
    /** 单例：法轮只用一个空模型。 */
    private static final IClientItemExtensions EMPTY=new IClientItemExtensions(){
        @Override public net.minecraft.client.model.Model getGenericArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){return null;}
    };
    /** 空的 {@link IClientItemExtensions}（内部匿名类不能实例化，这里只是保留类型引用）。 */
    private MahoragaArmorModel(){}
}
