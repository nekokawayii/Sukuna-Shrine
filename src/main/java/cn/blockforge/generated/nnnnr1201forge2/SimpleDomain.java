package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 捌的分支「**简易领域 · 捌**」：**长按释放**的贴身简易领域。
 *
 *  <p>按《使用说明》的写法实现：</p>
 *  <ul>
 *    <li>以玩家为中心、**半径 5 格、高 5 格**的圆柱内出现**无数细小斩击**（每刻补一批随机位置的短线）；</li>
 *    <li>范围内**碰到斩击特效的敌人**：被**击退 6 格**，同时**向它的后方放一道剑气**（从敌人背后射出去）；</li>
 *    <li>斩击伤害 = **御厨子的 10%**（御厨子对单体是每 2 刻 1 刀 ×{@link Curse#power}，即每秒 10 次，
 *        所以 10% = 每秒 1 次 {@code power}，这里按 5 刻一跳结算）；</li>
 *    <li>**长按来释放**：按住期间领域持续存在并持续结算；</li>
 *    <li>**冷却 = 释放时间的 70%**（长按 1 秒 → 冷却 0.7 秒）；</li>
 *    <li>释放时**减速 50%**、**两手上举**（姿态走 {@link SimpleDomainPose}）。</li>
 *  </ul> */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="sukuna")
public final class SimpleDomain {
    /** 技能号（捌的新分支，挂在轮盘 side 列表里）。
     *  **注意：不能用 33** —— 33 是 {@code FinaleRules.SKILL}（终式），
     *  {@code TechniqueInput.receive} 第一段就把 33 整包交给终式处理并 return，
     *  简易领域永远收不到输入（这就是"内部代码与终式冲突"的根因）。
     *  已占用：0-32 宿傩/御厨子分支、33 终式、34-41 六眼九式、42 神武解充能；所以取 43。 */
    public static final int SKILL=43;
    /** 领域尺寸：半径 5 格、高 5 格。 */
    public static final double RADIUS=5,HEIGHT=5;
    /** 击退距离（格）与抬升。 */
    public static final double KNOCKBACK=6,KNOCK_LIFT=0.35;
    /** 伤害：御厨子对单体每秒 10 刀 × power，这里取 10%。 */
    /** 每 0.1 秒（2 刻）对范围内每个目标造成的伤害：**2 点**。 */
    public static final double UNLIMITED_DAMAGE=2;
    /** 保留旧常量名，供说明/其它引用。 */
    public static final double MAHITO_SHARE=0.10;
    /** 每 5 刻结算一跳（= 每秒 1 次 power）。 */
    public static final int DAMAGE_INTERVAL=5;
    /** 减速 50%：缓慢 II 持续 10 刻，长按时每 10 刻续一次。 */
    public static final int SLOW_REFRESH=10;
    /** 冷却 = 释放时间 × 70%，且有下限，避免"点一下就好"。 */
    public static final double COOLDOWN_SHARE=0.70;
    /** 冷却地板：只防止"零冷却连按"，2 刻 = 0.1 秒；旧值 10 刻（0.5 秒）会把轻点也锁半秒，表现为"卡冷却"。 */
    public static final int COOLDOWN_MIN=2;
    /** 每刻在圆柱内补几道随机斩击（r155 的节奏：一批一批出）。 */
    public static final int CUTS_PER_TICK=15;   // 再减半（60 → 30 → 15）
    /** 每道斩击的尺寸（长度）与寿命。 */
    public static final float CUT_SIZE=10f;
    public static final int CUT_LIFE=12;   // 与 r155 一致
    /** 斩击本身的伤害系数：0 = 只作表现（伤害由 {@link #DAMAGE_INTERVAL} 那条 tick 结算）。 */
    public static final float CUT_DAMAGE_SCALE=0f;
    /** 单次释放的最长时长（刻），防止无限按住。 */
    /** 长按**无上限**：想按多久按多久，冷却严格跟着实际长按时间走（见 {@link #COOLDOWN_SHARE}）。
     *  这里保留一个极宽的安全阀（1 小时），只用来兜住"死亡/断线判定失效"这类异常，不参与正常玩法。 */
    public static final int MAX_HOLD=20*60*60;

