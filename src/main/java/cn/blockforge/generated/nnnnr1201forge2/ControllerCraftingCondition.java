package cn.blockforge.generated.nnnnr1201forge2;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/** 配方由服务端加载并同步；关闭时九格泥土不产生控制器。 */
public final class ControllerCraftingCondition implements ICondition {
    private static final ResourceLocation ID=new ResourceLocation("sukuna","controller_crafting_enabled");
    @Override public ResourceLocation getID(){return ID;}
    @Override public boolean test(IContext context){return GrowthConfig.SPEC.isLoaded()&&GrowthConfig.CONTROLLER_CRAFTING.get();}
    public static void register(){CraftingHelper.register(new Serializer());}
    private static final class Serializer implements IConditionSerializer<ControllerCraftingCondition> {
        @Override public ResourceLocation getID(){return ID;}
        @Override public ControllerCraftingCondition read(JsonObject json){return new ControllerCraftingCondition();}
        @Override public void write(JsonObject json,ControllerCraftingCondition condition){}
    }
}
