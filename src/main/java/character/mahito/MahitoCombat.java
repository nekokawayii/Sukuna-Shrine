package character.mahito;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** 真人共用战斗规则。延迟命中重查敌友；临时属性均使用独立修饰符，不改基础存档数值。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class MahitoCombat {
    public static final int TOUCH=730,TAIL=731,THREADS=732,ARMS=733,DODGE=734,REFORGE=735,DOMAIN=736,DOMAIN_END=737,PUPPET=738;
    private static final ResourceKey<DamageType> SOUL=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("sukuna","mahito_soul"));
    private static final UUID ARMOR_ZERO=UUID.fromString("a3e7c83d-1883-486a-a617-1bac84e21d00");
    private static final UUID SOUL_LOSS=UUID.fromString("a3e7c83d-1883-486a-a617-1bac84e21d01");
    private static final UUID ROOT=UUID.fromString("a3e7c83d-1883-486a-a617-1bac84e21d02");
    private record Collapse(ServerPlayer caster,LivingEntity victim,Vec3 anchor,long start){}
    private record Cast(ServerPlayer caster,int kind,Vec3 origin,Vec3 direction,long at,ServerLevel level){
        Cast(ServerPlayer caster,int kind,Vec3 origin,Vec3 direction,long at){this(caster,kind,origin,direction,at,caster.serverLevel());}
    }
    private record Realm(ServerPlayer caster,Vec3 center,double radius){}
    private static final List<Collapse> COLLAPSES=new ArrayList<>();
    private static final List<Cast> CASTS=new ArrayList<>();
    private static final Map<UUID,Realm> REALMS=new HashMap<>();
    private static final Map<UUID,Set<UUID>> MARKED=new HashMap<>();
    public static LivingEntity body(ServerPlayer p){return MaidBridge.body(p);}
    private static long now(LivingEntity e){return e.level().getGameTime();}
    private static boolean can(ServerPlayer p,LivingEntity t){return p.isAlive()&&CombatSettings.canHit(p,t)&&!DomainClash.active(p)&&!DomainClash.active(t);}
    public static boolean isMahito(LivingEntity e){return e instanceof SorcererNpcEntity npc&&"mahito".equals(npc.characterId());}
    private static void fx(ServerPlayer p,int kind,Vec3 at,Vec3 direction,float size,int life){
        Network.send(p.serverLevel(),new Network.Effect(kind,body(p).getId(),at,direction,size,life,now(p)));
    }
    private static void pose(ServerPlayer p,int action,int duration){if(body(p) instanceof SorcererNpcEntity npc)npc.mahitoPose(action,duration);body(p).swing(InteractionHand.MAIN_HAND);}
    public static boolean soulSource(DamageSource source){return source.is(SOUL);}
    public static boolean soulDamage(ServerPlayer p,LivingEntity target,float damage){
        if(!can(p,target))return false;
        DamageSource source=new DamageSource(p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOUL),body(p));
        return CombatSettings.hurt(p,target,source,damage);
    }
    public static boolean touch(ServerPlayer p,LivingEntity victim){
        if(!can(p,victim))return false;
        boolean realm=inRealm(p,victim);
        if(!realm&&(body(p).getBoundingBox().inflate(3).distanceToSqr(victim.position())>1||!body(p).hasLineOfSight(victim)))return false;
        var data=body(p).getPersistentData();if(data.getLong("MahitoTouchAfter")>now(p))return false;
        data.putLong("MahitoTouchAfter",now(p)+20);
        pose(p,1,12);fx(p,TOUCH,body(p).getEyePosition(),victim.getBoundingBox().getCenter(),1,12);
        if(realm){
            // 结界没有固定伤害脉冲：只有挥掌命中才扣上限，一次恰好一颗心。
            if(character.mainline.Mainline.simpleProtected(victim))return false;
            if(victim instanceof Player)lowerSoul(p,victim);else soulDamage(p,victim,8);
            return true;
        }
        boolean hit=soulDamage(p,victim,8);
        if(hit){
            if(victim instanceof ServerPlayer player&&SoulCombat.layers(player)>0)SoulCombat.peel(player,false);
            else for(EquipmentSlot slot:EquipmentSlot.values())if(slot.getType()==EquipmentSlot.Type.ARMOR){
                var stack=victim.getItemBySlot(slot);
                if(stack.getItem() instanceof ArmorItem armor&&armor.getDefense()>=2&&stack.isDamageableItem())stack.hurtAndBreak(50,victim,e->e.broadcastBreakEvent(slot));
            }
        }
        return hit;
    }
    public static LivingEntity aimed(ServerPlayer p,double range){
        LivingEntity caster=body(p);Vec3 look=p.getLookAngle();
        if(caster instanceof Mob mob&&mob.getTarget()!=null&&can(p,mob.getTarget())&&caster.distanceToSqr(mob.getTarget())<=range*range&&caster.hasLineOfSight(mob.getTarget()))return mob.getTarget();
        return p.level().getEntitiesOfClass(LivingEntity.class,caster.getBoundingBox().inflate(range),t->can(p,t)&&caster.distanceToSqr(t)<=range*range&&caster.hasLineOfSight(t)
            &&t.getBoundingBox().getCenter().subtract(caster.getEyePosition()).normalize().dot(look)>.65).stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
    }
    public static boolean strike(ServerPlayer p){ return strikeHit(p)!=null; }
    /** 与 strike 同一套索敌，但命中时把被打中的目标返回（无为转变用它接「造成伤害后弹出面板」）。 */
    public static LivingEntity strikeHit(ServerPlayer p){
        Realm realm=REALMS.get(p.getUUID());
        LivingEntity target;
        if(realm!=null){
            target=body(p) instanceof Mob mob?mob.getTarget():null;
            if(target==null||!inRealm(p,target)||!can(p,target))target=p.level().getEntitiesOfClass(LivingEntity.class,new AABB(realm.center,realm.center).inflate(realm.radius),t->can(p,t)&&inRealm(p,t))
                .stream().min(Comparator.comparingDouble(t->1-t.getBoundingBox().getCenter().subtract(body(p).getEyePosition()).normalize().dot(p.getLookAngle()))).orElse(null);
        }else target=aimed(p,5);
        return target!=null&&touch(p,target)?target:null;
    }
    /**
     * 无为转变的「伸手」：正前方 5 格（自闭圆顿裹内 30 格、不挑夹角）伸手够到谁就打谁，直接结算 8 点灵魂伤害，
     * 命中就返回那只生物（调用方拿它弹贮存面板）。刻意绕开 {@link #touch} 的 3 格贴脸与 20 刻防抖——
     * 否则会出现"看着打到了却没反应"的玄学。玩家不入选：贮存面板只处理可捕获生物。
     */
    public static LivingEntity reach(ServerPlayer p){
        LivingEntity caster=body(p);boolean realm=character.domainking.SelfEmbryoDomain.active(p);
        double range=realm?30:5;Vec3 look=p.getLookAngle();
        LivingEntity target=p.level().getEntitiesOfClass(LivingEntity.class,caster.getBoundingBox().inflate(range),
                t->t!=p&&t.isAlive()&&!(t instanceof Player)&&can(p,t)&&caster.distanceToSqr(t)<=range*range
                    &&(realm||caster.hasLineOfSight(t))
                    &&(realm||t.getBoundingBox().getCenter().subtract(caster.getEyePosition()).normalize().dot(look)>.55))
            .stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
        if(target==null)return null;
        pose(p,1,12);fx(p,TOUCH,caster.getEyePosition(),target.getBoundingBox().getCenter(),1,12);
        if(!soulDamage(p,target,8))return null;
        for(EquipmentSlot slot:EquipmentSlot.values())if(slot.getType()==EquipmentSlot.Type.ARMOR){
            var stack=target.getItemBySlot(slot);
            if(stack.getItem() instanceof ArmorItem armor&&armor.getDefense()>=2&&stack.isDamageableItem())
                stack.hurtAndBreak(50,target,e->e.broadcastBreakEvent(slot));
        }
        return target;
    }
    public static boolean ready(ServerPlayer p,LivingEntity target,int id){
        if(id==MahitoSkills.REFORGE)return body(p).getHealth()<body(p).getMaxHealth()*.8f||target.getActiveEffects().stream().anyMatch(e->e.getEffect().isBeneficial());
        if(id==MahitoSkills.DODGE)return body(p).hurtTime>0||body(p).distanceToSqr(target)<16;
        if(id==MahitoSkills.COLLAPSE)return body(p).distanceToSqr(target)<=100&&body(p).hasLineOfSight(target);
        if(id==MahitoSkills.TAIL)return body(p).distanceToSqr(target)<=100;
        if(id==MahitoSkills.TRANSMUTE)return inRealm(p,target)||body(p).distanceToSqr(target)<=25;
        return true;
    }
    public static void cast(ServerPlayer p,int id){
        LivingEntity caster=body(p);Vec3 direction=p.getLookAngle().multiply(1,0,1).normalize();
        if(id==MahitoSkills.TAIL){pose(p,2,24);fx(p,TAIL,caster.position(),direction,1,24);CASTS.add(new Cast(p,TAIL,caster.position(),direction,now(p)+12));}
        else if(id==MahitoSkills.COLLAPSE){
            LivingEntity victim=aimed(p,10);if(victim==null)return;
            pose(p,3,40);COLLAPSES.removeIf(c->c.victim==victim);
            COLLAPSES.add(new Collapse(p,victim,victim.position(),now(p)));root(victim,40);
            var armor=victim.getAttribute(Attributes.ARMOR);if(armor!=null){armor.removeModifier(ARMOR_ZERO);armor.addTransientModifier(new AttributeModifier(ARMOR_ZERO,"肉体崩坏",-1,AttributeModifier.Operation.MULTIPLY_TOTAL));}
            victim.getPersistentData().putLong("MahitoArmorUntil",now(p)+40);
            fx(p,THREADS,caster.getEyePosition(),victim.getBoundingBox().getCenter(),victim.getId(),40);
        }else if(id==MahitoSkills.ARMS){
            pose(p,4,22);int count=5+p.getRandom().nextInt(4);
            for(int i=0;i<count;i++){
                Vec3 at=caster.position().add(direction.scale(2+i*1.35));BlockPos foot=BlockPos.containing(at);
                for(int dy=3;dy>=-4;dy--){BlockPos q=foot.offset(0,dy,0);if(!p.level().getBlockState(q.below()).getCollisionShape(p.level(),q.below()).isEmpty()&&p.level().getBlockState(q).getCollisionShape(p.level(),q).isEmpty()){at=Vec3.atBottomCenterOf(q);break;}}
                fx(p,ARMS,at,direction,1,32+i*2);CASTS.add(new Cast(p,ARMS,at,direction,now(p)+8+i*2));
            }
        }else if(id==MahitoSkills.DODGE)dodge(p,direction);
        else if(id==MahitoSkills.REFORGE)reforge(p);
    }
    private static void dodge(ServerPlayer p,Vec3 forward){
        LivingEntity caster=body(p);Vec3 origin=caster.position();
        for(Vec3 dir:List.of(new Vec3(-forward.z,0,forward.x),new Vec3(forward.z,0,-forward.x),forward.scale(-1))){
            Vec3 best=null;
            for(int step=1;step<=16;step++){
                Vec3 at=origin.add(dir.scale(step*.5));AABB box=caster.getBoundingBox().move(at.subtract(origin));
                if(!p.serverLevel().hasChunkAt(BlockPos.containing(at))||!p.level().noCollision(caster,box))break;
                if(character.domain.DomainSafety.safeForMovement(p.serverLevel(),caster,at))best=at;
            }
            if(best==null||best.distanceToSqr(origin)<4)continue;
            caster.getPersistentData().putLong("MahitoDodgeUntil",now(p)+10);pose(p,5,10);
            fx(p,DODGE,origin,best,1,16);caster.teleportTo(best.x,best.y,best.z);caster.fallDistance=0;caster.setDeltaMovement(Vec3.ZERO);
            for(var listener:p.serverLevel().players())if(listener.distanceToSqr(caster)<24*24)listener.displayClientMessage(Component.literal("真人：打偏了哦～"),true);
            p.serverLevel().playSound(null,BlockPos.containing(origin),SoundEvents.SLIME_SQUISH,SoundSource.HOSTILE,1,.6f);return;
        }
    }
    private static void reforge(ServerPlayer p){
        LivingEntity caster=body(p);pose(p,6,20);LivingEntity friend=caster;
        for(MahitoPuppetEntity puppet:p.level().getEntitiesOfClass(MahitoPuppetEntity.class,caster.getBoundingBox().inflate(8),e->p.getUUID().equals(e.ownerId())))
            if(puppet.getHealth()/puppet.getMaxHealth()<friend.getHealth()/friend.getMaxHealth())friend=puppet;
        friend.heal(friend.getMaxHealth()*.2f);fx(p,REFORGE,friend.position(),Vec3.ZERO,1,20);
        LivingEntity enemy=aimed(p,5);if(!(enemy instanceof Player))return;
        MobEffectInstance buff=enemy.getActiveEffects().stream().filter(e->e.getEffect().isBeneficial()).findFirst().orElse(null);if(buff==null)return;
        MobEffect negative=buff.getEffect()==MobEffects.DAMAGE_BOOST?MobEffects.WEAKNESS:buff.getEffect()==MobEffects.MOVEMENT_SPEED?MobEffects.MOVEMENT_SLOWDOWN:buff.getEffect()==MobEffects.DIG_SPEED?MobEffects.DIG_SLOWDOWN:buff.getEffect()==MobEffects.REGENERATION?MobEffects.POISON:MobEffects.WITHER;
        enemy.removeEffect(buff.getEffect());enemy.addEffect(new MobEffectInstance(negative,Math.max(40,Math.min(200,buff.getDuration())),buff.getAmplifier()));
    }
    public static void openRealm(ServerPlayer p,Vec3 center,double radius){
        REALMS.put(p.getUUID(),new Realm(p,center,radius));pose(p,7,40);fx(p,DOMAIN,center,Vec3.ZERO,(float)radius,600);
    }
    public static void closeRealm(UUID owner){
        Realm r=REALMS.remove(owner);if(r!=null)fx(r.caster,DOMAIN_END,r.center,Vec3.ZERO,0,1);
        Set<UUID> victims=MARKED.remove(owner);if(r==null||victims==null)return;
        for(UUID id:victims){var entity=r.caster.serverLevel().getEntity(id);if(entity instanceof LivingEntity living&&living.getPersistentData().hasUUID("MahitoSoulOwner")&&owner.equals(living.getPersistentData().getUUID("MahitoSoulOwner")))restoreSoul(living);}
    }
    public static boolean inRealm(ServerPlayer p,LivingEntity victim){Realm r=REALMS.get(p.getUUID());return r!=null&&victim.level()==p.level()&&victim.position().distanceToSqr(r.center)<(r.radius-1)*(r.radius-1)&&body(p).position().distanceToSqr(r.center)<r.radius*r.radius;}
    private static void lowerSoul(ServerPlayer p,LivingEntity victim){
        if(!(victim instanceof Player))return;
        var attribute=victim.getAttribute(Attributes.MAX_HEALTH);if(attribute==null)return;
        var data=victim.getPersistentData();
        // 多领域交叠时仍只持有一份标记，离开其所属领域即恢复。
        if(data.hasUUID("MahitoSoulOwner")&&!data.getUUID("MahitoSoulOwner").equals(p.getUUID()))restoreSoul(victim);
        double before=attribute.getValue();attribute.removeModifier(SOUL_LOSS);
        // ADDITION 还会受其它生命倍率修饰：测出倍率，保证最终上限每次恰好少2点。
        double original=attribute.getValue();
        attribute.addTransientModifier(new AttributeModifier(SOUL_LOSS,"无为转变·测算",-1,AttributeModifier.Operation.ADDITION));
        double scale=Math.max(.0001,original-attribute.getValue());attribute.removeModifier(SOUL_LOSS);
        double lost=Math.max(0,(original-Math.max(2,before-2))/scale);
        attribute.addTransientModifier(new AttributeModifier(SOUL_LOSS,"无为转变·灵魂削减",-lost,AttributeModifier.Operation.ADDITION));
        victim.setHealth(Math.min(victim.getHealth(),victim.getMaxHealth()));data.putUUID("MahitoSoulOwner",p.getUUID());
        MARKED.computeIfAbsent(p.getUUID(),k->new HashSet<>()).add(victim.getUUID());
        if(victim instanceof ServerPlayer player)player.displayClientMessage(Component.literal("灵魂被改造：最大生命减少一颗心，离开领域或击败真人后恢复"),true);
    }
    private static void restoreSoul(LivingEntity entity){var attribute=entity.getAttribute(Attributes.MAX_HEALTH);if(attribute!=null)attribute.removeModifier(SOUL_LOSS);entity.getPersistentData().remove("MahitoSoulOwner");}
    public static void root(LivingEntity victim,int ticks){
        victim.getPersistentData().putLong("MahitoRootUntil",Math.max(now(victim)+ticks,victim.getPersistentData().getLong("MahitoRootUntil")));
        var speed=victim.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null&&!speed.hasModifier(new AttributeModifier(ROOT,"灵魂定身",-1,AttributeModifier.Operation.MULTIPLY_TOTAL)))speed.addTransientModifier(new AttributeModifier(ROOT,"灵魂定身",-1,AttributeModifier.Operation.MULTIPLY_TOTAL));
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks,9));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void invincible(LivingAttackEvent event){
        if(event.getEntity().getPersistentData().getLong("MahitoDodgeUntil")>now(event.getEntity())&&!event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))event.setCanceled(true);
    }
    @SubscribeEvent public static void status(LivingEvent.LivingTickEvent event){
        LivingEntity e=event.getEntity();if(e.level().isClientSide)return;var data=e.getPersistentData();long now=now(e);
        if(data.contains("MahitoRootUntil")){
            var speed=e.getAttribute(Attributes.MOVEMENT_SPEED);
            if(now>=data.getLong("MahitoRootUntil")){if(speed!=null)speed.removeModifier(ROOT);data.remove("MahitoRootUntil");}
            else{if(e instanceof Mob mob)mob.getNavigation().stop();if(data.getLong("MahitoLaunchedUntil")<=now)e.setDeltaMovement(0,Math.min(0,e.getDeltaMovement().y),0);e.hurtMarked=true;}
        }
        if(data.contains("MahitoArmorUntil")&&now>=data.getLong("MahitoArmorUntil")){var a=e.getAttribute(Attributes.ARMOR);if(a!=null)a.removeModifier(ARMOR_ZERO);data.remove("MahitoArmorUntil");}
        if(data.hasUUID("MahitoSoulOwner")){
            Realm r=REALMS.get(data.getUUID("MahitoSoulOwner"));if(r==null||!r.caster.isAlive()||!inRealm(r.caster,e))restoreSoul(e);
        }
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        for(var it=COLLAPSES.iterator();it.hasNext();){
            Collapse c=it.next();long age=now(c.victim)-c.start;
            if(age>=40||!can(c.caster,c.victim)||c.caster.level()!=c.victim.level()||c.victim.position().distanceToSqr(c.anchor)>16||c.victim.getPersistentData().getLong("DomainSafetyUntil")>now(c.victim)){
                var armor=c.victim.getAttribute(Attributes.ARMOR);if(armor!=null)armor.removeModifier(ARMOR_ZERO);
                c.victim.getPersistentData().putLong("MahitoRootUntil",now(c.victim));it.remove();continue;
            }
            if(age==0||age==20)soulDamage(c.caster,c.victim,6);
            c.victim.teleportTo(c.anchor.x,c.anchor.y,c.anchor.z);c.victim.setDeltaMovement(Vec3.ZERO);c.victim.fallDistance=0;
            if(age%5==0)c.caster.serverLevel().sendParticles(ParticleTypes.DAMAGE_INDICATOR,c.victim.getX(),c.victim.getY()+1,c.victim.getZ(),5,.3,.5,.3,.02);
        }
        for(var it=CASTS.iterator();it.hasNext();){Cast cast=it.next();if(cast.level.getGameTime()<cast.at)continue;it.remove();if(!cast.caster.isAlive()||cast.caster.level()!=cast.level||DomainClash.active(cast.caster))continue;
            double radius=cast.kind==TAIL?9:1.3;
            for(LivingEntity target:cast.caster.level().getEntitiesOfClass(LivingEntity.class,new AABB(cast.origin,cast.origin).inflate(radius,4,radius),t->can(cast.caster,t))){
                Vec3 to=target.position().subtract(cast.origin);if(cast.kind==TAIL&&(to.horizontalDistanceSqr()>81||to.multiply(1,0,1).dot(cast.direction)<0||!body(cast.caster).hasLineOfSight(target)))continue;
                if(cast.kind==TAIL){soulDamage(cast.caster,target,14);Vec3 push=to.multiply(1,0,1).normalize().scale(2.1);target.setDeltaMovement(push.x,.7,push.z);target.getPersistentData().putLong("MahitoLaunchedUntil",now(target)+12);root(target,24);}
                else{soulDamage(cast.caster,target,7);target.setDeltaMovement(target.getDeltaMovement().add(0,1,0));target.addEffect(new MobEffectInstance(MahitoContent.BLEEDING.get(),100));}
                target.hurtMarked=true;
            }
            cast.caster.serverLevel().playSound(null,BlockPos.containing(cast.origin),SoundEvents.GENERIC_EXPLODE,SoundSource.HOSTILE,1,.6f);
        }
        for(Realm realm:List.copyOf(REALMS.values())){
            if(!realm.caster.isAlive()||!character.domainking.SelfEmbryoDomain.active(realm.caster))closeRealm(realm.caster.getUUID());
            else if(now(realm.caster)%40==0)fx(realm.caster,DOMAIN,realm.center,Vec3.ZERO,(float)realm.radius,80);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        LivingEntity victim=event.getEntity();if(!(victim.level() instanceof ServerLevel level))return;
        if(isMahito(victim))closeRealm(victim.getUUID());restoreSoul(victim);
        Entity cause=event.getSource().getEntity();LivingEntity killer=cause instanceof NpcBridge.NpcActor actor?actor.mob:cause instanceof LivingEntity living?living:null;
        if(killer==null||!isMahito(killer)||killer.distanceToSqr(victim)>24*24||victim instanceof MahitoPuppetEntity)return;
        if(!(victim instanceof AbstractVillager||victim instanceof Animal||victim instanceof LowCurseEntity curse&&curse.form().grade()>=3||victim instanceof Monster&&victim.getMaxHealth()<=40))return;
        if(level.getEntitiesOfClass(MahitoPuppetEntity.class,killer.getBoundingBox().inflate(48),e->killer.getUUID().equals(e.ownerId())).size()>=8)return;
        MahitoPuppetEntity puppet=MahitoContent.PUPPET.get().create(level);if(puppet==null)return;
        puppet.moveTo(victim.position());puppet.setOwner(killer.getUUID());if(killer instanceof Mob mob)puppet.setTarget(mob.getTarget());
        level.addFreshEntity(puppet);var actor=NpcBridge.actorFor(killer);if(actor!=null)fx(actor,PUPPET,victim.position(),Vec3.ZERO,1,20);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){restoreSoul(event.getEntity());}
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppingEvent event){for(UUID owner:List.copyOf(REALMS.keySet()))closeRealm(owner);COLLAPSES.clear();CASTS.clear();MARKED.clear();}
    private MahitoCombat(){}
}
