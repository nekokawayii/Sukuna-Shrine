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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

/** 漏壶的三招实现、被动免疫与第一章「泡岩浆 30 秒」。全部在服务端结算，客户端只看粒子。 */
public final class JogoCombat {
    private static final String DONE="JogoLavaDone", PROGRESS="JogoLavaTicks", BUCKETS="JogoLavaBuckets",
            SIEGE="JogoNetherTicks", SIEGE_DONE="JogoNetherDone",
            CLASH_DONE="JogoClashDone", CLASH_AT="JogoClashAt", GOJO_KILL="JogoGojoKilled", GOJO_DONE="JogoGojoDone",
            KEY_GIVEN="JogoVoidKeyGiven", ASH_CRIT="JogoAshCrit";
    private static final int NETHER_TICKS=1200, GOJO_SURVIVE=3600;
    private static final int SOAK_TICKS=600;   // 30 秒
    private static final int SIEGE_TICKS=1200; // 盖棺铁围山：坚持 60 秒
    private static final float SIEGE_HEALTH=.30f;  // 生命低于 30%
    private static final int SIEGE_ENEMIES=5, SIEGE_RADIUS=24;
    private static final int BUG_LIFE=400;     // 火砾虫 20 秒
    private static final int PILLAR_LIFE=300;  // 火焰喷发岩浆柱 15 秒（用户指定）
    private static final double BUG_SPEED=0.55;

    private record Bug(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir, int life) {}
    private record Meteor(ServerPlayer owner, Vec3 target, int delay) {}
    private record Pillar(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir, float damage, Set<UUID> hit, int ticks) {}
    private record Falling(ServerLevel level, FallingBlockEntity block, UUID owner, float damage) {}
    private static final List<Bug> BUGS=new ArrayList<>();
    private static final List<Meteor> METEORS=new ArrayList<>();
    private static final List<Falling> FALLING=new ArrayList<>();
    private static final List<Pillar> PILLARS=new ArrayList<>();
    /** 火焰喷发：持续 15 秒不断向前飞的扇形火焰。 */
    private record Fan(ServerLevel level,UUID owner,Vec3 dir,float damage,int end,int wave) {}
    /** 火山喷发：一条粗大的火柱，向前飞 40 格。 */
    private record Column(ServerLevel level,UUID owner,Vec3 pos,Vec3 dir,float damage,double traveled) {}
    private static final List<Fan> FANS=new ArrayList<>();
    private static final List<Column> COLUMNS=new ArrayList<>();
    /** 盖棺铁围山：封闭式火山领域，持续 30 秒，界内敌人被高温烘烤、漏壶冷却减半。 */
    private record Domain(ServerPlayer owner, Vec3 center, int end, double radius, Map<UUID,Vec3> captives) {}
    private static final List<Domain> DOMAINS=new ArrayList<>();
    public static final int DOMAIN_RADIUS=100, DOMAIN_LIFE=600;   // 用户指定：半径 100 格
    private static final double DOMAIN_INSET=0.35;               // 碰撞箱留出的安全余量

