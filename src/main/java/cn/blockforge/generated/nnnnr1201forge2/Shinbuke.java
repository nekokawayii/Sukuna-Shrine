package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 咒具「神武解」：左键近战 15 伤害，右键打出**面前扇形 15 格**的彩色雷击。
 *
 *  <p>雷击伤害 = {@link #STRIKE_DAMAGE}（基准 15 的 **20 倍** = 300 原值），走模组统一的
 *  {@code BalanceRules} / {@code CombatSettings.hurt} 结算，并按 {@link AttackTypes#LIGHTNING}
 *  归类（魔虚罗的"雷电适应"吃这一条）。</p> */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="sukuna")
public final class Shinbuke {
    /** 左键近战伤害（原值）。 */
    public static final double MELEE_DAMAGE=15;
    /** 右键雷击伤害倍率与最终值：15 × 20 = 300。 */
    public static final double STRIKE_MULTIPLIER=20,STRIKE_DAMAGE=MELEE_DAMAGE*STRIKE_MULTIPLIER;
    /** 扇形：面前 30 格、半角 45°（合计 90°）。 */
    public static final double RANGE=30,HALF_ANGLE=Math.PI/4;
    /** 右键冷却（刻）。 */
    public static final int COOLDOWN=60;
    /** 充能：**一秒内加 50 次、每次 1 点**（即逐刻 +1），每满 {@link #CHARGE_STEP} 点范围与伤害各 +25%，
     *  同时**本体近战伤害永久 +10%**。 */
    public static final int CHARGE_ID=42,CHARGE_PER_TICK=1,CHARGE_STEP=100,CHARGE_MAX=2000;
    /** 每秒充能次数（= 20 刻 × {@link #CHARGE_PER_TICK}）。 */
    public static final int CHARGE_PER_SECOND=CHARGE_PER_TICK*20;
    /** 每档加成：范围 +25%、伤害 +25%、近战永久 +10%。 */
    public static final double RANGE_PER_STEP=0.25,DAMAGE_PER_STEP=0.25,MELEE_PERMANENT_PER_STEP=0.10;
    /** 玩家存档键：累计充能档数（近战永久加成用）。 */
    public static final String STEPS_KEY="ShinbukeSteps";
    /** 充能过程（只在服务端 tick 里推进）。 */
    private static final java.util.Map<java.util.UUID,int[]> charging=new java.util.concurrent.ConcurrentHashMap<>();
    /** 是否手持神武解（主手或副手）。 */
    public static boolean holding(net.minecraft.world.entity.LivingEntity p){
        return p.getMainHandItem().is(VoidContent.SHINBUKE.get())||p.getOffhandItem().is(VoidContent.SHINBUKE.get());
    }
    /** 已累计的充能档数（永久近战加成 = 档数 × 10%）。 */
    public static int steps(ServerPlayer p){return Math.max(0,Curse.data(p).getInt(STEPS_KEY));}
    /** 本体近战伤害（含永久加成）。 */
    public static double meleeDamage(ServerPlayer p){return MELEE_DAMAGE*(1+steps(p)*MELEE_PERMANENT_PER_STEP);}
    /** 充能期间的加成：范围 ×(1+25%/档)、伤害 ×(1+25%/档)。 */
    public static double rangeFactor(int charge){return 1+RANGE_PER_STEP*(charge/CHARGE_STEP);}
    public static double damageFactor(int charge){return 1+DAMAGE_PER_STEP*(charge/CHARGE_STEP);}
    /** 服务端：当前充能值。 */
    public static int chargeOf(ServerPlayer p){int[] v=charging.get(p.getUUID());return v==null?0:v[0];}
    public static boolean isCharging(ServerPlayer p){return charging.containsKey(p.getUUID());}
    /** 客户端：服务端同步过来的充能值（HUD 用；本地玩家不是 ServerPlayer，查不到服务端的表）。 */
    private static volatile int clientCharge;
    public static int clientCharge(){return clientCharge;}
    public static void setClientCharge(int value){clientCharge=Math.max(0,Math.min(CHARGE_MAX,value));}
    /** 右键：立刻以"当前充能"打一发（不充能时就是基础扇形）。 */
    /** 右键：优先释放已经蓄好的充能；没有存量就是基础扇形一发。 */
    public static boolean strike(ServerPlayer p){return fire(p);}
    /** 是否已停止充能、等待右键释放。 */
    public static boolean ready(ServerPlayer p){int[] v=charging.get(p.getUUID());return v!=null&&v[1]==1&&v[0]>0;}
    /** 已蓄的充能值（未释放时）。 */
    public static int storedCharge(ServerPlayer p){int[] v=charging.get(p.getUUID());return v==null?0:v[0];}
    /** 右键释放：把存着的充能打出去，并按档数结算永久近战加成（需求：充能完毕后仍需右键使用）。 */
    public static boolean fire(ServerPlayer p){
        int[] v=charging.get(p.getUUID());
        int charge=v==null?0:v[0];
        if(v!=null)charging.remove(p.getUUID());
        int gained=charge/CHARGE_STEP;
        if(gained>steps(p)){Curse.data(p).putInt(STEPS_KEY,gained);CastFeedback.tell(p,"神武解 · 本体近战永久 +"+BalanceRules.number(gained*MELEE_PERMANENT_PER_STEP*100)+"%（累计 "+gained+" 档）");}
        boolean ok=strike(p,charge);
        syncChargeValue(p,0); // 打完后把 HUD 清零
        return ok;
    }
    /** 带充能档位的雷击：范围与伤害随充能成长。 */
    public static boolean strike(ServerPlayer p,int charge){
        if(Curse.active(p,"ShinbukeCooldown")){
            CastFeedback.tell(p,"神武解 · 雷霆冷却中（"+((Curse.data(p).getLong("ShinbukeCooldown")-p.level().getGameTime()+19)/20)+" 秒）");
            return false;
        }
        ServerLevel level=p.serverLevel();
        Vec3 eye=p.getEyePosition(),look=p.getLookAngle().multiply(1,0,1);
        if(look.lengthSqr()<1e-4)look=new Vec3(0,0,1);
        look=look.normalize();
        double range=RANGE*rangeFactor(charge);
        double dmg=STRIKE_DAMAGE*damageFactor(charge);
        List<LivingEntity> hit=new ArrayList<>();
        for(LivingEntity e:Skills.targets(level,p,eye,range+2)){
            Vec3 rel=e.position().subtract(eye);
            Vec3 flat=new Vec3(rel.x,0,rel.z);
            double dist=flat.length();
            if(dist>range)continue;
            if(dist>0.6){
                double cos=flat.normalize().dot(look);
                if(cos<Math.cos(HALF_ANGLE))continue; // 不在面前扇形里
            }
            if(!p.hasLineOfSight(e))continue;
            hit.add(e);
        }
        Vec3 center=eye.add(look.scale(range*0.5));
        if(hit.isEmpty()){
            // 空放也给一道视觉雷（打在地上），让人知道技能确实出手了
            Network.send(level,new Network.Effect(ShinbukeVisuals.STRIKE,p.getId(),eye.add(look.scale(6)),Vec3.ZERO,1,ShinbukeVisuals.LIFE,0));
            level.playSound(null,p.blockPosition(),SoundEvents.TRIDENT_THUNDER,SoundSource.PLAYERS,1.6f,1.4f);
            Curse.status(p,"ShinbukeCooldown",COOLDOWN);
            p.displayClientMessage(Component.literal("神武解 · 雷击（扇形内没有目标）").setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0x9BE8FF)),true);
            return true;
        }
        int i=0;
        for(LivingEntity e:hit){
            Network.send(level,new Network.Effect(ShinbukeVisuals.STRIKE,p.getId(),e.position(),Vec3.ZERO,1,ShinbukeVisuals.LIFE,i++));
            e.invulnerableTime=0;
            AttackTypes.mark(AttackTypes.LIGHTNING);
            CombatSettings.hurt(p,e,level.damageSources().indirectMagic(p,p),BalanceRules.damage(STRIKE_DAMAGE*damageFactor(charge)));
            e.setSecondsOnFire(2);
            Vec3 push=e.position().subtract(center).multiply(1,0,1);
            if(push.lengthSqr()>1e-4){push=push.normalize();e.push(push.x*1.2,0.35,push.z*1.2);e.hurtMarked=true;}
        }
        level.playSound(null,p.blockPosition(),SoundEvents.TRIDENT_THUNDER,SoundSource.PLAYERS,2.2f,1.2f);
        Curse.status(p,"ShinbukeCooldown",COOLDOWN);
        p.displayClientMessage(Component.literal("神武解 · 雷霆贯穿 "+hit.size()+" 个目标 · 单发 "+BalanceRules.number(BalanceRules.damage(dmg))
            +" · 范围 "+BalanceRules.number(range)+" 格 · 充能 "+charge)
            .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0x9BE8FF)),true);
        return true;
    }
    /** 服务端推进充能：**每刻 +1 点**（一秒 20 刻 = 20 次，客户端的"50 次/秒"由心跳与 UI 呈现），
     *  手持被换掉/死亡就自动结算掉；已停止充能（等待右键）时只保留数值、不再增长。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void chargeTick(net.minecraftforge.event.TickEvent.ServerTickEvent e){
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var server=e.getServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        boolean sync=server.getTickCount()%10==0; // 每 10 刻同步一次给 HUD
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            int[] v=charging.get(p.getUUID());
            if(v==null)continue;
            if(!holding(p)||!p.isAlive()){cancelCharge(p);continue;}
            if(v[1]==0)v[0]=Math.min(CHARGE_MAX,v[0]+CHARGE_PER_TICK);
            if(sync)syncCharge(p); // 充能中与"充能完毕等右键"两种状态都要持续同步，UI 才不会消失
        }
    }
    /** 心跳：把当前充能值发给客户端（HUD 显示用）。 */
    public static void syncCharge(ServerPlayer p){syncChargeValue(p,chargeOf(p));}
    /** 直接同步一个充能值（打完后清零用）；flag 1 = 已停止充能、等待右键释放。 */
    public static void syncChargeValue(ServerPlayer p,int value){
        if(p==null||p.serverLevel()==null)return;
        int[] v=charging.get(p.getUUID());
        long flag=(v!=null&&v[1]==1)?1L:0L;
        Network.send(p.serverLevel(),new Network.Effect(ShinbukeVisuals.CHARGE,p.getId(),p.position(),Vec3.ZERO,value,0,flag));
    }
    /** 取消充能（不释放、不打雷）。 */
    public static void cancelCharge(ServerPlayer p){charging.remove(p.getUUID());}
    /** 开始充能（按住释放键）。 */
    public static void beginCharge(ServerPlayer p){
        if(!holding(p))return;
        int[] v=charging.computeIfAbsent(p.getUUID(),k->new int[]{0,0});
        v[1]=0; // 1 = 已停止充能、等右键释放
        CastFeedback.tell(p,"神武解 · 开始充能（每 100 点：范围 +25%、伤害 +25%、本体近战永久 +10%）");
    }
    /** 松手：**只停止增长**，充能值保留下来等玩家右键释放（不再自动打出去）。 */
    public static void release(ServerPlayer p){
        int[] v=charging.get(p.getUUID());
        if(v==null)return;
        if(!p.isAlive()||!holding(p)){cancelCharge(p);return;}
        v[1]=1; // 1 = 已停止充能、等右键释放
        syncCharge(p);
        CastFeedback.tell(p,"神武解 · 充能完成 "+v[0]+" 点（右键释放 · 范围 ×"+BalanceRules.number(rangeFactor(v[0]))+" 伤害 ×"+BalanceRules.number(damageFactor(v[0]))+"）");
    }
    private Shinbuke(){}
}
