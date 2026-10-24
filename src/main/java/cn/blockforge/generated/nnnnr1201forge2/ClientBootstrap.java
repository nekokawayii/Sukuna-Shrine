package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** 由模组构造器在客户端显式调用，不依赖自动扫描来接入关键渲染事件。 */
public final class ClientBootstrap {
    private static boolean installed;

    public static void install() {
        if(installed)return;
        installed=true;
        character.CharacterClient.install();
        var modBus=FMLJavaModLoadingContext.get().getModEventBus();
        modBus.register(Client.Registration.class);
        modBus.register(TenShadowsClient.Registration.class);
        modBus.addListener(DismantleMotionClient.Registration::layers);
        modBus.addListener(FinalePoseClient.Registration::layers);
        modBus.addListener(MeleeComboClient.Registration::layers);
        modBus.register(GojoOpticsClient.Registration.class);
        modBus.register(BloodClient.Registration.class);
        modBus.addListener(BloodRealmRenderer::shaders);
        Network.motionReceiver=DismantleMotionClient::receive;
        MeleeCombo.receiver=MeleeComboClient::receive;
        modBus.addListener(EntityRendererRegistry::register);
        modBus.addListener(EventPriority.LOWEST,EntityRendererRegistry::afterReload);

        var gameBus=MinecraftForge.EVENT_BUS;
        gameBus.register(Client.class);
        gameBus.register(TenShadowsClient.class);
        gameBus.register(ShadowClient.class);
        modBus.register(ShadowClient.Registration.class);
        ShadowSystem.receiver=ShadowClient::receive;
        ShadowSystem.fxReceiver=ShadowClient::effect;
        gameBus.register(DismantleMotionClient.class);
        gameBus.register(FinaleClient.class);
        gameBus.register(FinalePoseClient.class);
        gameBus.register(CastHands.class);
        gameBus.register(ClientInput.class);
        gameBus.register(MeleeComboClient.class);
        gameBus.register(GojoOpticsClient.class);
        gameBus.register(BloodClient.class);
        gameBus.register(DeathStarClient.class);
        gameBus.register(BloodRealmRenderer.class);
        gameBus.addListener(EntityRendererRegistry::login);
        gameBus.addListener(EntityRendererRegistry::logout);
        gameBus.addListener(EventPriority.LOWEST,EntityRendererRegistry::entityJoined);
        gameBus.addListener(EventPriority.LOWEST,EntityRendererRegistry::beforeFrame);
        gameBus.addListener(EventPriority.LOWEST,EntityRendererRegistry::beforeEntities);
        LogUtils.getLogger().info("[宿傩渲染 r127] 实体、解动作、终式、结印与八段体术已从模组入口接入。");
    }
    private ClientBootstrap() {}
}
