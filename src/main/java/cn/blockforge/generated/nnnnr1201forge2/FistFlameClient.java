package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.phys.Vec3;

/** 「咒力附拳」的表现层（客户端，**所有玩家都能看见**）：双手上飘荡的红色半透明咒力火焰。
 *
 *  <p>服务端在附拳开启期间周期性广播一条 {@code kind=236} 特效（带玩家 entityId 与寿命），
 *  客户端据此把该玩家登记进 {@link #BURNING}，然后在**玩家渲染器上加一层**，
 *  用 {@code ModelPart#translateAndRotate} 把面片贴到左右手的位置上——所以别人看你的手也着着火。</p>
 *
 *  <p>火焰＝若干张相机朝向的红色面片，逐片随时间上下飘（`sin`）、左右摆、明暗闪烁；全半透明、加法感靠多层叠加。
 *  第一人称下自己的手走的是手持渲染通路，看不到这一层，所以本机保留原有 {@link FistAura} 的屏幕表现。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class FistFlameClient {
    public static final int FX=236;
    /** 正在附拳的玩家 → 到期刻。 */
    private static final Map<Integer,Long> BURNING=new HashMap<>();
    private static final int FLAMES=7;
    /** PhotonFx 用的 bloom 发光色与通用光效贴图（PhotonFx 里的常量是私有的，这里自备一份）。 */
    private static final int BLOOM_RED=0xFF8A1208;
    private static final String GLOW="textures/effect/glow.png";
    /** 手部世界坐标（每帧由渲染层读矩阵得到，供 PhotonFx 在**手的位置**点火）。 */
    private static final Map<Integer,Vec3[]> HANDS=new HashMap<>();
    private static final int SPAWN_INTERVAL=2;
    private static long spawnTick;

    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        BURNING.put(e.owner(),mc.level.getGameTime()+Math.max(10,e.life()));
    }
    /** 每刻：清理过期状态 + 在每只手的位置点一小簇 PhotonFx 咒力火焰（短命发射器，用完即散）。 */
    public static void tick(){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        long now=mc.level.getGameTime();BURNING.entrySet().removeIf(en->now>en.getValue());
        if(BURNING.isEmpty()&&!Client.progress.fist()){HANDS.clear();return;}
        if(now%SPAWN_INTERVAL!=0)return;
        for(var entry:HANDS.entrySet()){
            boolean local=entry.getKey()==(mc.player==null?-1:mc.player.getId());
            if(!local&&!burning(entry.getKey()))continue;
            if(local&&!Client.progress.fist())continue;
            for(Vec3 hand:entry.getValue())if(hand!=null)spawn(hand);
        }
    }
    /** 一只手上的咒力火焰：PhotonFx 的暗红火团（带轻微上升）+ 一层加法发光（bloom 只能靠加法混合）。 */
    private static void spawn(Vec3 hand){
        try{
            PhotonFx.blobs(hand,0.13,0.10,0.07,0xFF5512,BLOOM_RED,0.06,-0.012,5,1,9);          // 火团：漂荡上升
            PhotonFx.spark(hand,0.07,0.11,0xFF2A10,BLOOM_RED,GLOW,true,1,7,4,0.25);           // 发光核：加法 bloom
        }catch(Throwable ignored){/* 没装 Photon：保留下面的手部定位，不画即降级 */ }
    }
    public static void clear(){BURNING.clear();HANDS.clear();}
    private static boolean burning(int id){Long until=BURNING.get(id);return until!=null&&Minecraft.getInstance().level!=null&&Minecraft.getInstance().level.getGameTime()<=until;}

    @Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
            for(String skin:e.getSkins()){PlayerRenderer renderer=e.getSkin(skin);if(renderer!=null)renderer.addLayer(new Flames(renderer));}
        }
    }
    /** 渲染层不再自己画面片，只负责**读出手部世界坐标**，交给 {@link #spawn} 用 PhotonFx 点火。 */
    private static final class Flames extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        Flames(PlayerRenderer parent){super(parent);}
        @Override public void render(PoseStack ps,MultiBufferSource source,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
            if(!burning(player.getId()))return;
            var model=getParentModel();
            Vec3 right=handPos(ps,model.rightArm),left=handPos(ps,model.leftArm);
            if(right!=null||left!=null)HANDS.put(player.getId(),new Vec3[]{right,left});
        }
        /** 手臂骨骼 → 往手掌方向挪 9/16 格（模型空间 +Y 向下）→ 读出该点的世界坐标。 */
        private static Vec3 handPos(PoseStack ps,net.minecraft.client.model.geom.ModelPart arm){
            ps.pushPose();arm.translateAndRotate(ps);ps.translate(0,9/16.0,0);
            Vec3 p=worldFromPose(ps);ps.popPose();return p;
        }
    }
    /** 把渲染用的相机相对矩阵平移量还原成世界坐标（渲染层里的 PoseStack 以相机为原点）。 */
    private static Vec3 worldFromPose(PoseStack ps){
        var m=ps.last().pose();var cam=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return cam.add(m.m30(),m.m31(),m.m32());
    }
        /** 以某条手臂的骨骼为基准，往手掌方向偏移后画一簇火焰。 */
    /** 第一人称：自己的手走的是手持渲染通路（不经过玩家模型），这里按摄像机基向量估算**持手的世界坐标**，
     *  登记进 {@link #HANDS}，让 PhotonFx 在本机手上也点火。偏移嫌偏就改这三个系数。 */
    @SubscribeEvent public static void hand(RenderHandEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.level==null||!Client.progress.fist()||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        var cam=mc.gameRenderer.getMainCamera();
        Vec3 eye=cam.getPosition(),look=new Vec3(cam.getLookVector()),right=new Vec3(cam.getLeftVector()).scale(-1),up=new Vec3(cam.getUpVector());
        Vec3 hand=eye.add(look.scale(0.42)).add(right.scale(0.34)).subtract(up.scale(0.22));
        HANDS.put(p.getId(),new Vec3[]{hand,null});
    }
    private FistFlameClient(){}
}