    private static final class Field {
        final ServerPlayer p;final ServerLevel level;final UUID body;
        final long start;long last;int nextDamage;int slowUntil;int hits;
        /** 客户端明确松手/取消后置位：只有它为 true 才允许"非显式"退出。 */
        boolean released;
        Field(ServerPlayer p){this.p=p;level=p.serverLevel();body=MaidBridge.body(p).getUUID();start=level.getGameTime();last=start;}
        Vec3 center(){return MaidBridge.body(p).position();}
    }
    private static final Map<UUID,Field> fields=new HashMap<>();
    public static boolean active(ServerPlayer p){return fields.containsKey(p.getUUID());}
    /** 简易领域"接管中"：正在维持**或**它的冷却还没走完。
     *  这段时间内不应该再让宿傩六式（解/捌/开/灶/领域展开…）掺进来——它们的通用冷却
     *  （{@code Skills.cast} 的 times[]）会弹出"XX 冷却中"，而简易领域本身压根不走那套冷却。 */
    public static boolean owns(ServerPlayer p){return active(p)||cooldownLeft(p)>0;}
    /** 冷却剩余刻数（由"释放时间 ×70%"算出，写在 Curse 的长期键上）。 */
    public static int cooldownLeft(ServerPlayer p){return (int)Math.max(0,Curse.data(p).getLong("SimpleDomainCooldown")-p.level().getGameTime());}

