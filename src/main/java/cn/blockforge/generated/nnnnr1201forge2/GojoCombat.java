package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 玩家侧五条悟技能组：吃下碎片后可用，各自独立冷却，与右侧小轮盘一一对应。 */
public final class GojoCombat {
    public static final int COUNT=GojoPlayer.COUNT;
    /** 「茈」打魔虚罗的伤害倍率：**10 倍**（其余目标不受影响）。 */
    public static final float MAHORAGA_DAMAGE=10f;
    /** 茈「一下打不死」时的引爆补伤：在 500 本体之上再补 **25%**（同一套加成与适应占位）。 */
    public static final float PURPLE_BANG_BONUS=0.25f;
    /** 宇宙大爆炸的视觉尺寸（客户端按这个半径铺三层冲击壳 + 光针）。 */
    public static final float PURPLE_BANG_SIZE=10f;
    /** 「无限制茈」打魔虚罗的倍率：沿用旧值 3 倍（本次需求只点了茈，要一起改就把这里也换成 10）。 */
    public static final float MAHORAGA_DAMAGE_UNLIMITED=3f;
    /** 茈命中魔虚罗后的**缓慢**时长与等级：30 秒 · 缓慢 I。 */
    public static final int PURPLE_SLOW_TICKS=20*30,PURPLE_SLOW_AMPLIFIER=0;
    /** 茈命中后的减速：只对魔虚罗生效（其余目标不吃这一条）。 */
    private static void purpleSlow(LivingEntity target){
        if(target instanceof MahoragaEntity)target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,PURPLE_SLOW_TICKS,PURPLE_SLOW_AMPLIFIER,false,false,false));
    }
    private static final Map<UUID,Orb> guiding=new HashMap<>();
    private static final class State {
        final ServerPlayer p;final ServerLevel level;
        int casting=-1,age;UUID target;Vec3 center=Vec3.ZERO;long domainUntil;int chargeSkill=-1;long chargeAt;boolean chargeFired;
        /** 结印"放下"后 = true：无量空处只控制、**不再造成伤害**（下一次展开时复位）。 */
        boolean controlOnly;
        /** 展开音效的时间轴：>0 表示还要在那一刻播放 gojodomainmid（起手音 1.445 秒之后）。 */
        long midSoundAt;
        State(ServerPlayer p){this.p=p;level=p.serverLevel();}
    }
    /** 无量空处：持续 **90 秒**（1 分半）。 */
    public static final int UNLIMITED_TICKS=1800;
    /** 无量空处持续伤害：**每 0.1 秒（2 刻）2 点**。 */
    public static final double UNLIMITED_DAMAGE=2;
    /** 在"绝对无法移动"之上再叠的减速档：移动缓慢 IV（amplifier 3）。 */
    public static final int UNLIMITED_SLOW_AMPLIFIER=3;
    private static final Map<UUID,State> states=new HashMap<>();
    private static final List<Orb> orbs=new ArrayList<>();
    private static final List<Beam> beams=new ArrayList<>();
    private static int orbId;
    private static final class Orb {
        final ServerPlayer owner;final ServerLevel level;final boolean blue;final boolean purple;
        Vec3 pos,dir;UUID target;int age,life;final int id;boolean guide;boolean dead;
        /** 已经结算过的目标：茈会穿透，途中碰到的每一只都吃伤害，但同一只只吃一次。 */
        final java.util.Set<UUID> struck=new java.util.HashSet<>();
        Orb(ServerPlayer owner,boolean blue,boolean purple,Vec3 pos,Vec3 dir,UUID target,int life){this.owner=owner;this.level=owner.serverLevel();this.blue=blue;this.purple=purple;this.pos=pos;this.dir=dir;this.target=target;this.life=life;this.id=++orbId;}
    }
    private record Beam(ServerPlayer owner,ServerLevel level,Vec3 from,Vec3 dir,double length,float damage){}
    /** 六眼本身就是全部门槛：不再要求宿傩之路，女仆与未开阶段的玩家都能直接用。 */
    private static boolean unlocked(ServerPlayer p){return GojoPlayer.unlocked(p)&&!MaidBridge.recovering(p)&&Curse.canUseTechnique(p)&&!SoulCombat.suppressed(p);}
    private static String key(int skill){return "GojoCooldown"+skill;}
    public static boolean available(ServerPlayer p,int skill){
        if(skill<0||skill>=COUNT||!unlocked(p)||!p.isAlive()||p.isSpectator()||DomainClash.active(p))return false;
        if(GuardianPlayerSkills.casting(p)||Skills.sustaining(p)&&skill!=7)return false;
        if(Curse.active(p,key(skill)))return false;
        return Curse.canSpend(p,GojoPlayer.COST[skill]);
    }
    /** 被拒时逐条报出真实原因，避免"按了没反应"这种无法排查的体验。 */
    private static void reject(ServerPlayer p,int skill){
        String name=GojoPlayer.NAMES[skill];
        if(!GojoPlayer.unlocked(p)){CastFeedback.tell(p,"尚未睁开六眼：右键使用六眼即可，无需任何阶段");return;}
        if(DomainClash.penalized(p)){CastFeedback.tell(p,"领域对拼败北熔断 · 剩余 "+((DomainClash.penaltyTicks(p)+19)/20)+" 秒 · 不能提前修复");return;}
        if(Curse.burnout(p)){CastFeedback.tell(p,"术式熔断中：用术式反转或修复熔断解除");return;}
        if(DomainClash.active(p)){CastFeedback.tell(p,"领域对拼中，无法使用"+name);return;}
        if(GuardianPlayerSkills.casting(p)){CastFeedback.tell(p,"守护者术式动作未结束，请稍候");return;}
        if(Skills.sustaining(p)&&skill!=7){CastFeedback.tell(p,"正在维持领域，无法使用"+name);return;}
        if(CastFeedback.cooldown(p,key(skill),name))return;
        if(!Curse.canSpend(p,GojoPlayer.COST[skill])){
            CastFeedback.tell(p,name+" 需要 "+BalanceRules.number(GojoPlayer.COST[skill])+" 咒力（当前 "+BalanceRules.number(Curse.energy(p))+"）");return;
        }
        CastFeedback.tell(p,"当前状态无法使用"+name);
    }
    /** 领域剩余刻数（0 = 未展开）。 */
    public static int domainTicks(ServerPlayer p){State s=states.get(p.getUUID());return s==null?0:(int)Math.max(0,s.domainUntil-p.level().getGameTime());}
    /** 该技能剩余冷却秒数（向上取整）。 */
    public static int cooldownSeconds(ServerPlayer p,int skill){return (int)(Math.max(0,Curse.data(p).getLong(key(skill))-p.level().getGameTime())+19)/20;}
    /** 无量空处是否正在展开（心跳期间保持 true）。 */
    public static boolean domainOpen(ServerPlayer p){State s=states.get(p.getUUID());return s!=null&&s.domainUntil>p.level().getGameTime();}
    /** 无量空处此刻能不能展开：不能就**逐条说清原因**（不再静默失败）。返回 true = 可以继续 cast。 */
    private static boolean domainReady(ServerPlayer p){
        if(!GojoPlayer.unlocked(p)){CastFeedback.tell(p,"尚未睁开六眼：右键使用六眼即可，无需任何阶段");return false;}
        if(!p.isAlive()||p.isSpectator()){CastFeedback.tell(p,"当前状态无法展开领域");return false;}
        if(DomainClash.active(p)){CastFeedback.tell(p,"领域对拼中，无法展开无量空处");return false;}
        if(DomainClash.penalized(p)){CastFeedback.tell(p,"领域对拼败北熔断 · 剩余 "+((DomainClash.penaltyTicks(p)+19)/20)+" 秒 · 不能提前修复");return false;}
        if(Curse.burnout(p)){CastFeedback.tell(p,"术式熔断 · 剩余 "+((Curse.burnoutTicks(p)+19)/20)+" 秒：用术式反转或修复熔断解除");return false;}
        if(MaidBridge.recovering(p)){CastFeedback.tell(p,"女仆恢复中，无法展开领域");return false;}
        if(SoulCombat.suppressed(p)){CastFeedback.tell(p,"灵魂压制中，无法展开领域");return false;}
        if(GuardianPlayerSkills.casting(p)){CastFeedback.tell(p,"守护者术式动作未结束，请稍候");return false;}
        if(GuardianPlayerSkills.sustaining(p)||Skills.sustaining(p)){CastFeedback.tell(p,"正在维持另一个领域，先把它收起来");return false;}
        long left=Curse.data(p).getLong(key(7))-p.level().getGameTime();
        if(left>0){CastFeedback.tell(p,"无量空处冷却中 · 剩余 "+(left+19)/20+" 秒");return false;}
        if(!Curse.canSpend(p,GojoPlayer.COST[7])){CastFeedback.tell(p,"无量空处需要 "+BalanceRules.number(GojoPlayer.COST[7])+" 咒力（当前 "+BalanceRules.number(Curse.energy(p))+"）");return false;}
        return true;
    }
    /** 无量空处释放键：三档循环（结印 → 放下仅控制 → 收起）统一由 {@link Skills#voidPress} 处理——
     *  它持有与御厨子**同一套**结印状态机（Seal/freeze/pose/released）。返回值保持"这次输入已被消耗"。 */
    public static boolean toggleDomain(ServerPlayer p){
        Skills.voidPress(p);
        return true;
    }
    /** 无量空处结印起手：门槛沿用 {@link #domainReady}（逐条说明原因），扣费与冷却沿用原来 cast 里的写法。 */
    public static boolean payVoidSeal(ServerPlayer p){
        if(!domainReady(p))return false;
        if(!Curse.spend(p,GojoPlayer.COST[7]))return false;
        if(GojoPlayer.COOLDOWN[7]>0)Curse.status(p,key(7),GojoPlayer.COOLDOWN[7]);
        return true;
    }
    /** 第 80 刻（4 秒）：领域真正生成——原 case 7 的整段逻辑原样搬到这里。 */
    public static void openVoidDomain(ServerPlayer p){
        State s=states.computeIfAbsent(p.getUUID(),k->new State(p));
        if(s.p!=p||s.level!=p.level())states.put(p.getUUID(),s=new State(p));
        s.controlOnly=false;   // 新一次展开：伤害恢复
        // 展开音效：先放起手，1.445 秒（29 刻）后再放 mid —— 两段无缝衔接
        // 展开音效已提前到"按下释放键"那一刻由 Skills.startVoidSeal 播放（音量 5），这里不再重复播。
        s.center=p.position();s.domainUntil=p.level().getGameTime()+UNLIMITED_TICKS;
        // direction 带上朝向：客户端把白边黑洞摆在正前方
        Network.send(p.serverLevel(),new Network.Effect(GojoFx.DOMAIN_OPEN,p.getId(),s.center,p.getLookAngle(),GojoFx.DOMAIN_RADIUS,400,0));
        DomainClash.opened(p,s.center,GojoFx.DOMAIN_RADIUS);
        p.displayClientMessage(Component.literal("领域展开 · 无量空处：情报灌输，范围内敌人无法行动 · 再按释放键放下（放下后仅控制）"),true);
        p.sendSystemMessage(Component.literal("无量空处已展开 · 半径 "+(int)GojoFx.DOMAIN_RADIUS+" 格 · "+(UNLIMITED_TICKS/20)+" 秒 · 再按=放下(仅控制) · 放下后再按一次=收起（收起后 120 秒熔断，可修复）"));
        s.casting=-1;
    }
    /** 放下结印：领域与控制照旧，只是**伤害那一路整段跳过**（见 tick 里的 now%2==0）。 */
    public static void controlOnly(ServerPlayer p){
        State s=states.get(p.getUUID());
        if(s!=null&&s.domainUntil>p.level().getGameTime())s.controlOnly=true;
    }
    /** 苍/赫 按下：记录按住起点；越过长按阈值后才进入最大功率（按住期间持续引导/蓄力）。 */
    public static void chargeStart(ServerPlayer p,int slot){
        int skill=slot>=0&&slot<GojoPlayer.SLOTS?GojoPlayer.WHEEL[slot]:-1;
        // 无量空处：领域开着时再点一次就是收起，必须先于冷却/咒力/对拼检查
        if(skill==7){toggleDomain(p);return;}
        if(skill<0)return;
        if(!available(p,skill)){reject(p,skill);return;} // 不再静默失败：直接说明是哪一道门槛
        State s=states.computeIfAbsent(p.getUUID(),k->new State(p));
        s.chargeSkill=slot;s.chargeAt=p.level().getGameTime();s.chargeFired=false;
        if(slot>1){cast(p,skill);s.chargeSkill=-1;} // 其余技能按下即放
    }
    /** 松手：没越过阈值就是普通版；已进入最大功率则由引导/蓄力自行收尾。 */
    public static void chargeRelease(ServerPlayer p,int slot){
        State s=states.get(p.getUUID());
        if(s==null||s.chargeSkill!=slot)return;
        boolean fired=s.chargeFired;
        s.chargeSkill=-1;s.chargeFired=false;
        if(!fired)cast(p,GojoPlayer.WHEEL[slot]);
        else if(slot==0)releaseBlueMax(p); // 苍·最大功率：松手停在原地 5 秒
    }
    /** 按住期间：越过阈值就真正开始最大功率。 */
    private static void chargeTick(State s){
        if(s.chargeSkill<0||s.chargeFired)return;
        long held=s.p.level().getGameTime()-s.chargeAt;
        int threshold=s.chargeSkill==0?6:10;
        if(held<threshold)return;
        s.chargeFired=true;
        cast(s.p,s.chargeSkill==0?8:2);
    }
    public static void cast(ServerPlayer p,int skill){
        // 无量空处不再按下即展开：走 Skills 里那一套结印时间轴（按下=结印，第 80 刻才真正展开）
        if(skill==7){Skills.voidPress(p);return;}
        if(!available(p,skill)){reject(p,skill);return;}
        State s=states.computeIfAbsent(p.getUUID(),k->new State(p));
        if(s.p!=p||s.level!=p.level()){states.put(p.getUUID(),s=new State(p));}
        if(!Curse.spend(p,GojoPlayer.COST[skill]))return;
        // Curse.status 的第三个参数是"刻"，之前误按"秒"传入导致冷却只剩几刻
        if(GojoPlayer.COOLDOWN[skill]>0)Curse.status(p,key(skill),GojoPlayer.COOLDOWN[skill]);
        LivingEntity aim=Skills.aimTarget(p);
        s.casting=skill;s.age=0;s.target=aim==null?null:aim.getUUID();
        switch(skill){
            case 0->{ // 苍
                Vec3 at=aim==null?Skills.aimPoint(p,24):aim.getBoundingBox().getCenter();
                orbs.add(new Orb(p,true,false,at,Vec3.ZERO,aim==null?null:aim.getUUID(),200)); // 苍停留 10 秒
                Network.send(p.serverLevel(),new Network.Effect(GojoFx.BLUE,p.getId(),at,at,2.5f,6,orbId));
                p.displayClientMessage(Component.literal("苍 · 引力拉扯"),true);
            }
            case 1->{ // 赫
                Orb field=fieldBlue(p);
                Vec3 from=p.getEyePosition().add(p.getLookAngle().scale(1));
                Vec3 dir=field!=null?field.pos.subtract(from).normalize()
                    :(aim==null?p.getLookAngle():aim.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize());
                orbs.add(new Orb(p,false,false,from,dir.scale(1.8),null,60)); // 直线飞出：发射瞬间定方向，空中不拐弯
                p.displayClientMessage(Component.literal("赫 · 斥力冲击球"),true);
            }
            case 2->{ // 赫·最大功率
                p.displayClientMessage(Component.literal("赫 · 最大功率：蓄力中"),true);
            }
            case 3->{ // 茈
                p.displayClientMessage(Component.literal("茈：苍与赫对撞中"),true);
                // 对撞特效由 channel 每刻发送（跟着自己），这里不再提前发一发造成闪一下
            }
            case 4->{ // 无限制茈
                p.displayClientMessage(Component.literal("无限制茈：需要念完四句咒词，反噬由自己承受"),true);
            }
            case 5->{ // 瞬移
                Vec3 from=p.position();
                Vec3 dir=p.getLookAngle().multiply(1,0,1).normalize();
                Vec3 to=from;
                for(int i=1;i<=8;i++){
                    Vec3 step=from.add(dir.scale(i));
                    var block=p.serverLevel().clip(new ClipContext(from.add(0,p.getBbHeight()*0.5,0),step.add(0,p.getBbHeight()*0.5,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
                    if(block.getType()!=HitResult.Type.MISS)break;
                    if(!p.serverLevel().noCollision(p,p.getBoundingBox().move(step.subtract(p.position()))))break;
                    to=step;
                }
                p.teleportTo(to.x,to.y,to.z);
                Network.send(p.serverLevel(),new Network.Effect(GojoFx.TELEPORT,p.getId(),from,to,1,12,0));
                s.casting=-1;
            }
            case 6->{ // 体术连击
                p.displayClientMessage(Component.literal("体术连击"),true);
            }
            case 8->{ // 苍·最大功率：长按跟准星缓慢飞出，松开停留 5 秒
                Orb orb=new Orb(p,true,false,p.getEyePosition().add(p.getLookAngle().scale(1.2)),Vec3.ZERO,null,20*120);
                orb.guide=true;orbs.add(orb);guiding.put(p.getUUID(),orb);
                p.displayClientMessage(Component.literal("苍·最大功率：按住引导，松开后停留 5 秒"),true);
                s.casting=-1;
            }
            case 7->s.casting=-1; // 无量空处不在这里展开：见 openVoidDomain（结印第 80 刻）
            default->s.casting=-1;
        }
        if(skill!=5&&skill!=7)SkillSounds.play(p,"guardian_"+((skill%6)+4));
    }
    private static Orb fieldBlue(ServerPlayer p){for(Orb o:orbs)if(o.blue&&o.owner==p)return o;return null;}
    /** 手心：身前 0.45、下方 0.35、右侧 0.32 —— 蓄力的红球与激光都从手里出去。 */
    private static Vec3 hand(ServerPlayer p){
        Vec3 look=p.getLookAngle();
        Vec3 right=new Vec3(-look.z,0,look.x);
        right=right.lengthSqr()<1e-6?new Vec3(1,0,0):right.normalize();
        return p.getEyePosition().add(look.scale(0.45)).add(0,-0.35,0).add(right.scale(0.32));
    }
    /** 苍与赫对撞：直接引爆无限制茈，代价是自己 40% 最大生命。 */
    /** 松开按键：苍停在原地 5 秒。 */
    public static void releaseBlueMax(ServerPlayer p){
        Orb o=guiding.remove(p.getUUID());
        if(o!=null){o.guide=false;o.life=o.age+100;}
    }
    /** 无限制茈：48 格内 999 伤害（打魔虚罗 ×3），反噬自己 40% 最大生命。念咒版与对撞版数值一致。 */
    public static void unlimited(ServerPlayer p,Vec3 at){unlimited(p,at,"赫与苍对撞");}
    public static void unlimited(ServerPlayer p,Vec3 at,String reason){
        double radius=48;
        fx(p,GojoFx.UNLIMITED,at,Vec3.ZERO,(float)radius,60,0);
        for(LivingEntity victim:Skills.targets(p.serverLevel(),p,at,radius))if(victim.position().distanceToSqr(at)<=radius*radius)hit(p,victim,999,false,MAHORAGA_DAMAGE_UNLIMITED,AttackTypes.MAGIC_PURPLE); // 无限制茈 999（打魔虚罗 ×3）
        p.invulnerableTime=0;
        p.hurt(p.serverLevel().damageSources().indirectMagic(p,p),Math.max(1,p.getMaxHealth()*0.4f));
        p.displayClientMessage(Component.literal(reason+" · 无限制茈（反噬 40% 最大生命）"),true);
    }
    public static boolean sustaining(ServerPlayer p){State s=states.get(p.getUUID());return s!=null&&s.domainUntil>p.level().getGameTime();}
    /** 无量空处只要被关闭（主动收起 / 到时 / 对拼结束）就进入术式熔断，与对拼败北同为 120 秒。 */
    public static void closeDomain(ServerPlayer p){
        State s=states.get(p.getUUID());
        if(s==null||s.domainUntil<=0)return;
        s.domainUntil=0;s.controlOnly=false;
        Skills.endVoidSeal(p); // 领域关闭：无量空处的结印状态（解冻/手势/放下标记）一并清掉
        fx(p,GojoFx.DOMAIN_CLOSE,s.center,Vec3.ZERO,1,4,0);
        // 只进入普通熔断：不写 DomainClashPenaltyUntil，因此可以用"修复熔断"解除
        Curse.enterBurnout(p,DomainClashRules.BURNOUT_TICKS);
        p.sendSystemMessage(Component.literal("无量空处关闭 · 术式熔断 "+BalanceRules.number(DomainClashRules.BURNOUT_TICKS/20)+" 秒（可用修复熔断解除）"));
    }
    public static void clashVisual(ServerPlayer p,boolean show){State s=states.get(p.getUUID());if(s==null||s.domainUntil<=0)return;if(show)fx(p,GojoFx.DOMAIN,s.center,p.getLookAngle(),GojoFx.DOMAIN_RADIUS,400,0);else fx(p,GojoFx.DOMAIN_CLOSE,s.center,Vec3.ZERO,1,4,0);}
    public static void extendDomain(ServerPlayer p,int ticks){State s=states.get(p.getUUID());if(s!=null&&s.domainUntil>0)s.domainUntil+=ticks;}
    /** 赫的爆炸半径（格）与范围内伤害/击退（用户指定：半径 3.5 格内都有伤害和击退）。 */
    public static final double RED_RADIUS=3.5;
    public static final double RED_AREA_DAMAGE=60;
    /** 赫 爆炸：以爆心为中心，半径 {@link #RED_RADIUS} 内每个生物都吃一次伤害 + 沿径向击退。 */
    private static void redBurst(ServerPlayer p,Vec3 center,net.minecraft.server.level.ServerLevel level){
        var box=new net.minecraft.world.phys.AABB(center,center).inflate(RED_RADIUS);
        for(LivingEntity victim:level.getEntitiesOfClass(LivingEntity.class,box,v->v!=p&&v.isAlive()&&CombatSettings.canHit(p,v))){
            if(victim.distanceToSqr(center)>RED_RADIUS*RED_RADIUS)continue;      // 球形范围
            hit(p,victim,RED_AREA_DAMAGE,false,1f,AttackTypes.MAGIC_RED);        // 独立适应占位：赫
            Vec3 push=victim.position().subtract(center);
            if(push.lengthSqr()<1e-4)push=p.getLookAngle();
            push=push.normalize().scale(1.15);
            victim.push(push.x,0.42,push.z);victim.hurtMarked=true;              // 击退（向外 + 上抛）
        }
    }
    private static boolean hit(ServerPlayer p,LivingEntity target,double damage,boolean knock){return hit(p,target,damage,knock,1f,AttackTypes.MAGIC);}
    /** 命中结算。{@code bossMultiplier} 只对<b>魔虚罗</b>生效：茈打它 **10 倍**（{@link #MAHORAGA_DAMAGE}），
     *  无限制茈 3 倍（{@link #MAHORAGA_DAMAGE_UNLIMITED}）；
     *  {@code type} 是这次的适应占位：苍/赫/茈各自一条进度（{@link AttackTypes#MAGIC_BLUE} 等）。 */
    private static boolean hit(ServerPlayer p,LivingEntity target,double damage,boolean knock,float bossMultiplier,String type){
        if(target==null||!CombatSettings.canHit(p,target))return false;
        target.invulnerableTime=0;
        float amount=CombatSettings.spellDamage(p,(float)damage);
        if(bossMultiplier!=1f&&target instanceof MahoragaEntity)amount*=bossMultiplier;
        AttackTypes.mark(type);
        boolean hurt=CombatSettings.hurt(p,target,p.serverLevel().damageSources().indirectMagic(p,p),amount);
        if(hurt&&knock){Vec3 push=target.position().subtract(p.position()).multiply(1,0,1).normalize();target.push(push.x*5,1.6,push.z*5);target.hurtMarked=true;}
        return hurt;
    }
    private static void fx(ServerPlayer p,int kind,Vec3 pos,Vec3 dir,float size,int life,long seed){Network.send(p.serverLevel(),new Network.Effect(kind,fxOwner(p),pos,dir,size,life,seed));}
    /** 女仆施法时用女仆本体的实体 id：客户端才能把画布/特效锚在她身上跟着走。 */
    private static int fxOwner(ServerPlayer p){return p instanceof MaidBridge.Actor a?a.maid.getId():p.getId();}
    /** 正在引导或蓄力（女仆 AI 用，避免打断自己的招式）。 */
    public static boolean busy(ServerPlayer p){State s=states.get(p.getUUID());return s!=null&&(s.casting>=0||s.chargeSkill>=0);}
    /** 场上是否已有自己的苍（AI 用它决定"赫撞苍"）。 */
    public static boolean hasBlue(ServerPlayer p){return fieldBlue(p)!=null;}
    private static void channel(State s){
        ServerPlayer p=s.p;int skill=s.casting,age=++s.age;
        LivingEntity t=s.target==null?null:(s.level.getEntity(s.target) instanceof LivingEntity e&&CombatSettings.canHit(p,e)?e:null);
        switch(skill){
            case 2->{ // 蓄力 3 秒后激光
                Vec3 hand=hand(p); // 蓄力红球就在手心里
                if(age<=60&&age%2==0)fx(p,GojoFx.CHARGE,hand,Vec3.ZERO,1,30,0); // 每 2 刻一颗短命红球，客户端不做句柄移动
                if(age==60){
                    Vec3 from=hand(p).add(p.getLookAngle().scale(0.5)); // 激光也从手里出去
                    Vec3 dir=t==null?p.getLookAngle():t.getBoundingBox().getCenter().subtract(from).normalize();
                    var block=p.serverLevel().clip(new ClipContext(from,from.add(dir.scale(64)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
                    double length=block.getType()==HitResult.Type.MISS?64:from.distanceTo(block.getLocation());
                    beams.add(new Beam(p,p.serverLevel(),from,dir,length,45));
                    fx(p,GojoFx.BEAM,from,from.add(dir.scale(Math.max(1,length))),0.6f,16,0);
                    // 激光贯穿场上的苍：直接引爆无限制茈（玩家侧不念咒词）
                    for(Orb b:orbs)if(b.blue&&b.owner==p){Vec3 rel=b.pos.subtract(from);if(rel.dot(dir)>0&&rel.cross(dir).lengthSqr()<12.25){for(Orb x:orbs)if(x.blue)x.dead=true;guiding.clear();unlimited(p,b.pos);break;}}
                    p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.WARDEN_SONIC_BOOM,SoundSource.PLAYERS,2.4f,0.8f);
                }
                if(age>=80)s.casting=-1;
            }
            case 3->{ // 5 秒对撞后打出紫球
                Vec3 gather=p.getEyePosition().add(p.getLookAngle().scale(5.4)); // 前移 3 格
                if(age<=100)fx(p,GojoFx.PURPLE_GATHER,gather,Vec3.ZERO,3,100,age);
                if(age==100){
                    // 茈是直线炮击：只按发射那一刻的准星定方向，出膛后不再追踪目标；速度 20 格/秒
                    fx(p,GojoFx.PURPLE_FORM,gather,Vec3.ZERO,3,30,0);
                    orbs.add(new Orb(p,false,true,gather,p.getLookAngle().scale(1.0),null,100));
                }
                if(age>=115)s.casting=-1;
            }
            case 4->{ // 四句咒词后引爆：念咒期间掌间的苍与赫收拢，始终跟着自己
                if(age<=120)fx(p,GojoFx.PURPLE_GATHER,p.getEyePosition().add(p.getLookAngle().scale(1.8)),Vec3.ZERO,2,100,Math.min(95,age*95/120));
                if(age%25==1){
                    int step=(age-1)/25;
                    if(step<4)p.displayClientMessage(Component.literal("「"+new String[]{"九纲","偏光","乌与声明","表里之间"}[step]+"」"),false);
                }
                if(age==126){
                    unlimited(p,p.position(),"四句咒词念毕"); // 48 格 999，反噬由自己承受
                    s.casting=-1;
                }
                if(age>140)s.casting=-1;
            }
            case 6->{ // 三连拳
                if(age==1||age==8||age==15){
                    LivingEntity victim=t!=null&&p.distanceToSqr(t)<=16?t:null;
                    if(victim!=null&&hit(p,victim,18,false,1f,AttackTypes.MAGIC))victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1));
                    else fx(p,GojoFx.HIT,p.getEyePosition().add(p.getLookAngle().scale(2)),Vec3.ZERO,1.1f,8,0);
                }
                if(age>=22)s.casting=-1;
            }
            default->s.casting=-1;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(Iterator<State> it=states.values().iterator();it.hasNext();){
            State s=it.next();ServerPlayer p=s.p;
            if(!p.isAlive()||p.isRemoved()||p.level()!=s.level){it.remove();continue;}
            if(s.domainUntil>0&&s.domainUntil<=p.level().getGameTime())closeDomain(p);
            if(s.domainUntil>0){
                s.center=p.position(); // 领域跟着施法者走：画布与判定半径都以他为球心
                if(p.level().getGameTime()%20==0)fx(p,GojoFx.DOMAIN,s.center,p.getLookAngle(),GojoFx.DOMAIN_RADIUS,30,0);
                long now=p.level().getGameTime();
                // 无量空处展开音效：起手 gojodomainstart（1.445 秒）之后无缝接 gojodomainmid
                if(s.midSoundAt>0&&now>=s.midSoundAt){s.midSoundAt=0;SkillSounds.play(p,"gojo_domain_mid");}
                double domR=GojoFx.DOMAIN_RADIUS;
                for(LivingEntity victim:Skills.targets(p.serverLevel(),p,s.center,(int)domR)){
                    if(victim.position().distanceToSqr(s.center)>domR*domR)continue;
                    if(VoidLock.immune(p,victim))continue; // 自己/盟友与默认玩家/宠物/女仆/永恒挚爱：绝对不生效
                    if(DomainClash.active(p))break;
                    VoidLock.hold(victim,now); // 强制控制：拴在原地，绝对无法移动
                    // 每 0.1 秒 2 点（基础「魔法」）；结印"放下"（controlOnly）后这一段整段跳过，只留控制
                    if(!s.controlOnly&&now%2==0)hit(p,victim,UNLIMITED_DAMAGE,false,1f,AttackTypes.MAGIC);
                }
            }
            chargeTick(s);
            if(s.casting>=0)channel(s);
        }
        for(Iterator<Orb> it=orbs.iterator();it.hasNext();){
            Orb o=it.next();o.age++;
            if(!o.owner.isAlive()||o.owner.level()!=o.level){it.remove();continue;}
            if(o.blue){
                if(o.guide&&guiding.get(o.owner.getUUID())==o)o.pos=o.pos.add(o.owner.getLookAngle().multiply(1,0,1).normalize().scale(0.25)); // 每秒 5 格
                fx(o.owner,GojoFx.BLUE,o.pos,o.pos,2.5f,6,o.id);
                for(LivingEntity victim:Skills.targets(o.level,o.owner,o.pos,24)){
                    Vec3 pull=o.pos.subtract(victim.position());double d=pull.length();
                    if(d<0.6||d>24)continue;
                    BossTargets.move(victim,victim.position().add(pull.normalize().scale(Math.min(1.1,6.0/Math.max(1,d)))));
                    if(d<6){victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,1));victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,40,0));}
                    if(o.age%20==0)hit(o.owner,victim,10,false,1f,AttackTypes.MAGIC_BLUE); // 苍：每秒 10（独立适应）
                }
                if(o.age>=o.life){fx(o.owner,GojoFx.BLUE_END,o.pos,Vec3.ZERO,5,4,o.id);it.remove();}
                continue;
            }
            o.pos=o.pos.add(o.dir); // 赫与茈全程直线：出膛后不追人，命中与否取决于发射时的瞄准
            fx(o.owner,o.purple?GojoFx.PURPLE:GojoFx.RED,o.pos,o.pos.add(o.dir),o.purple?3:1,6,o.id);
            // 场上的苍还在、赫撞上就直接爆无限制茈（玩家侧不需要念咒词），且先判对撞再判命中
            if(!o.purple){
                boolean collided=false;
                for(Orb b:orbs)if(b.blue&&b!=o&&b.owner==o.owner&&b.pos.distanceToSqr(o.pos)<12.25){collided=true;break;}
                if(collided){
                    // 只标记，循环结束后统一清理
                    o.dead=true;for(Orb b:orbs)if(b.blue)b.dead=true;
                    guiding.remove(o.owner.getUUID());unlimited(o.owner,o.pos);continue;
                }
            }
            boolean hitSomething=false;
            for(LivingEntity victim:Skills.targets(o.level,o.owner,o.pos,5)){
                if(victim.getBoundingBox().inflate(0.7).distanceToSqr(o.pos)>2.25)continue;
                if(o.purple){
                    if(!o.struck.add(victim.getUUID()))continue; // 茈：同一只只结算一次
                    // 命中演出：变大 + 渐隐，纯视觉效果，不再附带溅射伤害
                    fx(o.owner,GojoFx.PURPLE_BURST,o.pos,Vec3.ZERO,12,30,o.id);
                    // 茈 500：打魔虚罗 ×10（{@link #MAHORAGA_DAMAGE}），并且命中后给它挂 30 秒缓慢
                    boolean hurt=hit(o.owner,victim,500,false,MAHORAGA_DAMAGE,AttackTypes.MAGIC_PURPLE);
                    if(hurt)purpleSlow(victim);
                    // 一下打死 → 继续飞（穿透，去撞后面的人）；一下打不死 → 引爆：补 25% 伤害 + 宇宙大爆炸 + 自身消散
                    if(hurt&&victim.isAlive()&&victim.getHealth()>0){
                        hit(o.owner,victim,500*PURPLE_BANG_BONUS,false,MAHORAGA_DAMAGE,AttackTypes.MAGIC_PURPLE);
                        o.level.playSound(null,o.pos.x,o.pos.y,o.pos.z,SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,1.5f,0.55f);
                        fx(o.owner,GojoFx.BIG_BANG,o.pos,Vec3.ZERO,PURPLE_BANG_SIZE,40,o.id);
                        o.dead=true;
                        break;
                    }
                    continue;
                }
                fx(o.owner,GojoFx.RED_END,o.pos,Vec3.ZERO,7,30,o.id);
                // 赫：击中即爆炸 —— 半径 RED_RADIUS 格内所有目标都吃伤害 + 击退（用户要求：范围伤害）
                redBurst(o.owner,o.pos,o.level);
                hitSomething=true;break; // 赫：打到就停
            }
            if(o.age>=o.life||(hitSomething&&!o.purple)){ // 茈只在飞满生命周期后消散
                if(!hitSomething&&!o.purple)fx(o.owner,GojoFx.RED_END,o.pos,Vec3.ZERO,0,4,o.id); // 自然消散：只消失，不爆
                if(o.purple)fx(o.owner,GojoFx.PURPLE_BURST,o.pos,Vec3.ZERO,0,4,o.id);
                it.remove();
            }
        }
        if(orbs.removeIf(o->o.dead))guiding.values().removeIf(o->o.dead);
        for(Iterator<Beam> it=beams.iterator();it.hasNext();){
            Beam b=it.next();it.remove();
            Vec3 end=b.from().add(b.dir().scale(b.length()));
            for(LivingEntity victim:Skills.targets(b.level(),b.owner(),b.from(),b.length()+2)){
                if(victim.getBoundingBox().inflate(0.7).clip(b.from(),end).isEmpty()&&!victim.getBoundingBox().inflate(0.7).contains(b.from()))continue;
                if(hit(b.owner(),victim,b.damage(),false)){
                    Vec3 push=b.dir().multiply(1,0,1).normalize();
                    victim.push(push.x*6,2.4,push.z*6);victim.hurtMarked=true;
                }
            }
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){states.clear();orbs.clear();beams.clear();}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){states.remove(p.getUUID());orbs.removeIf(o->o.owner==p);beams.removeIf(b->b.owner()==p);}}
    public static void cancel(ServerPlayer p){State s=states.remove(p.getUUID());if(s!=null)s.casting=-1;Skills.endVoidSeal(p);orbs.removeIf(o->o.owner==p);beams.removeIf(b->b.owner()==p);}
    private GojoCombat(){}
}
