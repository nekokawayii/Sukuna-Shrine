package character.mahito;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 分身 / 尾巴渲染：玩家模型（默认 Steve 皮肤）。
 * <p>尾巴（{@code tail} 标记）**不画模型**，只保留实体做伤害结算。</p>
 */
public final class MahitoCloneRenderer extends MobRenderer<MahitoCloneEntity, PlayerModel<MahitoCloneEntity>> {
    private static final ResourceLocation SKIN = new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public MahitoCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(MahitoCloneEntity entity) { return SKIN; }

    @Override public void render(MahitoCloneEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.isTail()) return;   // 尾巴无模型，只结算伤害
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    @Override public boolean shouldRender(MahitoCloneEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                         double x, double y, double z) {
        if (entity.isTail()) return false;
        return super.shouldRender(entity, frustum, x, y, z);
    }
}
