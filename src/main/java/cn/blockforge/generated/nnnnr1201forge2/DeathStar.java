package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;

/** 极之番服务端：单次扣费、40刻蓄力、扫掠碰撞、每次命中重新核验敌友。 */
public final class DeathStar {
    public static final int FX_CHARGE=440,FX_FLIGHT=441,FX_BURST=442,FX_STOP=443,FX_ANEMIA=444;
    public static final ResourceKey<DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("sukuna","death_star"));
    private static final TagKey<EntityType<?>> BOSSES=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("sukuna","death_star_bosses"));
    private static final TagKey<EntityType<?>> FORGE_BOSSES=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("forge","bosses"));
    private static final UUID SLOW=UUID.fromString("ae134000-0001-4000-8000-000000000001");
    private static final String UNTIL="DeathStarAnemiaUntil",CEILING="DeathStarHealthCeiling";
    private static final Map<UUID,Cast> casts=new HashMap<>();
    private static final List<Star> stars=new ArrayList<>();
    private static final Map<LivingEntity,Float> trueHits=new IdentityHashMap<>();
    private static long serial;
    private static class Cast {
        final ServerPlayer p;final ServerLevel level;final Vec3 origin;final long id;int age;
        Cast(ServerPlayer p){this.p=p;level=p.serverLevel();origin=p.position();id=++serial;}
    }
    private static class Star {
        final ServerPlayer p;final ServerLevel level;final Vec3 direction;final long id;final Set<UUID> hit=new HashSet<>();Vec3 at;int age;
        Star(Cast c){p=c.p;level=c.level;direction=p.getLookAngle().normalize();at=hand(p);id=c.id;}
    }
    private static long now(ServerPlayer p){return p.server.overworld().getGameTime();}
    public static boolean anemic(ServerPlayer p){return Curse.data(p).getLong(UNTIL)>now(p);}
    public static boolean charging(ServerPlayer p){return casts.containsKey(p.getUUID());}
    public static boolean source(DamageSource source){return source.is(DAMAGE);}
    private static Vec3 hand(ServerPlayer p){return p.getEyePosition().add(p.getLookAngle().scale(1.15)).add(0,-.22,0);}
    private static boolean valid(ServerPlayer p,ServerLevel level){return p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&(p instanceof MaidBridge.Actor||!p.hasDisconnected())&&p.level()==level&&MaidBridge.body(p).level()==level;}
    private static void tell(ServerPlayer p,String message){p.displayClientMessage(Component.literal(message),true);}
    private static void fx(ServerPlayer p,int kind,Vec3 at,Vec3 direction,float size,int life,long id){BloodArts.fx(p,kind,at,direction,size,life,id);}
    private static void pose(ServerPlayer p,int ticks){fx(p,70,p.position(),p.getLookAngle(),DeathStarRules.SKILL,ticks,0);}
    public static void start(ServerPlayer p){
        if(charging(p)||anemic(p)){tell(p,"严重贫血期间无法使用赤血操术");return;}
        if(!BloodArts.unlocked(p)||!BloodDomain.free(p)){tell(p,"死胎星只能在自己展开的胎海领域内释放");return;}
        if(!TechniqueInput.allowed(p)||Skills.sealing(p)||DomainClash.active(p)||SixfoldFinale.active(p)||DismantleMotion.active(p)||FurnaceMax.active(p)||CombatExpansion.chanting(p)){tell(p,"当前动作未结束，无法凝聚死胎星");return;}
        if(casts.size()+stars.size()>=32){tell(p,"当前血星过多，请稍后再试");return;}
        BloodArts.sacrifice(p);TechniqueInput.cancel(p);Skills.cancelSustained(p);
        p.setHealth(DeathStarRules.remainingHealth(p.getHealth()));MaidBridge.body(p).setAbsorptionAmount(0);
        Curse.add(p,-Curse.energy(p));Curse.data(p).putLong(UNTIL,now(p)+DeathStarRules.ANEMIA_TICKS);Curse.data(p).putFloat(CEILING,p.getHealth());
        Cast cast=new Cast(p);casts.put(p.getUUID(),cast);slow(p,true);syncAnemia(p);Curse.sync(p);pose(p,DeathStarRules.CHARGE_TICKS);
        fx(p,FX_CHARGE,hand(p),p.getLookAngle(),0,DeathStarRules.CHARGE_TICKS,cast.id);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.WARDEN_HEARTBEAT,SoundSource.PLAYERS,2,.5f);
        tell(p,"极之番·死胎星：蓄力2秒，生命与咒力已献祭，中断不返还");
    }
    private static void slow(ServerPlayer p,boolean on){var a=MaidBridge.body(p).getAttribute(Attributes.MOVEMENT_SPEED);if(a==null)return;if(!on)a.removeModifier(SLOW);else if(a.getModifier(SLOW)==null)a.addTransientModifier(new AttributeModifier(SLOW,"严重贫血",-.5,AttributeModifier.Operation.MULTIPLY_TOTAL));}
    private static void syncAnemia(ServerPlayer p){if(p instanceof MaidBridge.Actor)return;int ticks=(int)Math.max(0,Curse.data(p).getLong(UNTIL)-now(p));Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new Network.Effect(FX_ANEMIA,p.getId(),p.position(),Vec3.ZERO,0,ticks,0));}
    public static void cancel(ServerPlayer p){Cast c=casts.remove(p.getUUID());if(c!=null){pose(p,0);fx(p,FX_STOP,hand(p),Vec3.ZERO,0,0,c.id);}}
    private static boolean boss(LivingEntity t){
        // 玩家不是可秒杀的杂兵；第三方首领可通过标签补充，不以最大生命值猜测身份。
        return t instanceof Player||t instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon||t instanceof net.minecraft.world.entity.boss.wither.WitherBoss
            ||BossTargets.managed(t)&&(!(t instanceof ParasiteEntity parasite)||parasite.mother())||t instanceof MegumiEntity||t.getType().is(BOSSES)||t.getType().is(FORGE_BOSSES);
    }
    private static List<LivingEntity> near(ServerPlayer p,Vec3 at,double radius){return p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius),t->CombatSafety.canTarget(p,t)&&t.getBoundingBox().getCenter().distanceToSqr(at)<=radius*radius);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(Cast c:List.copyOf(casts.values())){
            ServerPlayer p=c.p;
            if(!valid(p,c.level)||!BloodDomain.free(p)||PrisonSystem.locked(MaidBridge.body(p))||!Curse.canUseTechnique(p)||SoulCombat.suppressed(p)||DomainClash.active(p)||TenShadows.locked(p)||!TechniqueInput.allowed(p)){cancel(p);continue;}
            c.age++;LivingEntity body=MaidBridge.body(p);body.setDeltaMovement(0,Math.max(-.18,Math.min(.16,c.origin.y+1.35-body.getY())),0);body.fallDistance=0;body.hurtMarked=true;
            Vec3 ball=hand(p);
            for(LivingEntity t:near(p,p.position(),DeathStarRules.PULL_RADIUS)){
                Vec3 delta=ball.subtract(t.getBoundingBox().getCenter());if(delta.lengthSqr()<2)continue;
                if(!p.hasLineOfSight(t))continue;
                Vec3 velocity=t.getDeltaMovement().scale(.3).add(delta.normalize().scale(Math.min(.7,.12+delta.length()*.025)));
                if(CombatSafety.canAffect(p,t)){t.setDeltaMovement(velocity);t.hurtMarked=true;}
                if(c.age%8==0)fx(p,BloodArts.FX_DRAIN,t.getBoundingBox().getCenter(),ball,1,9,0);
            }
            if(c.age%4==0)fx(p,FX_CHARGE,ball,p.getLookAngle(),c.age/(float)DeathStarRules.CHARGE_TICKS,8,c.id);
            if(c.age>=DeathStarRules.CHARGE_TICKS){
                casts.remove(p.getUUID());fx(p,FX_STOP,ball,Vec3.ZERO,0,0,c.id);pose(p,10);
                Curse.add(p,-Curse.energy(p));Curse.data(p).putLong(UNTIL,now(p)+DeathStarRules.ANEMIA_TICKS);syncAnemia(p);Curse.sync(p);
                Star s=new Star(c);stars.add(s);fx(p,FX_FLIGHT,s.at,s.direction,1,6,s.id);
                c.level.playSound(null,p.blockPosition(),SoundEvents.WARDEN_SONIC_BOOM,SoundSource.PLAYERS,3,.55f);
            }
        }
        for(Star s:List.copyOf(stars)){
            if(!stars.contains(s))continue;if(!valid(s.p,s.level)){fx(s.p,FX_STOP,s.at,Vec3.ZERO,0,0,s.id);stars.remove(s);continue;}
            Vec3 next=s.at.add(s.direction.scale(DeathStarRules.SPEED));
            if(!s.level.hasChunkAt(BlockPos.containing(next))||!s.level.getWorldBorder().isWithinBounds(BlockPos.containing(next))){explode(s,s.at);stars.remove(s);continue;}
            Vec3 wall=wallContact(s,next);boolean blocked=wall!=null;
            if(blocked)next=wall;Vec3 end=next;
            var victims=s.level.getEntitiesOfClass(LivingEntity.class,new AABB(s.at,end).inflate(DeathStarRules.BALL_RADIUS),t->CombatSafety.canTarget(s.p,t)&&!s.hit.contains(t.getUUID())&&(t.getBoundingBox().inflate(DeathStarRules.BALL_RADIUS).contains(s.at)||t.getBoundingBox().inflate(DeathStarRules.BALL_RADIUS).clip(s.at,end).isPresent()));
            victims.sort(Comparator.comparingDouble(t->t.distanceToSqr(s.at)));boolean detonated=false;
            for(LivingEntity t:victims){
                if(boss(t)){Vec3 contact=t.getBoundingBox().inflate(DeathStarRules.BALL_RADIUS).clip(s.at,end).orElse(s.at);explode(s,contact);detonated=true;break;}
                s.hit.add(t.getUUID());hit(s.p,t,Math.max(1000000,t.getMaxHealth()+t.getHealth()+1));
            }
            if(detonated){stars.remove(s);continue;}s.at=next;
            if(blocked||++s.age>=DeathStarRules.FLIGHT_TICKS){explode(s,s.at);stars.remove(s);continue;}
            if(s.age%2==0)fx(s.p,FX_FLIGHT,s.at,s.direction,1,6,s.id);
        }
    }
    /** 用血星体积扫过方块碰撞盒，边缘擦墙也爆炸；不是只检查球心细线。 */
    private static Vec3 wallContact(Star s,Vec3 next){
        double radius=DeathStarRules.BALL_RADIUS,best=Double.MAX_VALUE;Vec3 found=null;
        AABB sweep=new AABB(s.at,next).inflate(radius);
        for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(sweep.minX,sweep.minY,sweep.minZ),BlockPos.containing(sweep.maxX,sweep.maxY,sweep.maxZ))){
            if(!s.level.hasChunkAt(pos))continue;
            for(AABB local:s.level.getBlockState(pos).getCollisionShape(s.level,pos).toAabbs()){
                AABB box=local.move(pos).inflate(radius);Vec3 contact=box.contains(s.at)?s.at:box.clip(s.at,next).orElse(null);
                if(contact!=null&&s.at.distanceToSqr(contact)<best){best=s.at.distanceToSqr(contact);found=contact;}
            }
        }
        return found;
    }
    private static void hit(ServerPlayer p,LivingEntity t,float amount){
        if(!CombatSafety.canAffect(p,t)||DomainClash.active(t))return;
        float absorption=t.getAbsorptionAmount();t.setAbsorptionAmount(0);t.invulnerableTime=0;
        Float previous=trueHits.put(t,amount);
        try{AttackTypes.mark(AttackTypes.BLOOD);CombatSafety.hurt(p,t,new DamageSource(p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE),p),amount);}
        finally{t.setAbsorptionAmount(absorption);if(previous==null)trueHits.remove(t);else trueHits.put(t,previous);}
    }
    private static void explode(Star s,Vec3 at){
        fx(s.p,FX_STOP,at,Vec3.ZERO,0,0,s.id);fx(s.p,FX_BURST,at,s.direction,50,32,s.id);
        s.level.playSound(null,BlockPos.containing(at),SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,6,.55f);
        for(LivingEntity t:near(s.p,at,DeathStarRules.RADIUS))hit(s.p,t,DeathStarRules.damage(t.getMaxHealth()));
        // 成长增加爆心向外的索敌血线，不重复命中已在爆心内结算的目标。
        int rank=BloodArts.rank(s.p);if(rank>0){var outside=near(s.p,at,DeathStarRules.RADIUS+rank*3);outside.removeIf(t->t.distanceToSqr(at)<=DeathStarRules.RADIUS*DeathStarRules.RADIUS);outside.sort(Comparator.comparingDouble(t->t.distanceToSqr(at)));for(LivingEntity t:outside.stream().limit(rank*4).toList()){Vec3 end=t.getBoundingBox().getCenter();if(s.level.clip(new ClipContext(at,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,MaidBridge.body(s.p))).getType()!=HitResult.Type.MISS)continue;fx(s.p,BloodExpansion.FX_CELL,at,end,1,20,s.id);hit(s.p,t,DeathStarRules.damage(t.getMaxHealth())*.1f);}}
        SkillDestruction.sphere(s.p,at,DeathStarRules.RADIUS);
    }
    /** 保留外部取消伤害事件的权利；不直接 setHealth 杀人，不绕过敌友/领地保护。 */
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void trueDamage(LivingHurtEvent event){Float amount=trueHits.get(event.getEntity());if(source(event.getSource())&&amount!=null)event.setAmount(amount);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void outgoing(LivingDamageEvent event){ServerPlayer p=CombatSettings.caster(event.getSource());if(p!=null&&anemic(p)&&!source(event.getSource()))event.setAmount(event.getAmount()*.5f);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void heal(LivingHealEvent event){ServerPlayer p=event.getEntity() instanceof ServerPlayer player?player:MaidBridge.caster(event.getEntity());if(p!=null&&anemic(p))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void playerTick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p))return;
        tickAnemia(p);
    }
    public static void tickAnemia(ServerPlayer p){
        boolean active=anemic(p);slow(p,active);
        if(active&&p.isAlive()){
            float ceiling=Curse.data(p).contains(CEILING)?Curse.data(p).getFloat(CEILING):p.getHealth();
            if(p.getHealth()>ceiling)p.setHealth(Math.max(.001f,ceiling));else Curse.data(p).putFloat(CEILING,p.getHealth());
            if(p.tickCount%20==0)syncAnemia(p);
        }else if(!active&&Curse.data(p).contains(UNTIL)){Curse.data(p).remove(UNTIL);Curse.data(p).remove(CEILING);syncAnemia(p);}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){cancel(p);stars.removeIf(s->s.p==p);slow(p,false);}}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p){cancel(p);stars.removeIf(s->s.p==p);syncAnemia(p);}}
    @SubscribeEvent public static void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer p){cancel(p);stars.removeIf(s->s.p==p);Curse.data(p).remove(CEILING);}}
    /** 仅创作测试恢复物品调用，正常死亡或重连不能清除严重贫血。 */
    static void testReset(ServerPlayer p){cancel(p);stars.removeIf(s->s.p==p);Curse.data(p).remove(UNTIL);Curse.data(p).remove(CEILING);slow(p,false);syncAnemia(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){casts.clear();stars.clear();trueHits.clear();}
    private DeathStar(){}
}
