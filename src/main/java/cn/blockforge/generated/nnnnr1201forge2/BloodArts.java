package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;

/** 赤血操术服务端。客户端只能提交按键，耗能、冷却、命中和状态均在这里核验。 */
public final class BloodArts {
    public static final int FX=400,FX_HIT=420,FX_POISON=421,FX_TRAIL=422,FX_HEART=423,FX_DRAIN=424,FX_SHATTER=425,FX_READY=426;
    private static final ResourceKey<DamageType> BLOOD=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("sukuna","blood"));
    private static final ResourceKey<DamageType> TRUE=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("sukuna","blood_true"));
    private static final UUID SPEED=UUID.fromString("ae133000-0001-4000-8000-000000000001"),ATTACK=UUID.fromString("ae133000-0001-4000-8000-000000000002"),SLOW=UUID.fromString("ae133000-0001-4000-8000-000000000003"),BREAK=UUID.fromString("ae133000-0001-4000-8000-000000000004");
    private static final Map<UUID,State> states=new HashMap<>();
    private static final List<Dot> dots=new ArrayList<>();
    private static final List<Binding> bindings=new ArrayList<>();
    private static final List<Area> areas=new ArrayList<>();
    private static final List<Ring> rings=new ArrayList<>();
    private static final List<Thorn> thorns=new ArrayList<>();
    private static final List<Follow> follows=new ArrayList<>();
    private static final List<Canal> canals=new ArrayList<>();
    private static class Canal {final ServerPlayer p;final ServerLevel level;final UUID target;Vec3 at;int age;Canal(ServerPlayer p,LivingEntity target,Vec3 at){this.p=p;level=p.serverLevel();this.target=target.getUUID();this.at=at;}}
    private static boolean bloodDamage;
    private static class State {
        final ServerPlayer p;final ServerLevel level;final long[] cooldown=new long[BloodRules.LAST-BloodRules.FIRST+1];
        long charge=-1,heartbeat,compressed,flow,blade,wings,mirror,shieldEnd;boolean vision,automaticCharge;
        int charges;final Map<UUID,Long> marks=new LinkedHashMap<>();
        float shield;long lastBlade;
        State(ServerPlayer p){this.p=p;level=p.serverLevel();var saved=Curse.data(p).getCompound("BloodCooldowns");for(int i=0;i<cooldown.length;i++)cooldown[i]=saved.getLong(Integer.toString(i));}
    }
    private record Dot(ServerPlayer p,ServerLevel level,UUID target,long end,boolean poison){}
    private record Binding(ServerPlayer p,ServerLevel level,UUID target,long end,Vec3 anchor){}
    private record Area(ServerPlayer p,ServerLevel level,Vec3 at,long end,double radius,boolean mist){}
    private record Thorn(ServerPlayer p,ServerLevel level,Vec3 at,Vec3 dir,long due){}
    private record Follow(ServerPlayer p,ServerLevel level,UUID target,long due){}
    private static class Ring {
        final ServerPlayer p;final ServerLevel level;final Vec3 start,dir,side;final Set<UUID> hits=new HashSet<>();
        Vec3 at;int age;final int lane;
        Ring(ServerPlayer p,Vec3 start,Vec3 dir,int lane){this.p=p;level=p.serverLevel();this.start=start;this.dir=dir;side=NetBranches.side(dir);at=start;this.lane=lane;}
    }
    public static int rank(ServerPlayer p){return BloodRules.rank(SukunaBranches.stage(p));}
    public static boolean unlocked(ServerPlayer p){return Journey.nine(p);}
    public static boolean vision(ServerPlayer p){State s=states.get(p.getUUID());return s!=null&&s.vision&&s.flow>p.level().getGameTime();}
    public static boolean casting(ServerPlayer p){State s=states.get(p.getUUID());return s!=null&&s.charge>=0||DeathStar.charging(p);}
    public static boolean ongoing(ServerPlayer p){return states.containsKey(p.getUUID())||BloodDomain.active(p);}
    public static boolean available(ServerPlayer p,int skill){
        if(!BloodRules.skill(skill)||!usable(p)||DeathStar.anemic(p))return false;
        if(skill==BloodRules.DEATH_STAR)return BloodDomain.free(p)&&!DeathStar.charging(p)&&(!(p instanceof MaidBridge.Actor)||MaidBridge.body(p).getHealth()<=MaidBridge.body(p).getMaxHealth()*.35f);
        State s=state(p);long now=p.level().getGameTime();
        if(s.charge>=0||s.cooldown[skill-BloodRules.FIRST]>now||skill==BloodRules.OCEAN&&!BloodExpansion.canOcean(p)||skill==BloodRules.COMPRESS&&s.compressed>now)return false;
        if(skill==BloodRules.VISION&&(s.flow<=now||s.vision)||skill==BloodRules.SENBON&&s.blade<=now)return false;
        if(skill==BloodRules.FLOW&&s.flow>now||skill==BloodRules.BLADE&&s.blade>now||skill==BloodRules.WINGS&&s.wings>now||skill==BloodRules.DOMAIN&&BloodDomain.active(p))return false;
        return BloodDomain.free(p)||Curse.canSpend(p,BloodRules.cost(skill));
    }
    public static void automatic(ServerPlayer p,int skill){if(!available(p,skill))return;input(p,new TechniqueInput.Input(skill,0,false,false,false,false,0,skill));if(skill==BloodRules.COMPRESS){State s=state(p);s.automaticCharge=s.charge>=0;}}

    /** 自动战斗优先兑现前置状态，不让军团轮询一圈后才想起穿血或千本。 */
    public static int followup(ServerPlayer p){
        State s=states.get(p.getUUID());if(s==null)return -1;long now=p.level().getGameTime();
        int[] choices=s.compressed>now?new int[]{BloodRules.PIERCE,BloodRules.NOVA}:!s.marks.isEmpty()?new int[]{BloodRules.NOVA,BloodRules.THORNS,BloodRules.PIERCE}:s.blade>now?new int[]{BloodRules.SENBON}:s.flow>now&&!s.vision?new int[]{BloodRules.VISION}:new int[0];
        for(int id:choices)if(available(p,id))return id;return -1;
    }
    private static State state(ServerPlayer p){State s=states.get(p.getUUID());if(s==null||s.p!=p||s.level!=p.level()){if(s!=null)clear(s);s=new State(p);states.put(p.getUUID(),s);}return s;}
    private static boolean valid(ServerPlayer p,ServerLevel level){return p!=null&&p.isAlive()&&!p.isRemoved()&&(p instanceof MaidBridge.Actor||!p.hasDisconnected())&&p.level()==level&&MaidBridge.body(p).level()==level;}
    private static boolean usable(ServerPlayer p){return valid(p,p.serverLevel())&&unlocked(p)&&TechniqueInput.allowed(p)&&!Skills.sealing(p)&&!SixfoldFinale.active(p)&&!DomainClash.active(p)&&!DismantleMotion.active(p)&&!TenShadows.locked(p)&&!ObserverRitual.active(p)&&!FurnaceMax.active(p)&&!CombatExpansion.chanting(p);}
    public static boolean armored(LivingEntity target){State s=states.get(target.getUUID());return s!=null&&s.shield>0&&s.shieldEnd>s.level.getGameTime();}
    public static void input(ServerPlayer p,TechniqueInput.Input in){
        if(!BloodRules.skill(in.skill())||in.phase()<0||in.phase()>3)return;
        if(in.skill()==BloodRules.DEATH_STAR){if(in.phase()==0)DeathStar.start(p);return;}
        if(DeathStar.anemic(p)){tell(p,"严重贫血：暂时无法使用其他赤血操术");return;}
        State s=state(p);long now=p.level().getGameTime();
        if(in.phase()==3){cancelCharge(s);return;}
        if(in.skill()==BloodRules.COMPRESS){
            if(in.phase()==1){if(s.charge>=0)s.heartbeat=now;return;}
            if(in.phase()==2){if(s.charge>=0){boolean ready=now-s.charge>=BloodRules.CHARGE_TICKS&&usable(p);cancelCharge(s);if(ready){s.charges=1+rank(p);s.compressed=now+600;fx(p,FX_READY,p.getEyePosition(),p.getLookAngle(),1,40,0);tell(p,"百敛完成：下一发穿血或超新星伤害与穿透翻倍");echo(p,s,BloodRules.COMPRESS,false);}else tell(p,"百敛未蓄满，强化没有生成");}return;}
        }else if(in.phase()!=0)return;
        if(in.phase()!=0)return;
        if(s.charge>=0){if(in.skill()!=BloodRules.COMPRESS)cancelCharge(s);else return;}
        if(in.skill()==BloodRules.DOMAIN&&BloodDomain.active(p)){BloodDomain.close(p);return;}
        if(!usable(p)){tell(p,"赤血操术尚未解锁，或当前不能施术");return;}
        if(in.skill()==BloodRules.FLOW&&s.flow>now){s.flow=0;s.vision=false;attributes(s);return;}
        if(in.skill()==BloodRules.VISION){if(s.vision){s.vision=false;return;}if(s.flow<=now){tell(p,"先开启赤鳞跃动，再使用载");return;}}
        if(in.skill()==BloodRules.SENBON&&s.blade<=now){tell(p,"先开启血刃，再剥离为千本");return;}
        if(in.skill()==BloodRules.OCEAN&&!BloodExpansion.canOcean(p)){tell(p,"血洋仍在扩散或爆发，请等待结束");return;}
        int index=in.skill()-BloodRules.FIRST;
        if(s.cooldown[index]>now){tell(p,"冷却剩余 "+((s.cooldown[index]-now+19)/20)+" 秒");return;}
        if(!BloodDomain.free(p)&&!Curse.spend(p,BloodRules.cost(in.skill()))){tell(p,"咒力不足：需要 "+BloodRules.cost(in.skill()));return;}
        s.cooldown[index]=now+BloodRules.cooldown(in.skill());var saved=Curse.data(p).getCompound("BloodCooldowns");saved.putLong(Integer.toString(index),s.cooldown[index]);Curse.data(p).put("BloodCooldowns",saved);Curse.sync(p);
        if(in.skill()==BloodRules.COMPRESS){s.charge=now;s.heartbeat=now;attributes(s);pose(p,BloodRules.COMPRESS,60);return;}
        boolean empowered=(in.skill()==BloodRules.PIERCE||in.skill()==BloodRules.NOVA)&&s.compressed>now;
        if(empowered&&--s.charges<=0)s.compressed=0;
        pose(p,in.skill(),16);execute(p,s,in.skill(),p.getEyePosition(),p.getLookAngle(),empowered,false);echo(p,s,in.skill(),empowered);
    }
    private static void echo(ServerPlayer p,State s,int skill,boolean empowered){
        if(skill==BloodRules.CANAL||skill==BloodRules.FLOW||skill==BloodRules.BLADE||skill==BloodRules.ARMOR||skill==BloodRules.WINGS||skill==BloodRules.MIRROR||skill==BloodRules.DOMAIN||skill==BloodRules.COMPRESS||skill==BloodRules.VISION||skill==BloodRules.OCEAN)return;
        var marks=new LinkedHashMap<>(s.marks);s.marks.clear();
        for(var mark:marks.entrySet()){Entity marked=s.level.getEntity(mark.getKey());if(mark.getValue()>s.level.getGameTime()&&marked instanceof LivingEntity target&&CombatSafety.canAffect(p,target))execute(p,s,skill,target.getBoundingBox().getCenter(),p.getLookAngle(),empowered,true);}
    }
    private static void execute(ServerPlayer p,State s,int skill,Vec3 origin,Vec3 direction,boolean empowered,boolean echo){
        long now=p.level().getGameTime();int rank=rank(p);Vec3 floor=ground(p,origin);Vec3 dir=direction.normalize();float size=empowered?2:1;
        if(skill<BloodRules.DOMAIN&&skill!=BloodRules.PIERCE&&skill!=BloodRules.CANAL&&skill!=BloodRules.METEOR&&skill!=BloodRules.NOVA)fx(p,FX+skill-BloodRules.FIRST,origin,dir,size,skill==BloodRules.COMPRESS?40:30,0);
        switch(skill){
            case BloodRules.BIND -> {for(LivingEntity t:near(p,origin,12+rank*2))if(cone(origin,dir,t,.25-rank*.05)&&visible(p,origin,t)){int duration=BloodRules.bindTicks(poisoned(p,t)||BloodExpansion.slowed(t));bindings.removeIf(b->b.p==p&&b.target.equals(t.getUUID()));if(bindings.size()<1024)bindings.add(new Binding(p,p.serverLevel(),t.getUUID(),now+duration,t.position()));fx(p,FX,t.position(),Vec3.ZERO,t.getBbHeight(),duration,t.getId());}}
            case BloodRules.RINGS -> {int half=BloodRules.rings(rank)/2;for(int lane=-half;lane<=half&&rings.size()<384;lane++)rings.add(new Ring(p,origin,dir,lane));}
            case BloodRules.COMPRESS -> fx(p,FX_READY,origin,dir,1,30,0);
            case BloodRules.PIERCE -> {for(int i=0;i<=rank;i++)pierce(p,origin.add(NetBranches.side(dir).scale((i-rank*.5)*.45)),dir,empowered,false,null);}
            case BloodRules.FLOW -> {s.flow=now+BloodRules.duration(300,rank);attributes(s);}
            case BloodRules.BLADE -> s.blade=now+BloodRules.duration(200,rank);
            case BloodRules.METEOR -> BloodExpansion.meteors(p,origin,dir);
            case BloodRules.ARMOR -> {s.shield=Math.max(s.shield,BalanceRules.damage(160*(1+.15f*rank)));s.shieldEnd=now+BloodRules.duration(300,rank);}
            case BloodRules.NOVA -> {nova(p,origin,empowered);BloodExpansion.novaEchoes(p,origin,empowered);}
            case BloodRules.CANAL -> {var targets=near(p,origin,36+rank*4);targets.removeIf(t->!cone(origin,dir,t,.65)||!visible(p,origin,t));targets.sort(Comparator.<LivingEntity>comparingDouble(t->-t.getBoundingBox().getCenter().subtract(origin).normalize().dot(dir)).thenComparingDouble(t->t.distanceToSqr(origin)));canals.removeIf(c->c.p==p);s.marks.clear();for(LivingEntity t:targets.stream().limit(BloodRules.canalTargets(rank)).toList())if(canals.size()<512)canals.add(new Canal(p,t,origin));if(targets.isEmpty())tell(p,"运河没有找到准星附近的敌人");}
            case BloodRules.WINGS -> {s.wings=now+BloodRules.duration(160,rank);LivingEntity body=MaidBridge.body(p);body.setDeltaMovement(body.getDeltaMovement().add(dir.scale(.7)).add(0,.7,0));body.hurtMarked=true;}
            case BloodRules.MIST -> {if(areas.size()<512)areas.add(new Area(p,p.serverLevel(),floor,now+BloodRules.duration(100,rank),9+rank*2,true));}
            case BloodRules.THORNS -> {Vec3 flat=new Vec3(dir.x,0,dir.z).normalize();if(flat.lengthSqr()<.1)flat=new Vec3(0,0,1);int lanes=7+rank*2;for(int lane=0;lane<lanes;lane++){Vec3 ray=flat.yRot((float)(-Math.PI/3+lane*(Math.PI*2/3)/(lanes-1)));for(int i=1;i<=5+rank&&thorns.size()<1024;i++)thorns.add(new Thorn(p,p.serverLevel(),ground(p,origin.add(ray.scale(i*1.7))),ray,now+i*2));}}
            case BloodRules.MIRROR -> s.mirror=now+BloodRules.duration(60,rank);
            case BloodRules.DOMAIN -> BloodDomain.open(p,echo?origin.subtract(0,1,0):p.position(),echo);
            case BloodRules.OCEAN -> BloodExpansion.ocean(p,floor);
            case BloodRules.TRAP -> BloodExpansion.trap(p,origin,dir);
            case BloodRules.VISION -> {s.vision=true;if(!(p instanceof MaidBridge.Actor))BlackFlashMarks.extendBloodVision(p);}
            case BloodRules.SENBON -> {s.blade=0;BloodExpansion.senbon(p,origin,dir);}
            default -> {}
        }
    }
    public static void fx(ServerPlayer p,int kind,Vec3 at,Vec3 direction,float size,int life,long seed){Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.NEAR.with(()->new net.minecraftforge.network.PacketDistributor.TargetPoint(at.x,at.y,at.z,192,p.level().dimension())),new Network.Effect(kind,p.getId(),at,direction,size,life,seed));}
    private static void pose(ServerPlayer p,int skill,int ticks){Network.send(p.serverLevel(),new Network.Effect(70,p.getId(),p.position(),p.getLookAngle(),skill,ticks,0));}
    private static void tell(ServerPlayer p,String text){p.displayClientMessage(Component.literal(text),true);}
    private static Vec3 ground(ServerPlayer p,Vec3 origin){var hit=p.level().clip(new ClipContext(origin.add(0,2,0),origin.add(0,-8,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return hit.getType()==HitResult.Type.MISS?origin.subtract(0,p.getEyeHeight(),0):hit.getLocation().add(0,.03,0);}
    private static List<LivingEntity> near(ServerPlayer p,Vec3 at,double radius){return p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius),t->CombatSafety.canTarget(p,t)&&t.getBoundingBox().getCenter().distanceToSqr(at)<=radius*radius);}
    private static boolean visible(ServerPlayer p,Vec3 at,LivingEntity t){return p.level().clip(new ClipContext(at,t.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;}
    private static boolean cone(Vec3 at,Vec3 dir,LivingEntity t,double cosine){Vec3 delta=t.getBoundingBox().getCenter().subtract(at);return delta.lengthSqr()<.5||delta.normalize().dot(dir)>=cosine;}
    private static LivingEntity aim(ServerPlayer p,Vec3 from,Vec3 dir,double range){return near(p,from,range).stream().filter(t->cone(from,dir,t,.86)&&p.hasLineOfSight(t)).min(Comparator.comparingDouble(t->from.distanceToSqr(t.getBoundingBox().getCenter()))).orElse(null);}
    private static boolean poisoned(ServerPlayer p,LivingEntity target){long now=p.level().getGameTime();return dots.stream().anyMatch(d->d.poison&&d.level==target.level()&&d.target.equals(target.getUUID())&&d.end>now&&CombatSafety.canAffect(d.p,target));}
    static void dot(ServerPlayer p,LivingEntity t,int duration,boolean poison){if(!CombatSafety.canAffect(p,t))return;dots.removeIf(d->d.p==p&&d.target.equals(t.getUUID())&&d.poison==poison);if(dots.size()<2048)dots.add(new Dot(p,p.serverLevel(),t.getUUID(),p.level().getGameTime()+duration,poison));}
    public static boolean hit(ServerPlayer p,LivingEntity t,float base,float armorRemaining,boolean trueDamage){return hit(p,t,base,armorRemaining,trueDamage,1);}
    private static boolean hit(ServerPlayer p,LivingEntity t,float base,float armorRemaining,boolean trueDamage,float multiplier){
        if(!valid(p,p.serverLevel())||!Curse.canUseTechnique(p)||SoulCombat.suppressed(p)||PrisonSystem.locked(MaidBridge.body(p))||DeathStar.anemic(p)||!CombatSafety.canAffect(p,t)||DomainClash.active(p)||DomainClash.active(t))return false;
        float amount=CombatSettings.spellDamage(p,base)*multiplier*BloodRules.damageScale(rank(p));
        if(!trueDamage)amount=CombatRules.getDamageAfterAbsorb(amount,t.getArmorValue()*armorRemaining,(float)t.getAttributeValue(Attributes.ARMOR_TOUGHNESS)*armorRemaining);
        var holder=p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(trueDamage?TRUE:BLOOD);
        float before=t.getHealth()+t.getAbsorptionAmount();t.invulnerableTime=0;
        boolean previous=bloodDamage;bloodDamage=true;boolean accepted;
        try{AttackTypes.mark(AttackTypes.BLOOD);accepted=CombatSafety.hurt(p,t,new DamageSource(holder,p),amount);}finally{bloodDamage=previous;}
        if(accepted&&t.getHealth()+t.getAbsorptionAmount()<before){if(t.getPersistentData().getLong("BloodHitFxAt")<p.level().getGameTime()){t.getPersistentData().putLong("BloodHitFxAt",p.level().getGameTime()+3);fx(p,FX_HIT,t.getBoundingBox().getCenter(),p.getLookAngle(),1,12,t.getId());}BloodDomain.onHit(p,t);return true;}return false;
    }
    public static void pierce(ServerPlayer p,Vec3 start,Vec3 dir,boolean empowered,boolean sure,LivingEntity designated){
        Vec3 end=start.add(dir.scale(sure?12:72));
        if(!sure){BlockHitResult block=p.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();}
        Vec3 stop=end;
        List<LivingEntity> targets=sure&&designated!=null?List.of(designated):p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(1),t->CombatSafety.canAffect(p,t)&&t.getBoundingBox().inflate(.12).clip(start,stop).isPresent());
        targets=new ArrayList<>(targets);targets.sort(Comparator.comparingDouble(t->t.position().distanceToSqr(start)));
        int remaining=sure?1:BloodRules.penetration(empowered);
        for(LivingEntity t:targets){if(remaining--<=0)break;boolean weak=!sure&&BlackFlashMarks.bloodWeakPoint(p,t,start,end);hit(p,t,32,.5f,false,BloodRules.multiplier(empowered,weak)*(sure?BloodRules.domainBase(MaidBridge.body(p).getMaxHealth())/32/(1+rank(p)):1));if(weak)fx(p,FX_HIT,t.getEyePosition().add(dir),dir,3,24,t.getId());}
        fx(p,FX+3,start,end,empowered?2:1,12,sure?2:0);
    }
    static void nova(ServerPlayer p,Vec3 at,boolean empowered){
        fx(p,FX+8,at,Vec3.ZERO,empowered?2:1,32,BloodRules.novaRays(rank(p)));
        p.serverLevel().playSound(null,BlockPos.containing(at),SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,1.1f,.7f);
        // 每条血弹方向独立计算穿透次数，成长增加方向而非只加伤害。
        int count=BloodRules.novaRays(rank(p));int[] rays=new int[count];var targets=near(p,at,16);targets.sort(Comparator.comparingDouble(t->t.position().distanceToSqr(at)));
        for(LivingEntity t:targets){Vec3 d=t.getBoundingBox().getCenter().subtract(at);int lane=Math.floorMod((int)Math.floor((Math.atan2(d.z,d.x)+Math.PI)*count/(Math.PI*2)),count);if(rays[lane]>=BloodRules.penetration(empowered))continue;
            if(p.level().clip(new ClipContext(at,t.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS)continue;rays[lane]++;
            if(hit(p,t,24,1,false,empowered?2:1)){dot(p,t,160,true);CombatSafety.push(p,t,d.normalize().scale(d.length()<8?1.3:.5).add(0,.2,0));}}
    }
    private static void modifier(LivingEntity e,net.minecraft.world.entity.ai.attributes.Attribute attr,UUID id,double value,boolean active){var instance=e.getAttribute(attr);if(instance==null)return;if(!active){instance.removeModifier(id);return;}if(instance.getModifier(id)==null)instance.addTransientModifier(new AttributeModifier(id,"赤血操术",value,AttributeModifier.Operation.MULTIPLY_TOTAL));}
    private static void attributes(State s){long now=s.level.getGameTime();modifier(MaidBridge.body(s.p),Attributes.MOVEMENT_SPEED,SPEED,.5+rank(s.p)*.05,s.flow>now);modifier(MaidBridge.body(s.p),Attributes.ATTACK_SPEED,ATTACK,.3+rank(s.p)*.03,s.flow>now);modifier(MaidBridge.body(s.p),Attributes.MOVEMENT_SPEED,SLOW,-.65,s.charge>=0);}
    private static void cancelCharge(State s){if(s.charge<0)return;s.charge=-1;s.automaticCharge=false;modifier(MaidBridge.body(s.p),Attributes.MOVEMENT_SPEED,SLOW,0,false);pose(s.p,BloodRules.COMPRESS,0);}
    public static void cancel(ServerPlayer p){State s=states.get(p.getUUID());if(s!=null)cancelCharge(s);}
    /** 敌友菜单改动立即释放本施术者的血网、毒血、流血和运河，不等下一次伤害。 */
    public static void protect(ServerPlayer p,UUID id){dots.removeIf(d->d.p==p&&d.target.equals(id));bindings.removeIf(d->d.p==p&&d.target.equals(id));follows.removeIf(d->d.p==p&&d.target.equals(id));canals.removeIf(d->d.p==p&&d.target.equals(id));State s=states.get(p.getUUID());if(s!=null)s.marks.remove(id);BloodExpansion.protect(p,id);}
    /** 献祭时清空血盾、血刃、强化和在途赤血效果，但保留领域外壳供血星释放。 */
    public static void sacrifice(ServerPlayer p){
        BloodExpansion.exit(p);State s=states.remove(p.getUUID());if(s!=null)clear(s);
        dots.removeIf(d->d.p==p);bindings.removeIf(d->d.p==p);areas.removeIf(d->d.p==p);rings.removeIf(d->d.p==p);thorns.removeIf(d->d.p==p);follows.removeIf(d->d.p==p);canals.removeIf(d->d.p==p);
    }
    private static void clear(State s){cancelCharge(s);s.flow=0;attributes(s);s.shield=0;s.wings=0;}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(State s:List.copyOf(states.values())){
            ServerPlayer p=s.p;long now=s.level.getGameTime();
            if(!valid(p,s.level)){clear(s);states.remove(p.getUUID());continue;}
            if(!usable(p)){cancelCharge(s);s.flow=s.blade=s.wings=s.mirror=0;s.vision=false;s.shield=0;}
            attributes(s);
            s.marks.entrySet().removeIf(e->e.getValue()<=now);
            if(s.flow<=now)s.vision=false;
            if(s.vision&&now%20==0&&!BloodDomain.free(p)&&!Curse.spend(p,15)){s.vision=false;tell(p,"咒力不足，载已关闭");}
            if(s.automaticCharge&&s.charge>=0){s.heartbeat=now;if(now-s.charge>=BloodRules.CHARGE_TICKS){cancelCharge(s);s.charges=1+rank(p);s.compressed=now+600;}}
            if(s.charge>=0){if(now-s.heartbeat>40)cancelCharge(s);else if(now%4==0){fx(p,FX+2,p.getEyePosition().add(p.getLookAngle().scale(.65)),p.getLookAngle(),(float)Math.min(1,(now-s.charge)/(double)BloodRules.CHARGE_TICKS),8,0);pose(p,BloodRules.COMPRESS,10);}}
            if(s.shield>0&&now>=s.shieldEnd)s.shield=0;
            if(s.wings>now){LivingEntity body=MaidBridge.body(p);Vec3 v=body.getDeltaMovement(),look=p.getLookAngle();Vec3 flight=new Vec3(v.x*.97+look.x*.045,0,v.z*.97+look.z*.045);if(flight.lengthSqr()>1)flight=flight.normalize();body.setDeltaMovement(flight.x,Math.max(v.y,-.09),flight.z);body.fallDistance=0;body.hurtMarked=true;if(now%6==0&&areas.size()<512){Vec3 at=ground(p,p.position());areas.add(new Area(p,s.level,at,now+80+rank(p)*20,1.4+rank(p)*.35,false));fx(p,FX_TRAIL,at,Vec3.ZERO,1.4f,80,0);}}
            if(s.mirror>now)reflect(s);
            if(now%5==0){
                if(s.compressed>now)fx(p,FX_READY,p.getEyePosition(),p.getLookAngle(),1,7,0);
                if(s.flow>now){fx(p,FX+4,p.position(),p.getDeltaMovement(),1,7,s.vision?1:0);if(s.vision&&!(p instanceof MaidBridge.Actor))for(LivingEntity t:near(p,p.getEyePosition(),40+rank(p)*8))if(visible(p,p.getEyePosition(),t)){Vec3 velocity=t.position().subtract(t.xo,t.yo,t.zo);Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new Network.Effect(FX_HEART,t.getId(),t.position(),velocity,1+rank(p)*.25f,7,0));}}
                if(s.blade>now)fx(p,FX+5,p.position(),p.getLookAngle(),1,7,0);
                if(s.shield>0)fx(p,FX+7,p.position(),Vec3.ZERO,s.shield,7,0);
                if(s.wings>now)fx(p,FX+10,p.position(),p.getLookAngle(),1,7,0);
                if(s.mirror>now)fx(p,FX+13,p.getEyePosition().add(p.getLookAngle()),p.getLookAngle(),1,7,0);
            }
        }
        tickRings();tickEffects();
        for(Iterator<Canal> it=canals.iterator();it.hasNext();){Canal c=it.next();Entity entity=c.level.getEntity(c.target);if(!valid(c.p,c.level)||++c.age>40||!(entity instanceof LivingEntity target)||!CombatSafety.canAffect(c.p,target)){it.remove();continue;}Vec3 to=target.getBoundingBox().getCenter(),delta=to.subtract(c.at),next=delta.length()<1.5?to:c.at.add(delta.normalize().scale(1.5));if(c.level.clip(new ClipContext(c.at,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.p)).getType()!=HitResult.Type.MISS){it.remove();continue;}fx(c.p,FX+9,c.at,next,1,5,-1);c.at=next;if(delta.length()<1.5){State s=state(c.p);s.marks.put(target.getUUID(),c.level.getGameTime()+BloodRules.MARK_TICKS+rank(c.p)*20);fx(c.p,FX+9,target.getBoundingBox().getCenter(),to,1,100,target.getId());it.remove();}}
        for(Follow f:List.copyOf(follows)){if(!follows.contains(f)||f.level.getGameTime()<f.due)continue;follows.remove(f);if(valid(f.p,f.level)&&f.level.getEntity(f.target) instanceof LivingEntity t&&hit(f.p,t,8,1,false)){Curse.add(f.p,15);fx(f.p,FX_DRAIN,t.getBoundingBox().getCenter(),f.p.getEyePosition(),1,14,0);}}
        for(Thorn thorn:List.copyOf(thorns)){if(!thorns.contains(thorn)||thorn.level.getGameTime()<thorn.due)continue;thorns.remove(thorn);if(!valid(thorn.p,thorn.level))continue;fx(thorn.p,FX+12,thorn.at,thorn.dir,1,22,0);for(LivingEntity t:near(thorn.p,thorn.at.add(0,1,0),2.4))if(hit(thorn.p,t,12,.5f,false)){CombatSafety.push(thorn.p,t,new Vec3(0,.65,0));modifier(t,Attributes.ARMOR,BREAK,-.35,true);t.getPersistentData().putLong("BloodArmorBreak",thorn.level.getGameTime()+100);t.getPersistentData().putUUID("BloodArmorBreakOwner",thorn.p.getUUID());}}
    }
    private static void tickRings(){for(Ring r:List.copyOf(rings)){if(!rings.contains(r))continue;if(!valid(r.p,r.level)||++r.age>24||r.hits.size()>=2+rank(r.p)){rings.remove(r);continue;}Vec3 next=r.start.add(r.dir.scale(r.age*1.4)).add(r.side.scale(r.lane*.65+Math.sin(r.age*.5)*.22)).add(0,r.lane==0?.35:-.15,0);var block=r.level.clip(new ClipContext(r.at,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,r.p));boolean wall=block.getType()!=HitResult.Type.MISS;if(wall)next=block.getLocation();Vec3 end=next;
        var targets=r.level.getEntitiesOfClass(LivingEntity.class,new AABB(r.at,next).inflate(.8),t->CombatSafety.canAffect(r.p,t)&&!r.hits.contains(t.getUUID())&&t.getBoundingBox().inflate(.55).clip(r.at,end).isPresent());targets.sort(Comparator.comparingDouble(t->t.distanceToSqr(r.at)));
        for(LivingEntity t:targets){if(r.hits.size()>=2+rank(r.p))break;r.hits.add(t.getUUID());if(hit(r.p,t,12,1,false)){dot(r.p,t,100,false);r.level.playSound(null,t.blockPosition(),SoundEvents.GRINDSTONE_USE,SoundSource.PLAYERS,.5f,1.5f);}}
        fx(r.p,FX+1,next,r.dir,1,5,r.lane+2);r.at=next;if(wall)rings.remove(r);}}
    private static void tickEffects(){
        for(Dot d:List.copyOf(dots)){if(!dots.contains(d))continue;Entity entity=d.level.getEntity(d.target);long now=d.level.getGameTime();if(!valid(d.p,d.level)||now>=d.end||!(entity instanceof LivingEntity t)||!CombatSafety.canAffect(d.p,t)){dots.remove(d);continue;}if(now%20==0){hit(d.p,t,d.poison?3:2,1,false);fx(d.p,FX_POISON,t.position(),Vec3.ZERO,1,24,d.poison?1:0);}}
        for(Iterator<Binding> it=bindings.iterator();it.hasNext();){Binding b=it.next();Entity entity=b.level.getEntity(b.target);if(!valid(b.p,b.level)||b.level.getGameTime()>=b.end||!(entity instanceof LivingEntity t)||!CombatSafety.canAffect(b.p,t)){it.remove();continue;}t.setDeltaMovement(Vec3.ZERO);if(t instanceof Mob m)m.getNavigation().stop();if(t.position().distanceToSqr(b.anchor)>.0025)t.teleportTo(b.anchor.x,b.anchor.y,b.anchor.z);t.hurtMarked=true;}
        for(Area a:List.copyOf(areas)){if(!areas.contains(a))continue;long now=a.level.getGameTime();if(!valid(a.p,a.level)||now>=a.end){areas.remove(a);continue;}if(now%10!=0)continue;if(a.mist)fx(a.p,FX+11,a.at,Vec3.ZERO,(float)a.radius,15,1);for(LivingEntity t:near(a.p,a.at.add(0,1,0),a.radius)){if(a.mist){CombatSafety.effect(a.p,t,new MobEffectInstance(MobEffects.BLINDNESS,25,0));dot(a.p,t,100,true);}else {CombatSafety.effect(a.p,t,new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,25,1));hit(a.p,t,2,1,false);}}}
    }
    private static void reflect(State s){
        Vec3 eye=s.p.getEyePosition(),look=s.p.getLookAngle();long now=s.level.getGameTime();
        for(Projectile projectile:s.level.getEntitiesOfClass(Projectile.class,MaidBridge.body(s.p).getBoundingBox().inflate(4+rank(s.p)))){
            if(projectile.getPersistentData().getLong("BloodReflected")>now||projectile.getOwner()==s.p||projectile.getDeltaMovement().dot(look)>=0)continue;
            if(!(projectile.getOwner() instanceof LivingEntity shooter)||!CombatSafety.canAffect(s.p,shooter))continue;
            Vec3 delta=projectile.position().subtract(eye);if(delta.dot(look)<0||delta.subtract(look.scale(delta.dot(look))).lengthSqr()>3+rank(s.p))continue;
            Vec3 back=projectile.getDeltaMovement().scale(-1);projectile.setOwner(s.p);projectile.setDeltaMovement(back);projectile.hurtMarked=true;
            if(projectile instanceof AbstractHurtingProjectile fire){fire.xPower=-fire.xPower;fire.yPower=-fire.yPower;fire.zPower=-fire.zPower;}
            projectile.getPersistentData().putLong("BloodReflected",now+10);fx(s.p,FX+13,projectile.position(),look,1.4f,12,2);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void shield(LivingHurtEvent event){
        ServerPlayer p=event.getEntity() instanceof ServerPlayer player?player:MaidBridge.caster(event.getEntity());
        if(p==null||DeathStar.source(event.getSource())||event.getAmount()<=0||event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return;State s=states.get(p.getUUID());if(s==null||s.shield<=0||s.shieldEnd<p.level().getGameTime())return;
        float taken=Math.min(s.shield,event.getAmount());s.shield-=taken;event.setAmount(event.getAmount()-taken);if(s.shield<=0){fx(p,FX_SHATTER,p.position().add(0,1,0),Vec3.ZERO,3,25,0);for(LivingEntity t:near(p,p.position(),5+rank(p)))hit(p,t,18,1,false);BloodExpansion.novaEchoes(p,p.position().add(0,1,0),false);}
    }
    @SubscribeEvent public static void knockback(LivingKnockBackEvent event){State s=states.get(event.getEntity().getUUID());if(s!=null&&s.shield>0&&s.shieldEnd>s.level.getGameTime())event.setCanceled(true);}
    @SubscribeEvent public static void blade(LivingDamageEvent event){
        if(bloodDamage||event.getAmount()<=0)return;ServerPlayer p=CombatSettings.caster(event.getSource());if(p==null)return;State s=states.get(p.getUUID());long now=p.level().getGameTime();if(s==null||s.blade<=now||s.lastBlade==now||!CombatSafety.canAffect(p,event.getEntity()))return;
        String type=AttackTypes.peek();boolean slash=AttackTypes.DISMANTLE.equals(type)||AttackTypes.CLEAVE.equals(type)||event.getSource().is(DamageTypes.PLAYER_ATTACK)||event.getSource().is(DamageTypes.MOB_ATTACK);if(!slash)return;
        s.lastBlade=now;for(int i=0;i<=rank(p)&&follows.size()<512;i++)follows.add(new Follow(p,p.serverLevel(),event.getEntity().getUUID(),now+1+i*3));fx(p,FX+5,event.getEntity().getBoundingBox().getCenter(),p.getLookAngle(),2,12,1);
    }
    @SubscribeEvent public static void armorBreak(LivingEvent.LivingTickEvent event){LivingEntity t=event.getEntity();if(!(t.level() instanceof ServerLevel level)||!t.getPersistentData().contains("BloodArmorBreak"))return;var tag=t.getPersistentData();ServerPlayer owner=tag.hasUUID("BloodArmorBreakOwner")?MaidBridge.find(level,tag.getUUID("BloodArmorBreakOwner")):null;if(level.getGameTime()>=tag.getLong("BloodArmorBreak")||!CombatSafety.canAffect(owner,t)){modifier(t,Attributes.ARMOR,BREAK,0,false);tag.remove("BloodArmorBreak");tag.remove("BloodArmorBreakOwner");}}
    @SubscribeEvent public static void fall(LivingFallEvent event){State s=states.get(event.getEntity().getUUID());if(s!=null&&s.wings>s.level.getGameTime()){event.setDistance(0);event.setDamageMultiplier(0);}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)exit(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p)exit(p);}
    @SubscribeEvent public static void death(LivingDeathEvent event){ServerPlayer p=event.getEntity() instanceof ServerPlayer player?player:MaidBridge.caster(event.getEntity());if(p!=null)exit(p);}
    public static void exit(ServerPlayer p){BloodExpansion.exit(p);State s=states.remove(p.getUUID());if(s!=null)clear(s);BloodDomain.close(p);dots.removeIf(d->d.p==p);bindings.removeIf(d->d.p==p);areas.removeIf(d->d.p==p);rings.removeIf(d->d.p==p);thorns.removeIf(d->d.p==p);follows.removeIf(d->d.p==p);canals.removeIf(d->d.p==p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){for(State s:states.values())clear(s);states.clear();dots.clear();bindings.clear();areas.clear();rings.clear();thorns.clear();follows.clear();canals.clear();BloodDomain.clear();}
    private BloodArts(){}
}
