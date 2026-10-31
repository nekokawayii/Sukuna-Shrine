package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/** 单一的完整实体目录：正常注册与运行时校验共用，不从已经触发过的事件收集目录。 */
public final class EntityRendererRegistry {
    private static final Logger LOGGER=LogUtils.getLogger();
    private record Entry<T extends Entity>(Supplier<EntityType<T>> type,Class<?> rendererClass,
                                           EntityRendererProvider<T> provider) {
        void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(type.get(),provider);
        }
        boolean matches(Map<EntityType<?>,EntityRenderer<?>> live) {
            return rendererClass.isInstance(live.get(type.get()));
        }
        void require(Map<EntityType<?>,RendererRepair.Factory<EntityRenderer<?>>> target,
                     Supplier<EntityRendererProvider.Context> context) {
            target.put(type.get(),new RendererRepair.Factory<>(rendererClass,()->provider.create(context.get())));
        }
    }
    private static <T extends Entity> Entry<T> entry(Supplier<EntityType<T>> type,Class<?> rendererClass,
                                                     EntityRendererProvider<T> provider) {
        return new Entry<>(type,rendererClass,provider);
    }
    // RegistryObject 仅在注册/检查时取值，不能在模组构造阶段提前解析。
    private static final java.util.List<Entry<?>> ENTRIES=java.util.List.of(
        entry(VoidContent.GOJO,GojoRenderer.class,GojoRenderer::new),
        entry(Content.CHOSO,ChosoRenderer.class,ChosoRenderer::new),
        entry(Content.URAUME_NPC,UraumeNpcRenderer.class,UraumeNpcRenderer::new),
        entry(Content.JOGO_NPC,JogoNpcRenderer.class,JogoNpcRenderer::new),
        entry(Content.YIDUI,YiduiShouRenderer.class,YiduiShouRenderer::new),
        entry(Content.MAHITO_CLONE,character.mahito.MahitoCloneRenderer.class,character.mahito.MahitoCloneRenderer::new),
        entry(VoidContent.MAHORAGA,MahoragaRenderer.class,MahoragaRenderer::new),
        entry(SoulContent.YUJI,YujiRenderer.class,YujiRenderer::new),
        entry(SoulContent.GUARDIAN,GuardianRenderer.class,GuardianRenderer::new),
        entry(SoulContent.MEGUMI,TenShadowsClient.MegumiRenderer.class,TenShadowsClient.MegumiRenderer::new),
        entry(ShadowContent.SHIKIGAMI,ShadowRenderer.class,ShadowRenderer::new),
        entry(PrisonContent.REALM,PrisonClient.RealmRenderer.class,PrisonClient.RealmRenderer::new),
        entry(PrisonContent.LIGHT,PrisonClient.LightRenderer.class,PrisonClient.LightRenderer::new),
        entry(Content.OBSERVER,ObserverRenderer.class,ObserverRenderer::new),
        entry(Content.DAITIAN,DaitianRenderer.class,DaitianRenderer::new),
        entry(Content.BROKEN_MAHITO,BrokenMahitoRenderer.class,BrokenMahitoRenderer::new),
        // 真人的逻辑肢体本来就由主体绘制；仅这一种逻辑实体故意不另画模型。
        entry(Content.MAHITO_LIMB,NoopRenderer.class,NoopRenderer::new),
        entry(Content.BUDDHA,BuddhaRenderer.class,BuddhaRenderer::new),
        entry(Content.PARASITE,BuddhaRenderer.class,BuddhaRenderer::new),
        entry(Content.MOTHER,BuddhaRenderer.class,BuddhaRenderer::new),
        entry(Content.GETO,GetoRenderer.class,GetoRenderer::new),
        entry(Content.LOW_CURSE,LowCurseRenderer.class,LowCurseRenderer::new),
        entry(Content.RIKA,RikaRenderer.class,RikaRenderer::new)
    );
    private static boolean ready,repairing;
    private static final Set<ResourceLocation> reportedMissing=new HashSet<>();
    private static final Set<ResourceLocation> recoveryTried=new HashSet<>();
    private static boolean reportedPlayer,playerRecoveryTried;

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Content.YUTA_GRAVE_BE.get(), character.yuta.YutaGraveRenderer::new);
        for(var entry:ENTRIES)entry.register(event);
        Set<EntityType<?>> covered=new HashSet<>();
        for(var entry:ENTRIES) {
            if(!covered.add(entry.type().get()))throw new IllegalStateException("重复的实体渲染器注册");
        }
        for(var type:Content.ENTITIES.getEntries()) {
            if(!covered.contains(type.get()))throw new IllegalStateException("实体缺少渲染器注册："+type.getId());
        }
        LOGGER.info("[宿傩渲染 r125] 已注册全部 {} 种实体的渲染器。",covered.size());
    }

    public static void afterReload(EntityRenderersEvent.AddLayers event) {
        ready=true;
        ensureOwn(Minecraft.getInstance());
        LOGGER.info("[宿傩渲染 r125] 资源重载后的实体渲染器检查通过。");
    }

    static EntityRendererProvider.Context context(Minecraft mc) {
        var dispatcher=mc.getEntityRenderDispatcher();
        return new EntityRendererProvider.Context(dispatcher,mc.getItemRenderer(),mc.getBlockRenderer(),
            dispatcher.getItemInHandRenderer(),mc.getResourceManager(),mc.getEntityModels(),mc.font);
    }

    public static void ensureOwn(Minecraft mc) {
        if(!ready||repairing||mc==null)return;
        repairing=true;
        try {
            var dispatcher=mc.getEntityRenderDispatcher();
            boolean complete=true;
            for(var entry:ENTRIES)if(!entry.matches(dispatcher.renderers)){complete=false;break;}
            if(complete)return; // 正常每帧只查 12 个映射，不分配对象也不重建模型。
            EntityRendererProvider.Context[] cachedContext={null};
            Supplier<EntityRendererProvider.Context> lazyContext=()-> {
                if(cachedContext[0]==null)cachedContext[0]=context(mc);
                return cachedContext[0];
            };
            Map<EntityType<?>,RendererRepair.Factory<EntityRenderer<?>>> required=new LinkedHashMap<>();
            for(var entry:ENTRIES)entry.require(required,lazyContext);
            var result=RendererRepair.repair(dispatcher.renderers,required);
            dispatcher.renderers=result.renderers();
            for(var type:result.repaired())LOGGER.warn("[宿傩渲染 r125] 已恢复实体的真实渲染器：{}",ForgeRegistries.ENTITY_TYPES.getKey(type));
        } catch(RuntimeException | LinkageError failure) {
            // 保留完整原因，不吞掉初始化错误再让原版在无名称的 null 处崩溃。
            LOGGER.error("[宿傩渲染 r125] 实体渲染器恢复失败。",failure);
            throw failure;
        } finally {
            repairing=false;
        }
    }

    public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        resetDiagnostics();
        var mc=Minecraft.getInstance();
        ensureOwn(mc);
        if(mc.player!=null)verifyEntity(mc,mc.player);
        LOGGER.info("[宿傩渲染 r125] 进入世界前校验：就绪={}，本模组实体={}，渲染表总数={}。",
            ready,ENTRIES.size(),mc.getEntityRenderDispatcher().renderers.size());
    }
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {resetDiagnostics();}
    private static void resetDiagnostics() {
        reportedMissing.clear();recoveryTried.clear();reportedPlayer=false;playerRecoveryTried=false;
    }
    public static void entityJoined(EntityJoinLevelEvent event) {
        if(!ready||!event.getLevel().isClientSide())return;
        var mc=Minecraft.getInstance();
        ensureOwn(mc);
        verifyEntity(mc,event.getEntity());
    }
    public static void beforeFrame(TickEvent.RenderTickEvent event) {
        if(event.phase==TickEvent.Phase.START) {
            var mc=Minecraft.getInstance();
            if(mc.level!=null)ensureOwn(mc);
        }
    }
    public static void beforeEntities(RenderLevelStageEvent event) {
        // 此阶段位于原版遍历实体、调用 shouldRender 之前，而非 AFTER_ENTITIES。
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS||!ready)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null)return;
        ensureOwn(mc);
        for(Entity entity:mc.level.entitiesForRendering())verifyEntity(mc,entity);
    }
    private static void verifyEntity(Minecraft mc,Entity entity) {
        var dispatcher=mc.getEntityRenderDispatcher();
        if(dispatcher.getRenderer(entity)!=null)return;
        ResourceLocation id=ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        boolean player=entity instanceof net.minecraft.client.player.AbstractClientPlayer;
        boolean retry=player?!playerRecoveryTried:recoveryTried.add(id);
        if(player)playerRecoveryTried=true;
        // 外部实体/玩家仅尝试按正常 provider 恢复一次，不给它们安装本模组的错误模型。
        if(retry) {
            LOGGER.warn("[宿傩渲染 r125] 检测到缺失渲染器，尝试按注册目录恢复：类型={}，实体类={}，编号={}。",
                id,entity.getClass().getName(),entity.getId());
            dispatcher.onResourceManagerReload(mc.getResourceManager());
            ensureOwn(mc);
            if(dispatcher.getRenderer(entity)!=null) {
                LOGGER.info("[宿傩渲染 r125] 缺失渲染器已恢复：{}。",id);
                return;
            }
        }
        if(player?!reportedPlayer:reportedMissing.add(id)) {
            if(player)reportedPlayer=true;
            LOGGER.error("[宿傩渲染 r125] 注册目录仍缺少渲染器：类型={}，实体类={}，编号={}，维度={}；请同时提供 crash-reports 中的报告。",
                id,entity.getClass().getName(),entity.getId(),entity.level().dimension().location());
        }
    }
    private EntityRendererRegistry() {}
}
