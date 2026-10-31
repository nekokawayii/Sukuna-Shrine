package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

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
        MinecraftForge.EVENT_BUS.register(GojoEntity.class);
        MinecraftForge.EVENT_BUS.register(MahoragaEntity.class);
        MinecraftForge.EVENT_BUS.register(DomainAmplification.class);
        MinecraftForge.EVENT_BUS.register(GojoCombat.class);
        MinecraftForge.EVENT_BUS.register(VoidCombat.class);
        EternalLove.init();
        MinecraftForge.EVENT_BUS.register(BossTargets.class);
        MinecraftForge.EVENT_BUS.register(BossControl.class);
        MinecraftForge.EVENT_BUS.register(IwateWaterRepair.class);
        SoulContent.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        ShadowContent.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        PrisonContent.init();
        MinecraftForge.EVENT_BUS.register(SoulJourney.class);
        MinecraftForge.EVENT_BUS.register(RuinAtmosphere.class);
        MinecraftForge.EVENT_BUS.register(SoulCombat.class);
        ModTab.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        var armor=(net.minecraft.world.entity.ai.attributes.RangedAttribute)net.minecraft.world.entity.ai.attributes.Attributes.ARMOR;
        try{double cap=armor.getMaxValue();for(var field:net.minecraft.world.entity.ai.attributes.RangedAttribute.class.getDeclaredFields())if(field.getType()==double.class){field.setAccessible(true);if(field.getDouble(armor)==cap){field.setDouble(armor,1000000);break;}}}catch(ReflectiveOperationException ex){throw new IllegalStateException("无法启用净土守护者防御属性",ex);}
        ProgressionContent.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(Journey.class);
        Authority.init();
        Content.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        SkillSounds.init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        Network.init();
        character.CharacterPackets.register();
        // 乙骨忧太：两格轮盘的服务端效果（只登记一次，重复登记会抛异常）。
        character.CharacterCasting.registerEffect("yuta", character.yuta.YutaSkills.RIKA, character.yuta.YutaEffects::summonRika);
        character.CharacterCasting.registerEffect("yuta", character.yuta.YutaSkills.COPY, character.yuta.YutaEffects::copyMenu);
        character.CharacterCasting.registerEffect("yuta", character.yuta.YutaSkills.DOMAIN, character.yuta.YutaEffects::domain);
        character.CharacterCasting.registerEffect("yuta", character.yuta.YutaSkills.SURGERY, character.yuta.YutaEffects::surgery);
        character.CharacterCasting.registerEffect("yuta", character.yuta.YutaSkills.RCT_OUT, character.yuta.YutaEffects::rctOut);
        character.CharacterCasting.registerEffect("higuruma", character.higuruma.HigurumaSkills.SEAL, (character.CharacterCasting.Effect) (p, s2, r) -> character.higuruma.HigurumaCombat.seal(p));
        character.CharacterCasting.registerEffect("higuruma", character.higuruma.HigurumaSkills.ENHANCE, (character.CharacterCasting.Effect) (p, s2, r) -> character.higuruma.HigurumaCombat.enhanceThrow(p));
        character.CharacterCasting.registerEffect("higuruma", character.higuruma.HigurumaSkills.DOMAIN, (character.CharacterCasting.Effect) (p, s2, r) -> character.higuruma.HigurumaCombat.domain(p));
        MinecraftForge.EVENT_BUS.register(character.higuruma.HigurumaCombat.class);
        MinecraftForge.EVENT_BUS.addListener(character.higuruma.HigurumaDomain::serverTick);
        MinecraftForge.EVENT_BUS.addListener(character.higuruma.HigurumaDomain::serverStopped);
        // 鹿紫云一 · 幻兽琥珀：电荷 / 必中落雷 / 终奥
        character.CharacterCasting.registerEffect("kashimo", character.kashimo.KashimoSkills.DASH,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.kashimo.KashimoCombat.dash(p));
        character.CharacterCasting.registerEffect("kashimo", character.kashimo.KashimoSkills.DISCHARGE,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.kashimo.KashimoCombat.discharge(p));
        character.CharacterCasting.registerEffect("kashimo", character.kashimo.KashimoSkills.ATTRACT,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.kashimo.KashimoCombat.attract(p));
        character.CharacterCasting.registerEffect("kashimo", character.kashimo.KashimoSkills.ATTACH,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.kashimo.KashimoCombat.attach(p));
        character.CharacterCasting.registerEffect("kashimo", character.kashimo.KashimoSkills.DETONATE,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.kashimo.KashimoCombat.detonate(p));
        character.CharacterCasting.registerEffect("kashimo", character.kashimo.KashimoSkills.FIELD,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.kashimo.KashimoCombat.field(p));
        character.CharacterCasting.registerEffect("kashimo", character.kashimo.KashimoSkills.BOLT,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.kashimo.KashimoCombat.bolt(p));
        character.CharacterCasting.registerEffect("kashimo", character.kashimo.KashimoSkills.ORB,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.kashimo.KashimoCombat.orb(p));
        character.CharacterCasting.registerEffect("kashimo", character.kashimo.KashimoSkills.CHAIN,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.kashimo.KashimoCombat.chain(p));
        character.CharacterCasting.registerEffect("kashimo", character.kashimo.KashimoSkills.SHADOW,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.kashimo.KashimoCombat.shadow(p));
        character.CharacterCasting.registerLock("kashimo", new character.CharacterCasting.Lock() {
            @Override public boolean locked(net.minecraft.server.level.ServerPlayer player) {
                return character.kashimo.KashimoCombat.crashing(player);
            }
            @Override public String reason() { return "肉体崩溃中：无法使用任何术式"; }
        });
        MinecraftForge.EVENT_BUS.register(character.kashimo.KashimoCombat.class);
        // 东堂葵 · 不义游戏：注入咒力 / 换位
        character.CharacterCasting.registerEffect("todo", character.todo.TodoSkills.INJECT,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.todo.TodoCombat.inject(p));
        character.CharacterCasting.registerEffect("todo", character.todo.TodoSkills.SWAP,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.todo.TodoCombat.swap(p));
        MinecraftForge.EVENT_BUS.register(character.todo.TodoCombat.class);
        // 七海建人 · 十划咒法：7:3 弱点
        character.CharacterCasting.registerEffect("nanami", character.nanami.NanamiSkills.SMASH,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.nanami.NanamiCombat.smash(p));
        character.CharacterCasting.registerEffect("nanami", character.nanami.NanamiSkills.VALOR,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.nanami.NanamiCombat.valor(p));
        character.CharacterCasting.registerEffect("nanami", character.nanami.NanamiSkills.GAP,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.nanami.NanamiCombat.gap(p));
        character.CharacterCasting.registerEffect("nanami", character.nanami.NanamiSkills.OVERTIME,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.nanami.NanamiCombat.overtime(p));
        MinecraftForge.EVENT_BUS.register(character.nanami.NanamiCombat.class);
        // 石流龙 · 咒力强化/放出：冰沙冲击波（蓄力在 TechniqueInput 里拦截）/ 追踪弹 / 咒力喷涌 / 甜品 / 领域占位
        character.CharacterCasting.registerEffect("ryu", character.ryu.RyuSkills.SCATTER,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.ryu.RyuCombat.scatter(p));
        character.CharacterCasting.registerEffect("ryu", character.ryu.RyuSkills.BURST,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.ryu.RyuCombat.burst(p));
        character.CharacterCasting.registerEffect("ryu", character.ryu.RyuSkills.DESSERT,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.ryu.RyuCombat.dessert(p));
        character.CharacterCasting.registerEffect("ryu", character.ryu.RyuSkills.DOMAIN,
            (character.CharacterCasting.Effect) (p, s2, r) -> character.ryu.RyuCombat.domain(p));
        character.CharacterCasting.registerLock("ryu", new character.CharacterCasting.Lock() {
            @Override public boolean locked(net.minecraft.server.level.ServerPlayer player) {
                return character.ryu.RyuCombat.dessertActive(player);
            }
            @Override public String reason() { return "甜品时间：正在吃，2 秒内不能施术"; }
        });
        MinecraftForge.EVENT_BUS.register(character.ryu.RyuCombat.class);
        MinecraftForge.EVENT_BUS.register(character.ryu.RyuDomain.class);
        MinecraftForge.EVENT_BUS.register(character.toji.TojiQte.class);
        // 禅院直哉·投射咒法：八个技能全部走自己的服务端逻辑
        for (int naoyaSkill : character.naoya.NaoyaSkills.wheel()) {
            character.CharacterCasting.registerEffect("naoya", naoyaSkill,
                (character.CharacterCasting.Effect) (p2, s3, r3) -> character.naoya.NaoyaCombat.cast(p2, s3.id()));
        }
        MinecraftForge.EVENT_BUS.register(character.naoya.NaoyaCombat.class);
        MinecraftForge.EVENT_BUS.register(character.naoya.NaoyaDomain.class);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent event) -> {
            character.kashimo.KashimoCharges.reset();
            character.kashimo.KashimoZones.reset();
            character.todo.TodoMarks.reset();
            character.nanami.NanamiRatio.reset();
            character.ryu.RyuCombat.reset();
            character.ryu.RyuDomain.reset();
            character.naoya.NaoyaDomain.reset();
        });
        character.CharacterCasting.registerEffect("megumi", character.megumi.MegumiSkills.TEN_SHADOWS, (net.minecraft.server.level.ServerPlayer p, character.CharacterSkill skill, int rank) -> Network.sendTo(p, new Network.Effect(308, p.getId(), p.position(), net.minecraft.world.phys.Vec3.ZERO, 0f, 0, 0)));
        // 九十九由基·星之怒：四招全部原版伤害实现
        character.CharacterCasting.registerEffect("yuki", character.yuki.YukiSkills.MASS_FIST, character.yuki.YukiCombat::massFist);
        character.CharacterCasting.registerEffect("yuki", character.yuki.YukiSkills.GARUDA, character.yuki.YukiCombat::garuda);
        character.CharacterCasting.registerEffect("yuki", character.yuki.YukiSkills.STAR_CRASH, character.yuki.YukiCombat::starCrash);
        character.CharacterCasting.registerEffect("yuki", character.yuki.YukiSkills.DOMAIN, character.yuki.YukiCombat::domain);
        MinecraftForge.EVENT_BUS.addListener(character.yuki.YukiDomain::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(character.yuki.YukiCombat::onHurt);
        MinecraftForge.EVENT_BUS.addListener(character.yuki.YukiCombat::onPlayerTick);

        // 秤金次·坐杀博打：拳、连打、开盘、领域 + 领域 tick
        // 秤金次·坐杀博徒（r371 大改）：十个技能全部走 HakariCombat.cast
        for (int hakariSkill : character.hakari.HakariSkills.allIds()) {
            character.CharacterCasting.registerEffect("hakari", hakariSkill,
                (character.CharacterCasting.Effect) (p3, s3, r3) -> character.hakari.HakariCombat.cast(p3, s3));
        }
        character.CharacterCasting.registerCooldownScale("hakari", character.hakari.HakariCombat::cooldownScale);
        MinecraftForge.EVENT_BUS.addListener(character.hakari.HakariDomain::onServerTick);
        MinecraftForge.EVENT_BUS.register(character.hakari.HakariCombat.class);
        character.CharacterCasting.registerEffect("yuki", character.yuki.YukiSkills.STAR_RAGE, character.yuki.YukiCombat::starRage);
        MinecraftForge.EVENT_BUS.register(character.CharacterService.class);
        MinecraftForge.EVENT_BUS.register(character.CharacterRewards.class);
        // 漏壶·灾厄火焰：三招自定义效果 + 被动免疫 + 第一章泡岩浆进度
        character.CharacterCasting.registerEffect("jogo",character.jogo.JogoSkills.FIRE_BUG,character.jogo.JogoCombat::castBugs);
        character.CharacterCasting.registerEffect("jogo",character.jogo.JogoSkills.METEOR,character.jogo.JogoCombat::castMeteors);
        character.CharacterCasting.registerEffect("jogo",character.jogo.JogoSkills.LAVA_BURST,character.jogo.JogoCombat::castPillars);
        character.CharacterCasting.registerEffect("jogo",character.jogo.JogoSkills.DOMAIN,character.jogo.JogoCombat::castDomain);
        character.CharacterCasting.registerEffect("jogo",character.jogo.JogoSkills.VOLCANO_BEAM,character.jogo.JogoCombat::castVolcanoBeam);
        character.CharacterCasting.registerEffect("jogo",character.jogo.JogoSkills.MAGMA_TRAP,character.jogo.JogoCombat::castMagmaTrap);
        character.CharacterCasting.registerEffect("jogo",character.jogo.JogoSkills.ASH_VEIL,character.jogo.JogoCombat::castAshVeil);
        character.CharacterCasting.allowWhileMindLocked("jogo",character.jogo.JogoSkills.DOMAIN);
        character.CharacterService.registerRanker("jogo",character.jogo.JogoCombat::rank);
        character.CharacterCasting.registerCooldownScale("jogo",pl->character.jogo.JogoCombat.inDomain(pl)?0.5:1.0);
        MinecraftForge.EVENT_BUS.register(character.jogo.JogoCombat.class);
        character.CharacterCasting.registerEffect("mahito",character.mahito.MahitoSkills.TRANSMUTE,
            (p,skill,rank)->character.mahito.MahitoTransfigure.arm(p));
        MinecraftForge.EVENT_BUS.register(character.mahito.MahitoTransfigure.class);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.living.LivingHurtEvent ev)->{if(ev.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp&&character.mahito.MahitoSelf.damageScale(sp)<1f)ev.setAmount(ev.getAmount()*character.mahito.MahitoSelf.damageScale(sp));});
        MinecraftForge.EVENT_BUS.register(character.mahito.MahitoPowers.class);
        MinecraftForge.EVENT_BUS.register(character.mahito.MahitoSelf.class);
        character.CharacterCasting.registerEffect("mahito",character.mahito.MahitoSkills.SELF_EMBRYO,
            (p,skill,rank)->character.domainking.SelfEmbryoDomain.toggle(p));
        character.domainking.DomainKingCombat.register();
        MinecraftForge.EVENT_BUS.register(character.domainking.DomainKingCombat.class);
        MinecraftForge.EVENT_BUS.register(character.domainking.SelfEmbryoDomain.class);
        MinecraftForge.EVENT_BUS.register(NpcCurse.class);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e)->{if(e.phase==TickEvent.Phase.END)NpcBridge.tick();});
        character.CharacterCasting.registerNpcProvider(JogoNpcEntity.class,(e,skill)->PlayerSkillBridge.cast(character.CharacterCasting.caster(e),"jogo",skill));
        character.CharacterCasting.registerNpcProvider(ChosoEntity.class,(e,skill)->PlayerSkillBridge.cast(character.CharacterCasting.caster(e),"choso",skill));
        character.CharacterCasting.registerNpcProvider(UraumeNpcEntity.class,(e,skill)->PlayerSkillBridge.cast(character.CharacterCasting.caster(e),"uraume",skill));
        MinecraftForge.EVENT_BUS.register(character.toji.TojiCombat.class);
        Network.registerDrawing();
        MinecraftForge.EVENT_BUS.register(SlashForms.class);
        MinecraftForge.EVENT_BUS.register(DismantleMotion.class);
        MinecraftForge.EVENT_BUS.register(MeleeCombo.class);
        MinecraftForge.EVENT_BUS.register(FreehandDrawing.class);
        MinecraftForge.EVENT_BUS.register(Skills.class);
        MinecraftForge.EVENT_BUS.register(NetBranches.class);
        MinecraftForge.EVENT_BUS.register(Curse.class);
        MinecraftForge.EVENT_BUS.register(CombatSettings.class);
        MinecraftForge.EVENT_BUS.register(CombatSafety.class);
        MinecraftForge.EVENT_BUS.register(BloodArts.class);
        MinecraftForge.EVENT_BUS.register(BloodDomain.class);
        MinecraftForge.EVENT_BUS.register(DeathStar.class);
        MinecraftForge.EVENT_BUS.register(GuardianCombat.class);
        MinecraftForge.EVENT_BUS.register(GuardianProgress.class);
        // 对战败北在本模组的伤害加成结算后判断，防止附加伤害穿过1血保护。
        MinecraftForge.EVENT_BUS.register(EternalLove.class);
        if(net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid"))MinecraftForge.EVENT_BUS.register(MaidDuel.class);
        // 专用服务器不加载任何客户端类；客户端关键事件改为从入口显式接入。
        net.minecraftforge.fml.DistExecutor.safeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->ClientBootstrap::install);
    }
}
