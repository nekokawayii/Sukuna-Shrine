package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.*;

/** 玩家和女仆共用同一份阶段校验与奖励。 */
public final class StageFood extends Item {
    public enum Stage { RESIDUE,SOUL,VOID,LEGACY }
    public static final RegistryObject<Item> RESIDUE=Content.ITEMS.register("residue",()->new StageFood(Stage.RESIDUE));
    public static final RegistryObject<Item> SOUL=Content.ITEMS.register("soul_shrine_fragment",()->new StageFood(Stage.SOUL));
    public static final RegistryObject<Item> VOID=Content.ITEMS.register("void_phase_fetus",()->new StageFood(Stage.VOID));
    public static final RegistryObject<Item> LEGACY=Content.ITEMS.register("legacy",()->new StageFood(Stage.LEGACY));
    private final Stage stage;
    private StageFood(Stage stage){super(new Properties().rarity(Rarity.EPIC).food(new FoodProperties.Builder().nutrition(1).saturationMod(0).alwaysEat().build()));this.stage=stage;}
    public static void init(){}
    public static boolean allowed(Player p,Stage stage){return character.CharacterService.permits(p,character.CharacterDefinition.Family.SHRINE)&&ShrineRules.allows(stage.ordinal(),Journey.nine(p),Curse.data(p).getInt("ResidueUpgrades"),SoulJourney.evolved(p),VoidJourney.evolved(p),GuardianCombat.awakened(p));}
    public static boolean consume(ServerPlayer p,Stage stage){
        if(!allowed(p,stage)){p.displayClientMessage(Component.literal(stage==Stage.VOID?"虚空相咒胎需要先觉醒灵魂御厨子，且不能重复吸收；击败记录不等于已觉醒":"当前成长阶段不符，不能跨阶段或重复吸收"),true);return false;}
        float old=MaidBridge.body(p).getMaxHealth();var d=Curse.data(p);
        switch(stage){
            case RESIDUE->d.putInt("ResidueUpgrades",Math.min(34,d.getInt("ResidueUpgrades")+1));
            case SOUL->{d.putBoolean("SoulShrine",true);d.putBoolean("SoulPowerDouble",true);}
            case VOID->{d.putBoolean("VoidShrine",true);d.putBoolean("VoidBossReward70",true);}
            case LEGACY->{d.putBoolean("PureLandVictory",true);d.putBoolean("PureLandRewardR38",true);}
        }
        Curse.apply(p);if(p instanceof MaidBridge.Actor a)MaidBridge.apply(a);
        MaidBridge.body(p).heal(Math.max(0,MaidBridge.body(p).getMaxHealth()-old));Curse.add(p,Curse.maximum(p)*0.4);Curse.sync(p);Skills.syncAwakening(p);
        p.sendSystemMessage(Component.literal("成长已提升："+Journey.guide(p)));return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){if(!level.isClientSide&&!allowed(p,stage)){p.displayClientMessage(Component.literal(stage==Stage.VOID?"请先吃下灵魂御厨子碎片；虚空相咒胎不能跨阶段或重复吸收":"请先完成上一阶段；此物品不能重复吸收"),true);return InteractionResultHolder.fail(p.getItemInHand(hand));}return super.use(level,p,hand);}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){if(user instanceof ServerPlayer p&&!consume(p,stage))return stack;return super.finishUsingItem(stack,level,user);}
    public static boolean feed(ServerPlayer actor,ItemStack stack){
        if(Uraume.unlocked(actor)&&MaidBridge.isCaster(actor))return false;
        if(stack.getItem() instanceof StageFood food)return consume(actor,food.stage);
        if(stack.is(Content.FINGER.get())){if(Curse.fingers(actor)>=20)return false;Curse.data(actor).putBoolean("Memory",true);Curse.consume(actor,false);return true;}
        if(stack.is(Content.BODY.get())){if(Curse.fingers(actor)<20||Curse.awakened(actor))return false;Curse.data(actor).putBoolean("BuddhaDefeated",true);Curse.consume(actor,true);return true;}
        if(stack.is(ProgressionContent.NINE.get())){if(!Curse.awakened(actor)||Journey.nine(actor))return false;float old=actor.getMaxHealth();Curse.data(actor).putBoolean("Nine",true);Curse.data(actor).putInt("SoulLayers",9);Curse.apply(actor);actor.heal(Math.max(0,actor.getMaxHealth()-old));Curse.add(actor,Curse.maximum(actor)*0.4);return true;}
        return false;
    }
    public static boolean feedItem(ItemStack stack){return stack.getItem() instanceof StageFood||stack.is(Content.FINGER.get())||stack.is(Content.BODY.get())||stack.is(ProgressionContent.NINE.get())||stack.is(ProgressionContent.MEMORY.get());}
    public static ServerPlayer killer(net.minecraft.world.damagesource.DamageSource source){ServerPlayer p=CombatSettings.caster(source);return p!=null?p:MaidBridge.caster(source.getEntity());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void drop(LivingDeathEvent event){
        if(event.isCanceled()||event.getEntity().level().isClientSide)return;
        LivingEntity victim=event.getEntity();ServerPlayer p=killer(event.getSource());
        LivingEntity credit=victim.getKillCredit();if(p==null&&credit!=null)p=credit instanceof ServerPlayer player?player:MaidBridge.caster(credit);
        if(victim instanceof Player||victim instanceof net.minecraft.world.entity.decoration.ArmorStand)return;
        if((p!=null||credit!=null)&&victim.getRandom().nextInt(ShrineRules.RESIDUE_DROP_DENOMINATOR)==0)victim.spawnAtLocation(RESIDUE.get());
        if(!(victim.level() instanceof net.minecraft.server.level.ServerLevel level))return;
        if(victim instanceof YujiEntity y&&!y.decoy()&&!y.familyMember()){
            boolean eligible=p!=null&&SoulJourney.ready(p);
            for(var entity:level.getAllEntities())if(entity instanceof LivingEntity living&&living.distanceToSqr(y)<128*128&&y.participated(living.getUUID())){ServerPlayer participant=MaidBridge.progression(living);if(participant!=null&&SoulJourney.ready(participant))eligible=true;}
            if(eligible)victim.spawnAtLocation(SOUL.get());
        }
        // 五条悟在确认死亡后统一结算任务和实物；不再按最后一击的成长阶段漏掉掉落。
    }
}
