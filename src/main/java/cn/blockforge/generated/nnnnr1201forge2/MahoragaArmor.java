package cn.blockforge.generated.nnnnr1201forge2;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** 「法轮」的防具材质：**头盔**，戴在头上才有模型。
 *
 *  <p>只占头部槽位（{@link #slot()} 返回 HEAD），防御力很低但带 4 点盔甲韧性——它不是防具，
 *  而是召唤物/进度显示器。耐久仿照下界合金（不可损坏），修理用金锭。</p> */
public final class MahoragaArmor {
    private static final Map<ArmorItem.Type,Integer> DEFENSE=new EnumMap<>(ArmorItem.Type.class);
    static{
        DEFENSE.put(ArmorItem.Type.BOOTS,1);
        DEFENSE.put(ArmorItem.Type.LEGGINGS,2);
        DEFENSE.put(ArmorItem.Type.CHESTPLATE,3);
        DEFENSE.put(ArmorItem.Type.HELMET,2);
    }
    private static final ArmorMaterial MATERIAL=new ArmorMaterial(){
        @Override public int getDurabilityForType(ArmorItem.Type type){return 37*13;}
        @Override public int getDefenseForType(ArmorItem.Type type){return DEFENSE.getOrDefault(type,0);}
        @Override public int getEnchantmentValue(){return 22;}
        @Override public net.minecraft.sounds.SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_GOLD;}
        @Override public Ingredient getRepairIngredient(){return Ingredient.of(Items.GOLD_INGOT);}
        @Override public String getName(){return "sukuna:mahoraga_wheel";}
        @Override public float getToughness(){return 4f;}
        @Override public float getKnockbackResistance(){return 0.1f;}
    };
    public static ArmorMaterial material(){return MATERIAL;}
    private MahoragaArmor(){}
}
