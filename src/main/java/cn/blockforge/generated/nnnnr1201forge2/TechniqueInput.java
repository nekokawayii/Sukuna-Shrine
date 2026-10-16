package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 按住状态按玩家隔离；心跳只续租，不重复施法。菜单、失焦与断线取消而非松手释放。 */
public final class TechniqueInput {
    /** {@code motion} = 本次按下要不要走"动作解"（后翻/飞踢/滑铲/冲拳/跪地/环斩），由客户端按当时的按键决定。 */
    public record Input(int skill,int phase,boolean forward,boolean backward,boolean jump,boolean crouch,int motion,int wheelSkill){
        public Input(int skill,int phase,boolean forward,boolean backward,boolean jump,boolean crouch){
            this(skill,phase,forward,backward,jump,crouch,DismantleMotionRules.choose(crouch,forward,backward,jump,false,true,0),-1);
        }
        public Input(int skill,int phase,boolean forward,boolean backward,boolean jump,boolean crouch,int motion){
            this(skill,phase,forward,backward,jump,crouch,motion,-1);
        }
        public Input(int skill,int phase,boolean forward,boolean backward,boolean jump){this(skill,phase,forward,backward,jump,false);}
    }
    private static final class Hold {
        final ServerPlayer player;final ServerLevel level;final int skill;final long start;
        final Input input;long heartbeat;int slashes;double budget;boolean triggered;
        /** 客户端轮盘选中的真实技能号（简易领域 = 43）。 */
        final int wheelSkill;
        Hold(ServerPlayer p,Input i){player=p;level=p.serverLevel();skill=i.skill;input=i;start=heartbeat=level.getGameTime();wheelSkill=i.wheelSkill();}
    }
    private static final Map<UUID,Hold> holds=new HashMap<>();
    private static final Map<UUID,Input> intents=new HashMap<>();
    public static void withIntent(ServerPlayer p,Input input,Runnable action){intents.put(p.getUUID(),input);try{action.run();}finally{intents.remove(p.getUUID());}}
    public static Input intent(ServerPlayer p){return intents.getOrDefault(p.getUUID(),new Input(0,0,false,false,false));}
    public static boolean holding(ServerPlayer p,int skill){Hold h=holds.get(p.getUUID());return h!=null&&h.skill==skill;}
    public static void receive(ServerPlayer p,Input input){
        if(input.skill==FinaleRules.SKILL){
            if(input.phase==0)SixfoldFinale.start(p);
            else if(input.phase==1)SixfoldFinale.heartbeat(p);
            else if(input.phase==2||input.phase==3)SixfoldFinale.release(p);
            return;
        }
        if(SixfoldFinale.active(p))return;
        // 无量空处的收起不受任何动作守卫限制：正在对拼/熔断/别的动作里也必须能收掉自己的领域
        if(input.skill==41&&input.phase==0&&GojoCombat.domainOpen(p)){GojoCombat.toggleDomain(p);return;}
        // 神武解：按住释放键充能，松手打出去；取消（phase 3）只丢充能不打雷
        if(input.skill==Shinbuke.CHARGE_ID){
            if(input.phase==0)Shinbuke.beginCharge(p);
            else if(input.phase==1)Shinbuke.syncCharge(p);   // 心跳：把当前充能值同步给客户端 HUD
            else if(input.phase==2)Shinbuke.release(p);
            else if(input.phase==3)Shinbuke.cancelCharge(p);
            return;
        }
        if(Shinbuke.holding(p)){return;} // 手持神武解：除充能外任何术式都不可用
        // 御厨子结印期间：其余术式一律不理（释放键本身除外——第二次按下是"放下"或"取消"）
        if(Skills.sealing(p)&&input.skill!=5){CastFeedback.tell(p,"结印中：只能再次按释放键（放下/取消）");return;}
        // 简易领域 · 捌：长按维持、松手收招（不走普通 hold 的"松手才结算"路径）
        if(input.wheelSkill()==SimpleDomain.SKILL){
            if(input.phase==0)SimpleDomain.start(p);
            else if(input.phase==1)SimpleDomain.hold(p);
            else SimpleDomain.release(p,input.phase==2?"客户端松手(phase2)":"客户端取消(phase3)");
            return;
        }
        // 灶 · 最大功率 · 开：长按蓄力 20 秒（满 20 秒自动打出，提前松手/受伤/取消都算中断）
        if(input.wheelSkill()==FurnaceMax.SKILL){
            if(input.phase==0)FurnaceMax.start(p);
            else if(input.phase==1)FurnaceMax.heartbeat(p);
            else if(input.phase==2)FurnaceMax.release(p);
            else FurnaceMax.cancel(p);
            return;
        }
        // 简易领域接管期间：宿傩六式的任何输入都直接忽略（含"松手顺带放一次解"），
        // 免得它去碰通用冷却、弹"XX 冷却中"。
        if(SimpleDomain.owns(p)&&input.skill>=0&&input.skill<7)return;
        // 最大功率蓄力期间同理：六式一律不理
        if(FurnaceMax.active(p)&&input.skill>=0&&input.skill<7)return;
        // 动作解：客户端把"按下时的按键组合"带过来了，服务端据此挑一个动作（服务端的冲刺/离地状态更准）
        if(input.phase()==0&&input.skill()==0){
            LivingEntity body=MaidBridge.body(p);
            // 客户端已经按"按下那一瞬间"的按键与疾跑/离地状态算好了动作意图，服务端复核并兜底
            int kind=DismantleMotionRules.choose(input.crouch(),input.forward(),input.backward(),input.jump(),
                body.isSprinting(),body.onGround(),body.getDeltaMovement().y);
            if(kind==0&&input.motion()!=0&&DismantleMotionRules.valid(input.motion()))kind=input.motion();
            if(kind!=0&&!DismantleMotionRules.valid(kind))kind=0;
            if(kind!=0){
                // 解动作已回档到 r101 行为：位移一律沿起手朝向（不再按移动键反向），所以这里不再传"转向意图"
                DismantleMotion.start(p,kind);
                return;
            }
            // 埋点：玩家明显是想做动作（按住方向/蹲/疾跑）却没凑成动作 —— 把这组按键写进日志，
            // 否则"没触发"完全无从判断（服务端这里静默返回）。
            if(input.motion()!=0||input.forward()||input.backward()||input.crouch()||body.isSprinting())
                DismantleMotion.why(p,"按键组合不构成动作：蹲="+input.crouch()+" 前="+input.forward()+" 后="+input.backward()
                    +" 跳="+input.jump()+" 疾跑="+body.isSprinting()+" 落地="+body.onGround()+" 竖速="+String.format("%.2f",body.getDeltaMovement().y)
                    +" 客户端意图="+input.motion());
        }
        if(input.phase==3){cancel(p);return;}
        Hold old=holds.get(p.getUUID());
        if(input.phase==1){if(input.skill==SlashFormRules.WORLD)CombatExpansion.chantHeartbeat(p);if(old!=null&&old.skill==input.skill)old.heartbeat=p.level().getGameTime();return;}
        if(input.phase==2){
            if(old==null||old.skill!=input.skill)return;holds.remove(p.getUUID());
            if(!allowed(p)||p.level()!=old.level){cancel(p);return;}
            if(CombatExpansionRules.linger(old.skill)){
                if(!old.triggered){
                    if(old.skill==CombatExpansionRules.CLEAVE_LINGER&&MaidBridge.body(p).isShiftKeyDown()){if(Curse.fingers(p)>=5&&Curse.spend(p,Curse.COST[0]))TechniqueInput.withIntent(p,old.input,()->CombatExpansion.afterCast(p,old.skill));}
                    else cast(p,0,old.input);
                }else CombatExpansion.releaseLinger(p);
            }else if(old.skill>=34&&old.skill<34+GojoPlayer.COUNT)GojoCombat.chargeRelease(p,old.skill-34); // 短按普通 / 长按最大功率
            else if(old.skill==2&&!old.triggered)Skills.cleaveCharge(p,false);
            else if(old.skill==CombatExpansionRules.DRAGON_STAR)CombatExpansion.stopDragonChant(p);
            intents.remove(p.getUUID());return;
        }
        if(input.phase!=0||!SkillAction.isSkill(input.skill)||old!=null||!allowed(p)||DismantleMotion.active(p)||CombatExpansion.chanting(p))return;
        if(!Journey.unlocked(p)&&input.skill!=22&&input.skill!=23&&!(input.skill>=34&&input.skill<34+GojoPlayer.COUNT))return;
        Hold hold=new Hold(p,input);holds.put(p.getUUID(),hold);
        if(CombatExpansionRules.branch(input.skill)&&(!Journey.unlocked(p)||input.skill==CombatExpansionRules.CLEAVE_LINGER&&Curse.fingers(p)<5)){holds.remove(p.getUUID());return;}
        if(input.skill==2&&!Curse.awakened(p)&&!MaidBridge.body(p).isShiftKeyDown()){Skills.cleaveCharge(p,true);return;}
        if(CombatExpansionRules.linger(input.skill))return;
        if(input.skill==CombatExpansionRules.DRAGON_STAR){CombatExpansion.dragonPress(p);return;}
        if(input.skill==0&&!Curse.canSpend(p,Curse.COST[0])){cancel(p);CastFeedback.unavailable(p,Curse.COST[0]);return;}
        cast(p,input.skill,input);hold.triggered=true;hold.slashes=1;
    }
    static boolean allowed(ServerPlayer p){return !Shinbuke.holding(p)&&!GojoEntity.mindLocked(p)&&CombatExpansion.valid(p,p.serverLevel())&&(Journey.unlocked(p)||Uraume.unlocked(p)||GojoPlayer.unlocked(p))&&Curse.canUseTechnique(p)&&!SoulCombat.suppressed(p)&&!MaidBridge.recovering(p)&&!CombatExpansion.controlled(p);}
    private static void cast(ServerPlayer p,int skill,Input input){
        intents.put(p.getUUID(),input);
        try{castRaw(p,skill);}finally{intents.remove(p.getUUID());}
    }
    public static void castRaw(ServerPlayer p,int skill){
        // 每次施法先落一条适应细分：解/捌/网分支/形状分支/苍/赫/茈…延迟命中的伤害靠它分类。
        // 各技能内部的 AttackTypes.prepare 更精确，这里只是兜底，保证绝不残留上一次的招。
        AttackTypes.prepare(AttackTypes.pendingFor(skill));
        if(skill==CombatExpansionRules.PURPLE_FURNACE){CombatExpansion.purple(p);return;}
        if(skill==SlashFormRules.WORLD){CombatExpansion.worldChant(p,intent(p));return;}
        if(skill==DismantleMotionRules.RING_SKILL){DismantleMotion.start(p,DismantleMotionRules.SPIN);return;}
        if(skill>=24&&skill<=26){if(skill!=26)SlashForms.cast(p,skill);return;}
        if(skill==17||skill==18){NetBranches.cast(p,skill);return;}
        if(skill>=19&&skill<=21){SukunaBranches.cast(p,skill);return;}
        if(skill==22||skill==23){Uraume.cast(p,skill);return;}
        if(skill>=34&&skill<34+GojoPlayer.COUNT){GojoCombat.chargeStart(p,skill-34);return;}
        if(skill>=7&&skill<=16){GuardianPlayerSkills.cast(p,skill-7);return;}
        if(skill==6){Skills.peel(p);return;}
        if(skill>=0&&skill<6)Skills.cast(p,skill);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(Hold h:List.copyOf(holds.values())){
            ServerPlayer p=h.player;long now=h.level.getGameTime();
            // 心跳超时放宽到 100 刻（5 秒）：客户端每 10 刻发一次，丢包不该被判定成"松手"。
            boolean domainHold=h.wheelSkill==SimpleDomain.SKILL&&SimpleDomain.active(p);
            if(!allowed(p)||p.level()!=h.level||(!domainHold&&now-h.heartbeat>100)){
                SdLog.w("Hold 失效 wheel="+h.wheelSkill+" 间隔="+(now-h.heartbeat)+" allowed="+allowed(p)+" 领域维持="+domainHold);
                if(h.wheelSkill==SimpleDomain.SKILL)SimpleDomain.release(p,"Hold失效 间隔="+(now-h.heartbeat)+" allowed="+allowed(p)+" 领域维持="+domainHold);
                cancel(p);continue;
            }
            int age=(int)(now-h.start);
            if(CombatExpansionRules.linger(h.skill)){
                if(!h.triggered&&age>=CombatExpansionRules.HOLD_DELAY){withIntent(p,h.input,()->h.triggered=CombatExpansion.startLinger(p,h.skill));if(!h.triggered)cancel(p);}
                continue;
            }
            if(h.skill==CombatExpansionRules.DRAGON_STAR){if(age>=CombatExpansionRules.HOLD_DELAY)CombatExpansion.dragonHold(p);continue;}
            if(h.skill!=0||age<CombatExpansionRules.HOLD_DELAY||DismantleMotion.active(p))continue;
            h.budget+=CombatExpansionRules.rate(age,TechniqueConfig.rate())/20;
            // 30次/秒需要部分游戏刻结算两次；不发30个输入包，也不突破服务端上限。
            int count=Math.min(2,(int)(h.budget+1e-9));h.budget=Math.max(0,h.budget-count);
            for(int i=0;i<count;i++){
                if(!Curse.spend(p,CombatExpansionRules.slashCost(Curse.COST[0],h.slashes))){cancel(p);break;}
                Skills.heldSlash(p);h.slashes++;
            }
        }
    }
    public static void cancel(ServerPlayer p){holds.remove(p.getUUID());intents.remove(p.getUUID());Skills.cancelCharge(p);CombatExpansion.cancelHeld(p);}
    public static void clear(){holds.clear();intents.clear();}
    private TechniqueInput(){}
}
