package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "sukuna";
    public GeneratedMod() {
        GrowthConfig.register();
        TechniqueConfig.register();
        MinecraftForge.EVENT_BUS.register(TechniqueInput.class);
        MinecraftForge.EVENT_BUS.register(CombatExpansion.class);
        MinecraftForge.EVENT_BUS.register(SixfoldFinale.class);
        SkillDestruction.init();
        IceContent.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(SukunaBranches.class);
        MinecraftForge.EVENT_BUS.register(Uraume.class);
        SukunaTempleStructure.TYPES.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(SukunaTempleStructure.class);
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(
            (net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(ControllerCraftingCondition::register));
        StageFood.init();
        TechniqueEffects.EFFECTS.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(StageFood.class);
        MinecraftForge.EVENT_BUS.register(MaidBridge.class);
        MinecraftForge.EVENT_BUS.register(GuardianPlayerSkills.class);
        MinecraftForge.EVENT_BUS.register(DomainClash.class);
        // 原版生命属性会把5000截为1024；两端同步提升数值容许上限。
        var health=(net.minecraft.world.entity.ai.attributes.RangedAttribute)net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH;
        if(health.getMaxValue()<150000){try{double cap=health.getMaxValue();for(var field:net.minecraft.world.entity.ai.attributes.RangedAttribute.class.getDeclaredFields())if(field.getType()==double.class){field.setAccessible(true);if(field.getDouble(health)==cap){field.setDouble(health,1000000);break;}}if(health.getMaxValue()<150000)throw new IllegalStateException("生命属性上限未能提高");}catch(ReflectiveOperationException ex){throw new IllegalStateException("无法启用即身佛5000生命",ex);}}
        VoidContent.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(VoidJourney.class);
        MinecraftForge.EVENT_BUS.register(VoidCombat.class);
        EternalLove.init();
        MinecraftForge.EVENT_BUS.register(BossTargets.class);
        MinecraftForge.EVENT_BUS.register(BossControl.class);
        MinecraftForge.EVENT_BUS.register(IwateWaterRepair.class);
        SoulContent.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(SoulJourney.class);
        MinecraftForge.EVENT_BUS.register(RuinAtmosphere.class);
        MinecraftForge.EVENT_BUS.register(SoulCombat.class);
        ModTab.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        var armor=(net.minecraft.world.entity.ai.attributes.RangedAttribute)net.minecraft.world.entity.ai.attributes.Attributes.ARMOR;
        try{double cap=armor.getMaxValue();for(var field:net.minecraft.world.entity.ai.attributes.RangedAttribute.class.getDeclaredFields())if(field.getType()==double.class){field.setAccessible(true);if(field.getDouble(armor)==cap){field.setDouble(armor,1000000);break;}}}catch(ReflectiveOperationException ex){throw new IllegalStateException("无法启用净土守护者防御属性",ex);}
        ProgressionContent.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(Journey.class);
        Content.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        SkillSounds.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        Network.init();
        Network.registerDrawing();
        MinecraftForge.EVENT_BUS.register(SlashForms.class);
        MinecraftForge.EVENT_BUS.register(DismantleMotion.class);
        MinecraftForge.EVENT_BUS.register(FreehandDrawing.class);
        MinecraftForge.EVENT_BUS.register(Skills.class);
        MinecraftForge.EVENT_BUS.register(NetBranches.class);
        MinecraftForge.EVENT_BUS.register(Curse.class);
        MinecraftForge.EVENT_BUS.register(CombatSettings.class);
        MinecraftForge.EVENT_BUS.register(GuardianCombat.class);
        MinecraftForge.EVENT_BUS.register(GuardianProgress.class);
        // 对战败北在本模组的伤害加成结算后判断，防止附加伤害穿过1血保护。
        MinecraftForge.EVENT_BUS.register(EternalLove.class);
        if(net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid"))MinecraftForge.EVENT_BUS.register(MaidDuel.class);
    }
}
