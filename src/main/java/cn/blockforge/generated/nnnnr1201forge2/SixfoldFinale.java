package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;

/** 终结技唯一权威时间轴；客户端只展示，不接受目标、伤害或瞬移位置。 */
public final class SixfoldFinale {
    private static final String SCAR="SukunaSixfoldScar";
    private static final Map<UUID,Cast> casts=new HashMap<>();
    private static final Map<UUID,Prison> prisoners=new HashMap<>();
    private static final class Cast {
        final ServerPlayer p;final ServerLevel level;final LivingEntity body;final long start,token;final float yaw;final boolean gravity;
        Vec3 center,anchor,descentFrom,landing;long heartbeat;int age,combo,variant;boolean committed;Vec3 dragon;int dragonTarget;
        final List<LivingEntity> targets=new ArrayList<>(),dragonRoute=new ArrayList<>();
        final Map<UUID,BitSet> dragonHits=new HashMap<>(),wallHits=new HashMap<>();final Set<UUID> worldHits=new HashSet<>();
        final List<Meteor> meteors=new ArrayList<>();Vec3 wallOrigin;

        Cast(ServerPlayer p){this.p=p;level=p.serverLevel();body=MaidBridge.body(p);start=heartbeat=level.getGameTime();token=p.getRandom().nextLong();yaw=p.getYRot();center=body.position();anchor=center;gravity=body.isNoGravity();}
    }
    private static final class Meteor {Vec3 pos;final UUID target;final int deadline;Meteor(Vec3 pos,UUID target,int deadline){this.pos=pos;this.target=target;this.deadline=deadline;}}
    private static final class Prison {
        final Cast cast;final LivingEntity target;final Vec3 start,anchor;final long born;
        Prison(Cast s,LivingEntity t,Vec3 anchor){cast=s;target=t;start=t.position();this.anchor=anchor;born=s.level.getGameTime();}
    }
    public static boolean active(LivingEntity e){if(e instanceof MaidBridge.Actor a)e=a.maid;for(Cast s:casts.values())if(s.body==e||s.p==e)return true;return false;}
    public static boolean controlled(LivingEntity e){if(e instanceof MaidBridge.Actor a)e=a.maid;return prisoners.containsKey(e.getUUID());}
    public static boolean ready(ServerPlayer p){return !active(p)&&!controlled(p)&&CombatExpansion.allowed(p)&&!DismantleMotion.active(p)&&!CombatExpansion.chanting(p)&&FinaleRules.ready(Curse.energy(p),Curse.maximum(p),CombatExpansion.lingerCount(p));}
    public static void start(ServerPlayer p){
        if(!ready(p)){CastFeedback.tell(p,"终式需要咒力严格高于80%、至少3道已停住的滞留斩，且术式可用");return;}
        Cast s=new Cast(p);casts.put(p.getUUID(),s);Skills.cancelSustained(p);sync(s,false);Curse.sync(p);
        CastFeedback.tell(p,"終式·六道連斬 · 持续按住8秒，可移动，受击不会打断");
    }
    public static void heartbeat(ServerPlayer p){Cast s=casts.get(p.getUUID());if(s!=null)s.heartbeat=s.level.getGameTime();}
    public static void release(ServerPlayer p){Cast s=casts.get(p.getUUID());if(s!=null&&!s.committed)finish(s,false);}
    private static void fx(Cast s,int kind,Vec3 at,Vec3 direction,float size,int life,long seed){Network.send(s.level,new Network.Effect(kind,s.body.getId(),at,direction,size,life,seed));}
    private static void sync(Cast s,boolean end){fx(s,FinaleRules.FX_STATE,s.center,new Vec3(s.yaw,s.variant,0),end?-1:s.age,end?0:FinaleRules.END-s.age,s.token);}
    private static void chant(Cast s,int stage,int word){
        String text=FinaleRules.WORDS[stage][word];
        int duration=(word+1)*30/FinaleRules.WORDS[stage].length-word*30/FinaleRules.WORDS[stage].length;
        for(ServerPlayer listener:s.p.server.getPlayerList().getPlayers()){
            if(TechniqueConfig.chat())listener.sendSystemMessage(Component.literal("["+s.body.getName().getString()+"] "+text));
            else {
                listener.connection.send(new ClientboundSetTitlesAnimationPacket(1,Math.max(1,duration-2),1));
                listener.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(FinaleRules.NAME+" · 第"+(stage+1)+"阶段")));
                listener.connection.send(new ClientboundSetTitleTextPacket(Component.literal(text)));
            }
        }
    }
    private static void sound(Cast s,SoundEvent sound,float volume,float pitch){s.level.playSound(null,BlockPos.containing(s.center),sound,SoundSource.PLAYERS,volume,pitch);}
    private static boolean valid(Cast s){return CombatExpansion.valid(s.p,s.level)&&s.body.level()==s.level;}
    private static List<LivingEntity> arena(Cast s){return Skills.crosshairOrder(s.p,Skills.targets(s.level,s.p,s.center,FinaleRules.RADIUS)).stream().filter(t->t.position().distanceToSqr(s.center)<=FinaleRules.RADIUS*FinaleRules.RADIUS&&!active(t)).toList();}
    private static Vec3 safe(Cast s,LivingEntity body,Vec3 point){
        for(int dy=0;dy<=6;dy++){
            Vec3 q=point.add(0,dy,0);BlockPos pos=BlockPos.containing(q);
            if(pos.getY()>=s.level.getMaxBuildHeight()-body.getBbHeight()||pos.getY()<s.level.getMinBuildHeight()||!s.level.hasChunkAt(pos)||!s.level.getWorldBorder().isWithinBounds(pos))continue;
            if(s.level.noCollision(body,body.getBoundingBox().move(q.subtract(body.position()))))return q;
        }
        return body.position();
    }
    private static Vec3 ground(Cast s){
        Vec3 top=s.body.position().add(0,2,0),bottom=s.center.add(0,-24,0);
        var hit=s.level.clip(new ClipContext(top,bottom,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.body));
        return safe(s,s.body,hit.getType()==HitResult.Type.MISS?s.center:hit.getLocation().add(0,.05,0));
    }
    private static void teleport(LivingEntity t,Vec3 q,float yaw){
        t.stopRiding();t.setDeltaMovement(Vec3.ZERO);t.fallDistance=0;t.hurtMarked=true;
        if(t instanceof ServerPlayer sp)sp.connection.teleport(q.x,q.y,q.z,yaw,t.getXRot());else{t.teleportTo(q.x,q.y,q.z);t.setYRot(yaw);}
        t.yBodyRot=yaw;t.yHeadRot=yaw;
    }
    private static void place(Cast s,Vec3 q){s.anchor=safe(s,s.body,q);teleport(s.body,s.anchor,s.body.getYRot());if(s.body!=s.p)s.p.setPos(s.body.position());}
    private static void hold(Prison prison){
        LivingEntity t=prison.target;Cast s=prison.cast;
        if(!t.isAlive()||t.level()!=s.level||!CombatSettings.canHit(s.p,t)||!casts.containsValue(s)){release(prison);return;}
        double amount=Math.min(1,(s.level.getGameTime()-prison.born)/6.0);Vec3 q=prison.start.lerp(prison.anchor,amount);
        t.stopUsingItem();if(t instanceof Mob mob)mob.getNavigation().stop();teleport(t,q,t.getYRot());
    }
    private static void release(Prison prison){
        if(!prisoners.remove(prison.target.getUUID(),prison))return;LivingEntity t=prison.target;t.fallDistance=0;t.setDeltaMovement(Vec3.ZERO);
        Network.send(prison.cast.level,new Network.Effect(NetBranchRules.FX_HOLD,t.getId(),t.position(),Vec3.ZERO,1,0,prison.cast.token));
    }
    private static void lock(Cast s,LivingEntity t,boolean push){
        if(prisoners.containsKey(t.getUUID())||active(t))return;
        ServerPlayer victim=t instanceof ServerPlayer sp?sp:MaidBridge.caster(t);
        if(victim!=null){Skills.cancelCombat(victim);GuardianPlayerSkills.clashInterrupt(victim);}
        Vec3 away=t.position().subtract(s.center).multiply(1,0,1).normalize();if(away.lengthSqr()<.01)away=new Vec3(1,0,0);
        Prison prison=new Prison(s,t,push?safe(s,t,t.position().add(away.scale(2.4)).add(0,.35,0)):t.position());prisoners.put(t.getUUID(),prison);
        Network.send(s.level,new Network.Effect(NetBranchRules.FX_HOLD,t.getId(),t.position(),Vec3.ZERO,1,FinaleRules.END-s.age+10,s.token));
    }
    private static void shrine(Cast s){
        s.targets.addAll(arena(s));
        for(LivingEntity t:List.copyOf(s.targets)){
            SlashForms.exactHit(s.p,t,1);if(t.isAlive())lock(s,t,true);
        }
        fx(s,FinaleRules.FX_SHRINE,s.center,Vec3.ZERO,1,24,12);sound(s,SoundEvents.BELL_BLOCK,1,.6f);sound(s,SoundEvents.ANVIL_LAND,.8f,.6f);
    }
    private static void combo(Cast s){
        int due=FinaleRules.comboDue(s.age,s.targets.size());
        while(s.combo<due){
            int index=s.combo++;if(s.targets.isEmpty())break;LivingEntity t=s.targets.get(index%s.targets.size());if(!t.isAlive()||t.level()!=s.level||!CombatSettings.canHit(s.p,t))continue;
            int round=index/s.targets.size();s.variant=s.p.getRandom().nextInt(8);
            Vec3 f=t.getLookAngle().multiply(1,0,1).normalize();if(f.lengthSqr()<.01)f=new Vec3(0,0,1);
            Vec3 q=t.position().add(f.scale(round%2==0?2.2:-2.2)).add(0,5,0);place(s,q);
            Vec3 dir=t.getBoundingBox().getCenter().subtract(s.body.getEyePosition()).normalize();float yaw=(float)Math.toDegrees(Math.atan2(-dir.x,dir.z));s.body.setYRot(yaw);s.body.yBodyRot=yaw;s.body.yHeadRot=yaw;
            fx(s,FinaleRules.FX_COMBO,t.getBoundingBox().getCenter(),s.body.getEyePosition(),s.variant,6,index);
            // 随机招式只决定形状和动作，每段统一为单发解70%，绝不调用带扣费或额外伤害的普通入口。
            SlashForms.exactHit(s.p,t,.7);s.body.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        if(s.age%3==0){sound(s,SoundEvents.PLAYER_ATTACK_SWEEP,.45f,1.4f);sound(s,SoundEvents.PLAYER_ATTACK_STRONG,.3f,.8f);}
    }
    private static void wall(Cast s){
        place(s,s.center.add(0,10,0));s.wallOrigin=s.center.add(0,12,0);fx(s,FinaleRules.FX_WALL,s.wallOrigin,new Vec3(0,-1,0),300,30,0);
    }
    private static void wallTick(Cast s){
        if(s.wallOrigin==null)return;double previous=s.wallOrigin.y-(s.age-FinaleRules.STARTS[2])*2,next=previous-2;
        // 每刻扫过2格，水平300×300、间距1格，共301横+301纵；每条对每个目标仅结算一次。
        for(LivingEntity t:arena(s)){
            AABB box=t.getBoundingBox().inflate(.35);if(box.minY>previous||box.maxY<next)continue;BitSet seen=s.wallHits.computeIfAbsent(t.getUUID(),id->new BitSet(602));
            for(int i=-150;i<=150&&t.isAlive();i++){
                double x=s.center.x+i,z=s.center.z+i;int line=(i+150)*2;
                if(!seen.get(line)&&box.minX<=x&&box.maxX>=x&&box.maxZ>=s.center.z-150&&box.minZ<=s.center.z+150){seen.set(line);SlashForms.exactHit(s.p,t,.5);}
                if(!seen.get(line+1)&&box.minZ<=z&&box.maxZ>=z&&box.maxX>=s.center.x-150&&box.minX<=s.center.x+150){seen.set(line+1);SlashForms.exactHit(s.p,t,.5);}
            }
        }
    }
    private static boolean aboveGround(Cast s,LivingEntity t){
        // 从目标向天空检查实心遮挡，地下或被屋顶完全遮住的目标不进入龙的路线。
        Vec3 from=t.getEyePosition(),to=from.add(0,Math.max(1,s.level.getMaxBuildHeight()-from.y-1),0);
        return s.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,t)).getType()==HitResult.Type.MISS;
    }
    private static void dragonStart(Cast s){
        s.dragonRoute.addAll(arena(s).stream().filter(t->aboveGround(s,t)).sorted(Comparator.comparingDouble(LivingEntity::getMaxHealth).reversed()).toList());
        s.dragon=s.body.position().add(s.body.getLookAngle().scale(-4)).add(0,2,0);
        if(!s.dragonRoute.isEmpty()){
            LivingEntity t=s.dragonRoute.get(0);Vec3 c=t.getBoundingBox().getCenter();
            for(int i=0;i<6;i++){double a=-Math.PI/2+i*Math.PI/3,b=a+2*Math.PI/3;Vec3 from=c.add(Math.cos(a)*4,Math.sin(a)*4,0),to=c.add(Math.cos(b)*4,Math.sin(b)*4,0);fx(s,CombatExpansionRules.FX_SLASH,from,to.subtract(from),1,10,0);SlashForms.exactHit(s.p,t,.25);}
        }
        fx(s,CombatExpansionRules.FX_DRAGON,s.dragon,new Vec3(0,1,0),1,12,s.age);sound(s,SoundEvents.ENDER_DRAGON_GROWL,.7f,.8f);
    }
    private static void dragonTick(Cast s){
        int elapsed=s.age-FinaleRules.TRIGGERS[2];if(s.dragon==null)return;
        int due=Math.min(s.dragonRoute.size(),(elapsed+1)*s.dragonRoute.size()/14);
        while(s.dragonTarget<due){
            LivingEntity t=s.dragonRoute.get(s.dragonTarget++);if(!t.isAlive()||!CombatSettings.canHit(s.p,t))continue;
            Vec3 next=t.getBoundingBox().getCenter(),dir=next.subtract(s.dragon).normalize();if(dir.lengthSqr()<.01)dir=new Vec3(0,0,1);
            // 对路线分段采样，判定与渲染共用63道龙身斩击；每道为普通龙的50%，即解25%。
            int n=Math.max(1,(int)Math.ceil(s.dragon.distanceTo(next)/8));
            for(int i=1;i<=n;i++){Vec3 from=s.dragon.lerp(next,(i-1)/(double)n),to=s.dragon.lerp(next,i/(double)n);CombatExpansion.dragonDamage(s.p,s.level,from,to,dir,s.age,s.dragonHits,.25,s.dragonRoute::contains);}
            s.dragon=next;
        }
        if(elapsed==14){for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 at=s.dragon.add(Math.cos(a)*4,3,Math.sin(a)*4);fx(s,CombatExpansionRules.FX_STAR,at,Vec3.ZERO,1,10,i);CombatExpansion.starDamage(s.p,at,.25);}sound(s,SoundEvents.FIREWORK_ROCKET_BLAST,.65f,.65f);}
        if(elapsed>=15&&elapsed<24){
            List<LivingEntity> targets=arena(s).stream().filter(t->aboveGround(s,t)).toList();
            int from=(elapsed-15)*120/9,to=(elapsed-14)*120/9;
            for(int i=from;i<to;i++){double a=(i/20)*Math.PI/3;Vec3 start=s.dragon.add(Math.cos(a)*4,3,Math.sin(a)*4);s.meteors.add(new Meteor(start,targets.isEmpty()?null:targets.get(i%targets.size()).getUUID(),s.age+7));}
        }
    }
    private static void meteorTick(Cast s){
        for(Meteor m:List.copyOf(s.meteors)){
            LivingEntity target=m.target!=null&&s.level.getEntity(m.target) instanceof LivingEntity t&&CombatSettings.canHit(s.p,t)?t:null;
            Vec3 end=target==null?s.center:target.getBoundingBox().getCenter();Vec3 next=m.pos.lerp(end,1.0/Math.max(1,m.deadline-s.age));Vec3 axis=NetBranches.side(end.subtract(m.pos));
            fx(s,CombatExpansionRules.FX_SLASH,next.subtract(axis.scale(.65)),axis.scale(1.3),1,2,0);
            if(target!=null&&(target.getBoundingBox().inflate(.4).contains(next)||target.getBoundingBox().inflate(.4).clip(m.pos,next).isPresent())){SlashForms.exactHit(s.p,target,.25);s.meteors.remove(m);}
            else if(s.age>=m.deadline)s.meteors.remove(m);m.pos=next;
        }
    }
    private static void world(Cast s){
        place(s,ground(s));s.body.setNoGravity(s.gravity);fx(s,FinaleRules.FX_WORLD,s.body.position(),Vec3.ZERO,FinaleRules.RADIUS,10,0);
        SkillSounds.play(s.p,"space_slash");
    }
    private static void worldTerrain(Cast s){
        int wave=s.age-FinaleRules.TRIGGERS[3];double radius=(wave+1)*15.0;
        for(LivingEntity t:arena(s))if(!s.worldHits.contains(t.getUUID())&&t.position().multiply(1,0,1).distanceToSqr(s.center.multiply(1,0,1))<=radius*radius){
            s.worldHits.add(t.getUUID());t.invulnerableTime=0;float amount=WorldSlashRules.damage((float)(t.getMaxHealth()*Authority.damageScale(s.p)),NetBranches.dismantleDamage(s.p,t));
            AttackTypes.mark(AttackTypes.FINALE);CombatSettings.hurt(s.p,t,s.level.damageSources().sonicBoom(s.p),amount*.8f);
        }
        for(Entity entity:s.level.getEntities(s.body,new AABB(s.center,s.center).inflate(radius),e->e instanceof Projectile))if(entity.position().distanceToSqr(s.center)<=radius*radius)entity.discard();
        if(!SkillDestruction.enabled(s.level))return;
        for(int i=0;i<48;i++){double a=i*Math.PI/24,z=(i+1)*Math.PI/24;Vec3 from=s.center.add(Math.cos(a)*Math.max(0,radius-15),.1,Math.sin(a)*Math.max(0,radius-15)),to=s.center.add(Math.cos(a)*radius,.1,Math.sin(a)*radius);SkillDestruction.line(s.p,from,to,.45);SkillDestruction.line(s.p,to,s.center.add(Math.cos(z)*radius,.1,Math.sin(z)*radius),.45);}
    }
    private static void absorb(Cast s){
        Skills.cancelCombat(s.p);fx(s,FinaleRules.FX_ABSORB,s.body.position(),Vec3.ZERO,FinaleRules.RADIUS,14,s.token);
    }
    private static void scar(Cast s){
        for(LivingEntity t:arena(s)){
            var tag=t.getPersistentData();tag.putBoolean(SCAR,true);tag.putUUID(SCAR+"Owner",s.p.getUUID());
            Network.Effect appearance=new Network.Effect(FinaleRules.FX_SCAR,t.getId(),t.position(),Vec3.ZERO,1,80,0).fromCaster(s.p);
            tag.putBoolean(SCAR+"Desktop",appearance.desktopSlashes());if(appearance.wallpaperOwner()!=null)tag.putUUID(SCAR+"Wallpaper",appearance.wallpaperOwner());else tag.remove(SCAR+"Wallpaper");
            tag.putFloat(SCAR+"Damage",Math.max(1,Math.min(20,NetBranches.dismantleDamage(s.p,t)*.05f)));tag.putInt(SCAR+"Tick",0);scarFx(t);
        }
    }
    private static void scarFx(LivingEntity t){if(t.level() instanceof ServerLevel level){var tag=t.getPersistentData();Network.send(level,new Network.Effect(FinaleRules.FX_SCAR,t.getId(),t.position(),Vec3.ZERO,1,80,t.getUUID().getLeastSignificantBits()).wallpaper(tag.hasUUID(SCAR+"Wallpaper")?tag.getUUID(SCAR+"Wallpaper"):null,tag.getBoolean(SCAR+"Desktop")));}}
    private static void pay(ServerPlayer p){
        ServerPlayer pool=Authority.pool(p);if(pool!=null){Curse.status(pool,"FinaleNoRecovery",FinaleRules.REGEN_LOCK);Curse.status(pool,"RecoveryDelay",FinaleRules.REGEN_LOCK);}
        Curse.add(p,-Curse.energy(p));Curse.status(p,"FinaleNoRecovery",FinaleRules.REGEN_LOCK);Curse.status(p,"FinaleBurnout",FinaleRules.BURNOUT);Curse.status(p,"RecoveryDelay",FinaleRules.REGEN_LOCK);Curse.data(p).remove("FinalePending");Curse.data(p).remove("FinaleSavedGravity");Curse.enterBurnout(p,FinaleRules.BURNOUT);Curse.sync(p);
    }
    private static void finish(Cast s,boolean chargeCost){
        if(!casts.remove(s.p.getUUID(),s))return;
        for(Prison prison:List.copyOf(prisoners.values()))if(prison.cast==s)release(prison);
        if(s.body.isAlive()&&s.body.level()==s.level&&s.committed)place(s,ground(s));s.body.setNoGravity(s.gravity);s.body.fallDistance=0;
        sync(s,true);if(chargeCost)pay(s.p);else Curse.sync(s.p);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(Cast s:List.copyOf(casts.values())){
            if(!valid(s)){finish(s,s.committed);continue;}s.age=(int)(s.level.getGameTime()-s.start);
            if(!s.committed){
                if(s.level.getGameTime()-s.heartbeat>25){finish(s,false);continue;}
                s.center=s.body.position();s.anchor=s.center;
                if(s.age>=FinaleRules.CHARGE){
                    if(!FinaleRules.ready(Curse.energy(s.p),Curse.maximum(s.p),CombatExpansion.lingerCount(s.p))){finish(s,false);CastFeedback.tell(s.p,"蓄力结束时条件不足，终式未发动");continue;}
                    s.committed=true;CombatExpansion.consumeLingers(s.p);Skills.cancelCombat(s.p);Curse.data(s.p).putBoolean("FinalePending",true);Curse.data(s.p).putBoolean("FinaleSavedGravity",s.gravity);s.body.setNoGravity(true);
                }
            }
            if(s.committed){
                s.body.fallDistance=0;if(s.age>=FinaleRules.STARTS[1])teleport(s.body,s.anchor,s.body.getYRot());
                for(int i=0;i<FinaleRules.STARTS.length;i++){int word=FinaleRules.wordAt(i,s.age);if(word>=0)chant(s,i,word);}
                if(s.age>=FinaleRules.STARTS[4]){s.body.setYRot(s.yaw);s.body.yBodyRot=s.body.yBodyRotO=s.yaw;s.body.yHeadRot=s.body.yHeadRotO=s.yaw;s.body.setXRot(0);}
                if(s.age==FinaleRules.TRIGGERS[0])sound(s,SoundEvents.BELL_RESONATE,.8f,.6f);
                if(s.age==FinaleRules.TRIGGERS[0]+12)shrine(s);
                if(s.age>=FinaleRules.TRIGGERS[0]+12&&s.age%10==0)for(LivingEntity t:arena(s)){if(s.age<FinaleRules.TRIGGERS[1]&&!s.targets.contains(t))s.targets.add(t);lock(s,t,false);}
                if(s.age>=FinaleRules.TRIGGERS[1]&&s.age<FinaleRules.STARTS[2])combo(s);
                if(s.age==FinaleRules.STARTS[2])wall(s);
                if(s.age>=FinaleRules.STARTS[2]&&s.age<FinaleRules.TRIGGERS[2])wallTick(s);
                if(s.age==FinaleRules.TRIGGERS[2])dragonStart(s);
                if(s.age>=FinaleRules.TRIGGERS[2]&&s.age<FinaleRules.STARTS[3])dragonTick(s);
                if(!s.meteors.isEmpty())meteorTick(s);
                if(s.age==FinaleRules.TRIGGERS[3]-8){s.descentFrom=s.body.position();s.landing=ground(s);}
                if(s.age>=FinaleRules.TRIGGERS[3]-8&&s.age<FinaleRules.TRIGGERS[3]&&s.descentFrom!=null)place(s,s.descentFrom.lerp(s.landing,(s.age-(FinaleRules.TRIGGERS[3]-8)+1)/8.0));
                if(s.age==FinaleRules.TRIGGERS[3])world(s);
                if(s.age>=FinaleRules.TRIGGERS[3]&&s.age<FinaleRules.STARTS[4])worldTerrain(s);
                if(s.age==FinaleRules.TRIGGERS[4])absorb(s);
                if(s.age==FinaleRules.SHATTER)scar(s);
                if(s.age>=FinaleRules.END){finish(s,true);continue;}
            }
            if(s.age%2==0)sync(s,false);
        }
        for(Prison prison:List.copyOf(prisoners.values()))hold(prison);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void living(LivingEvent.LivingTickEvent e){if(e.getEntity().level().isClientSide)return;Prison prison=prisoners.get(e.getEntity().getUUID());if(prison!=null){hold(prison);if(!(e.getEntity() instanceof Player))e.setCanceled(true);}}
    private static boolean gathering(LivingEntity t){for(Cast s:casts.values())if(s.age>=FinaleRules.TRIGGERS[4]&&s.age<FinaleRules.END&&s.level==t.level()&&t.distanceToSqr(s.center)<=FinaleRules.RADIUS*FinaleRules.RADIUS)return true;return false;}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(LivingAttackEvent e){
        boolean administrative=e.getSource().is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)||e.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD);
        if(!administrative&&(active(e.getEntity())||gathering(e.getEntity())||e.getSource().getEntity() instanceof LivingEntity t&&controlled(t)))e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void knockback(LivingKnockBackEvent e){if(active(e.getEntity())||controlled(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void fall(LivingFallEvent e){if(active(e.getEntity())||controlled(e.getEntity())){e.setDistance(0);e.setDamageMultiplier(0);}}
    @SubscribeEvent public static void interact(PlayerInteractEvent e){if(e.isCancelable()&&(active(e.getEntity())||controlled(e.getEntity())))e.setCanceled(true);}
    @SubscribeEvent public static void melee(AttackEntityEvent e){if(active(e.getEntity())||controlled(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void breakSpeed(PlayerEvent.BreakSpeed e){if(active(e.getEntity())||controlled(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void teleportEvent(EntityTeleportEvent e){if(e.isCancelable()&&e.getEntity() instanceof LivingEntity t&&(controlled(t)||active(t)))e.setCanceled(true);}
    @SubscribeEvent public static void scars(TickEvent.LevelTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.level instanceof ServerLevel level))return;
        for(Entity entity:level.getAllEntities())if(entity instanceof LivingEntity t&&t.isAlive()&&t.getPersistentData().getBoolean(SCAR)){
            var tag=t.getPersistentData();int age=tag.getInt(SCAR+"Tick")+1;tag.putInt(SCAR+"Tick",age%200);
            if(age%40==0)scarFx(t);
            if(age>=200&&!gathering(t)){
                ServerPlayer owner=tag.hasUUID(SCAR+"Owner")?level.getServer().getPlayerList().getPlayer(tag.getUUID(SCAR+"Owner")):null;
                if(!CombatSafety.canAffect(owner,t))continue;
                t.invulnerableTime=0;CombatSafety.hurt(owner,t,level.damageSources().sonicBoom(owner),tag.getFloat(SCAR+"Damage"));
            }
        }
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){
        var old=e.getOriginal().getPersistentData();var tag=e.getEntity().getPersistentData();if(old.getBoolean(SCAR)){tag.putBoolean(SCAR,true);if(old.hasUUID(SCAR+"Owner"))tag.putUUID(SCAR+"Owner",old.getUUID(SCAR+"Owner"));tag.putFloat(SCAR+"Damage",old.getFloat(SCAR+"Damage"));}
    }
    @SubscribeEvent public static void playerState(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&p.tickCount%5==0)Network.syncCombat(p);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p&&Curse.data(p).getBoolean("FinalePending")){MaidBridge.body(p).setNoGravity(Curse.data(p).getBoolean("FinaleSavedGravity"));pay(p);}}
    static void testReset(ServerPlayer p){Cast s=casts.get(p.getUUID());if(s!=null)finish(s,false);Prison prison=prisoners.get(p.getUUID());if(prison!=null)release(prison);}
    private static void exit(ServerPlayer p){Cast s=casts.get(p.getUUID());if(s!=null)finish(s,s.committed);Prison prison=prisoners.get(p.getUUID());if(prison!=null)release(prison);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)exit(p);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent e){if(!e.isCanceled()&&e.getEntity() instanceof ServerPlayer p)exit(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)exit(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){for(Cast s:List.copyOf(casts.values()))finish(s,s.committed);prisoners.clear();}
    private SixfoldFinale(){}
}
