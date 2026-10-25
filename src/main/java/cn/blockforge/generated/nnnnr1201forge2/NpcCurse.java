package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * NPC 咒力池（借用玩家技能接口层的资源层）。
 * · 每个敌对生物（含原版怪物）第一次被查询时按「最大生命的 20 倍」初始化；
 * · 每秒回复最大值的 2%，死亡/换维度自动清零重建；
 * · 拥有咒式的 NPC 通过 {@link #spend} 扣费，扣不起就放不出来。
 */
public final class NpcCurse {
    private static final String KEY="NpcCurseEnergy", MAX="NpcCurseMax";
    public static double max(LivingEntity e){
        var d=e.getPersistentData();
        if(!d.contains(MAX)){
            double value=Math.max(100.0,e.getMaxHealth()*20.0);   // 用户指定：血量上限的 20 倍
            d.putDouble(MAX,value);d.putDouble(KEY,value);
        }
        return d.getDouble(MAX);
    }
    public static double energy(LivingEntity e){max(e);return e.getPersistentData().getDouble(KEY);}
    public static void add(LivingEntity e,double amount){
        double cap=max(e);
        e.getPersistentData().putDouble(KEY,Math.max(0,Math.min(cap,energy(e)+amount)));
    }
    /** 施术扣费；不足返回 false（调用方自行决定是否放弃本次施法）。 */
    public static boolean spend(LivingEntity e,double amount){
        if(amount<=0)return true;
        double now=energy(e);
        if(now<amount)return false;
        e.getPersistentData().putDouble(KEY,now-amount);
        return true;
    }
    /** 原版与自定义怪物都缓慢回蓝：每 20 刻回复上限的 2%。 */
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){
        LivingEntity e=event.getEntity();
        if(e.level().isClientSide||!(e instanceof Mob)||e.tickCount%20!=0)return;
        add(e,max(e)*0.02);
    }
    private NpcCurse(){}
}
