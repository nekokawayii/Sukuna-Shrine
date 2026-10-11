package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 按住状态按玩家隔离；心跳只续租，不重复施法。菜单、失焦与断线取消而非松手释放。 */
public final class TechniqueInput {
    public record Input(int skill,int phase,boolean forward,boolean backward,boolean jump,boolean crouch){
        public Input(int skill,int phase,boolean forward,boolean backward,boolean jump){this(skill,phase,forward,backward,jump,false);}
    }
    private static final class Hold {
        final ServerPlayer player;final ServerLevel level;final int skill;final long start;
        final Input input;long heartbeat;int slashes;double budget;boolean triggered;
        Hold(ServerPlayer p,Input i){player=p;level=p.serverLevel();skill=i.skill;input=i;start=heartbeat=level.getGameTime();}
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
            }else if(old.skill>=34&&old.skill<41)GojoCombat.chargeRelease(p,old.skill-34); // 短按普通 / 长按最大功率
            else if(old.skill==2&&!old.triggered)Skills.cleaveCharge(p,false);
            else if(old.skill==CombatExpansionRules.DRAGON_STAR)CombatExpansion.stopDragonChant(p);
            intents.remove(p.getUUID());return;
        }
        if(input.phase!=0||!SkillAction.isSkill(input.skill)||old!=null||!allowed(p)||DismantleMotion.active(p)||CombatExpansion.chanting(p))return;
        if(!Journey.unlocked(p)&&input.skill!=22&&input.skill!=23&&!(input.skill>=34&&input.skill<41))return;
        Hold hold=new Hold(p,input);holds.put(p.getUUID(),hold);
        if(CombatExpansionRules.branch(input.skill)&&(!Journey.unlocked(p)||input.skill==CombatExpansionRules.CLEAVE_LINGER&&Curse.fingers(p)<5)){holds.remove(p.getUUID());return;}
        if(input.skill==2&&!Curse.awakened(p)&&!MaidBridge.body(p).isShiftKeyDown()){Skills.cleaveCharge(p,true);return;}
        if(CombatExpansionRules.linger(input.skill))return;
        if(input.skill==CombatExpansionRules.DRAGON_STAR){CombatExpansion.dragonPress(p);return;}
        if(input.skill==0&&!Curse.canSpend(p,Curse.COST[0])){cancel(p);CastFeedback.unavailable(p,Curse.COST[0]);return;}
        cast(p,input.skill,input);hold.triggered=true;hold.slashes=1;
    }
    private static boolean allowed(ServerPlayer p){return !GojoEntity.mindLocked(p)&&CombatExpansion.valid(p,p.serverLevel())&&(Journey.unlocked(p)||Uraume.unlocked(p)||GojoPlayer.unlocked(p))&&Curse.canUseTechnique(p)&&!SoulCombat.suppressed(p)&&!MaidBridge.recovering(p)&&!CombatExpansion.controlled(p);}
    private static void cast(ServerPlayer p,int skill,Input input){
        intents.put(p.getUUID(),input);
        try{castRaw(p,skill);}finally{intents.remove(p.getUUID());}
    }
    public static void castRaw(ServerPlayer p,int skill){
        if(skill==CombatExpansionRules.PURPLE_FURNACE){CombatExpansion.purple(p);return;}
        if(skill==SlashFormRules.WORLD){CombatExpansion.worldChant(p,intent(p));return;}
        if(skill==DismantleMotionRules.RING_SKILL){DismantleMotion.start(p,DismantleMotionRules.SPIN);return;}
        if(skill>=24&&skill<=26){if(skill!=26)SlashForms.cast(p,skill);return;}
        if(skill==17||skill==18){NetBranches.cast(p,skill);return;}
        if(skill>=19&&skill<=21){SukunaBranches.cast(p,skill);return;}
        if(skill==22||skill==23){Uraume.cast(p,skill);return;}
        if(skill>=34&&skill<41){GojoCombat.chargeStart(p,skill-34);return;}
        if(skill>=7&&skill<=16){GuardianPlayerSkills.cast(p,skill-7);return;}
        if(skill==6){Skills.peel(p);return;}
        if(skill>=0&&skill<6)Skills.cast(p,skill);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(Hold h:List.copyOf(holds.values())){
            ServerPlayer p=h.player;long now=h.level.getGameTime();
            if(!allowed(p)||p.level()!=h.level||now-h.heartbeat>25){cancel(p);continue;}
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
