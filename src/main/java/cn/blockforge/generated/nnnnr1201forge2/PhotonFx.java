package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.lang.reflect.Proxy;
import java.util.*;

/** 五条悟全套特效：全部由 Photon 发射器在客户端即时构建，不依赖 .fx 资源文件。
 *  对照 Photon 1.1.17：ParticleEmitter/ParticleConfig、BeamEmitter/BeamConfig、FX/FXRuntime。 */
public final class PhotonFx {
    /** 发射器形状：球体内部 / 球壳 / 圆环（XZ 平面）/ 点 / 圆柱（轴向 +Y） */
    public static final int SPHERE=0,SHELL=1,RING=2,DOT=3,CYLINDER=4;
    public static final int MODE_BILLBOARD=0,MODE_VERTICAL=1;
    private static final String ROOT="com.lowdragmc.photon.client.";
    private static final Map<String,Object> ENUMS=new HashMap<>();
    private static boolean broken;
    public record Handle(Object runtime,Object root) {}
    private record Emitter(Object object,Object config) {}
    /** rot 可以是 Vector3f（欧拉弧度）或 Quaternionf（任意朝向，用于把局部 +Y 对准任意方向）。 */
    private record Placement(Emitter emitter,Vec3 local,Object rot) {
        Placement(Emitter emitter,Vec3 local){this(emitter,local,null);}
    }
    private static Placement P(Emitter e,Vec3 local){return new Placement(e,local,null);}
    private static Placement P(Emitter e,Vec3 local,double rotX,double rotY,double rotZ){return new Placement(e,local,new Vector3f((float)rotX,(float)rotY,(float)rotZ));}
    private static Placement PA(Emitter e,Vec3 local,Vec3 dir){return new Placement(e,local,new org.joml.Quaternionf().rotateTo(new Vector3f(0,1,0),new Vector3f((float)dir.x,(float)dir.y,(float)dir.z)));}

    // ------------------------------------------------------------ 反射底座
    private static Class<?> type(String n)throws Exception{return Class.forName(ROOT+n);}
    private static Object call(Object o,String name,Object...args)throws Exception{
        Class<?> c=o instanceof Class<?> cl?cl:o.getClass();
        outer:for(var m:c.getMethods()){if(!m.getName().equals(name)||m.getParameterCount()!=args.length)continue;Class<?>[] p=m.getParameterTypes();for(int i=0;i<p.length;i++)if(!wrap(p[i]).isInstance(args[i]))continue outer;return m.invoke(o instanceof Class<?>?null:o,args);}
        throw new NoSuchMethodException(c.getName()+"/"+name+"/"+args.length);
    }
    private static Class<?> wrap(Class<?> c){if(c==int.class)return Integer.class;if(c==boolean.class)return Boolean.class;if(c==double.class)return Double.class;if(c==float.class)return Float.class;if(c==long.class)return Long.class;return c;}
    private static Object field(Object o,String n)throws Exception{return o.getClass().getField(n).get(o);}
    private static void setField(Object o,String n,Object v)throws Exception{o.getClass().getField(n).set(o,v);}
    @SuppressWarnings("unchecked")
    private static Object enumOf(String binary,String name)throws Exception{String key=binary+"/"+name;Object v=ENUMS.get(key);if(v==null){v=Enum.valueOf((Class<? extends Enum>)Class.forName(binary),name);ENUMS.put(key,v);}return v;}
    /** 单个可选参数设置失败不能拖垮整套特效：只记录一次，其余照常构建。 */
    private static void tryCall(Object o,String name,Object...args){try{call(o,name,args);}catch(Throwable t){note(name,t);}}
    private static void note(String what,Throwable t){
        long now=System.currentTimeMillis();
        if(now-lastNote<3000)return;
        lastNote=now;
        com.mojang.logging.LogUtils.getLogger().warn("五条悟特效参数 " + what + " 设置失败（已跳过该参数，特效继续构建）",t);
    }
    private static long lastNote;
    private static Object num(Number n)throws Exception{return call(type("gameobject.emitter.data.number.NumberFunction"),"constant",n);}
    private static Object colorNum(int argb)throws Exception{return call(type("gameobject.emitter.data.number.NumberFunction"),"color",argb);}
    private static Object nf3(Object x,Object y,Object z)throws Exception{return type("gameobject.emitter.data.number.NumberFunction3").getConstructor(Number.class,Number.class,Number.class).newInstance(x,y,z);}
    private static Object nums(double x,double y,double z)throws Exception{return nf3(x,y,z);}

