package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.network.PacketDistributor;

/** 一对一、服务端裁决；客户端只上报按下/松开，不能提交分数或胜负。 */
public final class DomainClash {
    private record Field(LivingEntity owner,net.minecraft.world.level.Level level,Vec3 center,double radius){}
    private static final Map<UUID,Field> fields=new LinkedHashMap<>();
    private static final Map<UUID,Match> matches=new HashMap<>();
    private static final class Side {
        final Field field;final Vec3 anchor;final DomainClashInput input=new DomainClashInput();
        int cps,credit;
        Side(Field f){field=f;anchor=f.owner.position();}
    }
    private static final class Match {
        final UUID id=UUID.randomUUID();final Side a,b;int age,lead;
        Match(Field a,Field b){this.a=new Side(a);this.b=new Side(b);}
    }
    public static boolean active(LivingEntity entity){return matches.containsKey(entity.getUUID());}
    public static boolean stunned(LivingEntity entity){return entity.getPersistentData().getLong("DomainClashStunUntil")>entity.level().getGameTime();}
    public static boolean penalized(Player p){return Curse.active(p,"DomainClashPenaltyUntil");}
    public static int penaltyTicks(Player p){return (int)Math.max(0,Math.min(DomainClashRules.BURNOUT_TICKS,Curse.data(p).getLong("DomainClashPenaltyUntil")-p.level().getGameTime()));}
    public static boolean player(LivingEntity e){return e instanceof ServerPlayer&&!(e instanceof net.minecraftforge.common.util.FakePlayer);}
    public static void opened(LivingEntity owner,Vec3 center,double radius){
        if(!(player(owner)||owner instanceof MaidBridge.Actor||owner instanceof YujiEntity y&&!y.decoy()&&!y.familyMember()||owner instanceof GuardianEntity g&&!g.friendly()))return;
        if(SixfoldFinale.active(owner)||SixfoldFinale.controlled(owner)||active(owner))return;
        Field previous=fields.get(owner.getUUID());if(previous!=null&&previous.owner==owner&&previous.level==owner.level()&&previous.center.equals(center)&&previous.radius==radius)return;
        fields.put(owner.getUUID(),new Field(owner,owner.level(),center,radius));discover();
    }
    private static boolean live(Field f){
        LivingEntity e=f.owner;
        if(!e.isAlive()||e.isRemoved()||e.isSpectator()||e.level()!=f.level)return false;
        if(e instanceof MaidBridge.Actor a)return a.maid.level()==f.level&&!EternalLove.settling(a.maid)&&(Skills.clashDomainActive(a)||GuardianPlayerSkills.sustaining(a));
        if(e instanceof ServerPlayer p)return !p.isCreative()&&p.server.getPlayerList().getPlayer(p.getUUID())==p&&(Skills.clashDomainActive(p)||GuardianPlayerSkills.sustaining(p));
        if(e instanceof YujiEntity y)return y.clashDomainActive();
        return e instanceof GuardianEntity g&&g.clashDomainActive();
    }
    private static boolean enemies(Field a,Field b){
        if(a.owner.level()!=b.owner.level())return false;
        if(a.owner instanceof ServerPlayer p&&b.owner instanceof ServerPlayer q)
            return CombatSettings.canHit(p,MaidBridge.body(q))||CombatSettings.canHit(q,MaidBridge.body(p));
        ServerPlayer p=a.owner instanceof ServerPlayer v?v:b.owner instanceof ServerPlayer v?v:null;
        LivingEntity boss=p==a.owner?b.owner:a.owner;
        return p!=null&&boss instanceof Mob mob&&BossTargets.hostile(mob,p)&&CombatSettings.canHit(p,boss);
    }
    private static void discover(){
        fields.values().removeIf(f->!live(f)&&!active(f.owner));
        List<Field> list=new ArrayList<>(fields.values());
        for(int i=0;i<list.size();i++)for(int j=i+1;j<list.size();j++){
            Field a=list.get(i),b=list.get(j);
            if(active(a.owner)||active(b.owner)||!live(a)||!live(b)||!enemies(a,b)||!DomainClashRules.overlaps(a.center.distanceToSqr(b.center),a.radius,b.radius))continue;
            Match m=new Match(a,b);matches.put(a.owner.getUUID(),m);matches.put(b.owner.getUUID(),m);
            prepare(m.a);prepare(m.b);sync(m);
        }
    }
    private static void prepare(Side s){
        LivingEntity e=body(s.field.owner);e.stopRiding();e.setDeltaMovement(Vec3.ZERO);
        var saved=e.getPersistentData();if(!saved.contains("ClashPreviousGravity"))saved.putBoolean("ClashPreviousGravity",e.isNoGravity());e.setNoGravity(true);
        if(s.field.owner instanceof ServerPlayer p){if(player(p))p.closeContainer();Skills.cancelSustained(p);GuardianPlayerSkills.clashInterrupt(p);Curse.data(p).remove("Iwate");Curse.data(p).remove("PureLandNoHeal");p.getPersistentData().remove("GuardianSkillNoHeal");}
        else if(e instanceof YujiEntity y)y.clashInterrupt();else if(e instanceof GuardianEntity g)g.clashInterrupt();
        visual(s.field,false);pin(e,s.anchor);
    }
    private static void visual(Field f,boolean show){
        if(f.owner instanceof ServerPlayer p){Skills.clashVisual(p,show);GuardianPlayerSkills.clashVisual(p,show);}
        else if(f.owner instanceof YujiEntity y)y.clashVisual(show);else if(f.owner instanceof GuardianEntity g)g.clashVisual(show);
    }
    private static void extend(Field f,int ticks){
        if(f.owner instanceof ServerPlayer p){Skills.extendDomain(p,ticks);GuardianPlayerSkills.extendDomain(p,ticks);}
        else if(f.owner instanceof YujiEntity y)y.extendDomain(ticks);else if(f.owner instanceof GuardianEntity g)g.extendDomain(ticks);
    }
    private static void close(Field f){
        if(f.owner instanceof ServerPlayer p){Skills.closeDomain(p);GuardianPlayerSkills.closeOwnedDomain(p,false);}
        else if(f.owner instanceof YujiEntity y)y.clashCloseDomain();else if(f.owner instanceof GuardianEntity g)g.clashCloseDomain();
    }
    public static void press(ServerPlayer p,UUID id,boolean down){
        Match m=matches.get(p.getUUID());if(m==null||!m.id.equals(id))return;
        Side s=m.a.field.owner==p?m.a:m.b;if(s.field.owner!=p||!live(s.field))return;
        s.input.accept(down,m.age>=DomainClashRules.READY_TICKS,p.level().getGameTime());
    }
    private static int clicks(Side s,int age){
        if(player(s.field.owner))return s.input.drain();
        if(s.field.owner instanceof MaidBridge.Actor){s.cps=5;s.credit+=s.cps;int n=s.credit/20;s.credit%=20;return n;}
        if((age-DomainClashRules.READY_TICKS)%20==0)s.cps=DomainClashRules.bossCps(s.field.owner instanceof GuardianEntity,s.field.owner.getRandom().nextInt(100000));
        s.credit+=s.cps;int n=s.credit/20;s.credit%=20;return n;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;discover();
        for(Match m:new HashSet<>(matches.values())){
            boolean a=live(m.a.field),b=live(m.b.field);
            if(!a||!b){finish(m,a&&!b?1:b&&!a?-1:0);continue;}
            if(!enemies(m.a.field,m.b.field)){finish(m,0);continue;}
            pin(m.a.field.owner,m.a.anchor);pin(m.b.field.owner,m.b.anchor);
            if(m.a.field.owner instanceof ServerPlayer)extend(m.a.field,1);if(m.b.field.owner instanceof ServerPlayer)extend(m.b.field,1);
            if(m.age>=DomainClashRules.READY_TICKS){m.lead+=clicks(m.a,m.age)-clicks(m.b,m.age);int winner=DomainClashRules.winner(m.lead);if(winner!=0){finish(m,winner);continue;}}
            if(++m.age>=DomainClashRules.READY_TICKS+DomainClashRules.LIMIT_TICKS){finish(m,0);continue;}
            sync(m);
        }
    }
    public static void hold(LivingEntity e){Match m=matches.get(e.getUUID());if(m!=null)pin(e,m.a.field.owner.getUUID().equals(e.getUUID())?m.a.anchor:m.b.anchor);else if(stunned(e))pin(e,new Vec3(e.getPersistentData().getDouble("ClashAnchorX"),e.getPersistentData().getDouble("ClashAnchorY"),e.getPersistentData().getDouble("ClashAnchorZ")));}
    private static LivingEntity body(LivingEntity e){return e instanceof ServerPlayer p?MaidBridge.body(p):e;}
    private static void pin(LivingEntity e,Vec3 point){
        e=body(e);
        if(e instanceof Mob mob)mob.getNavigation().stop();
        e.stopRiding();e.setDeltaMovement(Vec3.ZERO);e.fallDistance=0;
        if(e instanceof ServerPlayer p){if(p.position().distanceToSqr(point)>0.000001)p.connection.teleport(point.x,point.y,point.z,p.getYRot(),p.getXRot());}
        else e.setPos(point.x,point.y,point.z);
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer p&&active(p)){Match m=matches.get(p.getUUID());Field f=m.a.field.owner==p?m.a.field:m.b.field;if(f.owner==p&&f.level==p.level())hold(p);}}
    private static void defeat(Field f){
        if(f.owner instanceof ServerPlayer p){Curse.status(p,"DomainClashPenaltyUntil",DomainClashRules.BURNOUT_TICKS);Curse.enterBurnout(p,DomainClashRules.BURNOUT_TICKS);p.sendSystemMessage(Component.literal("领域对拼失败：术式熔断2分钟，不能提前修复；每次治疗消耗咒力上限的55%，不足则治疗不生效。"));}
        else {LivingEntity e=f.owner;var t=e.getPersistentData();t.putLong("DomainClashStunUntil",e.level().getGameTime()+DomainClashRules.STUN_TICKS);t.putDouble("ClashAnchorX",e.getX());t.putDouble("ClashAnchorY",e.getY());t.putDouble("ClashAnchorZ",e.getZ());if(e instanceof YujiEntity y)y.clashInterrupt();else if(e instanceof GuardianEntity g)g.clashInterrupt();}
    }
    private static void finish(Match m,int winner){
        // 关闭时仍标记为对拼，禁止败者虚空领域触发闭合爆发。
        if(winner==0){close(m.a.field);close(m.b.field);}
        else {Field lost=winner>0?m.b.field:m.a.field;close(lost);defeat(lost);}
        matches.remove(m.a.field.owner.getUUID());matches.remove(m.b.field.owner.getUUID());
        restoreGravity(m.a.field.owner);restoreGravity(m.b.field.owner);
        if(winner!=0){Field won=winner>0?m.a.field:m.b.field;extend(won,DomainClashRules.EXTEND_TICKS);visual(won,true);}
        result(m.a,m.id,winner);result(m.b,m.id,-winner);
        fields.values().removeIf(f->!live(f));
    }
    private static void result(Side s,UUID id,int winner){if(player(s.field.owner)&&s.field.owner instanceof ServerPlayer p){Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Network.ClashState(id,false,DomainClashRules.percent(winner*25),0,0,0,"",winner));p.sendSystemMessage(Component.literal(winner>0?"领域对拼获胜：领域恢复，剩余时间额外延长20秒。":winner<0?"领域对拼失败，领域已解除。":"领域对拼平局或中止：双方领域解除，不追加对拼败北惩罚。"));}}
    private static void sync(Match m){sync(m,m.a,m.b,m.lead);sync(m,m.b,m.a,-m.lead);}
    private static void sync(Match m,Side self,Side other,int lead){if(player(self.field.owner)&&self.field.owner instanceof ServerPlayer p){String name=body(other.field.owner).getDisplayName().getString();if(name.length()>128)name=name.substring(0,128);Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Network.ClashState(m.id,true,DomainClashRules.percent(lead),Math.max(0,DomainClashRules.READY_TICKS-m.age),Math.max(0,DomainClashRules.READY_TICKS+DomainClashRules.LIMIT_TICKS-m.age),other.cps,name,0));}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(LivingAttackEvent event){
        if(event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        Entity source=event.getSource().getEntity();ServerPlayer caster=CombatSettings.caster(event.getSource());
        if(active(event.getEntity())||source instanceof LivingEntity e&&(active(e)||stunned(e))||caster!=null&&active(caster))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void heal(LivingHealEvent event){
        ServerPlayer p=MaidBridge.progression(event.getEntity());
        if(p==null||!penalized(p)||event.getAmount()<=0||p.getHealth()>=p.getMaxHealth())return;
        if(active(p)||!Curse.spendEffective(p,DomainClashRules.healCost(Curse.maximum(p))))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){Match m=matches.get(event.getEntity().getUUID());if(m!=null)finish(m,m.a.field.owner.getUUID().equals(event.getEntity().getUUID())?-1:1);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){Match m=matches.get(event.getEntity().getUUID());if(m!=null)finish(m,m.a.field.owner.getUUID().equals(event.getEntity().getUUID())?-1:1);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){Match m=matches.get(p.getUUID());if(m!=null)finish(m,m.a.field.owner==p?-1:1);}}
    static void testReset(ServerPlayer p){Match m=matches.get(p.getUUID());if(m!=null)finish(m,0);fields.remove(p.getUUID());restoreGravity(p);}
    private static void restoreGravity(LivingEntity e){e=body(e);var saved=e.getPersistentData();if(saved.contains("ClashPreviousGravity")){e.setNoGravity(saved.getBoolean("ClashPreviousGravity"));saved.remove("ClashPreviousGravity");}}
    @SubscribeEvent public static void recoverGravity(LivingEvent.LivingTickEvent event){LivingEntity e=event.getEntity();if(!e.level().isClientSide&&!active(e))restoreGravity(e);}
    @SubscribeEvent public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event){for(Match m:new HashSet<>(matches.values()))finish(m,0);}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){for(Match m:new HashSet<>(matches.values())){restoreGravity(m.a.field.owner);restoreGravity(m.b.field.owner);}matches.clear();fields.clear();}
    private DomainClash(){}
}
