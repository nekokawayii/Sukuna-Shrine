package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 存档保存控制器原值，生效时按模式换算；重载不会重复缩放。 */
public final class BossControl extends SavedData {
    private CompoundTag values=new CompoundTag();
    public static BossControl get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(BossControl::load,BossControl::new,"sukuna_boss_control");}
    public static BossControl load(CompoundTag tag){BossControl s=new BossControl();s.values=tag.copy();return s;}
    @Override public CompoundTag save(CompoundTag tag){tag.merge(values);return tag;}
    public double value(String key,double fallback){return values.contains(key)?values.getDouble(key):fallback;}
    public void set(MinecraftServer server,String key,double value){values.putDouble(key,value);setDirty();for(ServerLevel level:server.getAllLevels())for(var e:level.getAllEntities())if(e instanceof LivingEntity living)apply(living,key);}
    public static double value(LivingEntity boss,String key,double fallback){return boss.getServer()==null?fallback:get(boss.getServer()).value(key,fallback);}
    public static boolean war(LivingEntity boss){return value(boss,"AllHostile",0)>0;}
    public static CompoundTag guardian(MinecraftServer server){CompoundTag tag=get(server).values.getCompound("Guardian").copy();GuardianSettings.defaults(tag);return tag;}
    public void guardian(MinecraftServer server,String key,double value){CompoundTag tag=guardian(server);tag.putDouble(key,value);values.put("Guardian",tag);setDirty();for(ServerLevel level:server.getAllLevels())for(var e:level.getAllEntities())if(e instanceof GuardianEntity g)g.refreshSettings(tag,key);}
    // 五条悟固定 150000、魔虚罗固定 400000 原值（关闭模式按"固定值首领生命"比值生效），都不受控制器遗留值影响。
    private static String healthKey(LivingEntity boss){return boss instanceof GojoEntity||boss instanceof MahoragaEntity?null:boss instanceof YujiEntity?"YujiHealth":boss instanceof BuddhaEntity?"BuddhaHealth":null;}
    private static boolean managed(LivingEntity boss){return BossTargets.managed(boss);}
    private static boolean excluded(LivingEntity boss){return boss instanceof GuardianEntity g&&g.friendly()||boss instanceof YujiEntity y&&(y.decoy()||y.familyMember())||boss instanceof MahoragaEntity m&&m.ally();} // 友方魔虚罗固定 800 血，不受控制面板缩放
    private static void resize(LivingEntity boss,double raw,boolean fill){
        var a=boss.getAttribute(Attributes.MAX_HEALTH);if(a==null)return;
        // 五条悟/魔虚罗等"固定值首领"走 BOSS_FIXED 列，其余首领走 BOSS 列；开启模式两列都是 1.0。
        BalanceRules.Category category=fixed(boss)?BalanceRules.Category.BOSS_FIXED:BalanceRules.Category.BOSS;
        double expected=BalanceRules.health(raw,category);float oldMax=boss.getMaxHealth(),old=boss.getHealth();
        if(a.getBaseValue()!=expected||!a.getModifiers().isEmpty()){a.removeModifiers();a.setBaseValue(expected);boss.setHealth((float)BalanceRules.healthAfterResize(old,oldMax,boss.getMaxHealth()));}
        if(fill)boss.setHealth(boss.getMaxHealth());
    }
    /** 原版固定值首领：生命不随控制器原值变化，关闭模式也要有按新基数的关闭值。 */
    private static boolean fixed(LivingEntity boss){return boss instanceof GojoEntity||boss instanceof MahoragaEntity;}
    private static final java.util.UUID ATTACK_SCALE=java.util.UUID.fromString("ee670b21-4d44-4c54-84a8-b6eab9696401");
    private static void attribute(LivingEntity boss,net.minecraft.world.entity.ai.attributes.Attribute key,double raw){
        var a=boss.getAttribute(key);if(a==null)return;
        if(key==Attributes.ATTACK_DAMAGE){
            // 放在总倍率末端，戴天的力量药水加伤也只缩放一次。
            a.setBaseValue(raw);var previous=a.getModifier(ATTACK_SCALE);double amount=BalanceRules.factor(BalanceRules.modpackMode(),BalanceRules.Category.BOSS_ATTACK)-1;
            if(previous!=null&&previous.getAmount()==amount)return;
            a.removeModifier(ATTACK_SCALE);if(amount!=0)a.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(ATTACK_SCALE,"整合包模式攻击缩放",amount,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        }else if(a.getBaseValue()!=BalanceRules.strength(raw,key==Attributes.ARMOR?BalanceRules.Category.BOSS_ARMOR:BalanceRules.Category.BOSS))a.setBaseValue(BalanceRules.strength(raw,key==Attributes.ARMOR?BalanceRules.Category.BOSS_ARMOR:BalanceRules.Category.BOSS));
    }
    private static void stats(LivingEntity boss,boolean fill){
        if(!managed(boss)||excluded(boss)||boss instanceof GuardianEntity)return;
        boolean mother=boss instanceof ParasiteEntity p&&p.mother();
        // 兼容r63强化母体：旧档只有480生命的属性，第一次加载时补上身份标记。
        if(mother&&boss.getAttribute(Attributes.MAX_HEALTH).getBaseValue()==480)boss.getPersistentData().putBoolean("SukunaRevealedMother",true);
        boolean enhanced=mother&&boss.getPersistentData().getBoolean("SukunaRevealedMother");
        double health=boss instanceof GojoEntity?150000:boss instanceof MahoragaEntity?MahoragaEntity.HEALTH:boss instanceof YujiEntity?30000:boss instanceof BuddhaEntity?5000:boss instanceof DaitianEntity?200:boss instanceof BrokenMahitoEntity?20000:mother?(enhanced?480:160):12;
        String key=healthKey(boss);resize(boss,key==null?health:value(boss,key,health),fill);
        double attack=boss instanceof MahoragaEntity?30:boss instanceof DaitianEntity?5:boss instanceof BuddhaEntity?10:boss instanceof YujiEntity?5:boss instanceof BrokenMahitoEntity?40:mother?(enhanced?24:12):2;
        attribute(boss,Attributes.ATTACK_DAMAGE,attack);
        if(boss instanceof MahoragaEntity)attribute(boss,Attributes.ARMOR,MahoragaEntity.ARMOR);
        if(boss instanceof BrokenMahitoEntity)attribute(boss,Attributes.ARMOR,20);
    }
    public static void revealedMother(ParasiteEntity mother){mother.getPersistentData().putBoolean("SukunaRevealedMother",true);stats(mother,true);}
    private void apply(LivingEntity boss,String changed){
        if(!managed(boss)||excluded(boss))return;
        if(boss instanceof GuardianEntity g){g.refreshSettings(guardian(boss.getServer()),changed==null?"":changed);return;}
        stats(boss,changed!=null&&changed.equals(healthKey(boss)));
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void enforce(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event){
        var boss=event.getEntity();if(boss.level().isClientSide||!managed(boss)||excluded(boss))return;
        if(boss instanceof GuardianEntity g)g.enforceSettings();else stats(boss,false);
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent e){if(e.getLevel() instanceof ServerLevel level&&e.getEntity() instanceof LivingEntity living&&managed(living)&&!excluded(living)){get(level.getServer()).apply(living,null);if(!e.loadedFromDisk()){if(living instanceof GuardianEntity g)g.refreshSettings(guardian(level.getServer()),"Health");else living.setHealth(living.getMaxHealth());}}}
}