    /** 章节进度：一章=泡岩浆满 30 秒；二章=盖棺铁围山坚持 60 秒。 */
    public static int rank(ServerPlayer p) {
        var data=Curse.data(p);
        if(!data.getBoolean(DONE))return 0;
        if(!data.getBoolean(SIEGE_DONE))return 1;
        return data.getBoolean(GOJO_DONE)?3:2;
    }
        // ---------------------------------------------------------------- 第三章解锁的三招 + 被动
    /** 火山喷发：掌心高压岩浆柱，直线贯穿 10 格并把沿途地表化为焦岩（岩浆块，8 秒后还原）。 */
    public static void castVolcanoBeam(ServerPlayer p,CharacterSkill skill,int rank) {
        COLUMNS.add(new Column(p.serverLevel(),p.getUUID(),p.getEyePosition().add(p.getLookAngle().scale(1.2)),
            p.getLookAngle(),CombatSettings.spellDamage(p,skill.damageAt(rank)),0));
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.BLAZE_SHOOT,SoundSource.PLAYERS,1.4f,0.6f);
    }

    /** 岩浆陷阱：前方地面隆起、刺出火山柱，把敌人击飞。 */
    public static void castMagmaTrap(ServerPlayer p,CharacterSkill skill,int rank) {
        ServerLevel level=p.serverLevel();Vec3 dir=p.getLookAngle();float dmg=CombatSettings.spellDamage(p,skill.damageAt(rank));
        for(int i=0;i<6;i++){
            Vec3 at=p.position().add(dir.scale(2+i*1.5));
            int x=(int)Math.floor(at.x),z=(int)Math.floor(at.z);
            int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);
            Vec3 ground=new Vec3(x+0.5,y+0.2,z+0.5);
            Network.send(level,new Network.Effect(JogoFx.PILLAR,0,ground,dir,1,8,0));
            for(var target:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    new net.minecraft.world.phys.AABB(ground,ground).inflate(2.2),t->CombatSettings.canHit(p,t)&&t!=p)){
                AttackTypes.mark(AttackTypes.MAGIC);
                try{CombatSettings.hurt(p,target,p.damageSources().indirectMagic(p,p),dmg);}
                finally{AttackTypes.clear();}
                target.setDeltaMovement(target.getDeltaMovement().add(0,0.95,0));
                target.hurtMarked=true;
                target.setSecondsOnFire(3);
            }
        }
        level.playSound(null,p.blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,1.0f,0.6f);
    }
    /** 火山灰隐身：化作灰烬消散，隐身+加速，下一次攻击必定暴击。 */
    public static void castAshVeil(ServerPlayer p,CharacterSkill skill,int rank) {
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,120,0,false,false));
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,120,2,false,false));
        Curse.data(p).putBoolean(ASH_CRIT,true);
        Network.send(p.serverLevel(),new Network.Effect(JogoFx.BUG_BURST,0,p.position(),Vec3.ZERO,2,8,0));
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("火山灰隐身：下一次攻击必定暴击"));
    }
    private static final Map<UUID,LinkedHashMap<BlockPos,BlockState>> BEAM_JOURNAL=new HashMap<>();
    private static final Map<UUID,Long> BEAM_EXPIRE=new HashMap<>();
    private static final Map<UUID,Long> PILLAR_STAMP=new HashMap<>();
    /** 盖棺铁围山：界内敌人只要受伤，脚下就窜出一根火柱（同一目标 8 刻内不重复）。 */
    @SubscribeEvent public static void domainPillar(LivingDamageEvent e) {
        LivingEntity victim=e.getEntity();if(victim instanceof ServerPlayer)return;
        if(DOMAINS.isEmpty())return;
        ServerLevel level=null;ServerPlayer owner=null;
        for(var d:DOMAINS){
            if(victim.level()!=d.owner().level())continue;
            if(victim.position().distanceToSqr(d.center())>d.radius()*d.radius())continue;
            if(victim==d.owner())continue;
            level=(ServerLevel)victim.level();owner=d.owner();break;
        }
        if(level==null||owner==null)return;
        long now=level.getGameTime();
        if(now-PILLAR_STAMP.getOrDefault(victim.getUUID(),0L)<8)return;
        PILLAR_STAMP.put(victim.getUUID(),now);
        Network.send(level,new Network.Effect(JogoFx.PILLAR,owner.getId(),victim.position(),Vec3.ZERO,1.6f,10,0));
        victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(),60));
    }

    /** 被动「火焰附着」：漏壶造成的伤害附带火焰；隐身后的第一击额外 50% 暴击伤害。 */
    @SubscribeEvent public static void jogoDamage(LivingDamageEvent e) {
        if(!(e.getSource().getEntity() instanceof ServerPlayer caster))return;
        var target=e.getEntity();if(target==caster)return;
        var d=CharacterService.selected(caster);if(d==null||!d.id().equals("jogo"))return;
        target.setSecondsOnFire(4);
        if(Curse.data(caster).getBoolean(ASH_CRIT)){
            Curse.data(caster).putBoolean(ASH_CRIT,false);
            e.setAmount(e.getAmount()*1.5f);
        }
    }
    /** 五条悟被漏壶击杀（无掉落物）→ 第三章条件之一。 */
    @SubscribeEvent public static void gojoDeath(net.minecraftforge.event.entity.living.LivingDeathEvent e) {
        if(!(e.getEntity() instanceof GojoEntity))return;
        if(!(e.getSource().getEntity() instanceof ServerPlayer p))return;
        var d=CharacterService.selected(p);if(d==null||!d.id().equals("jogo"))return;
        Curse.data(p).putBoolean(GOJO_KILL,true);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("五条悟已被击败 · 第三章节条件之一达成（无掉落物）"));
    }
    private static void tickBeam() {
        if(BEAM_EXPIRE.isEmpty())return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        for(var it=BEAM_EXPIRE.entrySet().iterator();it.hasNext();){
            var entry=it.next();
            var owner=server.getPlayerList().getPlayer(entry.getKey());
            if(owner==null)continue;
            if(owner.serverLevel().getGameTime()<entry.getValue())continue;
            var journal=BEAM_JOURNAL.remove(entry.getKey());
            if(journal!=null)for(var b:journal.entrySet())owner.serverLevel().setBlock(b.getKey(),b.getValue(),2);
            it.remove();
        }
    }

    /** 领域对拼获胜（由 DomainClash 回调）：五条悟会用茈打碎领域，算主动收回，并开始 3 分钟生存计时。 */
    public static void onClashWon(ServerPlayer p) {
        var data=Curse.data(p);
        data.putBoolean(CLASH_DONE,true);
        data.putLong(CLASH_AT,p.level().getGameTime());
        closeDomain(p);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("领域对拼获胜：五条悟以外道「茈」击碎了盖棺铁围山 · 从现在起活过 3 分钟"));
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
        float damage=CombatSettings.spellDamage(p,skill.damageAt(rank));
        FANS.add(new Fan(p.serverLevel(),p.getUUID(),p.getLookAngle(),damage,p.getServer().getTickCount()+PILLAR_LIFE,0));
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.FIRECHARGE_USE,SoundSource.PLAYERS,1.3f,0.8f);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        tickBugs();tickMeteors();tickFalling();tickFans();tickColumns();tickDomains();JogoTerrain.tick();tickBeam();
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
    /** 岩浆柱：前 8 刻飞出 12 格，之后在原地停留，**总生命周期 15 秒（300 刻）**，每 0.5 秒灼烧一次。 */
    private static void tickPillars() {
        for(Iterator<Pillar> it=PILLARS.iterator();it.hasNext();){
            Pillar pillar=it.next();
            int ticks=pillar.ticks()+1;
            ServerLevel level=pillar.level();
            Vec3 next=ticks<=8?pillar.pos().add(pillar.dir().scale(1.6)):pillar.pos();
            if(ticks%2==0)Network.send(level,new Network.Effect(JogoFx.PILLAR,0,next,pillar.dir(),1,6,0));
            if(ticks%10==0){
                pillar.hit().clear();   // 停留期允许重复灼烧
                ServerPlayer caster=level.getServer().getPlayerList().getPlayer(pillar.owner());
                if(caster!=null)for(var target:level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(next,next).inflate(1.8),
                        t->CombatSettings.canHit(caster,t)&&pillar.hit().add(t.getUUID()))){
                    AttackTypes.mark(AttackTypes.MAGIC);
                    try{CombatSettings.hurt(caster,target,caster.damageSources().indirectMagic(caster,caster),pillar.damage());}
                    finally{AttackTypes.clear();}
                    target.setSecondsOnFire(3);
                }
            }
            if(ticks>=PILLAR_LIFE||(ticks<=8&&!pillarFlies(next,pillar.dir(),level))){it.remove();continue;}
            PILLARS.set(PILLARS.indexOf(pillar),new Pillar(level,pillar.owner(),next,pillar.dir(),pillar.damage(),pillar.hit(),ticks));
        }
    }
    private static boolean pillarFlies(Vec3 at,Vec3 dir,ServerLevel level) {
        return level.getBlockState(BlockPos.containing(at)).isAir()&&at.y>level.getMinBuildHeight()+1;
    }
    /** 火焰喷发：每 2 刻向前推一波扇形火舌，持续 15 秒。 */
    private static void tickFans() {
        if(FANS.isEmpty())return;
        for(Iterator<Fan> it=FANS.iterator();it.hasNext();){
            Fan fan=it.next();ServerLevel level=fan.level();
            ServerPlayer owner=level.getServer().getPlayerList().getPlayer(fan.owner());
            if(owner==null||level.getServer().getTickCount()>=fan.end()){it.remove();continue;}
            if(level.getServer().getTickCount()%2!=0)continue;
            Vec3 side=fan.dir().cross(new Vec3(0,1,0)).normalize();
            int count=Math.max(3,Math.min(15,1+CharacterService.rank(owner)*5));
            double reach=6+(level.getServer().getTickCount()%60)*0.35;
            for(int i=0;i<count;i++){
                double angle=Math.toRadians(-22+44.0*(count==1?0.5:i/(double)(count-1)));
                Vec3 dir=fan.dir().scale(Math.cos(angle)).add(side.scale(Math.sin(angle))).normalize();
                Vec3 at=owner.getEyePosition().add(dir.scale(reach));
                Network.send(level,new Network.Effect(JogoFx.PILLAR,0,at,dir,1,6,0));
                for(var target:level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(at,at).inflate(2.2),t->CombatSettings.canHit(owner,t))){
                    AttackTypes.mark(AttackTypes.MAGIC);
                    try{CombatSettings.hurt(owner,target,owner.damageSources().indirectMagic(owner,owner),fan.damage());}
                    finally{AttackTypes.clear();}
                    target.setSecondsOnFire(3);
                }
            }
        }
    }
    /** 火山喷发：一条粗大的火柱，每刻前进 2.5 格，最远 40 格。 */
    private static void tickColumns() {
        if(COLUMNS.isEmpty())return;
        for(Iterator<Column> it=COLUMNS.iterator();it.hasNext();){
            Column c=it.next();ServerLevel level=c.level();
            ServerPlayer owner=level.getServer().getPlayerList().getPlayer(c.owner());
            if(owner==null||c.traveled()>=40){it.remove();continue;}
            Vec3 next=c.pos().add(c.dir().scale(2.5));
            Network.send(level,new Network.Effect(JogoFx.PILLAR,0,next,c.dir(),2,8,0));
            for(var target:level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(c.pos(),next).inflate(2.2),t->CombatSettings.canHit(owner,t))){
                AttackTypes.mark(AttackTypes.MAGIC);
                try{CombatSettings.hurt(owner,target,owner.damageSources().indirectMagic(owner,owner),c.damage());}
                finally{AttackTypes.clear();}
                target.setSecondsOnFire(4);
            }
            int x=(int)Math.floor(next.x),z=(int)Math.floor(next.z);
            int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);
            var pos=new BlockPos(x,y,z);
            BlockState old=level.getBlockState(pos);
            if(!old.isAir()&&!old.hasBlockEntity()&&old.getDestroySpeed(level,pos)>=0){
                BEAM_JOURNAL.computeIfAbsent(owner.getUUID(),k->new LinkedHashMap<>()).putIfAbsent(pos,old);
                level.setBlock(pos,Blocks.MAGMA_BLOCK.defaultBlockState(),2);
                BEAM_EXPIRE.putIfAbsent(owner.getUUID(),level.getGameTime()+160);
            }
            COLUMNS.set(COLUMNS.indexOf(c),new Column(level,c.owner(),next,c.dir(),c.damage(),c.traveled()+2.5));
        }
    }

        private static Vec3 aimPoint(ServerPlayer p,double range) {
        Vec3 start=p.getEyePosition(),end=start.add(p.getLookAngle().scale(range));
        var hit=p.level().clip(new net.minecraft.world.level.ClipContext(start,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        return hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS?end:hit.getLocation();
    }

    // ---------------------------------------------------------------- 技能 4：领域展开·盖棺铁围山
    /** 提前收起：解除规则、还原地形，并通知客户端移除球壳。 */
    public static boolean closeDomain(ServerPlayer p) {
        boolean removed=DOMAINS.removeIf(d->d.owner()==p);
        if(removed){
            JogoTerrain.restore(p);
            Network.send(p.serverLevel(),new Network.Effect(JogoFx.DOMAIN_END,p.getId(),p.position(),Vec3.ZERO,0,0,0));
        }
        return removed;
    }
    public static void castDomain(ServerPlayer p,CharacterSkill skill,int rank) {
        if(closeDomain(p)){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("盖棺铁围山 · 收"));return;}
        Vec3 center=p.position();
        DOMAINS.removeIf(d->d.owner()==p);
        var captives=new HashMap<UUID,Vec3>();
        for(var e:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(center,center).inflate(DOMAIN_RADIUS),
                t->t.isAlive()&&!t.isSpectator()&&t.position().distanceToSqr(center)<=DOMAIN_RADIUS*DOMAIN_RADIUS)){
            captives.put(e.getUUID(),e.position());
        }
        DOMAINS.add(new Domain(p,center,p.getServer().getTickCount()+DOMAIN_LIFE,DOMAIN_RADIUS,captives));
        JogoTerrain.open(p,center,DOMAIN_RADIUS);
        // 只有身处五条悟「无量空处」范围里才能开领域进入对拼；其他情况不登记对拼。
        if(insideVoidDomain(p))DomainClash.opened(p,center,DOMAIN_RADIUS);
        Network.send(p.serverLevel(),new Network.Effect(JogoFx.DOMAIN,p.getId(),center,Vec3.ZERO,(float)DOMAIN_RADIUS,DOMAIN_LIFE,0));
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,1.6f,0.45f);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("领域展开 · 盖棺铁围山"));
    }
    /** 施术者是否正站在某个生效中的「无量空处」里（五条悟首领或玩家五条悟都算）。 */
    public static boolean insideVoidDomain(ServerPlayer p) {
        double r=GojoFx.DOMAIN_RADIUS;
        for(var boss:p.serverLevel().getEntitiesOfClass(GojoEntity.class,
                new net.minecraft.world.phys.AABB(p.position(),p.position()).inflate(200))){
            Vec3 c=boss.clashCenter();
            if(boss.clashDomainActive()&&c!=null&&p.position().distanceToSqr(c)<=r*r)return true;
        }
        for(ServerPlayer other:p.server.getPlayerList().getPlayers()){
            if(other==p||other.level()!=p.level())continue;
            if(GojoCombat.domainOpen(other)&&p.position().distanceToSqr(GojoCombat.domainCenter(other))<=r*r)return true;
        }
        return false;
    }
    /** 作弊道具用：直接完成"当前章节"（按顺序推进一章→二章→三章，奖励正常发放）。 */
    public static boolean completeNextChapter(ServerPlayer p) {
        var data=Curse.data(p);
        if(!data.getBoolean(DONE)){
            data.putBoolean(DONE,true);data.putInt(PROGRESS,SOAK_TICKS);data.putInt(BUCKETS,3);
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("作弊：第一章·于长夜重返大地 已跳过"));
        }else if(!data.getBoolean(SIEGE_DONE)){
            data.putBoolean(SIEGE_DONE,true);
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("作弊：第二章·盖棺铁围山 已跳过"));
        }else if(!data.getBoolean(GOJO_DONE)){
            data.putBoolean(CLASH_DONE,true);data.putLong(CLASH_AT,p.level().getGameTime()-GOJO_SURVIVE);
            data.putBoolean(GOJO_DONE,true);
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("作弊：第三章·灾厄火焰：五条悟 已跳过"));
        }else{
            return false;
        }
        CharacterRewardsHook.grant(p);
        return true;
    }

    /** 对拼获胜时延长领域剩余时间（DomainClash.extend 会调用）。 */
    public static void extendDomain(ServerPlayer p,int ticks) {
        for(int i=0;i<DOMAINS.size();i++){
            var d=DOMAINS.get(i);
            if(d.owner()==p)DOMAINS.set(i,new Domain(d.owner(),d.center(),d.end()+ticks,d.radius(),d.captives()));
        }
    }
    public static boolean inDomain(ServerPlayer p) {
        for(Domain d:DOMAINS)if(d.owner()==p)return true;
        return false;
    }
    private static void tickDomains() {
        if(DOMAINS.isEmpty())return;
        for(Iterator<Domain> it=DOMAINS.iterator();it.hasNext();){
            Domain d=it.next();ServerPlayer owner=d.owner();
            if(owner==null||!owner.isAlive()||owner.level()!=owner.serverLevel()){it.remove();if(owner!=null)JogoTerrain.restore(owner);continue;}
            if(owner.getServer().getTickCount()>=d.end()){
                JogoTerrain.restore(owner);
                Network.send(owner.serverLevel(),new Network.Effect(JogoFx.DOMAIN_END,owner.getId(),d.center(),Vec3.ZERO,0,0,0));
                it.remove();continue;
            }
            ServerLevel level=owner.serverLevel();
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
                if(target.fireImmune()||!target.onGround())continue;
                Network.send(level,new Network.Effect(JogoFx.PILLAR,owner.getId(),target.position(),Vec3.ZERO,1.6f,10,0));
                AttackTypes.mark(AttackTypes.MAGIC);
                try{CombatSettings.hurt(owner,target,owner.damageSources().indirectMagic(owner,owner),damage);}
                finally{AttackTypes.clear();}
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),40));
            }
        }
    }

    /** 关服/退存档兜底：把没结束的领域地形还原，避免残留。 */
    @SubscribeEvent public static void serverStopped(net.minecraftforge.event.server.ServerStoppedEvent e) {
        for(ServerPlayer p:e.getServer().getPlayerList().getPlayers())JogoTerrain.restore(p);
        DOMAINS.clear();BUGS.clear();METEORS.clear();FALLING.clear();PILLARS.clear();
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
        if(!data.getBoolean(SIEGE_DONE)){
            boolean nether=p.level().dimension()==net.minecraft.world.level.Level.NETHER;
            boolean hurt=p.getHealth()<p.getMaxHealth()*SIEGE_HEALTH;
            if(nether&&hurt){
                int siege=data.getInt(SIEGE)+1;data.putInt(SIEGE,siege);
                if(siege%100==0)p.displayClientMessage(net.minecraft.network.chat.Component.literal("盖棺铁围山 "+siege/20+" / 60 秒"),true);
                if(siege>=NETHER_TICKS){
                    data.putBoolean(SIEGE_DONE,true);
                    p.sendSystemMessage(net.minecraft.network.chat.Component.literal("第二章·盖棺铁围山 完成"));
                    CharacterRewardsHook.grant(p);
                }
            }
            return;
        }
        // 第三章：开始时给虚空钥匙；对拼获胜后活过 3 分钟 + 击杀五条悟
        if(!data.getBoolean(KEY_GIVEN)){
            data.putBoolean(KEY_GIVEN,true);
            Journey.give(p,new net.minecraft.world.item.ItemStack(VoidContent.KEY.get()));
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("第三章·灾厄火焰：虚空钥匙已放入背包 · 右键进入虚空竞技场"));
        }
        if(data.getBoolean(GOJO_DONE))return;
        if(data.getBoolean(CLASH_DONE)&&p.level().getGameTime()-data.getLong(CLASH_AT)>=GOJO_SURVIVE){
            data.putBoolean(GOJO_DONE,true);
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("第三章·灾厄火焰：五条悟 完成"));
            CharacterRewardsHook.grant(p);
        }
        return;
        // 旧判定已废弃
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
