package cn.blockforge.generated.nnnnr1201forge2.compat;

import cn.blockforge.generated.nnnnr1201forge2.ExpeditionContent;
import cn.blockforge.generated.nnnnr1201forge2.RitualRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** JEI 插件：把祭坛仪式（RitualRecipes.ALL）展示成配方分类。JEI 是可选前置，未安装时本类不会被加载。 */
@JeiPlugin
public class SukunaJeiPlugin implements IModPlugin {

    @Override public ResourceLocation getPluginUid() {
        return new ResourceLocation("jujutsu_kaisen_cursed_paths", "jei_plugin");
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RitualJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RitualJeiCategory.TYPE, RitualJeiCategory.all());
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
            new ItemStack(ExpeditionContent.ITEMS.get("refining_altar").get()),
            RitualJeiCategory.TYPE);
    }
}
