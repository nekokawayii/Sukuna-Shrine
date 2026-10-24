package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;

public final class GuardianCombat {
    public static void poison(LivingEntity p,GuardianEntity source,int ticks,float damage,double cooldown){ServerPlayer caster=MaidBridge.progression(p);var t=caster!=null?Curse.data(caster):p.getPersistentData();t.putLong("GuardianPoisonUntil",p.level().getGameTime()+ticks);t.putFloat("GuardianPoisonDamage",damage);t.putDouble("GuardianPoisonCooldown",cooldown);t.putUUID("GuardianPoisonSource",source.getUUID());}
    public static double cooldown(Player p){return Math.max(Curse.active(p,"GuardianPoisonUntil")?Math.max(1,Curse.data(p).getDouble("GuardianPoisonCooldown")):1,Curse.active(p,"PlayerGuardianPoisonUntil")?Math.max(1,Curse.data(p).getDouble("PlayerGuardianPoisonCooldown")):1);}
    public static boolean awakened(Player p){return Curse.data(p).getBoolean("PureLandVictory");}
    public static double multiplier(Player p){return 1;}
    public static void reward(ServerPlayer p){StageFood.consume(p,StageFood.Stage.LEGACY);}
    @SubscribeEvent public static void heal(LivingHealEvent e){if(e.getEntity() instanceof GuardianEntity g&&g.friendly()||e.getEntity() instanceof ServerPlayer p&&Curse.active(p,"PureLandNoHeal")||GuardianEntity.locked(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void cap(LivingDamageEvent e){if(!DeathStar.source(e.getSource())&&e.getEntity() instanceof GuardianEntity g)e.setAmount(GuardianRules.cap(e.getAmount(),g.cap()));}
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent e){LivingEntity p=e.getEntity();if(!(p.level() instanceof ServerLevel level)||p.tickCount%20!=0||!p.isAlive()||p instanceof Player player&&(player.isCreative()||player.isSpectator()))return;ServerPlayer caster=MaidBridge.progression(p);var t=caster!=null?Curse.data(caster):p.getPersistentData();if(t.getLong("GuardianPoisonUntil")<=level.getGameTime())return;var source=t.hasUUID("GuardianPoisonSource")?level.getEntity(t.getUUID("GuardianPoisonSource")):null;if(source==null)return;if(source instanceof GuardianEntity g&&g.friendly()){t.remove("GuardianPoisonUntil");return;}ServerPlayer owner=CombatSafety.owner(source);if(owner!=null){if(!CombatSafety.canAffect(owner,p)){t.remove("GuardianPoisonUntil");return;}CombatSafety.hurt(owner,p,p.damageSources().indirectMagic(source,source),BalanceRules.damage(t.getFloat("GuardianPoisonDamage"),BalanceRules.Category.BOSS_ATTACK));}else p.hurt(p.damageSources().indirectMagic(source,source),BalanceRules.damage(t.getFloat("GuardianPoisonDamage"),BalanceRules.Category.BOSS_ATTACK));}
    private GuardianCombat(){}
}
