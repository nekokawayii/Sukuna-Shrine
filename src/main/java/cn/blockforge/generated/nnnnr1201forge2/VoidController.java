package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class VoidController {
    // 字段顺序即界面顺序；五条悟使用固定数值，不再暴露首领生命/频率/伤害三项。
    private static final String[] KEYS={"VoidDamage","DomainSeconds","Resonance","MaxRifts","SlashLength","BlackFlash","RedCut","YujiHealth","YujiFrequency","YujiDamage","BuddhaHealth","BuddhaFrequency","BuddhaDamage"};
    private static final double[] DEFAULTS={1,20,1,64,60,100,1,30000,1,1,5000,1,1};
    private static final int RED_CUT=6;
    private static boolean bossField(int field){return field==0||field==5||field>=7;}
    private static CompoundTag tag(Player p){CompoundTag root=Curse.data(p);if(!root.contains("VoidController"))root.put("VoidController",new CompoundTag());CompoundTag tag=root.getCompound("VoidController");for(int i=0;i<KEYS.length;i++)if(i!=RED_CUT&&!tag.contains(KEYS[i]))tag.putDouble(KEYS[i],DEFAULTS[i]);if(!tag.getBoolean("RedCutR29")){tag.putBoolean("RedCut",true);tag.putBoolean("RedCutR29",true);}return tag;}
    private static double raw(Player p,int field){return bossField(field)&&p.getServer()!=null?BossControl.get(p.getServer()).value(KEYS[field],DEFAULTS[field]):tag(p).getDouble(KEYS[field]);}
    public static boolean boosted(Player p){return Curse.data(p).getBoolean("VoidBossReward70");}
    public static void reward(ServerPlayer p){Curse.data(p).putBoolean("VoidBossReward70",true);Curse.apply(p);}
    public static double voidDamage(Player p){return raw(p,0);}
    public static int domainTicks(Player p){return (int)Math.min(1200000,Math.max(0,raw(p,1))*20);}
    public static double resonance(Player p){return raw(p,2);}
    public static int maxRifts(Player p){return (int)Math.max(0,Math.min(2048,raw(p,3)));}
    public static int slashLength(Player p){return (int)Math.max(0,Math.min(100000,raw(p,4)));}
    public static int blackFlashChance(Player p){return (int)Math.max(0,Math.min(100,raw(p,5)));}
    public static boolean redCut(Player p){return tag(p).getBoolean("RedCut");}
    public static double yujiHealth(Player p){return raw(p,7);}public static double yujiFrequency(Player p){return raw(p,8);}public static double yujiDamage(Player p){return raw(p,9);}
    public static double buddhaHealth(Player p){return raw(p,10);}public static double buddhaFrequency(Player p){return raw(p,11);}public static double buddhaDamage(Player p){return raw(p,12);}
    public static Network.ControllerState state(ServerPlayer p){CompoundTag guardian=BossControl.guardian(p.server);guardian.putBoolean("PureLandVictory",GuardianCombat.awakened(p));guardian.putBoolean("ModpackMode",BalanceRules.modpackMode());guardian.putBoolean("AllHostile",BossControl.get(p.server).value("AllHostile",0)>0);guardian.putBoolean("Desblock",SkillDestruction.enabled(p.serverLevel()));guardian.putBoolean("AimEnabled",AimAssist.enabled(p));guardian.putDouble("AimAngle",AimAssist.angle(p));guardian.putDouble("AimRange",AimAssist.range(p));guardian.putBoolean("AimCircle",AimAssist.circle(p));return new Network.ControllerState(voidDamage(p),domainTicks(p)/20,resonance(p),maxRifts(p),slashLength(p),blackFlashChance(p),redCut(p),boosted(p),yujiHealth(p),yujiFrequency(p),yujiDamage(p),buddhaHealth(p),buddhaFrequency(p),buddhaDamage(p),guardian);}
    public static void change(ServerPlayer p,int field,int delta){if(field==AimAssist.ENABLED||field==AimAssist.CIRCLE){AimAssist.toggle(p,field);}else if(field==201){if(p.hasPermissions(2))SkillDestruction.set(p.server,!SkillDestruction.enabled(p.serverLevel()));else p.displayClientMessage(Component.literal("只有管理员能修改方块破坏开关"),true);}else if(field==200){BossControl.get(p.server).set(p.server,"AllHostile",BossControl.get(p.server).value("AllHostile",0)>0?0:1);}else if(field==RED_CUT)tag(p).putBoolean("RedCut",!redCut(p));else if(field>=0&&field<KEYS.length){setValue(p,field,Double.toString(raw(p,field)+delta));return;}Network.sendController(p);}
    public static void setValue(ServerPlayer p,int field,String value){try{
        if(field==RED_CUT)tag(p).putBoolean("RedCut",Boolean.parseBoolean(value));
        else{double n=Double.parseDouble(value);if(!Double.isFinite(n)||n<0)throw new NumberFormatException();
            if(field==AimAssist.ANGLE||field==AimAssist.RANGE)AimAssist.set(p,field,n);
            else if(field>=100&&field<100+GuardianSettings.KEYS.length)BossControl.get(p.server).guardian(p.server,GuardianSettings.KEYS[field-100],n);
            else if(field>=0&&field<KEYS.length){if(bossField(field))BossControl.get(p.server).set(p.server,KEYS[field],n);else tag(p).putDouble(KEYS[field],n);}else return;
        }p.displayClientMessage(Component.literal("控制器参数已立即生效"),true);
    }catch(NumberFormatException ex){p.displayClientMessage(Component.literal(field==AimAssist.ANGLE?"索敌半角请填0.5～15度":field==AimAssist.RANGE?"索敌距离请填4～256格":"请输入大于或等于0的有效数字"),true);}Network.sendController(p);}
    private VoidController(){}
}
