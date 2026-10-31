package character.mahito;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * 肢体：在玩家模型上额外画出多条手臂（第三人称/他人可见）。
 * <p>手臂数量来自服务端 Effect(305)（entityId → 数量）。每条手臂复用玩家模型的右臂几何，
 * 沿肩部左右交替错开、略微前倾，看起来像从身体侧后方长出来的多余手臂。</p>
 * <p>说明：目前只做“模型渲染”，额外手臂的独立持物/使用与第一人称手部还没有接入。</p>
 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MahitoArmsClient {
    private static final Map<Integer, Integer> ARMS = new HashMap<>();

    public static void set(int entityId, int count) { ARMS.put(entityId, Math.min(4, Math.max(0, count))); }
    public static void clear() { ARMS.clear(); }

    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new Arms(renderer));
        }
    }

    private static final class Arms extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        Arms(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

        @Override public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     float limbSwing, float limbSwingAmount, float partial, float age,
                                     float netHeadYaw, float headPitch) {
            int arms = ARMS.getOrDefault(player.getId(), 0);
            if (arms <= 0) return;
            PlayerModel<AbstractClientPlayer> model = getParentModel();
            var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(player.getSkinTextureLocation()));
            for (int i = 0; i < arms; i++) {
                boolean right = (i % 2) == 0;
                int pair = i / 2 + 1;
                ps.pushPose();
                (right ? model.rightArm : model.leftArm).translateAndRotate(ps);
                ps.translate(right ? 0.22 * pair : -0.22 * pair, 0.18 * pair, 0.06 * pair);
                ps.mulPose(Axis.ZP.rotationDegrees(right ? 22f * pair : -22f * pair));
                ps.mulPose(Axis.XP.rotationDegrees(-12f));
                (right ? model.rightArm : model.leftArm).render(ps, vc, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
                ps.popPose();
            }
        }
    }

    private MahitoArmsClient() {}
}
