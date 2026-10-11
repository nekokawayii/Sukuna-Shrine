package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** 独立参数不受五条悟奖励倍率影响；控制器原样保存输入。 */
public final class GuardianSettings {
    public static final String[] KEYS={"Health","Armor","HitCap","Punch","FlashChance","FlashExponent","Reach","PunchInterval","SoulDelay","SoulSlowSeconds","BloodCount","BloodSpeed","BloodDamage","PoisonSeconds","PoisonDamage","PoisonSlow","PoisonCooldown","BoostSeconds","SpeedMultiplier","AttackSpeedMultiplier","DamageMultiplier","GridRadius","GridDamage","RangedReduction","MeleeImmune","DomainRadius","DomainSeconds","DomainDamage","DomainInterval","TeleportDistance","DashWindup","DashDistance","ComboCount","ComboInterval","ComboSlow","ComboSlowSeconds","HealPerSecond","HealWindup","HealSeconds","HealRecovery","MoveSpeed","SkillGap","GuardWindup","GuardSeconds","GuardCooldown","HealStun","BeamWindup","BeamSeconds","BeamCooldown","BeamDamage","BeamRange","BeamInterval"};
    public static final String[] LABELS={"净土生命","净土防御","单次受伤上限","咒力拳基础伤害","黑闪概率（%）","黑闪伤害次方","拳击距离","普攻间隔（秒）","径庭拳延迟（秒）","灵魂僵直（秒）","超新星数量","超新星速度（格/刻）","超新星伤害","毒血持续（秒）","毒血每秒伤害","毒血缓慢等级","毒血冷却倍率","赤鳞持续（秒）","赤鳞额外移速","赤鳞减少攻击间隔（刻）","赤鳞额外伤害","网格斩半径","网格斩伤害","远程减伤（%）","近战免疫（1开0关）","净土领域半径","净土领域持续（秒）","领域必中斩伤害","领域必中间隔（秒）","连续拳瞬移距离","连续拳前摇（秒）","连续拳冲刺距离","连续拳次数","连续拳间隔（秒）","连续拳缓慢等级","连续拳缓慢（秒）","反转每秒回血","反转前摇（秒）","反转回血持续（秒）","反转后摇（秒）","基础移动速度","招式间隔（秒）","斩击无下限前摇（秒）","斩击无下限持续（秒）","斩击无下限冷却（秒）","反转打断僵直（秒）","百敛·穿血前摇（秒）","百敛·穿血持续（秒）","百敛·穿血冷却（秒）","百敛·穿血每次伤害","百敛·穿血射程","百敛·穿血伤害间隔（秒）"};
    public static final double[] DEFAULTS={800000,80,10000,8,99,2.5,3.4,0.3,0.5,5,30,1.15,8,10,2,2,1.5,20,0.42,3,8,64,40,75,1,48,20,12,1,3,1,5,10,0.15,3,3,5000,1,5,3,0.42,0,2,30,90,2,2,6,20,20,64,0.25};
    public static CompoundTag data(Player p){CompoundTag root=Curse.data(p);if(!root.contains("GuardianSettings"))root.put("GuardianSettings",new CompoundTag());CompoundTag t=root.getCompound("GuardianSettings");defaults(t);return t;}
    public static void defaults(CompoundTag t){if(!t.getBoolean("GuardianR38")){if(!t.contains("GuardCooldown")||t.getDouble("GuardCooldown")==60)t.putDouble("GuardCooldown",90);t.putBoolean("GuardianR38",true);}if(!t.getBoolean("GuardianR32")){if(!t.contains("HealPerSecond")||t.getDouble("HealPerSecond")==500)t.putDouble("HealPerSecond",5000);t.putBoolean("GuardianR32",true);}for(int i=0;i<KEYS.length;i++)if(!t.contains(KEYS[i]))t.putDouble(KEYS[i],DEFAULTS[i]);}
    public static double value(CompoundTag t,String key){double n=t.getDouble(key);return Double.isFinite(n)?n:0;}
    private GuardianSettings(){}
}
