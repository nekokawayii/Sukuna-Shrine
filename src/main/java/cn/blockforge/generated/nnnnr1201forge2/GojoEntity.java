package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 虚空竞技场阶段首领：五条悟。玩家模型外观，无下限免疫除世界斩与领域展延外的全部伤害。 */
public final class GojoEntity extends Monster {
    public static final double HEALTH=150000,ARMOR=20;
    /** 0苍 1赫 2赫·最大功率 3茈 4无限制茈 5瞬移 6体术连击 7无量空处 */
    public static final int COUNT=9;
    public static final String[] NAMES={"苍","赫","赫·最大功率","茈","无限制茈","瞬移","体术连击","无量空处","苍·最大功率"};
    private static final int[] COOLDOWN={160,200,600,400,0,100,0,1200,300};
    private static final String[] CHANT={"九纲","偏光","乌与声明","表里之间"};
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(GojoEntity.class,EntityDataSerializers.INT),AGE=SynchedEntityData.defineId(GojoEntity.class,EntityDataSerializers.INT);
    private final ServerBossEvent bar=new ServerBossEvent(Component.literal("五条悟"),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.NOTCHED_20);
    private final long[] cooldowns=new long[COUNT];
    private final Set<UUID> participants=new HashSet<>();
    private final List<Orb> orbs=new ArrayList<>();
    private final List<Beam> beams=new ArrayList<>();
    private int rest,alternate,chantStep;
    private Vec3 center=Vec3.ZERO;
    private int domain;
    public GojoEntity(EntityType<? extends Monster> type,Level level){super(type,level);setPersistenceRequired();xpReward=5000;}
    public static void attributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent e){
        e.put(VoidContent.GOJO.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,HEALTH).add(Attributes.ARMOR,ARMOR).add(Attributes.MOVEMENT_SPEED,0.3).add(Attributes.ATTACK_DAMAGE,20).add(Attributes.FOLLOW_RANGE,120).add(Attributes.KNOCKBACK_RESISTANCE,0.8).build());
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ACTION,0);entityData.define(AGE,0);}
    public int action(){return entityData.get(ACTION);}public int actionAge(){return entityData.get(AGE);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putLongArray("GojoCooldowns",cooldowns);tag.putInt("GojoDomain",domain);tag.putDouble("GojoCenterX",center.x);tag.putDouble("GojoCenterY",center.y);tag.putDouble("GojoCenterZ",center.z);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);long[] saved=tag.getLongArray("GojoCooldowns");for(int i=0;i<Math.min(saved.length,cooldowns.length);i++)cooldowns[i]=saved[i];domain=tag.getInt("GojoDomain");center=new Vec3(tag.getDouble("GojoCenterX"),tag.getDouble("GojoCenterY"),tag.getDouble("GojoCenterZ"));}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    public Set<UUID> participants(){return participants;}
    private ServerLevel server(){return (ServerLevel)level();}
    private void fx(int kind,Vec3 p,Vec3 q,float size,int life,long seed){Network.send(server(),new Network.Effect(kind,getId(),p,q,size,life,seed));}
    // ------------------------------------------------------------ 无下限
    /** 除世界斩、领域展延与自身无限制茈反噬外，一切伤害都被无下限挡下。 */
    @SubscribeEvent public static void infinity(LivingAttackEvent event){
        // 客户端命中也会触发本事件，必须只在服务端判定，否则 (ServerLevel)level() 会崩。
        if(event.getEntity().level().isClientSide)return;
        if(!(event.getEntity() instanceof GojoEntity gojo)||gojo.isRemoved())return;
        var source=event.getSource();
        if(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        if(gojo.getPersistentData().getBoolean("GojoSelfDamage"))return;
        if(gojo.getPersistentData().getBoolean("SukunaWorldSlashHit"))return;
        if(gojo.getPersistentData().getBoolean("GojoAmplified"))return;
        LivingEntity attacker=source.getEntity() instanceof LivingEntity living?living:null;
        if(attacker!=null&&DomainAmplification.active(attacker))return;
        event.setCanceled(true);
        gojo.block(source);
    }
    private void block(DamageSource source){
        LivingEntity attacker=source.getEntity() instanceof LivingEntity living?living:null;
        if(attacker!=null){
            Vec3 push=attacker.position().subtract(position()).multiply(1,0,1);
            if(push.lengthSqr()>0.01)attacker.push(-push.x*0.6,0.25,-push.z*0.6);
            if(attacker instanceof ServerPlayer p)p.displayClientMessage(Component.literal("无下限：伤害被无限减速挡下，需要领域展延"),true);
        }
        if(tickCount%10==0)fx(GojoFx.BLOCK,getEyePosition().add(0,0.6,0),Vec3.ZERO,1.6f,8,0);
        level().playSound(null,blockPosition(),SoundEvents.SHIELD_BLOCK,SoundSource.HOSTILE,1.4f,0.6f);
    }
    /** 领域展延开启时，攻击者的术式可以穿透无下限。 */
    public void markAmplified(){getPersistentData().putBoolean("GojoAmplified",true);}
    private void selfDamage(float amount){
        getPersistentData().putBoolean("GojoSelfDamage",true);
        try{hurt(damageSources().indirectMagic(this,this),BalanceRules.damage(amount));}finally{getPersistentData().remove("GojoSelfDamage");}
    }
    // ------------------------------------------------------------ 战斗
    private boolean valid(LivingEntity p){return BossTargets.valid(this,p);}
    private boolean hit(LivingEntity p,double amount,boolean knock){
        if(!valid(p))return false;
        p.invulnerableTime=0;
        boolean hurt=p.hurt(soulSource(),BalanceRules.damage(amount,BalanceRules.Category.BOSS_ATTACK));
        if(hurt&&knock)fx(GojoFx.HIT,p.getBoundingBox().getCenter(),Vec3.ZERO,1.4f,10,p.getId());
        return hurt;
    }
    private DamageSource soulSource(){
        var holder=server().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("sukuna","guardian_soul")));
        return new DamageSource(holder,this);
    }
    private void face(Vec3 point){
        Vec3 d=point.subtract(getEyePosition());
        getLookControl().setLookAt(point.x,point.y,point.z,180,180);
        setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));
        setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));
        yBodyRot=getYRot();yHeadRot=getYRot();
    }
    private void begin(int skill,LivingEntity target){
        entityData.set(ACTION,skill);entityData.set(AGE,0);
        if(target!=null)face(target.getBoundingBox().getCenter());
        cooldowns[skill]=level().getGameTime()+COOLDOWN[skill];
        if(skill!=6)SkillSounds.play(this,"guardian_"+(4+skill%6));
    }
    private void finish(){entityData.set(ACTION,0);entityData.set(AGE,0);rest=12;}
    @Override protected void customServerAiStep(){
        if(level().isClientSide)return;
        long now=level().getGameTime();
        bar.setProgress(Math.max(0,Math.min(1,getHealth()/getMaxHealth())));
        bar.setName(Component.literal("五条悟 · "+(int)getHealth()+" / "+(int)getMaxHealth()+(domain>0?" · 无量空处":"")));
        LivingEntity target=BossTargets.select(this,96);
        if(target!=null)participants.add(target.getUUID());
        if(DomainClash.active(this)){tickDomain(now);return;}
        tickOrbs(now);tickBeams(now);tickDomain(now);
        if(action()!=0){channel(target);return;}
        if(target==null)return;
        double distance=distanceTo(target);
        if(rest>0){rest--;}
        if(distance>3&&distance<40)getNavigation().moveTo(target,1.1);
        if(rest>0)return;
        int skill=choose(target,distance,now);
        if(skill<0)return;
        begin(skill,target);
    }
    private int choose(LivingEntity target,double distance,long now){
        List<Integer> ready=new ArrayList<>();
        for(int i=0;i<COUNT;i++)if(i!=4&&i!=7&&now>=cooldowns[i])ready.add(i);
        if(distance<=4&&now>=cooldowns[6])return 6;
        if(domain<=0&&now>=cooldowns[7]&&distance<40)return 7;
        if(ready.isEmpty())return -1;
        alternate++;
        return ready.get(getRandom().nextInt(ready.size()));
    }
    private void channel(LivingEntity target){
        int skill=action(),age=entityData.get(AGE)+1;entityData.set(AGE,age);
        LivingEntity t=target!=null&&valid(target)?target:null;
        if(t!=null&&skill!=3&&skill!=7)face(t.getBoundingBox().getCenter());
        switch(skill){
            case 0->{ // 苍：引力拉扯
                if(age==1){
                    Vec3 at=t==null?getEyePosition().add(getLookAngle().scale(12)):t.position().add(0,0.6,0);
                    orbs.add(new Orb(true,at,Vec3.ZERO,t==null?null:t.getUUID(),200,nextOrbId())); // 苍停留 10 秒，等赫来撞
                }
                if(age>=40)finish();
            }
            case 1->{ // 赫：斥力冲击球
                if(age==1){
                    Orb field=fieldBlue();
                    Vec3 from=getEyePosition().add(getLookAngle().scale(0.8));
                    Vec3 dir=field!=null?field.pos.subtract(from).normalize()
                        :(t==null?getLookAngle():t.getBoundingBox().getCenter().subtract(getEyePosition()).normalize());
                    orbs.add(new Orb(false,from,dir.scale(1.6),null,60,nextOrbId()));
                }
                if(age>=14)finish();
            }
            case 2->{ // 赫·最大功率：蓄力 3 秒后射出持续跟随玩家的圆柱激光
                if(age<=60&&age%2==0)fx(GojoFx.CHARGE,hand(),Vec3.ZERO,1,30,0); // 短命红球，每 2 刻一颗，位置在手心
                if(age==60)level().playSound(null,blockPosition(),SoundEvents.WARDEN_SONIC_BOOM,SoundSource.HOSTILE,3,0.7f);
                // 60～100 刻持续跟瞄：每刻重新朝向目标并结算当前射线
                if(age>=60&&age<100){
                    LivingEntity aim=t;
                    if(aim!=null)face(aim.getBoundingBox().getCenter());
                    Vec3 from=hand().add(getLookAngle().scale(0.5)); // 激光从手心出去
                    Vec3 dir=aim==null?getLookAngle():aim.getBoundingBox().getCenter().subtract(from).normalize();
                    fx(GojoFx.BEAM,from,from.add(dir.scale(64)),0.55f,3,0);
                    for(Orb b:orbs)if(b.blue){Vec3 rel=b.pos.subtract(from);if(rel.dot(dir)>0&&rel.cross(dir).lengthSqr()<12.25){for(Orb x:orbs)if(x.blue)x.dead=true;begin(4,null);entityData.set(AGE,0);announce("激光贯穿苍——无限制茈：苍被吞没");orbs.removeIf(o->o.dead);break;}}
                    for(LivingEntity victim:BossTargets.nearby(this,66)){
                        if(victim==this||!valid(victim))continue;
                        Vec3 rel=victim.getBoundingBox().getCenter().subtract(from);
                        if(rel.dot(dir)<=0||rel.cross(dir).lengthSqr()>4.0)continue;
                        if(hit(victim,45,false)){
                            Vec3 push=dir.multiply(1,0,1).normalize();
                            victim.push(push.x*6,2.4,push.z*6);victim.hurtMarked=true;
                        }
                    }
                }
                if(age>=100)finish();
            }
            case 3->{ // 茈：双手合十 5 秒，苍球与赫球收拢对撞后才出现紫球
                Vec3 gather=getEyePosition().add(getLookAngle().scale(5.4)); // 前移 3 格，别挡视野
                if(age<=100)fx(GojoFx.PURPLE_GATHER,gather,Vec3.ZERO,3,100,age);
                if(age==100){
                    Vec3 dir=t==null?getLookAngle():t.getBoundingBox().getCenter().subtract(getEyePosition()).normalize();
                    fx(GojoFx.PURPLE_FORM,gather,Vec3.ZERO,3,30,0);
                    orbs.add(new Orb(false,gather,dir.scale(1.0),null,110,nextOrbId(),true)); // 直线，20 格/秒
                }
                if(age>=115)finish();
            }
            case 4->{ // 无限制茈：念完四句咒词后原地引爆，反噬自己
                // 念咒期间掌间苍赫收拢，特效始终挂在五条悟身上
                if(age<=120)fx(GojoFx.PURPLE_GATHER,getEyePosition().add(getLookAngle().scale(1.8)),Vec3.ZERO,2,100,Math.min(95,age*95/120));
                if(age%25==1){
                    int step=(age-1)/25;
                    if(step<CHANT.length){chantStep=step;bossChant(CHANT[step]);}
                    else{detonateUnlimited();finish();}
                }
                if(age>140)finish();
            }
            case 8->{ // 苍·最大功率：缓慢跟准星飞出，收招后停留 5 秒
                if(age==1){
                    orbs.add(new Orb(true,getEyePosition().add(getLookAngle().scale(1.2)),Vec3.ZERO,null,160,nextOrbId()).guided());
                }
                if(age>=60)finish();
            }
            case 5->{ // 瞬移（被无量空处抓住时连瞬移都放不出来：绝对无法移动）
                if(age==1&&!mindLocked(this)){
                    Vec3 behind=t==null?position():t.position().subtract(t.getLookAngle().scale(4));
                    Vec3 from=position();
                    if(server().noCollision(this,getBoundingBox().move(behind.subtract(position()))))teleportTo(behind.x,behind.y,behind.z);
                    fx(GojoFx.TELEPORT,from,position(),1,12,0);
                    if(t!=null)face(t.getBoundingBox().getCenter());
                }
                if(age>=10)finish();
            }
            case 6->{ // 体术连击
                if(age==1||age%7==0){
                    if(t!=null&&distanceToSqr(t)<=16&&hasLineOfSight(t)){
                        if(hit(t,8,false)){t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1));fx(GojoFx.HIT,t.getBoundingBox().getCenter(),Vec3.ZERO,1.2f,8,t.getId());}
                    }
                }
                if(age>=29)finish();
            }
            case 7->{ // 无量空处
                if(age==1){
                    center=position();domain=400;
                    fx(GojoFx.DOMAIN_OPEN,center,getLookAngle(),GojoFx.DOMAIN_RADIUS,400,0);
                    level().playSound(null,blockPosition(),SoundEvents.WARDEN_ROAR,SoundSource.HOSTILE,3,0.5f);
                    DomainClash.opened(this,center,GojoFx.DOMAIN_RADIUS);
                    announce("领域展开 · 无量空处：情报灌输，无法行动");
                }
                if(age>=20)finish();
            }
            default->finish();
        }
    }
    private int orbId;
    private int nextOrbId(){return ++orbId;}
    private static final class Orb {
        final boolean blue;Vec3 pos,dir;UUID target;int life;final int id;boolean purple;int age;boolean guide;boolean dead;
        Orb guided(){guide=true;return this;}
        Orb(boolean blue,Vec3 pos,Vec3 dir,UUID target,int life,int id){this(blue,pos,dir,target,life,id,false);}
        Orb(boolean blue,Vec3 pos,Vec3 dir,UUID target,int life,int id,boolean purple){this.blue=blue;this.pos=pos;this.dir=dir;this.target=target;this.life=life;this.id=id;this.purple=purple;}
    }
    private record Beam(Vec3 from,Vec3 dir,double length,int power,float damage){}
    private void tickOrbs(long now){
        for(Iterator<Orb> it=orbs.iterator();it.hasNext();){
            Orb o=it.next();o.age++;
            if(o.blue){
                if(o.guide){
                    if(action()==8)o.pos=o.pos.add(getLookAngle().multiply(1,0,1).normalize().scale(0.25)); // 每秒 5 格
                }else if(o.dir.lengthSqr()>1e-6)o.pos=o.pos.add(o.dir.scale(0.02));
            }
            else o.pos=o.pos.add(o.dir);
            if(o.blue){
                fx(GojoFx.BLUE,o.pos,o.pos,o.purple?3:2.5f,6,o.id);
                for(LivingEntity p:BossTargets.nearby(this,26)){
                    if(!valid(p))continue;
                    Vec3 pull=o.pos.subtract(p.position());
                    double d=pull.length();if(d<0.6||d>26)continue;
                    Vec3 step=pull.normalize().scale(Math.min(1.1,6.0/Math.max(1,d)));
                    BossTargets.move(p,p.position().add(step));
                    if(d<6){p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,1));p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,40,0));}
                    if(o.age%20==0)hit(p,10,false); // 苍：每秒 10
                }
            }else{
                fx(o.purple?GojoFx.PURPLE:GojoFx.RED,o.pos,o.pos.add(o.dir),o.purple?3:1,6,o.id);
                // 赫撞上留存的苍：先判对撞再判命中，否则会被站在苍上的玩家挡下来
                if(!o.purple){
                    boolean collided=false;
                    for(Orb b:orbs)if(b.blue&&b!=o&&!b.dead&&b.pos.distanceToSqr(o.pos)<12.25){collided=true;break;}
                    if(collided){
                        // 只做标记，循环结束后统一清理，避免 ConcurrentModificationException
                        o.dead=true;for(Orb b:orbs)if(b.blue)b.dead=true;
                        if(action()!=4){begin(4,null);entityData.set(AGE,0);announce("赫与苍对撞——无限制茈：苍被吞没");}
                        continue;
                    }
                }
                for(LivingEntity p:BossTargets.nearby(this,6)){
                    if(!valid(p)||p.getBoundingBox().inflate(0.6).distanceToSqr(o.pos)>2.25)continue;
                    boolean hurt=hit(p,o.purple?500:60,false); // 茈 500 / 赫 60
                    if(hurt){
                        Vec3 push=p.position().subtract(o.pos).multiply(1,0,1).normalize();
                        p.push(push.x*(o.purple?8:5),o.purple?2.2:1.6,push.z*(o.purple?8:5));
                        p.hurtMarked=true;
                    }
                    if(o.purple){
                        fx(GojoFx.PURPLE_BURST,o.pos,Vec3.ZERO,12,30,o.id); // 命中演出：变大 + 渐隐，无溅射伤害
                    }else fx(GojoFx.RED_END,o.pos,Vec3.ZERO,7,30,o.id);
                    it.remove();break;
                }
            }
            if(!o.dead&&o.age>o.life){fx(o.blue?GojoFx.BLUE_END:(o.purple?GojoFx.PURPLE_BURST:GojoFx.RED_END),o.pos,Vec3.ZERO,o.blue?5:0,4,o.id);it.remove();}
        }
        orbs.removeIf(o->o.dead);
        checkUnlimited();
        orbs.removeIf(o->o.dead);
    }
    private Orb fieldBlue(){for(Orb o:orbs)if(o.blue)return o;return null;}
    /** 手心：身前 0.55、下方 0.35、右侧 0.35 —— 蓄力红球与激光都从手里出去。 */
    private Vec3 hand(){
        Vec3 look=getLookAngle();
        Vec3 right=new Vec3(-look.z,0,look.x);
        right=right.lengthSqr()<1e-6?new Vec3(1,0,0):right.normalize();
        return getEyePosition().add(look.scale(0.55)).add(0,-0.35,0).add(right.scale(0.35));
    }
    /** 苍与赫对撞：进入无限制茈咏唱。 */
    private void checkUnlimited(){
        if(action()==4)return;
        for(Orb blue:orbs){
            if(!blue.blue||blue.dead)continue;
            for(Orb red:orbs){
                if(red.blue||red.purple||red.dead)continue;
                if(blue.pos.distanceToSqr(red.pos)<9.0){
                    blue.dead=true;red.dead=true;
                    begin(4,null);entityData.set(AGE,0);announce("赫与苍对撞——无限制茈：苍被吞没");
                    return;
                }
            }
        }
    }
    private void bossChant(String word){
        for(ServerPlayer p:server().getServer().getPlayerList().getPlayers())if(p.level()==level())p.displayClientMessage(Component.literal("五条悟：「"+word+"」"),false);
    }
    private void detonateUnlimited(){
        Vec3 at=position();double radius=64;orbs.removeIf(o->o.blue);
        fx(GojoFx.UNLIMITED,at,Vec3.ZERO,(float)radius,60,0);
        level().playSound(null,blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundSource.HOSTILE,4,0.5f);
        for(LivingEntity p:BossTargets.nearby(this,radius))if(valid(p)&&p.position().distanceToSqr(at)<=radius*radius)hit(p,999,false); // 无限制茈 999
        selfDamage(60000);
        announce("无限制茈 · 虚式反噬：范围 64 格，对五条悟自身同样生效");
    }
    private void tickBeams(long now){
        for(Iterator<Beam> it=beams.iterator();it.hasNext();){
            Beam b=it.next();it.remove();
            Vec3 end=b.from().add(b.dir().scale(b.length()));
            for(LivingEntity p:BossTargets.nearby(this,b.length()+2)){
                if(!valid(p))continue;
                if(p.getBoundingBox().inflate(0.7).clip(b.from(),end).isEmpty()&&!p.getBoundingBox().inflate(0.7).contains(b.from()))continue;
                if(hit(p,45,false)){
                    Vec3 push=b.dir().multiply(1,0,1).normalize();
                    p.push(push.x*6,2.4,push.z*6);p.hurtMarked=true;
                }
            }
        }
    }
    // ------------------------------------------------------------ 无量空处
    public boolean clashDomainActive(){return domain>0;}
    public void extendDomain(int ticks){if(domain>0)domain+=ticks;}
    public void clashCloseDomain(){domain=0;fx(GojoFx.DOMAIN_CLOSE,center,Vec3.ZERO,1,4,0);}
    public static boolean mindLocked(LivingEntity e){return e.getPersistentData().getLong("GojoVoid")>e.level().getGameTime();}
    private void tickDomain(long now){
        if(domain<=0)return;
        domain--;
        if(domain<=0){fx(GojoFx.DOMAIN_CLOSE,center,Vec3.ZERO,1,4,0);return;}
        center=position(); // 领域跟着五条悟走：画布与判定半径都以他为球心
        if(now%20==0)fx(GojoFx.DOMAIN,center,getLookAngle(),GojoFx.DOMAIN_RADIUS,30,0);
        double domR=GojoFx.DOMAIN_RADIUS;
        for(LivingEntity p:BossTargets.nearby(this,(int)domR)){
            if(!valid(p)||p.position().distanceToSqr(center)>domR*domR)continue;
            if(DomainClash.active(this))break;
            if(VoidLock.immune(this,p))continue; // 女仆/宠物/永恒挚爱/盟友：绝对不生效
            VoidLock.hold(p,now);                // 强制控制：拴在原地
            if(now%20==0)hit(p,6,false);
        }
    }
    private void announce(String text){
        for(ServerPlayer p:server().getServer().getPlayerList().getPlayers())if(p.level()==level())p.displayClientMessage(Component.literal(text),false);
    }
    @Override public boolean canAttack(LivingEntity target){return valid(target);}
    @Override protected boolean isSunBurnTick(){return false;}
    @Override public void die(DamageSource source){
        if(!level().isClientSide){
            bar.removeAllPlayers();
            VoidJourney.reward(this,source);
            for(LivingEntity p:BossTargets.nearby(this,64))p.getPersistentData().remove("GojoVoid");
        }
        super.die(source);
    }
    @Override public void remove(RemovalReason reason){
        if(!level().isClientSide&&reason!=RemovalReason.KILLED)bar.removeAllPlayers();
        super.remove(reason);
    }
    /** 咒词与招式提示用的公开名。 */
    public static String name(int skill){return skill>=0&&skill<COUNT?NAMES[skill]:"术式";}
}
