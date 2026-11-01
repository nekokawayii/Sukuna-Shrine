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
    /** 新领域只需实现这四个生命周期回调；不要求继承玩家或内置 NPC。 */
    public interface Lifecycle {
        boolean active();
        void extend(int ticks);
        void close();
        default void visual(boolean show) { }
        default void clash(ClashView view) { }
        /** 统一裁决后的通知；不要在此重复延时、开域或施工。 */
        default void resolved(Outcome outcome) { }
    }
    public enum Outcome { WON, LOST, CANCELLED }
    /** 扩展可接收对拼阶段；只做表现，不在回调里重新展开领域或递归登记。 */
    public record ClashView(boolean active,Vec3 enemyCenter,float progress,int remaining) { }
    /** 所有球壳、地板与图片共用的切面；本方占 signedDistance<=0 的半空间。 */
    public record Partition(Vec3 plane,Vec3 normal) {
        public double signedDistance(Vec3 point){return point.subtract(plane).dot(normal);}
        public boolean contains(Vec3 point){return signedDistance(point)<=0;}
    }
    public static Partition partition(UUID owner){
        Match m=matches.get(owner);if(m==null)return null;
        return new Partition(m.plane,owner.equals(m.a.field.owner.getUUID())?m.axis:m.axis.scale(-1));
    }
    public static boolean ownsTerrain(UUID owner,Vec3 point){var cut=partition(owner);return cut==null||cut.contains(point);}
    private static void updatePartition(Match m){
        Vec3 delta=m.b.field.center.subtract(m.a.field.center);
        Vec3 axis=new Vec3(delta.x,0,delta.z).normalize();
        if(axis.lengthSqr()<.01){delta=m.b.anchor.subtract(m.a.anchor);axis=new Vec3(delta.x,0,delta.z).normalize();}
        if(axis.lengthSqr()<.01)axis=new Vec3(1,0,0);
        double distance=m.b.field.center.subtract(m.a.field.center).dot(axis);
        double offset=character.domain.DomainVisualRules.partitionOffset(distance,m.a.field.radius,m.b.field.radius,m.lead);
        Vec3 plane=m.a.field.center.add(axis.scale(offset));
        boolean changed=m.plane==null||plane.distanceToSqr(m.plane)>.000001;
        m.axis=axis;m.plane=plane;
        if(changed)for(Side side:List.of(m.a,m.b))character.domain.DomainTerrain.refresh((net.minecraft.server.level.ServerLevel)side.field.level,side.field.owner.getUUID());
    }
    private record Field(LivingEntity owner,net.minecraft.world.level.Level level,Vec3 center,double radius,Lifecycle lifecycle,character.domain.DomainPresentation.Style style){}
    private static final Map<UUID,Field> fields=new LinkedHashMap<>();
    private static final Map<UUID,Match> matches=new HashMap<>();
    private static final class Side {
        final Field field;final Vec3 anchor;final DomainClashInput input=new DomainClashInput();
        int cps,credit;
        Side(Field f){field=f;anchor=f.owner.position();}
    }
    private static final class Match {
        final UUID id=UUID.randomUUID();final Side a,b;int age,lead;Vec3 plane,axis;boolean finishing;
        final net.minecraft.server.level.ServerBossEvent bar=new net.minecraft.server.level.ServerBossEvent(
            Component.literal("领域对拼"),net.minecraft.world.BossEvent.BossBarColor.PURPLE,net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
        int limit(){return character.domain.DomainVisualRules.clashLimit(player(a.field.owner)||player(b.field.owner));}
        Match(Field a,Field b){this.a=new Side(a);this.b=new Side(b);}
    }
    public static boolean active(LivingEntity entity){return matches.containsKey(entity.getUUID());}
    public static boolean stunned(LivingEntity entity){return entity.getPersistentData().getLong("DomainClashStunUntil")>entity.level().getGameTime();}
    public static boolean penalized(Player p){return Curse.active(p,"DomainClashPenaltyUntil");}
    public static int penaltyTicks(Player p){return (int)Math.max(0,Math.min(DomainClashRules.BURNOUT_TICKS,Curse.data(p).getLong("DomainClashPenaltyUntil")-p.level().getGameTime()));}
    public static boolean player(LivingEntity e){return e instanceof ServerPlayer&&!(e instanceof net.minecraftforge.common.util.FakePlayer);}
    public static void opened(LivingEntity owner,Vec3 center,double radius){opened(owner,center,radius,null);}
    /** 先建立领域状态，再登记；必须从服务端调用。对拼过程中领域伤害应检查 active(owner)。 */
    public static void opened(LivingEntity owner,Vec3 center,double radius,Lifecycle lifecycle){
        opened(owner,center,radius,lifecycle,null);
    }
    public static void opened(LivingEntity owner,Vec3 center,double radius,Lifecycle lifecycle,character.domain.DomainPresentation.Style style){
        if(owner==null||owner.level().isClientSide||center==null||!Double.isFinite(radius)||radius<=0)return;
        if(owner instanceof Mob mob&&NpcBridge.supports(mob))owner=NpcBridge.actorFor(mob);
        if(lifecycle==null&&!(player(owner)||owner instanceof NpcBridge.NpcActor||owner instanceof MaidBridge.Actor||owner instanceof YujiEntity y&&!y.decoy()&&!y.familyMember()||owner instanceof GuardianEntity g&&!g.friendly()||owner instanceof GojoEntity))return;
        if(SixfoldFinale.active(owner)||SixfoldFinale.controlled(owner)||active(owner))return;
        boolean fresh=!fields.containsKey(owner.getUUID())||!live(fields.get(owner.getUUID()));
        fields.put(owner.getUUID(),new Field(owner,owner.level(),center,radius,lifecycle,
            style==null?character.domain.DomainPresentation.style(owner):style));
        if(fresh&&owner instanceof ServerPlayer p)character.mainline.Mainline.onDomainOpened(p);
        if(fresh&&body(owner) instanceof Mob mob)mob.getPersistentData().putLong("NpcDomainReadyAt",mob.level().getGameTime()+character.npc.NpcBattleRules.DOMAIN_COOLDOWN);
        discover();
    }
    public static boolean registered(LivingEntity owner){Field f=fields.get(owner.getUUID());return f!=null&&live(f);}
    /** 自定义领域自然结束后可调用；对拼中的提前结束由统一裁决完成解锁。 */
    public static void closed(LivingEntity owner){
        if(!active(owner))fields.remove(owner.getUUID());
        if(owner.level() instanceof net.minecraft.server.level.ServerLevel level)character.domain.DomainTerrain.closeOwner(level,owner.getUUID());
    }
    private static boolean live(Field f){
        LivingEntity e=f.owner;
        if(!e.isAlive()||e.isRemoved()||e.isSpectator()||e.level()!=f.level)return false;
        if(f.lifecycle!=null)return f.lifecycle.active();
        if(e instanceof NpcBridge.NpcActor a)return a.mob.level()==f.level&&hasDomain(a);
        if(e instanceof MaidBridge.Actor a)return a.maid.level()==f.level&&!EternalLove.settling(a.maid)&&hasDomain(a);
        if(e instanceof ServerPlayer p)return p.server.getPlayerList().getPlayer(p.getUUID())==p&&hasDomain(p);
        if(e instanceof YujiEntity y)return y.clashDomainActive();
        return e instanceof GuardianEntity g&&g.clashDomainActive()||e instanceof GojoEntity gojo&&gojo.clashDomainActive();
    }
    public static boolean hasDomain(ServerPlayer p){Field custom=fields.get(p.getUUID());if(custom!=null&&custom.lifecycle!=null&&custom.lifecycle.active())return true;return Skills.clashDomainActive(p)||GuardianPlayerSkills.sustaining(p)||GojoCombat.sustaining(p)||BloodDomain.active(p)||character.jogo.JogoCombat.inDomain(p)||character.ryu.RyuDomain.active(p)||character.naoya.NaoyaDomain.active(p)||character.higuruma.HigurumaDomain.active(p)||character.domainking.SelfEmbryoDomain.active(p)||character.yuta.YutaDomain.active(p)||character.yuki.YukiDomain.active(p)||character.hakari.HakariDomain.active(p);}
    /** 使用登记时的固定球心与真实半径，而不是施术者当前站位。 */
    public static boolean inEnemyDomain(LivingEntity p){return fields.values().stream().anyMatch(f->live(f)&&f.level==p.level()&&!f.owner.getUUID().equals(p.getUUID())&&p.position().distanceToSqr(f.center)<=f.radius*f.radius&&(f.owner instanceof ServerPlayer caster?CombatSettings.canHit(caster,p instanceof ServerPlayer sp?MaidBridge.body(sp):p):f.owner instanceof Mob mob&&BossTargets.hostile(mob,p instanceof ServerPlayer sp?MaidBridge.body(sp):p)));}
    private static final ThreadLocal<Set<UUID>> COUNTER_CAST=ThreadLocal.withInitial(HashSet::new);
    public static boolean counterCasting(LivingEntity owner){return COUNTER_CAST.get().contains(owner.getUUID());}
    /** 仅在领域起手的同步调用栈内放行领域压制，不解除普通控制/熔断。 */
    public static <T> T counterCast(LivingEntity owner,java.util.function.Supplier<T> cast){
        boolean added=COUNTER_CAST.get().add(owner.getUUID());
        try{return cast.get();}finally{if(added)COUNTER_CAST.get().remove(owner.getUUID());}
    }
    public static boolean domainCapable(LivingEntity e){if(e instanceof ServerPlayer p)return hasDomain(p)||character.mainline.Mainline.domainUnlocked(p);if(e instanceof Mob mob&&NpcBridge.supports(mob))return character.npc.NpcCombatCasting.skills(mob).stream().anyMatch(id->character.mainline.Mainline.domainSkill(NpcBridge.character(mob),id));return e instanceof GojoEntity||e instanceof YujiEntity y&&!y.decoy()&&!y.familyMember()||e instanceof GuardianEntity g&&!g.friendly();}
    private static boolean hakari(LivingEntity owner){return owner instanceof ServerPlayer p&&character.CharacterService.hakari(p);}
    private static boolean enemies(Field a,Field b){
        if(a.owner.level()!=b.owner.level())return false;
        if(a.owner instanceof ServerPlayer p&&b.owner instanceof ServerPlayer q)
            return player(p)&&player(q)?CombatSettings.canHit(p,q)&&CombatSettings.canHit(q,p):CombatSettings.canHit(p,MaidBridge.body(q))||CombatSettings.canHit(q,MaidBridge.body(p));
        ServerPlayer p=a.owner instanceof ServerPlayer v?v:b.owner instanceof ServerPlayer v?v:null;
        LivingEntity boss=p==a.owner?b.owner:a.owner;
        if(p!=null&&boss instanceof Mob mob)return !mob.isAlliedTo(MaidBridge.body(p))&&(BossTargets.hostile(mob,MaidBridge.body(p))||CombatSettings.canHit(p,boss));
        return a.owner instanceof Mob left&&b.owner instanceof Mob right&&!left.isAlliedTo(right)&&(BossTargets.hostile(left,right)||BossTargets.hostile(right,left));
    }
    private static void discover(){
        for(Field f:List.copyOf(fields.values()))if(!live(f)&&!active(f.owner)){
            fields.remove(f.owner.getUUID());
            character.domain.DomainTerrain.closeOwner((net.minecraft.server.level.ServerLevel)f.level,f.owner.getUUID());
        }
        List<Field> list=new ArrayList<>(fields.values());
        for(int i=0;i<list.size();i++)for(int j=i+1;j<list.size();j++){
            Field a=list.get(i),b=list.get(j);
            if(active(a.owner)||active(b.owner)||!live(a)||!live(b)||!enemies(a,b)||!DomainClashRules.overlaps(a.center.distanceToSqr(b.center),a.radius,b.radius))continue;
            Match m=new Match(a,b);matches.put(a.owner.getUUID(),m);matches.put(b.owner.getUUID(),m);
            updatePartition(m);prepare(m.a);prepare(m.b);sync(m);worldClash(m);
            syncWorld(((net.minecraft.server.level.ServerLevel)a.level).getServer());
            // 坐杀博徒的结界构造极简、展开极快：对撞必胜（直接判胜，不进入手速小游戏）
            if(hakari(a.owner)!=hakari(b.owner)){finish(m,hakari(a.owner)?1:-1);} // 两名秤不能按登记顺序判胜
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
        if(f.lifecycle!=null){f.lifecycle.visual(show);return;}
        if(f.owner instanceof ServerPlayer p){Skills.clashVisual(p,show);GuardianPlayerSkills.clashVisual(p,show);GojoCombat.clashVisual(p,show);}
        else if(f.owner instanceof YujiEntity y)y.clashVisual(show);else if(f.owner instanceof GuardianEntity g)g.clashVisual(show);else if(f.owner instanceof GojoEntity g)g.clashVisual(show);
    }
    private static void extend(Field f,int ticks){
        if(f.lifecycle!=null){f.lifecycle.extend(ticks);return;}
        if(f.owner instanceof ServerPlayer p){Skills.extendDomain(p,ticks);BloodDomain.extend(p,ticks);GuardianPlayerSkills.extendDomain(p,ticks);GojoCombat.extendDomain(p,ticks);character.jogo.JogoCombat.extendDomain(p,ticks);
            character.ryu.RyuDomain.extend(p,ticks);character.naoya.NaoyaDomain.extend(p,ticks);
            character.higuruma.HigurumaDomain.extend(p,ticks);character.domainking.SelfEmbryoDomain.extend(p,ticks);
            character.yuta.YutaDomain.extend(p,ticks);character.yuki.YukiDomain.extend(p,ticks);
            character.hakari.HakariDomain.extend(p,ticks);}
        else if(f.owner instanceof YujiEntity y)y.extendDomain(ticks);else if(f.owner instanceof GuardianEntity g)g.extendDomain(ticks);else if(f.owner instanceof GojoEntity g)g.extendDomain(ticks);
    }
    private static void close(Field f){close(f,"领域对拼落败");}
    private static void close(Field f,String reason){
        if(f.lifecycle!=null){f.lifecycle.close();return;}
        if(f.owner instanceof ServerPlayer p){Skills.closeDomain(p);BloodDomain.close(p);GuardianPlayerSkills.closeOwnedDomain(p,false);GojoCombat.closeDomain(p);character.jogo.JogoCombat.closeDomain(p);
            if(character.ryu.RyuDomain.isOwner(p))character.ryu.RyuDomain.close(p,reason);
            if(character.naoya.NaoyaDomain.isOwner(p))character.naoya.NaoyaDomain.close(p,reason);
            if(character.higuruma.HigurumaDomain.active(p))character.higuruma.HigurumaDomain.close(p,reason);
            if(character.domainking.SelfEmbryoDomain.active(p))character.domainking.SelfEmbryoDomain.close(p,reason);
            if(character.yuta.YutaDomain.active(p))character.yuta.YutaDomain.close(p,reason);
            if(character.yuki.YukiDomain.active(p))character.yuki.YukiDomain.close(p,reason);
            if(character.hakari.HakariDomain.active(p))character.hakari.HakariDomain.close(p,reason);}
        else if(f.owner instanceof YujiEntity y)y.clashCloseDomain();else if(f.owner instanceof GuardianEntity g)g.clashCloseDomain();else if(f.owner instanceof GojoEntity g)g.clashCloseDomain();
    }
    /** 主动撤回：只关闭请求者自己的领域，胜负统一留到本刻裁决；双方同刻撤回是中止。 */
    public static boolean withdraw(LivingEntity owner){
        var field=fields.get(owner.getUUID());if(field==null)return false;
        close(field,"主动收起");character.domain.DomainTerrain.closeOwner((net.minecraft.server.level.ServerLevel)field.level,owner.getUUID());
        if(!active(owner)){fields.remove(owner.getUUID());syncWorld(((net.minecraft.server.level.ServerLevel)field.level).getServer());}
        return true;
    }
    public static void withdraw(ServerPlayer p,UUID match){
        var m=matches.get(p.getUUID());if(m!=null&&m.id.equals(match)&&player(p))withdraw(p);
    }
    public static void press(ServerPlayer p,UUID id,boolean down){
        Match m=matches.get(p.getUUID());if(m==null||!m.id.equals(id))return;
        Side s=m.a.field.owner==p?m.a:m.b;if(s.field.owner!=p||!live(s.field))return;
        s.input.accept(down,m.age>=DomainClashRules.READY_TICKS,p.level().getGameTime());
    }
    private static int clicks(Side s,int age){
        if(player(s.field.owner))return s.input.drain();
        if(s.field.owner instanceof MaidBridge.Actor){s.cps=5;s.credit+=s.cps;int n=s.credit/20;s.credit%=20;return n;}
        if((age-DomainClashRules.READY_TICKS)%20==0)s.cps=DomainClashRules.bossCps(s.field.owner instanceof GuardianEntity,s.field.owner instanceof GojoEntity,s.field.owner.getRandom().nextInt(100000));
        s.credit+=s.cps;int n=s.credit/20;s.credit%=20;return n;
    }
    /** 在世界推进和各角色的到期检查之前暂停绝对截止时间，不能等 END 判活之后才补时。 */
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void pauseTimers(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.START)return;
        for(Match m:new HashSet<>(matches.values()))for(Side side:List.of(m.a,m.b))
            if(side.field.lifecycle!=null||side.field.owner instanceof ServerPlayer)extend(side.field,1);
        // 旧版实体领域使用剩余刻数，在自己的 tickDomain 中暂停递减，不在这里重复补偿。
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;discover();
        for(Match m:new HashSet<>(matches.values())){
            boolean a=live(m.a.field),b=live(m.b.field);
            if(!a||!b){finish(m,a&&!b?1:b&&!a?-1:0);continue;}
            if(!enemies(m.a.field,m.b.field)){finish(m,0);continue;}
            pin(m.a.field.owner,m.a.anchor);pin(m.b.field.owner,m.b.anchor);
            if(m.age>=DomainClashRules.READY_TICKS){m.lead+=clicks(m.a,m.age)-clicks(m.b,m.age);int winner=DomainClashRules.winner(m.lead);if(winner!=0){finish(m,winner);continue;}}
            if(++m.age>=DomainClashRules.READY_TICKS+m.limit()){
                finish(m,DomainClashRules.timeoutWinner(m.lead,m.a.field.owner.getRandom().nextBoolean()));continue;
            }
            sync(m);
            if(event.getServer().getTickCount()%5==0){updatePartition(m);worldClash(m);}
        }
        if(event.getServer().getTickCount()%5==0)syncWorld(event.getServer());
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
        else {LivingEntity e=f.owner;var t=e.getPersistentData();t.putLong("NpcLegacyPenaltyUntil",e.level().getGameTime()+DomainClashRules.BURNOUT_TICKS);t.putLong("NpcLegacyBurnout",e.level().getGameTime()+DomainClashRules.BURNOUT_TICKS);t.putLong("DomainClashStunUntil",e.level().getGameTime()+DomainClashRules.STUN_TICKS);t.putDouble("ClashAnchorX",e.getX());t.putDouble("ClashAnchorY",e.getY());t.putDouble("ClashAnchorZ",e.getZ());if(e instanceof YujiEntity y)y.clashInterrupt();else if(e instanceof GuardianEntity g)g.clashInterrupt();}
    }
    private static void finish(Match m,int winner){
        if(m.finishing)return;m.finishing=true;
        // 关闭时仍标记为对拼，禁止败者虚空领域触发闭合爆发。
        if(winner==0){close(m.a.field,"领域对拼中止");close(m.b.field,"领域对拼中止");}
        else {
            Field won=winner>0?m.a.field:m.b.field;
            // 必须在解除对拼标记之前续时；临界到期的领域仍受暂停保护，不能先 live() 再漏掉奖励。
            extend(won,DomainClashRules.EXTEND_TICKS);
            Field lost=winner>0?m.b.field:m.a.field;close(lost);defeat(lost);
        }
        matches.remove(m.a.field.owner.getUUID());matches.remove(m.b.field.owner.getUUID());
        m.bar.removeAllPlayers();
        for(Side side:List.of(m.a,m.b)){
            if(side.field.level instanceof net.minecraft.server.level.ServerLevel level){
                Vec3 at=body(side.field.owner).position().add(0,1,0);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,at.x,at.y,at.z,24,1.5,1.5,1.5,.08);
                for(ServerPlayer viewer:level.players())if(viewer.position().distanceToSqr(at)<64*64)
                    viewer.displayClientMessage(Component.literal(winner==0?"领域对拼结束 · 双方解除":
                        "领域对拼结束 · "+body((winner>0?m.a:m.b).field.owner).getDisplayName().getString()+"获胜"),true);
            }
        }
        clashCallback(m.a,m.b,false,m);clashCallback(m.b,m.a,false,m);
        restoreGravity(m.a.field.owner);restoreGravity(m.b.field.owner);
        for(Side side:List.of(m.a,m.b)){
            var level=(net.minecraft.server.level.ServerLevel)side.field.level;
            if(winner==0||side==(winner>0?m.b:m.a)){
                fields.remove(side.field.owner.getUUID());
                character.domain.DomainTerrain.closeOwner(level,side.field.owner.getUUID());
            }else character.domain.DomainTerrain.refresh(level,side.field.owner.getUUID());
        }
        if(m.a.field.lifecycle!=null)m.a.field.lifecycle.resolved(winner==0?Outcome.CANCELLED:winner>0?Outcome.WON:Outcome.LOST);
        if(m.b.field.lifecycle!=null)m.b.field.lifecycle.resolved(winner==0?Outcome.CANCELLED:winner<0?Outcome.WON:Outcome.LOST);
        result(m.a,m.id,winner);result(m.b,m.id,-winner);
        fields.values().removeIf(f->!live(f));
        // 先撤销客户端裁切，再发完整领域重现包；否则重现包会被旧对拼快照当成越界心跳丢弃。
        syncWorld(((net.minecraft.server.level.ServerLevel)m.a.field.level).getServer());
        if(winner!=0){Field won=winner>0?m.a.field:m.b.field;if(live(won))visual(won,true);}
    }
    private static void clashCallback(Side self,Side other,boolean active,Match m){
        if(self.field.lifecycle!=null)self.field.lifecycle.clash(new ClashView(active,other.field.center,
            DomainClashRules.percent(self==m.a?m.lead:-m.lead)/100f,Math.max(0,DomainClashRules.READY_TICKS+m.limit()-m.age)));
    }
    private static void worldClash(Match m){
        int remain=Math.max(0,DomainClashRules.READY_TICKS+m.limit()-m.age);
        m.bar.setName(Component.literal("领域对拼 · "+body(m.a.field.owner).getDisplayName().getString()+" ↔ "
            +body(m.b.field.owner).getDisplayName().getString()+" · "+((remain+19)/20)+"秒"));
        m.bar.setProgress(DomainClashRules.percent(m.lead)/100f);
        var center=m.a.field.center.lerp(m.b.field.center,.5);
        double range=Math.max(m.a.field.radius,m.b.field.radius)+64;
        var level=(net.minecraft.server.level.ServerLevel)m.a.field.level;
        for(ServerPlayer p:List.copyOf(m.bar.getPlayers()))if(p.level()!=level||p.position().distanceToSqr(center)>range*range)m.bar.removePlayer(p);
        for(ServerPlayer p:level.players())if(p.position().distanceToSqr(center)<=range*range)m.bar.addPlayer(p);
        clashCallback(m.a,m.b,true,m);clashCallback(m.b,m.a,true,m);
    }
    private static void syncWorld(net.minecraft.server.MinecraftServer server){
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            List<character.domain.DomainPresentation.Frame> frames=new ArrayList<>();
            var nearby=fields.values().stream().filter(f->f.level==p.level()&&live(f)&&p.position().distanceToSqr(f.center)<Math.pow(f.radius+128,2))
                .sorted(Comparator.comparingDouble(f->p.position().distanceToSqr(f.center))).limit(64).toList();
            for(Field f:nearby){
                Match m=matches.get(f.owner.getUUID());Vec3 plane=f.center,normal=Vec3.ZERO;int phase=0,remaining=0;float progress=.5f;
                if(m!=null){
                    var cut=partition(f.owner.getUUID());plane=cut.plane();normal=cut.normal();
                    progress=DomainClashRules.percent(m.a.field==f?m.lead:-m.lead)/100f;
                    phase=m.age<DomainClashRules.READY_TICKS?1:2;remaining=Math.max(0,DomainClashRules.READY_TICKS+m.limit()-m.age);
                }
                var style=m==null?f.style:character.domain.DomainPresentation.clashStyle(f.owner,f.style);
                if(m!=null&&m.b.field==f&&m.a.field.style.color()==style.color())
                    style=new character.domain.DomainPresentation.Style(style.texture(),0xFF9966,style.yaw(),style.spin(),style.cameraSky());
                frames.add(new character.domain.DomainPresentation.Frame(f.owner.getUUID(),f.center,f.radius,style,phase,progress,plane,normal,remaining));
            }
            character.domain.DomainPresentation.send(p,frames);
        }
    }
    private static void result(Side s,UUID id,int winner){if(player(s.field.owner)&&s.field.owner instanceof ServerPlayer p){if(winner>0&&character.CharacterService.selected(p)!=null&&"jogo".equals(character.CharacterService.selected(p).id()))character.jogo.JogoCombat.onClashWon(p);
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Network.ClashState(id,false,DomainClashRules.percent(winner*25),0,0,0,"",winner));p.sendSystemMessage(Component.literal(winner>0?"领域对拼获胜：本方领域完全展开，剩余时间额外延长30秒。":winner<0?"领域对拼失败，领域已解除。":"领域对拼中止：双方领域解除，不追加对拼败北惩罚。"));}}
    private static void sync(Match m){sync(m,m.a,m.b,m.lead);sync(m,m.b,m.a,-m.lead);}
    private static void sync(Match m,Side self,Side other,int lead){if(player(self.field.owner)&&self.field.owner instanceof ServerPlayer p){String name=body(other.field.owner).getDisplayName().getString();if(name.length()>128)name=name.substring(0,128);Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Network.ClashState(m.id,true,DomainClashRules.percent(lead),Math.max(0,DomainClashRules.READY_TICKS-m.age),Math.max(0,DomainClashRules.READY_TICKS+m.limit()-m.age),other.cps,name,0));}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(LivingAttackEvent event){
        if(event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        Entity source=event.getSource().getEntity();ServerPlayer caster=CombatSettings.caster(event.getSource());
        if(active(event.getEntity())||source instanceof LivingEntity e&&(active(e)||stunned(e))||caster!=null&&active(caster))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void heal(LivingHealEvent event){
        ServerPlayer p=MaidBridge.progression(event.getEntity());
        if(p==null){var mob=event.getEntity();if(mob.getPersistentData().getLong("NpcLegacyPenaltyUntil")>mob.level().getGameTime()
            &&event.getAmount()>0&&mob.getHealth()<mob.getMaxHealth()&&!NpcCurse.spend(mob,DomainClashRules.healCost(NpcCurse.max(mob))))event.setCanceled(true);return;}
        if(!penalized(p)||event.getAmount()<=0||p.getHealth()>=p.getMaxHealth())return;
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
