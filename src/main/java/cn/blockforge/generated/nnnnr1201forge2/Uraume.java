package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;

/** 冰系施法、强控和成长均在服务端执行，不永久改变重力或AI。 */
public final class Uraume {
    private static final class Breath {
        final ServerPlayer owner;final ServerLevel level;final long end;long nextDamage;final Set<UUID> frozen=new HashSet<>();
        Breath(ServerPlayer p){owner=p;level=p.serverLevel();end=level.getGameTime()+100;nextDamage=level.getGameTime();}
    }
    private static final class Pillar {
        final ServerPlayer owner;final ServerLevel level;final Vec3 start,velocity;final long born;final int delay;Vec3 position;int age;
        Pillar(ServerPlayer p,Vec3 start,Vec3 velocity,int delay){owner=p;level=p.serverLevel();this.start=start;this.velocity=velocity;this.delay=delay;position=start;born=level.getGameTime();}
    }
    private record Frozen(ServerPlayer owner,ServerLevel level,LivingEntity target,Vec3 anchor,long end,long id){}
    private static final List<Breath> breaths=new ArrayList<>();
    private static final List<Pillar> pillars=new ArrayList<>();
    private static final Map<UUID,Frozen> frozen=new HashMap<>();
    private static long nextFreeze;
    public static boolean unlocked(Player p){return character.yuta.CopyBorrow.borrowing(p)||character.CharacterService.family(p,character.CharacterDefinition.Family.ICE)||character.CharacterService.permits(p,character.CharacterDefinition.Family.ICE)&&Curse.data(p).getBoolean("UraumeMemory");}
    public static int fish(Player p){return Math.max(0,Curse.data(p).getInt("IceFish"));}
    public static int upgrades(Player p){return BranchRules.upgrades(fish(p));}
    public static double length(Player p){return BranchRules.iceLength(fish(p));}
    public static double width(Player p){return BranchRules.iceWidth(fish(p));}
    public static double height(Player p){return BranchRules.iceHeight(fish(p));}
    public static double scatter(Player p){return BranchRules.iceScatter(fish(p));}
    public static int pillarBonus(Player p){return BranchRules.icePillars(fish(p));}
    public static boolean consumeMemory(ServerPlayer p){
        if(!character.CharacterService.permits(p,character.CharacterDefinition.Family.ICE)||!BranchRules.acceptMemory(Journey.unlocked(p),unlocked(p),MaidBridge.isCaster(p),true))return false;
        Curse.data(p).putBoolean("UraumeMemory",true);Curse.data(p).putBoolean("MemoryGiven",true);Curse.addRaw(p,400);Curse.sync(p);
        p.sendSystemMessage(Component.literal("里梅记忆苏醒：长按 N 选霜凪或直瀑，R 释放；吃冰鱼强化。"));return true;
    }
    public static boolean feedFish(ServerPlayer p){
        if(!unlocked(p)||fish(p)==Integer.MAX_VALUE)return false;
        int old=upgrades(p);Curse.data(p).putInt("IceFish",fish(p)+1);
        Curse.addRaw(p,EnergyRules.ICE_FISH_BONUS);Curse.sync(p);
        p.displayClientMessage(Component.literal("冰鱼 "+fish(p)+" 条 · 冰伤 +"+fish(p)+"% · 咒力上限 +"+BalanceRules.number(EnergyRules.ICE_FISH_BONUS)+"（现为 "+BalanceRules.number(Curse.maximum(p))+"）"+(upgrades(p)>old?" · 技能范围、冰柱数量与精度强化！":"")),true);return true;
    }
    public static boolean locked(LivingEntity t){if(t instanceof MaidBridge.Actor a)t=a.maid;Frozen f=frozen.get(t.getUUID());return f!=null&&f.target==t&&validFreeze(f);}
    private static boolean valid(ServerPlayer p,ServerLevel l){return p.isAlive()&&!p.isRemoved()&&p.serverLevel()==l&&MaidBridge.body(p).level()==l&&!EternalLove.settling(MaidBridge.body(p))&&!DomainClash.active(p);}
    private static boolean validFreeze(Frozen f){return valid(f.owner,f.level)&&f.target.isAlive()&&!f.target.isRemoved()&&f.target.level()==f.level&&f.level.getGameTime()<f.end&&CombatSettings.canHit(f.owner,f.target)&&!DomainClash.active(f.target);}
    public static int cost(int skill){return skill==BranchRules.FROST?160:240;}
    public static boolean breathing(ServerPlayer p){return breaths.stream().anyMatch(b->b.owner==p&&valid(p,b.level)&&p.level().getGameTime()<b.end);}
    public static boolean available(ServerPlayer p,int skill){return (skill==BranchRules.FROST||skill==BranchRules.ICEFALL)&&unlocked(p)&&valid(p,p.serverLevel())&&!p.isSpectator()&&!MaidBridge.recovering(p)&&!SoulCombat.suppressed(p)&&Curse.canUseTechnique(p)&&!Curse.active(p,"IceCooldown"+skill)&&Curse.canSpend(p,cost(skill));}
    public static void cast(ServerPlayer p,int skill){
        if(skill!=BranchRules.FROST&&skill!=BranchRules.ICEFALL)return;
        if(CastFeedback.cooldown(p,"IceCooldown"+skill,BranchRules.name(skill)))return;
        if(!unlocked(p)){CastFeedback.tell(p,"需要先吸收里梅记忆");return;}
        if(!available(p,skill)){CastFeedback.unavailable(p,cost(skill));return;}
        if((skill==BranchRules.FROST&&breaths.size()>=128)||(skill==BranchRules.ICEFALL&&pillars.size()+200>4096)){p.displayClientMessage(Component.literal("当前冰系施法过多，请稍后再试"),true);return;}
        if(!Curse.spend(p,cost(skill)))return;Curse.status(p,"IceCooldown"+skill,skill==BranchRules.FROST?300:240);
        if(skill==BranchRules.FROST){if(breaths.size()<128)breaths.add(new Breath(p));return;}
        Vec3 direction=p.getLookAngle().normalize(),side=NetBranches.side(direction),up=direction.cross(side).normalize();LivingEntity aim=Skills.aimTarget(p);
        Vec3 center=aim==null?Skills.aimPoint(p,48):aim.getBoundingBox().getCenter();int count=50+p.getRandom().nextInt(11)+pillarBonus(p);
        for(int i=0;i<count&&pillars.size()<4096;i++){
            double angle=p.getRandom().nextDouble()*Math.PI*2,radius=Math.sqrt(p.getRandom().nextDouble())*scatter(p);
            Vec3 target=center.add(side.scale(Math.cos(angle)*radius)).add(up.scale(Math.sin(angle)*radius));
            Vec3 start=p.getEyePosition().subtract(direction.scale(2+p.getRandom().nextDouble()*4)).add(side.scale((p.getRandom().nextDouble()-0.5)*8)).add(0,1+p.getRandom().nextDouble()*4,0);
            pillars.add(new Pillar(p,start,target.subtract(start).normalize().scale(2.0),i/6));
        }
        p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.GLASS_BREAK,net.minecraft.sounds.SoundSource.PLAYERS,1.4f,0.6f);
    }
    private static void hurt(ServerPlayer p,LivingEntity t,double base){
        if(!CombatSettings.canHit(p,t)||DomainClash.active(t)||DomainClash.active(p))return;
        t.invulnerableTime=0;var source=new net.minecraft.world.damagesource.DamageSource(p.damageSources().freeze().typeHolder(),MaidBridge.body(p));
        float damage=(float)(CombatSettings.spellDamage(p,base)*BranchRules.iceDamage(fish(p)));AttackTypes.mark(AttackTypes.ICE);CombatSettings.hurt(p,t,source,damage);
    }
    private static void freeze(ServerPlayer owner,LivingEntity t){
        if(SixfoldFinale.active(t)||SixfoldFinale.controlled(t)||NetBranches.locked(t)||!CombatSettings.canHit(owner,t)||DomainClash.active(t))return;
        Frozen old=frozen.get(t.getUUID());if(old!=null&&validFreeze(old))return;if(frozen.size()>=2048)return;
        Frozen f=new Frozen(owner,owner.serverLevel(),t,t.position(),owner.level().getGameTime()+200,--nextFreeze);frozen.put(t.getUUID(),f);
        t.stopRiding();t.stopUsingItem();ServerPlayer caster=t instanceof ServerPlayer p?p:MaidBridge.caster(t);if(caster!=null)Skills.cancelCharge(caster);hold(f);syncHold(f,200);
    }
    private static void syncHold(Frozen f,int life){Network.send(f.level,new Network.Effect(NetBranchRules.FX_HOLD,f.target.getId(),f.anchor,Vec3.ZERO,1,life,f.id));Network.send(f.level,new Network.Effect(BranchRules.FX_FREEZE,f.target.getId(),f.anchor,Vec3.ZERO,Math.max(f.target.getBbHeight(),1),life,f.id));}
    private static void hold(Frozen f){LivingEntity t=f.target;Vec3 a=f.anchor;t.stopRiding();t.stopUsingItem();t.setDeltaMovement(Vec3.ZERO);t.fallDistance=0;t.hurtMarked=true;if(t instanceof Mob m)m.getNavigation().stop();if(t instanceof ServerPlayer p)p.connection.teleport(a.x,a.y,a.z,p.getYRot(),p.getXRot());else t.teleportTo(a.x,a.y,a.z);}
    private static void release(Frozen f){syncHold(f,0);f.target.setDeltaMovement(Vec3.ZERO);f.target.fallDistance=0;}
    private static boolean visible(ServerPlayer p,Vec3 from,Vec3 to){var hit=p.serverLevel().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return hit.getType()==HitResult.Type.MISS||hit.getLocation().distanceToSqr(from)+0.2>=to.distanceToSqr(from);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(Frozen f:List.copyOf(frozen.values())){if(!validFreeze(f)){frozen.remove(f.target.getUUID(),f);release(f);}else{hold(f);if(f.level.getGameTime()%20==0)syncHold(f,(int)(f.end-f.level.getGameTime()));}}
        for(Breath b:List.copyOf(breaths)){
            ServerPlayer p=b.owner;long now=b.level.getGameTime();if(!valid(p,b.level)||now>=b.end||!Curse.canUseTechnique(p)){breaths.remove(b);continue;}
            Vec3 from=p.getEyePosition(),dir=p.getLookAngle().normalize(),side=NetBranches.side(dir),up=dir.cross(side).normalize();double length=length(p),halfWidth=width(p)/2,halfHeight=height(p)/2;
            if(now%2==0)Network.send(b.level,new Network.Effect(BranchRules.FX_FROST,p.getId(),from,dir,(float)length,6,fish(p)));
            if(now<b.nextDamage)continue;b.nextDamage=now+10;
            for(LivingEntity t:Skills.targets(b.level,p,from,length+width(p))){Vec3 d=t.getBoundingBox().getCenter().subtract(from);double forward=d.dot(dir),r=t.getBbWidth()/2;if(forward>=-r&&forward<=length+r&&Math.abs(d.dot(side))<=halfWidth+r&&Math.abs(d.dot(up))<=halfHeight+t.getBbHeight()/2&&visible(p,from,t.getBoundingBox().getCenter())){hurt(p,t,2);if(t.isAlive()&&b.frozen.add(t.getUUID()))freeze(p,t);}}
            for(double x=-halfWidth;x<=halfWidth;x+=1)for(double y=-halfHeight;y<=halfHeight;y+=1){Vec3 a=from.add(side.scale(x)).add(up.scale(y));SkillDestruction.line(p,a,a.add(dir.scale(length)),0.5);}
        }
        for(Pillar q:List.copyOf(pillars)){
            if(!valid(q.owner,q.level)||q.age>=50){pillars.remove(q);continue;}if(q.level.getGameTime()<q.born+q.delay)continue;
            if(q.age%5==0)Network.send(q.level,new Network.Effect(BranchRules.FX_ICE,q.owner.getId(),q.position,q.velocity,1,6,0));
            Vec3 next=q.position.add(q.velocity);var block=q.level.clip(new ClipContext(q.position,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,q.owner));
            if(block.getType()!=HitResult.Type.MISS)next=block.getLocation();LivingEntity victim=null;double nearest=Double.MAX_VALUE;
            for(LivingEntity t:q.level.getEntitiesOfClass(LivingEntity.class,new AABB(q.position,next).inflate(0.7),t->CombatSettings.canHit(q.owner,t)&&!DomainClash.active(t))){var box=t.getBoundingBox().inflate(0.45);Vec3 hit=box.contains(q.position)?q.position:box.clip(q.position,next).orElse(null);if(hit!=null&&hit.distanceToSqr(q.position)<nearest){nearest=hit.distanceToSqr(q.position);victim=t;}}
            SkillDestruction.line(q.owner,q.position,next,0.6);
            if(victim!=null){hurt(q.owner,victim,4);pillars.remove(q);}else if(block.getType()!=HitResult.Type.MISS)pillars.remove(q);else {q.position=next;q.age++;}
        }
    }
    public static void protect(ServerPlayer owner,UUID target){Frozen f=frozen.get(target);if(f!=null&&f.owner==owner){frozen.remove(target,f);release(f);}}
    public static void cancel(ServerPlayer p){breaths.removeIf(b->b.owner==p);pillars.removeIf(q->q.owner==p);for(Frozen f:List.copyOf(frozen.values()))if(f.owner==p||f.target.getUUID().equals(p.getUUID())){frozen.remove(f.target.getUUID(),f);release(f);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void frozenTick(LivingEvent.LivingTickEvent e){if(e.getEntity().level().isClientSide||!locked(e.getEntity()))return;hold(frozen.get(e.getEntity().getUUID()));if(!(e.getEntity() instanceof Player))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent e){if(locked(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent e){if(e.isCancelable()&&locked(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void outgoing(LivingAttackEvent e){if(e.getSource().getEntity() instanceof LivingEntity t&&locked(t))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breakSpeed(PlayerEvent.BreakSpeed e){if(locked(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void teleport(net.minecraftforge.event.entity.EntityTeleportEvent e){if(e.isCancelable()&&e.getEntity() instanceof LivingEntity t&&locked(t))e.setCanceled(true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){breaths.clear();pillars.clear();frozen.clear();nextFreeze=0;}
    private Uraume(){}
}
