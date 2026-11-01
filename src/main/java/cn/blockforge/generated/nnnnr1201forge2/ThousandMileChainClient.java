package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

/** 使用原版方块锁链模型，不使用原版扁平物品图，也不新增链节实体。 */
public final class ThousandMileChainClient {
    private static final Map<UUID, Visual> VISUALS = new HashMap<>();
    private static final UUID NONE = new UUID(0,0);
    private static ClientLevel world;
    private static long clock;
    private static double fov=70;
    private static int budget;
    private static final MultiBufferSource.BufferSource CHAIN_BUFFERS=MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(262144));
    private static final class Visual {
        ThousandMileChain.Snapshot previous, current;
        long received;
        Visual(ThousandMileChain.Snapshot s){previous=current=s;received=clock;}
        void update(ThousandMileChain.Snapshot s){previous=current;current=s;received=clock;}
        double blend(float partial){return Math.min(1,(clock-received+partial)/2.0);}
        Vec3 anchor(float partial){return previous.anchor().lerp(current.anchor(),blend(partial));}
        Vec3 tip(float partial){return previous.tip().lerp(current.tip(),blend(partial));}
    }
    public static boolean holding(){var p=Minecraft.getInstance().player;return p!=null&&ThousandMileChainItem.isChain(p.getMainHandItem());}
    private static boolean usable(){var mc=Minecraft.getInstance();return holding()&&mc.level!=null&&mc.screen==null&&mc.isWindowActive()&&mc.mouseHandler.isMouseGrabbed()&&mc.player.isAlive()&&!mc.player.isSpectator()&&!bound();}
    private static boolean bound(){var p=Minecraft.getInstance().player;return p!=null&&VISUALS.values().stream().anyMatch(v->v.current.target()==p.getId()&&(v.current.phase()==ThousandMileChain.BOUND||v.current.phase()==ThousandMileChain.RETURNING));}
    private static void press(){
        if(!usable())return;
        var mc=Minecraft.getInstance();var camera=mc.gameRenderer.getMainCamera();var look=camera.getLookVector();
        Visual v=VISUALS.get(mc.player.getUUID());
        Network.CHANNEL.sendToServer(new ThousandMileChain.Press(v==null?NONE:v.current.token(),camera.getPosition(),new Vec3(look.x(),look.y(),look.z()),Math.max(1,Math.min(175,fov)),Math.max(.2,Math.min(8,mc.getWindow().getWidth()/(double)Math.max(1,mc.getWindow().getHeight()))),mc.player.isShiftKeyDown()));
    }
    /** 在角色的左键连招之前接管，长按不重复推进三段操作。 */
    public static boolean mouse(int button,int action){
        var mc=Minecraft.getInstance();if(!holding()||mc.screen!=null)return false;
        boolean attack=mc.options.keyAttack.matchesMouse(button),use=mc.options.keyUse.matchesMouse(button);
        if(!attack&&!use)return false;
        if(action==GLFW.GLFW_PRESS&&usable()){if(attack)press();else Network.CHANNEL.sendToServer(new ThousandMileChain.Configure());}
        return true;
    }
    public static boolean key(int key,int scan,int action){
        var mc=Minecraft.getInstance();if(!holding()||mc.screen!=null)return false;
        boolean attack=mc.options.keyAttack.matches(key,scan),use=mc.options.keyUse.matches(key,scan);
        if(!attack&&!use)return false;
        if(action==GLFW.GLFW_PRESS&&usable()){if(attack)press();else Network.CHANNEL.sendToServer(new ThousandMileChain.Configure());}
        return true;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interaction(InputEvent.InteractionKeyMappingTriggered e){
        if((holding()&&(e.isAttack()||e.isUseItem()))||(bound()&&e.isAttack())){e.setCanceled(true);e.setSwingHand(false);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void move(MovementInputUpdateEvent e){
        if(!bound())return;var input=e.getInput();input.forwardImpulse=input.leftImpulse=0;
        input.up=input.down=input.left=input.right=input.jumping=false;input.shiftKeyDown=false;
        e.getEntity().setDeltaMovement(Vec3.ZERO);
    }
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov e){if(e.usedConfiguredFov())fov=e.getFOV();}
    public static void receive(ThousandMileChain.Snapshot s){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(world!=mc.level){VISUALS.clear();world=mc.level;}
        if(!s.dimension().equals(mc.level.dimension().location()))return;
        Visual old=VISUALS.get(s.owner());
        if(s.phase()==ThousandMileChain.END){if(old!=null&&old.current.token().equals(s.token()))VISUALS.remove(s.owner());return;}
        if(old==null||!old.current.token().equals(s.token()))VISUALS.put(s.owner(),new Visual(s));else old.update(s);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;clock++;
        var mc=Minecraft.getInstance();
        if(world!=mc.level){VISUALS.clear();world=mc.level;}
        VISUALS.values().removeIf(v->clock-v.received>40);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||VISUALS.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        PoseStack pose=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();float partial=e.getPartialTick();
        var buffers=CHAIN_BUFFERS;budget=768;
        // 总绘制预算有上限，优先显示本地玩家与最近的锁链。
        List<Visual> order=new ArrayList<>(VISUALS.values());
        order.sort(Comparator.comparingDouble(v->mc.player!=null&&v.current.owner().equals(mc.player.getUUID())?-1:v.current.hand().distanceToSqr(cam)));
        for(Visual v:order){
            var s=v.current;Vec3 hand=s.hand();var owner=mc.level.getPlayerByUUID(s.owner());
            if(owner!=null){
                Vec3 look=owner.getLookAngle();Vec3 side=look.cross(new Vec3(0,1,0)).normalize();
                double sign=owner.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1;
                hand=owner.getPosition(partial).add(0,owner.getEyeHeight()-.4,0).add(side.scale(.26*sign)).add(look.scale(.35));
                if(owner==mc.player&&mc.options.getCameraType().isFirstPerson())hand=cam.add(0,-.35,0).add(side.scale(.3*sign)).add(look.scale(.55));
            }
            Vec3 anchor=v.anchor(partial),tip=v.tip(partial);
            // 物理首端用轻微下垂连接；释放出去的工作端始终是无重力直线。
            loose(pose,buffers,cam,hand,anchor);
            piece(pose,buffers,cam,anchor.add(0,-.08,0),anchor.add(0,.22,0));
            if(s.phase()!=ThousandMileChain.ANCHOR){
                segment(pose,buffers,cam,hand,tip);
                if(s.target()>=0){
                    var target=mc.level.getEntity(s.target());
                    if(target!=null)tip=target.getPosition(partial).add(0,s.height()/2,0);
                    double radius=Math.max(.27,s.width()*.58),height=Math.max(.4,s.height()*.65);
                    Vec3 last=tip.add(radius,-height/2,0);
                    for(int j=1;j<=32;j++){
                        double t=j/32.0,a=t*Math.PI*6;Vec3 next=tip.add(Math.cos(a)*radius,(t-.5)*height,Math.sin(a)*radius);
                        piece(pose,buffers,cam,last,next);last=next;
                    }
                }
                if(s.armed()&&tip.distanceToSqr(cam)<80*80){
                    pose.pushPose();pose.translate(tip.x-cam.x,tip.y-cam.y,tip.z-cam.z);pose.scale(.45f,.45f,.45f);
                    // 仍使用已经接入的用户原模型，不改它的display和UV。
                    mc.getItemRenderer().renderStatic(new ItemStack(Content.TENKA_SPEAR.get()),ItemDisplayContext.FIXED,light(tip),OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);
                    pose.popPose();
                }
            }
        }
        // 独立缓冲只包含锁链与附件；及时提交，避免延迟到世界矩阵退出后才绘制。
        buffers.endBatch();
    }
    private static void loose(PoseStack pose,MultiBufferSource buffers,Vec3 cam,Vec3 a,Vec3 b){
        // 首端太远时不制作巨量曲线，近处仍保留原版链节比例。
        double length=a.distanceTo(b),sag=Math.min(1.2,length*.1);Vec3 last=a;
        for(int i=1;i<=12;i++){double t=i/12.0;Vec3 at=a.lerp(b,t).add(0,-Math.sin(t*Math.PI)*sag,0);segment(pose,buffers,cam,last,at);last=at;}
    }
    private static void segment(PoseStack pose,MultiBufferSource buffers,Vec3 cam,Vec3 a,Vec3 b){
        Vec3 delta=b.subtract(a);double length=delta.length();if(length<.001||budget<=0)return;
        Vec3 dir=delta.scale(1/length),relative=a.subtract(cam);
        // 精确裁剪到镜头80格球内：不因百万格总长度而循环百万次或拉伸链节。
        double along=relative.dot(dir),disc=along*along-relative.lengthSqr()+80*80;if(disc<0)return;
        double root=Math.sqrt(disc),start=Math.max(0,-along-root),end=Math.min(length,-along+root);
        for(double d=start;d<end&&budget>0;d+=1)piece(pose,buffers,cam,a.add(dir.scale(d)),a.add(dir.scale(Math.min(end,d+1))));
    }
    private static int light(Vec3 at){var level=Minecraft.getInstance().level;BlockPos pos=BlockPos.containing(at);return level!=null&&level.hasChunkAt(pos)?LevelRenderer.getLightColor(level,pos):LightTexture.FULL_BRIGHT;}
    private static void piece(PoseStack pose,MultiBufferSource buffers,Vec3 cam,Vec3 a,Vec3 b){
        if(budget<=0||a.distanceToSqr(cam)>82*82)return;Vec3 d=b.subtract(a);double length=d.length();if(length<.001)return;budget--;
        pose.pushPose();pose.translate(a.x-cam.x,a.y-cam.y,a.z-cam.z);
        Vec3 normal=d.scale(1/length);pose.mulPose(new Quaternionf().rotationTo(0,1,0,(float)normal.x,(float)normal.y,(float)normal.z));
        pose.scale(1,(float)length,1);pose.translate(-.5,0,-.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.CHAIN.defaultBlockState(),pose,buffers,light(a),OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
    private ThousandMileChainClient(){}
}
