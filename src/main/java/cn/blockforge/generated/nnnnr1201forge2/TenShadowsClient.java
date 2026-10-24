package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** 事件由 ClientBootstrap 显式注册。 */
public final class TenShadowsClient {
    public static final ResourceLocation SKIN=new ResourceLocation("sukuna","textures/entity/meguna.png");
    private record State(boolean incarnated,boolean tamed,boolean partial,long end,long manifestEnd){}
    private static final Map<UUID,State> states=new HashMap<>();
    private static long tick;private static VesselRenderer renderer;private static PlayerModel<AbstractClientPlayer> arms;private static boolean rendering;
    public static void receive(TenShadows.State s){states.put(s.player(),new State(s.incarnated(),s.tamed(),s.partial(),tick+s.remaining(),tick+s.manifest()));}
    public static boolean has(UUID id){State s=states.get(id);return s!=null&&s.incarnated;}
    public static boolean has(){var p=Minecraft.getInstance().player;return p!=null&&has(p.getUUID());}
    public static boolean tamed(){var p=Minecraft.getInstance().player;State s=p==null?null:states.get(p.getUUID());return s!=null&&s.tamed;}
    public static boolean partial(UUID id){State s=states.get(id);return s!=null&&s.partial;}
    public static boolean partial(){var p=Minecraft.getInstance().player;return p!=null&&partial(p.getUUID());}
    public static boolean chanting(UUID id){State s=states.get(id);return s!=null&&s.end>tick;}
    public static boolean locked(){var p=Minecraft.getInstance().player;return p!=null&&chanting(p.getUUID());}
    /** 显现冷却剩余秒数（向上取整，0=可用）。界面用它置灰按钮，服务端仍做最终判定。 */
    public static int manifestCooldown(){
        var p=Minecraft.getInstance().player;State s=p==null?null:states.get(p.getUUID());
        if(s==null)return 0;
        long left=s.manifestEnd()-tick;
        return left<=0?0:(int)((left+19)/20);
    }
    public static ResourceLocation skin(AbstractClientPlayer p){return has(p.getUUID())?SKIN:p.getSkinTextureLocation();}
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){renderer=new VesselRenderer(e.getContext());arms=new PlayerModel<>(e.getContext().bakeLayer(ModelLayers.PLAYER),false);}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level!=null&&!mc.isPaused())tick++;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){states.clear();tick=0;}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void render(RenderPlayerEvent.Pre e){
        if(rendering||renderer==null||!(e.getEntity() instanceof AbstractClientPlayer p)||!has(p.getUUID()))return;
        if((DismantleMotionClient.posing(p.getId())||FinaleClient.active(p.getId()))&&!chanting(p.getUUID()))return;
        e.setCanceled(true);rendering=true;try{renderer.render(p,p.getYRot(),e.getPartialTick(),e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight());}finally{rendering=false;}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void input(MovementInputUpdateEvent e){if(!locked())return;var input=e.getInput();input.forwardImpulse=input.leftImpulse=0;input.up=input.down=input.left=input.right=input.jumping=false;input.shiftKeyDown=true;e.getEntity().setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered e){if(locked()){e.setCanceled(true);e.setSwingHand(false);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hand(RenderHandEvent e){
        if(!locked()||arms==null)return;e.setCanceled(true);if(e.getHand()!=InteractionHand.MAIN_HAND)return;
        PoseStack ps=e.getPoseStack();var b=e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(SKIN));
        for(int side:new int[]{-1,1}){
            var arm=side<0?arms.leftArm:arms.rightArm;var sleeve=side<0?arms.leftSleeve:arms.rightSleeve;
            arm.xRot=arm.yRot=arm.zRot=0;arm.x=arm.y=arm.z=0;sleeve.copyFrom(arm);
            ps.pushPose();ps.translate(side*.1,side<0?-.52:-.25,-.62);ps.mulPose(Axis.ZP.rotationDegrees(side<0?80:-80));ps.mulPose(Axis.XP.rotationDegrees(-25));
            arm.render(ps,b,e.getPackedLight(),OverlayTexture.NO_OVERLAY);sleeve.render(ps,b,e.getPackedLight(),OverlayTexture.NO_OVERLAY);ps.popPose();
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void arm(RenderArmEvent e){
        if(!has(e.getPlayer().getUUID())||arms==null)return;e.setCanceled(true);
        boolean left=e.getArm()==net.minecraft.world.entity.HumanoidArm.LEFT;
        arms.setupAnim(e.getPlayer(),0,0,0,0,0);arms.attackTime=0;arms.crouching=false;
        var part=left?arms.leftArm:arms.rightArm;var sleeve=left?arms.leftSleeve:arms.rightSleeve;
        part.x=left?5:-5;part.y=2;part.z=0;part.xRot=part.yRot=part.zRot=0;sleeve.copyFrom(part);
        var b=e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(SKIN));part.render(e.getPoseStack(),b,e.getPackedLight(),OverlayTexture.NO_OVERLAY);sleeve.render(e.getPoseStack(),b,e.getPackedLight(),OverlayTexture.NO_OVERLAY);
    }
    private static final class VesselModel extends PlayerModel<AbstractClientPlayer>{
        VesselModel(){super(MotionPlayerGeometry.player(false),false);}
        @Override public void copyPropertiesTo(net.minecraft.client.model.HumanoidModel<AbstractClientPlayer> target){super.copyPropertiesTo(target);MotionPlayerGeometry.knees(target,leftLeg.getChild("knee").xRot,rightLeg.getChild("knee").xRot);}
        @Override public void setupAnim(AbstractClientPlayer p,float limb,float amount,float age,float yaw,float pitch){
            super.setupAnim(p,limb,amount,age,yaw,pitch);MotionPlayerGeometry.knees(this,0,0);
            if(!chanting(p.getUUID()))return;crouching=false;leftArmPose=rightArmPose=net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
            body.xRot=.18f;leftLeg.xRot=-1.35f;rightLeg.xRot=.35f;MotionPlayerGeometry.knees(this,1.5f,1.8f);
            rightArm.xRot=-1.4f;leftArm.xRot=-1.1f;rightArm.yRot=-.8f;leftArm.yRot=.8f;rightArm.zRot=.2f;leftArm.zRot=-.2f;
            rightArm.y=-1;leftArm.y=3;head.xRot=.25f;
            hat.copyFrom(head);jacket.copyFrom(body);leftSleeve.copyFrom(leftArm);rightSleeve.copyFrom(rightArm);leftPants.copyFrom(leftLeg);rightPants.copyFrom(rightLeg);
        }
    }
    private static final class VesselRenderer extends net.minecraft.client.renderer.entity.player.PlayerRenderer{
        VesselRenderer(EntityRendererProvider.Context c){super(c,false);model=new VesselModel();
            layers.removeIf(layer->layer instanceof net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer);
            addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,MotionPlayerGeometry.armor(.5f),MotionPlayerGeometry.armor(1),c.getModelManager()));
        }
        @Override public ResourceLocation getTextureLocation(AbstractClientPlayer p){return SKIN;}
        @Override protected void setupRotations(AbstractClientPlayer p,PoseStack ps,float age,float yaw,float partial){super.setupRotations(p,ps,age,yaw,partial);if(chanting(p.getUUID()))ps.translate(0,-.55,0);}
    }
    private static final class MegumiModel extends PlayerModel<MegumiEntity>{
        MegumiModel(EntityRendererProvider.Context c){super(c.bakeLayer(ModelLayers.PLAYER),false);}
        @Override public void setupAnim(MegumiEntity e,float limb,float amount,float age,float yaw,float pitch){
            super.setupAnim(e,limb,amount,age,yaw,pitch);
            if(e.chanting()){body.xRot=.16f;head.xRot=.2f;leftArm.xRot=-1.15f;rightArm.xRot=-1.4f;leftArm.yRot=.65f;rightArm.yRot=-.65f;leftArm.zRot=-.18f;rightArm.zRot=.18f;}
            if(e.downed()){head.xRot=body.xRot=leftArm.xRot=rightArm.xRot=leftLeg.xRot=rightLeg.xRot=0;leftArm.zRot=-.12f;rightArm.zRot=.12f;}
            hat.copyFrom(head);jacket.copyFrom(body);leftSleeve.copyFrom(leftArm);rightSleeve.copyFrom(rightArm);leftPants.copyFrom(leftLeg);rightPants.copyFrom(rightLeg);
        }
    }
    static final class MegumiRenderer extends MobRenderer<MegumiEntity,PlayerModel<MegumiEntity>>{
        private static final ResourceLocation SKIN=new ResourceLocation("sukuna","textures/entity/megumi.png");
        MegumiRenderer(EntityRendererProvider.Context c){super(c,new MegumiModel(c),.3f);}
        @Override public ResourceLocation getTextureLocation(MegumiEntity e){return SKIN;}
        @Override protected void setupRotations(MegumiEntity e,PoseStack ps,float age,float yaw,float partial){if(e.downed()){ps.mulPose(Axis.YP.rotationDegrees(180-yaw));ps.translate(0,.2,-.8);ps.mulPose(Axis.XP.rotationDegrees(90));}else super.setupRotations(e,ps,age,yaw,partial);}
    }
    private TenShadowsClient(){}
}
