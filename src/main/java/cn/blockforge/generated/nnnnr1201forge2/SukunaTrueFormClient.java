package cn.blockforge.generated.nnnnr1201forge2;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 两面宿傩「原身形态」的皮肤渲染（r268）。
 *
 * <p>服务端用 {@code Network.Effect(640)} 广播"谁已进入原身"；客户端替换该玩家的
 * {@link PlayerRenderer}，用 {@code textures/entity/sukuna_true_form.png}（64×64）。
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class SukunaTrueFormClient {
    public static final ResourceLocation SKIN =
        new ResourceLocation("sukuna", "textures/entity/sukuna_true_form.png");
    private static final Set<UUID> HAS = new HashSet<>();
    private static PlayerRenderer renderer;
    private static boolean rendering;

    public static void set(Network.Effect e) {
        if (e.size() > 0) HAS.add(new UUID(0, e.owner()));
        else HAS.remove(new UUID(0, e.owner()));
    }

    public static boolean has(UUID id) { return HAS.contains(id); }

    public static final class Registration {
        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers e) {
            renderer = new TrueFormRenderer(e.getContext());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void render(RenderPlayerEvent.Pre e) {
        if (rendering || renderer == null || !(e.getEntity() instanceof AbstractClientPlayer p)) return;
        if (!has(p.getUUID())) return;
        // 与十影/动作系统共用玩家渲染时让它们优先
        if (DismantleMotionClient.posing(p.getId()) || FinaleClient.active(p.getId())) return;
        if (TenShadowsClient.chanting(p.getUUID())) return;
        e.setCanceled(true);
        rendering = true;
        try {
            renderer.render(p, p.getYRot(), e.getPartialTick(), e.getPoseStack(),
                e.getMultiBufferSource(), e.getPackedLight());
        } finally {
            rendering = false;
        }
    }

    private static final class TrueFormRenderer extends PlayerRenderer {
        TrueFormRenderer(EntityRendererProvider.Context c) {
            super(c, false);
        }

        @Override
        public ResourceLocation getTextureLocation(AbstractClientPlayer p) { return SKIN; }
    }

    private SukunaTrueFormClient() { }
}
