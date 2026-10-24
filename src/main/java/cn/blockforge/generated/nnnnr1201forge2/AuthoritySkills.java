package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** 权限目录使用稳定编号；六眼 100+内部号，绝不混用轮盘格号。 */
public final class AuthoritySkills {
    public static List<Integer> current(ServerPlayer p){
        List<Integer> out=new ArrayList<>();
        if(Journey.unlocked(p)){
            out.addAll(List.of(0,1,3,5,24,26,28,29,33,200,201,202,203,204,205));
            if(Curse.fingers(p)>=5)out.addAll(List.of(2,19,20,25,32,43,303));
            if(Curse.awakened(p))out.addAll(List.of(4,17,18,21,27,31));
            if(SoulJourney.evolved(p))out.add(6);
            if(VoidJourney.evolved(p))out.addAll(List.of(30,44));
            if(Curse.fingers(p)>=15)out.addAll(List.of(300,301));
            if(DomainAmplification.unlocked(p))out.add(302);
            if(Journey.nine(p)){out.add(304);for(int id=BloodRules.FIRST;id<=BloodRules.LAST;id++)out.add(id);}
            if(GuardianCombat.awakened(p))for(int i=7;i<=16;i++)out.add(i);
        }
        if(Uraume.unlocked(p))out.addAll(List.of(22,23));
        if(GojoPlayer.unlocked(p))for(int i=100;i<109;i++)out.add(i);
        return out;
    }
    public static List<Integer> owned(ServerPlayer p){var ids=new TreeSet<Integer>(current(p));for(int i:Curse.data(p).getCompound("AuthorityHistory").getIntArray("Skills"))if(valid(i))ids.add(i);return List.copyOf(ids);}
    public static boolean owned(ServerPlayer p,int id){return valid(id)&&owned(p).contains(id);}
    public static boolean valid(int id){return BloodRules.skill(id)||id>=0&&id<=33||id==43||id==44||id>=100&&id<109||id>=200&&id<=205||id>=300&&id<=304;}
    public static String name(int id){
        if(BloodRules.skill(id))return BloodRules.name(id);
        if(id>=100&&id<109)return GojoPlayer.nameOf(id-100);
        if(id>=200&&id<=205)return new String[]{"解 · 后翻","解 · 飞踢","解 · 滑铲","解 · 冲拳","解 · 跪地斩","解 · 落地追击"}[id-200];
        if(id>=300&&id<=304)return new String[]{"反转术式","修复熔断","领域展延","咒力附拳","九相轮转"}[id-300];
        if(id==43)return "简易领域 · 捌";if(id==44)return FurnaceMax.NAME;
        if(id==33)return FinaleRules.NAME;if(id>=29&&id<=32)return CombatExpansionRules.name(id);
        if(id>=24&&id<=28)return SlashFormRules.name(id);
        if(id>=19&&id<=23)return BranchRules.name(id);
        if(id==17||id==18)return NetBranchRules.name(id);
        if(id>=7&&id<=16)return GuardianPlayerSkills.NAMES[id-7];
        return id>=0&&id<7?Skills.NAMES[id]:"未刻写";
    }
    public static int fromInput(int id){return id>=34&&id<34+GojoPlayer.SLOTS?100+GojoPlayer.WHEEL[id-34]:id;}
    public static boolean input(ServerPlayer p,TechniqueInput.Input input){
        if(!Authority.unlocked(p))return false;
        int main=Authority.medium(p.getMainHandItem()),off=Authority.medium(p.getOffhandItem());
        int selected=input.wheelSkill()==43||input.wheelSkill()==44?input.wheelSkill():fromInput(input.skill());
        boolean two=main>=0||off>=0;
        boolean instant=selected>=0&&selected<=5||selected==27||selected==30||selected==31||selected==44;
        if(!two&&!instant)return false;
        if(input.phase()==3){TechniqueInput.cancel(p);return true;}
        boolean support=(main>=300||off>=300||main==14||off==14)&&p.isAlive()&&!p.isSpectator()&&!DomainClash.active(p)&&!SixfoldFinale.active(p);
        if(input.phase()==0&&(support||TechniqueInput.allowed(p))){
            int first=main>=0?main:selected;
            // 有明确组合键的主手解仍播放动作；普通按释放键和副手瞬发保持原有行为。
            if(owned(p,first)){
                if(first!=0||!DismantleMotion.fromInput(p,input))TechniqueInput.withIntent(p,input,()->cast(p,first));
            }
            if(off>=0&&off!=first&&owned(p,off)){if(off>=300||off==14)cast(p,off);else AuthorityEditor.offhand(p,off);}
        }
        return true;
    }
    public static boolean damaging(int id){if(BloodRules.skill(id))return id!=BloodRules.COMPRESS&&id!=BloodRules.FLOW&&id!=BloodRules.BLADE&&id!=BloodRules.ARMOR&&id!=BloodRules.CANAL&&id!=BloodRules.VISION&&id!=BloodRules.WINGS&&id!=BloodRules.MIRROR;return !(id==10||id==14||id==15||id==105||id==29||id==32||id>=300);}
    public static boolean ready(ServerPlayer p,int id){
        if(!p.isAlive()||ObserverRitual.active(p))return false;
        if(id==300)return MaidBridge.body(p).getHealth()<MaidBridge.body(p).getMaxHealth()&&Curse.canSpend(p,Curse.reverseCost(p))&&!Curse.active(p,"ReverseCooldown");
        if(id==301)return Curse.burnout(p)&&!DomainClash.penalized(p)&&Curse.canSpend(p,ShrineRules.repairCost(Curse.maximum(p)));
        if(!Curse.canUseTechnique(p)||SoulCombat.suppressed(p))return false;
        if(id<0)return false;
        if(BloodRules.skill(id))return BloodArts.available(p,id);
        if(id<6)return Skills.available(p,id)&&!(id==5&&Skills.domainActive(p));
        if(id==6)return Skills.peelAvailable(p);
        if(id>=7&&id<=16)return GuardianPlayerSkills.available(p,id-7)&&!(id==12&&GuardianPlayerSkills.sustaining(p));
        if(id==17||id==18)return NetBranches.available(p,id);
        if(id>=19&&id<=21)return SukunaBranches.available(p,id);
        if(id==22||id==23)return Uraume.available(p,id);
        if(id>=100&&id<109)return GojoCombat.available(p,id-100)&&!(id==107&&GojoCombat.domainOpen(p));
        if(id==24||id==25||id==27)return SlashForms.available(p,id);
        if(id==29||id==30||id==31||id==32)return CombatExpansion.available(p,id);
        if(id==44)return VoidJourney.evolved(p)&&FurnaceMax.cooldownLeft(p)==0&&Curse.canSpend(p,FurnaceMax.COST);
        if(id==33)return SixfoldFinale.ready(p);
        if(id==43)return SimpleDomain.cooldownLeft(p)==0&&!SimpleDomain.active(p)&&Curse.canSpend(p,Curse.COST[0]);
        if(id>=200&&id<=205||id==28){int kind=id==28?DismantleMotionRules.SPIN:new int[]{DismantleMotionRules.BACKFLIP,DismantleMotionRules.FLYING_KICK,DismantleMotionRules.SLIDE,DismantleMotionRules.AIR_PUNCH,DismantleMotionRules.KNEEL,DismantleMotionRules.GROUND_SMASH}[id-200];return DismantleMotion.available(p,kind);}
        return !Curse.active(p,"AuthoritySkill"+id)&&Curse.canSpend(p,70);
    }
    private record Auto(ServerPlayer player,long until,int kind){}
    private static final Map<UUID,Auto> automatic=new HashMap<>();
    public static void tickAutomatic(ServerPlayer p){
        if(p instanceof MaidBridge.Actor&&Authority.pool(p)!=null&&DomainAmplification.active(p)&&!Curse.spend(p,DomainAmplification.COST_PER_SECOND/20))DomainAmplification.stop(p);
        Auto auto=automatic.get(p.getUUID());if(auto==null)return;
        if(!p.isAlive()||p.level().getGameTime()>auto.until){if(auto.kind==43)SimpleDomain.cancel(p);automatic.remove(p.getUUID());return;}
        if(auto.kind==33)SixfoldFinale.heartbeat(p);else if(auto.kind==27)CombatExpansion.chantHeartbeat(p);else if(auto.kind==43&&!SimpleDomain.active(p))automatic.remove(p.getUUID());
    }
    public static void cancel(ServerPlayer p){BloodArts.exit(p);DeathStar.cancel(p);automatic.remove(p.getUUID());DomainAmplification.stop(p);SixfoldFinale.testReset(p);SimpleDomain.cancel(p);FurnaceMax.cancel(p);Skills.cancelCombat(p);GojoCombat.closeDomain(p);GojoCombat.cancel(p);GuardianPlayerSkills.cancel(p);TechniqueInput.cancel(p);}
    public static void clear(){automatic.clear();}
    public static boolean busy(ServerPlayer p){return BloodArts.casting(p)||SimpleDomain.active(p)||Skills.sealing(p)||GojoCombat.busy(p)||GuardianPlayerSkills.casting(p)||NetBranches.casting(p)||Uraume.breathing(p)||DismantleMotion.active(p)||CombatExpansion.automatic(p)||SixfoldFinale.active(p);}
    public static void cast(ServerPlayer p,int id){
        if(BloodRules.skill(id)){BloodArts.automatic(p,id);return;}
        if(id<6){Skills.cast(p,id);return;}if(id==6){Skills.peel(p);return;}
        if(id>=100&&id<109){GojoCombat.cast(p,id-100);if(id==108)GojoCombat.releaseBlueMax(p);return;}
        if(id>=7&&id<=16){GuardianPlayerSkills.cast(p,id-7);return;}
        if(id==17||id==18){NetBranches.cast(p,id);return;}
        if(id>=19&&id<=21){SukunaBranches.cast(p,id);return;}
        if(id==22||id==23){Uraume.cast(p,id);return;}
        if(id==24||id==25||id==27){SlashForms.cast(p,id);if(id==27&&CombatExpansion.chanting(p))automatic.put(p.getUUID(),new Auto(p,p.level().getGameTime()+400,27));return;}
        if(id==28){DismantleMotion.start(p,DismantleMotionRules.SPIN);return;}
        if(id==29||id==32){if(CombatExpansion.startLinger(p,id))CombatExpansion.releaseLinger(p);return;}
        if(id==30){CombatExpansion.purple(p);return;}if(id==31){CombatExpansion.castAutomatic(p,id);return;}
        if(id==33){SixfoldFinale.start(p);if(SixfoldFinale.active(p))automatic.put(p.getUUID(),new Auto(p,p.level().getGameTime()+200,33));return;}if(id==44){FurnaceMax.start(p);return;}
        if(id==43){if(SimpleDomain.start(p))automatic.put(p.getUUID(),new Auto(p,p.level().getGameTime()+60,43));return;}
        if(id>=200&&id<=205){int[] motions={DismantleMotionRules.BACKFLIP,DismantleMotionRules.FLYING_KICK,DismantleMotionRules.SLIDE,DismantleMotionRules.AIR_PUNCH,DismantleMotionRules.KNEEL,DismantleMotionRules.GROUND_SMASH};DismantleMotion.start(p,motions[id-200]);return;}
        switch(id){
            case 300->Curse.reverse(p);case 301->Curse.repairBurnout(p);case 302->DomainAmplification.toggle(p);case 303->Skills.toggleFist(p);case 304->Journey.rotate(p);
            // 纵心自动勾画十字，调用原本按像素计费/伤害的绘制输出。
            case 26->{if(!ready(p,id)||!Curse.spend(p,70))return;Curse.status(p,"AuthoritySkill"+id,60);SlashForms.freehand(p,List.of(new SlashFormRules.Segment(0,50,100,50),new SlashFormRules.Segment(50,0,50,100)),100,200);}
            default->{}
        }
    }
    /** 仅复制成长，不复制玩家冷却、军队存档或临时状态。保留生物原有成长。 */
    public static void inherit(ServerPlayer owner,MaidBridge.Actor actor){
        var src=Curse.data(owner);var dst=Curse.data(actor);var history=src.getCompound("AuthorityHistory");
        for(String k:List.of("Memory","Awakened","Nine","SoulShrine","SoulPowerDouble","VoidShrine","GojoMemory","UraumeMemory","PureLandVictory"))if(src.getBoolean(k)||history.getBoolean(k))dst.putBoolean(k,true);
        for(String k:List.of("Fingers","SoulLayers","SoulFeasts","ResidueUpgrades","IceFish"))dst.putInt(k,Math.max(dst.getInt(k),Math.max(src.getInt(k),history.getInt(k))));
        dst.putBoolean("MemoryGiven",true);dst.putBoolean("MaidRecovering",false);
        if(Authority.echo(actor.maid))actor.maid.setHealth(Math.max(1,actor.maid.getMaxHealth()));
        for(String k:List.of("VoidController","DomainRadiusPts","DomainDamagePts","DomainSpeedPts","DomainVerticalKill","WallSpacing","WallDiagonal","VoidFloor","AimAssist"))if(src.contains(k))dst.put(k,src.get(k).copy());
        if(src.getBoolean("VoidBossReward70"))dst.putBoolean("VoidBossReward70",true);
        actor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        var merged=dst.getCompound("AuthorityHistory");for(String key:history.getAllKeys())if(!key.equals("Skills"))merged.put(key,history.get(key).copy());var learned=new TreeSet<Integer>();for(int id:merged.getIntArray("Skills"))learned.add(id);for(int id:history.getIntArray("Skills"))learned.add(id);learned.addAll(current(actor));merged.putIntArray("Skills",learned.stream().mapToInt(Integer::intValue).toArray());dst.put("AuthorityHistory",merged);
    }
    private AuthoritySkills(){}
}
