package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.*;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;

/** 每种招式独立音频槽位；客户端资源包可覆盖同名 Vorbis ogg。 */
public final class SkillSounds {
    private static final DeferredRegister<SoundEvent> REGISTRY=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,"sukuna");
    private static final Map<String,RegistryObject<SoundEvent>> SOUNDS=new LinkedHashMap<>();
    public static final String[] NORMAL={"dismantle","web","cleave","flame_bow","furnace","shrine","soul_peel"};
    public static final String[] AWAKENED={"world_slash","sphere_web","void_cleave","heaven_bow","inferno","burning_shrine","soul_peel"};
    public static final String[] VOID={"void_dismantle","void_web","void_cleave","void_bow","void_furnace","void_shrine","soul_peel"};
    static {
        for(String id:NORMAL)register(id);
        for(String id:AWAKENED)register(id);
        for(String id:VOID)register(id);
        for(String id:List.of("space_charge","space_slash","nine_rotation","fetus_joint","void_needle","boss_reveal","daitian_curse","daitian_slam"))register(id);
        for(int i=1;i<=6;i++)register("yuji_"+i);
        for(String id:List.of("yuji_soul_impact","iwate_wind","soul_shrine","soul_peel","soul_feast","guardian_victory"))register(id);
        register("black_flash");   // 黑闪命中音效
        register("gojo_domain_start");register("gojo_domain_mid");register("domainshatter");   // 无量空处展开：起手 + 1.445 秒后无缝接 mid
        // 咏唱·茈：四句咒词（九纲/偏光/乌与声明/表里之间）+ 释放音；音频长度决定蓄力时长，见 GojoCombat 的 CHANT_*
        for(int i=1;i<=4;i++)register("gojo_chant_"+i);
        register("gojo_chant_release");
        for(int i=1;i<=11;i++)register("buddha_"+i);
        for(int i:new int[]{1,3,4,5,6,8,9,10,11,12})register("guardian_"+i);
        for(String id:AWAKENED)register("nine_"+id);
    }
    private static void register(String id){
        if(SOUNDS.containsKey(id))return;
        SOUNDS.put(id,REGISTRY.register(id,()->SoundEvent.createVariableRangeEvent(new ResourceLocation("sukuna",id))));
    }
    public static void init(IEventBus bus){REGISTRY.register(bus);}
    public static void play(Entity entity,String id){
        var sound=SOUNDS.get(id);if(sound==null||!(entity.level() instanceof ServerLevel level))return;
        if("guardian_victory".equals(id)){
            // 音乐发送到每个听众自己的位置，不受首领坐标的距离衰减影响。
            for(ServerPlayer listener:level.players())if(listener.distanceToSqr(entity)<=512*512)
                listener.connection.send(new ClientboundSoundPacket(Holder.direct(sound.get()),SoundSource.MUSIC,listener.getX(),listener.getY(),listener.getZ(),1,1,level.random.nextLong()));
            return;
        }
        level.playSound(null,entity.blockPosition(),sound.get(),entity instanceof ServerPlayer?SoundSource.PLAYERS:SoundSource.HOSTILE,2,1);
    }
    /** 领域展开类音效的音量：默认 {@link #play(Entity,String)} 用 2，用户要求"翻 2.5 倍" → 5。 */
    public static final float DOMAIN_VOLUME=5f;
    /** 指定音量的播放。 */
    public static void play(Entity entity,String id,float volume){
        var sound=SOUNDS.get(id);if(sound==null||!(entity.level() instanceof ServerLevel level))return;
        level.playSound(null,entity.blockPosition(),sound.get(),entity instanceof ServerPlayer?SoundSource.PLAYERS:SoundSource.HOSTILE,volume,1);
    }
    public static void cast(ServerPlayer p,int skill){
        if(skill<0||skill>=NORMAL.length)return;
        // 开与御厨子不再随成长阶段切换音频；其他招式保留原有阶段槽位。
        if(skill==3){play(p,"flame_bow");return;}
        if(skill==5){play(p,"shrine");return;}
        play(p,VoidJourney.evolved(p)?VOID[skill]:Journey.nine(p)?"nine_"+AWAKENED[skill]:Curse.awakened(p)?AWAKENED[skill]:NORMAL[skill]);
    }
}