    /** 长按开始：只有满足解锁/咒力才起手；缓存在 holds 里由 {@link TechniqueInput} 心跳续租。 */
    public static boolean start(ServerPlayer p){
        if(active(p))return true;
        if(!TechniqueInput.allowed(p))return false;
        if(cooldownLeft(p)>0){CastFeedback.tell(p,"简易领域 · 捌 冷却中（"+((cooldownLeft(p)+19)/20)+" 秒）");return false;}
        if(!Journey.unlocked(p)||Curse.fingers(p)<5){CastFeedback.tell(p,"简易领域 · 捌 需要先吃下 5 根宿指");return false;}
        if(!Curse.spend(p,Curse.COST[0]))return false;
        Field f=new Field(p);fields.put(p.getUUID(),f);
        MaidBridge.body(p).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,SLOW_REFRESH,1,false,false,false));
        p.displayClientMessage(Component.literal("简易领域 · 捌（长按维持 · 松手收招）").setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFF9AD6FF)),true);
        level(p).playSound(null,p.blockPosition(),SoundEvents.WARDEN_SONIC_CHARGE,SoundSource.PLAYERS,0.7f,1.6f);
        return true;
    }
    private static ServerLevel level(ServerPlayer p){return p.serverLevel();}

    /** 长按心跳：续减速、补斩击、按节奏结算伤害（r155 的通路：这些都挂在心跳上）。 */
    public static void hold(ServerPlayer p){
        Field f=fields.get(p.getUUID());
        if(f==null){start(p);return;}
        LivingEntity body=MaidBridge.body(p);
        if(f.level.getGameTime()-f.start>MAX_HOLD){release(p,"安全阀");return;}
        if(!body.isAlive()||body.level()!=f.level||!TechniqueInput.allowed(p)){
            // 心跳时的校验失败**不再直接收招**（以前会在这里退出并写入冷却）：交给 tick 的死亡/显式松手判据处理。
            SdLog.w("SD.hold 校验失败（仅记录，不收招）alive="+body.isAlive()+" allowed="+TechniqueInput.allowed(p));
            return;
        }
        body.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,SLOW_REFRESH,1,false,false,false));
        f.slowUntil=(int)(f.level.getGameTime()+SLOW_REFRESH);
        net.minecraft.util.RandomSource rnd=p.getRandom();
        Vec3 center=f.center();
        for(int i=0;i<CUTS_PER_TICK;i++){
            double a=rnd.nextDouble()*Math.PI*2,r=RADIUS*Math.sqrt(rnd.nextDouble());
            Vec3 at=new Vec3(center.x+Math.cos(a)*r,center.y+rnd.nextDouble()*HEIGHT,center.z+Math.sin(a)*r);
            Skills.domainSlashRandom(p,at,CUT_SIZE,CUT_LIFE);
        }
        bend(p,f);
    }
    /** 领域每刻逻辑：范围伤害（每 0.1 秒 2 点）+ 击退 6 格 + 敌背后补一道剑气。**不看任何视觉**。 */
    static void bend(ServerPlayer p,Field f){
        Vec3 center=f.center();
        long now=f.level.getGameTime();
        if(now<f.nextDamage)return;
        f.nextDamage=(int)(now+DAMAGE_INTERVAL);
        double damage=UNLIMITED_DAMAGE; // 每 0.1 秒 2 点（与御厨子同模型：范围 tick 命中）
        for(LivingEntity t:Skills.targets(f.level,p,center,RADIUS+1)){
            Vec3 rel=t.position().subtract(center);
            double flat=Math.hypot(rel.x,rel.z);
            if(flat>RADIUS||rel.y<-1||rel.y>HEIGHT)continue;
            // 击退 6 格：直接位移 + 速度补偿，做到"被推出去 6 格"而不是轻轻一推
            Vec3 push=new Vec3(rel.x,0,rel.z);
            if(push.lengthSqr()<1e-4)push=new Vec3(Math.cos(p.getRandom().nextDouble()*6.283),0,Math.sin(p.getRandom().nextDouble()*6.283));
            push=push.normalize();
            BossTargets.move(t,t.position().add(push.scale(Math.min(KNOCKBACK,Math.max(1.5,flat>0.4?KNOCKBACK-flat*0.4:KNOCKBACK)))));
            t.setDeltaMovement(push.x*1.4,Math.max(0.25,KNOCK_LIFT),push.z*1.4);t.hurtMarked=true;
            AttackTypes.mark(AttackTypes.CLEAVE);
            CombatSettings.hurt(p,t,f.level.damageSources().playerAttack(p),BalanceRules.damage(damage));
            // 向"它的后方"补一道剑气：从敌人背后沿着击退方向射出去
            Vec3 behind=t.position().subtract(push.scale(1.2)).add(0,Math.min(1.2,t.getBbHeight()*0.6),0);
            SlashForms.directed(p,behind,push,26);
            f.hits++;
        }
    }

    /** 松手收招：结算冷却（= 本次释放时间 ×70%），清掉减速与领域。 */
    public static void release(ServerPlayer p){release(p,"未标注");}
    public static void release(ServerPlayer p,String why){
        Field f=fields.remove(p.getUUID());
        if(f==null)return;
        f.released=true;
        long heldTicks=f.level.getGameTime()-f.start;
        SdLog.w("SD.release 原因="+why+" 持续="+heldTicks+"刻");
        long held=Math.max(1,f.level.getGameTime()-f.start);
        int cooldown=Math.max(COOLDOWN_MIN,(int)Math.round(held*COOLDOWN_SHARE)); // 释放时间 ×70%，仅保留 2 刻地板
        Curse.data(p).putLong("SimpleDomainCooldown",f.level.getGameTime()+cooldown);
        MaidBridge.body(p).removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        f.level.playSound(null,p.blockPosition(),SoundEvents.WARDEN_SONIC_BOOM,SoundSource.PLAYERS,0.8f,1.3f);
        p.displayClientMessage(Component.literal("简易领域 · 捌 收招 · 持续 "+String.format(Locale.ROOT,"%.1f",held/20.0)+" 秒 · 冷却 "
            +String.format(Locale.ROOT,"%.1f",cooldown/20.0)+" 秒 · 命中 "+f.hits+" 次").setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFF9AD6FF)),true);
    }
    /** 死亡/下线/维度切换等强制中断：按当前时长结算冷却（与松手一致）。 */
    public static void cancel(ServerPlayer p){release(p);}
    /** 服务端每刻：**唯一的退出判据就是"客户端明确松手"（released）**，外加死亡/跨维度/安全阀。
     *
     *  <p>这里曾经还有一条 `心跳断了 40 刻` 的判定 —— 那是遗留的心跳依赖：客户端每 10 刻发一次心跳，
     *  再加上一个包的处理延迟，判定会在 **41 刻（2.05 秒）** 稳定触发，于是长按总是在 2.1 秒被强制收招。
     *  现在彻底删掉：丢包、延迟、心跳间隔都不再能结束技能。</p> */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent e){
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var server=e.getServer();if(server==null||fields.isEmpty())return;
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            Field f=fields.get(p.getUUID());
            if(f==null)continue;
            LivingEntity body=MaidBridge.body(p);
            boolean gone=!body.isAlive()||body.level()!=f.level||f.level.getGameTime()-f.start>MAX_HOLD||f.released;
            if(gone){release(p,"tick alive="+body.isAlive()+" 跨维度="+(body.level()!=f.level)+" 松手="+f.released);continue;}
            hold(p); // 每刻真正执行领域逻辑（减速/斩痕/伤害），与心跳包是否到达无关
        }
    }
    private SimpleDomain(){}
}
