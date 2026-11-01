package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** 原始自由格式模型不能用普通物品JSON表达，直接保留bbmodel的旋转与逐面UV。 */
public final class SoulDevourerClient {
    public static void item(java.util.function.Consumer<IClientItemExtensions> consumer){
        consumer.accept(new IClientItemExtensions(){
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null){var mc=Minecraft.getInstance();renderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){
                    private final BbModel model=new BbModel("soul_devourer","soul_devourer");
                    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                        pose.pushPose();pose.translate(.5,.5,.5);
                        float scale=context==ItemDisplayContext.GUI?.32f:context==ItemDisplayContext.GROUND?.28f:.52f;
                        pose.scale(scale,scale,scale);
                        // 上传文件没有display；刀尖原本向下，先翻正再按展示场景摆放。
                        pose.mulPose(Axis.ZP.rotationDegrees(context==ItemDisplayContext.GUI?140:180));
                        if(context==ItemDisplayContext.GUI)pose.mulPose(Axis.YP.rotationDegrees(15));
                        model.render(pose,buffers,light,"",0,1,1,1,1);pose.popPose();
                    }
                };}return renderer;
            }
        });
    }
    private SoulDevourerClient(){}
}
