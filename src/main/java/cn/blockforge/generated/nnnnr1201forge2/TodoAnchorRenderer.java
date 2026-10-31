package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 特效锚点：什么都不画（咒力环由 TodoVisuals 负责），只需要一个合法的渲染器。 */
public final class TodoAnchorRenderer extends EntityRenderer<TodoAnchorEntity> {
    private static final ResourceLocation DUMMY =
        new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public TodoAnchorRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public void render(TodoAnchorEntity entity, float yaw, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light) { }

    @Override public ResourceLocation getTextureLocation(TodoAnchorEntity entity) { return DUMMY; }
}
