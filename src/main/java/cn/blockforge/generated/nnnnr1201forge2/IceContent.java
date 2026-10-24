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
        private boolean allowed(Player p){return memory?character.CharacterService.permits(p,character.CharacterDefinition.Family.ICE)&&BranchRules.acceptMemory(Journey.unlocked(p),Uraume.unlocked(p),MaidBridge.isCaster(p),true):Uraume.unlocked(p);}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(!l.isClientSide&&!allowed(p)){p.displayClientMessage(Component.literal(memory?"已经吃过里梅记忆":"先吃里梅记忆，冰鱼才能强化冰凝咒法"),true);return InteractionResultHolder.fail(p.getItemInHand(h));}return super.use(l,p,h);}
        @Override public void appendHoverText(ItemStack stack,Level l,List<Component> lines,TooltipFlag flags){
            lines.add(Component.literal(memory?"长按右键：解锁霜凪与直瀑":"右键**使用**：每条永久增加"+BalanceRules.number(EnergyRules.ICE_FISH_BONUS)+"咒力上限，并恢复等量咒力；冰伤 +1%"));
            if(!memory)lines.add(Component.literal("按住右键可以不松手连续使用：每 2 刻用掉一条，直到背包里的冰鱼用完"));
            if(!memory)lines.add(Component.literal("每10条强化技能；以前吃过的冰鱼自动计入上限"));
            lines.add(Component.literal(memory?"先宿傩后里梅可双修；反过来不行":"任意名字带冰的物品 + 名字带鱼的物品，无序合成"));
            if(memory)lines.add(Component.literal("潜行右键喂女仆；女仆只能选一种记忆"));
        }
        /** 冰鱼是"使用"而不是"吃"：按住右键连续使用，每条之间隔 {@link #FISH_USE_GAP} 刻。 */
        private static final int FISH_USE_GAP=2,FISH_HOLD_TICKS=72000;
        @Override public int getUseDuration(ItemStack stack){return memory?32:FISH_HOLD_TICKS;}
        @Override public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack){return memory?net.minecraft.world.item.UseAnim.EAT:net.minecraft.world.item.UseAnim.NONE;}
        @Override public void onUseTick(Level l,net.minecraft.world.entity.LivingEntity user,ItemStack stack,int remaining){
            if(l.isClientSide)return;
            // 只在"刚按下的那一份"和第 3、5、7…刻结算：按住不放就一直用
            int elapsed=FISH_HOLD_TICKS-remaining;
            if(elapsed!=0&&elapsed%2!=0)return;
            if(!(user instanceof ServerPlayer p))return;
            if(p.getCooldowns().isOnCooldown(stack.getItem()))return;
            if(!allowed(p))return;
            if(!Uraume.feedFish(p)){p.getCooldowns().addCooldown(stack.getItem(),20);return;}
            p.getCooldowns().addCooldown(stack.getItem(),FISH_USE_GAP);
            if(!p.isCreative())stack.shrink(1);
            if(stack.isEmpty())p.stopUsingItem();
        }
        /** 冰鱼：效果已经在 {@link #onUseTick} 里给过了，收尾不再走原版进食；
         *  里梅记忆仍走原版（它保留食物属性、音效与"长按右键解锁"的手感）。 */
        @Override public ItemStack finishUsingItem(ItemStack stack,Level l,LivingEntity user){
            if(memory){
                if(user instanceof ServerPlayer p){if(!allowed(p))return stack;if(!Uraume.consumeMemory(p))return stack;}
                return super.finishUsingItem(stack,l,user);
            }
            return stack;
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
