package character.jogo;

import character.CharacterService;
import character.CharacterSkill;
import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

/** 漏壶的三招实现、被动免疫与第一章「泡岩浆 30 秒」。全部在服务端结算，客户端只看粒子。 */
public final class JogoCombat {
    private static final String DONE="JogoLavaDone", PROGRESS="JogoLavaTicks", BUCKETS="JogoLavaBuckets", SIEGE="JogoSiegeTicks", SIEGE_DONE="JogoSiegeDone";
    private static final int SOAK_TICKS=600;   // 30 秒
    private static final int SIEGE_TICKS=1200; // 盖棺铁围山：坚持 60 秒
    private static final float SIEGE_HEALTH=.30f;  // 生命低于 30%
    private static final int SIEGE_ENEMIES=5, SIEGE_RADIUS=24;
    private static final int BUG_LIFE=400;     // 火砾虫 20 秒
    private static final double BUG_SPEED=0.55;

    private record Bug(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir, int life) {}
    private record Meteor(ServerPlayer owner, Vec3 target, int delay) {}
    private record Pillar(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir, float damage, Set<UUID> hit) {}
    private record Falling(ServerLevel level, FallingBlockEntity block, UUID owner, float damage) {}
    private static final List<Bug> BUGS=new ArrayList<>();
    private static final List<Meteor> METEORS=new ArrayList<>();
    private static final List<Falling> FALLING=new ArrayList<>();
    private static final List<Pillar> PILLARS=new ArrayList<>();
    /** 盖棺铁围山：封闭式火山领域，持续 30 秒，界内敌人被高温烘烤、漏壶冷却减半。 */
    private record Domain(ServerPlayer owner, Vec3 center, int end, double radius, Map<UUID,Vec3> captives) {}
    private static final List<Domain> DOMAINS=new ArrayList<>();
    public static final int DOMAIN_RADIUS=100, DOMAIN_LIFE=600;   // 用户指定：半径 100 格
    private static final double DOMAIN_INSET=0.35;               // 碰撞箱留出的安全余量

    /** 章节进度：一章=泡岩浆满 30 秒；二章=盖棺铁围山坚持 60 秒。 */
    public static int rank(ServerPlayer p) {
        var data=Curse.data(p);
        if(!data.getBoolean(DONE))return 0;
        return data.getBoolean(SIEGE_DONE)?2:1;
    }

