package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;

/** 无量空处的**强制控制**：被抓住的敌人绝对无法移动（拴点 + 速度压到接近 0 + 禁止跳跃位移被拉回）。
 *
 *  <p>免疫名单（一律不生效）：施法者自己、自己的女仆本体、**任何女仆**、**任何宠物/驯服生物**、
 *  被任何玩家用**永恒挚爱**选中的生物，以及——通过统一的 {@link CombatSettings#canHit} 判定——
 *  **盟友或默认关系的玩家**、创造模式玩家、未开 PvP 的服务器、自己名下的生物。</p> */
public final class VoidLock {
    /** true = 这个目标不能被无量空处抓住。 */
    public static boolean immune(LivingEntity caster,LivingEntity victim){
        if(victim==null||victim==caster||!victim.isAlive()||victim.isRemoved()||victim.isSpectator())return true;
        if(caster instanceof ServerPlayer cp&&victim==MaidBridge.body(cp))return true; // 自己的女仆本体
        if(MaidBridge.isMaid(victim))return true;                          // 女仆：一个都不控
        if(victim instanceof TamableAnimal pet&&pet.isTame())return true;  // 宠物（含别人的宠物）
        if(EternalLove.protectedByAnyone(victim))return true;              // 永恒挚爱选中的生物
        if(caster instanceof ServerPlayer p){
            // 敌我总判定：盟友/默认玩家、创造、未开PvP、自己名下的生物、永恒挚爱都会在这里被排除
            return !CombatSettings.canHit(p,victim);
        }
        return false;
    }
    public static boolean valid(LivingEntity caster,LivingEntity victim){return !immune(caster,victim);}

    /**
     * 抓住：第一次被抓住的位置就是锚点，之后每刻把人/怪拴回去。
     * 速度用"移动缓慢 V"压到 10%（属性下限是 0，不会变成负速度倒退），
     * 玩家再叠一层连接传送——客户端自己就走不动，跳跃与击退同样会被拉回原位。
     */
    public static void hold(LivingEntity victim,long now){
        var tag=victim.getPersistentData();
        boolean expired=tag.getLong("GojoVoidUntil")<now||!tag.contains("GojoVoidX");
        if(expired){ // 上一次的控制已经过期：重新取锚点，避免被拉回很久以前的位置
            tag.putDouble("GojoVoidX",victim.getX());
            tag.putDouble("GojoVoidY",victim.getY());
            tag.putDouble("GojoVoidZ",victim.getZ());
        }
        tag.putLong("GojoVoidUntil",now+3);
        tag.putLong("GojoVoid",now+3); // 与五条悟的"情报灌输"共用同一个锁定标记
        double x=tag.getDouble("GojoVoidX"),y=tag.getDouble("GojoVoidY"),z=tag.getDouble("GojoVoidZ");
        victim.stopRiding();
        victim.fallDistance=0;
        victim.setDeltaMovement(0,Math.min(0,victim.getDeltaMovement().y),0); // 水平速度清零，保留下落避免悬空
        victim.hurtMarked=true;
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,10,5,false,false));
        victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,20,4,false,false));
        if(victim instanceof ServerPlayer p){
            if(p.isFallFlying())p.stopFallFlying();
            p.connection.teleport(x,y,z,p.getYRot(),p.getXRot());
        }else if(victim instanceof Mob mob){
            mob.getNavigation().stop();
            mob.setDeltaMovement(0,0,0);
            victim.setPos(x,y,z);
        }else victim.setPos(x,y,z);
    }
    /** 解除控制：清掉锚点与锁定标记，下一次被抓会从新位置重新计算。 */
    public static void free(LivingEntity victim){
        if(victim==null)return;
        var tag=victim.getPersistentData();
        tag.remove("GojoVoidX");tag.remove("GojoVoidY");tag.remove("GojoVoidZ");
        tag.remove("GojoVoidUntil");tag.remove("GojoVoid");
    }
    private VoidLock(){}
}
