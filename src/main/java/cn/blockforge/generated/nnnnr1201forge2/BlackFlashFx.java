package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Method;
import java.util.*;

/** 黑闪表现层（客户端）：播放用户提供的 4 个 Photon 特效文件。
 *  阶段对应：起手闪光 blackflasheffect → 命中爆发 blackflash → 终结冲击 blackflashfinal → 收尾火花 blackflashfinalspark。
 *  资源路径为 {@code assets/sukuna/fx/<名>.fx}（Photon 的 FXHelper.FX_PATH="fx/"，内部拼成 {@code fx/<名>.fx}）。
 *  前置 Photon 缺失或 .fx 资源缺失时静默降级：只记一条日志，绝不抛出、不刷屏。 */
public final class BlackFlashFx {
    private static final String ROOT="com.lowdragmc.photon.client.";
    /** 阶段顺序 = .fx 文件名（不含扩展名）。 */
    private static final String[] STAGE={"blackflasheffect","blackflash","blackflashfinal","blackflashfinalspark"};
    /** 每阶段的延迟刻数：起手与命中同刻，终结第 3 刻，收尾火花第 9 刻。 */
    private static final int[] DELAY={0,0,3,9};
    private static final Map<String,Method> METHODS=new HashMap<>();
    /** 资源缺失的 .fx 只尝试一次，避免每次触发都抛异常。 */
    private static final Set<String> MISSING=new HashSet<>();
    private static boolean broken;

    // ------------------------------------------------------------ 反射底座（对照 Photon 1.1.17 公开 API）
    private static Class<?> type(String n)throws Exception{return Class.forName(ROOT+n);}
    private static Object call(Object o,String name,Object...args)throws Exception{
        Class<?> c=o instanceof Class<?> cl?cl:o.getClass();
        String key=c.getName()+"/"+name+"/"+Arrays.toString(Arrays.stream(args).map(a->a.getClass().getName()).toArray());
        Method m=METHODS.get(key);
        if(m==null){
            outer:for(Method v:c.getMethods()){if(!v.getName().equals(name)||v.getParameterCount()!=args.length)continue;Class<?>[] p=v.getParameterTypes();for(int i=0;i<p.length;i++)if(!wrap(p[i]).isInstance(args[i]))continue outer;m=v;break;}
            if(m==null)throw new NoSuchMethodException(key);
            METHODS.put(key,m);
        }
        return m.invoke(o instanceof Class<?>?null:o,args);
    }
    private static Class<?> wrap(Class<?> c){if(c==int.class)return Integer.class;if(c==boolean.class)return Boolean.class;if(c==double.class)return Double.class;if(c==float.class)return Float.class;return c;}

    /** 黑闪触发：以命中点为中心铺开四段 .fx。每次触发记一行状态日志，不随刻刷屏。 */
    public static void play(Vec3 pos){
        if(Minecraft.getInstance().level==null)return;
        LogUtils.getLogger().info("[黑闪] 触发 · 命中点 "+String.format(Locale.ROOT,"(%.1f, %.1f, %.1f)",pos.x,pos.y,pos.z)+" · 播放 "+String.join(" → ",STAGE));
        for(int i=0;i<STAGE.length&&!broken;i++)spawn(STAGE[i],DELAY[i],pos);
    }
    private static void spawn(String name,int delay,Vec3 pos){
        if(MISSING.contains(name))return;
        try{
            Object fx=call(type("fx.FXHelper"),"getFX",new ResourceLocation("sukuna",name));
            BlockPos bp=BlockPos.containing(pos);
            Object effect=type("fx.BlockEffect").getConstructor(type("fx.FX"),Level.class,BlockPos.class).newInstance(fx,Minecraft.getInstance().level,bp);
            if(delay>0)call(effect,"setDelay",delay);
            call(effect,"setAllowMulti",true);
            call(effect,"setOffset",pos.x-bp.getX()-0.5,pos.y-bp.getY()-0.5,pos.z-bp.getZ()-0.5);
            call(effect,"start");
        }catch(Throwable t){
            if(t instanceof ClassNotFoundException||t instanceof NoClassDefFoundError){
                if(!broken){broken=true;LogUtils.getLogger().warn("[黑闪] 未安装 Photon，黑闪特效已跳过（音效不受影响）");}
            }else if(MISSING.add(name))LogUtils.getLogger().warn("[黑闪] 特效文件缺失或不可用，已跳过：sukuna:fx/{}.fx",name);
        }
    }
    private BlackFlashFx(){}
}
