package character.example;

import character.*;
import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

/** 可编译的纯特效领域范例；默认不注册、不改变现有角色或地形。详见中文教程。 */
public final class EffectDomainExample {
    private static final Map<UUID,Field> FIELDS=new HashMap<>();
    private static final DustParticleOptions BLUE=new DustParticleOptions(new Vector3f(.15f,.55f,1),1);
    public static final int SKILL=9100;
    public static final double RADIUS=15;
    private static final class Field implements DomainClash.Lifecycle {
        final ServerPlayer actor;final LivingEntity body;final ServerLevel level;final Vec3 center;
        long end;boolean visible=true;
        Field(ServerPlayer p){actor=p;body=MaidBridge.body(p);level=p.serverLevel();center=p.position();end=level.getGameTime()+400;}
        public boolean active(){return FIELDS.get(body.getUUID())==this&&body.isAlive()&&!body.isRemoved()&&body.level()==level&&level.getGameTime()<end;}
        public void extend(int ticks){end+=Math.max(0,ticks);}
        public void visual(boolean show){visible=show;}
        public void close(){if(FIELDS.remove(body.getUUID(),this)){DomainClash.closed(body);Curse.endDomain(actor,false);}}
    }
    /** 在公共初始化阶段调用一次；角色定义里还必须加入对应技能数据。 */
    public static void install(String role){
        TechniqueApi.registerDomain(role,SKILL,(caster,skill,rank)->open(TechniqueApi.actor(caster.body())));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(EffectDomainExample.class);
    }
    public static void open(ServerPlayer actor){
        if(actor==null||actor.level().isClientSide||DomainClash.hasDomain(actor))return;
        Field field=new Field(actor);FIELDS.put(field.body.getUUID(),field);
        TechniqueApi.opened(actor,field.center,RADIUS,field);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(Field f:List.copyOf(FIELDS.values())){
            if(!f.active()){f.close();continue;}
            if(DomainClash.active(f.body))continue;
            long now=f.level.getGameTime();
            if(now%20==0){
                if(!Curse.spend(f.actor,20)){f.close();continue;}
                SkillCaster caster=SkillCaster.of(f.actor);
                for(LivingEntity target:f.level.getEntitiesOfClass(LivingEntity.class,new AABB(f.center,f.center).inflate(RADIUS),
                    t->t.position().distanceToSqr(f.center)<=RADIUS*RADIUS&&TechniqueApi.canAffect(caster,t,true)))caster.hurt(target,6);
            }
            if(f.visible&&now%10==0){
                // 48 个均匀球面采样点，不发送数万个方块，也不使用逐玩家伤害循环。
                for(int i=0;i<48;i++){
                    double y=1-2*(i+.5)/48.0,a=i*2.399963229728653,r=Math.sqrt(1-y*y);
                    Vec3 at=f.center.add(Math.cos(a)*r*RADIUS,y*RADIUS,Math.sin(a)*r*RADIUS);
                    f.level.sendParticles(BLUE,at.x,at.y,at.z,1,0,0,0,0);
                }
            }
        }
    }
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppingEvent event){for(Field f:List.copyOf(FIELDS.values()))f.close();}
    private EffectDomainExample() { }
}
