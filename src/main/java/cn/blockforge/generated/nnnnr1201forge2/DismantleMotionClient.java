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
public final class DismantleMotionClient {
    private static final class State {
        Network.MotionState packet;double age;long received;UUID entity;
        State(Network.MotionState packet,long now){this.packet=packet;age=packet.age();received=now;}
    }
    private static final Map<Integer,State> states=new HashMap<>();
    private static CameraType savedCamera;private static UUID cameraPlayer;private static Object world;
    private static long clock;private static boolean rendering;
    private static MotionRenderer normal,slim;
    private static final MotionSafety.Frames<UUID> frames=new MotionSafety.Frames<>();
    private static final Set<String> failures=new HashSet<>();
    private static long retryAfter;
    private static MotionRenderer renderer(AbstractClientPlayer player){
        if((normal==null||slim==null)&&clock>=retryAfter){
            try{var context=EntityRendererRegistry.context(Minecraft.getInstance());normal=new MotionRenderer(context,false);slim=new MotionRenderer(context,true);}
            catch(RuntimeException|LinkageError ex){retryAfter=clock+40;report("模型恢复",ex);}
        }
        return "slim".equals(player.getModelName())?slim:normal;
    }
    private static void report(String key,Throwable ex){if(failures.add(key))com.mojang.logging.LogUtils.getLogger().warn("[动作兜底] {}，保留可用绘制并稍后重试",key,ex);}
    private static boolean visible(AbstractClientPlayer player){
        var mc=Minecraft.getInstance();
        return player.isAlive()&&!player.isSpectator()&&!player.isInvisible()&&posing(player.getId())
            &&!FinaleClient.active(player.getId())&&!TenShadowsClient.chanting(player.getUUID())
            &&!(mc.getCameraEntity()==player&&mc.options.getCameraType().isFirstPerson());
    }
    @SubscribeEvent public static void beforeFrame(net.minecraftforge.event.TickEvent.RenderTickEvent e){
        if(e.phase==net.minecraftforge.event.TickEvent.Phase.START)frames.begin();
    }
    /** 其他模型模组未派发玩家事件时，世界实体阶段再补一次；只补漏，不叠两个玩家。 */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void rescue(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||rendering)return;
        var mc=Minecraft.getInstance();if(mc.level==null||ObserverClient.active())return;
        var buffers=mc.renderBuffers().bufferSource();Vec3 camera=e.getCamera().getPosition();boolean drew=false;
        for(AbstractClientPlayer player:mc.level.players()){
            if(!visible(player)||!frames.missing(player.getUUID())||player.distanceToSqr(camera)>128*128
                ||!e.getFrustum().isVisible(player.getBoundingBox().inflate(2)))continue;
            MotionRenderer renderer=renderer(player);if(renderer==null)continue;
            float partial=e.getPartialTick();var ps=e.getPoseStack();ps.pushPose();rendering=true;
            try{
                ps.translate(net.minecraft.util.Mth.lerp(partial,player.xo,player.getX())-camera.x,
                    net.minecraft.util.Mth.lerp(partial,player.yo,player.getY())-camera.y,
                    net.minecraft.util.Mth.lerp(partial,player.zo,player.getZ())-camera.z);
                renderer.render(player,player.getYRot(),partial,ps,buffers,mc.getEntityRenderDispatcher().getPackedLightCoords(player,partial));
                frames.mark(player.getUUID());drew=true;
            }catch(RuntimeException|LinkageError ex){report("漏画补绘",ex);}
            finally{ps.popPose();rendering=false;}
        }
        if(drew)buffers.endBatch();
    }
    public static boolean active(int id){return states.containsKey(id);}
    /** 兜底只提示一次，避免刷屏。 */
    private static boolean fallbackNoted;
    /** 已经记录过"应用自绘姿态"的 kind（只写一次，避免每帧刷日志）。 */
    private static final java.util.Set<Integer> poseNoted=new java.util.HashSet<>();
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
            if(packet.owner()==mc.player.getId()&&!locked()&&!MotionFallbackClient.active(packet.owner()))restoreCamera();return;
        }
        if(!DismantleMotionRules.valid(packet.kind())||packet.age()<0||packet.age()>DismantleMotionRules.duration(packet.kind())
            ||!Float.isFinite(packet.yaw())||!Double.isFinite(packet.position().lengthSqr()))return;
        if(state==null||!state.packet.token().equals(packet.token())){state=new State(packet,clock);states.put(packet.owner(),state);
            com.mojang.logging.LogUtils.getLogger().info("[解动作] 客户端收到状态：kind={} owner={} age={}（动作表现将从这一刻开始）",packet.kind(),packet.owner(),packet.age());}
        else {state.packet=packet;state.received=clock;state.age=MotionSafety.synchronize(state.age,packet.age(),DismantleMotionRules.duration(packet.kind()));}
        var actor=mc.level.getEntity(packet.owner());if(actor!=null)state.entity=actor.getUUID();
        if(packet.owner()==mc.player.getId()){
            if(savedCamera==null){savedCamera=mc.options.getCameraType();cameraPlayer=mc.player.getUUID();}
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            // 服务端的逐刻安全位置替代瞬移整段路径，保留上一帧位置用于插值。
            mc.player.setPos(packet.position());mc.player.setDeltaMovement(Vec3.ZERO);mc.player.fallDistance=0;
        }
    }
    public static void tick(){
        var mc=Minecraft.getInstance();if(mc.isPaused())return;clock++;MotionFallbackClient.tick();
        if(world!=mc.level||mc.player==null||!mc.player.isAlive()||cameraPlayer!=null&&!cameraPlayer.equals(mc.player.getUUID())){clear();world=mc.level;return;}
        simpleDomain.entrySet().removeIf(en->clock-en.getValue()>=6);
        states.entrySet().removeIf(entry->{
            State s=entry.getValue();var p=mc.level.getEntity(entry.getKey());
            // 实体生成包可能晚于动作包几刻；短暂缺席不立即吞动作，也不容许编号复用串人。
            if(p==null)return clock-s.received>6;
            if(s.entity==null)s.entity=p.getUUID();
            return !s.entity.equals(p.getUUID())||!p.isAlive()||clock-s.received>20||s.age>DismantleMotionRules.duration(s.packet.kind())+2;
        });
        for(State s:states.values())s.age=Math.min(DismantleMotionRules.duration(s.packet.kind())+3,s.age+1);
        if(locked()){
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);mc.player.setDeltaMovement(Vec3.ZERO);mc.player.fallDistance=0;
        }else if(MotionFallbackClient.active(mc.player.getId()))mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        else restoreCamera();
    }
    /** 新动作同样临时借用第三人称，但不锁输入、不移动玩家；结束或切世界即归还。 */
    static void watch(AbstractClientPlayer player){
        var mc=Minecraft.getInstance();if(world!=mc.level){clear();world=mc.level;}
        if(player==mc.player){if(savedCamera==null){savedCamera=mc.options.getCameraType();cameraPlayer=player.getUUID();}mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
    }
    private static void restoreCamera(){if(savedCamera!=null)Minecraft.getInstance().options.setCameraType(savedCamera);savedCamera=null;cameraPlayer=null;}
    public static void clear(){states.clear();simpleDomain.clear();MotionFallbackClient.clear();frames.begin();failures.clear();restoreCamera();world=null;}
    public static boolean posing(int id){return active(id)||simpleDomain(id)||MeleeComboClient.active(id)||CastHands.hasPose(id)||MotionFallbackClient.active(id);}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void input(MovementInputUpdateEvent e){if(e.getEntity()!=Minecraft.getInstance().player||!locked())return;var input=e.getInput();input.forwardImpulse=input.leftImpulse=0;input.up=input.down=input.left=input.right=input.jumping=false;input.shiftKeyDown=false;e.getEntity().setDeltaMovement(Vec3.ZERO);}
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered e){if(locked()){e.setCanceled(true);e.setSwingHand(false);}}
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){normal=new MotionRenderer(e.getContext(),false);slim=new MotionRenderer(e.getContext(),true);retryAfter=0;failures.clear();}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void render(RenderPlayerEvent.Pre e){
        if(rendering||!(e.getEntity() instanceof AbstractClientPlayer player)||!visible(player))return;
        MotionRenderer renderer=renderer(player);if(renderer==null)return;
        rendering=true;var ps=e.getPoseStack();ps.pushPose();
        try{
            renderer.render(player,player.getYRot(),e.getPartialTick(),ps,e.getMultiBufferSource(),e.getPackedLight());
            frames.mark(player.getUUID());e.setCanceled(true);
        }catch(RuntimeException|LinkageError ex){report("玩家事件绘制",ex);}
        finally{ps.popPose();rendering=false;}
    }
    /** 女仆（含 YSM）继续由原渲染器绘制；嵌套调用和取消事件也能成对恢复矩阵。 */
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true)
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void living(RenderLivingEvent.Pre e){
        var entity=e.getEntity();
        if(rendering||!posing(entity.getId())||entity.isInvisible())return;
        if(entity instanceof AbstractClientPlayer player){
            if(!visible(player))return;
            if(!frames.missing(player.getUUID())){e.setCanceled(true);return;}
            MotionRenderer renderer=renderer(player);if(renderer==null)return;
            rendering=true;e.getPoseStack().pushPose();
            try{renderer.render(player,player.getYRot(),e.getPartialTick(),e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight());frames.mark(player.getUUID());e.setCanceled(true);}
            catch(RuntimeException|LinkageError ex){report("生物事件绘制",ex);}
            finally{e.getPoseStack().popPose();rendering=false;}
            return;
        }
        if(!MaidBridge.isMaid(entity))return;
        e.setCanceled(true);rendering=true;var ps=e.getPoseStack();ps.pushPose();
        try{
            transform(entity,ps,e.getPartialTick());
            // YSM 的深层 renderer 不一定覆写原版 render；从实体原入口重入才能保留其 geoRender 分流。
            var motionRenderer=Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if(motionRenderer!=null)motionRenderer.render(entity,entity.getYRot(),e.getPartialTick(),ps,e.getMultiBufferSource(),e.getPackedLight());
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
    private static double age(State state,float partial){return MotionSafety.age(state.age,partial,DismantleMotionRules.duration(state.packet.kind()));}
    /** 复用原版渲染器的皮肤、盔甲、披风和手持物，只替换动作中的姿势计算。 */
    private static final class MotionRenderer extends net.minecraft.client.renderer.entity.LivingEntityRenderer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        MotionRenderer(EntityRendererProvider.Context context,boolean slim){super(context,new AnimatedPlayer(context,slim),.5f);
            layers.removeIf(layer->layer instanceof net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer);
            addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,MotionPlayerGeometry.armor(0.5f),MotionPlayerGeometry.armor(1),context.getModelManager()));
            addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this,context.getItemInHandRenderer()));
            addLayer(new net.minecraft.client.renderer.entity.layers.CapeLayer(this));
            addLayer(new MeleeWindClient.Layer(this));
        }
        /** 不再调用 super.render：它会再次派发可取消事件，导致已经接到动作也可能整个人被吞。 */
        @Override public void render(AbstractClientPlayer p,float yaw,float partial,PoseStack ps,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
            ps.pushPose();try{
                float bodyYaw=net.minecraft.util.Mth.rotLerp(partial,p.yBodyRotO,p.yBodyRot);
                float headYaw=net.minecraft.util.Mth.rotLerp(partial,p.yHeadRotO,p.yHeadRot)-bodyYaw;
                float pitch=net.minecraft.util.Mth.lerp(partial,p.xRotO,p.getXRot()),age=p.tickCount+partial;
                model.attackTime=0;model.riding=false;model.young=false;model.crouching=false;
                model.leftArmPose=model.rightArmPose=net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
                model.setAllVisible(true);
                model.prepareMobModel(p,0,0,partial);model.setupAnim(p,0,0,age,headYaw,pitch);
                setupRotations(p,ps,age,bodyYaw,partial);ps.scale(-.9375f,-.9375f,.9375f);ps.translate(0,-1.501,0);
                model.renderToBuffer(ps,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(getTextureLocation(p))),
                    light,getOverlayCoords(p,0),1,1,1,1);
                // 单个盔甲/手持物兼容层失败不能再连带吞掉基础身体与动作。
                for(var layer:layers){ps.pushPose();try{layer.render(ps,buffers,light,p,0,0,partial,age,headYaw,pitch);}
                    catch(RuntimeException|LinkageError ex){report(layer.getClass().getSimpleName(),ex);}finally{ps.popPose();}}
            }finally{ps.popPose();}
        }
        @Override protected void setupRotations(AbstractClientPlayer p,PoseStack ps,float age,float yaw,float partial){
            State s=states.get(p.getId());
            if(s==null){
                super.setupRotations(p,ps,age,yaw,partial);
                if(MeleeComboClient.active(p.getId())){
                    var pose=MeleeComboClient.pose(p.getId(),partial);
                    ps.translate(0,.9-pose.sink(),0);ps.mulPose(Axis.ZP.rotationDegrees((float)pose.lean()));ps.translate(0,-.9,0);
                }else if(!simpleDomain(p.getId())&&!CastHands.hasPose(p.getId())){
                    var frame=MotionFallbackClient.sample(p.getId(),partial);
                    if(frame!=null){ps.translate(0,.9-frame.pose().sink(),0);ps.mulPose(Axis.XP.rotationDegrees((float)frame.flip()));
                        ps.mulPose(Axis.YP.rotationDegrees((float)frame.spin()));ps.mulPose(Axis.ZP.rotationDegrees((float)frame.roll()));ps.translate(0,-.9,0);}
                }
                return;
            }
            double t=age(s,partial);var pose=DismantlePose.sample(s.packet.kind(),t);
            super.setupRotations(p,ps,age,s.packet.yaw(),partial);
            ps.translate(0,0.9-pose.sink(),0);
            if(s.packet.kind()==DismantleMotionRules.SPIN)ps.mulPose(Axis.YP.rotationDegrees((float)DismantleMotionRules.spin(t)));
            else ps.mulPose(Axis.XP.rotationDegrees((float)DismantleMotionRules.flip(s.packet.kind(),t)));
            ps.mulPose(Axis.ZP.rotationDegrees((float)DismantleMotionRules.sideways(s.packet.kind(),t)));
            ps.translate(0,-0.9,0);
        }
        @Override public net.minecraft.resources.ResourceLocation getTextureLocation(AbstractClientPlayer p){return TenShadowsClient.skin(p);}
        @Override protected boolean shouldShowName(AbstractClientPlayer p){return false;}
        @Override public Vec3 getRenderOffset(AbstractClientPlayer p,float partial){return Vec3.ZERO;}
    }
    private static final class AnimatedPlayer extends PlayerModel<AbstractClientPlayer> {
        private final boolean thin;
        AnimatedPlayer(EntityRendererProvider.Context c,boolean slim){super(MotionPlayerGeometry.player(slim),slim);thin=slim;}
        @Override public void copyPropertiesTo(net.minecraft.client.model.HumanoidModel<AbstractClientPlayer> target){
            super.copyPropertiesTo(target);
            MotionPlayerGeometry.knees(target,leftLeg.getChild("knee").xRot,rightLeg.getChild("knee").xRot);
            MotionPlayerGeometry.elbows(target,leftArm.getChild("elbow").xRot,rightArm.getChild("elbow").xRot);
        }
        @Override public void translateToHand(net.minecraft.world.entity.HumanoidArm side,PoseStack ps){
            // 手持物必须跟着前臂，而不是仍绑在原来的直臂末端。
            var arm=side==net.minecraft.world.entity.HumanoidArm.LEFT?leftArm:rightArm;
            if(thin)arm.x+=side==net.minecraft.world.entity.HumanoidArm.LEFT?.5f:-.5f;
            try{arm.translateAndRotate(ps);ps.translate(0,.25,0);ps.mulPose(Axis.XP.rotation(arm.getChild("elbow").xRot));ps.translate(0,-.25,0);}
            finally{if(thin)arm.x-=side==net.minecraft.world.entity.HumanoidArm.LEFT?.5f:-.5f;}
        }
        @Override public void setupAnim(AbstractClientPlayer p,float limb,float amount,float age,float yaw,float pitch){
            super.setupAnim(p,0,0,age,yaw,pitch);
            MotionPlayerGeometry.reset(this,thin);
            if(!active(p.getId())&&!simpleDomain(p.getId())&&MeleeComboClient.active(p.getId())){
                var pose=MeleeComboClient.pose(p.getId(),Math.max(0,Math.min(1,age-p.tickCount)));
                body.xRot=rad(pose.body());body.yRot=rad(pose.twist());head.xRot=rad(pose.head());head.yRot=rad(yaw);
                leftArm.xRot=rad(pose.leftArm());rightArm.xRot=rad(pose.rightArm());
                leftArm.yRot=rad(pose.leftYaw());rightArm.yRot=rad(pose.rightYaw());
                leftArm.zRot=rad(pose.leftSpread());rightArm.zRot=rad(pose.rightSpread());
                // 躯干以腰部为轴，头和肩跟随前倾与转腰，不再胸口自己转、手臂挂在空中。
                double turn=Math.toRadians(pose.twist()),bend=Math.toRadians(pose.body());
                double ct=Math.cos(turn),st=Math.sin(turn),cb=Math.cos(bend),sb=Math.sin(bend);
                body.y=(float)(12-12*cb);body.x=(float)(-12*sb*st);body.z=(float)(-12*sb*ct);
                head.setPos(body.x,body.y,body.z);
                double shoulder=thin?2.5:2,dy=shoulder-12;
                leftArm.setPos((float)(5*ct+dy*sb*st),(float)(12+dy*cb),(float)(-5*st+dy*sb*ct));
                rightArm.setPos((float)(-5*ct+dy*sb*st),(float)(12+dy*cb),(float)(5*st+dy*sb*ct));
                leftLeg.xRot=rad(pose.leftLeg());rightLeg.xRot=rad(pose.rightLeg());
                leftLeg.yRot=rad(pose.leftLegYaw());rightLeg.yRot=rad(pose.rightLegYaw());
                leftLeg.zRot=rad(pose.leftLegSpread());rightLeg.zRot=rad(pose.rightLegSpread());
                MotionPlayerGeometry.knees(this,rad(pose.leftKnee()),rad(pose.rightKnee()));
                MotionPlayerGeometry.elbows(this,rad(pose.leftElbow()),rad(pose.rightElbow()));
                copyWear();return;
            }
            if(simpleDomain(p.getId())&&states.get(p.getId())==null){
                // 简易领域 · 捌：两手上举、身体微微后仰，腿保持站立（**自绘姿态，不依赖 PlayerAnimator**）
                leftArm.xRot=rightArm.xRot=SimpleDomainPose.ARM_X;
                leftArm.zRot=-SimpleDomainPose.ARM_Z;rightArm.zRot=SimpleDomainPose.ARM_Z;
                leftArm.yRot=rightArm.yRot=0;leftArm.y=rightArm.y=2;leftArm.z=rightArm.z=0;
                head.xRot=SimpleDomainPose.HEAD_X;
                copyWear();return;
            }
            State s=states.get(p.getId());if(s==null){
                if(!CastHands.apply(this,p.getId())){var frame=MotionFallbackClient.sample(p.getId(),Math.max(0,Math.min(1,age-p.tickCount)));if(frame!=null)applyPose(frame.pose());}
                copyWear();return;
            }
            double t=age(s,Math.max(0,Math.min(1,age-p.tickCount)));
            // 【自绘动作组】解动作的姿态一律由模组自己的关键帧动作集驱动（{@link DismantlePose}）——
            // 不再"装了就交给 PlayerAnimator"：前置装没装、动画读没读到都不影响动作表现，
            // 之前那种写法的后果是前置静默失败时玩家全程直立。
            if(!fallbackNoted){
                fallbackNoted=true;
                com.mojang.logging.LogUtils.getLogger().info("[解动作] 使用模组自绘关键帧动作组（8 段：后翻/飞踢/旋转/跪地/冲拳/滑铲/砸地/简易领域），不依赖 PlayerAnimator");
            }
            var pose=DismantlePose.sample(s.packet.kind(),t);crouching=false;
            if(!poseNoted.contains(s.packet.kind())){
                poseNoted.add(s.packet.kind());
                com.mojang.logging.LogUtils.getLogger().info("[解动作] 应用自绘姿态：kind={} t={}",s.packet.kind(),String.format("%.1f",t));
            }
            applyPose(pose);copyWear();
        }
        private void applyPose(DismantlePose.Pose pose){
            crouching=false;
            body.xRot=rad(pose.body());body.yRot=body.zRot=0;body.y=0;
            head.xRot=rad(pose.head());head.yRot=head.zRot=0;head.y=0;head.z=0;
            leftArm.xRot=rad(pose.leftArm());rightArm.xRot=rad(pose.rightArm());leftArm.zRot=rad(-pose.spread());rightArm.zRot=rad(pose.spread());leftArm.yRot=rightArm.yRot=0;
            leftArm.y=rightArm.y=2;leftArm.z=rightArm.z=0;
            leftLeg.xRot=rad(pose.leftLeg());rightLeg.xRot=rad(pose.rightLeg());leftLeg.yRot=rightLeg.yRot=leftLeg.zRot=rightLeg.zRot=0;leftLeg.y=rightLeg.y=12;leftLeg.z=(float)-pose.legZ();rightLeg.z=(float)pose.legZ();
            MotionPlayerGeometry.knees(this,rad(pose.leftKnee()),rad(pose.rightKnee()));
            copyWear();
        }
        private void copyWear(){hat.copyFrom(head);jacket.copyFrom(body);leftSleeve.copyFrom(leftArm);rightSleeve.copyFrom(rightArm);leftPants.copyFrom(leftLeg);rightPants.copyFrom(rightLeg);}
        private static float rad(double degrees){return (float)Math.toRadians(degrees);}
    }
    private DismantleMotionClient(){}
}
