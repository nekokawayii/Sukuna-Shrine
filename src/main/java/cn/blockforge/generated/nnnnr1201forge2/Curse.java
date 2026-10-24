package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

/** 所有成长和消耗由服务端决定；持久数据随存档及死亡重生保留。 */
public final class Curse {
    private static final String KEY="SukunaProgress";
    private static final UUID SLOW=UUID.fromString("2d46ed7e-6331-4677-b53c-5ff5431f2803");
    private static final UUID HEALTH=UUID.fromString("2d46ed7e-6331-4677-b53c-5ff5431f2801"), ATTACK=UUID.fromString("2d46ed7e-6331-4677-b53c-5ff5431f2802");
    public static final int[] COST={35,120,30,150,250,70,250};
    public static CompoundTag data(Player p){
        CompoundTag root=p.getPersistentData();
        if(!root.contains(KEY)){CompoundTag tag=new CompoundTag();tag.putDouble("Energy",1000);root.put(KEY,tag);}
        return root.getCompound(KEY);
    }
    public static int fingers(Player p){return Math.max(0,Math.min(20,data(p).getInt("Fingers")));}
    public static boolean awakened(Player p){return data(p).getBoolean("Awakened");}
    public static boolean nine(Player p){return data(p).getBoolean("Nine");}
    public static int tier(Player p){int n=fingers(p);return n>=20?5:n>=18?4:n>=15?3:n>=10?2:n>=5?1:0;}
    public static double fingerScale(Player p){return 1;}
    public static double scale(Player p){return 1;}
    public static double soulSlashMultiplier(Player p){return 1;}
    public static double soulSlashBonus(Player p){return Math.max(0,Math.min(300,data(p).getInt("SoulFeasts")));}
    private static boolean soul(Player p){return data(p).getBoolean("SoulPowerDouble");}
    private static double rawHealthBonus(Player p){return GrowthRules.health(fingers(p),awakened(p),nine(p),soul(p),VoidController.boosted(p),GuardianCombat.awakened(p));}
    public static double healthBonus(Player p){return BalanceRules.strength(rawHealthBonus(p),BalanceRules.Category.PLAYER_HEALTH);}
    public static double attackBonus(Player p){return BalanceRules.strength(GrowthRules.attack(fingers(p),awakened(p),nine(p),soul(p),VoidController.boosted(p),GuardianCombat.awakened(p)),BalanceRules.Category.PLAYER_ATTACK);}
    public static double spellBonus(Player p){return GrowthRules.spell(fingers(p),awakened(p),nine(p),soul(p),VoidController.boosted(p),GuardianCombat.awakened(p),0);}
    public static double domainSpellBonus(Player p){return spellBonus(p)*0.5;}
    public static double rawMaximum(Player p){ServerPlayer pool=Authority.pool(p);if(pool!=null)return rawMaximum(pool);return AuthorityRules.maximum(EnergyRules.maximum(GrowthRules.energy(fingers(p),awakened(p),nine(p),soul(p),VoidController.boosted(p),GuardianCombat.awakened(p)),Uraume.fish(p)),Authority.unlocked(p));}
    public static double maximum(Player p){return rawMaximum(p);}
    public static double rawEnergy(Player p){ServerPlayer pool=Authority.pool(p);return pool!=null?rawEnergy(pool):EnergyRules.current(data(p).getDouble("Energy"),rawMaximum(p));}
    public static double energy(Player p){return rawEnergy(p);}
    public static boolean canSpend(Player p,double rawCost){return EnergyRules.canSpend(rawEnergy(p),rawCost);}
    public static double power(Player p){return scale(p)*Journey.bonus(p)*(active(p,"Disorder")?0.8:1);}
    public static boolean active(Player p,String key){return data(p).getLong(key)>p.level().getGameTime();}
    public static boolean burnout(Player p){return active(p,"FinaleBurnout")||DomainClash.penalized(p)||data(p).getBoolean("TechniqueBurnout")&&(data(p).getLong("TechniqueBurnoutUntil")==0||active(p,"TechniqueBurnoutUntil"));}
    /** 熔断剩余刻数（给"为什么放不出来"的提示用）。 */
    public static int burnoutTicks(Player p){
        if(DomainClash.penalized(p))return DomainClash.penaltyTicks(p);
        long until=data(p).getLong("TechniqueBurnoutUntil");
        return (int)Math.max(0,until-p.level().getGameTime());
    }
    public static double reverseHealing(Player p){return BalanceRules.strength(ReverseRules.healing(rawHealthBonus(p)),BalanceRules.Category.HEALING);}
    public static double reverseCost(Player p){return DomainClash.penalized(p)?DomainClashRules.healCost(maximum(p)):ReverseRules.cost(rawHealthBonus(p));}
    public static boolean repaired(Player p){return active(p,"TechniqueRepairedUntil");}
    public static boolean canUseTechnique(Player p){return !PrisonSystem.locked(p)&&!SixfoldFinale.active(p)&&!SixfoldFinale.controlled(p)&&!burnout(p)&&!DomainClash.active(p)&&!NetBranches.locked(p)&&!Uraume.locked(p)&&!CombatExpansion.controlled(p);}
    public static double damageFactor(Player p){return burnout(p)?0.7:1.0;}
    public static double residueFactor(Player p){return 1+Journey.residuePercent(p)/100.0;}
    public static void status(Player p,String key,int ticks){data(p).putLong(key,p.level().getGameTime()+ticks);}
    public static void endDomain(ServerPlayer p,boolean voluntary){enterBurnout(p,ShrineRules.burnoutTicks(voluntary));}
    /** 御厨子领域结束：**只锁领域自己 30 秒**（熔断是全局禁用术式，领域不该走它）。 */
    public static void cooldownDomain(ServerPlayer p){status(p,"DomainCooldown",ShrineRules.DOMAIN_COOLDOWN_TICKS);}
    public static boolean domainCooling(ServerPlayer p){return active(p,"DomainCooldown");}
    public static int domainCooldownLeft(ServerPlayer p){return (int)Math.max(0,data(p).getLong("DomainCooldown")-p.level().getGameTime());}
    private static void clearBurnout(ServerPlayer p){data(p).putBoolean("TechniqueBurnout",false);data(p).remove("TechniqueBurnoutUntil");data(p).remove("TechniqueBurnoutAt");MaidBridge.body(p).removeEffect(TechniqueEffects.BURNOUT.get());}
    public static void enterBurnout(ServerPlayer p,int ticks){
        if(SixfoldFinale.active(p))return; // 终式期间不被关闭旧领域的熔断提前削减伤害，最终统一支付。
        ticks=Math.max(ticks,DomainClash.penaltyTicks(p));data(p).putBoolean("TechniqueBurnout",true);status(p,"TechniqueBurnoutUntil",ticks);data(p).remove("TechniqueRepairedUntil");data(p).remove("TechniqueBurnoutAt");data(p).remove("Rotation");
        MaidBridge.body(p).addEffect(new net.minecraft.world.effect.MobEffectInstance(TechniqueEffects.BURNOUT.get(),ticks,0,false,true));
        Skills.cancelSustained(p);sync(p);p.displayClientMessage(Component.literal("术式熔断：持续"+(ticks/20)+"秒，除反转外无法使用术式，术式伤害-30%"),true);
    }
    @SubscribeEvent public static void allDamage(net.minecraftforge.event.entity.living.LivingHurtEvent event){
        ServerPlayer p=CombatSettings.caster(event.getSource());
        if(p==null)p=MaidBridge.caster(event.getSource().getEntity());
        if(p==null)return;
        double factor=1;
        if(event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))factor*=residueFactor(p);
        event.setAmount((float)(event.getAmount()*factor));
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void repairedDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event){ServerPlayer p=CombatSettings.caster(event.getSource());if(p!=null&&repaired(p)&&!DeathStar.source(event.getSource()))event.setAmount(event.getAmount()*1.1f);}
    public static void permanentSoulFire(ServerPlayer p,net.minecraft.world.entity.LivingEntity target){
        target.setSecondsOnFire(8);var tag=target.getPersistentData();tag.putBoolean("SukunaSoulFire",true);tag.putUUID("SukunaSoulFireOwner",p.getUUID());tag.putBoolean("SukunaSoulFireMaid",MaidBridge.isCaster(p));tag.putBoolean("SukunaNoHeal",true);
    }
    /** 咒力的显示、存档、恢复和扣费统一用原单位，模式切换不改余额。 */
    public static void add(Player p,double amount){ServerPlayer pool=Authority.pool(p);if(pool!=null){add(pool,amount);return;}if(amount>0&&active(p,"FinaleNoRecovery"))return;data(p).putDouble("Energy",EnergyRules.add(rawEnergy(p),rawMaximum(p),amount));}
    public static void addRaw(Player p,double amount){add(p,amount);}
    /** 固定消耗与按上限百分比计算的消耗均不再受伤害倍率缩放。 */
    public static boolean spend(ServerPlayer p,double amount){return spendEffective(p,amount);}
    public static boolean spendEffective(ServerPlayer p,double amount){
        ServerPlayer pool=Authority.pool(p);if(pool!=null)return spendEffective(pool,amount);
        if(!EnergyRules.canSpend(energy(p),amount)){p.displayClientMessage(Component.literal("咒力不足，术式已停止"),true);sync(p);return false;}
        add(p,-amount);status(p,"RecoveryDelay",60);sync(p);return true;
    }
    public static double drain(ServerPlayer p,double fraction){ServerPlayer pool=Authority.pool(p);if(pool!=null)return drain(pool,fraction);double n=energy(p)*fraction;add(p,-n);status(p,"Siphon",14);status(p,"RecoveryDelay",60);sync(p);return n;}
    public static void apply(Player p){
        if(p instanceof MaidBridge.Actor actor){MaidBridge.apply(actor);return;}
        if(awakened(p)&&!p.getAbilities().mayfly){p.getAbilities().mayfly=true;if(p instanceof ServerPlayer sp)sp.onUpdateAbilities();}
        modifier(p.getAttribute(Attributes.MAX_HEALTH),HEALTH,healthBonus(p));
        modifier(p.getAttribute(Attributes.ATTACK_DAMAGE),ATTACK,attackBonus(p));
        modifier(p.getAttribute(Attributes.MOVEMENT_SPEED),SLOW,active(p,"Parasite")?-0.02:0);
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
    }
    private static void modifier(AttributeInstance a,UUID id,double amount){if(a==null)return;var old=a.getModifier(id);if(old!=null&&old.getOperation()==AttributeModifier.Operation.ADDITION&&Math.abs(old.getAmount()-amount)<0.00001)return;a.removeModifier(id);if(amount!=0)a.addTransientModifier(new AttributeModifier(id,"宿傩成长",amount,AttributeModifier.Operation.ADDITION));}
    public static void consume(ServerPlayer p,boolean body){
        int old=tier(p);float health=p.getMaxHealth();
        if(body){data(p).putBoolean("Awakened",true);data(p).putBoolean("BuddhaDefeated",true);}else data(p).putInt("Fingers",fingers(p)+1);
        apply(p);p.heal(p.getMaxHealth()-health);add(p,maximum(p)*0.2);sync(p);
        int tier=tier(p);
        if(body||old!=tier)Network.send(p.serverLevel(),new Network.Effect(20,p.getId(),p.position(),net.minecraft.world.phys.Vec3.ZERO,body?6:tier,80,body?1:0));
        p.displayClientMessage(Component.literal(body?"即身佛肉体融合：世界斩觉醒，"+GrowthConfig.BODY.description():"宿傩手指 "+fingers(p)+" / 20 · 每根"+GrowthConfig.FINGER.description()),false);
    }
    public static void sync(ServerPlayer p){if(p instanceof net.minecraftforge.common.util.FakePlayer)return;Network.syncCombat(p);Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Network.Progress(energy(p),maximum(p),fingers(p),awakened(p),active(p,"Siphon"),active(p,"Disorder"),Journey.unlocked(p)||Uraume.unlocked(p)||GojoPlayer.unlocked(p),Journey.nine(p),Journey.rotation(p),data(p).getBoolean("Fist"),Journey.guide(p),SoulJourney.evolved(p),VoidJourney.evolved(p),GuardianCombat.awakened(p),burnout(p),reverseHealing(p),reverseCost(p),BalanceRules.modpackMode(),DomainClash.penaltyTicks(p),Journey.unlocked(p),Uraume.unlocked(p),Uraume.fish(p),SukunaBranches.wallSpacing(p),SukunaBranches.diagonal(p),SlashForms.qi(p),DomainAmplification.active(p),GojoPlayer.unlocked(p),SukunaBranches.domainRadiusPts(p),SukunaBranches.domainDamagePts(p),SukunaBranches.domainSpeedPts(p),SukunaBranches.domainVerticalKill(p)));}
    private static String reverseBlocked(ServerPlayer p){
        if(DeathStar.anemic(p))return "严重贫血期间无法回血";
        if(SixfoldFinale.active(p)||SixfoldFinale.controlled(p)||!p.isAlive()||p.isSpectator()||!(Journey.unlocked(p)||GojoPlayer.unlocked(p))||DomainClash.active(p)||NetBranches.locked(p)||Uraume.locked(p))return "当前无法使用反转术式";
        if(fingers(p)<15)return "反转术式需要先吃下15根宿傩手指";
        if(active(p,"ReverseCooldown"))return "反转术式冷却中";
        var body=MaidBridge.body(p);var tag=body.getPersistentData();long now=p.level().getGameTime();
        if(active(p,"PureLandNoHeal")||active(p,"SoulGap")||tag.getBoolean("SukunaNoHeal")||tag.getLong("SoulGapUntil")>now||tag.getLong("VoidNoHeal")>now||tag.getLong("GuardianSkillNoHeal")>now||GuardianEntity.locked(body))return "当前处于禁疗状态，无法使用反转术式";
        if(body.getHealth()>=body.getMaxHealth())return "生命已满，无需使用反转术式";
        if(energy(p)+0.0001<reverseCost(p))return "咒力不足：反转术式需要 "+(long)reverseCost(p)+" 咒力";
        return null;
    }
    public static void reverse(ServerPlayer p){
        String blocked=reverseBlocked(p);if(blocked!=null){p.displayClientMessage(Component.literal(blocked),true);return;}
        float before=MaidBridge.body(p).getHealth();
        if(reversePulse(p))p.displayClientMessage(Component.literal("反转术式：恢复 "+String.format(java.util.Locale.ROOT,"%.1f",MaidBridge.body(p).getHealth()-before)+" 生命，消耗 "+(long)reverseCost(p)+" 咒力"),true);
        else p.displayClientMessage(Component.literal("治疗未生效，未消耗咒力"),true);
    }
    public static boolean reversePulse(ServerPlayer p){
        if(reverseBlocked(p)!=null)return false;
        var body=MaidBridge.body(p);float before=body.getHealth();body.heal((float)reverseHealing(p));
        if(body.getHealth()<=before)return false;
        // 对拼败北的55%费用由统一治疗事件收取，不能在这里再扣一次。
        if(!DomainClash.penalized(p))add(p,-reverseCost(p));status(p,"RecoveryDelay",60);status(p,"ReverseCooldown",ReverseRules.COOLDOWN_TICKS);sync(p);
        Network.send(p.serverLevel(),new Network.Effect(137,body.getId(),body.position(),net.minecraft.world.phys.Vec3.ZERO,1,20,0));return true;
    }
    public static void repairBurnout(ServerPlayer p){
        if(active(p,"FinaleBurnout")){CastFeedback.tell(p,"终式代价不能提前修复，剩余"+((data(p).getLong("FinaleBurnout")-p.level().getGameTime()+19)/20)+"秒");return;}
        if(DomainClash.active(p)||DomainClash.penalized(p)){p.displayClientMessage(Component.literal("领域对拼败北熔断无法提前修复，剩余"+((DomainClash.penaltyTicks(p)+19)/20)+"秒"),true);return;}
        if(fingers(p)<15||!burnout(p)){p.displayClientMessage(Component.literal("当前没有可修复的术式熔断"),true);return;}
        double cost=ShrineRules.repairCost(maximum(p));if(!spendEffective(p,cost))return;
        clearBurnout(p);status(p,"TechniqueRepairedUntil",ShrineRules.REPAIR_BUFF_TICKS);sync(p);p.displayClientMessage(Component.literal("术式熔断已修复：全部伤害提升10%，持续5秒"),true);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&p.tickCount%20==0)tickCaster(p);}
    public static void tickCaster(ServerPlayer p){
        if(Authority.pool(p)!=null){if(data(p).getBoolean("TechniqueBurnout")&&!active(p,"TechniqueBurnoutUntil"))clearBurnout(p);return;} // 军团不凭空为共享池叠加回咒。
        apply(p);data(p).remove("TechniqueBurnoutAt");
        if(data(p).getBoolean("TechniqueBurnout")){
            if(data(p).getLong("TechniqueBurnoutUntil")==0)status(p,"TechniqueBurnoutUntil",1200);
            if(!active(p,"TechniqueBurnoutUntil"))clearBurnout(p);
            else if(!MaidBridge.body(p).hasEffect(TechniqueEffects.BURNOUT.get()))MaidBridge.body(p).addEffect(new net.minecraft.world.effect.MobEffectInstance(TechniqueEffects.BURNOUT.get(),(int)(data(p).getLong("TechniqueBurnoutUntil")-p.level().getGameTime()),0,false,true));
        }
        // 旧存档的持续反转开关作废，改为轮盘单次释放，避免更新后自动扣咒力。
        data(p).remove("ReverseTechnique");
        // 「咒力附拳」开启期间：**不回复咒力**（这里排除掉），并且**每秒扣 10 点**（扣不动就自动关闭）。
        boolean fist=data(p).getBoolean("Fist");
        if(fist){
            if(!spend(p,10)){
                data(p).putBoolean("Fist",false);sync(p);
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("咒力耗尽，咒力附拳已关闭"),true);
                com.mojang.logging.LogUtils.getLogger().info("[附拳] {} 咒力不足，自动关闭",p.getScoreboardName());
            }else{
                com.mojang.logging.LogUtils.getLogger().info("[附拳] {} 每秒消耗 10 咒力，剩余 {}",p.getScoreboardName(),(int)energy(p));
            }
        }
        if(p.isAlive()&&!fist&&!SixfoldFinale.active(p)&&!active(p,"FinaleNoRecovery")&&!active(p,"RecoveryDelay")&&!Skills.sustaining(p)&&!GuardianPlayerSkills.sustaining(p))addRaw(p,15+fingers(p)*3+(awakened(p)?10:0)+(nine(p)?10:0)+(soul(p)?20:0)+(VoidController.boosted(p)?15:0)+(GuardianCombat.awakened(p)?30:0));sync(p);
    }
    @SubscribeEvent public static void soulFire(TickEvent.LevelTickEvent e){if(e.phase!=TickEvent.Phase.END||!(e.level instanceof net.minecraft.server.level.ServerLevel l)||l.getGameTime()%20!=0)return;for(var entity:l.getAllEntities())if(entity instanceof net.minecraft.world.entity.LivingEntity target&&target.getPersistentData().getBoolean("SukunaSoulFire")){var tag=target.getPersistentData();ServerPlayer owner=tag.hasUUID("SukunaSoulFireOwner")?MaidBridge.find(l,tag.getUUID("SukunaSoulFireOwner")):null;if(owner==null){target.clearFire();continue;}if(!CombatSafety.canAffect(owner,target)){target.clearFire();tag.remove("SukunaSoulFire");tag.remove("SukunaSoulFireOwner");tag.remove("SukunaNoHeal");continue;}target.setSecondsOnFire(3);tag.putBoolean("SukunaNoHeal",true);}}
    @SubscribeEvent public static void noSoulHeal(net.minecraftforge.event.entity.living.LivingHealEvent e){if(e.getEntity().getPersistentData().getBoolean("SukunaNoHeal"))e.setCanceled(true);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){e.getEntity().getPersistentData().put(KEY,data(e.getOriginal()).copy());apply(e.getEntity());if(e.isWasDeath()){data(e.getEntity()).putDouble("Energy",active(e.getEntity(),"FinaleNoRecovery")?0:rawMaximum(e.getEntity()));data(e.getEntity()).remove("Siphon");data(e.getEntity()).remove("Disorder");data(e.getEntity()).remove("RecoveryDelay");data(e.getEntity()).remove("MultiArmed");}}
    @SubscribeEvent public static void pickup(PlayerEvent.ItemPickupEvent e){if(e.getEntity() instanceof ServerPlayer p&&e.getStack().is(Content.BODY.get())){Network.send(p.serverLevel(),new Network.Effect(44,p.getId(),p.position(),net.minecraft.world.phys.Vec3.ZERO,1,30,0));p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.EVOKER_CELEBRATE,net.minecraft.sounds.SoundSource.PLAYERS,1,0.55f);}}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){apply(p);sync(p);}}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p){apply(p);p.setHealth(p.getMaxHealth());sync(p);}}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){apply(p);sync(p);}}
}
