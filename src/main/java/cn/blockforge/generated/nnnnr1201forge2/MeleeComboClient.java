package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import org.lwjgl.glfw.GLFW;

/** 按实际攻击输入一次推进一段；不消费原版按键，也不取消正常攻击。 */
public final class MeleeComboClient {
    private static final class Clip {
        final UUID player;final int stage;final long start;final MeleeComboRules.Pose entry;boolean continues;
        Clip(UUID player,int stage,long start,MeleeComboRules.Pose entry){this.player=player;this.stage=stage;this.start=start;this.entry=entry;}
        UUID player(){return player;}int stage(){return stage;}long start(){return start;}
        long end(){return start+MeleeComboRules.duration(stage);}
    }
    private static final Map<Integer,Deque<Clip>> clips=new HashMap<>();
    private static Object world;
    private static long clock;
    private static boolean chainRequested;
    private static PlayerModel<AbstractClientPlayer> normalHands,slimHands;
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
            normalHands=new PlayerModel<>(MotionPlayerGeometry.player(false),false);
            slimHands=new PlayerModel<>(MotionPlayerGeometry.player(true),true);
        }
    }
    private static boolean available(){
        var mc=Minecraft.getInstance();var p=mc.player;
        return p!=null&&mc.level!=null&&Client.progress.sukuna()&&p.isAlive()&&!p.isSpectator()
            &&mc.screen==null&&mc.isWindowActive()&&mc.mouseHandler.isMouseGrabbed()&&!p.isUsingItem()
            &&!p.isPassenger()&&!p.isSleeping()&&!p.isFallFlying()&&!p.isSwimming()
            &&!DismantleMotionClient.active(p.getId())&&!DismantleMotionClient.simpleDomain(p.getId())
            &&!FinaleClient.local()&&!DomainClashClient.active()&&!NetBranchVisuals.locked()
            &&!ObserverClient.active()&&!TenShadowsClient.locked()&&!CastHands.sealing();
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Post e){
        if(e.getAction()!=GLFW.GLFW_PRESS)return;
        if(Client.CAST.matchesMouse(e.getButton())||Client.WHEEL.matchesMouse(e.getButton())){interrupt();return;}
        if(Minecraft.getInstance().options.keyAttack.matchesMouse(e.getButton()))arm();
    }
    @SubscribeEvent public static void key(InputEvent.Key e){
        if(e.getAction()!=GLFW.GLFW_PRESS)return;
        if(Client.CAST.matches(e.getKey(),e.getScanCode())||Client.WHEEL.matches(e.getKey(),e.getScanCode())){interrupt();return;}
        if(Minecraft.getInstance().options.keyAttack.matches(e.getKey(),e.getScanCode()))arm();
    }
    private static void arm(){
        if(!available()){interrupt();return;}
        var hit=Minecraft.getInstance().hitResult;
        if(hit!=null&&hit.getType()==HitResult.Type.BLOCK){interrupt();return;}
        // 从物理按下计数：原版空挥冷却期间不一定派发 Interaction 事件，不能因此漏掉第2至8下。
        Network.CHANNEL.sendToServer(new MeleeCombo.Press(false));chainRequested=true;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered e){
        if(e.isUseItem()||e.isAttack()&&(e.isCanceled()||!available()))interrupt();
    }
    public static void receive(MeleeCombo.State state){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(world!=mc.level){clear();world=mc.level;}
        if(state.stage()==0){clips.remove(state.owner());return;}
        if(!MeleeComboRules.valid(state.stage())||state.delay()<0||state.delay()>MeleeComboRules.MAX_BUFFER_TICKS)return;
        var entity=mc.level.getEntity(state.owner());if(entity==null||!entity.getUUID().equals(state.player()))return;
        Deque<Clip> queue=clips.computeIfAbsent(state.owner(),id->new ArrayDeque<>());
        if(!queue.isEmpty()&&!queue.peekFirst().player().equals(state.player()))queue.clear();
        while(!queue.isEmpty()&&queue.peekFirst().end()<=clock)queue.removeFirst();
        Clip last=queue.peekLast();
        // 已有片段时紧接它的末帧；不能把每个网络包的到达抖动累计成段间停顿。
        long start=last==null?clock+state.delay():Math.max(clock,last.end());
        boolean connected=last!=null&&start==last.end();
        if(queue.size()<MeleeComboRules.STAGES){
            if(connected)last.continues=true;
            queue.addLast(new Clip(state.player(),state.stage(),start,connected?MeleeComboRules.GUARD:MeleeComboRules.REST));
        }
    }
    private static Clip current(int id){
        Deque<Clip> queue=clips.get(id);if(queue==null)return null;
        while(!queue.isEmpty()&&clock>=queue.peekFirst().end())queue.removeFirst();
        if(queue.isEmpty()){clips.remove(id);return null;}
        return queue.peekFirst().start()<=clock?queue.peekFirst():null;
    }
    public static boolean active(int id){return current(id)!=null;}
    public record WindFrame(int stage,double age){}
    public static WindFrame windFrame(int id,float partial){
        Clip c=current(id);if(c==null)return null;
        double age=clock-c.start()+partial;
        return MeleeViewRules.windAlpha(c.stage(),age)>0?new WindFrame(c.stage(),age):null;
    }
    public static MeleeComboRules.Pose pose(int id,float partial){Clip c=current(id);return c==null?MeleeComboRules.REST:MeleeComboRules.sample(c.stage(),clock-c.start()+partial,c.entry,c.continues);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(world!=mc.level||mc.player==null){clear();world=mc.level;return;}
        if(!available())interrupt();
        if(mc.isPaused())return;clock++;
        for(int id:List.copyOf(clips.keySet())){
            var entity=mc.level.getEntity(id);Deque<Clip> queue=clips.get(id);
            if(entity==null||!entity.isAlive()||queue!=null&&!queue.isEmpty()&&!queue.peekFirst().player().equals(entity.getUUID())
                ||DismantleMotionClient.active(id)||DismantleMotionClient.simpleDomain(id)||FinaleClient.active(id))clips.remove(id);
            else current(id);
        }
    }
    public static void interrupt(){
        var mc=Minecraft.getInstance();
        if(mc.player!=null){clips.remove(mc.player.getId());if(chainRequested&&mc.getConnection()!=null)Network.CHANNEL.sendToServer(new MeleeCombo.Press(true));}
        chainRequested=false;
    }
    public static void clear(){clips.clear();chainRequested=false;world=null;clock=0;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    /** 第一人称保留原版攻击，只额外画出左右拳交替；踢击段显示前踢的腿。 */
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hand(RenderHandEvent e){
        var mc=Minecraft.getInstance();if(mc.player==null||CastHands.sealing())return;
        boolean slim="slim".equals(mc.player.getModelName());var hands=slim?slimHands:normalHands;if(hands==null)return;
        Clip c=current(mc.player.getId());if(c==null)return;
        double age=clock-c.start()+e.getPartialTick(),power=MeleeComboRules.drive(c.stage(),age);
        var pose=pose(mc.player.getId(),e.getPartialTick());
        MotionPlayerGeometry.reset(hands,slim);
        if(!mc.player.getMainHandItem().isEmpty()||!mc.player.getOffhandItem().isEmpty()){
            // 持物时保留原物品，只画一次气流，不额外叠一对手。
            if(e.getHand()==InteractionHand.MAIN_HAND&&!mc.player.isInvisible())drawHands(e,hands,slim,c,age,pose,false);
            e.getPoseStack().translate((c.stage()%2==0?1:-1)*power*.08,-power*.04,-power*.18);
            e.getPoseStack().mulPose(Axis.ZP.rotationDegrees((float)((c.stage()%2==0?1:-1)*power*12)));return;
        }
        e.setCanceled(true);if(e.getHand()!=InteractionHand.MAIN_HAND||mc.player.isInvisible())return;
        drawHands(e,hands,slim,c,age,pose,true);
    }
    private static void drawHands(RenderHandEvent e,PlayerModel<AbstractClientPlayer> hands,boolean slim,Clip c,
                                  double age,MeleeComboRules.Pose pose,boolean skin){
        var mc=Minecraft.getInstance();var ps=e.getPoseStack();
        double envelope=MeleeComboRules.envelope(c.stage(),age),hit=Math.max(0,MeleeComboRules.drive(c.stage(),age));
        var skinType=RenderType.entityCutoutNoCull(TenShadowsClient.skin(mc.player));
        for(int side:new int[]{-1,1}){
            boolean strike=MeleeViewRules.striking(c.stage(),side);
            var arm=side<0?hands.leftArm:hands.rightArm;var sleeve=side<0?hands.leftSleeve:hands.rightSleeve;
            var view=MeleeViewRules.arm(c.stage(),side,age,pose);
            arm.setPos(0,0,0);arm.xRot=(float)Math.toRadians(view.pitch());
            arm.yRot=(float)Math.toRadians(view.yaw());arm.zRot=(float)Math.toRadians(view.roll());sleeve.copyFrom(arm);
            arm.getChild("elbow").xRot=sleeve.getChild("elbow").xRot=(float)Math.toRadians(view.elbow());
            ps.pushPose();
            try{
                ps.translate(view.x(),view.y(),view.z());
                // 必须先转换整个模型坐标，再让 ModelPart 按 Z/Y/X 旋转；只倒转一个 pitch 会把皮肤和肘关节扭反。
                ps.scale(MeleeViewRules.VIEW_X,MeleeViewRules.VIEW_Y,MeleeViewRules.VIEW_Z);
                if(skin){
                    var buffer=e.getMultiBufferSource().getBuffer(skinType);
                    arm.render(ps,buffer,e.getPackedLight(),OverlayTexture.NO_OVERLAY);sleeve.render(ps,buffer,e.getPackedLight(),OverlayTexture.NO_OVERLAY);
                }
                if(strike)MeleeWindClient.limb(ps,e.getMultiBufferSource(),arm,"elbow",MeleeWindClient.center(side,slim),c.stage()==6?0:.375,c.stage(),age,true);
            }finally{ps.popPose();}
        }
        if(MeleeComboRules.kick(c.stage())){
            var leg=c.stage()==5?hands.leftLeg:hands.rightLeg;var pants=c.stage()==5?hands.leftPants:hands.rightPants;
            leg.setPos(0,0,0);leg.xRot=(float)Math.toRadians(-45-40*hit);leg.yRot=0;
            leg.zRot=c.stage()==7?(float)Math.toRadians(-50*hit):0;pants.copyFrom(leg);MotionPlayerGeometry.knees(hands,0,0);
            ps.pushPose();try{
                ps.translate(c.stage()==5?-.12:.42-.55*hit,-.72+envelope*.15+hit*.18,-.10-hit*.15);
                ps.scale(MeleeViewRules.VIEW_X,MeleeViewRules.VIEW_Y,MeleeViewRules.VIEW_Z);
                if(skin){
                    var buffer=e.getMultiBufferSource().getBuffer(skinType);
                    leg.render(ps,buffer,e.getPackedLight(),OverlayTexture.NO_OVERLAY);pants.render(ps,buffer,e.getPackedLight(),OverlayTexture.NO_OVERLAY);
                }
                MeleeWindClient.limb(ps,e.getMultiBufferSource(),leg,"knee",0,.375,c.stage(),age,true);
            }finally{ps.popPose();}
        }
    }
    private MeleeComboClient(){}
}
