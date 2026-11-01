package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** 生存探索物品统一登记；旧版可食用残秽不改用途，新燃料独立分级。 */
public final class ExpeditionContent {
    public static final DeferredRegister<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENTS=DeferredRegister.create(ForgeRegistries.ENCHANTMENTS,"sukuna");
    public static final RegistryObject<net.minecraft.world.item.enchantment.Enchantment> SOUL_EDGE=ENCHANTMENTS.register("soul_edge",()->new net.minecraft.world.item.enchantment.Enchantment(net.minecraft.world.item.enchantment.Enchantment.Rarity.VERY_RARE,net.minecraft.world.item.enchantment.EnchantmentCategory.WEAPON,new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.MAINHAND}){@Override public boolean isTreasureOnly(){return true;}});
    public static final Map<String,RegistryObject<Item>> ITEMS=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Block>> BLOCKS=new LinkedHashMap<>();
    public static final DeferredRegister<BlockEntityType<?>> BES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"sukuna");
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,"sukuna");
    public static final RegistryObject<net.minecraft.world.effect.MobEffect> OVERHEAT=EFFECTS.register("overheat",()->new ExpeditionEffect(0xE65A22));
    public static final RegistryObject<net.minecraft.world.effect.MobEffect> RUNAWAY=EFFECTS.register("curse_runaway",()->new ExpeditionEffect(0xAD35DB));
    public static final String[] MATERIALS={"residue_low","residue_medium","residue_high","black_rope_fiber","blood_sakura_petal","ash_clump","eternal_fire","wraith_tear","raw_jujutsu_ore","jujutsu_alloy","star_iron_fragment","star_curse_core","soul_echo","volcanic_core","soul_molt","heavenly_bone","frame_retina","frozen_crystal","overtime_tie","seal_white","seal_black","seal_gold","kenjaku_thread","prison_fragment","curse_concentrate","ancient_talisman","deformed_bone","tainted_bone","mutant_gland","curse_crystal","gu_extract","broken_tool","alloy_blueprint","nine_paintings_manuscript","tengen_map","special_blueprint","curse_flesh","blood_wheat","nether_star_fragment","tenka_fragment","playful_cloud_parts","culling_key","simple_domain_disc","substitute_doll","reverse_injection","blood_sakura_wine","mental_resistance","cold_resistance","furnace_scroll","inferno_bomb","blood_blade","blood_armor","poison_coating","incarnation_reagent","soul_edge","frame_boots","foresight_goggles","overtime_binding","ice_scroll","ultimate_scroll","curtain_talisman","soul_capture_bottle","alloy_sword","alloy_chestplate","soul_splitter","thousand_mile_chain","forbidden_armor"};
    static {
        for(String id:MATERIALS) ITEMS.put(id,Content.ITEMS.register(id,()->makeItem(id)));
        block("cursed_soil",()->new Block(BlockBehaviour.Properties.copy(Blocks.DIRT)));
        block("blood_soil",()->new Block(BlockBehaviour.Properties.copy(Blocks.DIRT)));
        block("cursed_fissure",()->new Block(BlockBehaviour.Properties.copy(Blocks.CRYING_OBSIDIAN).strength(3).lightLevel(s->9)));
        block("blood_sakura_leaves",()->new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_LEAVES)));
        block("black_vines",()->new Block(BlockBehaviour.Properties.copy(Blocks.COBWEB).noCollission().noOcclusion().strength(.2f)));
        block("shimenawa",()->new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).noOcclusion()));
        block("volcanic_rock",()->new Block(BlockBehaviour.Properties.copy(Blocks.BASALT)));
        block("volcanic_ash",()->new Block(BlockBehaviour.Properties.copy(Blocks.SAND)));
        block("lava_obsidian",()->new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).lightLevel(s->6)));
        block("eternal_flame",()->new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5).noOcclusion().lightLevel(s->15)){
            @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource r){for(int i=0;i<4;i++){double x=p.getX()+.2+r.nextDouble()*.6,z=p.getZ()+.2+r.nextDouble()*.6;l.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,x,p.getY()+1.05,z,0,.025,0);l.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,x,p.getY()+1.2,z,0,.04,0);}}
        });
        block("jujutsu_ore",()->new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE)));
        block("alloy_block",()->new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(6,30)));
        block("alloy_bars",()->new IronBarsBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BARS)));
        block("curse_crystal_block",()->new Block(BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK).lightLevel(s->8)));
        block("spirit_stone",()->new Block(BlockBehaviour.Properties.copy(Blocks.CRYING_OBSIDIAN).lightLevel(s->5)));
        block("volcano_barrier",()->new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).strength(50,3600000)));
        block("simple_seal_pillar",()->new SealPillarBlock(false));
        block("special_seal_pillar",()->new SealPillarBlock(true));
        block("refining_altar",()->new RitualBlock(0));
        block("gu_vat",()->new RitualBlock(1));
        block("tengen_core",()->new RitualBlock(2));
        block("barrier_trap",()->new ExpeditionTrapBlock());
        block("culling_exchange",()->new Block(BlockBehaviour.Properties.copy(Blocks.LODESTONE)){
            @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand h,BlockHitResult hit){if(h==InteractionHand.MAIN_HAND&&p instanceof ServerPlayer sp)CullingGame.exchange(sp);return InteractionResult.sidedSuccess(l.isClientSide);}
        });
        block("culling_exit",()->new Block(BlockBehaviour.Properties.copy(Blocks.CRYING_OBSIDIAN).strength(-1,3600000).lightLevel(s->12)){
            @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand h,BlockHitResult hit){if(p instanceof ServerPlayer sp)CullingGame.leave(sp);return InteractionResult.sidedSuccess(l.isClientSide);}
        });
        block("culling_barrier",()->new GlassBlock(BlockBehaviour.Properties.copy(Blocks.BLACK_STAINED_GLASS).strength(-1,3600000).noOcclusion()));
    }
    public static final RegistryObject<BlockEntityType<RitualBlockEntity>> RITUAL=BES.register("ritual",()->BlockEntityType.Builder.of(RitualBlockEntity::new,block("refining_altar"),block("gu_vat"),block("tengen_core")).build(null));
    static { block("site_anchor",ExpeditionSiteBlock::new); }
    public static final RegistryObject<BlockEntityType<ExpeditionSiteEntity>> SITE=BES.register("expedition_site",()->BlockEntityType.Builder.of(ExpeditionSiteEntity::new,block("site_anchor")).build(null));
    private static Item makeItem(String id){
        if(id.equals("soul_splitter"))return new SoulDevourerItem();
        if(id.equals("thousand_mile_chain"))return new ThousandMileChainItem();
        if(id.equals("blood_blade")||id.equals("alloy_sword"))return new SwordItem(Tiers.DIAMOND,id.equals("blood_blade")?6:4,-2.4f,new Item.Properties());
        if(id.equals("frame_boots"))return new ExpeditionArmorItem(id,ArmorMaterials.DIAMOND,ArmorItem.Type.BOOTS,new Item.Properties());
        if(id.equals("blood_armor")||id.equals("alloy_chestplate")||id.equals("forbidden_armor"))return new ExpeditionArmorItem(id,ArmorMaterials.NETHERITE,ArmorItem.Type.CHESTPLATE,new Item.Properties().fireResistant());
        return new ExpeditionItem(id);
    }
    private static void block(String id,java.util.function.Supplier<Block> factory){var b=ProgressionContent.BLOCKS.register(id,factory);BLOCKS.put(id,b);ITEMS.put(id,Content.ITEMS.register(id,()->new BlockItem(b.get(),new Item.Properties()){ @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){if(id.equals("refining_altar")||id.equals("gu_vat")||id.equals("tengen_core"))lines.add(Component.literal("潜行右键切换配方，普通右键检查阵型并投入背包材料；成品留在台上右键领取。"));} }));}
    public static Item item(String id){return ITEMS.get(id).get();}
    public static Block block(String id){return BLOCKS.get(id).get();}
    public static void init(IEventBus bus){BES.register(bus);EFFECTS.register(bus);ENCHANTMENTS.register(bus);ExpeditionWorldgen.init(bus);}
    private static final class ExpeditionEffect extends net.minecraft.world.effect.MobEffect {
        ExpeditionEffect(int color){super(net.minecraft.world.effect.MobEffectCategory.HARMFUL,color);}
    }
    private ExpeditionContent(){}
}
