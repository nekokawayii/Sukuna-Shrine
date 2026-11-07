package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 死灭洄游专用羂索的渲染器：直接复用夏油杰皮肤 + 原版玩家骨架（r230）。 */
public final class CullingGetoRenderer extends MobRenderer<character.culling.CullingGetoEntity, PlayerModel<character.culling.CullingGetoEntity>> {
    private static final ResourceLocation SKIN = new ResourceLocation("jujutsu_kaisen_cursed_paths", "textures/entity/geto.png");

    public CullingGetoRenderer(EntityRendererProvider.Context context) { super(context, new Model(), 0.6f); }

    @Override public ResourceLocation getTextureLocation(character.culling.CullingGetoEntity entity) { return SKIN; }

    public static final class Model extends PlayerModel<character.culling.CullingGetoEntity> {
        Model() { super(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64).bakeRoot(), false); }
    }
}