    // ------------------------------------------------------------ 发射器装配
    private static Emitter emitter(int shape,double radius,double thickness)throws Exception{
        Object e=type("gameobject.emitter.particle.ParticleEmitter").getConstructor().newInstance();
        Object config=field(e,"config");
        String shapeName=switch(shape){case SPHERE,SHELL->"Sphere";case RING->"Circle";case CYLINDER->"Cylinder";default->"Dot";};
        Object s=type("gameobject.emitter.data.shape."+shapeName).getConstructor().newInstance();
        if(shape!=DOT){
            tryCall(s,"setRadius",(float)radius);
            // 圆环也支持"带宽"：0.55 = 从 0.45R 到 R 的一圈环带（银河带）；1 = 实心圆盘（吸积盘）
            tryCall(s,"setRadiusThickness",(float)(shape==DOT?1:Math.max(0.02,thickness)));
            tryCall(s,"setArc",360f);
        }
        tryCall(field(config,"shape"),"setShape",s);
        tryCall(config,"setDuration",200);tryCall(config,"setLooping",false);tryCall(config,"setMaxParticles",700);
        tryCall(config,"setStartSpeed",num(0));tryCall(config,"setStartLifetime",num(20));
        tryCall(config,"setStartSize",nums(0.8,0.8,0.8));tryCall(config,"setStartColor",colorNum(0xFFFFFFFF));
        tryCall(config,"setStartRotation",nums(0,0,0));
        tryCall(config,"setSimulationSpace",enumOf(ROOT+"gameobject.emitter.particle.ParticleConfig$Space","Local"));
        tryCall(field(config,"physics"),"setHasCollision",false);
        Object material=field(config,"material");
        tryCall(material,"setCull",false);tryCall(material,"setDepthTest",true);
        return new Emitter(e,config);
    }
    private static void lifetime(Object config,double life)throws Exception{
        int ticks=(int)Math.max(4,Math.min(20000,life));
        tryCall(config,"setDuration",ticks);tryCall(config,"setStartLifetime",num(ticks));
    }
    /** 发射器存活刻数（到点后自行从粒子引擎移除，不需要句柄）。 */
    private static void duration(Object config,double ticks){tryCall(config,"setDuration",(int)Math.max(1,Math.min(20000,ticks)));}
    /** 单颗粒子存活刻数：短命粒子 = 拖尾，长命粒子 = 固定形体。 */
    private static void particleLife(Object config,double ticks)throws Exception{tryCall(config,"setStartLifetime",num(Math.max(1,ticks)));}
    /**
     * 短命光斑：发射器只活 duration 刻、粒子只活 life 刻，之后自行消失。
     * 不做句柄移动也不做续期，所以既不会"跳"也不会因重建而闪；飞行体每刻在真实位置点一颗即可。
     */
    public static void spark(Vec3 pos,double radius,double size,int color,int bloom,String texture,boolean additive,int duration,int life,double rate,double orbit){
        try{
            Emitter e=emitter(SPHERE,Math.max(0.05,radius),1);Object c=e.config();
            duration(c,duration);particleLife(c,life);size(c,Math.max(0.05,size));rate(c,Math.max(1,rate));
            if(orbit!=0)orbit(c,orbit);
            colour(c,color);render(c,MODE_BILLBOARD,texture,bloom,additive);
            start(e,pos);
        }catch(Throwable t){fail(t);}
    }
    private static void size(Object config,double size)throws Exception{tryCall(config,"setStartSize",nums(size,size,size));}
    private static void stretch(Object config,double along,double across)throws Exception{tryCall(config,"setStartSize",nums(along,across,across));}
    private static void rate(Object config,double perTick)throws Exception{
        Object emission=field(config,"emission");tryCall(emission,"setEmissionRate",num(perTick));
        tryCall(emission,"setEmissionMode",enumOf(ROOT+"gameobject.emitter.data.EmissionSetting$Mode","Random"));
    }
    @SuppressWarnings("unchecked")
    private static void burst(Object config,int count)throws Exception{
        Object emission=field(config,"emission");tryCall(emission,"setEmissionRate",num(0));
        Object b=type("gameobject.emitter.data.EmissionSetting$Burst").getConstructor().newInstance();
        setField(b,"time",0);setField(b,"cycles",1);setField(b,"interval",1);setField(b,"probability",1f);
        tryCall(b,"setCount",num(count));
        ((List<Object>)call(emission,"getBursts")).add(b);
    }
    private static void colour(Object config,int argb)throws Exception{tryCall(config,"setStartColor",colorNum(argb));}
    private static void render(Object config,int mode,String texture,int bloom)throws Exception{render(config,mode,texture,bloom,true);}
    /** additive=false 走正常 alpha 混合，黑色才会真正压暗背景（黑洞核心用）。 */
    private static void render(Object config,int mode,String texture,int bloom,boolean additive)throws Exception{
        if(mode==MODE_VERTICAL)tryCall(field(config,"renderer"),"setRenderMode",enumOf(ROOT+"gameobject.emitter.data.RendererSetting$Particle$Mode","Vertical"));
        if(bloom!=0){Object r=field(config,"renderer");tryCall(r,"setBloomEffect",true);tryCall(r,"setBloomColor",bloom);}
        Object material=field(config,"material");
        tryCall(material,"setMaterial",type("gameobject.emitter.data.material.TextureMaterial").getConstructor(ResourceLocation.class).newInstance(new ResourceLocation("sukuna",texture)));
        Object blend=call(material,"getBlendMode");
        tryCall(blend,"setEnableBlend",true);
        if(additive){
            // 目标因子在 Photon 里是 DestFactor 枚举，传 SourceFactor 会 NoSuchMethod
            Object one=enumOf("com.mojang.blaze3d.platform.GlStateManager$SourceFactor","ONE");
            Object dstOne=enumOf("com.mojang.blaze3d.platform.GlStateManager$DestFactor","ONE");
            tryCall(blend,"setSrcColorFactor",one);tryCall(blend,"setDstColorFactor",dstOne);
            tryCall(blend,"setSrcAlphaFactor",one);tryCall(blend,"setDstAlphaFactor",dstOne);
            tryCall(blend,"setBlendFunc",enumOf(ROOT+"gameobject.emitter.data.material.BlendMode$BlendFuc","ADD"));
        }else{
            tryCall(blend,"setSrcColorFactor",enumOf("com.mojang.blaze3d.platform.GlStateManager$SourceFactor","SRC_ALPHA"));
            tryCall(blend,"setDstColorFactor",enumOf("com.mojang.blaze3d.platform.GlStateManager$DestFactor","ONE_MINUS_SRC_ALPHA"));
            tryCall(blend,"setSrcAlphaFactor",enumOf("com.mojang.blaze3d.platform.GlStateManager$SourceFactor","SRC_ALPHA"));
            tryCall(blend,"setDstAlphaFactor",enumOf("com.mojang.blaze3d.platform.GlStateManager$DestFactor","ONE_MINUS_SRC_ALPHA"));
            tryCall(blend,"setBlendFunc",enumOf(ROOT+"gameobject.emitter.data.material.BlendMode$BlendFuc","ADD"));
        }
    }
    private static void orbit(Object config,double angular)throws Exception{
        Object v=field(config,"velocityOverLifetime");tryCall(v,"setEnable",true);
        tryCall(v,"setOrbitalMode",enumOf(ROOT+"gameobject.emitter.data.VelocityOverLifetimeSetting$OrbitalMode","AngularVelocity"));
        tryCall(v,"setOrbital",nums(0,angular,0));tryCall(v,"setOffset",nums(0,0,0));
    }
    private static void speed(Object config,double perTick)throws Exception{tryCall(config,"setStartSpeed",num(perTick));}
    private static void rotation(Object config,double x,double y,double z)throws Exception{tryCall(config,"setStartRotation",nums(x,y,z));}
    private static void shapeScale(Object config,double x,double y,double z)throws Exception{tryCall(field(config,"shape"),"setScale",nums(x,y,z));}

