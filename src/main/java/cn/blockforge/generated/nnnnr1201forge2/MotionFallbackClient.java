package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import java.util.function.DoubleFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

/** 新动作的统一兜底入口：登记姿态、收到服务端许可后播放；不产生攻击、扣费或位移。 */
public final class MotionFallbackClient {
    public record Frame(DismantlePose.Pose pose,double flip,double spin,double roll) {
        public Frame(DismantlePose.Pose pose){this(pose,0,0,0);}
        boolean valid(){
            return pose!=null&&MotionSafety.finite(flip,spin,roll,pose.sink(),pose.body(),pose.head(),
                pose.leftArm(),pose.rightArm(),pose.spread(),pose.leftLeg(),pose.rightLeg(),pose.legZ(),pose.leftKnee(),pose.rightKnee());
        }
    }
    private record Definition(int duration,DoubleFunction<Frame> sampler){}
    private static final class Clip {
        final AbstractClientPlayer player;final UUID token;final String name;final Definition definition;
        double age;long updated;
        Clip(AbstractClientPlayer p,UUID token,String name,Definition definition,double age,long now){
            player=p;this.token=token;this.name=name;this.definition=definition;this.age=age;updated=now;
        }
    }
    private static final Map<String,Definition> definitions=new HashMap<>();
    private static final Map<Integer,Clip> clips=new HashMap<>();
    private static final Set<String> warned=new HashSet<>();
    /** 只在客户端初始化时登记一次；同名覆盖用于开发时更新动作。时长单位为游戏刻。 */
    public static void register(String name,int duration,DoubleFunction<Frame> sampler){
        if(name==null||name.isBlank()||duration<1||duration>12000||sampler==null)throw new IllegalArgumentException("动作名称、时长或姿态函数无效");
        definitions.put(name,new Definition(duration,sampler));warned.remove(name);
    }
    /** 从已验证的客户端收包回调调用。同一次动作使用相同 token，重复心跳只校时不重播。 */
    public static boolean play(AbstractClientPlayer player,String name,UUID token,double age){
        var mc=Minecraft.getInstance();Definition definition=definitions.get(name);
        if(player==null||token==null||definition==null||mc.level==null||player.level()!=mc.level||!Double.isFinite(age)||age<0||age>=definition.duration())return false;
        DismantleMotionClient.watch(player);
        long now=mc.level.getGameTime();Clip clip=clips.get(player.getId());
        if(clip!=null&&clip.player==player&&clip.token.equals(token)&&clip.name.equals(name)){
            clip.age=MotionSafety.synchronize(clip.age+Math.max(0,now-clip.updated),age,definition.duration());clip.updated=now;
        }else clips.put(player.getId(),new Clip(player,token,name,definition,age,now));
        return true;
    }
    public static void stop(int id,UUID token){Clip c=clips.get(id);if(c!=null&&c.token.equals(token))clips.remove(id);}
    private static Clip current(int id){
        Clip c=clips.get(id);if(c==null)return null;var mc=Minecraft.getInstance();
        if(mc.level==null||c.player.level()!=mc.level||mc.level.getEntity(id)!=c.player||!c.player.isAlive()
            ||mc.level.getGameTime()<c.updated||c.age+mc.level.getGameTime()-c.updated>=c.definition.duration()){
            clips.remove(id);return null;
        }
        return c;
    }
    public static boolean active(int id){return current(id)!=null;}
    public static Frame sample(int id,float partial){
        Clip c=current(id);if(c==null)return null;
        double age=MotionSafety.age(c.age+Minecraft.getInstance().level.getGameTime()-c.updated,partial,c.definition.duration());
        try{Frame frame=c.definition.sampler().apply(age);if(frame!=null&&frame.valid())return frame;}
        catch(RuntimeException|LinkageError ex){if(warned.add(c.name))com.mojang.logging.LogUtils.getLogger().warn("[动作兜底] {} 姿态异常，切换通用动作",c.name,ex);}
        if(warned.add(c.name))com.mojang.logging.LogUtils.getLogger().warn("[动作兜底] {} 返回空值或非法关节，切换通用动作",c.name);
        // 资源/姿态函数坏掉时仍有完整起手收招，绝不把 NaN 写入共用模型。
        return emergency(age,c.definition.duration());
    }
    public static Frame emergency(double age,int duration){
        double p=MotionSafety.age(age,0,duration)/Math.max(1,duration),weight=Math.sin(p*Math.PI);
        return new Frame(new DismantlePose.Pose(.12*weight,14*weight,-8*weight,-55*weight,-105*weight,
            14*weight,-25*weight,15*weight,0,35*weight,20*weight));
    }
    public static void tick(){for(int id:List.copyOf(clips.keySet()))current(id);}
    public static void clear(){clips.clear();warned.clear();}
    private MotionFallbackClient(){}
}
