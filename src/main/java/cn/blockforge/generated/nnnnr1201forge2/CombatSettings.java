package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

/** 每位施术者独立保存敌友关系和术式倍率，客户端只提交选择。 */
public final class CombatSettings {
    public static final int DEFAULT=0, ALLY=1, ENEMY=2, PAGE_SIZE=6;
    private static final int MAX_SAVED=2048;

    private static CompoundTag relations(Player p){return Curse.data(p).getCompound("Relations");}
    public static int relation(Player owner,UUID target){return relations(owner).getCompound(target.toString()).getInt("Mode");}
    public static boolean voidFloor(Player p){return Curse.data(p).getBoolean("VoidFloor");}
    public static void toggle(ServerPlayer p,int kind){
        if(kind!=1)return;
        CompoundTag d=Curse.data(p);d.putBoolean("VoidFloor",!voidFloor(p));Skills.syncVoidFloor(p);sync(p,0);p.sendSystemMessage(Component.literal("领域地面虚空特效："+(voidFloor(p)?"开启":"关闭")));
    }
    public static double multiplier(Player p){
        CompoundTag d=Curse.data(p);
        double value=d.contains("DamageMultiplier")?d.getDouble("DamageMultiplier"):1;
        return Double.isFinite(value)&&value>=0.001&&value<=10000?value:1;
    }
    /** 保留既有60%及御厨子额外削弱；女仆的基础伤害和成长整体再减半。 */
    public static float casterDamage(Player p,double amount){return BalanceRules.damage(CasterDamageRules.scale(amount,MaidBridge.isCaster(p)));}
    public static double unscaledSpellMultiplier(Player p){return 0.6*multiplier(p)*Curse.damageFactor(p)*Curse.residueFactor(p);}
    public static double baseSpellMultiplier(Player p){return BalanceRules.strength(unscaledSpellMultiplier(p)*CasterDamageRules.casterFactor(MaidBridge.isCaster(p)));}
    public static double spellMultiplier(Player p){return baseSpellMultiplier(p);}
    public static float spellDamage(Player p,double base){return BalanceRules.damage(CasterDamageRules.spell(base,multiplier(p),Curse.spellBonus(p),Curse.damageFactor(p),Curse.residueFactor(p),false,MaidBridge.isCaster(p)));}
    public static float domainSpellDamage(Player p,double base){return BalanceRules.damage(CasterDamageRules.spell(base,multiplier(p),Curse.domainSpellBonus(p),Curse.damageFactor(p),Curse.residueFactor(p),true,MaidBridge.isCaster(p)));}
    public static float fixedSpellDamage(Player p,double base,boolean domain){return BalanceRules.damage(CasterDamageRules.fixed(base,domain?Curse.domainSpellBonus(p):Curse.spellBonus(p),Curse.damageFactor(p),Curse.residueFactor(p),MaidBridge.isCaster(p)));}
    public static boolean canHit(ServerPlayer owner,LivingEntity target){
        if(target==owner||!target.isAlive()||target.isSpectator()||EternalLove.protects(owner,target))return false;
        if(MahoragaAdaptStore.friendlyTo(owner,target))return false; // 友方魔虚罗：绝对友方，术式不伤它
        if(owner instanceof MaidBridge.Actor actor)return MaidBridge.canHit(actor,target);
        if(target instanceof net.minecraft.world.entity.TamableAnimal maid&&EternalLove.duel(maid)&&owner.getUUID().equals(maid.getOwnerUUID()))return true;
        if(target instanceof net.minecraft.world.entity.TamableAnimal tame&&owner.getUUID().equals(tame.getOwnerUUID()))return false;
        if(target instanceof GuardianEntity g&&g.friendly()||target instanceof YujiEntity y&&y.familyMember())return false;
        int mode=relation(owner,target.getUUID());
        if(mode==ALLY)return false;
        if(target instanceof Player player){
            if(player.isCreative()||!owner.server.isPvpAllowed())return false;
            // 主动标敌可以覆盖队伍关系，但不能覆盖服务器禁用PvP。
            return mode==ENEMY;
        }
        return mode==ENEMY||!target.isAlliedTo(owner);
    }
    private static final Map<net.minecraft.world.damagesource.DamageSource,ServerPlayer> spellOwners=new IdentityHashMap<>();
    public static ServerPlayer caster(net.minecraft.world.damagesource.DamageSource source){if(source.getEntity() instanceof ServerPlayer p)return p;ServerPlayer mapped=spellOwners.get(source);return mapped!=null?mapped:MaidBridge.caster(source.getEntity());}
    public static boolean hurt(ServerPlayer owner,LivingEntity target,net.minecraft.world.damagesource.DamageSource source,float amount){
        if(!canHit(owner,target)||SixfoldFinale.controlled(owner)||!SixfoldFinale.active(owner)&&(Uraume.locked(owner)||NetBranches.locked(owner)))return false;
        if(!(owner instanceof MaidBridge.Actor)&&target instanceof Player&&relation(owner,target.getUUID())==ENEMY){
            // 已通过服务端PvP许可；只对这一次术式绕过队伍禁伤，不修改队伍或服务器设置。
            source=new net.minecraft.world.damagesource.DamageSource(source.typeHolder(),owner,null,owner.position());
            spellOwners.put(source,owner);
            target.setLastHurtByPlayer(owner);
        }
        if(owner instanceof MaidBridge.Actor actor){
            // 已由canHit授权的玩家命中不能再被驯养生物友伤或难度缩放吞掉；
            // 保留直接命中者和独立施术归属，不解除其他模组主动取消的伤害事件。
            var cause=target instanceof Player?null:actor.maid;
            source=new net.minecraft.world.damagesource.DamageSource(source.typeHolder(),actor.maid,cause,actor.maid.position());spellOwners.put(source,owner);
        }
        try{boolean accepted=target.hurt(source,amount);if(accepted&&amount>0&&!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)&&!source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))SkillDestruction.impact(owner,target.position(),1);return accepted;}finally{AttackTypes.consume();spellOwners.remove(source);}
    }
    /** 同队禁伤会早于LivingDamageEvent返回，只为明确标敌的咒术近战补上入口。 */
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void enemyMelee(net.minecraftforge.event.entity.player.AttackEntityEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p)||MaidBridge.isCaster(p)||!(event.getTarget() instanceof Player target))return;
        if(target.canHarmPlayer(p)||!canHit(p,target)||Curse.fingers(p)<5||!Curse.canUseTechnique(p)||SoulCombat.suppressed(p)||DomainClash.active(p)||DomainClash.active(target))return;
        float strength=p.getAttackStrengthScale(0.5f);
        float amount=(float)p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)*(0.2f+strength*strength*0.8f);
        amount+=net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(p.getMainHandItem(),target.getMobType())*strength;
        p.resetAttackStrengthTicker();
        if(hurt(p,target,p.damageSources().playerAttack(p),amount))p.setLastHurtMob(target);
        event.setCanceled(true);
    }
    public static void change(ServerPlayer owner,UUID id,int mode,int page){
        if(id.equals(owner.getUUID())||mode<DEFAULT||mode>ENEMY)return;
        CompoundTag saved=relations(owner);
        ServerPlayer target=owner.server.getPlayerList().getPlayer(id);
        // 只接受在线玩家或先前保存过的玩家，不能由客户端伪造档案。
        if(target==null&&!saved.contains(id.toString())){sync(owner,page);return;}
        if(mode==DEFAULT)saved.remove(id.toString());
        else {
            if(!saved.contains(id.toString())&&saved.getAllKeys().size()>=MAX_SAVED){owner.sendSystemMessage(Component.literal("敌友名单已满，请先把不用的记录恢复默认。"));sync(owner,page);return;}
            CompoundTag entry=new CompoundTag();entry.putInt("Mode",mode);
            entry.putString("Name",target==null?saved.getCompound(id.toString()).getString("Name"):target.getGameProfile().getName());
            saved.put(id.toString(),entry);
        }
        Curse.data(owner).put("Relations",saved);
        if(mode!=ENEMY){
            Skills.protectAlly(owner,id);
            if(target!=null)SoulCombat.clearOwnedEffects(owner,target);
        }
        GuardianProgress.relationsChanged(owner);
        sync(owner,page);
    }
    public static void sync(ServerPlayer owner,int requestedPage){
        Map<UUID,Network.RelationRow> rows=new HashMap<>();
        CompoundTag saved=relations(owner);
        for(String key:saved.getAllKeys()){
            try{UUID id=UUID.fromString(key);CompoundTag entry=saved.getCompound(key);int mode=entry.getInt("Mode");
                if(!id.equals(owner.getUUID())&&(mode==ALLY||mode==ENEMY))rows.put(id,new Network.RelationRow(id,entry.getString("Name"),mode,false));
            }catch(IllegalArgumentException ignored){}
        }
        for(ServerPlayer target:owner.server.getPlayerList().getPlayers())if(target!=owner){
            String name=target.getGameProfile().getName();
            if(saved.contains(target.getUUID().toString()))saved.getCompound(target.getUUID().toString()).putString("Name",name);
            rows.put(target.getUUID(),new Network.RelationRow(target.getUUID(),name,relation(owner,target.getUUID()),true));
        }
        List<Network.RelationRow> sorted=new ArrayList<>(rows.values());
        sorted.sort(Comparator.comparing(Network.RelationRow::online).reversed().thenComparing(Network.RelationRow::name,String.CASE_INSENSITIVE_ORDER).thenComparing(r->r.id().toString()));
        int pages=Math.max(1,(sorted.size()+PAGE_SIZE-1)/PAGE_SIZE),page=Math.max(0,Math.min(pages-1,requestedPage));
        int start=page*PAGE_SIZE,end=Math.min(sorted.size(),start+PAGE_SIZE);
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->owner),new Network.Relations(page,pages,multiplier(owner),voidFloor(owner),List.copyOf(sorted.subList(start,end))));
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("sukuna").then(Commands.literal("balance").executes(c->{
            c.getSource().sendSuccess(()->Component.literal((BalanceRules.modpackMode()?"整合包模式：开启，强度100%":"整合包模式：关闭，强度10%（降低90%）")+"。咒力上限、恢复、消耗始终使用原值；持续时间、冷却、数量和范围不变。配置：config/sukuna-growth.toml 的 [balance] modpackMode，修改后重启。"),false);return 1;
        })));
        event.getDispatcher().register(Commands.literal("sukuna").then(Commands.literal("damage")
            .executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();
                p.sendSystemMessage(Component.literal("当前术式伤害倍率："+multiplier(p)+" 倍（/sukuna damage 数字 可改，1 为原倍率）"));
                p.sendSystemMessage(Component.literal("通用因子：模式强度 x"+BalanceRules.factor()+" · 术式基础 0.6 · 成长加成 +"+Curse.spellBonus(p)+" · 状态 x"+Curse.damageFactor(p)+" · 残渣 x"+Curse.residueFactor(p)));
                p.sendSystemMessage(Component.literal("苍(每秒) 原值10 → "+BalanceRules.number(spellDamage(p,10))+" · 赫 原值60 → "+BalanceRules.number(spellDamage(p,60))+" · 茈 原值500 → "+BalanceRules.number(spellDamage(p,500))+"（打魔虚罗 x"+GojoCombat.MAHORAGA_DAMAGE+" + 缓慢 "+GojoCombat.PURPLE_SLOW_TICKS/20+" 秒） · 无限制茈 原值999 → "+BalanceRules.number(spellDamage(p,999))+"（打魔虚罗 x"+GojoCombat.MAHORAGA_DAMAGE_UNLIMITED+"）"));
                return 1;})
            .then(Commands.argument("multiplier",DoubleArgumentType.doubleArg(0.001,10000)).requires(s->s.hasPermission(2)).executes(c->{
                ServerPlayer p=c.getSource().getPlayerOrException();double value=DoubleArgumentType.getDouble(c,"multiplier");
                if(!Double.isFinite(value)||value<0.001||value>10000){c.getSource().sendFailure(Component.literal("倍率必须是 0.001～10000 之间的有效数字。"));return 0;}
                Curse.data(p).putDouble("DamageMultiplier",value);
                p.sendSystemMessage(Component.literal("你的全部术式伤害已设为 "+value+" 倍（1 为原倍率）。"));return 1;
            }))));
    }
}
