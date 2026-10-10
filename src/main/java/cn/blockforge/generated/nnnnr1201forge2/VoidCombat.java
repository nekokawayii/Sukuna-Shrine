package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 服务端管理裂隙，不依赖原版火焰或可被牛奶解除的药水。 */
public final class VoidCombat {
    public static final String[] NAMES={"解 · 虚空","解 · 虚空罗网","捌 · 虚空","开 · 虚空烬","灶 · 虚空","领域展开 · 虚空御厨子","灵魂剥离"};
    private record Key(UUID owner,UUID target){}
    private static final class Wound {
        final ServerPlayer owner; final ServerLevel level; final LivingEntity target; Vec3 anchor;
        final List<Long> cuts=new ArrayList<>();
        long lockUntil, swallowAt, nextDamage; boolean swallowed, domain, resonated, burning, manual;
        Wound(ServerPlayer p,LivingEntity t){owner=p;level=p.serverLevel();target=t;anchor=t.position();nextDamage=level.getGameTime()+20;}
    }
    private record Fusion(ServerPlayer owner,ServerLevel level,UUID target,Vec3 center,Vec3 point,long due,int count){}
    private record DelayedDismantle(ServerPlayer owner,ServerLevel level,UUID target,Vec3 point,Vec3 direction,float size,long due,long angle){}
    private static final Map<Key,Wound> wounds=new LinkedHashMap<>();
    private static final List<Fusion> fusions=new ArrayList<>();
    private static final List<DelayedDismantle> delayedDismantles=new ArrayList<>();
    private static boolean dealing;
    private static LivingEntity fixedTarget;
    private static net.minecraft.world.damagesource.DamageSource fixedSource;
    private static float fixedAmount;
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void exactDamage(LivingDamageEvent e){if(e.getEntity()==fixedTarget&&(e.getSource()==fixedSource||fixedSource!=null&&CombatSettings.caster(e.getSource())==fixedSource.getEntity()))e.setAmount(fixedAmount);}
    private record ContactHit(ServerPlayer owner,LivingEntity target){}
    private static final List<ContactHit> contacts=new ArrayList<>();
    private static boolean valid(Wound w){return w.owner.isAlive()&&!w.owner.isRemoved()&&w.owner.level()==w.level&&w.target.level()==w.level&&!w.target.isRemoved()&&CombatSettings.canHit(w.owner,w.target);}
    private static void fx(ServerPlayer p,int kind,Vec3 point,Vec3 dir,float size,int life,long seed){Network.send(p.serverLevel(),new Network.Effect(kind,p.getId(),point,dir,size,life,seed));}
    private static void visual(Wound w){
        long now=w.level.getGameTime();int flags=(w.burning?1:0)|(w.domain?2:0);
        fx(w.owner,113,w.target.getBoundingBox().getCenter(),new Vec3(w.cuts.size(),Math.max(0,w.lockUntil-now),flags),Math.max(1,w.target.getBbHeight()),12,w.target.getId());
    }
    private static void remove(Wound w){Network.send(w.level,new Network.Effect(114,w.owner.getId(),w.target.position(),Vec3.ZERO,0,1,w.target.getId()));}
    private static void saveFire(Wound w){
        var root=w.target.getPersistentData();var flames=root.getCompound("SukunaVoidFlames");var damage=root.getCompound("SukunaVoidFlameDamage");var scales=root.getCompound("SukunaVoidFlameScaleR64");var casters=root.getCompound("SukunaVoidFlameCastersR63");String id=w.owner.getUUID().toString();
        if(w.burning&&!w.domain){flames.putInt(id,w.cuts.size());damage.putFloat(id,CombatSettings.fixedSpellDamage(w.owner,VoidTechniqueRules.FIRE_DAMAGE*VoidController.voidDamage(w.owner),false));casters.putBoolean(id,MaidBridge.isCaster(w.owner));scales.putDouble(id,BalanceRules.factor());}
        else{flames.remove(id);damage.remove(id);casters.remove(id);scales.remove(id);}
        if(flames.isEmpty()){root.remove("SukunaVoidFlames");root.remove("SukunaVoidFlameDamage");root.remove("SukunaVoidFlameCastersR63");root.remove("SukunaVoidFlameScaleR64");}else{root.put("SukunaVoidFlames",flames);root.put("SukunaVoidFlameDamage",damage);root.put("SukunaVoidFlameCastersR63",casters);root.put("SukunaVoidFlameScaleR64",scales);}
    }
    @SubscribeEvent public static void persistentFire(LivingEvent.LivingTickEvent event){
        LivingEntity target=event.getEntity();if(!(target.level() instanceof ServerLevel level)||!target.isAlive()||target.tickCount%20!=0||!target.getPersistentData().contains("SukunaVoidFlames"))return;
        var flames=target.getPersistentData().getCompound("SukunaVoidFlames");
        for(String id:Set.copyOf(flames.getAllKeys())){
            UUID owner;try{owner=UUID.fromString(id);}catch(IllegalArgumentException ex){flames.remove(id);continue;}
            Key key=new Key(owner,target.getUUID());if(wounds.containsKey(key))continue;
            ServerPlayer p=MaidBridge.find(level,owner);
            if(EternalLove.protects(owner,target)||p!=null&&!CombatSettings.canHit(p,target)){flames.remove(id);continue;}
            if(p!=null&&p.isAlive()&&!p.isRemoved()&&p.level()==level&&wounds.size()<2048){Wound w=new Wound(p,target);for(int i=0;i<Math.min(VoidTechniqueRules.MAX_RIFTS,Math.max(1,flames.getInt(id)));i++)w.cuts.add((long)(i*360/Math.max(1,flames.getInt(id))));w.burning=true;w.manual=true;wounds.put(key,w);saveFire(w);visual(w);}
            else if(p!=null){damage(p,target,(float)(VoidTechniqueRules.FIRE_DAMAGE*VoidController.voidDamage(p)),true);}
            else {
                var casters=target.getPersistentData().getCompound("SukunaVoidFlameCastersR63");
                // 旧档只存UUID，施术者未加载时无法区分女仆和玩家；先暂停，待其加载后重算，避免旧伤害继续生效。
                if(!casters.contains(id))continue;
                var saved=target.getPersistentData().getCompound("SukunaVoidFlameDamage");float value=saved.contains(id)?saved.getFloat(id):CasterDamageRules.scale(VoidTechniqueRules.FIRE_DAMAGE,casters.getBoolean(id));
                double previous=saved.contains(id)?target.getPersistentData().getCompound("SukunaVoidFlameScaleR64").getDouble(id):1;
                var source=level.damageSources().magic();fixedTarget=target;fixedSource=source;fixedAmount=BalanceRules.storedDamage(value,previous);
                try{target.invulnerableTime=0;target.hurt(source,fixedAmount);}finally{fixedTarget=null;fixedSource=null;fixedAmount=0;}
            }
        }
        if(flames.isEmpty()){target.getPersistentData().remove("SukunaVoidFlames");target.getPersistentData().remove("SukunaVoidFlameDamage");target.getPersistentData().remove("SukunaVoidFlameCastersR63");target.getPersistentData().remove("SukunaVoidFlameScaleR64");}
    }
    // 这里只作捌的破防判定，保持旧阈值；实际伤害在统一结算时减半，避免再次降档。
    private static double power(ServerPlayer p){return Curse.power(p)*CombatSettings.unscaledSpellMultiplier(p);}
    private static void damage(ServerPlayer p,LivingEntity target,float amount,boolean fixed){damage(p,target,amount,fixed,false);}
    private static void damage(ServerPlayer p,LivingEntity target,float amount,boolean fixed,boolean domain){scaledDamage(p,target,amount,fixed,domain,1);}
    private static void scaledDamage(ServerPlayer p,LivingEntity target,float amount,boolean fixed,boolean domain,double fraction){
        if(DomainClash.active(p)||DomainClash.active(target)||!CombatSettings.canHit(p,target))return;
        dealing=true;
        var source=p.serverLevel().damageSources().sonicBoom(p);
        float finalAmount=fixed?CombatSettings.fixedSpellDamage(p,amount,domain):(domain?CombatSettings.domainSpellDamage(p,amount*Curse.power(p)):CombatSettings.spellDamage(p,amount*Curse.power(p)));
        finalAmount*=fraction;
        if(fixed){fixedTarget=target;fixedSource=source;fixedAmount=finalAmount;}
        try {target.invulnerableTime=0;CombatSettings.hurt(p,target,source,finalAmount);}
        finally {dealing=false;fixedTarget=null;fixedSource=null;fixedAmount=0;}
    }
    private static void add(ServerPlayer p,LivingEntity target,boolean dismantle){add(p,target,dismantle,p.getRandom().nextInt(360));}
    private static void add(ServerPlayer p,LivingEntity target,boolean dismantle,long angle){
        if(DomainClash.active(p)||DomainClash.active(target)||!CombatSettings.canHit(p,target))return;
        Key key=new Key(p.getUUID(),target.getUUID());
        if(!wounds.containsKey(key)&&wounds.size()>=2048)return;
        Wound w=wounds.computeIfAbsent(key,k->new Wound(p,target));long now=p.level().getGameTime();int before=w.cuts.size();
        int maximum=Math.min(VoidTechniqueRules.MAX_RIFTS,VoidController.maxRifts(p));
        if(before<maximum)w.cuts.add(angle);
        w.domain|=Skills.inDomain(p,target);
        // 裂隙是永久攻击角度，不再自动过期或被新的裂隙顶掉。
        if(!w.resonated&&VoidTechniqueRules.resonate(before,w.cuts.size())&&!Skills.domainClashing(p)){
            w.resonated=true;damage(p,target,(float)(VoidTechniqueRules.fusion(w.cuts.size())*VoidController.resonance(p)),false);
            fx(p,118,target.getBoundingBox().getCenter(),Vec3.ZERO,Math.min(10,w.cuts.size()),24,target.getId());
        }
        saveFire(w);visual(w);
    }
    public static void formHit(ServerPlayer p,LivingEntity target){add(p,target,true);Wound w=wounds.get(new Key(p.getUUID(),target.getUUID()));if(w!=null)w.manual=true;}
    public static boolean ready(ServerPlayer p,int skill){
        if(skill==3&&fusions.stream().anyMatch(f->f.owner==p)){p.displayClientMessage(Component.literal("虚空烬正在引爆"),true);return false;}
        return true;
    }
    private static boolean inField(ServerPlayer p,LivingEntity e){return e.position().distanceToSqr(Skills.domainCenter(p))<=VoidTechniqueRules.FIELD_RADIUS*VoidTechniqueRules.FIELD_RADIUS;}
    private static LivingEntity contact(ServerPlayer p){LivingEntity t=Skills.aimTarget(p);return t!=null&&t.getBoundingBox().inflate(0.2).clip(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(3))).isPresent()?t:null;}
    public static void cast(ServerPlayer p,int skill){
        switch(skill){
            case 0 -> dismantle(p,Skills.aimTarget(p));
            case 1 -> slashNet(p);
            case 2 -> {LivingEntity t=contact(p);if(t==null)t=Skills.aimTarget(p);if(t!=null)cleave(p,t);else fx(p,115,Skills.aimPoint(p,24),p.getLookAngle(),12,12,p.getRandom().nextInt(360));}
            case 3 -> {
                int count=0;Vec3 point=Skills.aimPoint(p,48),center=Skills.domainCenter(p);
                for(Wound w:wounds.values())if(w.owner==p&&valid(w)&&inField(p,w.target)&&!w.cuts.isEmpty()){
                    count+=w.cuts.size(); w.burning=false; saveFire(w);
                    fusions.add(new Fusion(p,p.serverLevel(),w.target.getUUID(),w.target.getBoundingBox().getCenter(),point,p.level().getGameTime()+VoidTechniqueRules.FUSION_TICKS,w.cuts.size()));
                    fx(p,116,w.target.getBoundingBox().getCenter(),point,Math.max(3,w.cuts.size()),VoidTechniqueRules.FUSION_TICKS,w.target.getId()); visual(w);
                }
                fx(p,120,point,center,200,30,count);
                p.displayClientMessage(Component.literal("开·虚空：蓄力引爆 "+count+" 道裂隙"),true);
            }
            case 4 -> {
                fx(p,116,Skills.aimPoint(p,24),p.getLookAngle(),3,24,0);
                // 伤害事件可能触发对战收尾或敌友清理，不能直接遍历可被回调修改的集合。
                for(Wound w:List.copyOf(wounds.values()))if(w.owner==p&&valid(w)&&inField(p,w.target)){
                    w.burning=true;w.manual=true;saveFire(w);visual(w);
                    Vec3 center=w.target.getBoundingBox().getCenter();fx(p,116,center,center.add(0,1,0),Math.max(2,w.cuts.size()),24,w.target.getId());
                    furnaceBlast(p,center);
                    damage(p,w.target,VoidTechniqueRules.FIRE_DAMAGE*(float)VoidController.voidDamage(p),true);
                    if(CombatSettings.canHit(p,w.target)){
                        var tag=w.target.getPersistentData();tag.putUUID("SukunaVoidIgnitionOwner",p.getUUID());tag.putBoolean("SukunaVoidIgnitionMaid",MaidBridge.isCaster(p));tag.putLong("SukunaVoidIgnitionUntil",p.level().getGameTime()+160);
                        w.target.setSecondsOnFire(8);
                    }
                }
            }
        }
    }
    private static ServerPlayer playerBlast;
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void playerBlastDamage(LivingHurtEvent event){
        if(playerBlast!=null&&event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)&&CombatSettings.caster(event.getSource())==playerBlast){
            if(!CombatSettings.canHit(playerBlast,event.getEntity())){event.setCanceled(true);return;}
            event.setAmount(BalanceRules.damage(event.getAmount()));
        }
    }
    private static void furnaceBlast(ServerPlayer p,Vec3 center){
        ServerLevel level=p.serverLevel();
        if(!MaidBridge.isCaster(p)){
            // 玩家原有爆炸行为不变；女仆绝不把FakePlayer交给第三方爆炸监听器。
            ServerPlayer previous=playerBlast;playerBlast=p;
            try{level.explode(p,center.x,center.y,center.z,2.5f,false,net.minecraft.world.level.Level.ExplosionInteraction.NONE);}finally{playerBlast=previous;}
            return;
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,center.x,center.y,center.z,1,0,0,0,0);
        level.playSound(null,center.x,center.y,center.z,SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,4,0.7f);
        LivingEntity body=MaidBridge.body(p);
        for(LivingEntity target:Skills.targets(level,p,center,5)){
            if(target.ignoreExplosion())continue;
            double distance=target.position().distanceTo(center);
            double visibility=net.minecraft.world.level.Explosion.getSeenPercent(center,target);
            float amount=BalanceRules.damage(CasterDamageRules.furnaceBlast(distance,visibility,true));
            if(amount<=0)continue;
            if(CombatSettings.hurt(p,target,level.damageSources().explosion(body,body),amount)){
                double impact=(1-distance/5)*visibility;Vec3 push=target.getEyePosition().subtract(center).normalize().scale(impact);
                target.push(push.x,push.y,push.z);target.hurtMarked=true;
            }
        }
    }
    /** 虚空灶附带的原版着火伤害也需识别女仆；不叠加成长、不重复减半。 */
    public static void ignitionDamage(LivingHurtEvent event){
        LivingEntity target=event.getEntity();var tag=target.getPersistentData();
        if(!tag.hasUUID("SukunaVoidIgnitionOwner")||tag.getLong("SukunaVoidIgnitionUntil")<=target.level().getGameTime())return;
        UUID id=tag.getUUID("SukunaVoidIgnitionOwner");ServerPlayer owner=target.level() instanceof ServerLevel level?MaidBridge.find(level,id):null;
        if(EternalLove.protects(id,target)||owner!=null&&!CombatSettings.canHit(owner,target)){event.setCanceled(true);target.clearFire();return;}
        event.setAmount(BalanceRules.damage(CasterDamageRules.scale(event.getAmount(),owner==null?tag.getBoolean("SukunaVoidIgnitionMaid"):MaidBridge.isCaster(owner))));
    }
    public static void dismantle(ServerPlayer p,LivingEntity target){
        if(Skills.inDomain(p,p)&&target==null)target=Skills.closestToCrosshair(p,Skills.targets(p.serverLevel(),p,Skills.domainCenter(p),200));
        Vec3 point=target==null?Skills.aimPoint(p,50):target.getBoundingBox().inflate(0.08).clip(p.getEyePosition(),target.getBoundingBox().getCenter()).orElse(target.getBoundingBox().getCenter());
        Vec3 direction=p.getLookAngle();long angle=p.getRandom().nextInt(360);long tint=angle+(VoidController.redCut(p)?720:0);
        Vec3 collisionAxis=NetBranches.side(direction).scale(Math.max(12,VoidController.slashLength(p))/2.0);if(CombatExpansion.collideSlash(p,point.subtract(collisionAxis),point.add(collisionAxis),direction))return;
        fx(p,123,point,direction,SlashReach.INFINITE,6,tint);
        for(LivingEntity victim:SlashReach.targets(p,target,point,direction,angle))delayedDismantles.add(new DelayedDismantle(p,p.serverLevel(),victim.getUUID(),point,direction,0,p.level().getGameTime()+6,angle));
        delayedDismantles.add(new DelayedDismantle(p,p.serverLevel(),null,point,direction,SlashReach.INFINITE,p.level().getGameTime()+6,angle));
    }
    private static void slashNet(ServerPlayer p){
        Vec3 origin=p.getEyePosition(),dir=p.getLookAngle();int index=0;
        for(LivingEntity t:Skills.targets(p.serverLevel(),p,p.position(),32)){
            if(t.distanceToSqr(p)>32*32)continue;
            Vec3 end=t.getBoundingBox().getCenter();fx(p,122,end.subtract(dir.scale(2)),dir,Math.max(5,VoidController.slashLength(p)*0.45f),10,index++);
            dismantle(p,t);
        }
        if(index==0)fx(p,122,origin.add(dir.scale(8)),dir,VoidController.slashLength(p)*0.45f,10,0);
    }
    private static void cleave(ServerPlayer p,LivingEntity t){
        add(p,t,false);Wound wound=wounds.get(new Key(p.getUUID(),t.getUUID()));if(wound!=null)wound.manual=true;
        double caster=p.getAttributeValue(Attributes.ATTACK_DAMAGE)+30*power(p);
        double victim=t.getAttribute(Attributes.ATTACK_DAMAGE)==null?0:t.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean pierce=VoidTechniqueRules.pierces(caster,victim);
        float amount=(float)((pierce?160:8)*VoidController.voidDamage(p));
        double fraction=SukunaBranches.cleaveFactor(p);
        for(int i=0;i<VoidTechniqueRules.CLEAVE_SLASHES;i++){
            double angle=(i-1)*Math.PI/5.0;Vec3 dir=p.getLookAngle().yRot((float)angle);
            fx(p,119,t.getBoundingBox().getCenter(),dir,Math.max(2,t.getBbHeight()),20,t.getId()+i);
            scaledDamage(p,t,amount/2,false,false,fraction);
        }
    }
    @SubscribeEvent public static void melee(LivingDamageEvent e){
        ServerPlayer p=CombatSettings.caster(e.getSource());if(dealing||e.getAmount()<=0||p==null||!VoidJourney.evolved(p)||!Curse.data(p).getBoolean("Fist")||!(e.getSource().is(DamageTypes.PLAYER_ATTACK)||MaidBridge.isCaster(p)&&e.getSource().is(DamageTypes.MOB_ATTACK))||SoulCombat.suppressed(p)||!Curse.canUseTechnique(p))return;
        if(p.distanceToSqr(e.getEntity())>16||!CombatSettings.canHit(p,e.getEntity()))return;
        long now=p.level().getGameTime();if(now<p.getPersistentData().getLong("VoidContactCooldown")||!Curse.spend(p,30))return;
        p.getPersistentData().putLong("VoidContactCooldown",now+20);contacts.add(new ContactHit(p,e.getEntity()));
    }
    @SubscribeEvent public static void heal(LivingHealEvent e){
        if(e.getEntity().level().isClientSide)return;
        long now=e.getEntity().level().getGameTime();
        // 永久裂隙不禁疗；灶点燃造成的伤害仍由正常伤害事件处理。
    }
    public static void domainHit(ServerPlayer p,LivingEntity t){
        Vec3 end=t.getBoundingBox().getCenter();Wound local=wounds.get(new Key(p.getUUID(),t.getUUID()));
        int angleCount=local==null?1:Math.max(1,local.cuts.size());double angle=p.getRandom().nextInt(angleCount)*Math.PI*2/angleCount;
        Vec3 origin=end.add(Math.cos(angle)*2,Math.sin(angle*1.3),Math.sin(angle)*2);
        for(Wound source:wounds.values())if(source.owner==p&&source.domain&&source.target!=t&&valid(source)){origin=source.target.getBoundingBox().getCenter();break;}
        fx(p,121,origin,end,Math.max(2,t.getBbHeight()),8,p.getRandom().nextInt(360));
        int count=Math.min(5,Math.max(2,local==null?2:local.cuts.size()+1));
        for(int i=0;i<count;i++) {
            double a=angle+i*Math.PI*2/count;
            fx(p,121,end.add(Math.cos(a)*2,Math.sin(a*1.3),Math.sin(a)*2),end,Math.max(2,t.getBbHeight()),8,p.getRandom().nextInt(360));
            add(p,t,false);
        }
        scaledDamage(p,t,(float)(4*VoidController.voidDamage(p)),false,true,SlashFormRules.DAMAGE);
        // 领域命中沿用灵魂剪切虚线，并在0.3秒后补刀光，避免只剩裂隙。
        Vec3 direction=new Vec3(Math.cos(angle),0.4,Math.sin(angle));float size=Math.max(4,t.getBbHeight()*2);long cutAngle=p.getRandom().nextInt(360);
        fx(p,123,end,direction,size,6,cutAngle+720);
        Network.send(p.serverLevel(),new Network.Effect(93,t.getId(),end,Vec3.ZERO,size,12,cutAngle).fromCaster(p));
        delayedDismantles.add(new DelayedDismantle(p,p.serverLevel(),null,end,direction,size,p.level().getGameTime()+6,cutAngle));
    }
    public static void closeDomain(ServerPlayer p,boolean burst){
        for(Iterator<Wound> it=wounds.values().iterator();it.hasNext();){Wound w=it.next();if(w.owner!=p||!w.domain)continue;if(burst&&valid(w)){damage(p,w.target,VoidTechniqueRules.closure(w.cuts.size()),false,true);fx(p,118,w.target.getBoundingBox().getCenter(),Vec3.ZERO,Math.min(10,w.cuts.size()),20,w.target.getId());}remove(w);it.remove();}
    }
    public static void cancelCombat(ServerPlayer p){
        for(Iterator<Wound> it=wounds.values().iterator();it.hasNext();){Wound w=it.next();if(w.owner==p){w.burning=false;saveFire(w);remove(w);it.remove();}}
        fusions.removeIf(f->f.owner==p);delayedDismantles.removeIf(d->d.owner==p);contacts.removeIf(c->c.owner==p);
    }
    static void testReset(ServerPlayer p){cancelCombat(p);for(Iterator<Wound> it=wounds.values().iterator();it.hasNext();){Wound w=it.next();if(w.target==p){w.burning=false;saveFire(w);remove(w);it.remove();}}fusions.removeIf(f->p.getUUID().equals(f.target));delayedDismantles.removeIf(d->p.getUUID().equals(d.target));contacts.removeIf(c->c.target==p);}
    public static void protect(ServerPlayer p,UUID target){Wound w=wounds.remove(new Key(p.getUUID(),target));if(w!=null){w.burning=false;saveFire(w);remove(w);}}
    public static void tick(){
        for(Iterator<DelayedDismantle> it=delayedDismantles.iterator();it.hasNext();){DelayedDismantle d=it.next();if(d.level.getGameTime()<d.due)continue;it.remove();if(!d.owner.isAlive()||d.owner.level()!=d.level)continue;if(d.size!=0)fx(d.owner,115,d.point,d.direction,d.size,12,d.angle+(VoidController.redCut(d.owner)?720:0));if(d.target!=null&&d.level.getEntity(d.target) instanceof LivingEntity target&&CombatSettings.canHit(d.owner,target)){add(d.owner,target,true,d.angle);Wound w=wounds.get(new Key(d.owner.getUUID(),target.getUUID()));if(w!=null)w.manual=true;scaledDamage(d.owner,target,VoidTechniqueRules.DISMANTLE_DAMAGE*(float)VoidController.voidDamage(d.owner),false,false,SlashFormRules.DAMAGE);}}
        for(ContactHit hit:List.copyOf(contacts))if(hit.owner.isAlive()&&!hit.owner.isRemoved()&&hit.owner.level()==hit.target.level()&&CombatSettings.canHit(hit.owner,hit.target))cleave(hit.owner,hit.target);
        contacts.clear();
        for(Iterator<Wound> it=wounds.values().iterator();it.hasNext();){
            Wound w=it.next();long now=w.level.getGameTime();
            if(!valid(w)){remove(w);it.remove();continue;}
            // 永久裂隙不再定身；它只记录宿傩下一次攻击可以出现的角度。
            boolean muted=DomainClash.active(w.owner)||w.domain&&!w.manual&&Skills.domainClashing(w.owner);
            // 裂隙不持续伤害，只有灶点燃后才造成虚空火焰伤害。
            if(!muted&&w.burning&&now>=w.nextDamage){w.nextDamage=now+20;saveFire(w);damage(w.owner,w.target,(float)(VoidTechniqueRules.FIRE_DAMAGE*VoidController.voidDamage(w.owner)),true);}
            // cuts 永久保留，直到领域闭合或玩家关系保护主动清理。
            if(w.cuts.isEmpty()||!w.target.isAlive()){remove(w);it.remove();continue;}
            if(now%5==0){if(muted)remove(w);else visual(w);}
        }
        for(Iterator<Fusion> it=fusions.iterator();it.hasNext();){Fusion f=it.next();if(!f.owner.isAlive()||f.owner.isRemoved()||f.owner.level()!=f.level){it.remove();continue;}if(f.level.getGameTime()<f.due)continue;it.remove();
            if(f.level.getEntity(f.target) instanceof LivingEntity target&&CombatSettings.canHit(f.owner,target))damage(f.owner,target,VoidTechniqueRules.fusion(f.count)* (float)VoidController.voidDamage(f.owner),false);
            Wound w=wounds.remove(new Key(f.owner.getUUID(),f.target));if(w!=null){w.burning=false;saveFire(w);remove(w);}
            fx(f.owner,120,f.point,f.center,200,30,f.count);f.level.playSound(null,f.point.x,f.point.y,f.point.z,SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,4,0.5f);
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e){wounds.clear();fusions.clear();contacts.clear();delayedDismantles.clear();dealing=false;fixedTarget=null;fixedSource=null;fixedAmount=0;}
    private VoidCombat(){}
}
