package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** 复用已有玩家骨架、皮肤和盔甲；神武解只是渲染层的铁剑，不改玩家背包。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class FinalePoseClient {
    private static FinaleRenderer normal,slim;private static boolean rendering;
    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){normal=new FinaleRenderer(e.getContext(),false);slim=new FinaleRenderer(e.getContext(),true);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void render(RenderPlayerEvent.Pre e){
        if(rendering||!FinaleClient.active(e.getEntity().getId())||!(e.getEntity() instanceof AbstractClientPlayer player)||MotionRenderCompat.externalPlayerModels())return;
        PlayerRenderer renderer="slim".equals(player.getModelName())?slim:normal;if(renderer==null)return;e.setCanceled(true);rendering=true;
        try{renderer.render(player,player.getYRot(),e.getPartialTick(),e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight());}finally{rendering=false;}
    }
    private static final class FinaleRenderer extends PlayerRenderer {
        FinaleRenderer(EntityRendererProvider.Context context,boolean slim){
            super(context,slim);model=new AnimatedPlayer(slim);
            layers.removeIf(layer->layer instanceof net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer||layer instanceof net.minecraft.client.renderer.entity.layers.ItemInHandLayer);
            addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,MotionPlayerGeometry.armor(.5f),MotionPlayerGeometry.armor(1),context.getModelManager()));
            addLayer(new Weapon(this,context.getItemInHandRenderer()));
        }
        @Override public Vec3 getRenderOffset(AbstractClientPlayer p,float partial){return Vec3.ZERO;}
    }
    private static final class AnimatedPlayer extends PlayerModel<AbstractClientPlayer> {
        AnimatedPlayer(boolean slim){super(MotionPlayerGeometry.player(slim),slim);}
        @Override public void copyPropertiesTo(net.minecraft.client.model.HumanoidModel<AbstractClientPlayer> target){super.copyPropertiesTo(target);MotionPlayerGeometry.knees(target,leftLeg.getChild("knee").xRot,rightLeg.getChild("knee").xRot);}
        @Override public void setupAnim(AbstractClientPlayer p,float limb,float amount,float age,float yaw,float pitch){
            float t=FinaleClient.age(p.getId()),partial=age-p.tickCount;t+=partial;
            super.setupAnim(p,t<FinaleRules.CHARGE?limb:0,t<FinaleRules.CHARGE?amount:0,age,yaw,pitch);crouching=false;body.xRot=body.yRot=body.zRot=0;body.y=0;
            leftArm.y=rightArm.y=2;leftArm.z=rightArm.z=0;leftArm.yRot=rightArm.yRot=0;leftArm.zRot=rightArm.zRot=0;
            MotionPlayerGeometry.knees(this,0,0);
            if(t<FinaleRules.TRIGGERS[0]){leftArm.xRot=rightArm.xRot=-2.5f;leftArm.zRot=-.25f;rightArm.zRot=.25f;}
            else if(t>=FinaleRules.TRIGGERS[1]&&t<FinaleRules.STARTS[2]){
                // 大腿抬起、膝盖收住，手臂按每段种类继续挥动。
                leftLeg.xRot=-1.25f;rightLeg.xRot=-.55f;leftLeg.yRot=rightLeg.yRot=0;leftLeg.zRot=-.15f;rightLeg.zRot=.15f;
                MotionPlayerGeometry.knees(this,1.5f,1.2f);body.xRot=.16f;
                float swing=(float)Math.sin((t-FinaleRules.TRIGGERS[1])*Math.PI/3);
                if(FinaleClient.variant(p.getId())==7){rightArm.xRot=-2.7f+(swing+1)*1.35f;leftArm.xRot=-.5f;rightArm.zRot=.2f;}
                else{rightArm.xRot=-1.3f-swing*.8f;leftArm.xRot=-1.2f+swing*.6f;rightArm.yRot=-swing*.55f;leftArm.yRot=swing*.55f;}
            }else if(t>=FinaleRules.STARTS[2]&&t<FinaleRules.TRIGGERS[3]){leftArm.xRot=rightArm.xRot=-1.7f;leftArm.zRot=-.6f;rightArm.zRot=.6f;leftLeg.xRot=-.4f;rightLeg.xRot=.3f;MotionPlayerGeometry.knees(this,.6f,.25f);}
            else if(t>=FinaleRules.STARTS[4]){head.xRot=head.yRot=0;leftArm.xRot=rightArm.xRot=-1.45f;leftArm.yRot=.65f;rightArm.yRot=-.65f;leftArm.zRot=.12f;rightArm.zRot=-.12f;leftLeg.xRot=rightLeg.xRot=0;}
            else{rightArm.xRot=-1.5f;leftArm.xRot=-.4f;}
            hat.copyFrom(head);jacket.copyFrom(body);leftSleeve.copyFrom(leftArm);rightSleeve.copyFrom(rightArm);leftPants.copyFrom(leftLeg);rightPants.copyFrom(rightLeg);
        }
    }
    private static final class Weapon extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        private final ItemInHandRenderer items;private final ItemStack sword=new ItemStack(Items.IRON_SWORD);
        Weapon(PlayerRenderer parent,ItemInHandRenderer items){super(parent);this.items=items;}
        @Override public void render(PoseStack ps,MultiBufferSource source,int light,AbstractClientPlayer p,float limb,float amount,float partial,float age,float yaw,float pitch){
            float t=FinaleClient.age(p.getId());if(t<FinaleRules.TRIGGERS[1]||t>=FinaleRules.STARTS[2]||FinaleClient.variant(p.getId())!=7)return;
            ps.pushPose();getParentModel().translateToHand(HumanoidArm.RIGHT,ps);ps.mulPose(Axis.XP.rotationDegrees(-90));ps.mulPose(Axis.YP.rotationDegrees(180));ps.translate(-.0625,.125,-.625);
            items.renderItem(p,sword,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,false,ps,source,light);ps.popPose();
        }
    }
    private FinalePoseClient(){}
}
