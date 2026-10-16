package cn.blockforge.generated.nnnnr1201forge2;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/** 用 **PlayerAnimator** 播动作动画（前置依赖）。
 *
 *  <p>设计要点：</p>
 *  <ul>
 *    <li>动画文件放在 {@code assets/sukuna/player_animations/<名字>.json}，由 PlayerAnimator 自己加载；</li>
 *    <li>每个玩家占一个 {@code ModifierLayer<KeyframeAnimationPlayer>}（第 {@link #LAYER} 层）；</li>
 *    <li>**时间轴由我们自己驱动**：动作的时长与击发时机由服务端的 {@code DismantleMotionRules} 决定，
 *        所以这里只把"当前第几刻"喂给它，不用它的自动 tick，保证动画与剑气/位移严格对齐；</li>
 *    <li>整体翻转（后空翻/转身）仍然由姿态层用矩阵完成——那是刚体旋转，矩阵比骨骼动画更省事也更稳。</li>
 *  </ul> */
public final class PlayerAnimClient {
    /** 我们占用的动画层号（PlayerAnimator 的层是叠加上去的，取一个不常用的低位）。 */
    public static final int LAYER=8;
    /** 动作 → 动画名。 */
    public static final Map<Integer,String> ANIMATIONS=Map.of(
        DismantleMotionRules.BACKFLIP,"backflip",
        DismantleMotionRules.FLYING_KICK,"flying_kick",
        DismantleMotionRules.SPIN,"spin_slash",
        DismantleMotionRules.KNEEL,"kneel",
        DismantleMotionRules.AIR_PUNCH,"air_punch",
        DismantleMotionRules.SLIDE,"slide",
        DismantleMotionRules.GROUND_SMASH,"ground_smash");
    /** 简易领域（长按维持）用的循环动画。 */
    public static final String SIMPLE_DOMAIN="simple_domain";

    /** 一把玩家与它当前的动画层（玩家下线/换维度时清掉）。 */
    private static final Map<UUID,Layer> LAYERS=new HashMap<>();
    /** 由动作渲染层每帧调用：告诉某个玩家该播哪段动画、播到第几刻。 */
    public static void play(AbstractClientPlayer player,int motionKind,double age){
        String name=ANIMATIONS.get(motionKind);
        if(name==null){stop(player);return;}
        set(player,name,age,false);
    }
    /** 简易领域：循环播放"两手上举"。 */
    public static void playLoop(AbstractClientPlayer player,String name,double age){set(player,name,age,true);}
    public static void stop(AbstractClientPlayer player){Layer l=LAYERS.get(player.getUUID());if(l!=null)l.setAnimation(null);}

    private static void set(AbstractClientPlayer player,String name,double age,boolean loop){
        KeyframeAnimation data=animation(name);
        if(data==null){stop(player);return;}
        Layer layer=layer(player);
        if(layer==null)return;
        layer.play(data,age,loop);
    }

    /** 动画数据按名字缓存（PlayerAnimator 从资源包加载，找不到就返回 null —— 缺前置也不会崩）。 */
    private static final Map<String,KeyframeAnimation> CACHE=new HashMap<>();
    private static KeyframeAnimation animation(String name){
        return CACHE.computeIfAbsent(name,k->{
            try{
                return PlayerAnimationRegistry.getAnimation(new ResourceLocation("sukuna",k));
            }catch(Throwable t){
                System.err.println("[SUKUNA] PlayerAnimator 动画读取失败 "+k+"："+t);
                return null;
            }
        });
    }
    private static Layer layer(AbstractClientPlayer player){
        try{
            Layer cached=LAYERS.get(player.getUUID());
            if(cached!=null)return cached;
            AnimationStack stack=PlayerAnimationAccess.getPlayerAnimLayer(player);
            if(stack==null)return null;
            Layer layer=new Layer();
            stack.addAnimLayer(LAYER,layer);
            LAYERS.put(player.getUUID(),layer);
            return layer;
        }catch(Throwable t){ // 没装 PlayerAnimator：静默降级到内置姿态层
            return null;
        }
    }
    public static void clear(){
        LAYERS.clear();
    }
    /** 玩家下线/换维度时清层。 */
    public static void forget(UUID id){
        LAYERS.remove(id);
    }
    /** 动画层：包一个 {@link KeyframeAnimationPlayer}，时间轴按"动作已过的刻数"对齐。
     *
     *  <p>{@code KeyframeAnimationPlayer} 自己会用 {@code currentTick} 推进，而我们的动作时长以
     *  服务端 {@code DismantleMotionRules} 为准；两者一致时直接让它自走，不一致时用反射把刻度钉住
     *  （拿不到字段就退化为自走，不会崩）。</p> */
    private static final class Layer extends ModifierLayer<KeyframeAnimationPlayer> {
        private KeyframeAnimation data;
        void play(KeyframeAnimation data,double age,boolean loop){
            if(this.data!=data){this.data=data;setAnimation(new KeyframeAnimationPlayer(data));}
            KeyframeAnimationPlayer player=getAnimation();
            if(player==null)return;
            double end=Math.max(1,loop?data.endTick:data.stopTick);
            double t=loop?age%end:Math.min(age,end);
            pin(player,t);
        }
        /** 把播放位置钉到第 t 刻（尽力而为：字段名在 1.0.2 里是 currentTick/tickDelta）。 */
        private static void pin(KeyframeAnimationPlayer player,double t){
            int tick=(int)Math.floor(t);
            try{
                var f=KeyframeAnimationPlayer.class.getDeclaredField("currentTick");
                f.setAccessible(true);f.setInt(player,tick);
                var d=KeyframeAnimationPlayer.class.getDeclaredField("tickDelta");
                d.setAccessible(true);d.setFloat(player,(float)(t-tick));
            }catch(Throwable ignored){/* 反射不可用就让播放器自走 */}
        }
        @Override public boolean isActive(){return getAnimation()!=null&&getAnimation().isActive();}
    }
    private PlayerAnimClient(){}
}
