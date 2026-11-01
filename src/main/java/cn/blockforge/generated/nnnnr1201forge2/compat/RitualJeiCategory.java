package cn.blockforge.generated.nnnnr1201forge2.compat;

import cn.blockforge.generated.nnnnr1201forge2.ExpeditionContent;
import cn.blockforge.generated.nnnnr1201forge2.RitualRecipes;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** JEI：祭坛（精炼祭坛）仪式配方分类。 */
public class RitualJeiCategory implements IRecipeCategory<RitualRecipes.Recipe> {

    public static RecipeType<RitualRecipes.Recipe> TYPE =
        RecipeType.create("sukuna", "ritual", RitualRecipes.Recipe.class);

    private final IDrawable icon;
    private final IDrawable background;

    public RitualJeiCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(ExpeditionContent.ITEMS.get("refining_altar").get()));
        this.background = helper.createBlankDrawable(162, 60);
    }

    @Override public RecipeType<RitualRecipes.Recipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.literal("祭坛仪式"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, RitualRecipes.Recipe recipe, IFocusGroup focuses) {
        int i = 0;
        for (Map.Entry<String, Integer> entry : recipe.input().entrySet()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 2 + (i % 4) * 20, 2 + (i / 4) * 20)
                .addItemStack(new ItemStack(RitualRecipes.resolve(entry.getKey()), entry.getValue()));
            i++;
            if (i >= 12) break;
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 20).addItemStack(recipe.result());
    }

    public static List<RitualRecipes.Recipe> all() { return RitualRecipes.ALL; }
}
