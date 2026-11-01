package character.domain;

import cn.blockforge.generated.nnnnr1201forge2.*;
import character.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 图片领域模板：不施工、不改地形。玩家与 NPC 进入完全相同的状态机和效果。 */
public final class PictureDomainApi {
    @FunctionalInterface public interface Pulse { void tick(SkillCaster caster,Vec3 fixedCenter,double radius); }
    public record Definition(double radius,int duration,int interval,double upkeep,DomainPresentation.Style style,Pulse pulse) {
        public Definition {
            if(!Double.isFinite(radius)||radius<2||radius>128||duration<20||duration>12000||interval<1
                ||!Double.isFinite(upkeep)||upkeep<0||style==null||pulse==null)throw new IllegalArgumentException("图片领域配置无效");
        }
    }
    private static final Map<UUID,Field> FIELDS=new HashMap<>();
    private static final class Field implements DomainClash.Lifecycle {
        final ServerPlayer owner;final SkillCaster caster;final Vec3 center;final Definition definition;
        final net.minecraft.server.level.ServerLevel level;long end;long next;
        Field(ServerPlayer p,Definition d){owner=p;caster=SkillCaster.of(p);center=MaidBridge.body(p).position();definition=d;level=p.serverLevel();end=level.getGameTime()+d.duration;next=level.getGameTime()+d.interval;}
        public boolean active(){return FIELDS.get(owner.getUUID())==this&&owner.isAlive()&&!owner.hasDisconnected()&&owner.level()==level;}
        public void extend(int ticks){end+=ticks;}
        public void resolved(DomainClash.Outcome outcome){if(outcome==DomainClash.Outcome.WON)next=level.getGameTime()+definition.interval;}
        public void close(){
            if(FIELDS.remove(owner.getUUID(),this)){DomainClash.closed(owner);Curse.data(owner).putBoolean("NpcHadDomain",false);Curse.endDomain(owner,false);}
        }
    }
    /** 角色/技能先登记。效果和 AI 共用此登记，不另外写 NPC 版本。 */
    public static void register(String role,int skill,Definition definition){
        TechniqueApi.registerDomain(role,skill,(caster,s,rank)->open(TechniqueApi.actor(caster.body()),definition));
    }
    public static boolean open(ServerPlayer p,Definition definition){
        if(p==null||!p.isAlive()||!character.mainline.Mainline.requireDomain(p)||DomainClash.hasDomain(p))return false;
        var field=new Field(p,definition);FIELDS.put(p.getUUID(),field);
        DomainClash.opened(p,field.center,definition.radius,field,definition.style);
        return field.active();
    }
    public static void close(ServerPlayer p){var field=FIELDS.get(p.getUUID());if(field!=null)field.close();}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(var f:List.copyOf(FIELDS.values())){
            long now=f.level.getGameTime();
            if(!f.active()||now>=f.end){f.close();continue;}
            if(DomainClash.active(f.owner))continue;
            if(now>=f.next){
                f.next=now+f.definition.interval;
                if(!Curse.spend(f.owner,f.definition.upkeep)){f.close();continue;}
                f.definition.pulse.tick(f.caster,f.center,f.definition.radius);
            }
        }
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent e){for(var f:List.copyOf(FIELDS.values()))f.close();}
    /** 可选必中模板：有敌友、简易领域和对拼保护，不借用在线玩家。 */
    public static Pulse damage(float amount){
        if(!Float.isFinite(amount)||amount<0)throw new IllegalArgumentException("伤害无效");
        return (caster,center,radius)->{
            var box=new net.minecraft.world.phys.AABB(center,center).inflate(radius);
            for(var target:caster.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,box,
                t->t.position().distanceToSqr(center)<=radius*radius&&TechniqueApi.canAffect(caster,t,true)))caster.hurt(target,amount);
        };
    }
    private PictureDomainApi(){ }
}
