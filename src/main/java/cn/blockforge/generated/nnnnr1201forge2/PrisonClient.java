package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** 直接绘制用户提供的两份原始bbmodel与关键帧，不替换造型。 */
public final class PrisonClient {
    private static ShadowModel realm,light;
    private static ShadowModel realm(){if(realm==null)realm=new ShadowModel("sealing","prison_realm");return realm;}
    private static ShadowModel light(){if(light==null)light=new ShadowModel("sealing","jacobs_ladder");return light;}
    public static void reset(){realm=null;light=null;}
    public static final class RealmRenderer extends EntityRenderer<PrisonRealmEntity> {
        public RealmRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.6f;reset();}
        @Override public ResourceLocation getTextureLocation(PrisonRealmEntity e){return new ResourceLocation("sukuna","textures/entity/sealing/prison_realm_0.png");}
        @Override public void render(PrisonRealmEntity e,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int packed){
            ps.pushPose();ps.mulPose(Axis.YP.rotationDegrees(180-e.getYRot()));ps.translate(0,-.125,0);
            float frame=Math.max(0,Math.min(PrisonRules.OPEN_TICKS,e.frame()+(e.open()?partial:-2*partial)));
            boolean idle=e.open()&&e.frame()>=PrisonRules.OPEN_TICKS;
            realm().render(ps,buffers,packed,idle?"巨眼待机_展开形态":"变身_方块展开为巨眼",idle?(e.tickCount+partial)/20:frame/20,1);ps.popPose();super.render(e,yaw,partial,ps,buffers,packed);
        }
    }
    public static final class LightRenderer extends EntityRenderer<JacobsLadderEntity> {
        public LightRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=0;}
        @Override public ResourceLocation getTextureLocation(JacobsLadderEntity e){return new ResourceLocation("sukuna","textures/entity/sealing/jacobs_ladder_0.png");}
        @Override public void render(JacobsLadderEntity e,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int packed){
            ps.pushPose();float scale=10/Math.max(1,light().width());ps.scale(scale,16/Math.max(1,light().height()),scale);
            light().render(ps,buffers,LightTexture.FULL_BRIGHT,"holy_aurora",Math.min(6,(e.age()+partial)/20),1);ps.popPose();
        }
    }
    public static void item(java.util.function.Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){
        private BlockEntityWithoutLevelRenderer renderer;
        @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
            if(renderer==null){var mc=Minecraft.getInstance();renderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){
                @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack ps,MultiBufferSource buffers,int lightValue,int overlay){
                    boolean prison=stack.is(PrisonContent.PRISON.get());ShadowModel model=prison?realm():light();ps.pushPose();
                    ps.translate(.5,.15,.5);float scale=prison?.55f:.8f/Math.max(model.width(),model.height());ps.scale(scale,scale,scale);
                    ps.mulPose(Axis.YP.rotationDegrees(prison?145:30));if(!prison)ps.translate(0,-model.minY(),0);
                    model.render(ps,buffers,prison?lightValue:LightTexture.FULL_BRIGHT,prison?"变身_方块展开为巨眼":"holy_aurora",prison?0:3.3f,1);ps.popPose();
                }
            };}return renderer;
        }
    });}
    private PrisonClient(){}
}