    // ---------------------------------------------------------------- 技能 1：火砾虫
    public static void castBugs(ServerPlayer p,CharacterSkill skill,int rank) {
        int count=Math.min(10,1+rank*3);
        Vec3 look=p.getLookAngle(),start=p.getEyePosition();
        for(int i=0;i<count;i++){
            double spread=(i-(count-1)/2.0)*0.07;
            Vec3 dir=look.add(p.getLookAngle().cross(new Vec3(0,1,0)).normalize().scale(spread)).normalize();
            BUGS.add(new Bug(p.serverLevel(),p.getUUID(),start.add(dir.scale(0.8)).add(0,-0.2,0),dir,BUG_LIFE));
        }
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.BLAZE_SHOOT,SoundSource.PLAYERS,1f,1.2f);
    }

    // ---------------------------------------------------------------- 技能 2：陨石
    public static void castMeteors(ServerPlayer p,CharacterSkill skill,int rank) {
        int count=Math.min(6,2+rank*2);
        Vec3 target=aimPoint(p,32);
        for(int i=0;i<count;i++)METEORS.add(new Meteor(p,target.add((p.getRandom().nextDouble()-.5)*6,0,(p.getRandom().nextDouble()-.5)*6),i*8));
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.AMBIENT_UNDERWATER_LOOP,SoundSource.PLAYERS,0.4f,0.5f);
    }

    // ---------------------------------------------------------------- 技能 3：火焰喷发
    /** 火焰喷发：每刻前进 1.6 格的快速岩浆柱，8 刻飞完 12 格；沿途命中即结算。 */
    public static void castPillars(ServerPlayer p,CharacterSkill skill,int rank) {
        int count=Math.min(15,1+rank*5);
        Vec3 start=p.getEyePosition(),look=p.getLookAngle();
        Vec3 side=look.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
        float damage=CombatSettings.spellDamage(p,skill.damageAt(rank));
        for(int i=0;i<count;i++){
            double angle=Math.toRadians(-22+44.0*(count==1?0.5:i/(double)(count-1)));
            Vec3 dir=look.scale(Math.cos(angle)).add(side.scale(Math.sin(angle))).normalize();
            PILLARS.add(new Pillar(p.serverLevel(),p.getUUID(),start.add(dir.scale(0.6)),dir,damage,new HashSet<>()));
        }
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.FIRECHARGE_USE,SoundSource.PLAYERS,1.2f,0.8f);
    }

    // ---------------------------------------------------------------- 技能 4：领域展开·盖棺铁围山
    public static void castDomain(ServerPlayer p,CharacterSkill skill,int rank) {
        Vec3 center=p.position();
        DOMAINS.removeIf(d->d.owner()==p);
        var captives=new HashMap<UUID,Vec3>();
        for(var e:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(center,center).inflate(DOMAIN_RADIUS),
                t->t.isAlive()&&!t.isSpectator()&&t.position().distanceToSqr(center)<=DOMAIN_RADIUS*DOMAIN_RADIUS)){
            captives.put(e.getUUID(),e.position());
        }
        DOMAINS.add(new Domain(p,center,p.getServer().getTickCount()+DOMAIN_LIFE,DOMAIN_RADIUS,captives));
        JogoTerrain.open(p,center,DOMAIN_RADIUS);
        Network.send(p.serverLevel(),new Network.Effect(JogoFx.DOMAIN,p.getId(),center,Vec3.ZERO,(float)DOMAIN_RADIUS,DOMAIN_LIFE,0));
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,1.6f,0.45f);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("领域展开 · 盖棺铁围山"));
    }
    /** 漏壶是否正站在自己的领域里（冷却减半用）。 */
    public static boolean inDomain(ServerPlayer p) {
        for(Domain d:DOMAINS)if(d.owner()==p)return true;
        return false;
    }
    private static void tickDomains() {
        if(DOMAINS.isEmpty())return;
        int now=DOMAINS.get(0).owner().getServer().getTickCount();
        for(Iterator<Domain> it=DOMAINS.iterator();it.hasNext();){
            Domain d=it.next();ServerPlayer owner=d.owner();
            if(owner==null||!owner.isAlive()||owner.level()!=owner.serverLevel()||owner.serverLevel().getGameTime()<=0){it.remove();if(owner!=null)JogoTerrain.restore(owner);continue;}
            if(owner.getServer().getTickCount()>=d.end()){JogoTerrain.restore(owner);it.remove();continue;}
            ServerLevel level=owner.serverLevel();
            // 封闭：闭合时被抓到的活物越界就拉回最近一次安全位置（碰撞箱留余量）
            double limit=d.radius()-DOMAIN_INSET;
            for(var it2=d.captives().entrySet().iterator();it2.hasNext();){
                var entry=it2.next();var raw=level.getEntity(entry.getKey());
                if(!(raw instanceof LivingEntity living)||!living.isAlive()||living.isSpectator()){it2.remove();continue;}
                double dx=living.getX()-d.center().x,dy=living.getY()-d.center().y,dz=living.getZ()-d.center().z;
                if(dx*dx+dy*dy+dz*dz<=limit*limit){entry.setValue(living.position());continue;}
                Vec3 back=entry.getValue();
                if(living instanceof ServerPlayer sp)sp.connection.teleport(back.x,back.y,back.z,sp.getYRot(),sp.getXRot());
                else living.teleportTo(back.x,back.y,back.z);
                living.setDeltaMovement(Vec3.ZERO);
                living.hurtMarked=true;
            }
            if(owner.getServer().getTickCount()%10!=0)continue;
            float damage=CombatSettings.spellDamage(owner,6);
            for(var target:level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(d.center(),d.center()).inflate(d.radius()),
                    t->t!=owner&&t.isAlive()&&t.position().distanceToSqr(d.center())<=d.radius()*d.radius()&&CombatSettings.canHit(owner,t))){
                // 有火抗或飞在空中（不落地）的敌人不吃持续烘烤
                if(target.fireImmune()||!target.onGround())continue;
                AttackTypes.mark(AttackTypes.MAGIC);
                try{CombatSettings.hurt(owner,target,owner.damageSources().indirectMagic(owner,owner),damage);}
                finally{AttackTypes.clear();}
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),40));
            }
        }
    }

    // ---------------------------------------------------------------- 每刻推进：火砾虫 / 陨石 / 坠落物
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        tickBugs();tickMeteors();tickFalling();tickPillars();tickDomains();
    }
    private static void tickBugs() {
        for(Iterator<Bug> it=BUGS.iterator();it.hasNext();){
            Bug bug=it.next();Vec3 next=bug.pos().add(bug.dir().scale(BUG_SPEED));
            ServerLevel level=bug.level();int life=bug.life()-1;
            Network.send(level,new Network.Effect(JogoFx.BUG,0,bug.pos(),Vec3.ZERO,1,6,0));
            var hit=level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(next,next).inflate(1.2),
                t->t.getUUID().equals(bug.owner())?false:t instanceof ServerPlayer sp?CombatSettings.canHit(level.getServer().getPlayerList().getPlayer(bug.owner()),t):true);
            boolean blocked=!level.getBlockState(BlockPos.containing(next)).isAir();
            if(life<=0||blocked||!hit.isEmpty()){
                explode(level,bug.owner(),next);it.remove();continue;
            }
            BUGS.set(BUGS.indexOf(bug),new Bug(level,bug.owner(),next,bug.dir(),life));
        }
    }
    private static void explode(ServerLevel level,UUID owner,Vec3 at) {
        Network.send(level,new Network.Effect(JogoFx.BUG_BURST,0,at,Vec3.ZERO,3,10,0));
        ServerPlayer caster=level.getServer().getPlayerList().getPlayer(owner);if(caster==null)return;
        float damage=CombatSettings.spellDamage(caster,8);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(at,at).inflate(3),t->CombatSettings.canHit(caster,t))){
            AttackTypes.mark(AttackTypes.MAGIC);
            try{CombatSettings.hurt(caster,target,caster.damageSources().indirectMagic(caster,caster),damage);}
            finally{AttackTypes.clear();}
            target.setSecondsOnFire(2);
        }
    }
    private static void tickMeteors() {
        for(Iterator<Meteor> it=METEORS.iterator();it.hasNext();){
            Meteor meteor=it.next();
            if(meteor.owner().level()!=meteor.owner().serverLevel()||!meteor.owner().isAlive()){it.remove();continue;}
            if(meteor.delay()>0){METEORS.set(METEORS.indexOf(meteor),new Meteor(meteor.owner(),meteor.target(),meteor.delay()-1));continue;}
            ServerLevel level=meteor.owner().serverLevel();
            var block=FallingBlockEntity.fall(level,BlockPos.containing(meteor.target().x,meteor.target().y+28,meteor.target().z),Blocks.OBSIDIAN.defaultBlockState());
            block.setDeltaMovement(0,-1.6,0);block.dropItem=false;
            level.addFreshEntity(block);
            FALLING.add(new Falling(level,block,meteor.owner().getUUID(),CombatSettings.spellDamage(meteor.owner(),24)));
            it.remove();
        }
    }
    private static void tickFalling() {
        for(Iterator<Falling> it=FALLING.iterator();it.hasNext();){
            Falling f=it.next();var block=f.block();
            if(block.isRemoved()){impact(f.level(),f.owner(),block.position(),f.damage());it.remove();continue;}
            var at=block.position();
            Network.send(f.level(),new Network.Effect(JogoFx.METEOR_TRAIL,0,at,Vec3.ZERO,1.5f,8,0));
            if(!block.onGround()&&at.y>f.level().getMinBuildHeight()+2&&!block.verticalCollision)continue;
            impact(f.level(),f.owner(),at,f.damage());block.discard();it.remove();
        }
    }
    private static void impact(ServerLevel level,UUID owner,Vec3 at,float damage) {
        Network.send(level,new Network.Effect(JogoFx.METEOR_IMPACT,0,at,Vec3.ZERO,5f,14,0));
        level.playSound(null,BlockPos.containing(at),SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,1.4f,0.7f);
        ServerPlayer caster=level.getServer().getPlayerList().getPlayer(owner);if(caster==null)return;
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(at,at).inflate(4.5),t->CombatSettings.canHit(caster,t))){
            AttackTypes.mark(AttackTypes.MAGIC);
            try{CombatSettings.hurt(caster,target,caster.damageSources().indirectMagic(caster,caster),damage);}
            finally{AttackTypes.clear();}
        }
    }
    private static void tickPillars() {
        for(Iterator<Pillar> it=PILLARS.iterator();it.hasNext();){
            Pillar pillar=it.next();
            Vec3 next=pillar.pos().add(pillar.dir().scale(1.6));
            ServerLevel level=pillar.level();
            Network.send(level,new Network.Effect(JogoFx.PILLAR,0,next,pillar.dir(),1,6,0));
            ServerPlayer caster=level.getServer().getPlayerList().getPlayer(pillar.owner());
            if(caster!=null)for(var target:level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(pillar.pos(),next).inflate(1.5),
                    t->CombatSettings.canHit(caster,t)&&pillar.hit().add(t.getUUID()))){
                AttackTypes.mark(AttackTypes.MAGIC);
                try{CombatSettings.hurt(caster,target,caster.damageSources().indirectMagic(caster,caster),pillar.damage());}
                finally{AttackTypes.clear();}
                target.setSecondsOnFire(3);
            }
            PILLARS.set(PILLARS.indexOf(pillar),new Pillar(level,pillar.owner(),next,pillar.dir(),pillar.damage(),pillar.hit()));
            if(!pillarFlies(next,pillar.dir(),level))it.remove();
        }
    }
    private static boolean pillarFlies(Vec3 at,Vec3 dir,ServerLevel level) {
        return level.getBlockState(BlockPos.containing(at)).isAir()&&at.y>level.getMinBuildHeight()+1;
    }
    private static Vec3 aimPoint(ServerPlayer p,double range) {
        Vec3 start=p.getEyePosition(),end=start.add(p.getLookAngle().scale(range));
        var hit=p.level().clip(new net.minecraft.world.level.ClipContext(start,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        return hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS?end:hit.getLocation();
    }

    // ---------------------------------------------------------------- 被动：免疫火焰与岩浆；第一章泡岩浆
    @SubscribeEvent public static void immune(LivingDamageEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        var d=CharacterService.selected(p);if(d==null||!d.id().equals("jogo"))return;
        var source=e.getSource();
        if(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypeTags.IS_EXPLOSION)&&source.is(DamageTypeTags.IS_FIRE)||source.is(net.minecraft.world.damagesource.DamageTypes.LAVA)||source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)||source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)||source.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR)){
            e.setCanceled(true);
        }
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        var d=CharacterService.selected(p);if(d==null||!d.id().equals("jogo"))return;
        var data=Curse.data(p);
        // 不再直接发岩浆：玩家要自己去弄到 3 桶岩浆（记录曾经同时持有过的最大桶数）。
        int buckets=p.getInventory().countItem(net.minecraft.world.item.Items.LAVA_BUCKET);
        if(buckets>data.getInt(BUCKETS))data.putInt(BUCKETS,buckets);
        if(!data.getBoolean(DONE)){
        boolean lava=p.isInLava()||p.level().getFluidState(p.blockPosition()).is(net.minecraft.tags.FluidTags.LAVA);
        if(!lava)return;
        int ticks=data.getInt(PROGRESS)+1;data.putInt(PROGRESS,ticks);
        if(ticks%100==0)p.displayClientMessage(net.minecraft.network.chat.Component.literal("岩浆浸泡 "+ticks/20+" / 30 秒 · 岩浆桶 "+data.getInt(BUCKETS)+" / 3"),true);
        if(ticks>=SOAK_TICKS&&data.getInt(BUCKETS)>=3){
            data.putBoolean(DONE,true);
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("第一章·于长夜重返大地 完成"));
            CharacterRewardsHook.grant(p);
        }
        return;
        }
        if(data.getBoolean(SIEGE_DONE))return;
        boolean hurt=p.getHealth()<p.getMaxHealth()*SIEGE_HEALTH;
        int nearby=hurt?enemies(p):0;
        boolean burning=p.isOnFire()||p.isInLava();
        if(!burning||nearby<SIEGE_ENEMIES)return;
        int siege=data.getInt(SIEGE)+1;data.putInt(SIEGE,siege);
        if(siege%100==0)p.displayClientMessage(net.minecraft.network.chat.Component.literal("盖棺铁围山 "+siege/20+" / 60 秒 · 敌人 "+nearby),true);
        if(siege>=SIEGE_TICKS){
            data.putBoolean(SIEGE_DONE,true);
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("第二章·盖棺铁围山 完成"));
            CharacterRewardsHook.grant(p);
        }
    }
    /** 周围 24 格内存活的敌对生物数量（不数玩家、不数自己的召唤物）。 */
    private static int enemies(ServerPlayer p) {
        int count=0;
        for(var e:p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(SIEGE_RADIUS),t->t.isAlive()&&t instanceof net.minecraft.world.entity.monster.Enemy&&t!=p)){
            if(CombatSafety.canAffect(p,e))count++;
        }
        return count;
    }
    /** 只在本类内部用来触发发奖，避免循环依赖写死 CharacterRewards 的调用顺序。 */
    static final class CharacterRewardsHook {
        static void grant(ServerPlayer p) { character.CharacterRewards.grant(p); }
    }

    private JogoCombat() {}
}
