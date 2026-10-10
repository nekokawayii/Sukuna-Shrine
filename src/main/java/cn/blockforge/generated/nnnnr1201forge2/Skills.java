package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.sounds.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

public final class Skills {
    public static final String[] NAMES={"解 · 远程斩击","解 · 八方罗网","捌 · 必断斩击","开 · 炎弓","灶 · 粉尘爆炸","领域展开 · 伏魔御厨子","灵魂剥离"};
    public static final String[] AWAKENED={"解 · 世界斩","解 · 天球罗网","捌 · 虚空捌","开 · 焚天炎弓","灶 · 众生焚爆","领域展开 · 燃烧御厨子","灵魂剥离"};
    private static final int[] COOLDOWN={0,60,40,60,100,20,1800};
    private static final Map<UUID,long[]> cooldowns=new HashMap<>();
    private static final Map<UUID,Charge> cleaveCharges=new HashMap<>();
    private record Impact(ServerPlayer owner,ServerLevel level,UUID target,long due,float amount,boolean world,boolean rift,boolean soulSlash,double fraction){
        Impact(ServerPlayer owner,ServerLevel level,UUID target,long due,float amount,boolean world,boolean rift,boolean soulSlash){this(owner,level,target,due,amount,world,rift,soulSlash,1);}
    }
    private static final List<Impact> impacts=new ArrayList<>();
    private record MeleeCut(ServerPlayer owner,ServerLevel level,UUID target,long due,float amount){}
    private static final List<MeleeCut> meleeCuts=new ArrayList<>();
    private record Strike(ServerPlayer owner,ServerLevel level,UUID target,Vec3 point,Vec3 direction,long due,float amount,float size,long tint){}
    private static final List<Strike> strikes=new ArrayList<>();
    private record SoulCut(ServerPlayer owner,ServerLevel level,UUID target,Vec3 point,Vec3 direction,long due,float amount,float size,long angle,boolean world){}
    private static final List<SoulCut> soulCuts=new ArrayList<>();
    private static void soulDismantle(ServerPlayer p,LivingEntity target,Vec3 point,Vec3 dir,float size,float amount,boolean world){
        if(target!=null){Vec3 center=target.getBoundingBox().getCenter(),toward=p.getEyePosition().subtract(center).normalize();point=target.getBoundingBox().inflate(0.08).clip(p.getEyePosition(),center).orElse(center).add(toward.scale(0.05));}
        long angle=2160+p.getRandom().nextInt(360);
        fx(p,96,point,dir,size,6,angle);
        List<LivingEntity> victims=size==SlashReach.INFINITE?SlashReach.targets(p,target,point,dir,angle):target==null?List.of():List.of(target);
        for(LivingEntity victim:victims){
            scaledDamage(p.serverLevel(),p,victim,amount*0.35f,world,false,true,SlashFormRules.DAMAGE);
            soulCuts.add(new SoulCut(p,p.serverLevel(),victim.getUUID(),point,dir,p.level().getGameTime()+6,amount,0,angle,world));
        }
        soulCuts.add(new SoulCut(p,p.serverLevel(),null,point,dir,p.level().getGameTime()+6,0,size,angle,world));
        RuinAtmosphere.echo(p);
    }
    private record Furnace(ServerPlayer owner,ServerLevel level,long due){}
    private static final List<Furnace> furnaces=new ArrayList<>();
    private static final List<Slash> slashes=new ArrayList<>();
    private static final Map<UUID,Aura> auras=new HashMap<>();
    private static final Map<UUID,Domain> domains=new HashMap<>();
    private static final List<Bow> bows=new ArrayList<>();
    private static final List<Fire> fires=new ArrayList<>();
    private static final List<Detonation> detonations=new ArrayList<>();
    public static final int CHARGE_DELAY=6,CHARGE_TIME=60;
    private record Joint(ServerPlayer owner,ServerLevel level,UUID target,long due,float power){}
    private static final Map<String,Joint> joints=new HashMap<>();
    private static final List<Rift> rifts=new ArrayList<>();
    private record Burn(UUID owner,long until,double multiplier){}
    private static final Map<LivingEntity,Burn> burns=new HashMap<>();
    private static void ignite(ServerPlayer owner,LivingEntity target,int seconds){
        if(!canHit(owner,target))return;
        if(SoulJourney.evolved(owner)){burns.put(target,new Burn(owner.getUUID(),Long.MAX_VALUE,CombatSettings.spellMultiplier(owner)));Curse.permanentSoulFire(owner,target);fx(owner,95,target.getBoundingBox().getCenter(),Vec3.ZERO,1,40,owner.getId());return;}
        target.setSecondsOnFire(seconds);
        Burn previous=burns.get(target);long until=target.level().getGameTime()+seconds*20L;
        if(previous==null||until>=previous.until)burns.put(target,new Burn(owner.getUUID(),until,CombatSettings.spellMultiplier(owner)));
    }
    public static void protectAlly(ServerPlayer owner,UUID target){
        CombatExpansion.protect(owner,target);VoidCombat.protect(owner,target);NetBranches.protect(owner,target);Uraume.protect(owner,target);
        joints.remove(owner.getUUID()+"/"+target);
        for(Rift r:rifts)if(r.owner.getUUID().equals(owner.getUUID())&&target.equals(r.target))r.target=null;
        burns.entrySet().removeIf(entry->{if(entry.getKey().getUUID().equals(target)&&entry.getValue().owner.equals(owner.getUUID())){entry.getKey().clearFire();return true;}return false;});
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void fistDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event){
        ServerPlayer p=CombatSettings.caster(event.getSource());if(event.isCanceled()||event.getAmount()<=0||p==null)return;
        LivingEntity target=event.getEntity();if(!canHit(p,target))return;
        remember(p,target);
        if(!(event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||MaidBridge.isCaster(p)&&event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))||Curse.fingers(p)<5||MaidBridge.recovering(p)||SoulCombat.suppressed(p)||!Curse.canUseTechnique(p))return;
        // 取本次普通攻击结算伤害；附加解不再叠术式成长，不递归触发普攻事件。
        if(meleeCuts.size()<4096)meleeCuts.add(new MeleeCut(p,p.serverLevel(),target.getUUID(),p.level().getGameTime()+1,event.getAmount()*0.7f));
        if(!Journey.unlocked(p)||!Curse.data(p).getBoolean("Fist"))return;
        float base=event.getAmount();event.setAmount((float)(base+10*CombatSettings.baseSpellMultiplier(p)));
        target.knockback(2.5,p.getX()-target.getX(),p.getZ()-target.getZ());target.hurtMarked=true;
        fx(p,69,target.getBoundingBox().getCenter(),p.getLookAngle(),2,12,0);
    }
    private static void remember(ServerPlayer p,LivingEntity target){
        var memory=cleaveMemory.computeIfAbsent(p.getUUID(),k->new LinkedHashSet<>());
        if(memory.size()>=4096)memory.remove(memory.iterator().next());memory.add(target.getUUID());
    }
    private static boolean remembered(ServerPlayer p,LivingEntity target){return cleaveMemory.getOrDefault(p.getUUID(),Set.of()).contains(target.getUUID());}
    @SubscribeEvent public static void burnDamage(net.minecraftforge.event.entity.living.LivingHurtEvent event){
        if(!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE))return;
        LivingEntity target=event.getEntity();Burn burn=burns.get(target);
        if(burn==null&&target.getPersistentData().getBoolean("SukunaSoulFire")&&target.getPersistentData().hasUUID("SukunaSoulFireOwner")){
            var tag=target.getPersistentData();UUID id=tag.getUUID("SukunaSoulFireOwner");ServerPlayer caster=target.level() instanceof ServerLevel level?MaidBridge.find(level,id):null;
            if(caster!=null)tag.putBoolean("SukunaSoulFireMaid",MaidBridge.isCaster(caster));
            if(!tag.contains("SukunaSoulFireMaid")){event.setCanceled(true);return;}
            burn=new Burn(id,Long.MAX_VALUE,BalanceRules.strength(CasterDamageRules.scale(0.5,tag.getBoolean("SukunaSoulFireMaid"))));
        }
        if(burn==null||target.level().getGameTime()>burn.until){VoidCombat.ignitionDamage(event);return;}
        ServerPlayer owner=target.level() instanceof ServerLevel level?MaidBridge.find(level,burn.owner):null;
        if(owner!=null&&(DomainClash.active(owner)||domains.containsKey(owner.getUUID())&&clashing(domains.get(owner.getUUID()))&&inDomain(owner,target))){event.setCanceled(true);return;}
        if(EternalLove.protects(burn.owner,target)||owner!=null&&!canHit(owner,target)){event.setCanceled(true);target.clearFire();burns.remove(target);return;}
        event.setAmount(owner==null?(float)(event.getAmount()*burn.multiplier):CombatSettings.spellDamage(owner,event.getAmount())*(Curse.repaired(owner)?1.1f:1));
    }
    private static int nextRift;
    private static final Map<UUID,Integer> airCuts=new HashMap<>();
    private static class Rift {
        final ServerPlayer owner;final ServerLevel level;final int id;final double radius;final float power;
        Vec3 pos,dir;UUID target;int age;
        Rift(ServerPlayer p,Vec3 point,LivingEntity victim,double radius){owner=p;level=p.serverLevel();id=++nextRift;pos=point;this.radius=radius;power=(float)Curse.power(p);target=victim==null?null:victim.getUUID();dir=new Vec3(p.getRandom().nextDouble()-0.5,p.getRandom().nextDouble()-0.5,p.getRandom().nextDouble()-0.5).normalize();}
    }
    private static void rift(ServerPlayer p,Vec3 pos,LivingEntity target,double radius){
        if(rifts.stream().filter(r->r.owner==p).count()>=64)return;
        Rift r=new Rift(p,pos,target,radius);rifts.add(r);SkillSounds.play(p,"void_needle");Network.send(r.level,new Network.Effect(61,p.getId(),pos,r.dir,Journey.nine(p)?2:1,140,r.id));
    }
    private static void joint(ServerPlayer p,LivingEntity target){
        String key=p.getUUID()+"/"+target.getUUID();if(joints.containsKey(key)||joints.size()>=1024)return;
        joints.put(key,new Joint(p,p.serverLevel(),target.getUUID(),p.level().getGameTime()+20,(float)(60*Curse.scale(p)*Journey.bonus(p))));
        fx(p,65,target.getBoundingBox().getCenter(),Vec3.ZERO,3,20,target.getId());
    }
    private static class Slash {
        ServerPlayer owner;ServerLevel level;Vec3 pos,dir,axis;float size,power;int age,life;boolean world,space;Set<UUID> hit=new HashSet<>();
        Slash(ServerPlayer p,Vec3 pos,Vec3 dir,Vec3 axis,float size){owner=p;level=p.serverLevel();this.pos=pos;this.dir=dir;this.axis=axis;this.size=size;power=(float)Curse.power(p);world=Curse.awakened(p);life=(int)(400*Curse.scale(p));}
    }
    private record Charge(ServerLevel level,long start){}
    private static final Map<UUID,Charge> charges=new HashMap<>();
    public static boolean inDomain(ServerPlayer p,LivingEntity target){Domain d=domains.get(p.getUUID());return !DomainClash.active(p)&&target!=null&&d!=null&&d.level==target.level()&&target.position().distanceToSqr(d.center)<=d.radius*d.radius;}
    public static boolean neutralizes(ServerPlayer p){return domains.values().stream().anyMatch(d->!DomainClash.active(d.owner)&&valid(d.owner,d.level)&&d.level==p.level()&&p.position().distanceToSqr(d.center)<=d.radius*d.radius);}
    private static boolean clashing(Domain d){return DomainClash.active(d.owner);}
    public static boolean domainClashing(ServerPlayer p){Domain d=domains.get(p.getUUID());return d!=null&&clashing(d);}
    public static Vec3 domainCenter(ServerPlayer p){Domain d=domains.get(p.getUUID());return d==null?p.position():d.center;}
    public static void closeDomain(ServerPlayer p){closeDomain(p,false);}
    public static void closeDomain(ServerPlayer p,boolean voluntary){Domain d=domains.remove(p.getUUID());if(d!=null){VoidCombat.closeDomain(p,valid(p,d.level)&&!clashing(d));Network.send(d.level,new Network.Effect(5,p.getId(),d.center,Vec3.ZERO,0,0,0));Curse.endDomain(p,voluntary&&d.level.getGameTime()<d.end);}}
    public static boolean domainActive(ServerPlayer p){return domains.containsKey(p.getUUID());}
    public static boolean clashDomainActive(ServerPlayer p){Domain d=domains.get(p.getUUID());return d!=null&&d.owner==p&&d.level==p.level()&&(DomainClash.active(p)||d.end>d.level.getGameTime());}
    public static void syncVoidFloor(ServerPlayer p){if(domains.containsKey(p.getUUID()))fx(p,25,p.position(),Vec3.ZERO,CombatSettings.voidFloor(p)?1:0,4,0);}
    private static void syncVoidShrine(Domain d){fx(d.owner,112,d.center,Vec3.ZERO,1,(int)Math.max(1,d.end-d.level.getGameTime()),GuardianCombat.awakened(d.owner)?1:0);}
    public static void syncAwakening(ServerPlayer p){Domain d=domains.get(p.getUUID());if(d!=null&&VoidJourney.evolved(p))syncVoidShrine(d);}
    public static void cancelCharge(ServerPlayer p){charges.remove(p.getUUID());cleaveCharges.remove(p.getUUID());}
    public static void cancelSustained(ServerPlayer p){cancelCharge(p);if(auras.remove(p.getUUID())!=null)fx(p,9,p.position(),Vec3.ZERO,0,1,0);}
    /** 仅在实体tick或退出处理执行，不可在伤害队列的回调中修改集合。 */
    public static void cancelCombat(ServerPlayer p){
        CombatExpansion.cancel(p);DismantleMotion.cancel(p);SlashForms.cancel(p);SukunaBranches.cancel(p);Uraume.cancel(p);NetBranches.cancel(p);VoidCombat.cancelCombat(p);closeDomain(p);cancelSustained(p);
        slashes.removeIf(s->s.owner==p);strikes.removeIf(s->s.owner==p);soulCuts.removeIf(s->s.owner==p);
        impacts.removeIf(s->s.owner==p);meleeCuts.removeIf(s->s.owner==p);bows.removeIf(s->s.owner==p);
        fires.removeIf(s->s.owner==p);furnaces.removeIf(s->s.owner==p);detonations.removeIf(s->s.owner==p);
        joints.values().removeIf(s->s.owner==p);rifts.removeIf(s->s.owner==p);
        for(LivingEntity target:List.copyOf(burns.keySet())){Burn burn=burns.get(target);if(burn!=null&&burn.owner.equals(p.getUUID()))SoulCombat.clearOwnedEffects(p,target);}
        cleaveMemory.remove(p.getUUID());airCuts.remove(p.getUUID());
    }
    public static void releaseCaster(ServerPlayer p){CombatExpansion.cancel(p);DismantleMotion.cancel(p);SlashForms.cancel(p);SukunaBranches.cancel(p);Uraume.cancel(p);NetBranches.cancel(p);closeDomain(p);cancelSustained(p);cooldowns.remove(p.getUUID());cleaveMemory.remove(p.getUUID());airCuts.remove(p.getUUID());}
    static void testReset(ServerPlayer p){cancelCombat(p);cooldowns.remove(p.getUUID());burns.remove(p);joints.values().removeIf(s->p.getUUID().equals(s.target));impacts.removeIf(s->p.getUUID().equals(s.target));meleeCuts.removeIf(s->p.getUUID().equals(s.target));soulCuts.removeIf(s->p.getUUID().equals(s.target));strikes.removeIf(s->p.getUUID().equals(s.target));for(Rift r:rifts)if(p.getUUID().equals(r.target))r.target=null;}
    public static void cleaveCharge(ServerPlayer p,boolean start){
        if(!Curse.canUseTechnique(p)){p.displayClientMessage(Component.literal("术式熔断中：只能使用术式反转或修复熔断"),true);return;}
        if(VoidJourney.evolved(p)){if(!start)cast(p,2);return;}
        if(!Journey.unlocked(p)||!p.isAlive()||p.isSpectator()||SoulCombat.suppressed(p))return;
        long now=p.level().getGameTime();
        if(start){if(Curse.fingers(p)<5){p.displayClientMessage(Component.literal("捌需要先吃下5根宿傩手指"),true);return;}cleaveCharges.putIfAbsent(p.getUUID(),new Charge(p.serverLevel(),now));return;}
        Charge charge=cleaveCharges.remove(p.getUUID());if(charge==null||charge.level!=p.serverLevel())return;
        long held=now-charge.start;if(held<14){cast(p,2);return;}if(held>200)return;
        long[] cd=cooldowns.computeIfAbsent(p.getUUID(),k->new long[7]);if(now<cd[2])return;
        LivingEntity aimed=aimTarget(p),target=aimed;Domain d=domains.get(p.getUUID());
        boolean guaranteed=inDomain(p,p);
        if(guaranteed&&target!=null&&!inDomain(p,target))target=null;
        if(target!=null&&!guaranteed&&!remembered(p,target))target=null; // 可空放；必中资格只限制命中。
        double fraction=SukunaBranches.cleaveFactor(p);
        float amount=(float)(Curse.rawMaximum(p)*0.12*Journey.bonus(p)*(Curse.active(p,"Disorder")?0.8:1));
        if(!Curse.spend(p,30))return;cd[2]=now+(long)Math.ceil(40*SoulCombat.cooldown(p));
        Vec3 point=target==null?aimPoint(p,50):target.getBoundingBox().getCenter();
        slashVisual(p,point,p.getLookAngle(),(float)(12*Curse.scale(p)),Curse.awakened(p)?720:0,2,false);
        if(target!=null)impacts.add(new Impact(p,p.serverLevel(),target.getUUID(),now+1,amount,Curse.awakened(p),Curse.awakened(p),SoulJourney.evolved(p),fraction));
        else if(Curse.awakened(p))rift(p,point,null,200);
        SkillSounds.cast(p,2);
    }
    private static final Map<UUID,Set<UUID>> cleaveMemory=new HashMap<>();
    private static double aimDistance(ServerPlayer p,LivingEntity e){Vec3 q=e.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();return 1-q.dot(p.getLookAngle());}
    /** 精确准星优先；未瞄中时才在可调小圆锥内选择最接近准星的可见敌人。 */
    static LivingEntity aimTarget(ServerPlayer p){
        if(p instanceof MaidBridge.Actor a&&a.aim!=null&&canHit(p,a.aim)&&p.hasLineOfSight(a.aim))return a.aim;
        Vec3 eye=p.getEyePosition(),dir=p.getLookAngle().normalize();
        LivingEntity direct=null,assisted=null;double directDistance=Double.MAX_VALUE,bestAngle=Double.MAX_VALUE,bestDistance=Double.MAX_VALUE;
        boolean assist=AimAssist.enabled(p);double angle=AimAssist.angle(p),range=AimAssist.range(p);
        for(var entity:p.serverLevel().getAllEntities())if(entity instanceof LivingEntity e&&canHit(p,e)){
            Vec3 center=e.getBoundingBox().getCenter(),delta=center.subtract(eye);double distance=delta.length();
            if(delta.dot(dir)<=0&&!e.getBoundingBox().contains(eye))continue;
            Vec3 q=e.getBoundingBox().inflate(e.getPickRadius()+0.1).clip(eye,eye.add(dir.scale(distance+e.getBbHeight()+e.getBbWidth()+2))).orElse(null);
            if(q!=null){double d=q.distanceToSqr(eye);if(d<directDistance&&visible(p,eye,q)){direct=e;directDistance=d;}}
            double dot=distance>0?delta.dot(dir)/distance:1,score=1-dot;
            if(assist&&AimRules.inside(dot,distance,angle,range)&&(score<bestAngle-1e-10||Math.abs(score-bestAngle)<=1e-10&&distance<bestDistance)&&visible(p,eye,center)){
                assisted=e;bestAngle=score;bestDistance=distance;
            }
        }
        return direct!=null?direct:assisted;
    }
    private static boolean visible(ServerPlayer p,Vec3 from,Vec3 to){
        ClipContext context=new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p);
        return net.minecraft.world.level.BlockGetter.traverseBlocks(from,to,context,(c,pos)->{
            if(!p.serverLevel().hasChunkAt(pos))return Boolean.FALSE;
            var state=p.serverLevel().getBlockState(pos);var hit=c.getBlockShape(state,p.serverLevel(),pos).clip(from,to,pos);
            return hit!=null&&hit.getLocation().distanceToSqr(from)+0.05<to.distanceToSqr(from)?Boolean.FALSE:null;
        },c->Boolean.TRUE);
    }
    static LivingEntity closestToCrosshair(ServerPlayer p,List<LivingEntity> candidates){LivingEntity t=aimTarget(p);return t!=null&&candidates.contains(t)?t:null;}
    static List<LivingEntity> crosshairOrder(ServerPlayer p,List<LivingEntity> candidates){return candidates.stream().sorted(Comparator.<LivingEntity>comparingDouble(e->aimDistance(p,e)).thenComparingDouble(e->e.distanceToSqr(p))).toList();}
    public static void toggleFist(ServerPlayer p){if(!Curse.canUseTechnique(p))return;if(Curse.fingers(p)<5){p.displayClientMessage(Component.literal("咒力附拳需要先吃下5根宿傩手指"),true);return;}boolean active=Curse.data(p).getBoolean("Fist");Curse.data(p).putBoolean("Fist",!active);Curse.sync(p);p.displayClientMessage(Component.literal("咒力附拳："+(!active?"开启 · 再按 Del 关闭":"关闭")),true);}
    public static void charge(ServerPlayer p,boolean start){
        if(start)CombatExpansion.worldChant(p,TechniqueInput.intent(p));
    }
    private record Aura(ServerPlayer owner,ServerLevel level,long start,long end,double radius){}
    private static final class Domain {
        final ServerPlayer owner;final ServerLevel level;final Vec3 center,shrineOffset;final long start;long end;final double radius;
        Domain(ServerPlayer owner,ServerLevel level,Vec3 center,Vec3 shrineOffset,long start,long end,double radius){this.owner=owner;this.level=level;this.center=center;this.shrineOffset=shrineOffset;this.start=start;this.end=end;this.radius=radius;}
    }
    public static void extendDomain(ServerPlayer p,int ticks){Domain d=domains.get(p.getUUID());if(d!=null)d.end+=ticks;}
    public static void clashVisual(ServerPlayer p,boolean show){Domain d=domains.get(p.getUUID());if(d==null)return;if(!show){fx(p,5,d.center,Vec3.ZERO,0,0,0);return;}if(VoidJourney.evolved(p))syncVoidShrine(d);fx(p,1,d.center,d.shrineOffset,(float)d.radius,(int)Math.max(1,d.end-d.level.getGameTime()),SoulJourney.evolved(p)?-d.start:d.start);syncVoidFloor(p);}
    private record Bow(ServerPlayer owner,ServerLevel level,long release,boolean purple){Bow(ServerPlayer p,ServerLevel l,long release){this(p,l,release,false);}}
    public static void purpleBow(ServerPlayer p){int draw=Curse.tier(p)>=3?20:30;bows.add(new Bow(p,p.serverLevel(),p.level().getGameTime()+draw,true));fx(p,CombatExpansionRules.FX_PURPLE,p.getEyePosition(),p.getLookAngle(),2,draw,1);SkillSounds.cast(p,3);}
    private static class Fire {ServerPlayer owner;ServerLevel level;Vec3 pos,dir;UUID target;int age;boolean purple;Fire(ServerPlayer p){owner=p;level=p.serverLevel();pos=p.getEyePosition();LivingEntity aimed=aimTarget(p);target=aimed==null?null:aimed.getUUID();dir=aimed==null?p.getLookAngle():aimed.getBoundingBox().getCenter().subtract(pos).normalize();}}
    private record Detonation(ServerPlayer owner,ServerLevel level,Vec3 pos,long time,float radius){}
    private static void fx(ServerPlayer p,int kind,Vec3 pos,Vec3 dir,float size,int life,long seed){Network.Effect effect=new Network.Effect(kind,p.getId(),pos,dir,size,life,seed).fromCaster(p);Network.send(p.serverLevel(),effect);if(size!=SlashReach.INFINITE&&(kind==96||kind==72)&&p.position().distanceToSqr(pos)>512*512)Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),effect);}
    private static boolean valid(ServerPlayer p,ServerLevel l){return p.isAlive()&&!p.isRemoved()&&p.serverLevel()==l;}
    public static boolean sustaining(ServerPlayer p){return auras.containsKey(p.getUUID())||domains.containsKey(p.getUUID());}
    /** 灵魂剥离只能由专属请求进入；解和蓄力释放不经过此入口。 */
    public static void peel(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator()||!Journey.unlocked(p))return;
        if(!Curse.canUseTechnique(p)){p.displayClientMessage(Component.literal("术式熔断中：只能使用术式反转或修复熔断"),true);return;}
        if(!SoulJourney.evolved(p)){p.displayClientMessage(Component.literal("灵魂剥离需要先击败灵魂通道的虎杖残影"),true);return;}
        long now=p.serverLevel().getGameTime();long[] times=cooldowns.computeIfAbsent(p.getUUID(),k->new long[7]);
        LivingEntity target=aimTarget(p);
        if(now<times[6]){p.displayClientMessage(Component.literal("灵魂剥离冷却中 · "+((times[6]-now+19)/20)+" 秒"),true);return;}
        if(!Curse.spend(p,Curse.COST[6]))return;
        times[6]=now+COOLDOWN[6];if(target!=null)SoulCombat.peelTarget(p,target);fx(p,92,target==null?aimPoint(p,24):target.getBoundingBox().getCenter(),Vec3.ZERO,1,100,p.getId());SkillSounds.play(p,"soul_peel");
    }
    public static boolean cleaveActive(ServerPlayer p){return auras.containsKey(p.getUUID());}
    public static boolean available(ServerPlayer p,int skill){return skill>=0&&skill<6&&(skill!=5||MaidBridge.domainReady(p)&&(!MaidBridge.isCaster(p)||!GuardianEntity.locked(MaidBridge.body(p))&&!GuardianPlayerSkills.locked(p)))&&!SoulCombat.suppressed(p)&&Curse.canUseTechnique(p)&&p.level().getGameTime()>=cooldowns.getOrDefault(p.getUUID(),new long[7])[skill]&&Curse.canSpend(p,Curse.COST[skill])&&(skill!=2||Curse.fingers(p)>=5)&&(skill!=4||Curse.awakened(p)||domainActive(p))&&(!VoidJourney.evolved(p)||VoidCombat.ready(p,skill));}
    public static boolean peelAvailable(ServerPlayer p){return SoulJourney.evolved(p)&&Curse.canSpend(p,Curse.COST[6])&&p.level().getGameTime()>=cooldowns.getOrDefault(p.getUUID(),new long[7])[6];}
    /** 动作只限制自身的时间轴，不再为普通解添加额外冷却。 */
    static boolean beginMotion(ServerPlayer p){return Curse.spend(p,Curse.COST[0]);}
    /** 按住连发每次只产生一道，费用由服务端按本次按住的累计次数结算。 */
    static void heldSlash(ServerPlayer p){
        if(SlashForms.qi(p)){SlashForms.basic(p);return;}
        if(VoidJourney.evolved(p)){VoidCombat.cast(p,0);return;}
        LivingEntity target=aimTarget(p);Vec3 point=target==null?aimPoint(p,50):target.getBoundingBox().getCenter();
        strikes.add(new Strike(p,p.serverLevel(),target==null?null:target.getUUID(),point,p.getLookAngle(),p.level().getGameTime(),(float)((Curse.awakened(p)?12:7)*Curse.scale(p)),(float)(12*Curse.scale(p)),Journey.nine(p)?1440:Curse.awakened(p)?720:0));
    }
    public static void cast(ServerPlayer p,int skill){
        if(skill<0||skill>=6||!p.isAlive()||p.isSpectator()||!Journey.unlocked(p))return;
        if(!Curse.canUseTechnique(p)){p.displayClientMessage(Component.literal("术式熔断中：只能使用术式反转或修复熔断"),true);return;}
        long now=p.serverLevel().getGameTime();long[] times=cooldowns.computeIfAbsent(p.getUUID(),k->new long[7]);
        if(skill==2&&Curse.fingers(p)<5){p.displayClientMessage(Component.literal("捌需要先吃下5根宿傩手指"),true);return;}
        if(skill==2&&auras.containsKey(p.getUUID())&&!VoidJourney.evolved(p)&&!MaidBridge.body(p).isShiftKeyDown()){auras.remove(p.getUUID());fx(p,9,p.position(),Vec3.ZERO,0,0,0);return;}
        if(skill==5&&GuardianPlayerSkills.sustaining(p)){GuardianPlayerSkills.closeOwnedDomain(p,true);return;}
        if(skill==5&&MaidBridge.isCaster(p)&&(GuardianEntity.locked(MaidBridge.body(p))||GuardianPlayerSkills.locked(p))){closeDomain(p);p.displayClientMessage(Component.literal("净土领域优先，御厨子无法展开"),true);return;}
        if(skill==5&&domains.containsKey(p.getUUID())){closeDomain(p,true);return;}
        if(skill==5&&!MaidBridge.domainReady(p))return;
        if(SoulCombat.suppressed(p)&&!(skill==5&&Curse.active(p,"Iwate")&&!Curse.active(p,"SoulGap")&&p.getPersistentData().getLong("VoidCaptured")<=now)){p.displayClientMessage(Component.literal("灵魂剥离或灵魂压制中，无法使用术式"),true);return;}
        if(now<times[skill]){p.displayClientMessage(Component.literal("术式冷却中 · "+String.format(Locale.ROOT,"%.1f",(times[skill]-now)/20.0)+" 秒"),true);return;}
        boolean awakened=Curse.awakened(p);int tier=Curse.tier(p);double scale=Curse.scale(p);
        if(skill==4&&!awakened&&!domains.containsKey(p.getUUID())){p.displayClientMessage(Component.literal("灶需要先展开伏魔御厨子积聚粉尘"),true);return;}
        if(VoidJourney.evolved(p)&&!VoidCombat.ready(p,skill))return;
        if(!Curse.spend(p,Curse.COST[skill]))return;
        fx(p,67,p.position(),Vec3.ZERO,Journey.nine(p)?1:0,1,0);SkillSounds.cast(p,skill);
        if(skill!=3)fx(p,70,p.position(),p.getLookAngle(),skill,skill==5?30:12,skill);
        times[skill]=now+(long)Math.ceil(COOLDOWN[skill]*SoulCombat.cooldown(p)*(Curse.active(p,"Siphon")?1.3:1)*(tier>=4?0.8:1));
        if(CombatExpansion.afterCast(p,skill))return;
        if(skill==0&&SlashForms.qi(p)){SlashForms.basic(p);return;}
        if(VoidJourney.evolved(p)&&skill!=5){if(skill==3)fx(p,70,p.position(),p.getLookAngle(),3,60,3);VoidCombat.cast(p,skill);return;}
        Vec3 forward=p.getLookAngle(),side=new Vec3(-forward.z,0,forward.x).normalize();if(side.lengthSqr()<0.1)side=new Vec3(1,0,0);
        switch(skill){
            case 0 -> {
                LivingEntity target=aimTarget(p);Vec3 point=target==null?aimPoint(p,50):target.getBoundingBox().getCenter();double distance=point.distanceTo(p.getEyePosition());double falloff=Math.pow(0.95,SoulJourney.evolved(p)?0:Math.max(0,(int)(distance/10)));int count=TechniqueInput.holding(p,0)?1:tier>=3?3:tier>=1?2:1;
                for(int i=0;i<count;i++)strikes.add(new Strike(p,p.serverLevel(),target==null?null:target.getUUID(),point,forward,now+i*SlashMotion.INTERVAL,(float)((awakened?12:7)*scale*falloff),(float)(12*scale),Journey.nine(p)?1440:awakened?720:0));
            }
            case 1 -> {
                if(awakened){
                    fx(p,22,p.position().add(0,1,0),Vec3.ZERO,(float)(12*scale),32,now);
                    for(int i=0;i<72;i++){double y=1-2*(i+0.5)/72.0,a=i*2.39996323,r=Math.sqrt(1-y*y);Vec3 d=new Vec3(Math.cos(a)*r,y,Math.sin(a)*r);slash(p,p.getEyePosition().add(d),d,side,(float)(10*scale));}
                }else for(int i=0;i<(tier>=2?16:12);i++){
                    int count=tier>=2?16:12;double a=i*Math.PI*2/count;Vec3 d=new Vec3(Math.cos(a),0,Math.sin(a)),s=new Vec3(-d.z,0,d.x);
                    for(int j=-1;j<=1;j++){slash(p,p.position().add(0,1.5+j*2.5*scale,0).add(d.scale(1.5)),d,s,(float)(10*scale));slash(p,p.position().add(0,1.5,0).add(d.scale(1.5)).add(s.scale(j*2.5*scale)),d,new Vec3(0,1,0),(float)(10*scale));}
                }
            }
            case 2 -> {airCuts.put(p.getUUID(),0);long life=tier>=2?600:400;auras.put(p.getUUID(),new Aura(p,p.serverLevel(),now,now+life,10*scale));fx(p,8,p.position(),Vec3.ZERO,(float)(10*scale),(int)life,now);p.displayClientMessage(Component.literal(awakened?"虚空捌开启 · 伤害随剩余咒力百分比变化，满咒力150% · 金边裂缝追敌":"捌开启 · 每秒30咒力 · 再按释放可关闭"),true);}
            case 3 -> {int draw=tier>=3?20:30;bows.add(new Bow(p,p.serverLevel(),now+draw));fx(p,2,p.position(),forward,1,draw,0);}
            case 4 -> furnaces.add(new Furnace(p,p.serverLevel(),now+40));
            case 5 -> {MaidBridge.domainOpened(p);double yaw=Math.toRadians(p.getYRot());Vec3 behind=new Vec3(Math.sin(yaw)*12,0,-Math.cos(yaw)*12);int duration=ShrineRules.domainTicks(MaidBridge.isCaster(p));Domain d=new Domain(p,p.serverLevel(),p.position(),behind,now,now+duration,200);domains.put(p.getUUID(),d);if(VoidJourney.evolved(p))syncVoidShrine(d);fx(p,1,d.center,d.shrineOffset,(float)d.radius,duration,SoulJourney.evolved(p)?-now:now);fx(p,25,p.position(),Vec3.ZERO,CombatSettings.voidFloor(p)?1:0,10,0);p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS,4,0.5f);p.displayClientMessage(Component.literal(VoidJourney.evolved(p)?"虚空御厨子 · "+(duration/20)+"秒 · 半径200格 · 五裂共鸣 · 结束闭合爆发":"领域展开 · 半径200格 · 每秒70咒力 · 再次释放收回"),true);}
        }
        if(skill==5)DomainClash.opened(p,domainCenter(p),200);
        if(skill!=0&&skill!=4)p.serverLevel().playSound(null,p.blockPosition(),skill==3?SoundEvents.BLAZE_SHOOT:SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,1.5f,skill==5?0.5f:0.8f);
    }
    static Vec3 aimPoint(ServerPlayer p,double range){
        Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(range));var block=p.serverLevel().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return block.getType()==HitResult.Type.MISS?end:block.getLocation();
    }
    private static long slashVisual(ServerPlayer p,Vec3 point,Vec3 dir,float size,long tint,int gesture,boolean hit){
        long angle=p.getRandom().nextInt(360);
        // 捌等其他斩击的表现；解的两段剪裁使用独立队列。
        if(SoulJourney.evolved(p))fx(p,93,point,dir,size,SlashMotion.LIFE,angle+2000);
        else fx(p,gesture==0?72:12,point,dir,size,SlashMotion.LIFE,angle+tint);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,1.2f,0.65f);
        return angle;
    }
    private static void slash(ServerPlayer p,Vec3 pos,Vec3 dir,Vec3 axis,float size){
        if(slashes.size()>=400)return;long angle=p.getRandom().nextInt(360);Vec3 side=new Vec3(-dir.z,0,dir.x).normalize();if(side.lengthSqr()<0.1)side=new Vec3(1,0,0);Vec3 up=dir.cross(side).normalize();double a=Math.toRadians(angle);axis=side.scale(Math.cos(a)).add(up.scale(Math.sin(a))).normalize();Slash s=new Slash(p,pos,dir,axis,size);slashes.add(s);fx(p,0,pos,dir,size,s.life,angle+(SoulJourney.evolved(p)?2000:Journey.nine(p)?1440:s.world?720:0));
    }
    private static void hitEffect(ServerLevel l,ServerPlayer caster,LivingEntity target,int kind,boolean red){Vec3 center=target.getBoundingBox().getCenter();float size=kind==10?Math.max(1.5f,Math.min(6,target.getBbHeight()*1.5f)):Math.max(4,target.getBbHeight()*3);Network.send(l,new Network.Effect(red?11:kind,target.getId(),center,center.subtract(target.position()),size,kind==10?6:10,target.getRandom().nextLong()).fromCaster(caster));}
    private static void hitEffectSoul(ServerLevel l,ServerPlayer caster,LivingEntity target){if(!target.isAlive())return;Vec3 center=target.getBoundingBox().getCenter();Network.send(l,new Network.Effect(93,target.getId(),center,Vec3.ZERO,Math.max(1,target.getBbHeight()),12,target.getRandom().nextLong()).fromCaster(caster));}
    private static double projection(LivingEntity e,Vec3 axis){return (Math.abs(axis.x)+Math.abs(axis.z))*e.getBbWidth()/2+Math.abs(axis.y)*e.getBbHeight()/2;}
    private static boolean canHit(ServerPlayer p,LivingEntity e){return CombatSettings.canHit(p,e);}
    private static boolean damage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world,boolean slash,boolean soulSlash,boolean domain){if(DomainClash.active(p)||DomainClash.active(e)||!canHit(p,e))return false;soulSlash=soulSlash||SoulJourney.evolved(p)&&slash;if(soulSlash){n+=Curse.soulSlashBonus(p);e.getPersistentData().putUUID("SukunaSoulSlashOwner",p.getUUID());e.getPersistentData().putBoolean("SukunaNoHeal",true);}n=domain?CombatSettings.domainSpellDamage(p,n):CombatSettings.spellDamage(p,n);if(domain&&slash)n*=(float)SlashFormRules.DAMAGE;float before=e.getHealth()+e.getAbsorptionAmount();e.invulnerableTime=0;boolean accepted=CombatSettings.hurt(p,e,world?l.damageSources().sonicBoom(p):l.damageSources().indirectMagic(p,p),n);if(accepted&&e instanceof BuddhaEntity boss&&world)boss.interruptSiphon();boolean hurt=accepted&&e.getHealth()+e.getAbsorptionAmount()<before;if(hurt&&slash&&Journey.nine(p)&&e.isAlive())joint(p,e);if(hurt&&soulSlash)SoulCombat.soulHit(p,e);return hurt;}
    static List<LivingEntity> targets(ServerLevel l,ServerPlayer p,Vec3 c,double r){if(r<=128)return l.getEntitiesOfClass(LivingEntity.class,new AABB(c,c).inflate(r),e->canHit(p,e));List<LivingEntity> found=new ArrayList<>();for(var entity:l.getAllEntities())if(entity instanceof LivingEntity e&&canHit(p,e)&&e.position().distanceToSqr(c)<=r*r)found.add(e);return found;}
    private static boolean damage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world){return damage(l,p,e,n,world,true,false);}
    private static boolean damage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world,boolean slash){return damage(l,p,e,n,world,slash,false);}
    private static boolean damage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world,boolean slash,boolean soulSlash){return scaledDamage(l,p,e,n,world,slash,soulSlash,1);}
    private static boolean scaledDamage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world,boolean slash,boolean soulSlash,double fraction){if(DomainClash.active(p)||DomainClash.active(e)||!canHit(p,e))return false;soulSlash=soulSlash||SoulJourney.evolved(p)&&slash;if(soulSlash){n+=Curse.soulSlashBonus(p);e.getPersistentData().putUUID("SukunaSoulSlashOwner",p.getUUID());e.getPersistentData().putBoolean("SukunaNoHeal",true);}n=(float)(CombatSettings.spellDamage(p,n)*fraction);float before=e.getHealth()+e.getAbsorptionAmount();e.invulnerableTime=0;boolean accepted=CombatSettings.hurt(p,e,world?l.damageSources().sonicBoom(p):l.damageSources().indirectMagic(p,p),n);if(accepted&&e instanceof BuddhaEntity boss&&world)boss.interruptSiphon();boolean hurt=accepted&&e.getHealth()+e.getAbsorptionAmount()<before;if(hurt&&slash&&Journey.nine(p)&&e.isAlive())joint(p,e);if(hurt&&soulSlash)SoulCombat.soulHit(p,e);return hurt;}
    private static boolean hitDamage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world){return damage(l,p,e,n*(float)Curse.power(p),world);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        VoidCombat.tick();
        for(Iterator<MeleeCut> it=meleeCuts.iterator();it.hasNext();){
            MeleeCut cut=it.next();if(!valid(cut.owner,cut.level)){it.remove();continue;}if(cut.level.getGameTime()<cut.due)continue;it.remove();
            if(cut.level.getEntity(cut.target) instanceof LivingEntity target&&canHit(cut.owner,target)){
                fx(cut.owner,72,target.getBoundingBox().getCenter(),cut.owner.getLookAngle(),Math.max(4,target.getBbHeight()*2),SlashMotion.LIFE,cut.owner.getRandom().nextInt(360));
                target.invulnerableTime=0;CombatSettings.hurt(cut.owner,target,cut.level.damageSources().sonicBoom(cut.owner),CombatSettings.casterDamage(cut.owner,cut.amount)*(float)SlashFormRules.DAMAGE);
            }
        }
        burns.entrySet().removeIf(entry->entry.getKey().isRemoved()||!entry.getKey().isAlive()||!entry.getKey().isOnFire()||entry.getKey().level().getGameTime()>entry.getValue().until);
        charges.entrySet().removeIf(entry->{Charge c=entry.getValue();ServerPlayer p=c.level.getServer().getPlayerList().getPlayer(entry.getKey());if(p!=null&&valid(p,c.level)&&c.level.getGameTime()-c.start==CHARGE_DELAY)SkillSounds.play(p,"space_charge");return p==null||!valid(p,c.level)||c.level.getGameTime()-c.start>200;});
        for(Iterator<Strike> it=strikes.iterator();it.hasNext();){
            Strike s=it.next();if(!valid(s.owner,s.level)){it.remove();continue;}if(s.level.getGameTime()<s.due)continue;it.remove();
            LivingEntity target=s.target!=null&&s.level.getEntity(s.target) instanceof LivingEntity e&&canHit(s.owner,e)?e:null;
            if(s.target!=null&&target==null)continue;
            Vec3 point=target==null?s.point:target.getBoundingBox().getCenter();
            if(target!=null){
                Vec3 toward=s.owner.getEyePosition().subtract(point).normalize();
                point=target.getBoundingBox().inflate(0.08).clip(s.owner.getEyePosition(),point).orElse(point).add(toward.scale(0.05));
            }else point=point.subtract(s.direction.scale(0.08));
            Vec3 collisionAxis=NetBranches.side(s.direction).scale(s.size/2);if(CombatExpansion.collideSlash(s.owner,point.subtract(collisionAxis),point.add(collisionAxis),s.direction))continue;
            if(SoulJourney.evolved(s.owner))soulDismantle(s.owner,target,point,s.direction,SlashReach.INFINITE,s.amount,s.tint>=720);
            else {slashVisual(s.owner,point,s.direction,s.size,s.tint,0,true);
                if(target!=null)impacts.add(new Impact(s.owner,s.level,s.target,s.level.getGameTime()+1,s.amount,s.tint>=720,false,false,SlashFormRules.DAMAGE));}
        }
        for(Iterator<SoulCut> it=soulCuts.iterator();it.hasNext();){
            SoulCut s=it.next();if(!valid(s.owner,s.level)){it.remove();continue;}if(s.level.getGameTime()<s.due)continue;it.remove();
            if(s.size!=0){fx(s.owner,72,s.point,s.direction,s.size,SlashMotion.LIFE,s.angle);
            s.level.playSound(null,s.point.x,s.point.y,s.point.z,SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,1.2f,0.65f);}
            if(s.target!=null&&s.level.getEntity(s.target) instanceof LivingEntity target&&canHit(s.owner,target))scaledDamage(s.level,s.owner,target,s.amount,s.world,false,true,SlashFormRules.DAMAGE);
        }
        for(Iterator<Impact> it=impacts.iterator();it.hasNext();){Impact i=it.next();if(!valid(i.owner,i.level)){it.remove();continue;}if(i.level.getGameTime()<i.due)continue;it.remove();var entity=i.level.getEntity(i.target);if(entity instanceof LivingEntity target&&canHit(i.owner,target)){Vec3 point=target.getBoundingBox().getCenter();scaledDamage(i.level,i.owner,target,i.amount,i.world,false,i.soulSlash,i.fraction);if(i.soulSlash)hitEffectSoul(i.level,i.owner,target);if(i.rift)rift(i.owner,point,target.isAlive()?target:null,200);}}
        for(Iterator<Furnace> it=furnaces.iterator();it.hasNext();){
            Furnace f=it.next();if(!valid(f.owner,f.level)){it.remove();continue;}if(f.level.getGameTime()<f.due)continue;it.remove();ServerPlayer p=f.owner;
            boolean domain=domains.containsKey(p.getUUID());if(!domain&&!Curse.awakened(p))continue;double radius=domain?200:50;
            for(LivingEntity target:targets(f.level,p,p.position(),radius))if(target.distanceToSqr(p)<=radius*radius){damage(f.level,p,target,16*(float)Curse.power(p)*(domain?1.5f:1),false,false,false,domain);ignite(p,target,12);fx(p,4,target.getBoundingBox().getCenter(),Vec3.ZERO,4,30,f.due);}
            fx(p,23,p.position(),Vec3.ZERO,(float)radius,45,f.due);
        }
        for(Iterator<Joint> it=joints.values().iterator();it.hasNext();){Joint j=it.next();if(!valid(j.owner,j.level)){it.remove();continue;}if(j.level.getGameTime()<j.due)continue;it.remove();var entity=j.level.getEntity(j.target);if(entity instanceof LivingEntity target&&canHit(j.owner,target)){damage(j.level,j.owner,target,j.power,true,false);fx(j.owner,59,target.getBoundingBox().getCenter(),Vec3.ZERO,4,20,target.getId());SkillSounds.play(j.owner,"fetus_joint");}}
        tickRifts();
        for(Iterator<Slash> it=slashes.iterator();it.hasNext();){Slash s=it.next();if(++s.age>s.life||!valid(s.owner,s.level)){it.remove();continue;}Vec3 old=s.pos;s.pos=s.pos.add(s.dir.scale(2));if(CombatExpansion.collideSlash(s.owner,s.pos.subtract(s.axis.scale(s.size/2)),s.pos.add(s.axis.scale(s.size/2)),s.dir)){it.remove();continue;}SkillDestruction.line(s.owner,s.pos.subtract(s.axis.scale(s.size/2)),s.pos.add(s.axis.scale(s.size/2)),0.6);AABB swept=new AABB(old,s.pos).inflate(s.size/2+1);
            for(LivingEntity e:targets(s.level,s.owner,s.pos,s.size/2+3)){if(!e.getBoundingBox().intersects(swept))continue;Vec3 q=e.getBoundingBox().getCenter().subtract(old.lerp(s.pos,0.5));double a=q.dot(s.axis),b=q.dot(s.dir);Vec3 normal=s.axis.cross(s.dir).normalize();double halfThickness=0.45;float amount=s.space?(float)((YujiEntity.percent(e,e.getMaxHealth()*0.25f)+300)*Journey.bonus(s.owner)*(Curse.active(s.owner,"Disorder")?0.8:1)):(s.world?12:6)*s.power;boolean soul=SoulJourney.evolved(s.owner);if(Math.abs(a)<s.size/2+projection(e,s.axis)&&Math.abs(b)<1+projection(e,s.dir)&&Math.abs(q.dot(normal))<halfThickness+projection(e,normal)&&(Journey.rotation(s.owner)||s.hit.add(e.getUUID()))&&scaledDamage(s.level,s.owner,e,amount,s.world,true,soul,SlashFormRules.DAMAGE)){if(soul)hitEffectSoul(s.level,s.owner,e);else hitEffect(s.level,s.owner,e,7,s.world);}}
        }
        for(Iterator<Aura> it=auras.values().iterator();it.hasNext();){Aura a=it.next();long now=a.level.getGameTime(),age=now-a.start;ServerPlayer p=a.owner;
            if(!valid(p,a.level)||!Curse.canUseTechnique(p)||now>=a.end||(age>0&&age%20==0&&!Curse.spend(p,30))){Network.send(a.level,new Network.Effect(9,p.getId(),p.position(),Vec3.ZERO,0,0,0));it.remove();continue;}
            if(age%20==0){fx(p,67,p.position(),Vec3.ZERO,Journey.nine(p)?1:0,1,0);fx(p,8,p.position(),Vec3.ZERO,(float)a.radius,(int)(a.end-now),a.start);}
            boolean awake=Curse.awakened(p);
            if(awake&&age>0&&age%10==0&&airCuts.getOrDefault(p.getUUID(),0)<20){airCuts.merge(p.getUUID(),1,Integer::sum);double angle=p.getRandom().nextDouble()*Math.PI*2,radius=Math.sqrt(p.getRandom().nextDouble())*a.radius;Vec3 point=p.position().add(Math.cos(angle)*radius,0.5+p.getRandom().nextDouble()*5,Math.sin(angle)*radius);rift(p,point,null,a.radius);}
            if(age>0&&age%(Journey.rotation(p)?1:awake?100:10)==0)for(LivingEntity target:targets(a.level,p,p.position(),a.radius))if(target.distanceToSqr(p)<=a.radius*a.radius){boolean hit=awake?scaledDamage(a.level,p,target,(float)((YujiEntity.percent(target,target.getMaxHealth()*0.1f)+20)*Journey.bonus(p)*(Curse.active(p,"Disorder")?0.8:1)),true,true,false,SukunaBranches.cleaveFactor(p)):scaledDamage(a.level,p,target,(float)(4*Curse.power(p)),false,true,false,SukunaBranches.cleaveFactor(p));if(hit){if(SoulJourney.evolved(p))hitEffectSoul(a.level,p,target);else hitEffect(a.level,p,target,7,awake);if(awake&&age%10==0)rift(p,target.getBoundingBox().getCenter().add(0,1,0),target,a.radius);}}
        }
        for(Iterator<Domain> it=domains.values().iterator();it.hasNext();){Domain d=it.next();long now=d.level.getGameTime(),age=now-d.start;ServerPlayer p=d.owner;
            if(DomainClash.active(p))continue;
            if(!valid(p,d.level)||now>=d.end||(age>0&&age%20==0&&!Curse.spend(p,70))){VoidCombat.closeDomain(p,valid(p,d.level)&&!clashing(d));Network.send(d.level,new Network.Effect(5,p.getId(),d.center,Vec3.ZERO,0,0,0));it.remove();Curse.endDomain(p,false);continue;}
            boolean clash=clashing(d);
            if(VoidJourney.evolved(p)){
                if(!clash&&age%2==0)for(LivingEntity target:targets(d.level,p,d.center,d.radius))VoidCombat.domainHit(p,target);
                if(age%40==0){syncVoidShrine(d);fx(p,1,d.center,d.shrineOffset,(float)d.radius,(int)(d.end-now),-d.start);}
                continue;
            }
            if(!clash&&age%(Journey.rotation(p)?1:2)==0)for(LivingEntity target:targets(d.level,p,d.center,d.radius))for(int cut=0;cut<5&&target.isAlive();cut++)if(target.position().distanceToSqr(d.center)<=d.radius*d.radius&&damage(d.level,p,target,(Curse.tier(p)>=5?1.25f:1)*(float)Curse.power(p),SoulJourney.evolved(p),true,SoulJourney.evolved(p),true)){boolean red=Curse.awakened(p);if(red)ignite(p,target,4);if(SoulJourney.evolved(p))hitEffectSoul(d.level,p,target);else hitEffect(d.level,p,target,10,red);}
            if(age%40==0){if(VoidJourney.evolved(p))syncVoidShrine(d);fx(p,67,p.position(),Vec3.ZERO,Journey.nine(p)?1:0,1,0);Network.send(d.level,new Network.Effect(1,p.getId(),d.center,d.shrineOffset,(float)d.radius,(int)(d.end-now),SoulJourney.evolved(p)?-d.start:d.start));fx(p,25,p.position(),Vec3.ZERO,CombatSettings.voidFloor(p)?1:0,2,0);}
            if(!clash&&Curse.awakened(p)&&age%20==0){
                fx(p,24,d.center,d.shrineOffset,(float)d.radius,40,now);
                // 领域粉尘自动燃烧；逐个目标判断敌友，避免环境火误伤盟友。
                for(LivingEntity target:targets(d.level,p,d.center,d.radius))if(target.position().distanceToSqr(d.center)<=d.radius*d.radius){ignite(p,target,3);damage(d.level,p,target,(float)(2*Curse.power(p)),false,false,false,true);}
            }
        }
        for(Iterator<Bow> it=bows.iterator();it.hasNext();){Bow b=it.next();if(!valid(b.owner,b.level)){it.remove();continue;}if(b.level.getGameTime()>=b.release){Fire f=new Fire(b.owner);f.purple=b.purple;fires.add(f);if(!f.purple)fx(b.owner,3,f.pos,f.dir,2,100,0);it.remove();}}
        for(Iterator<Fire> it=fires.iterator();it.hasNext();){Fire f=it.next();if(!valid(f.owner,f.level)||++f.age>100){it.remove();continue;}Vec3 next=f.pos.add(f.dir.scale(3));if(f.purple)fx(f.owner,CombatExpansionRules.FX_PURPLE,next,f.dir,2,3,0);var block=f.level.clip(new ClipContext(f.pos,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,f.owner));boolean hit=block.getType()!=HitResult.Type.MISS;for(LivingEntity target:targets(f.level,f.owner,f.pos,5))if(target.getBoundingBox().inflate(1).clip(f.pos,next).isPresent()){hit=true;next=target.position();break;}if(hit){if(block.getType()!=HitResult.Type.MISS)next=block.getLocation();detonate(f.owner,f.level,next,(float)(12*Curse.scale(f.owner)*(Curse.awakened(f.owner)?1.5:1)),f.purple);if(Curse.awakened(f.owner)&&!f.purple)fx(f.owner,23,next,Vec3.ZERO,32,60,0);it.remove();}else f.pos=next;}
        for(Iterator<Detonation> it=detonations.iterator();it.hasNext();){Detonation d=it.next();if(!valid(d.owner,d.level)){it.remove();continue;}if(d.level.getGameTime()>=d.time){detonate(d.owner,d.level,d.pos,d.radius);it.remove();}}
    }
    private static void tickRifts(){
        for(Iterator<Rift> it=rifts.iterator();it.hasNext();){Rift r=it.next();r.age++;
            if(!valid(r.owner,r.level)||r.age>140){Network.send(r.level,new Network.Effect(63,r.owner.getId(),r.pos,Vec3.ZERO,0,1,r.id));it.remove();continue;}
            if(r.age<12)continue;
            LivingEntity target=r.target!=null&&r.level.getEntity(r.target) instanceof LivingEntity e&&canHit(r.owner,e)?e:null;
            if(target==null&&r.age%4==0){target=targets(r.level,r.owner,r.owner.position(),r.radius).stream().filter(e->e.position().distanceToSqr(r.owner.position())<=r.radius*r.radius).min(Comparator.<LivingEntity>comparingDouble(e->aimDistance(r.owner,e)).thenComparingDouble(e->e.distanceToSqr(r.owner))).orElse(null);r.target=target==null?null:target.getUUID();if(target!=null)r.dir=target.getBoundingBox().getCenter().subtract(r.pos).normalize();}
            if(target==null)continue;
            Vec3 aim=target.getBoundingBox().getCenter().subtract(r.pos).normalize();
            if(r.age==12||r.age%5==0||target.getBoundingBox().getCenter().distanceToSqr(r.pos)<36){Vec3 side=new Vec3(-aim.z,0,aim.x).normalize();double zig=r.age<37&&target.getBoundingBox().getCenter().distanceToSqr(r.pos)>=36?((r.age/5)%2==0?1.8:-1.8):0;r.dir=aim.add(side.scale(zig)).normalize();}
            Vec3 next=r.pos.add(r.dir.scale(2.4));boolean hit=false;
            for(LivingEntity victim:targets(r.level,r.owner,r.pos,4))if(victim.getBoundingBox().inflate(0.25).clip(r.pos,next).isPresent()||victim.getBoundingBox().inflate(0.25).contains(r.pos)){damage(r.level,r.owner,victim,8*r.power,true,false);fx(r.owner,64,victim.getBoundingBox().getCenter(),r.dir,3,18,r.id);hit=true;break;}
            if(hit){Network.send(r.level,new Network.Effect(63,r.owner.getId(),r.pos,Vec3.ZERO,0,1,r.id));it.remove();continue;}
            r.pos=next;Network.send(r.level,new Network.Effect(62,r.owner.getId(),r.pos,r.dir,1,2,r.id));
        }
    }
    private static void detonate(ServerPlayer p,ServerLevel l,Vec3 pos,float radius){detonate(p,l,pos,radius,false);}
    private static void detonate(ServerPlayer p,ServerLevel l,Vec3 pos,float radius,boolean purple){Network.send(l,new Network.Effect(purple?CombatExpansionRules.FX_PURPLE:4,p.getId(),pos,Vec3.ZERO,radius,30,purple?2:l.getGameTime()));if(purple)SkillDestruction.impact(p,pos,4);l.playSound(null,pos.x,pos.y,pos.z,SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,3,0.7f);for(LivingEntity e:targets(l,p,pos,radius))if(e.position().distanceToSqr(pos)<=radius*radius){damage(l,p,e,16*(float)Curse.power(p),false,false);if(purple){Curse.permanentSoulFire(p,e);fx(p,CombatExpansionRules.FX_PURPLE,e.getBoundingBox().getCenter(),new Vec3(0,1,0),1,20,0);}else ignite(p,e,8);Vec3 push=e.position().subtract(pos).normalize().scale(1.5);e.push(push.x,0.4,push.z);}}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){slashes.clear();auras.clear();domains.clear();bows.clear();fires.clear();detonations.clear();cooldowns.clear();charges.clear();cleaveCharges.clear();cleaveMemory.clear();impacts.clear();meleeCuts.clear();strikes.clear();soulCuts.clear();furnaces.clear();joints.clear();rifts.clear();airCuts.clear();burns.clear();nextRift=0;}
}