    // ------------------------------------------------------------ 运行时装配
    private static Object effectProxy()throws Exception{
        Class<?> iface=type("fx.IEffect");
        return Proxy.newProxyInstance(PhotonFx.class.getClassLoader(),new Class<?>[]{iface},(proxy,method,args)->switch(method.getName()){
            case "getLevel"->Minecraft.getInstance().level;
            case "toString"->"sukuna:photon_fx";
            case "hashCode"->System.identityHashCode(proxy);
            case "equals"->proxy==args[0];
            default->null;
        });
    }
    private static Handle start(List<Placement> placements,Vec3 pos){
        if(broken||placements.isEmpty()||Minecraft.getInstance().level==null)return null;
        try{
            Object fx=type("fx.FX").getConstructor().newInstance();
            call(fx,"setFxLocation",new ResourceLocation("sukuna","gojo_fx"));
            @SuppressWarnings("unchecked") List<Object> objects=(List<Object>)call(call(fx,"getMainFX"),"objects");
            for(Placement p:placements)objects.add(p.emitter().object());
            // 用内部 runtime（不拷贝对象）：这样每个发射器的独立位置/朝向才真的生效，
            // 一个句柄里就能放下"银河 + 星空 + 白墨 + 彩线 + 吸积盘"整块画布，每刻只需移动一个 root。
            Object runtime=call(fx,"createInternalRuntime");
            call(runtime,"emmit",effectProxy());
            Object root=call(runtime,"getRoot");
            call(root,"updatePos",new Vector3f((float)pos.x,(float)pos.y,(float)pos.z));
            for(Placement p:placements){
                if(p.local()!=Vec3.ZERO)call(p.emitter().object(),"updatePos",new Vector3f((float)p.local().x,(float)p.local().y,(float)p.local().z));
                if(p.rot()!=null)call(p.emitter().object(),"updateRotation",p.rot());
            }
            return new Handle(runtime,root);
        }catch(Throwable t){fail(t);return null;}
    }
    private static Handle start(Emitter e,Vec3 pos){return start(List.of(new Placement(e,Vec3.ZERO)),pos);}
    public static void move(Handle h,Vec3 p){if(h==null||broken)return;try{call(h.root,"updatePos",new Vector3f((float)p.x,(float)p.y,(float)p.z));}catch(Throwable t){fail(t);}}
    public static void stop(Handle h){if(h==null||broken)return;try{call(h.runtime,"destroy",true);}catch(Throwable t){fail(t);}}
    public static boolean ready(){return !broken&&Minecraft.getInstance().level!=null;}
    private static long lastFail;
    /** 只有 Photon 本体缺失才整体停用；单个特效构建失败只记录，不影响其他招式。 */
    private static void fail(Throwable t){
        boolean missing=t instanceof ClassNotFoundException||t instanceof NoClassDefFoundError;
        if(missing){if(!broken){broken=true;com.mojang.logging.LogUtils.getLogger().error("未找到 Photon，五条悟特效无法显示",t);var p=Minecraft.getInstance().player;if(p!=null)p.displayClientMessage(net.minecraft.network.chat.Component.literal("Photon 特效不可用：请安装 Photon 1.1.17"),false);}return;}
        long now=System.currentTimeMillis();
        if(now-lastFail>2000){lastFail=now;com.mojang.logging.LogUtils.getLogger().error("五条悟特效构建失败（本条只记录，不停用其他特效）",t);}
    }

