package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** 仅动作期间借用第三人称；无论正常收招、死亡还是断线都会归还原视角。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class DismantleMotionClient {
    private static final class State {
        Network.MotionState packet;double age;long received;
        State(Network.MotionState packet,long now){this.packet=packet;age=packet.age();received=now;}
    }
    private static final Map<Integer,State> states=new HashMap<>();
    private static CameraType savedCamera;private static UUID cameraPlayer;private static Object world;
    private static long clock;private static boolean rendering;
    private static MotionRenderer normal,slim;
    public static boolean active(int id){return states.containsKey(id);}
    /** true = 用内置关节姿态（没装 PlayerAnimator 时的兜底）；false = 交给 PlayerAnimator 播动画。 */
    public static boolean useBuiltinPose(){
        try{return !net.minecraftforge.fml.ModList.get().isLoaded("playeranimator");}
        catch(Throwable t){return true;}
    }
    private static final boolean useBuiltinPose=useBuiltinPose();
    /** 兜底只提示一次，避免刷屏。 */
    private static boolean fallbackNoted;
    /** 正在维持「简易领域 · 捌」的实体：**每刻有细小斩击包到达就在册**，断包即过期（所以松手自动放下手）。 */
    private static final java.util.Map<Integer,Long> simpleDomain=new java.util.HashMap<>();
    public static boolean simpleDomain(int id){Long t=simpleDomain.get(id);return t!=null&&clock-t<6;}
    public static void simpleDomainState(int id){simpleDomain.put(id,clock);}
    public static void simpleDomainOff(int id){simpleDomain.remove(id);}
    public static boolean locked(){var mc=Minecraft.getInstance();var p=mc.player;return p!=null&&p.isAlive()&&world==mc.level&&p.getUUID().equals(cameraPlayer)&&active(p.getId());}
    public static int inputKind(){var mc=Minecraft.getInstance();return DismantleMotionRules.trigger(mc.options.keyShift.isDown(),mc.options.keyUp.isDown(),mc.options.keyDown.isDown());}
    public static void receive(Network.MotionState packet){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        if(world!=mc.level){clear();world=mc.level;}
        State state=states.get(packet.owner());
        if(packet.end()){
            if(state!=null&&state.packet.token().equals(packet.token()))states.remove(packet.owner());
            if(packet.owner()==mc.player.getId()&&!locked())restoreCamera();return;
        }
        if(!DismantleMotionRules.valid(packet.kind())||packet.age()<0||packet.age()>DismantleMotionRules.duration(packet.kind()))return;
        if(state==null||!state.packet.token().equals(packet.token())){state=new State(packet,clock);states.put(packet.owner(),state);}
        else {state.packet=packet;state.received=clock;state.age=Math.max(state.age,packet.age());}
        if(packet.owner()==mc.player.getId()){
            if(savedCamera==null){savedCamera=mc.options.getCameraType();cameraPlayer=mc.player.getUUID();}
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            // 服务端的逐刻安全位置替代瞬移整段路径，保留上一帧位置用于插值。
            mc.player.setPos(packet.position());mc.player.setDeltaMovement(Vec3.ZERO);mc.player.fallDistance=0;
        }
    }
    public static void tick(){
        var mc=Minecraft.getInstance();clock++;
        if(world!=mc.level||mc.player==null||!mc.player.isAlive()||cameraPlayer!=null&&!cameraPlayer.equals(mc.player.getUUID())){clear();world=mc.level;return;}
        simpleDomain.entrySet().removeIf(en->clock-en.getValue()>=6);
        states.entrySet().removeIf(entry->{State s=entry.getValue();var p=mc.level.getEntity(entry.getKey());return p==null||!p.isAlive()||clock-s.received>20||s.age>DismantleMotionRules.duration(s.packet.kind())+2;});
        for(State s:states.values())s.age=Math.min(DismantleMotionRules.duration(s.packet.kind())+3,s.age+1);
        if(locked()){
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);mc.player.setDeltaMovement(Vec3.ZERO);mc.player.fallDistance=0;
        }else restoreCamera();
    }
    private static void restoreCamera(){if(savedCamera!=null)Minecraft.getInstance().options.setCameraType(savedCamera);savedCamera=null;cameraPlayer=null;}
    public static void clear(){states.clear();restoreCamera();world=null;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void input(MovementInputUpdateEvent e){if(e.getEntity()!=Minecraft.getInstance().player||!locked())return;var input=e.getInput();input.forwardImpulse=input.leftImpulse=0;input.up=input.down=input.left=input.right=input.jumping=false;input.shiftKeyDown=false;e.getEntity().setDeltaMovement(Vec3.ZERO);}
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered e){if(locked()){e.setCanceled(true);e.setSwingHand(false);}}
    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){normal=new MotionRenderer(e.getContext(),false);slim=new MotionRenderer(e.getContext(),true);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void render(RenderPlayerEvent.Pre e){
        if(rendering||!(active(e.getEntity().getId())||simpleDomain(e.getEntity().getId()))||!(e.getEntity() instanceof AbstractClientPlayer player))return;
        // 始终准备原版动作后备；仅安装 YSM 不代表此玩家已启用自定义模型。
        // 内层事件仍交给 YSM 接管，确实使用自定义模型时保留其渲染入口。
        boolean custom=MotionRenderCompat.externalPlayerModels();
        PlayerRenderer renderer="slim".equals(player.getModelName())?slim:normal;if(renderer==null)return;
        e.setCanceled(true);rendering=true;var ps=e.getPoseStack();ps.pushPose();
        try{
            if(custom)transform(player,ps,e.getPartialTick());
            renderer.render(player,player.getYRot(),e.getPartialTick(),ps,e.getMultiBufferSource(),e.getPackedLight());
        }finally{ps.popPose();rendering=false;}
    }
    /** 女仆（含 YSM）继续由原渲染器绘制；嵌套调用和取消事件也能成对恢复矩阵。 */
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void living(RenderLivingEvent.Pre e){
        var entity=e.getEntity();
        if(rendering||!(active(entity.getId())||simpleDomain(entity.getId()))||!MaidBridge.isMaid(entity)&&!(entity instanceof AbstractClientPlayer&&MotionRenderCompat.externalPlayerModels()))return;
        e.setCanceled(true);rendering=true;var ps=e.getPoseStack();ps.pushPose();
        try{
            transform(entity,ps,e.getPartialTick());
            // YSM 的深层 renderer 不一定覆写原版 render；从实体原入口重入才能保留其 geoRender 分流。
            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).render(entity,entity.getYRot(),e.getPartialTick(),ps,e.getMultiBufferSource(),e.getPackedLight());
        }
        finally{ps.popPose();rendering=false;}
    }
    private static void transform(net.minecraft.world.entity.LivingEntity entity,PoseStack ps,float partial){
        State s=states.get(entity.getId());if(s==null)return; // 简易领域不翻转，只改手臂
        double t=age(s,partial),pivot=entity.getBbHeight()*0.5;
        // 自定义骨架只加整体翻转/旋转，不压低站姿模型，避免女仆陷进地面。
        ps.translate(0,pivot,0);
        if(s.packet.kind()==DismantleMotionRules.SPIN)ps.mulPose(Axis.YP.rotationDegrees((float)DismantleMotionRules.spin(t)));
        else{
            float facing=180-s.packet.yaw();ps.mulPose(Axis.YP.rotationDegrees(facing));
            ps.mulPose(Axis.XP.rotationDegrees((float)DismantleMotionRules.flip(s.packet.kind(),t)));
            ps.mulPose(Axis.ZP.rotationDegrees((float)DismantleMotionRules.sideways(s.packet.kind(),t)));
            ps.mulPose(Axis.YP.rotationDegrees(-facing));
        }
        ps.translate(0,-pivot,0);
    }
    private static double age(State state,float partial){return Math.min(DismantleMotionRules.duration(state.packet.kind()),state.age+partial);}
    /** 复用原版渲染器的皮肤、盔甲、披风和手持物，只替换动作中的姿势计算。 */
    private static final class MotionRenderer extends PlayerRenderer {
        MotionRenderer(EntityRendererProvider.Context context,boolean slim){super(context,slim);model=new AnimatedPlayer(context,slim);
            layers.removeIf(layer->layer instanceof net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer);
            addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,MotionPlayerGeometry.armor(0.5f),MotionPlayerGeometry.armor(1),context.getModelManager()));
        }
        @Override protected void setupRotations(AbstractClientPlayer p,PoseStack ps,float age,float yaw,float partial){
            State s=states.get(p.getId());
            if(s==null){super.setupRotations(p,ps,age,yaw,partial);return;} // 简易领域走默认站姿
            double t=age(s,partial);var pose=DismantlePose.sample(s.packet.kind(),t);
            super.setupRotations(p,ps,age,s.packet.yaw(),partial);
            // YSM 外层已做整体旋转，原版后备只补下蹲高度，不能再翻一次。
            if(MotionRenderCompat.externalPlayerModels()){ps.translate(0,-pose.sink(),0);return;}
            ps.translate(0,0.9-pose.sink(),0);
            if(s.packet.kind()==DismantleMotionRules.SPIN)ps.mulPose(Axis.YP.rotationDegrees((float)DismantleMotionRules.spin(t)));
            else ps.mulPose(Axis.XP.rotationDegrees((float)DismantleMotionRules.flip(s.packet.kind(),t)));
            ps.mulPose(Axis.ZP.rotationDegrees((float)DismantleMotionRules.sideways(s.packet.kind(),t)));
            ps.translate(0,-0.9,0);
        }
        @Override public Vec3 getRenderOffset(AbstractClientPlayer p,float partial){return Vec3.ZERO;}
    }
    private static final class AnimatedPlayer extends PlayerModel<AbstractClientPlayer> {
        AnimatedPlayer(EntityRendererProvider.Context c,boolean slim){super(MotionPlayerGeometry.player(slim),slim);}
        @Override public void copyPropertiesTo(net.minecraft.client.model.HumanoidModel<AbstractClientPlayer> target){super.copyPropertiesTo(target);MotionPlayerGeometry.knees(target,leftLeg.getChild("knee").xRot,rightLeg.getChild("knee").xRot);}
        @Override public void setupAnim(AbstractClientPlayer p,float limb,float amount,float age,float yaw,float pitch){
            super.setupAnim(p,0,0,age,yaw,pitch);
            if(simpleDomain(p.getId())&&states.get(p.getId())==null){
                // 简易领域 · 捌：两手上举、身体微微后仰，腿保持站立（**自绘姿态，不依赖 PlayerAnimator**）
                leftArm.xRot=rightArm.xRot=SimpleDomainPose.ARM_X;
                leftArm.zRot=-SimpleDomainPose.ARM_Z;rightArm.zRot=SimpleDomainPose.ARM_Z;
                leftArm.yRot=rightArm.yRot=0;leftArm.y=rightArm.y=2;leftArm.z=rightArm.z=0;
                head.xRot=SimpleDomainPose.HEAD_X;
                hat.copyFrom(head);leftSleeve.copyFrom(leftArm);rightSleeve.copyFrom(rightArm);
                return;
            }
            State s=states.get(p.getId());if(s==null)return;
            double t=age(s,age-p.tickCount);
            // 【自绘动作组】解动作的姿态一律由模组自己的关键帧动作集驱动（{@link DismantlePose}）——
            // 不再"装了就交给 PlayerAnimator"：前置装没装、动画读没读到都不影响动作表现，
            // 之前那种写法的后果是前置静默失败时玩家全程直立。
            if(!fallbackNoted){
                fallbackNoted=true;
                com.mojang.logging.LogUtils.getLogger().info("[解动作] 使用模组自绘关键帧动作组（8 段：后翻/飞踢/旋转/跪地/冲拳/滑铲/砸地/简易领域），不依赖 PlayerAnimator");
            }
            var pose=DismantlePose.sample(s.packet.kind(),t);crouching=false;
            body.xRot=rad(pose.body());body.yRot=body.zRot=0;body.y=0;
            head.xRot=rad(pose.head());head.yRot=head.zRot=0;head.y=0;head.z=0;
            leftArm.xRot=rad(pose.leftArm());rightArm.xRot=rad(pose.rightArm());leftArm.zRot=rad(-pose.spread());rightArm.zRot=rad(pose.spread());leftArm.yRot=rightArm.yRot=0;
            leftArm.y=rightArm.y=2;leftArm.z=rightArm.z=0;
            leftLeg.xRot=rad(pose.leftLeg());rightLeg.xRot=rad(pose.rightLeg());leftLeg.yRot=rightLeg.yRot=leftLeg.zRot=rightLeg.zRot=0;leftLeg.y=rightLeg.y=12;leftLeg.z=(float)-pose.legZ();rightLeg.z=(float)pose.legZ();
            MotionPlayerGeometry.knees(this,rad(pose.leftKnee()),rad(pose.rightKnee()));
            hat.copyFrom(head);jacket.copyFrom(body);leftSleeve.copyFrom(leftArm);rightSleeve.copyFrom(rightArm);leftPants.copyFrom(leftLeg);rightPants.copyFrom(rightLeg);
        }
        private static float rad(double degrees){return (float)Math.toRadians(degrees);}
    }
    private DismantleMotionClient(){}
}
