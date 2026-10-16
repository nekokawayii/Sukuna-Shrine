package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;
import java.lang.reflect.*;
import java.util.*;
import com.mojang.logging.LogUtils;

/** 对照 Photon 1.1.17 的公开 API 调用；隔离客户端特效依赖。 */
public final class PhotonBridge {
    private static final String ROOT="com.lowdragmc.photon.client.";
    private static boolean broken;
    private static final Map<String,Method> methods=new HashMap<>();
    public record Handle(Object runtime,Object root,Object effect,BlockPos anchor) {}
    private static Class<?> type(String n)throws Exception{return Class.forName(ROOT+n);}
    private static Object call(Object o,String name,Object...args)throws Exception{
        Class<?> c=o instanceof Class<?> cl?cl:o.getClass();String key=c.getName()+"/"+name+"/"+Arrays.toString(Arrays.stream(args).map(a->a.getClass().getName()).toArray());
        Method m=methods.get(key);
        if(m==null){outer:for(Method v:c.getMethods()){if(!v.getName().equals(name)||v.getParameterCount()!=args.length)continue;Class<?>[] p=v.getParameterTypes();for(int i=0;i<p.length;i++)if(!wrap(p[i]).isInstance(args[i]))continue outer;m=v;break;}if(m==null)throw new NoSuchMethodException(key);methods.put(key,m);}
        return m.invoke(o instanceof Class<?>?null:o,args);
    }
    private static Class<?> wrap(Class<?> c){if(c==int.class)return Integer.class;if(c==boolean.class)return Boolean.class;if(c==double.class)return Double.class;if(c==float.class)return Float.class;return c;}
    private static Object field(Object o,String n)throws Exception{return o.getClass().getField(n).get(o);}
    private static Object number(Number n)throws Exception{return call(type("gameobject.emitter.data.number.NumberFunction"),"constant",n);}
    private static Object triple(double x,double y,double z)throws Exception{return type("gameobject.emitter.data.number.NumberFunction3").getConstructor(Number.class,Number.class,Number.class).newInstance(x,y,z);}
    @SuppressWarnings("unchecked")
    private static final String TEX_FLAME="textures/effect/flame.png",TEX_DUST="textures/effect/dust.png",
        TEX_LINE="textures/effect/streak.png",TEX_RIFT="textures/effect/void_rift.png";
    public static Handle spawn(Vec3 pos,int color,double size,int count,int life){return particle(pos,color,size,count,life,textureFor(color),0,size*0.3);}
    /** 虚空用的裂痕细条：**只给虚空/剑气系用**，火焰系请用 {@link #thread}（否则会出现"虚空捌"那种紫色裂痕）。 */
    public static Handle needle(Vec3 pos,int life,float angle){return particle(pos,0xFFFFFFFF,3.2,1,life,TEX_RIFT,angle,0);}
    /** **丝线**：streak.png 的柔和亮线（可指定颜色与粗细），金橙色时就是参考图里那种火焰拉丝。 */
    public static Handle thread(Vec3 pos,int color,double size,int life,float angle){return particle(pos,color,size,1,life,TEX_LINE,angle,0);}
    /** **粒子云**（火焰）：在 {@code spread} 见方的盒子里撒 {@code count} 颗 {@code size} 大小的火焰软粒子。 */
    public static Handle cloud(Vec3 pos,int color,double spread,double size,int count,int life){return particle(pos,color,size,count,life,TEX_FLAME,0,spread);}
    /** **粒子云**（烟尘）：同样的东西换成 dust 贴图，用来铺"周身方块镀色"那种大面积薄雾。 */
    public static Handle smoke(Vec3 pos,int color,double spread,double size,int count,int life){return particle(pos,color,size,count,life,TEX_DUST,0,spread);}
    private static String textureFor(int color){return (color>>>24)<200?TEX_DUST:TEX_FLAME;}
    private static Handle particle(Vec3 pos,int color,double size,int count,int life,String texture,float angle,double spread){
        if(broken||Minecraft.getInstance().level==null)return null;
        try{
            Object emitter=type("gameobject.emitter.particle.ParticleEmitter").getConstructor().newInstance();Object config=field(emitter,"config");
            boolean line=TEX_LINE.equals(texture)||TEX_RIFT.equals(texture);
            call(config,"setDuration",1);call(config,"setLooping",false);call(config,"setMaxParticles",count);call(config,"setStartLifetime",number(life));call(config,"setStartSpeed",number(line?0:0.04));
            call(config,"setStartSize",triple(size,size,size));call(field(config,"physics"),"setHasCollision",false);call(config,"setStartRotation",triple(0,0,angle));
            call(config,"setStartColor",call(type("gameobject.emitter.data.number.NumberFunction"),"color",color));
            Object shape=field(config,"shape");call(shape,"setScale",triple(spread,spread,spread));
            Object renderer=field(config,"renderer");call(renderer,"setBloomEffect",true);call(renderer,"setBloomColor",color);
            Object material=field(config,"material");call(material,"setCull",false);
            Object tex=type("gameobject.emitter.data.material.TextureMaterial").getConstructor(ResourceLocation.class).newInstance(new ResourceLocation("sukuna",texture));call(material,"setMaterial",tex);
            Object emission=field(config,"emission");call(emission,"setEmissionRate",number(0));Object burst=type("gameobject.emitter.data.EmissionSetting$Burst").getConstructor().newInstance();
            burst.getClass().getField("time").set(burst,0);burst.getClass().getField("cycles").set(burst,1);burst.getClass().getField("interval").set(burst,1);burst.getClass().getField("probability").set(burst,1.0f);call(burst,"setCount",number(count));((List<Object>)call(emission,"getBursts")).add(burst);
            Object fx=type("fx.FX").getConstructor().newInstance();call(fx,"setFxLocation",new ResourceLocation("sukuna","cursed_flame"));((List<Object>)call(call(fx,"getMainFX"),"objects")).add(emitter);
            BlockPos bp=BlockPos.containing(pos);Object effect=type("fx.BlockEffect").getConstructor(type("fx.FX"),Level.class,BlockPos.class).newInstance(fx,Minecraft.getInstance().level,bp);
            call(effect,"setAllowMulti",true);call(effect,"setOffset",pos.x-bp.getX()-0.5,pos.y-bp.getY()-0.5,pos.z-bp.getZ()-0.5);call(effect,"start");Object runtime=call(effect,"getRuntime");return new Handle(runtime,call(runtime,"getRoot"),effect,bp);
        }catch(Exception e){fail(e);return null;}
    }
    public static void move(Handle h,Vec3 p){if(h==null||broken)return;try{call(h.root,"updatePos",new Vector3f((float)p.x,(float)p.y,(float)p.z));}catch(Exception e){fail(e);}}
    public static void stop(Handle h){if(h==null)return;try{call(h.runtime,"destroy",true);Object raw=type("fx.BlockEffect").getField("CACHE").get(null);if(raw instanceof Map<?,?> cache){Object entry=cache.get(h.anchor);if(entry instanceof List<?> list){list.remove(h.effect);if(list.isEmpty())cache.remove(h.anchor);}}}catch(Exception e){fail(e);}}
    private static void fail(Exception e){if(!broken){broken=true;LogUtils.getLogger().error("宿傩 Photon 特效初始化失败",e);var p=Minecraft.getInstance().player;if(p!=null)p.displayClientMessage(Component.literal("Photon 特效初始化失败，请使用 Forge 1.20.1 的 Photon 1.1.17，并查看日志"),false);}}
}
