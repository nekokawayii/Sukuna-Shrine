package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class VoidJourney {
    public static final ResourceKey<Level> ARENA=ResourceKey.create(Registries.DIMENSION,new ResourceLocation("sukuna","void_arena"));
    public static boolean evolved(Player p){return Curse.data(p).getBoolean("VoidShrine");}
    public static void enter(ServerPlayer p){
        if(p.level().dimension()==ARENA){leave(p);return;}
        ServerLevel l=p.server.getLevel(ARENA);if(l==null){p.sendSystemMessage(Component.literal("虚空竞技场尚未载入，请重新打开存档。"));return;}
        var d=Curse.data(p);d.putString("VoidReturnDimension",p.level().dimension().location().toString());d.putDouble("VoidReturnX",p.getX());d.putDouble("VoidReturnY",p.getY());d.putDouble("VoidReturnZ",p.getZ());
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)l.getChunk(x,z);
        Skills.closeDomain(p);Skills.cancelCharge(p);p.teleportTo(l,0.5,-59,32.5,180,0);
        if(l.getEntitiesOfClass(GojoEntity.class,new AABB(-64,-64,-64,64,0,64),GojoEntity::isAlive).isEmpty()){
            GojoEntity b=VoidContent.GOJO.get().create(l);if(b!=null){b.moveTo(0.5,-59,0.5,0,0);b.setTarget(p);l.addFreshEntity(b);b.setHealth(b.getMaxHealth());}
        }
        p.sendSystemMessage(Component.literal("虚空竞技场 · 基岩层 Y=-60 · 最强咒术师：五条悟。右键虚空钥匙或 /sukuna void leave 返回。"));
    }
    public static void leave(ServerPlayer p){
        if(p.level().dimension()!=ARENA)return;var d=Curse.data(p);ServerLevel l=null;
        try{l=p.server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation(d.getString("VoidReturnDimension"))));}catch(RuntimeException ignored){}
        if(l==null)l=p.server.overworld();var spawn=l.getSharedSpawnPos();
        clear(p);Skills.closeDomain(p);Skills.cancelCharge(p);p.teleportTo(l,d.contains("VoidReturnX")?d.getDouble("VoidReturnX"):spawn.getX()+0.5,d.contains("VoidReturnY")?d.getDouble("VoidReturnY"):spawn.getY(),d.contains("VoidReturnZ")?d.getDouble("VoidReturnZ"):spawn.getZ()+0.5,p.getYRot(),0);
    }
    public static boolean defeated(Player p){return Curse.data(p).getBoolean("VoidBossDefeated");}
    private static void addCredit(java.util.Map<java.util.UUID,ServerPlayer> credited,ServerPlayer p){if(p!=null)credited.put(p.getUUID(),p);}
    public static void reward(net.minecraft.world.entity.LivingEntity boss,net.minecraft.world.damagesource.DamageSource source){
        if(!(boss.level() instanceof ServerLevel level))return;
        java.util.Map<java.util.UUID,ServerPlayer> credited=new java.util.LinkedHashMap<>();
        addCredit(credited,CombatSettings.caster(source));
        if(source.getEntity() instanceof net.minecraft.world.entity.LivingEntity living)addCredit(credited,MaidBridge.progression(living));
        if(boss instanceof GojoEntity gojo&&gojo.getKillCredit()!=null)addCredit(credited,MaidBridge.progression(gojo.getKillCredit()));
        if(boss instanceof GojoEntity gojo)for(java.util.UUID id:gojo.participants())addCredit(credited,MaidBridge.find(level,id));
        // 参与女仆和主人都记通关；主人无需先吃记忆，也不因此直接获得术式。
        for(ServerPlayer p:java.util.List.copyOf(credited.values()))if(p instanceof MaidBridge.Actor actor){
            MaidBridge.recordBossDefeat(actor,boss);addCredit(credited,MaidBridge.owner(actor));
        }
        for(ServerPlayer p:credited.values()){
            Curse.data(p).putBoolean("VoidBossDefeated",true);
            Curse.data(p).putBoolean("VoidSettlementR60",true);Curse.sync(p);
            p.sendSystemMessage(Component.literal("五条悟已击败，通关已记录！首领掉落1个虚空相咒胎与1枚六眼；咒胎需先觉醒灵魂御厨子再吸收，六眼右键吃下即获得五条悟技能轮盘（无阶段要求，也可潜行右键喂给女仆）。"));
        }
        // 每个真正死亡的首领只掉一份；掉落不再取决于最后一击者是否觉醒。
        var drop=boss.spawnAtLocation(StageFood.VOID.get());
        if(drop!=null){drop.setInvulnerable(true);drop.setExtendedLifetime();if(level.dimension()==ARENA&&drop.getY()<-59)drop.setPos(0.5,-58,0.5);}
        var fragment=boss.spawnAtLocation(VoidContent.FRAGMENT.get());
        if(fragment!=null){fragment.setInvulnerable(true);fragment.setExtendedLifetime();if(level.dimension()==ARENA&&fragment.getY()<-59)fragment.setPos(0.5,-58,0.5);}
    }
    public static void repairOldVictory(ServerPlayer p){
        if(!p.isAlive())return;
        var d=Curse.data(p);if(d.getBoolean("VoidSettlementR60"))return;
        boolean recorded=defeated(p)||evolved(p)||(!(p instanceof MaidBridge.Actor)&&p.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED,VoidContent.GOJO.get())>0);
        if(!recorded)return;
        boolean compensate=!evolved(p);ServerPlayer recipient=p;
        if(p instanceof MaidBridge.Actor actor){
            ServerPlayer owner=MaidBridge.owner(actor);
            if(owner!=null){
                compensate&=!Curse.data(owner).getBoolean("VoidSettlementR60");recipient=owner;
                Curse.data(owner).putBoolean("VoidBossDefeated",true);Curse.data(owner).putBoolean("VoidSettlementR60",true);Curse.sync(owner);
            }
        }
        d.putBoolean("VoidBossDefeated",true);d.putBoolean("VoidSettlementR60",true);
        if(compensate){
            Journey.give(recipient,new ItemStack(StageFood.VOID.get()));
            recipient.sendSystemMessage(Component.literal("旧版五条悟通关记录已修复，补发1个虚空相咒胎（仅一次）；自己或女仆需先觉醒灵魂御厨子才能吸收。"));
        }
        Curse.sync(p);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        if(p.tickCount%20==0)repairOldVictory(p);
        if(p.level().dimension()==ARENA&&p.isAlive()&&p.tickCount%10==0&&(p.getY()<-60||p.getY()>-30||p.position().multiply(1,0,1).lengthSqr()>47*47))p.teleportTo(p.serverLevel(),0.5,-59,38.5,180,0);
    }
    @SubscribeEvent public static void livingTick(LivingEvent.LivingTickEvent e){
        var p=e.getEntity();if(p.level().isClientSide)return;
        if(p.getPersistentData().contains("VoidEyeUntil")&&p.getPersistentData().getLong("VoidEyeUntil")<=p.level().getGameTime()){p.getPersistentData().remove("VoidEyeUntil");var speed=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(java.util.UUID.fromString("731c3ef0-7dc0-422e-b4cd-0fae77215392"));}
        if(p.getPersistentData().contains("VoidCaptured")&&p.getPersistentData().getLong("VoidCaptured")<=p.level().getGameTime()){p.getPersistentData().remove("VoidCaptured");p.setInvisible(false);}
    }
    @SubscribeEvent public static void capturedDamage(LivingAttackEvent e){var p=e.getEntity();if(p.getPersistentData().getLong("VoidCaptured")>p.level().getGameTime()&&!e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))e.setCanceled(true);}
    @SubscribeEvent public static void heal(LivingHealEvent e){if(e.getEntity().getPersistentData().getLong("VoidNoHeal")>e.getEntity().level().getGameTime())e.setCanceled(true);}
    static void clear(Player p){p.getPersistentData().remove("VoidNoHeal");p.getPersistentData().remove("VoidEyeUntil");if(p.getPersistentData().contains("VoidCaptured")){p.getPersistentData().remove("VoidCaptured");p.setInvisible(false);}var a=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);if(a!=null)a.removeModifier(java.util.UUID.fromString("731c3ef0-7dc0-422e-b4cd-0fae77215392"));}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){clear(e.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){clear(e.getEntity());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){clear(e.getEntity());}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("sukuna").then(Commands.literal("void")
        .then(Commands.literal("enter").executes(c->{enter(c.getSource().getPlayerOrException());return 1;}))
        .then(Commands.literal("leave").executes(c->{leave(c.getSource().getPlayerOrException());return 1;}))
        .then(Commands.literal("key").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();if(!SoulJourney.evolved(p)&&!Journey.maidCleared(p,"YujiDefeated"))return 0;if(!p.getInventory().contains(new ItemStack(VoidContent.KEY.get())))Journey.give(p,new ItemStack(VoidContent.KEY.get()));return 1;}))
        .then(Commands.literal("gojo")
            .then(Commands.literal("status").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();
                p.sendSystemMessage(Component.literal("六眼："+(GojoPlayer.unlocked(p)?"已睁开 · 右侧轮盘应显示":"未睁开 · 右键使用六眼即为睁开（无阶段要求）")));
                p.sendSystemMessage(Component.literal("咒力："+BalanceRules.number(Curse.energy(p))+" / "+BalanceRules.number(Curse.maximum(p))
                    +" · 熔断："+(Curse.burnout(p)?"是":"否")+" · 对拼惩罚："+((DomainClash.penaltyTicks(p)+19)/20)+"秒 · 对拼中："+(DomainClash.active(p)?"是":"否")));
                p.sendSystemMessage(Component.literal("无量空处："+(GojoCombat.domainTicks(p)>0?"展开中 · 剩余 "+((GojoCombat.domainTicks(p)+19)/20)+" 秒":"未展开")
                    +" · 冷却 "+GojoCombat.cooldownSeconds(p,7)+" 秒 · 当前消耗 "+BalanceRules.number(GojoPlayer.COST[7])+" 咒力"));
                StringBuilder wheel=new StringBuilder("法轮：");
                if(MahoragaRules.wheelOnHead(p)){
                    var done=MahoragaAdaptStore.completed(p);
                    wheel.append("戴在头上 · 已完全适应 ").append(done.size()).append(" 项");
                    if(!done.isEmpty())wheel.append("（").append(String.join("、",done)).append("）");
                    var inProgress=new StringBuilder();
                    var tag=Curse.data(p).getCompound("MahoAdapted");
                    for(String key:tag.getAllKeys()){int v=tag.getInt(key);if(v>0&&v<MahoragaAdaptStore.CLEAR)inProgress.append(key).append(' ').append(v).append('/').append(MahoragaAdaptStore.CLEAR).append("  ");}
                    if(inProgress.length()>0)wheel.append(" · 累积中：").append(inProgress.toString().trim());
                }else wheel.append("未戴在头上（自身适应不生效）");
                p.sendSystemMessage(Component.literal(wheel.toString()));
                StringBuilder sb=new StringBuilder("冷却：");
                for(int i=0;i<GojoPlayer.COUNT;i++)sb.append(GojoPlayer.NAMES[i]).append(" ").append(GojoCombat.cooldownSeconds(p,i)).append("s ");
                p.sendSystemMessage(Component.literal(sb.toString().trim()));return 1;}))
            .then(Commands.literal("clear").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();Curse.data(p).remove("GojoMemory");Curse.sync(p);p.sendSystemMessage(Component.literal("已收回六眼记录，右侧轮盘隐藏"));return 1;})))
        .then(Commands.literal("status").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();repairOldVictory(p);p.sendSystemMessage(Component.literal(evolved(p)?"虚空御厨子已解锁":defeated(p)?"五条悟已击败，通关已记录；自己或女仆先觉醒灵魂御厨子，再吃下虚空相咒胎":"尚未记录五条悟通关：击败首领掉落虚空相咒胎，灵魂御厨子阶段食用后觉醒虚空御厨子"));return 1;}))));}
}
