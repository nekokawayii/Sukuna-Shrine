package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 天逆蛑·腰间挂件（第三人称/别人看得到）。
 *
 * <p>显示条件：玩家主背包或副手任意一格有 天逆蛑（服务端通过 {@link Network.PendantState} 同步），
 * 或者自己本地背包里确实有。拿在手上时不再重复画腰间那一份。</p>
 *
 * <p>不依赖任何前置模组：用原版 {@code ItemRenderer} 直接画物品模型（模型来自用户上传的
 * Blockbench 文件），再叠一个固定的腰间变换。</p>
 */
public final class TenkaPendantClient {
    // ------------------------------------------------------------ 微调区（游戏里看不顺眼就改这几个数）
    /** 左右：正数 = 角色左腰，单位格。 */
    public static double X = 0.30;
    /** 上下：0.75 ≈ 腰带高度（渲染坐标原点在头顶上方约 1.5 格，+y 向下）。 */
    public static double Y = 0.75;
    /** 前后：正数 = 稍微往身后，单位格。 */
    public static double Z = 0.16;
    /** 绕竖轴转（度）。 */
    public static float YAW = 0f;
    /** 前后倾（度）。 */
    public static float PITCH = 0f;
    /** 侧倾（度）：让长矛斜着挂在腰侧。 */
    public static float ROLL = 28f;
    /** 整体缩放：0.35 ≈ 0.65 格长的挂件。 */
    public static float SCALE = 0.35f;

    private static final Map<Integer, Boolean> VISIBLE = new HashMap<>();
    private static ItemStack renderStack = ItemStack.EMPTY;

    public static void receive(Network.PendantState state) {
        if (state.visible()) VISIBLE.put(state.entityId(), Boolean.TRUE);
        else VISIBLE.remove(state.entityId());
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        VISIBLE.clear();
    }

    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new Pendant(renderer));
        }
    }

    private static ItemStack stack() {
        if (renderStack.isEmpty()) renderStack = new ItemStack(Content.TENKA_SPEAR.get());
        return renderStack;
    }

    private static boolean localHas(AbstractClientPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Content.TENKA_SPEAR.get())) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(Content.TENKA_SPEAR.get())) return true;
        }
        return false;
    }

    private static final class Pendant extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        Pendant(PlayerRenderer parent) { super(parent); }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                     float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                     float netHeadYaw, float headPitch) {
            if (player.isSpectator()) return;
            // 拿在手上时由手部正常渲染，腰间不再重复画一份。
            if (player.getMainHandItem().is(Content.TENKA_SPEAR.get())
                || player.getOffhandItem().is(Content.TENKA_SPEAR.get())) return;
            boolean visible = VISIBLE.containsKey(player.getId());
            if (!visible && player == Minecraft.getInstance().player) visible = localHas(player);
            if (!visible) return;

            pose.pushPose();
            pose.translate(X, Y, Z);
            pose.mulPose(Axis.YP.rotationDegrees(YAW));
            pose.mulPose(Axis.ZP.rotationDegrees(ROLL));
            pose.mulPose(Axis.XP.rotationDegrees(PITCH));
            pose.scale(SCALE, SCALE, SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack(), ItemDisplayContext.NONE,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, player.level(), player.getId());
            pose.popPose();
        }
    }

    private TenkaPendantClient() { }
}
