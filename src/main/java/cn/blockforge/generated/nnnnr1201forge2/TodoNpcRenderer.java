package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 东堂葵 NPC：玩家模型 + 默认 Steve 皮肤。
 * <p>用户还没给皮肤，等发图后改成 {@code textures/entity/todo_npc.png} 即可（只改这一行）。
 */
public final class TodoNpcRenderer extends MobRenderer<TodoNpcEntity, PlayerModel<TodoNpcEntity>> {
    private static final ResourceLocation SKIN =
        new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public TodoNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override public ResourceLocation getTextureLocation(TodoNpcEntity entity) { return SKIN; }
}
