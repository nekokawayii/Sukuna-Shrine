package cn.blockforge.generated.nnnnr1201forge2;

import java.util.Comparator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

public final class ReverseOutput {
    public static boolean maidReady(MaidBridge.Actor actor){
        ServerPlayer owner=MaidBridge.owner(actor);
        if(!GuardianCombat.awakened(actor)||Curse.active(actor,"ReverseOutputCooldown")||!Curse.canSpend(actor,100)||owner==null||!owner.isAlive()||owner.isSpectator()||owner.level()!=actor.maid.level()||actor.maid.distanceToSqr(owner)>1024||!actor.maid.hasLineOfSight(owner))return false;
        if(owner.getHealth()<owner.getMaxHealth())return true;
        for(var armor:owner.getArmorSlots())if(armor.isDamaged())return true;
        return false;
    }
    // 耐久只能是整数；2.5点拆成每次2点、另有50%概率补1点，避免取整变成3点。
    private static int repairAmount(ServerPlayer p){double amount=BalanceRules.strength(25,BalanceRules.Category.HEALING);int whole=(int)amount;return whole+(p.getRandom().nextDouble()<amount-whole?1:0);}
    private static void healTarget(ServerPlayer p,LivingEntity target){
        if(!Curse.spend(p,100))return;Curse.status(p,"ReverseOutputCooldown",20);target.heal(BalanceRules.damage(10,BalanceRules.Category.HEALING));
        for(var armor:target.getArmorSlots())if(armor.isDamageableItem())armor.setDamageValue(Math.max(0,armor.getDamageValue()-repairAmount(p)));
        Network.send(p.serverLevel(),new Network.Effect(137,target.getId(),target.position(),Vec3.ZERO,1,20,0));
    }
    public static void cast(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator()||DomainClash.active(p)||!GuardianCombat.awakened(p)||Curse.active(p,"ReverseOutputCooldown"))return;
        if(p instanceof MaidBridge.Actor actor){if(maidReady(actor))healTarget(p,MaidBridge.owner(actor));return;}
        Vec3 eye=p.getEyePosition(),dir=p.getLookAngle();
        LivingEntity target=p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(32),e->e!=p&&e.isAlive()&&p.hasLineOfSight(e)&&e.getBoundingBox().getCenter().distanceToSqr(eye)<=1024&&e.getBoundingBox().getCenter().subtract(eye).normalize().dot(dir)>=Math.cos(Math.toRadians(12))).stream().min(Comparator.<LivingEntity>comparingDouble(e->1-e.getBoundingBox().getCenter().subtract(eye).normalize().dot(dir)).thenComparingDouble(p::distanceToSqr)).orElse(null);
        if(target==null){if(Curse.spend(p,100)){Curse.status(p,"ReverseOutputCooldown",20);Network.send(p.serverLevel(),new Network.Effect(137,p.getId(),Skills.aimPoint(p,16),Vec3.ZERO,1,20,0));}return;}
        if(!Curse.canSpend(p,100))return;healTarget(p,target);
        p.displayClientMessage(Component.literal("术式反转外放：恢复"+BalanceRules.number(BalanceRules.strength(10,BalanceRules.Category.HEALING))+"生命，每件护甲平均修复"+BalanceRules.number(BalanceRules.strength(25,BalanceRules.Category.HEALING))+"耐久"),true);
    }
    private ReverseOutput(){}
}
