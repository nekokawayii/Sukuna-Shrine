package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 低级咒灵渲染器（临时方案）：原版僵尸骨架 + 僵尸贴图。
 * <p>注意：{@code ZombieModel} 的泛型限定是 {@code T extends Zombie}，我们的实体不是僵尸，
 * 所以这里用同样 64×64 布局的 {@link HumanoidModel} 套僵尸的骨架层与贴图，外观就是僵尸。
 * <p>原来的 {@code basement_wanderer.bbmodel} 体量过大（592 元素 / 9 张内嵌贴图），先换掉跑通玩法。
 */
public final class LowCurseRenderer extends MobRenderer<LowCurseEntity, HumanoidModel<LowCurseEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/zombie/zombie.png");

    public LowCurseRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(LowCurseEntity entity) { return TEXTURE; }
}