    // ------------------------------------------------------------ 苍
    /** 苍：蓝色三维光球，外围白色光点像星环一样环绕。 */
    public static Handle blue(Vec3 pos,double radius,int life){
        try{
            Emitter core=emitter(SPHERE,radius,1);Object c=core.config();
            lifetime(c,life);size(c,Math.max(0.5,radius*0.5));rate(c,Math.max(1.5,radius*3.5));colour(c,0x993A8CFF);
            render(c,MODE_BILLBOARD,"textures/effect/glow.png",0xFF12304F);
            Emitter halo=emitter(SHELL,radius*0.92,0.05);Object h=halo.config();
            lifetime(h,life);size(h,Math.max(0.25,radius*0.28));rate(h,Math.max(1,radius*1.6));colour(h,0x3A4FC8FF);
            render(h,MODE_BILLBOARD,"textures/effect/glow.png",0xFF2A5A85);
            Emitter stars=emitter(RING,radius*1.35,1);Object s=stars.config();
            lifetime(s,life);stretch(s,0.18,0.18);rate(s,Math.max(1,radius*1.2));orbit(s,4.5);colour(s,0xAAFFFFFF);
            render(s,MODE_BILLBOARD,"textures/effect/glow.png",0);
            return start(List.of(new Placement(core,Vec3.ZERO),new Placement(halo,Vec3.ZERO),new Placement(stars,Vec3.ZERO)),pos);
        }catch(Throwable t){fail(t);return null;}
    }
    // ------------------------------------------------------------ 赫
    /** 赫：红色小光球，本体跟随句柄移动。 */
    public static Handle red(Vec3 pos,double radius,int life){
        try{
            Emitter core=emitter(SPHERE,radius,1);Object c=core.config();
            lifetime(c,life);size(c,radius*0.7);rate(c,26);colour(c,0xC0FF2A16);
            render(c,MODE_BILLBOARD,"textures/effect/glow.png",0xFF7A1206);
            Emitter trail=emitter(DOT,0,0);Object t=trail.config();
            lifetime(t,Math.max(4,life*0.6));size(t,radius*0.5);rate(t,14);colour(t,0x66FF5B3A);
            render(t,MODE_BILLBOARD,"textures/effect/glow.png",0xFF6A1A08);
            return start(List.of(new Placement(core,Vec3.ZERO),new Placement(trail,Vec3.ZERO)),pos);
        }catch(Throwable t){fail(t);return null;}
    }
    /** 赫·最大功率蓄力：手中红球 + 白色线条向球心飞入。 */
    public static Handle redCharge(Vec3 pos,int life){
        try{
            Emitter core=emitter(SPHERE,0.45,1);Object c=core.config();
            lifetime(c,life);size(c,0.5);rate(c,20);colour(c,0xC0FF3A20);
            render(c,MODE_BILLBOARD,"textures/effect/glow.png",0xFF7A1206);
            Emitter inward=emitter(SHELL,2.6,0.04);Object in=inward.config();
            lifetime(in,life);stretch(in,0.05,0.36);speed(in,-1.6);rate(in,12);colour(in,0x99FFFFFF);
            render(in,MODE_BILLBOARD,"textures/effect/streak.png",0);
            return start(List.of(new Placement(core,Vec3.ZERO),new Placement(inward,Vec3.ZERO)),pos);
        }catch(Throwable t){fail(t);return null;}
    }
    /** 赫·最大功率发射：真正的三维红色圆柱（Cylinder 体积），白色环由 GojoBeamClient 自绘。 */
    public static Handle beam(Vec3 from,Vec3 to,double width,int life){
        try{
            Vec3 delta=to.subtract(from);double length=Math.max(0.6,delta.length());
            if(length<1e-4)return null;
            Vec3 dir=delta.normalize();
            double r0=Math.max(0.18,width),half=length*0.5;
            List<Placement> list=new ArrayList<>();
            // 核心柱：亮红、密集
            Emitter core=emitter(CYLINDER,r0,1);Object c=core.config();
            duration(c,Math.max(2,life));particleLife(c,Math.max(2,life));
            rate(c,Math.max(8,r0*160));size(c,Math.max(0.22,r0*0.85));colour(c,0xC8FF3A1A);
            // Cylinder 的局部 y 先按 scale.y 取随机再整体乘一次 scale.y，实际高度是 scale.y²，所以这里开方
            shapeScale(c,1,Math.sqrt(length),1);
            render(c,MODE_BILLBOARD,GLOW,0xFF8A1008);
            // 外壳：更粗更淡，做出体积感
            Emitter shell=emitter(CYLINDER,r0*1.85,0.25);Object s=shell.config();
            duration(s,Math.max(2,life));particleLife(s,Math.max(2,life));
            rate(s,Math.max(6,r0*70));size(s,Math.max(0.3,r0*1.25));colour(s,0x55FF6A30);
            shapeScale(s,1,Math.sqrt(length),1);
            render(s,MODE_BILLBOARD,GLOW,0xFF6A0A06);
            list.add(PA(core,dir.scale(half),dir));   // 局部 +Y 对准射线方向
            list.add(PA(shell,dir.scale(half),dir));
            return start(list,from);
        }catch(Throwable t){fail(t);return null;}
    }
    /** 细小光线：从球壳向球心收束的细长粒子（stretch 的 x=宽、y=高，所以要"窄而长"）。 */
    public static void ray(Vec3 pos,double shell,double thin,double longSide,int color,int bloom,boolean additive,int duration,int life,double rate,double speedPerTick){
        try{
            Emitter e=emitter(SHELL,Math.max(0.2,shell),0.04);Object c=e.config();
            duration(c,duration);particleLife(c,life);
            stretch(c,Math.max(0.008,thin),Math.max(0.06,longSide));
            rate(c,Math.max(1,rate));
            if(speedPerTick!=0)speed(c,speedPerTick);
            colour(c,color);render(c,MODE_BILLBOARD,STREAK,bloom,additive);
            start(e,pos);
        }catch(Throwable t){fail(t);}
    }
    /** 茈：紫黑球体，内部像银河一样转动。 */
    public static Handle purple(Vec3 pos,double radius,int life){
        try{
            Emitter galaxy=emitter(SPHERE,radius,1);Object g=galaxy.config();
            lifetime(g,life);size(g,radius*0.95);rate(g,Math.max(3,radius*4.5));orbit(g,1.2);colour(g,0xB4FFFFFF);
            render(g,MODE_BILLBOARD,"textures/effect/galaxy.png",0xFF3E1C63);
            Emitter shell=emitter(SHELL,radius*1.02,0.05);Object s=shell.config();
            lifetime(s,life);size(s,radius*0.7);rate(s,Math.max(2,radius*2.5));colour(s,0x445C2AFF);
            render(s,MODE_BILLBOARD,"textures/effect/glow.png",0xFF3E1A6E);
            Emitter edge=emitter(RING,radius*1.15,1);Object e=edge.config();
            lifetime(e,life);size(e,0.32);rate(e,Math.max(1,radius*1.2));orbit(e,2.0);colour(e,0x77E0B0FF);
            render(e,MODE_BILLBOARD,"textures/effect/glow.png",0);
            return start(List.of(new Placement(galaxy,Vec3.ZERO),new Placement(shell,Vec3.ZERO),new Placement(edge,Vec3.ZERO)),pos);
        }catch(Throwable t){fail(t);return null;}
    }
    /** 无量空处：远处置一层黑雾当边界 + 稀疏白色水墨，中央大片区域保持通透，怪才看得见。
     *  白边黑洞不在这里画（Photon 的叠加混合画不出纯黑），见 GojoVoidClient。 */
    public static Handle domain(Vec3 pos,double radius,int life){
        try{
            Emitter shell=emitter(SHELL,radius,0.05);Object o=shell.config();
            duration(o,life);particleLife(o,70);size(o,radius*0.14);rate(o,Math.max(2,radius*0.10));orbit(o,0.18);colour(o,0x3C02030A);
            render(o,MODE_BILLBOARD,"textures/effect/glow.png",0xFF1A0640);
            // 体内雾只铺 0.5R~R 的球壳，玩家周围 24 格不生成粒子
            Emitter fill=emitter(SHELL,radius*0.9,0.5);Object i=fill.config();
            duration(i,life);particleLife(i,70);size(i,radius*0.10);rate(i,Math.max(2,radius*0.06));orbit(i,-0.22);colour(i,0x24010206);
            render(i,MODE_BILLBOARD,"textures/effect/glow.png",0xFF12042E);
            // 白色水墨：稀疏长笔触缓慢扫过
            Emitter ink=emitter(SHELL,radius*0.9,0.1);Object k=ink.config();
            duration(k,life);particleLife(k,50);stretch(k,0.05,0.16);speed(k,-0.35);rate(k,Math.max(3,radius*0.18));colour(k,0x8CFFFFFF);
            render(k,MODE_BILLBOARD,"textures/effect/streak.png",0xFF3A4560);
            Emitter wash=emitter(SHELL,radius*0.8,0.2);Object w=wash.config();
            duration(w,life);particleLife(w,50);stretch(w,0.06,0.14);speed(w,-0.20);rate(w,Math.max(2,radius*0.08));orbit(w,0.5);colour(w,0x44FFFFFF);
            render(w,MODE_BILLBOARD,"textures/effect/streak.png",0);
            return start(List.of(new Placement(shell,Vec3.ZERO),new Placement(fill,Vec3.ZERO),
                new Placement(ink,Vec3.ZERO),new Placement(wash,Vec3.ZERO)),pos);
        }catch(Throwable t){fail(t);return null;}
    }
    /** 冲击波：圆环向外扩张。 */
    public static Handle shockwave(Vec3 pos,int argb,double radius,int life){
        try{
            Emitter ring=emitter(RING,0.4,1);Object r=ring.config();
            lifetime(r,life);size(r,0.9);speed(r,2.2);shapeScale(r,radius*0.5,radius*0.5,radius*0.5);rate(r,60);colour(r,argb);
            render(r,MODE_BILLBOARD,"textures/effect/ring.png",argb);
            return start(List.of(new Placement(ring,Vec3.ZERO)),pos);
        }catch(Throwable t){fail(t);return null;}
    }

