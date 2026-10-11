package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import java.util.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;

/** 持久化四阶段任务；所有进度由服务端真实事件累计。 */
public final class Journey {
    public static final String[] PAGES={"胀相","坏相","血涂","脓烂","青瘀","啖相","散相","骨相","烧相"};
    public static final String[] IDS={"zhang","huai","xue","nong","qing","dan","san","gu","shao"};
    public static final String[] CONDITIONS={"生命低于30%持续战斗60秒","不穿护甲累计受到21点伤害","累计自身失血5000点","同时有至少3种负面效果存活30秒","隐身或黑暗中累计移动500米","累计被敌人命中200次","领域展开累计击杀20名敌人","无敌人时累计停留30分钟","累计承受火焰或爆炸伤害300点"};
    public static final double[] GOALS={1200,21,5000,600,500,200,20,36000,300};
    private record Position(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,net.minecraft.world.phys.Vec3 pos){}
    private static final Map<UUID,Position> positions=new HashMap<>();
    public static boolean unlocked(Player p){return Curse.data(p).getBoolean("Memory");}
    public static boolean maidCleared(Player p,String flag){return Curse.data(p).getBoolean(flag);}
    public static boolean nine(Player p){return Curse.data(p).getBoolean("Nine");}
    public static boolean rotation(Player p){return nine(p)&&Curse.active(p,"Rotation");}
    public static int residuePercent(Player p){return ShrineRules.residuePercent(Curse.data(p).getInt("ResidueUpgrades"));}
    public static double bonus(Player p){return 1;}
    private static void storeResidue(ServerPlayer p,ResidueProgress.State state){
        Curse.data(p).putInt("Residue",state.xp());Curse.data(p).putInt("ResidueUpgrades",state.level());
        Curse.apply(p);Curse.sync(p);
    }
    private static String residueStatus(Player p){
        return "残秽凝练 "+Math.min(100,residuePercent(p))+"/100%；已吸收 "+Curse.data(p).getInt("ResidueUpgrades")+" 个；伤害加成 +"+residuePercent(p)+"%";
    }
    private static int setResidueXp(ServerPlayer p,int amount){
        int before=ResidueProgress.clampLevel(Curse.data(p).getInt("ResidueUpgrades"));
        ResidueProgress.State state=ResidueProgress.normalize(amount,before);storeResidue(p,state);
        p.sendSystemMessage(Component.literal("残秽数量已设为 "+amount+"，满100自动凝炼（本次提升 "+(state.level()-before)+" 级）。"+residueStatus(p)));
        return 1;
    }
    private static int setResidueLevel(ServerPlayer p,int level){
        // 只设置加伤等级，保留未凝炼的残秽，不更改九相图或虎杖通关标记。
        storeResidue(p,new ResidueProgress.State(Math.max(0,Curse.data(p).getInt("Residue")),ResidueProgress.clampLevel(level)));
        p.sendSystemMessage(Component.literal("残秽加伤等级已设为 "+level+"（每个 +3%）。"+residueStatus(p)));
        return 1;
    }
    private static boolean ready(Player p){return unlocked(p)&&Curse.fingers(p)>=20&&Curse.awakened(p)&&Curse.data(p).getBoolean("BuddhaDefeated")&&!nine(p);}
    public static int page(Player p){for(int i=0;i<9;i++)if(!Curse.data(p).getBoolean("Page"+i))return i;return 9;}
    private static double value(Player p,int i){return Curse.data(p).getDouble("Task"+i);}
    private static void add(ServerPlayer p,int i,double n){if(!ready(p)||page(p)!=i)return;Curse.data(p).putDouble("Task"+i,Math.min(GOALS[i],value(p,i)+n));if(value(p,i)>=GOALS[i]){Curse.data(p).putBoolean("Page"+i,true);give(p,new ItemStack(ProgressionContent.PAGES.get(i).get()));p.sendSystemMessage(Component.literal("九相试炼完成："+PAGES[i]+"咒胎已获得。"));Curse.sync(p);}}
    public static void give(ServerPlayer p,ItemStack stack){if(p instanceof MaidBridge.Actor a){a.maid.spawnAtLocation(stack);return;}if(!p.getInventory().add(stack))p.drop(stack,false);}
    private static String mahitoGuide(Player p){return maidCleared(p,"BrokenMahitoDefeated")?"真人任务已完成 · 岩手通路已开启":"真人任务未完成 · 前往灵魂通道南端挑战真人";}
    public static String guide(Player p){
        if(Uraume.unlocked(p)&&!unlocked(p))return "里梅 · 冰鱼 "+Uraume.fish(p)+" 条 · 冰伤 +"+Uraume.fish(p)+"% · 技能强化 "+Uraume.upgrades(p)+" 次；长按 N 选冰凝咒法";
        if(GuardianCombat.awakened(p))return "阶段7 · 传承已吸收 · 蓝色觉醒 · 守护者技能与术式反转外放已解锁";
        if(!unlocked(p)&&VoidJourney.defeated(p))return "五条悟已通关 · 未吃宿傩记忆，不直接获得术式；虚空相咒胎可留给灵魂御厨子阶段的女仆";
        if(!unlocked(p)&&maidCleared(p,"BrokenMahitoDefeated"))return "女仆已完成真人任务 · 可前往岩手区域；吃下记忆后才解锁术式奖励";
        if(!unlocked(p)&&maidCleared(p,"YujiDefeated"))return "女仆已完成虎杖残影任务 · 可前往真人或虚空聚合体；吃下记忆后才解锁术式奖励";
        if(!unlocked(p))return "起始 · 吃下宿傩记忆，解锁术式与成长任务";
        if(Curse.fingers(p)<20)return "阶段1 · 吞噬宿傩手指 "+Curse.fingers(p)+"/20；探索宿傩神庙击败咒胎戴天";
        if(!Curse.data(p).getBoolean("BuddhaDefeated")||!Curse.awakened(p))return "阶段2 · 击败即身佛并吞噬肉体；空旷处右键即身佛召唤香（前方留8格）";
        if(!nine(p)){int i=page(p);if(i==9)return "阶段3 · 九胎齐全：在工作台合成咒胎九相图并吃下";double divisor=i==0||i==3||i==7?20:1;return "阶段3 · "+PAGES[i]+"："+CONDITIONS[i]+" ["+(int)(value(p,i)/divisor)+"/"+(int)(GOALS[i]/divisor)+"]";}
        if(VoidJourney.evolved(p))return "阶段6 · 虚空御厨子 · "+mahitoGuide(p)+" · 前往岩手挑战守护者并吸收传承 · 灵魂进食 "+Curse.data(p).getInt("SoulFeasts")+"/300";
        if(SoulJourney.evolved(p)&&VoidJourney.defeated(p))return "阶段5已通关 · "+mahitoGuide(p)+" · 五条悟已击败 · 吃下虚空相咒胎进入阶段6，或喂给灵魂御厨子阶段的女仆";
        if(SoulJourney.evolved(p))return "阶段5 · 灵魂御厨子 · "+mahitoGuide(p)+" · 右键虚空钥匙挑战五条悟 · 灵魂进食 "+Curse.data(p).getInt("SoulFeasts")+"/300 · 咒胎受肉 "+SoulCombat.layers(p)+"/9";
        if(SoulJourney.ready(p))return "阶段5 · 残秽凝练完成，击败虎杖残影后吃下灵魂御厨子碎片 · 咒胎受肉 "+SoulCombat.layers(p)+"/9";
        return "阶段4 · 残秽凝练 "+residuePercent(p)+"/100%；食用残秽每个永久伤害+3%，击杀0.1%掉落";
    }
    public static void rotate(ServerPlayer p){if(!Curse.canUseTechnique(p)){p.displayClientMessage(Component.literal("术式熔断中：无法使用九相轮转"),true);return;}if(!nine(p)||!p.isAlive()||p.isSpectator())return;if(Curse.active(p,"RotationCooldown")){p.displayClientMessage(Component.literal("九相轮转尚未冷却"),true);return;}SkillSounds.play(p,"nine_rotation");Curse.status(p,"Rotation",300);Curse.status(p,"RotationCooldown",(int)Math.ceil(1200*SoulCombat.cooldown(p)));Network.send(p.serverLevel(),new Network.Effect(54,p.getId(),p.position(),net.minecraft.world.phys.Vec3.ZERO,9,300,0));p.sendSystemMessage(Component.literal("九相轮转 · 15秒内斩击每游戏刻判定伤害（每秒20次）"));Curse.sync(p);}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer p)tickCaster(p);}
    public static void tickCaster(ServerPlayer p){
        if(!p.isAlive()){positions.remove(p.getUUID());return;}
        var d=Curse.data(p);
        if(!d.getBoolean("R7Migrated")){if(Curse.awakened(p))d.putBoolean("BuddhaDefeated",true);d.putBoolean("R7Migrated",true);}
        if(p.tickCount%20==0){
            if(!d.getBoolean("MemoryGiven")){give(p,new ItemStack(ProgressionContent.MEMORY.get()));d.putBoolean("MemoryGiven",true);p.sendSystemMessage(Component.literal("你获得了宿傩记忆。长按右键吃下后，按 N 选择术式；按 J 查看成长任务。"));}
            if(ready(p)){for(int i=0;i<9;i++)if(p.getInventory().countItem(ProgressionContent.PAGES.get(i).get())>0)d.putBoolean("Page"+i,true);}
        }
        if(!ready(p))return;
        int i=page(p);LivingEntity subject=MaidBridge.body(p);
        if(i==0){boolean fighting=Curse.active(p,"Combat")&&!p.level().getEntitiesOfClass(Monster.class,p.getBoundingBox().inflate(24),e->e.isAlive()).isEmpty();if(subject.getHealth()<subject.getMaxHealth()*0.3f&&fighting)add(p,0,1);else d.putDouble("Task0",0);}
        if(i==3){if(subject.getActiveEffects().stream().filter(e->!e.getEffect().isBeneficial()).count()>=3)add(p,3,1);else d.putDouble("Task3",0);}
        if(i==4){Position old=positions.put(p.getUUID(),new Position(p.level().dimension(),p.position()));if(old!=null&&old.dimension.equals(p.level().dimension())&&(subject.hasEffect(MobEffects.INVISIBILITY)||subject.hasEffect(MobEffects.DARKNESS))){double distance=old.pos.distanceTo(p.position());if(distance<2&&!subject.isPassenger())add(p,4,distance);}}
        else positions.remove(p.getUUID());
        if(i==7&&p.tickCount%20==0&&!Curse.active(p,"Combat")&&p.level().getEntitiesOfClass(Monster.class,p.getBoundingBox().inflate(32),LivingEntity::isAlive).isEmpty())add(p,7,20);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent e){if(e.getAmount()<=0||e.isCanceled())return;
        ServerPlayer attacker=CombatSettings.caster(e.getSource());if(attacker!=null&&e.getEntity()!=MaidBridge.body(attacker))Curse.status(attacker,"Combat",200);
        ServerPlayer p=MaidBridge.progression(e.getEntity());if(p==null)return;
        float actual=Math.min(p.getHealth(),e.getAmount());
        if(e.getSource().getEntity() instanceof LivingEntity enemy&&enemy!=p){Curse.status(p,"Combat",200);add(p,5,1);}
        boolean naked=true;for(ItemStack armor:p.getArmorSlots())if(!armor.isEmpty())naked=false;
        if(naked)add(p,1,actual);add(p,2,actual);
        if(e.getSource().is(DamageTypeTags.IS_FIRE)||e.getSource().is(DamageTypeTags.IS_EXPLOSION))add(p,8,actual);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent e){ServerPlayer p=CombatSettings.caster(e.getSource());if(e.isCanceled()||p==null)return;
        LivingEntity victim=e.getEntity();
        if(victim instanceof BuddhaEntity){Curse.data(p).putBoolean("BuddhaDefeated",true);p.sendSystemMessage(Component.literal("即身佛已击败！吞噬肉体后，按顺序完成九相试炼获得咒胎。"));}
        if(victim instanceof Monster&&Skills.inDomain(p,victim))add(p,6,1);
        boolean boss=victim instanceof BuddhaEntity||victim instanceof DaitianEntity||victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss||victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
        if(SoulJourney.evolved(p)&&CombatSettings.caster(e.getSource())==p&&(e.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)||e.getSource().is(net.minecraft.world.damagesource.DamageTypes.SONIC_BOOM))&&victim.getPersistentData().hasUUID("SukunaSoulSlashOwner")&&p.getUUID().equals(victim.getPersistentData().getUUID("SukunaSoulSlashOwner"))){int feast=Math.min(300,Curse.data(p).getInt("SoulFeasts")+1);Curse.data(p).putInt("SoulFeasts",feast);p.sendSystemMessage(Component.literal("灵魂进食：斩击基础伤害永久 +"+BalanceRules.number(BalanceRules.strength(1))+"（累计 "+BalanceRules.number(BalanceRules.strength(feast))+" / "+BalanceRules.number(BalanceRules.strength(300))+"点）"));Curse.sync(p);}
        // 残秽由击杀事件按0.1%掉落实物，食用才成长，不再自动积累击杀经验。
    }
    @SubscribeEvent public static void pickup(PlayerEvent.ItemPickupEvent e){if(e.getEntity() instanceof ServerPlayer p)for(int i=0;i<9;i++)if(e.getStack().is(ProgressionContent.PAGES.get(i).get()))Curse.data(p).putBoolean("Page"+i,true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){positions.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("sukuna")
            .then(Commands.literal("guide").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(guide(p)+"\n召唤香配方：上中烈焰棒；中排金锭、末影之眼、金锭；下中灵魂沙。\n九相条件按以下顺序，已获得则跳过："));for(int i=0;i<9;i++)p.sendSystemMessage(Component.literal((Curse.data(p).getBoolean("Page"+i)?"✓ ":"○ ")+PAGES[i]+"："+CONDITIONS[i]));return 1;}))
            .then(Commands.literal("ch")
                .executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(residueStatus(p)+"\n管理员设置：/sukuna ch xp 数字（满100自动凝炼）；/sukuna ch level 数字（每个加伤3%，34个完成凝练）。"));return 1;})
                .then(Commands.literal("xp").requires(s->s.hasPermission(2))
                    .then(Commands.argument("amount",IntegerArgumentType.integer(0))
                        .executes(c->setResidueXp(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"amount")))))
                .then(Commands.literal("level").requires(s->s.hasPermission(2))
                    .then(Commands.argument("level",IntegerArgumentType.integer(0,ResidueProgress.MAX_LEVEL))
                        .executes(c->setResidueLevel(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"level")))))));
    }
}
