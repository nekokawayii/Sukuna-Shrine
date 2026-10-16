package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 开 的终盘分支「**灶 · 最大功率 · 开**」（技能号 44）。
 *
 *  <p>用户口径：</p>
 *  <ul>
 *    <li>**长按蓄力 20 秒**；期间**无法移动**，**前 10 秒每秒向上飞 1 格**，周围 **15 格**的敌人**燃烧**；</li>
 *    <li>蓄力时**周身出现火焰，汇聚成一把玩家两倍大的弓**（客户端 Photon 表现，见 {@link FurnaceMaxClient}）；</li>
 *    <li>**松开或受伤取消**，取消后冷却 **30 秒**；（不满 20 秒就松手 = 取消，满 20 秒自动打出）</li>
 *    <li>打出的弓矢命中后：**半径 100 格**的火焰爆炸，随后**火柱冲天**并**缓慢熄灭 40 秒**，
 *        熄灭期间柱内所有生物**每秒 150 点**；</li>
 *    <li>全部特效由 Photon 制作。</li>
 *  </ul>
 *
 *  <p>与其它技能的关系：这是「开」的一支，**灶/开系对魔虚罗 ×20**的规则照旧生效
 *  （{@link MahoragaRules#bossDamage}）；蓄力期间接管输入，宿傩六式一律不理（同简易领域的做法）。</p> */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="sukuna")
public final class FurnaceMax {
    /** 技能号（开的分支，挂在轮盘 side 列表里）。已占用到 43（简易领域），所以取 44。 */
    public static final int SKILL=44;
    public static final String NAME="灶 · 最大功率 · 开",SHORT="灶·最大·开";
    /** 一次性的施法消耗（轮盘上显示的那个数）。 */
    public static final int COST=600;

    /** 蓄力总时长与"上升"段：20 秒，前 10 秒每秒抬 1 格。 */
    public static final int CHARGE_TICKS=20*20,RISE_TICKS=20*10,RISE_INTERVAL=20;
    public static final double RISE_STEP=1.0;
    /** 蓄力期间周围多少格的敌人被点燃、烧多久。 */
    public static final double BURN_RADIUS=15.0;
    /** 蓄力时周围敌人的"燃烧"：**每秒一跳火焰伤害 + 客户端 Photon 火云**。
     *  旧版用 {@code setSecondsOnFire}，会在怪身上刷原版火焰粒子——用户要求"不要原版粒子"，已改掉。 */
    public static final float BURN_DPS=8f;
    public static final int BURN_FX_TICKS=25;
    /** 中断（松手/受伤/异常）冷却 30 秒；打出去之后 60 秒。 */
    public static final int CANCEL_COOLDOWN=20*30,FIRE_COOLDOWN=20*60;
    /** 心跳超过这么久没到就认为客户端没了（只作兜底，正常 10 刻一次）。 */
    public static final int HEARTBEAT_TIMEOUT=100;

    /** 弓矢：每刻 4 格、最多飞 100 格；命中点半径 100 格的火焰爆炸。 */
    public static final double SHOT_STEP=4.0,SHOT_RANGE=100.0,BLAST_RADIUS=100.0;
    /** 爆炸伤害原值（与茈 500 / 无限制茈 999 同一口径，再走 {@link CombatSettings#spellDamage}）。 */
    public static final float BLAST_DAMAGE=2000f;
    /** **点名机制**：这一招打魔虚罗时的固定伤害（绕过它的适应/免疫，走真伤类型）。 */
    public static final float MAHORAGA_DAMAGE=100000f;
    /** 火柱：烧 15 秒，柱内每秒 150 点（原值）、半径 12 格；烧完缩成红球消失（客户端表现）。 */
    public static final int PILLAR_TICKS=20*15;
    public static final double PILLAR_RADIUS=12.0;
    public static final float PILLAR_DPS=150f;
    /** 蓄力时"周身方块被镀上橙色"的作用半径（客户端表现，见 {@link FurnaceMaxClient}）。 */
    public static final double FIELD_RADIUS=60;

    /** 特效号（客户端 {@link Client#receive} 全段交给 {@link FurnaceMaxClient}）。 */
    public static final int FX_CHARGE=230,FX_SHOT=231,FX_BLAST=232,FX_PILLAR=233,FX_BURN=234;
    private static final String COOLDOWN_KEY="FurnaceMaxCooldown";
    /** 「蓄力期间不能移动」用的移速修饰符：×0（直接改属性，不用缓慢效果）。 */
    private static final UUID FREEZE_ID=UUID.fromString("1f4c7a52-8d33-4a11-9c07-77b2e5d0a902");

    private static final class Charge {
        final ServerPlayer p;final ServerLevel level;final long start;
        /** 起手前的失重状态：和动作解/终式一样，结束时**还原**而不是一律设回 false。 */
        final boolean gravity;
        long heartbeat,nextRise,nextBurn,nextFx;
        Charge(ServerPlayer p){this.p=p;level=p.serverLevel();start=heartbeat=level.getGameTime();nextRise=start+RISE_INTERVAL;nextBurn=start+20;nextFx=start;gravity=MaidBridge.body(p).isNoGravity();}
    }
    private static final class Shot {
        final ServerPlayer p;final ServerLevel level;final Vec3 dir;Vec3 pos;double travelled;
        Shot(ServerPlayer p,ServerLevel l,Vec3 pos,Vec3 dir){this.p=p;level=l;this.pos=pos;this.dir=dir;}
    }
    private static final class Pillar {
        final ServerPlayer p;final ServerLevel level;final Vec3 center;final long end;long nextDamage;
        Pillar(ServerPlayer p,ServerLevel l,Vec3 c,long end){this.p=p;level=l;center=c;this.end=end;nextDamage=l.getGameTime();}
    }
    private static final Map<UUID,Charge> charges=new HashMap<>();
    private static final List<Shot> shots=new ArrayList<>();
    private static final List<Pillar> pillars=new ArrayList<>();

    public static boolean active(ServerPlayer p){return charges.containsKey(p.getUUID());}
    /** 冷却剩余刻数（松手/受伤 30 秒、打出去 60 秒，都写在同一把键上）。 */
    public static int cooldownLeft(ServerPlayer p){
        return (int)Math.max(0,Curse.data(p).getLong(COOLDOWN_KEY)-p.level().getGameTime());
    }
    private static void cooldown(ServerPlayer p,int ticks){
        Curse.data(p).putLong(COOLDOWN_KEY,p.level().getGameTime()+ticks);
    }

    // ------------------------------------------------------------------ 蓄力
    /** 起手：只有解锁、不在冷却、付得起咒力才成立。 */
    public static boolean start(ServerPlayer p){
        if(active(p))return true;
        if(!TechniqueInput.allowed(p))return false;
        if(cooldownLeft(p)>0){CastFeedback.tell(p,NAME+" 冷却中（"+((cooldownLeft(p)+19)/20)+" 秒）");return false;}
        if(!VoidJourney.evolved(p)){CastFeedback.tell(p,NAME+" 需要解锁虚空御厨子阶段");return false;}
        if(!Curse.spend(p,COST))return false;
        Charge c=new Charge(p);charges.put(p.getUUID(),c);
        freeze(MaidBridge.body(p));
        // 失重：前 10 秒靠 teleport 一格格抬升，之后原地悬停。
        // 不这么做的话，抬到 10 格后重力会把人拽下来，摔伤立刻触发"受伤取消"。
        MaidBridge.body(p).setNoGravity(true);
        sendFx(p,MaidBridge.body(p),0,CHARGE_TICKS);
        p.displayClientMessage(Component.literal(NAME+" · 蓄力 20 秒 · 期间无法移动 · 松手或受伤中断（30 秒冷却）")
            .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFFFF7A2A)),true);
        SkillSounds.play(p,"flame_bow");
        return true;
    }
    /** 心跳：只续租（客户端每 10 刻一次），真正的每刻逻辑在 {@link #tick} 里。 */
    public static void heartbeat(ServerPlayer p){
        Charge c=charges.get(p.getUUID());
        if(c!=null)c.heartbeat=p.level().getGameTime();
    }
    /** 松手：满 20 秒就是打出，不满 20 秒算取消（30 秒冷却）。 */
    public static void release(ServerPlayer p){
        Charge c=charges.get(p.getUUID());
        if(c==null)return;
        if(p.level().getGameTime()-c.start>=CHARGE_TICKS)fire(p,c);
        else cancel(p,"提前松手");
    }
    /** 客户端取消（失焦/换手/主动取消）。 */
    public static void cancel(ServerPlayer p){cancel(p,"客户端取消");}
    public static void cancel(ServerPlayer p,String why){
        Charge c=charges.remove(p.getUUID());
        if(c==null)return;
        unfreeze(MaidBridge.body(p));
        MaidBridge.body(p).setNoGravity(c.gravity);
        cooldown(p,CANCEL_COOLDOWN);
        stopChargeFx(c);
        CastFeedback.tell(p,NAME+" 蓄力中断（"+why+"）· 冷却 30 秒");
    }

    private static void freeze(LivingEntity body){
        AttributeInstance attr=body.getAttribute(Attributes.MOVEMENT_SPEED);
        if(attr==null)return;
        attr.removeModifier(FREEZE_ID);
        attr.addTransientModifier(new AttributeModifier(FREEZE_ID,"sukuna_furnace_max_charge",-1.0,
            AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    private static void unfreeze(LivingEntity body){
        AttributeInstance attr=body.getAttribute(Attributes.MOVEMENT_SPEED);
        if(attr!=null&&attr.getModifier(FREEZE_ID)!=null)attr.removeModifier(FREEZE_ID);
    }

    // ------------------------------------------------------------------ 打出
    private static void fire(ServerPlayer p,Charge c){
        charges.remove(p.getUUID());
        LivingEntity body=MaidBridge.body(p);
        unfreeze(body);
        body.setNoGravity(c.gravity);
        body.fallDistance=0;   // 释放瞬间落地不吃摔伤
        stopChargeFx(c);
        Vec3 dir=body.getLookAngle();if(dir.lengthSqr()<1e-6)dir=new Vec3(0,0,1);
        shots.add(new Shot(p,c.level,body.getEyePosition().add(dir.normalize().scale(1.5)),dir.normalize()));
        cooldown(p,FIRE_COOLDOWN);
        p.displayClientMessage(Component.literal(NAME+" · 最大功率释放").setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFFFF7A2A)),true);
    }
    /** 弓矢命中点：半径 100 格的火焰爆炸，并在命中点留下 15 秒的"冻结爆炸"。 */
    private static void blast(ServerPlayer p,ServerLevel level,Vec3 center){
        long now=level.getGameTime();
        for(LivingEntity t:Skills.targets(level,p,center,BLAST_RADIUS)){
            double d=Math.sqrt(t.position().distanceToSqr(center));if(d>BLAST_RADIUS)continue;
            AttackTypes.mark(AttackTypes.FIRE);
            t.invulnerableTime=0;
            // **点名机制**：对魔虚罗固定 100000 伤害，且绕过它自己的"适应/免疫"
            // （不绕的话法轮转满就会判 0，等于白打）
            if(t instanceof MahoragaEntity mahoraga){
                mahoraga.forceDamage(level.damageSources().genericKill(),MAHORAGA_DAMAGE);
                p.displayClientMessage(Component.literal(NAME+" · 对魔虚罗造成 "+BalanceRules.number(MAHORAGA_DAMAGE)+" 伤害")
                    .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFFFF7A2A)),true);
                burnFx(level,t);
                continue;
            }
            double falloff=1-0.5*Math.min(1,d/BLAST_RADIUS);   // 近处满伤、边缘半伤
            float amount=CombatSettings.spellDamage(p,BLAST_DAMAGE*(float)Curse.power(p)*(float)falloff
                *MahoragaRules.bossDamage(t,MahoragaRules.FURNACE_MULTIPLIER));
            CombatSettings.hurt(p,t,level.damageSources().explosion(p,p),amount);
            burnFx(level,t);   // 幸存者身上挂 2 秒 Photon 火云（不用原版着火）
        }
        Network.send(level,new Network.Effect(FX_BLAST,p.getId(),center,Vec3.ZERO,(float)BLAST_RADIUS,0,0));
        level.playSound(null,center.x,center.y,center.z,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
            net.minecraft.sounds.SoundSource.PLAYERS,6f,0.45f);
        SkillSounds.play(p,"inferno");
        // 每人只保留最新的一根：旧柱子立刻收掉表现（伤害结算也随之停止）
        for(Iterator<Pillar> it=pillars.iterator();it.hasNext();){
            Pillar q=it.next();
            if(q.p==p){Network.send(q.level,new Network.Effect(FX_PILLAR,p.getId(),q.center,Vec3.ZERO,(float)PILLAR_RADIUS,0,0));it.remove();}
        }
        pillars.add(new Pillar(p,level,center,now+PILLAR_TICKS));
        Network.send(level,new Network.Effect(FX_PILLAR,p.getId(),center,Vec3.ZERO,(float)PILLAR_RADIUS,PILLAR_TICKS,0));
    }

    // ------------------------------------------------------------------ 每刻
    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var server=e.getServer();if(server==null)return;
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            Charge c=charges.get(p.getUUID());
            if(c==null){unfreeze(MaidBridge.body(p));continue;}   // 兜底：异常路径漏下的"不能移动"在这里摘掉
            tickCharge(p,c);
        }
        tickShots();
        tickPillars();
    }
    private static void tickCharge(ServerPlayer p,Charge c){
        LivingEntity body=MaidBridge.body(p);
        if(!body.isAlive()||body.level()!=c.level||!TechniqueInput.allowed(p)){cancel(p,"状态失效");return;}
        long now=c.level.getGameTime();
        if(now-c.heartbeat>HEARTBEAT_TIMEOUT){cancel(p,"客户端失联");return;}
        freeze(body);   // 每刻重挂一次（幂等）
        long elapsed=now-c.start;
        // 前 10 秒：每秒抬 1 格
        if(elapsed<=RISE_TICKS&&now>=c.nextRise){
            c.nextRise=now+RISE_INTERVAL;
            BossTargets.move(body,body.position().add(0,RISE_STEP,0));
        }
        // 周围 15 格敌人燃烧：伤害走我们自己的火焰跳伤，表现交给客户端 Photon 火云（不出原版火焰粒子）
        if(now>=c.nextBurn){
            c.nextBurn=now+20;
            for(LivingEntity t:Skills.targets(c.level,p,body.position(),BURN_RADIUS)){
                AttackTypes.mark(AttackTypes.FIRE);
                t.invulnerableTime=0;
                CombatSettings.hurt(p,t,c.level.damageSources().onFire(),CombatSettings.spellDamage(p,BURN_DPS));
                burnFx(c.level,t);
            }
        }
        // 蓄力表现：每 4 刻同步一次进度与剩余时间
        if(now>=c.nextFx){
            c.nextFx=now+4;
            sendFx(p,body,Math.min(1f,elapsed/(float)CHARGE_TICKS),(int)Math.max(0,CHARGE_TICKS-elapsed));
        }
        if(elapsed>=CHARGE_TICKS)fire(p,c);
    }
    private static void tickShots(){
        for(Iterator<Shot> it=shots.iterator();it.hasNext();){
            Shot s=it.next();
            if(!s.p.isAlive()||s.p.isRemoved()||s.p.level()!=s.level){it.remove();continue;}
            Vec3 next=s.pos.add(s.dir.scale(SHOT_STEP));
            BlockHitResult block=s.level.clip(new ClipContext(s.pos,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.p));
            LivingEntity victim=null;
            for(LivingEntity t:Skills.targets(s.level,s.p,s.pos,4))
                if(t.getBoundingBox().inflate(1).clip(s.pos,next).isPresent()){victim=t;break;}
            Vec3 center=next;
            if(block.getType()!=HitResult.Type.MISS&&victim==null)center=block.getLocation();
            if(victim!=null)center=victim.getBoundingBox().getCenter();
            Network.send(s.level,new Network.Effect(FX_SHOT,s.p.getId(),center,s.dir,1,2,0));
            s.travelled+=SHOT_STEP;
            boolean stop=victim!=null||block.getType()!=HitResult.Type.MISS||s.travelled>=SHOT_RANGE;
            if(stop){it.remove();blast(s.p,s.level,center);}
            else s.pos=next;
        }
    }
    private static void tickPillars(){
        for(Iterator<Pillar> it=pillars.iterator();it.hasNext();){
            Pillar q=it.next();
            if(!q.p.isAlive()||q.p.level()!=q.level){it.remove();continue;}
            long now=q.level.getGameTime();
            if(now>=q.end){
                Network.send(q.level,new Network.Effect(FX_PILLAR,q.p.getId(),q.center,Vec3.ZERO,(float)PILLAR_RADIUS,0,0));
                it.remove();continue;
            }
            if(now<q.nextDamage)continue;
            q.nextDamage=now+20;
            for(LivingEntity t:Skills.targets(q.level,q.p,q.center,PILLAR_RADIUS)){
                AttackTypes.mark(AttackTypes.FIRE);
                t.invulnerableTime=0;
                CombatSettings.hurt(q.p,t,q.level.damageSources().explosion(q.p,q.p),
                    CombatSettings.spellDamage(q.p,PILLAR_DPS*MahoragaRules.bossDamage(t,MahoragaRules.FURNACE_MULTIPLIER)));
            }
            Network.send(q.level,new Network.Effect(FX_PILLAR,q.p.getId(),q.center,Vec3.ZERO,(float)PILLAR_RADIUS,(int)(q.end-now),0));
        }
    }
    private static void sendFx(ServerPlayer p,LivingEntity body,float progress,int remaining){
        Network.send(p.serverLevel(),new Network.Effect(FX_CHARGE,body.getId(),body.position(),body.getLookAngle(),progress,remaining,0));
    }
    /** 让客户端在这只生物身上挂一圈 Photon 火云：**纯 Photon 表现**，不刷原版火焰粒子。 */
    private static void burnFx(ServerLevel level,LivingEntity target){
        Network.send(level,new Network.Effect(FX_BURN,target.getId(),target.position(),Vec3.ZERO,
            (float)Math.max(1.0,target.getBbHeight()),BURN_FX_TICKS,0));
    }
    private static void stopChargeFx(Charge c){
        Network.send(c.level,new Network.Effect(FX_CHARGE,c.p.getId(),MaidBridge.body(c.p).position(),Vec3.ZERO,1,0,0));
    }
    /** 受伤即中断（30 秒冷却）：只有正在蓄力的那个身体算数。 */
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void hurt(LivingHurtEvent event){
        if(event.getAmount()<=0)return;
        LivingEntity victim=event.getEntity();
        ServerPlayer p=victim instanceof ServerPlayer self?self:MaidBridge.progression(victim);
        if(p==null||!charges.containsKey(p.getUUID()))return;
        cancel(p,"受伤");
    }
    /** 死亡/下线/换维度一律中断。 */
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&charges.containsKey(p.getUUID()))cancel(p,"下线");
    }
    @SubscribeEvent public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&charges.containsKey(p.getUUID()))cancel(p,"跨维度");
    }
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){
        charges.clear();shots.clear();pillars.clear();
    }
    private FurnaceMax(){}
}