    // ------------------------------------------------------------ 无量空处画布（全 Photon）
    private static final String GLOW="textures/effect/glow.png",GALAXY="textures/effect/galaxy.png",STREAK="textures/effect/streak.png";
    /** 统一零件构造：stretchAlong &gt; 0 时用拉伸尺寸（细线/笔触），否则用等向尺寸。 */
    private static Emitter paint(int shape,double r,double thick,double size,double rate,int color,int bloom,String tex,boolean additive,int duration,int life,double orbitVel,double speedPerTick,double stretchAlong,double stretchAcross)throws Exception{
        Emitter e=emitter(shape,r,thick);Object c=e.config();
        duration(c,duration);particleLife(c,life);
        if(stretchAlong>0)stretch(c,stretchAlong,stretchAcross);else size(c,Math.max(0.05,size));
        rate(c,Math.max(0.5,rate));
        if(orbitVel!=0)orbit(c,orbitVel);
        if(speedPerTick!=0)speed(c,speedPerTick);
        colour(c,color);
        render(c,MODE_BILLBOARD,tex,bloom,additive);
        return e;
    }
    /**
     * 无量空处整块画布：银河背景 + 星空 + 白墨泼溅 + 七彩超细线条飞向黑洞 + 白色吸积盘 + 黑洞本体。
     * 所有零件挂在同一个 root 上（内部 runtime 不拷贝对象，因此每个零件的位置与朝向都生效），
     * 客户端每刻只需把 root 移到施法者身上 —— 边界距离因此恒定，且只有一个句柄要维护。
     */
    public static Handle canvas(Vec3 pos,double radius,Vec3 hole,double holeRadius,int duration){
        try{
            double R=Math.max(16,radius),H=Math.max(2,holeRadius);
            List<Placement> pl=new ArrayList<>();
            // 银河：Circle 形状在 XZ 平面，绕 X 倾斜就成了一条斜跨天空的环带（两条不同高度）
            pl.add(P(paint(RING,R*0.94,0.55,R*0.22,5,0x50A8C8FF,0x38263F80,GALAXY,true,duration,150,0.05,0,0,0),Vec3.ZERO,Math.toRadians(64),0,0));
            pl.add(P(paint(RING,R*0.88,0.45,R*0.26,3,0x3A7A5CFF,0x2A1E2A6E,GALAXY,true,duration,160,0.04,0,0,0),Vec3.ZERO,Math.toRadians(34),0,0));
            // 星空：一层密集小星 + 少量亮星
            pl.add(P(paint(SHELL,R*0.99,0.02,0.55,10,0xCCFFFFFF,0,GLOW,true,duration,220,0,0,0,0),Vec3.ZERO));
            pl.add(P(paint(SHELL,R*0.96,0.04,1.5,1.5,0xFFFFFFFF,0x70FFFFFF,GLOW,true,duration,260,0,0,0,0),Vec3.ZERO));
            // 白墨：长笔触 + 短泼溅 + 大团墨晕，像泼在球壁上
            pl.add(P(paint(SHELL,R*0.90,0.22,0,7,0x9CFFFFFF,0x303A4560,STREAK,true,duration,90,0,-0.25,0.05,0.95),Vec3.ZERO));
            pl.add(P(paint(SHELL,R*0.84,0.30,0,5,0xB4FFFFFF,0x20FFFFFF,STREAK,true,duration,80,0,-0.15,0.11,0.42),Vec3.ZERO));
            pl.add(P(paint(SHELL,R*0.80,0.35,R*0.10,2,0x26FFFFFF,0x18FFFFFF,GLOW,true,duration,70,0,0,0,0),Vec3.ZERO));
            // 七彩超细线条：从外圈被吸进黑洞
            int[] rainbow={0xFF3B30,0xFF9500,0xFFE04A,0x4CD964,0x3BE0E0,0x4A90FF,0xB06BFF};
            for(int rgb:rainbow){
                pl.add(P(paint(SHELL,H*2.2,0.02,0,2,(0xC0<<24)|rgb,(0x40<<24)|rgb,STREAK,true,duration,26,0,-2.2,0.015,0.85),hole));
            }
            // 白色吸积盘：Circle 本身就在 XZ 平面，抬 22° 就成一圈接近水平、略带倾角的盘
            pl.add(P(paint(RING,H*1.70,0.45,H*0.05,14,0xE8FFFFFF,0x90FFFFFF,GLOW,true,duration,40,0.5,0,0,0),hole,Math.toRadians(22),0,0));
            pl.add(P(paint(RING,H*1.18,0.12,H*0.03,9,0xFFFFFFFF,0xFFFFFFFF,GLOW,true,duration,34,0.85,0,0,0),hole,Math.toRadians(22),0,0));
            // 黑洞本体：正常混合的近黑粒子先压暗（清晰黑盘与白边由 GojoVoidClient 补）
            pl.add(P(paint(SPHERE,H,1,H*0.42,4,0xDC000000,0,GLOW,false,duration,60,0,0,0,0),hole));
            return start(pl,pos);
        }catch(Throwable t){fail(t);return null;}
    }
    private PhotonFx(){}
}
