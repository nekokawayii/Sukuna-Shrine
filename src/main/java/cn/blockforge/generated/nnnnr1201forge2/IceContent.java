package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class IceContent {
    public static final RegistryObject<Item> MEMORY=Content.ITEMS.register("uraume_memory",()->new IceFood(true));
    public static final RegistryObject<Item> FISH=Content.ITEMS.register("ice_fish",()->new IceFood(false));
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,"sukuna");
    public static final RegistryObject<SimpleCraftingRecipeSerializer<IceFishRecipe>> FISH_RECIPE=RECIPES.register("ice_fish",()->new SimpleCraftingRecipeSerializer<>(IceFishRecipe::new));
    public static void init(IEventBus bus){RECIPES.register(bus);}
    private static final class IceFood extends Item {
        private final boolean memory;
        IceFood(boolean memory){super(new Properties().stacksTo(memory?1:64).rarity(memory?Rarity.EPIC:Rarity.UNCOMMON).food(new FoodProperties.Builder().nutrition(memory?0:2).saturationMod(0.1f).alwaysEat().build()));this.memory=memory;}
        private boolean allowed(Player p){return memory?BranchRules.acceptMemory(Journey.unlocked(p),Uraume.unlocked(p),MaidBridge.isCaster(p),true):Uraume.unlocked(p);}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(!l.isClientSide&&!allowed(p)){p.displayClientMessage(Component.literal(memory?"已经吃过里梅记忆":"先吃里梅记忆，冰鱼才能强化冰凝咒法"),true);return InteractionResultHolder.fail(p.getItemInHand(h));}return super.use(l,p,h);}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity user){if(user instanceof ServerPlayer p){if(!allowed(p))return stack;if(!(memory?Uraume.consumeMemory(p):Uraume.feedFish(p)))return stack;}return super.finishUsingItem(stack,l,user);}
        @Override public void appendHoverText(ItemStack stack,Level l,List<Component> lines,TooltipFlag flags){
            lines.add(Component.literal(memory?"长按右键：解锁霜凪与直瀑":"每条永久增加"+BalanceRules.number(EnergyRules.ICE_FISH_BONUS)+"咒力上限，并恢复等量咒力；冰伤 +1%"));
            if(!memory)lines.add(Component.literal("每10条强化技能；以前吃过的冰鱼自动计入上限"));
            lines.add(Component.literal(memory?"先宿傩后里梅可双修；反过来不行":"任意名字带冰的物品 + 名字带鱼的物品，无序合成"));
            if(memory)lines.add(Component.literal("潜行右键喂女仆；女仆只能选一种记忆"));
        }
    }
    public static final class IceFishRecipe extends CustomRecipe {
        public IceFishRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
        @Override public boolean matches(CraftingContainer c,Level l){List<ItemStack> items=new ArrayList<>();for(int i=0;i<c.getContainerSize();i++)if(!c.getItem(i).isEmpty())items.add(c.getItem(i));return items.size()==2&&((ice(items.get(0))&&fish(items.get(1)))||(ice(items.get(1))&&fish(items.get(0))));}
        @Override public ItemStack assemble(CraftingContainer c,RegistryAccess a){return new ItemStack(FISH.get());}
        @Override public boolean canCraftInDimensions(int w,int h){return w*h>=2;}
        @Override public ItemStack getResultItem(RegistryAccess a){return new ItemStack(FISH.get());}
        @Override public RecipeSerializer<?> getSerializer(){return FISH_RECIPE.get();}
    }
    private static final Map<String,Map<String,String>> chinese=new HashMap<>();
    private static String names(ItemStack stack){
        ResourceLocation id=ForgeRegistries.ITEMS.getKey(stack.getItem());if(id==null)return stack.getHoverName().getString();
        Map<String,String> translations=chinese.computeIfAbsent(id.getNamespace(),ns->{
            Map<String,String> map=new HashMap<>();
            try(var stream=IceContent.class.getResourceAsStream("/assets/"+ns+"/lang/zh_cn.json")){
                if(stream!=null)try(var reader=new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)){var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();json.entrySet().forEach(e->{if(e.getValue().isJsonPrimitive())map.put(e.getKey(),e.getValue().getAsString());});}
            }catch(java.io.IOException|RuntimeException ignored){}
            return map;
        });
        return (stack.getHoverName().getString()+" "+translations.getOrDefault(stack.getDescriptionId(),"")+" "+id.getPath()).toLowerCase(Locale.ROOT);
    }
    private static boolean ice(ItemStack s){if(s.is(FISH.get()))return false;String name=names(s);return s.is(Items.ICE)||s.is(Items.PACKED_ICE)||s.is(Items.BLUE_ICE)||name.contains("冰")||Arrays.asList(name.split("[_ ]")).contains("ice");}
    private static boolean fish(ItemStack s){if(s.is(FISH.get()))return false;String name=names(s);return s.is(net.minecraft.tags.ItemTags.FISHES)||name.contains("鱼")||name.contains("fish");}
    private IceContent(){}
}
