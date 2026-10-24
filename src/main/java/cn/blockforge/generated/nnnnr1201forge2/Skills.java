package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.sounds.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

public final class Skills {
    public static final String[] NAMES={"解 · 远程斩击","解 · 八方罗网","捌 · 必断斩击","开 · 炎弓","灶 · 粉尘爆炸","领域展开 · 伏魔御厨子","灵魂剥离"};
    public static final String[] AWAKENED={"解 · 世界斩","解 · 天球罗网","捌 · 虚空捌","开 · 焚天炎弓","灶 · 众生焚爆","领域展开 · 燃烧御厨子","灵魂剥离"};
    private static final int[] COOLDOWN={0,60,40,60,100,20,1800};
    private static final Map<UUID,long[]> cooldowns=new HashMap<>();
    private static final Map<UUID,Charge> cleaveCharges=new HashMap<>();
    private record Impact(ServerPlayer owner,ServerLevel level,UUID target,long due,float amount,boolean world,boolean rift,boolean soulSlash,double fraction){
        Impact(ServerPlayer owner,ServerLevel level,UUID target,long due,float amount,boolean world,boolean rift,boolean soulSlash){this(owner,level,target,due,amount,world,rift,soulSlash,1);}
    }
    private static final List<Impact> impacts=new ArrayList<>();
    private record MeleeCut(ServerPlayer owner,ServerLevel level,UUID target,long due,float amount){}
    private static final List<MeleeCut> meleeCuts=new ArrayList<>();
    private record Strike(ServerPlayer owner,ServerLevel level,UUID target,Vec3 point,Vec3 direction,long due,float amount,float size,long tint){}
    private static final List<Strike> strikes=new ArrayList<>();
    private record SoulCut(ServerPlayer owner,ServerLevel level,UUID target,Vec3 point,Vec3 direction,long due,float amount,float size,long angle,boolean world){}
    private static final List<SoulCut> soulCuts=new ArrayList<>();
    private static void soulDismantle(ServerPlayer p,LivingEntity target,Vec3 point,Vec3 dir,float size,float amount,boolean world){
        if(target!=null){Vec3 center=target.getBoundingBox().getCenter(),toward=p.getEyePosition().subtract(center).normalize();point=target.getBoundingBox().inflate(0.08).clip(p.getEyePosition(),center).orElse(center).add(toward.scale(0.05));}
        long angle=2160+p.getRandom().nextInt(360);
        fx(p,96,point,dir,size,6,angle);
        List<LivingEntity> victims=size==SlashReach.INFINITE?SlashReach.targets(p,target,point,dir,angle):target==null?List.of():List.of(target);
        for(LivingEntity victim:victims){
            scaledDamage(p.serverLevel(),p,victim,amount*0.35f,world,false,true,SlashFormRules.DAMAGE);
            soulCuts.add(new SoulCut(p,p.serverLevel(),victim.getUUID(),point,dir,p.level().getGameTime()+6,amount,0,angle,world));
        }
        soulCuts.add(new SoulCut(p,p.serverLevel(),null,point,dir,p.level().getGameTime()+6,0,size,angle,world));
        RuinAtmosphere.echo(p);
    }
    private record Furnace(ServerPlayer owner,ServerLevel level,long due){}
    private static final List<Furnace> furnaces=new ArrayList<>();
    /** 剑气弹幕的基础飞行速度（格/刻）：原来硬编码 2，现在提成常量，"斩击速度 −70%"就是乘 0.3。 */
    public static final double SLASH_SPEED=2;
    /** 领域随机斩击的寿命（刻）：短命，只闪一下就走完伤害判定。 */
    public static final int DOMAIN_CUT_LIFE=6;
    /** 领域里同时存在的静止斩击上限（密度×10 后需要更大的池子）。 */
    public static final int DOMAIN_CUT_LIMIT=2000;
    private static final List<Slash> slashes=new ArrayList<>();
    private static final Map<UUID,Aura> auras=new HashMap<>();
    private static final Map<UUID,Domain> domains=new HashMap<>();
    /** 领域结印状态（技能5 伏魔御厨子 / 五条悟技能7 无量空处共用同一套）：按下起 3 秒不能移动，80 刻领域出现，再按释放键取消/放下。 */
    private static final Map<UUID,Seal> seals=new HashMap<>();
    /** 结印类型：{@link #SEAL_SHRINE}=伏魔御厨子（技能5），{@link #SEAL_VOID}=无量空处（五条悟技能7）。 */
    public static final int SEAL_SHRINE=0,SEAL_VOID=1;
    /** 跳神龛的飞行状态（三击空格）。 */
    private static final List<Leap> leaps=new ArrayList<>();
    /** 「结印期间不能移动」用的移速修饰符：×0（与灶·最大功率同一套做法，UUID 必须独立）。 */
    private static final UUID SEAL_SLOW=UUID.fromString("7c1f0a6e-2b94-4d5a-9f31-4a6d2e8b0c17");
    private static final List<Bow> bows=new ArrayList<>();
    private static final List<Fire> fires=new ArrayList<>();
    private static final List<Detonation> detonations=new ArrayList<>();
    public static final int CHARGE_DELAY=6,CHARGE_TIME=60;
    private record Joint(ServerPlayer owner,ServerLevel level,UUID target,long due,float power){}
    private static final Map<String,Joint> joints=new HashMap<>();
    private static final List<Rift> rifts=new ArrayList<>();
    private record Burn(UUID owner,long until,double multiplier){}
    private static final Map<LivingEntity,Burn> burns=new HashMap<>();
    private static void ignite(ServerPlayer owner,LivingEntity target,int seconds){
        if(!canHit(owner,target))return;
        if(SoulJourney.evolved(owner)){burns.put(target,new Burn(owner.getUUID(),Long.MAX_VALUE,CombatSettings.spellMultiplier(owner)));Curse.permanentSoulFire(owner,target);fx(owner,95,target.getBoundingBox().getCenter(),Vec3.ZERO,1,40,owner.getId());return;}
        target.setSecondsOnFire(seconds);
        Burn previous=burns.get(target);long until=target.level().getGameTime()+seconds*20L;
        if(previous==null||until>=previous.until)burns.put(target,new Burn(owner.getUUID(),until,CombatSettings.spellMultiplier(owner)));
    }
    /** 有归属的短时燃烧；不会因灵魂进化自动升级成永久火。 */
    public static void burn(ServerPlayer owner,LivingEntity target,int seconds){
        if(!CombatSafety.canAffect(owner,target)||seconds<=0)return;
        target.setSecondsOnFire(seconds);long until=target.level().getGameTime()+seconds*20L;Burn old=burns.get(target);
        if(old==null||old.until<=until)burns.put(target,new Burn(owner.getUUID(),until,CombatSettings.spellMultiplier(owner)));
    }
    public static void protectAlly(ServerPlayer owner,UUID target){
        CombatExpansion.protect(owner,target);VoidCombat.protect(owner,target);NetBranches.protect(owner,target);Uraume.protect(owner,target);
        joints.remove(owner.getUUID()+"/"+target);
        for(Rift r:rifts)if(r.owner.getUUID().equals(owner.getUUID())&&target.equals(r.target))r.target=null;
        burns.entrySet().removeIf(entry->{if(entry.getKey().getUUID().equals(target)&&entry.getValue().owner.equals(owner.getUUID())){entry.getKey().clearFire();return true;}return false;});
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void fistDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event){
        ServerPlayer p=CombatSettings.caster(event.getSource());if(event.isCanceled()||event.getAmount()<=0||p==null)return;
        LivingEntity target=event.getEntity();if(!canHit(p,target))return;
        remember(p,target);
        if(!(event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||MaidBridge.isCaster(p)&&event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))||Curse.fingers(p)<5||MaidBridge.recovering(p)||SoulCombat.suppressed(p)||!Curse.canUseTechnique(p))return;
        // 取本次普通攻击结算伤害；附加解不再叠术式成长，不递归触发普攻事件。
        if(meleeCuts.size()<4096)meleeCuts.add(new MeleeCut(p,p.serverLevel(),target.getUUID(),p.level().getGameTime()+1,event.getAmount()*0.7f));
        if(!Journey.unlocked(p)||!Curse.data(p).getBoolean("Fist"))return;
        float base=event.getAmount();event.setAmount((float)(base+10*CombatSettings.baseSpellMultiplier(p)));
        target.knockback(2.5,p.getX()-target.getX(),p.getZ()-target.getZ());target.hurtMarked=true;
        fx(p,69,target.getBoundingBox().getCenter(),p.getLookAngle(),2,12,0);
    }
    private static void remember(ServerPlayer p,LivingEntity target){
        var memory=cleaveMemory.computeIfAbsent(p.getUUID(),k->new LinkedHashSet<>());
        if(memory.size()>=4096)memory.remove(memory.iterator().next());memory.add(target.getUUID());
    }
    private static boolean remembered(ServerPlayer p,LivingEntity target){return cleaveMemory.getOrDefault(p.getUUID(),Set.of()).contains(target.getUUID());}
    @SubscribeEvent public static void burnDamage(net.minecraftforge.event.entity.living.LivingHurtEvent event){
        if(!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE))return;
        LivingEntity target=event.getEntity();Burn burn=burns.get(target);
        if(burn==null&&target.getPersistentData().getBoolean("SukunaSoulFire")&&target.getPersistentData().hasUUID("SukunaSoulFireOwner")){
            var tag=target.getPersistentData();UUID id=tag.getUUID("SukunaSoulFireOwner");ServerPlayer caster=target.level() instanceof ServerLevel level?MaidBridge.find(level,id):null;
            if(caster!=null)tag.putBoolean("SukunaSoulFireMaid",MaidBridge.isCaster(caster));
            if(!tag.contains("SukunaSoulFireMaid")){event.setCanceled(true);return;}
            burn=new Burn(id,Long.MAX_VALUE,BalanceRules.strength(CasterDamageRules.scale(0.5,tag.getBoolean("SukunaSoulFireMaid")),BalanceRules.Category.MAID_SPELL));
        }
        if(burn==null||target.level().getGameTime()>burn.until){VoidCombat.ignitionDamage(event);return;}
        ServerPlayer owner=target.level() instanceof ServerLevel level?MaidBridge.find(level,burn.owner):null;
        if(owner==null){event.setCanceled(true);return;} // 无法核验离线施术者的敌友名单，暂停归属火伤。
        if(owner!=null&&(DomainClash.active(owner)||domains.containsKey(owner.getUUID())&&clashing(domains.get(owner.getUUID()))&&inDomain(owner,target))){event.setCanceled(true);return;}
        if(EternalLove.protects(burn.owner,target)||owner!=null&&!canHit(owner,target)){event.setCanceled(true);target.clearFire();burns.remove(target);return;}
        event.setAmount(owner==null?(float)(event.getAmount()*burn.multiplier):CombatSettings.spellDamage(owner,event.getAmount())*(Curse.repaired(owner)?1.1f:1));
    }
    private static int nextRift;
    private static final Map<UUID,Integer> airCuts=new HashMap<>();
    private static class Rift {
        final ServerPlayer owner;final ServerLevel level;final int id;final double radius;final float power;
        Vec3 pos,dir;UUID target;int age;
        Rift(ServerPlayer p,Vec3 point,LivingEntity victim,double radius){owner=p;level=p.serverLevel();id=++nextRift;pos=point;this.radius=radius;power=(float)Curse.power(p);target=victim==null?null:victim.getUUID();dir=new Vec3(p.getRandom().nextDouble()-0.5,p.getRandom().nextDouble()-0.5,p.getRandom().nextDouble()-0.5).normalize();}
    }
    private static void rift(ServerPlayer p,Vec3 pos,LivingEntity target,double radius){
        if(rifts.stream().filter(r->r.owner==p).count()>=64)return;
        Rift r=new Rift(p,pos,target,radius);rifts.add(r);SkillSounds.play(p,"void_needle");Network.send(r.level,new Network.Effect(61,p.getId(),pos,r.dir,Journey.nine(p)?2:1,140,r.id));
    }
    private static void joint(ServerPlayer p,LivingEntity target){
        String key=p.getUUID()+"/"+target.getUUID();if(joints.containsKey(key)||joints.size()>=1024)return;
        joints.put(key,new Joint(p,p.serverLevel(),target.getUUID(),p.level().getGameTime()+20,(float)(60*Curse.scale(p)*Journey.bonus(p))));
        fx(p,65,target.getBoundingBox().getCenter(),Vec3.ZERO,3,20,target.getId());
    }
    private static class Slash {
        ServerPlayer owner;ServerLevel level;Vec3 pos,dir,axis;float size,power;int age,life;boolean world,space;Set<UUID> hit=new HashSet<>();
        /** 每刻飞行格数：**这就是"斩击速度"**（剑气弹幕的飞行速度）。手持生成了才定型，中途换手不影响已经在飞的。 */
        double speed=SLASH_SPEED;
        final long visualId=SlashTerrain.id();List<SlashForms.Line> fragments;
        /** 伤害系数（1 = 普通解）。简易领域的随机斩击用小于 1 的系数。 */
        float damageScale=1f;
        Slash(ServerPlayer p,Vec3 pos,Vec3 dir,Vec3 axis,float size){owner=p;level=p.serverLevel();this.pos=pos;this.dir=dir;this.axis=axis;this.size=size;power=(float)Curse.power(p);world=Curse.awakened(p);life=(int)(400*Curse.scale(p));speed=SLASH_SPEED*(Hiten.holding(p)?Hiten.SLASH_SLOW:1);}
    }
    private record Charge(ServerLevel level,long start){}
    private static final Map<UUID,Charge> charges=new HashMap<>();
    public static boolean inDomain(ServerPlayer p,LivingEntity target){Domain d=domains.get(p.getUUID());return !DomainClash.active(p)&&target!=null&&d!=null&&d.level==target.level()&&target.position().distanceToSqr(d.center)<=d.radius*d.radius;}
    public static boolean neutralizes(ServerPlayer p){return domains.values().stream().anyMatch(d->!DomainClash.active(d.owner)&&valid(d.owner,d.level)&&d.level==p.level()&&p.position().distanceToSqr(d.center)<=d.radius*d.radius);}
    private static boolean clashing(Domain d){return DomainClash.active(d.owner);}
    public static boolean domainClashing(ServerPlayer p){Domain d=domains.get(p.getUUID());return d!=null&&clashing(d);}
    public static Vec3 domainCenter(ServerPlayer p){Domain d=domains.get(p.getUUID());return d==null?p.position():d.center;}
    /** 领域半径：按本次展开已保存的点数算出来的那个值（没有领域时给默认分配下的半径）。虚空阶段的索敌范围这类
     *  外部入口要跟着半径走，就用这个取值，别再写死 200。 */
    public static double domainRadius(ServerPlayer p){Domain d=domains.get(p.getUUID());return d==null?DomainOptions.radius(DomainOptions.DEFAULT_RADIUS):d.radius;}
    public static void closeDomain(ServerPlayer p){closeDomain(p,false);}
    /** 领域结束：**只锁领域自己 30 秒**（原来走的全局熔断 45/60 秒，会把其它术式一起禁掉）。 */
    public static void closeDomain(ServerPlayer p,boolean voluntary){Domain d=domains.remove(p.getUUID());endSeal(p);if(d!=null){VoidCombat.closeDomain(p,valid(p,d.level)&&!clashing(d));Network.send(d.level,new Network.Effect(5,p.getId(),d.center,Vec3.ZERO,0,0,0));Curse.cooldownDomain(p);}}
    public static boolean domainActive(ServerPlayer p){return domains.containsKey(p.getUUID());}
    public static boolean clashDomainActive(ServerPlayer p){Domain d=domains.get(p.getUUID());return d!=null&&d.owner==p&&d.level==p.level()&&(DomainClash.active(p)||d.end>d.level.getGameTime());}
    public static void syncVoidFloor(ServerPlayer p){if(domains.containsKey(p.getUUID()))fx(p,25,p.position(),Vec3.ZERO,CombatSettings.voidFloor(p)?1:0,4,0);}
    private static void syncVoidShrine(Domain d){fx(d.owner,112,d.center,Vec3.ZERO,1,(int)Math.max(1,d.end-d.level.getGameTime()),GuardianCombat.awakened(d.owner)?1:0);}
    public static void syncAwakening(ServerPlayer p){Domain d=domains.get(p.getUUID());if(d!=null&&VoidJourney.evolved(p))syncVoidShrine(d);}

    // ---------------------------------------------------------------- 技能5 · 伏魔御厨子（结印 → 展开 → 放下）
    /** 结印中（从按下到"放下"或领域结束）：不能释放其它术式、不能攻击。 */
    public static boolean sealing(ServerPlayer p){return seals.containsKey(p.getUUID());}
    /** 该类型的结印是否正在进行（释放键只让对应那一路的输入进来）。 */
    public static boolean sealing(ServerPlayer p,int type){Seal s=seals.get(p.getUUID());return s!=null&&s.type==type;}
    /** 状态转换一律落盘（这个项目的规矩：静默失败必须能在日志里查）。 */
    private static void note(String message){com.mojang.logging.LogUtils.getLogger().info("[御厨子] "+message);}
    /** 无量空处同样"状态转换一律落盘"，只是前缀不同。 */
    private static void noteVoid(String message){com.mojang.logging.LogUtils.getLogger().info("[无量空处] "+message);}
    private static String where(Vec3 v){return String.format(Locale.ROOT,"(%.1f, %.1f, %.1f)",v.x,v.y,v.z);}
    /** 结印手势：御厨子 kind=70/size=5 走 CastHands 的"双掌合于身前"，无量空处 kind=71/size=7 走"单手竖在屏幕正中央"；life=0 = 放下（立刻失效）。 */
    private static void pose(ServerPlayer p,int type,int life){
        if(type==SEAL_VOID)fx(p,71,p.position(),p.getLookAngle(),7,life,7);
        else fx(p,70,p.position(),p.getLookAngle(),5,life,5);
    }
    /** 「结印期间不能移动」：移速 ×0 的属性修饰符（不用缓慢效果，抄灶·最大功率的写法）。 */
    private static void freeze(ServerPlayer p){
        var attr=MaidBridge.body(p).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if(attr==null)return;
        attr.removeModifier(SEAL_SLOW);
        attr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SEAL_SLOW,"sukuna_domain_seal",-1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    private static void unfreeze(ServerPlayer p){
        var attr=MaidBridge.body(p).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if(attr!=null&&attr.getModifier(SEAL_SLOW)!=null)attr.removeModifier(SEAL_SLOW);
    }
    /** 释放键（技能5）循环：①结印 ②再按=放下(伤害50%) ③**放下后再按一次=收起领域**。 */
    private static void domainPress(ServerPlayer p){
        if(Authority.instant(p)&&domains.containsKey(p.getUUID())){closeDomain(p,true);return;}
        // 先判条件再清"放下"标记：另一条领域的放下标记不能被这条路径吃掉
        if(released.contains(p.getUUID())&&domains.containsKey(p.getUUID())){
            released.remove(p.getUUID());
            note("收起御厨子（放下后再按一次）");
            closeDomain(p,true);CastFeedback.tell(p,"御厨子已收起");
            return;
        }
        Seal seal=seals.get(p.getUUID());
        if(seal!=null){
            if(seal.type!=SEAL_SHRINE){CastFeedback.tell(p,"无量空处正在结印中，无法展开御厨子");return;}
            if(seal.opened)release(p,SEAL_SHRINE);else cancelSeal(p,"再次按下释放键");
            return;
        }
        if(domains.containsKey(p.getUUID())){CastFeedback.tell(p,"御厨子已展开 · 再按=放下(伤害50%) · 放下后再按一次=收起 · 三击空格跳上神龛");return;}
        startSeal(p);
    }
    /** 结印起手：扣费与起手表现都在这一刻（此刻起 3 秒不能移动）。 */
    private static void startSeal(ServerPlayer p){
        if(SimpleDomain.owns(p)||FurnaceMax.active(p)){note("结印被拒：简易领域/最大功率接管中");return;}
        if(!Curse.canUseTechnique(p)){p.displayClientMessage(Component.literal("术式熔断中：只能使用术式反转或修复熔断"),true);return;}
        if(GuardianPlayerSkills.sustaining(p)){GuardianPlayerSkills.closeOwnedDomain(p,true);return;}
        if(MaidBridge.isCaster(p)&&(GuardianEntity.locked(MaidBridge.body(p))||GuardianPlayerSkills.locked(p))){closeDomain(p);p.displayClientMessage(Component.literal("净土领域优先，御厨子无法展开"),true);return;}
        if(!MaidBridge.domainReady(p)){CastFeedback.tell(p,"御厨子尚在维护中，暂时无法展开");return;}
        if(Curse.domainCooling(p)){CastFeedback.tell(p,"御厨子冷却中 · "+((Curse.domainCooldownLeft(p)+19)/20)+" 秒");return;}
        long now=p.serverLevel().getGameTime();
        if(SoulCombat.suppressed(p)&&!(Curse.active(p,"Iwate")&&!Curse.active(p,"SoulGap")&&p.getPersistentData().getLong("VoidCaptured")<=now)){p.displayClientMessage(Component.literal("灵魂剥离或灵魂压制中，无法使用术式"),true);return;}
        long[] times=cooldowns.computeIfAbsent(p.getUUID(),k->new long[7]);
        if(now<times[5]){CastFeedback.tell(p,NAMES[5]+" 冷却中 · "+String.format(Locale.ROOT,"%.1f",(times[5]-now)/20.0)+" 秒");return;}
        if(VoidJourney.evolved(p)&&!VoidCombat.ready(p,5)){note("结印被拒：虚空术式未就绪");return;}
        if(!Curse.spend(p,Curse.COST[5]))return;
        AttackTypes.prepare(AttackTypes.DISMANTLE);
        times[5]=now+(long)Math.ceil(COOLDOWN[5]*SoulCombat.cooldown(p)*(Curse.active(p,"Siphon")?1.3:1)*(Curse.tier(p)>=4?0.8:1));
        if(Authority.instant(p)){openDomain(p);return;}
        seals.put(p.getUUID(),new Seal(p,SEAL_SHRINE));
        freeze(p);
        fx(p,67,p.position(),Vec3.ZERO,Journey.nine(p)?1:0,1,0);SkillSounds.cast(p,5);
        pose(p,SEAL_SHRINE,ShrineRules.domainTicks(MaidBridge.isCaster(p))+ShrineRules.DOMAIN_OPEN_TICKS+400);
        CastFeedback.tell(p,"伏魔御厨子 · 结印中 · "+(ShrineRules.DOMAIN_FREEZE_TICKS/20)+" 秒无法移动 · 再按释放键取消");
        note("结印开始 "+p.getScoreboardName()+" 冻结"+ShrineRules.DOMAIN_FREEZE_TICKS+"刻 "+ShrineRules.DOMAIN_OPEN_TICKS+"刻后展开");
    }
    /** 领域还没出现就再次按下：整体取消本次结印（不生成领域，按 30 秒冷却处理）。 */
    private static void cancelSeal(ServerPlayer p,String why){
        Seal seal=seals.remove(p.getUUID());if(seal==null)return;
        unfreeze(p);pose(p,seal.type,0);Curse.cooldownDomain(p);
        CastFeedback.tell(p,"结印取消 · 御厨子冷却"+(ShrineRules.DOMAIN_COOLDOWN_TICKS/20)+" 秒");
        note("结印取消（"+why+"）"+p.getScoreboardName());
    }
    /** 放下：手势失效、可以自由行动；御厨子伤害降到 50%，无量空处转为"仅控制"（不再造成伤害），领域都继续存在。 */
    private static void release(ServerPlayer p,int type){
        Seal seal=seals.remove(p.getUUID());if(seal==null)return;
        unfreeze(p);pose(p,seal.type,0);
        if(seal.type==SEAL_VOID){dropVoid(p);return;}
        Domain d=domains.get(p.getUUID());if(d!=null)d.damageScale=ShrineRules.DOMAIN_RELEASE_SCALE;
        CastFeedback.tell(p,"放下结印 · 御厨子伤害降至"+(int)(ShrineRules.DOMAIN_RELEASE_SCALE*100)+"% · 再按一次释放键收起领域");
        if(domains.containsKey(p.getUUID()))released.add(p.getUUID());
        note("放下（伤害×"+ShrineRules.DOMAIN_RELEASE_SCALE+"）"+p.getScoreboardName());
    }
    /** 领域结束或异常：收起结印状态（解冻 + 放下手势）。 */
    private static void endSeal(ServerPlayer p){
        released.remove(p.getUUID());
        Seal seal=seals.remove(p.getUUID());if(seal==null)return;
        unfreeze(p);pose(p,seal.type,0);
    }
    /** 结印第 80 刻：领域效果出现（原 case 5 的整段逻辑，原样搬过来）。
     *  三项点数（半径/伤害/速度）在这一刻才读存档：界面按过「确定」就是刚保存的值，没按就是上次保存的值。 */
    private static void openDomain(ServerPlayer p){
        long now=p.serverLevel().getGameTime();
        int radiusPts=SukunaBranches.domainRadiusPts(p),damagePts=SukunaBranches.domainDamagePts(p),speedPts=SukunaBranches.domainSpeedPts(p);
        boolean verticalKill=SukunaBranches.domainVerticalKill(p);
        int radius=DomainOptions.radius(radiusPts);float damageMult=(float)DomainOptions.damage(damagePts),speedMult=(float)DomainOptions.speed(speedPts);
        MaidBridge.domainOpened(p);double yaw=Math.toRadians(p.getYRot());Vec3 behind=new Vec3(Math.sin(yaw)*12,0,-Math.cos(yaw)*12);
        LivingEntity locked=Authority.instant(p)?aimTarget(p):null;Vec3 center=locked==null?p.position():locked.position();
        int duration=ShrineRules.domainTicks(Authority.countedAsMaid(p));Domain d=new Domain(p,p.serverLevel(),center,behind,now,now+duration,radius,damageMult,speedMult,verticalKill);domains.put(p.getUUID(),d);
        if(VoidJourney.evolved(p))syncVoidShrine(d);
        fx(p,1,d.center,d.shrineOffset,(float)d.radius,duration,SoulJourney.evolved(p)?-now:now);
        fx(p,25,p.position(),Vec3.ZERO,CombatSettings.voidFloor(p)?1:0,10,0);
        if(!Authority.instant(p))pose(p,SEAL_SHRINE,duration+400);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS,4,0.5f);
        p.displayClientMessage(Component.literal(VoidJourney.evolved(p)?"虚空御厨子 · "+(duration/20)+"秒 · 半径"+radius+"格 · 五裂共鸣 · 结束闭合爆发":"领域展开 · 半径"+radius+"格 · 每秒70咒力 · 再按释放键放下（伤害50%）"),true);
        DomainClash.opened(p,domainCenter(p),radius);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,1.5f,0.5f);
        note("领域生成 "+p.getScoreboardName()+" 中心"+where(d.center)+" 神龛"+where(d.center.add(d.shrineOffset))+" "+duration+"刻"
            +" 点数 半径"+radiusPts+"/伤害"+damagePts+"/速度"+speedPts+" → "+radius+"格 ×"+String.format(Locale.ROOT,"%.2f",damageMult)+" ×"+String.format(Locale.ROOT,"%.2f",speedMult)
            +" 纵向伤敌"+(verticalKill?"开":"关"));
    }
    private static void tickSeals(){
        // 副本遍历：openDomain 会走 DomainClash.opened，对拼当场结算时会反过来收起结印状态。
        for(Seal seal:List.copyOf(seals.values())){
            if(!seals.containsKey(seal.owner.getUUID()))continue;   // 本轮已被取消或放下
            ServerPlayer p=seal.owner;long now=seal.level.getGameTime(),age=now-seal.start;
            if(!valid(p,seal.level)){seals.remove(p.getUUID());unfreeze(p);pose(p,seal.type,0);continue;}
            if(age<ShrineRules.DOMAIN_FREEZE_TICKS)freeze(p);   // 每刻重挂一次（幂等）
            else unfreeze(p);                                   // 3 秒后可以移动，手势保持
            if(seal.type==SEAL_VOID&&seal.midAt>0&&now>=seal.midAt){seal.midAt=0;SkillSounds.play(p,"gojo_domain_mid",SkillSounds.DOMAIN_VOLUME);}
            int openAt=seal.type==SEAL_VOID?ShrineRules.VOID_OPEN_TICKS:ShrineRules.DOMAIN_OPEN_TICKS;
            if(!seal.opened&&age>=openAt){seal.opened=true;if(seal.type==SEAL_VOID)openVoidDomain(p);else openDomain(p);}
        }
    }
    // ---------------------------------------------------------------- 五条悟技能7 · 无量空处（同一套 Seal：结印 → 80 刻展开 → 放下仅控制 → 收起）
    /** 释放键循环：①结印 ②再按=放下(此后仅控制、无伤害) ③**放下后再按一次=收起领域**。
     *
     *  <p>判定一律以**各自的权威状态**为准：领域在不在看 {@code GojoCombat.domainOpen}（无量空处的领域状态存在
     *  GojoCombat.State 里，**从不进** {@link #domains}），"放下过没有"看 {@link #released}，
     *  结印在不在看 {@link #seals}。原来"结印还没生成领域"的分支写在 release 上（release 第一行就
     *  {@code seals.remove}），一旦 Seal 先没了，这条输入就只剩最后那句提示——既放不下也收不起。
     *  现在放下走 {@link #dropVoid}：不依赖 Seal 是否还在，只要"领域开着且还没放下过"就一定能放下。</p> */
    public static void voidPress(ServerPlayer p){
        boolean open=GojoCombat.domainOpen(p),did=released.contains(p.getUUID());
        Seal s0=seals.get(p.getUUID());
        noteVoid("释放键 领域在="+open+" 已放下="+did+" 结印="+(s0==null?"无":(s0.type==SEAL_VOID?(s0.opened?"无量空处(已展开)":"无量空处(结印中)"):"御厨子"))+" "+p.getScoreboardName());
        // 先判条件再清"放下"标记：御厨子的放下标记不能被这条路径吃掉
        if(did&&open){
            released.remove(p.getUUID());
            noteVoid("收起成功 · 无量空处已关闭（放下后再按一次）"+p.getScoreboardName());
            GojoCombat.closeDomain(p);   // 收起文案与术式熔断仍由 GojoCombat 自己负责
            CastFeedback.tell(p,"无量空处已收起");
            return;
        }
        Seal seal=seals.get(p.getUUID());
        if(seal!=null&&seal.type==SEAL_VOID){if(seal.opened)dropVoid(p);else cancelVoidSeal(p,"再次按下释放键");return;}
        // 结印已不在（或从来不在）但领域还开着、又没放下过：这一步仍然必须是"放下"，不能掉进最后的提示里
        if(GojoCombat.domainOpen(p)&&!released.contains(p.getUUID())){dropVoid(p);return;}
        if(GojoCombat.domainOpen(p)){CastFeedback.tell(p,"无量空处已展开 · 再按=放下(仅控制) · 放下后再按一次=收起");return;}
        startVoidSeal(p);
    }
    /** 结印起手：门槛与扣费仍走 GojoCombat 原本那套（逐条说明原因），这里只管冻结与姿态。 */
    private static void startVoidSeal(ServerPlayer p){
        if(seals.containsKey(p.getUUID())){CastFeedback.tell(p,"另一种领域正在结印中");return;}
        if(!GojoCombat.payVoidSeal(p))return;
        Seal seal=new Seal(p,SEAL_VOID);
        // 按下释放键立刻播展开音效（音量翻 2.5 倍＝5），并把 mid 排在起手音效结束处无缝衔接
        SkillSounds.play(p,"gojo_domain_start",SkillSounds.DOMAIN_VOLUME);
        seal.midAt=seal.start+ShrineRules.VOID_START_SOUND_TICKS;
        seals.put(p.getUUID(),seal);
        freeze(p);
        pose(p,SEAL_VOID,GojoCombat.UNLIMITED_TICKS+ShrineRules.VOID_OPEN_TICKS+400);
        CastFeedback.tell(p,"无量空处 · 结印中 · "+(ShrineRules.DOMAIN_FREEZE_TICKS/20)+" 秒无法移动 · 再按释放键取消");
        noteVoid("结印开始 "+p.getScoreboardName()+" 冻结"+ShrineRules.DOMAIN_FREEZE_TICKS+"刻 "+ShrineRules.DOMAIN_OPEN_TICKS+"刻后展开");
    }
    /** 领域还没出现就再次按下：取消本次结印（扣掉的咒力与冷却不退，与御厨子的取消一致）。 */
    private static void cancelVoidSeal(ServerPlayer p,String why){
        if(!sealing(p,SEAL_VOID))return;
        endSeal(p);
        CastFeedback.tell(p,"结印取消 · 无量空处冷却"+GojoCombat.cooldownSeconds(p,7)+" 秒");
        noteVoid("结印取消（"+why+"）"+p.getScoreboardName());
    }
    /** 无量空处第 80 刻：领域真正出现（GojoCombat 里原来按下就执行的那段逻辑），单手结印姿态保持到领域结束。 */
    private static void openVoidDomain(ServerPlayer p){
        pose(p,SEAL_VOID,GojoCombat.UNLIMITED_TICKS+400);   // 先摆姿态再生成：对拼当场结算时会反过来收起结印状态
        GojoCombat.openVoidDomain(p);
        noteVoid("领域生成 "+p.getScoreboardName()+" 中心"+where(p.position())+" 半径"+(int)GojoFx.DOMAIN_RADIUS+"格 "+GojoCombat.UNLIMITED_TICKS+"刻"
            +" 结印在="+sealing(p,SEAL_VOID)+" 领域在="+GojoCombat.domainOpen(p)+" 已放下="+released.contains(p.getUUID()));
    }
    /** 「放下」：手势失效（life=0 的姿态包）+ 无量空处转为仅控制（伤害整段跳过），领域继续存在。
     *  <b>不要求 Seal 还在</b>——放下这一步的权威状态是"领域开着且还没放下过"，Seal 在不在只决定姿态要不要额外清一次。
     *  这是本轮的修复点：原来放下挂在 {@code release()} 上，而它第一行就把 Seal 摘了，Seal 一旦先没，
     *  再按释放键只会落到最后那句提示里（既不能放下也不能收起）。 */
    private static void dropVoid(ServerPlayer p){
        Seal seal=seals.remove(p.getUUID());
        if(seal!=null){unfreeze(p);pose(p,SEAL_VOID,0);}   // Seal 还在：解冻 + 发 life=0 让结印姿态立刻失效
        GojoCombat.controlOnly(p);                          // 仅控制：伤害那一路整段跳过
        released.add(p.getUUID());
        CastFeedback.tell(p,"放下结印 · 无量空处仅剩控制（无伤害） · 再按一次释放键收起领域");
        noteVoid("放下成功 · 仅控制（伤害整段跳过）"+p.getScoreboardName()
            +" 结印在="+(seal!=null)+" 领域在="+GojoCombat.domainOpen(p));
    }
    /** 无量空处关闭（收起/到时/对拼）：清掉它的结印状态（由 GojoCombat.closeDomain 与 cancel 调用）。 */
    public static void endVoidSeal(ServerPlayer p){
        Seal seal=seals.get(p.getUUID());
        if(seal!=null&&seal.type!=SEAL_VOID)return;   // 御厨子的结印不归这条路径管
        endSeal(p);
    }
    /** 三击空格：沿二次贝塞尔飞到"领域中心 + 神龛偏移 + 站立高度"，落点站住。 */
    public static void leapToShrine(ServerPlayer p){
        Domain d=domains.get(p.getUUID());
        if(d==null||!valid(p,d.level)){CastFeedback.tell(p,"御厨子领域未展开 · 无法跳上神龛");return;}
        if(leaps.stream().anyMatch(l->l.owner==p))return;
        Vec3 to=d.center.add(d.shrineOffset).add(0,ShrineRules.SHRINE_STAND_Y,0);
        leaps.add(new Leap(p,p.position(),to));
        p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        warp(p,p.position());
        note("跳神龛开始 "+p.getScoreboardName()+" "+where(p.position())+" → "+where(to));
    }
    /** 逐刻发一次传送，客户端才能平滑跟随。 */
    private static void warp(ServerPlayer p,Vec3 pos){
        if(p.connection!=null)p.connection.teleport(pos.x,pos.y,pos.z,p.getYRot(),p.getXRot());
        else p.teleportTo(pos.x,pos.y,pos.z);
    }
    private static void tickLeaps(){
        for(Iterator<Leap> it=leaps.iterator();it.hasNext();){
            Leap leap=it.next();ServerPlayer p=leap.owner;long now=leap.level.getGameTime();
            if(!valid(p,leap.level)||!domains.containsKey(p.getUUID())){it.remove();p.setNoGravity(false);p.fallDistance=0;continue;}
            p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
            warp(p,leap.at(now));
            if(!leap.done(now))continue;
            it.remove();p.setNoGravity(false);p.setDeltaMovement(Vec3.ZERO);
            CastFeedback.tell(p,"御厨子 · 神龛之上");
            note("跳神龛落点 "+p.getScoreboardName()+" "+where(p.position()));
        }
    }
    public static void cancelCharge(ServerPlayer p){charges.remove(p.getUUID());cleaveCharges.remove(p.getUUID());}
    public static void cancelSustained(ServerPlayer p){cancelCharge(p);if(auras.remove(p.getUUID())!=null)fx(p,9,p.position(),Vec3.ZERO,0,1,0);}
    /** 仅在实体tick或退出处理执行，不可在伤害队列的回调中修改集合。 */
    public static void cancelCombat(ServerPlayer p){
        CombatExpansion.cancel(p);DismantleMotion.cancel(p);SlashForms.cancel(p);SukunaBranches.cancel(p);Uraume.cancel(p);NetBranches.cancel(p);VoidCombat.cancelCombat(p);closeDomain(p);cancelSustained(p);
        slashes.removeIf(s->s.owner==p);strikes.removeIf(s->s.owner==p);soulCuts.removeIf(s->s.owner==p);
        impacts.removeIf(s->s.owner==p);meleeCuts.removeIf(s->s.owner==p);bows.removeIf(s->s.owner==p);
        fires.removeIf(s->s.owner==p);furnaces.removeIf(s->s.owner==p);detonations.removeIf(s->s.owner==p);
        joints.values().removeIf(s->s.owner==p);rifts.removeIf(s->s.owner==p);
        for(LivingEntity target:List.copyOf(burns.keySet())){Burn burn=burns.get(target);if(burn!=null&&burn.owner.equals(p.getUUID()))SoulCombat.clearOwnedEffects(p,target);}
        cleaveMemory.remove(p.getUUID());airCuts.remove(p.getUUID());
    }
    public static void releaseCaster(ServerPlayer p){CombatExpansion.cancel(p);DismantleMotion.cancel(p);SlashForms.cancel(p);SukunaBranches.cancel(p);Uraume.cancel(p);NetBranches.cancel(p);closeDomain(p);cancelSustained(p);cooldowns.remove(p.getUUID());cleaveMemory.remove(p.getUUID());airCuts.remove(p.getUUID());}
    static void testReset(ServerPlayer p){cancelCombat(p);cooldowns.remove(p.getUUID());burns.remove(p);joints.values().removeIf(s->p.getUUID().equals(s.target));impacts.removeIf(s->p.getUUID().equals(s.target));meleeCuts.removeIf(s->p.getUUID().equals(s.target));soulCuts.removeIf(s->p.getUUID().equals(s.target));strikes.removeIf(s->p.getUUID().equals(s.target));for(Rift r:rifts)if(p.getUUID().equals(r.target))r.target=null;}
    public static void cleaveCharge(ServerPlayer p,boolean start){
        if(!Curse.canUseTechnique(p)){p.displayClientMessage(Component.literal("术式熔断中：只能使用术式反转或修复熔断"),true);return;}
        if(VoidJourney.evolved(p)){if(!start)cast(p,2);return;}
        if(!Journey.unlocked(p)||!p.isAlive()||p.isSpectator()||SoulCombat.suppressed(p))return;
        long now=p.level().getGameTime();
        if(start){if(Curse.fingers(p)<5){p.displayClientMessage(Component.literal("捌需要先吃下5根宿傩手指"),true);return;}cleaveCharges.putIfAbsent(p.getUUID(),new Charge(p.serverLevel(),now));return;}
        Charge charge=cleaveCharges.remove(p.getUUID());if(charge==null||charge.level!=p.serverLevel())return;
        long held=now-charge.start;if(held<14){cast(p,2);return;}if(held>200)return;
        long[] cd=cooldowns.computeIfAbsent(p.getUUID(),k->new long[7]);if(now<cd[2])return;
        LivingEntity aimed=aimTarget(p),target=aimed;Domain d=domains.get(p.getUUID());
        boolean guaranteed=inDomain(p,p);
        if(guaranteed&&target!=null&&!inDomain(p,target))target=null;
        if(target!=null&&!guaranteed&&!remembered(p,target))target=null; // 可空放；必中资格只限制命中。
        double fraction=SukunaBranches.cleaveFactor(p);
        float amount=(float)(Curse.rawMaximum(p)*0.12*Journey.bonus(p)*(Curse.active(p,"Disorder")?0.8:1));
        if(!Curse.spend(p,30))return;cd[2]=now+(long)Math.ceil(40*SoulCombat.cooldown(p));
        Vec3 point=target==null?aimPoint(p,50):target.getBoundingBox().getCenter();
        slashVisual(p,point,p.getLookAngle(),(float)(12*Curse.scale(p)),Curse.awakened(p)?720:0,2,false);
        if(target!=null)impacts.add(new Impact(p,p.serverLevel(),target.getUUID(),now+1,amount,Curse.awakened(p),Curse.awakened(p),SoulJourney.evolved(p),fraction));
        else if(Curse.awakened(p))rift(p,point,null,200);
        SkillSounds.cast(p,2);
    }
    private static final Map<UUID,Set<UUID>> cleaveMemory=new HashMap<>();
    private static double aimDistance(ServerPlayer p,LivingEntity e){Vec3 q=e.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();return 1-q.dot(p.getLookAngle());}
    /** 精确准星优先；未瞄中时才在可调小圆锥内选择最接近准星的可见敌人。 */
    static LivingEntity aimTarget(ServerPlayer p){
        if(p instanceof MaidBridge.Actor a&&a.aim!=null&&canHit(p,a.aim)&&p.hasLineOfSight(a.aim))return a.aim;
        Vec3 eye=p.getEyePosition(),dir=p.getLookAngle().normalize();
        LivingEntity direct=null,assisted=null;double directDistance=Double.MAX_VALUE,bestAngle=Double.MAX_VALUE,bestDistance=Double.MAX_VALUE;
        boolean assist=AimAssist.enabled(p);double angle=AimAssist.angle(p),range=AimAssist.range(p);
        for(var entity:p.serverLevel().getAllEntities())if(entity instanceof LivingEntity e&&CombatSafety.canTarget(p,e)){
            Vec3 center=e.getBoundingBox().getCenter(),delta=center.subtract(eye);double distance=delta.length();
            if(delta.dot(dir)<=0&&!e.getBoundingBox().contains(eye))continue;
            Vec3 q=e.getBoundingBox().inflate(e.getPickRadius()+0.1).clip(eye,eye.add(dir.scale(distance+e.getBbHeight()+e.getBbWidth()+2))).orElse(null);
            if(q!=null){double d=q.distanceToSqr(eye);if(d<directDistance&&visible(p,eye,q)){direct=e;directDistance=d;}}
            double dot=distance>0?delta.dot(dir)/distance:1,score=1-dot;
            if(assist&&AimRules.inside(dot,distance,angle,range)&&(score<bestAngle-1e-10||Math.abs(score-bestAngle)<=1e-10&&distance<bestDistance)&&visible(p,eye,center)){
                assisted=e;bestAngle=score;bestDistance=distance;
            }
        }
        return direct!=null?direct:assisted;
    }
    private static boolean visible(ServerPlayer p,Vec3 from,Vec3 to){
        ClipContext context=new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p);
        return net.minecraft.world.level.BlockGetter.traverseBlocks(from,to,context,(c,pos)->{
            if(!p.serverLevel().hasChunkAt(pos))return Boolean.FALSE;
            var state=p.serverLevel().getBlockState(pos);var hit=c.getBlockShape(state,p.serverLevel(),pos).clip(from,to,pos);
            return hit!=null&&hit.getLocation().distanceToSqr(from)+0.05<to.distanceToSqr(from)?Boolean.FALSE:null;
        },c->Boolean.TRUE);
    }
    static LivingEntity closestToCrosshair(ServerPlayer p,List<LivingEntity> candidates){LivingEntity t=aimTarget(p);return t!=null&&candidates.contains(t)?t:null;}
    static List<LivingEntity> crosshairOrder(ServerPlayer p,List<LivingEntity> candidates){return candidates.stream().sorted(Comparator.<LivingEntity>comparingDouble(e->aimDistance(p,e)).thenComparingDouble(e->e.distanceToSqr(p))).toList();}
    /** 「咒力附拳」的表现广播：附拳开启期间每 15 刻给全场发一条 kind=236（带 entityId 与寿命），
     *  别的玩家的客户端据此在**你的双手上**画红色咒力火焰——这样它才不是"只有自己看得见"的 HUD 特效。 */
    private static final int FIST_AURA_INTERVAL=15;
    private static void tickFistAura(){
        int sent=0;
        for(ServerPlayer p:serverPlayers()){
            if(p.level().getGameTime()%FIST_AURA_INTERVAL!=0)continue;
            if(!Curse.data(p).getBoolean("Fist"))continue;
            fx(p,FistFlameClient.FX,p.getEyePosition(),Vec3.ZERO,1,FIST_AURA_INTERVAL+20,p.getId());
            sent++;
        }
        // 验证埋点：每 15 刻（=发送节奏）最多写一行，说明服务端确实在广播
        if(sent>0)com.mojang.logging.LogUtils.getLogger().info("[附拳] 服务端广播 kind={} 给 {} 名附拳中的玩家",FistFlameClient.FX,sent);
    }
    private static List<ServerPlayer> serverPlayers(){
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        return server==null?List.of():List.copyOf(server.getPlayerList().getPlayers());
    }
    /** 咒力附拳（技能号 45）的技能号声明见 {@link #FIST_SKILL}。 */
    public static final int FIST_SKILL=45;
    public static void toggleFist(ServerPlayer p){if(!Curse.canUseTechnique(p))return;if(Curse.fingers(p)<5){p.displayClientMessage(Component.literal("咒力附拳需要先吃下5根宿傩手指"),true);return;}boolean active=Curse.data(p).getBoolean("Fist");Curse.data(p).putBoolean("Fist",!active);Curse.sync(p);p.displayClientMessage(Component.literal("咒力附拳："+(!active?"开启 · 再按一次切换关闭":"关闭")),true);}
    public static void charge(ServerPlayer p,boolean start){
        if(start)CombatExpansion.worldChant(p,TechniqueInput.intent(p));
    }
    private record Aura(ServerPlayer owner,ServerLevel level,long start,long end,double radius){}
    private static final class Domain {
        final ServerPlayer owner;final ServerLevel level;final Vec3 center,shrineOffset;final long start;long end;final double radius;
        /** 伤害倍率：结印"放下"后降到 {@link ShrineRules#DOMAIN_RELEASE_SCALE}，默认 1。 */
        float damageScale=1f;
        /** 展开那一刻已保存的点数换算出的两个倍率（{@link DomainOptions}）：领域伤害 ×{@code damageMult}、
         *  斩击节奏 ×{@code speedMult}。半径直接落在 {@link #radius} 上，不再单独存倍率。 */
        final float damageMult,speedMult;
        /** 「纵向伤敌」开关（展开那一刻的存档值）：关掉时跳过 Y 明显低于 {@link #center} 的目标——这就是关掉它
         *  多拿 1 点（{@link DomainOptions#total(boolean)}）的代价，判定见 {@link Skills#reachable}。 */
        final boolean verticalKill;
        /** 斩击节奏累积器：每刻攒 1，攒够 {@code SLASH_INTERVAL/speedMult}（九相轮转时 1/speedMult）刻结算一次，
         *  用累积而不是取整间隔，速度倍率才对每一刻都线性生效。 */
        double slashCharge;
        Domain(ServerPlayer owner,ServerLevel level,Vec3 center,Vec3 shrineOffset,long start,long end,double radius,float damageMult,float speedMult,boolean verticalKill){this.owner=owner;this.level=level;this.center=center;this.shrineOffset=shrineOffset;this.start=start;this.end=end;this.radius=radius;this.damageMult=damageMult;this.speedMult=speedMult;this.verticalKill=verticalKill;}
    }
    /** 纵向伤敌：「纵向伤敌」关掉时，跳过 Y 明显低于领域中心的目标（阈值 {@link DomainOptions#VERTICAL_MARGIN} 格，
     *  {@code d.center} 是展开那一刻玩家站的位置，也是领域的垂直基准）——省下的那 1 点就是换来的补偿。 */
    private static boolean reachable(Domain d,LivingEntity target){return d.verticalKill||target.getY()>=d.center.y-DomainOptions.VERTICAL_MARGIN;}
    /** 已经"放下"过（姿态解除、御厨子伤害 50% / 无量空处仅控制）但领域仍在的玩家：再按一次释放键即收起领域。 */
    private static final java.util.Set<UUID> released=new java.util.HashSet<>();
    /** 结印中的玩家：{@code start} 起 3 秒不能移动，{@code DOMAIN_OPEN_TICKS} 刻后领域出现。
     *  {@code type} 区分伏魔御厨子与无量空处（两者共用这一套状态机与手势/冻结机制）。 */
    private static final class Seal {
        final ServerPlayer owner;final ServerLevel level;final long start;final int type;boolean opened;long midAt;
        Seal(ServerPlayer p,int type){owner=p;level=p.serverLevel();start=level.getGameTime();this.type=type;}
    }
    /** 跳神龛：起点→上方控制点→神龛落点的二次贝塞尔，逐刻 teleport 让客户端平滑跟。 */
    private static final class Leap {
        final ServerPlayer owner;final ServerLevel level;final Vec3 from,control,to;final long start;
        Leap(ServerPlayer p,Vec3 from,Vec3 to){
            owner=p;level=p.serverLevel();this.from=from;this.to=to;start=level.getGameTime();
            control=from.add(to).scale(0.5).add(0,Math.max(4,from.distanceTo(to)*0.35),0);
        }
        Vec3 at(long now){
            double t=Math.min(1,(now-start)/(double)ShrineRules.SHRINE_LEAP_TICKS),u=1-t;
            return from.scale(u*u).add(control.scale(2*u*t)).add(to.scale(t*t));
        }
        boolean done(long now){return now-start>=ShrineRules.SHRINE_LEAP_TICKS;}
    }
    public static void extendDomain(ServerPlayer p,int ticks){Domain d=domains.get(p.getUUID());if(d!=null)d.end+=ticks;}
    public static void clashVisual(ServerPlayer p,boolean show){Domain d=domains.get(p.getUUID());if(d==null)return;if(!show){fx(p,5,d.center,Vec3.ZERO,0,0,0);return;}if(VoidJourney.evolved(p))syncVoidShrine(d);fx(p,1,d.center,d.shrineOffset,(float)d.radius,(int)Math.max(1,d.end-d.level.getGameTime()),SoulJourney.evolved(p)?-d.start:d.start);syncVoidFloor(p);}
    private record Bow(ServerPlayer owner,ServerLevel level,long release,boolean purple){Bow(ServerPlayer p,ServerLevel l,long release){this(p,l,release,false);}}
    public static void purpleBow(ServerPlayer p){if(Authority.instant(p)){LivingEntity t=aimTarget(p);detonate(p,p.serverLevel(),t==null?aimPoint(p,96):t.getBoundingBox().getCenter(),6,true);return;}int draw=Curse.tier(p)>=3?20:30;bows.add(new Bow(p,p.serverLevel(),p.level().getGameTime()+draw,true));fx(p,CombatExpansionRules.FX_PURPLE,p.getEyePosition(),p.getLookAngle(),2,draw,1);SkillSounds.cast(p,3);}
    private static class Fire {ServerPlayer owner;ServerLevel level;Vec3 pos,dir;UUID target;int age;boolean purple;Fire(ServerPlayer p){owner=p;level=p.serverLevel();pos=p.getEyePosition();LivingEntity aimed=aimTarget(p);target=aimed==null?null:aimed.getUUID();dir=aimed==null?p.getLookAngle():aimed.getBoundingBox().getCenter().subtract(pos).normalize();}}
    private record Detonation(ServerPlayer owner,ServerLevel level,Vec3 pos,long time,float radius){}
    private static void fx(ServerPlayer p,int kind,Vec3 pos,Vec3 dir,float size,int life,long seed){Network.Effect effect=new Network.Effect(kind,p.getId(),pos,dir,size,life,seed).fromCaster(p);Network.send(p.serverLevel(),effect);if(size!=SlashReach.INFINITE&&(kind==96||kind==72)&&p.position().distanceToSqr(pos)>512*512)Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),effect);}
    private static boolean valid(ServerPlayer p,ServerLevel l){return p.isAlive()&&!p.isRemoved()&&p.serverLevel()==l;}
    public static boolean sustaining(ServerPlayer p){return auras.containsKey(p.getUUID())||domains.containsKey(p.getUUID());}
    /** 灵魂剥离只能由专属请求进入；解和蓄力释放不经过此入口。 */
    public static void peel(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator()||!Journey.unlocked(p))return;
        if(!Curse.canUseTechnique(p)){p.displayClientMessage(Component.literal("术式熔断中：只能使用术式反转或修复熔断"),true);return;}
        if(!SoulJourney.evolved(p)){p.displayClientMessage(Component.literal("灵魂剥离需要先击败灵魂通道的虎杖残影"),true);return;}
        long now=p.serverLevel().getGameTime();long[] times=cooldowns.computeIfAbsent(p.getUUID(),k->new long[7]);
        LivingEntity target=aimTarget(p);
        if(now<times[6]){p.displayClientMessage(Component.literal("灵魂剥离冷却中 · "+((times[6]-now+19)/20)+" 秒"),true);return;}
        if(!Curse.spend(p,Curse.COST[6]))return;
        times[6]=now+COOLDOWN[6];if(target!=null)SoulCombat.peelTarget(p,target);fx(p,92,target==null?aimPoint(p,24):target.getBoundingBox().getCenter(),Vec3.ZERO,1,100,p.getId());SkillSounds.play(p,"soul_peel");
    }
    public static boolean cleaveActive(ServerPlayer p){return auras.containsKey(p.getUUID());}
    public static boolean available(ServerPlayer p,int skill){return skill>=0&&skill<6&&(skill!=5||MaidBridge.domainReady(p)&&(!MaidBridge.isCaster(p)||!GuardianEntity.locked(MaidBridge.body(p))&&!GuardianPlayerSkills.locked(p))&&!Curse.domainCooling(p)&&!sealing(p))&&!SoulCombat.suppressed(p)&&Curse.canUseTechnique(p)&&p.level().getGameTime()>=cooldowns.getOrDefault(p.getUUID(),new long[7])[skill]&&Curse.canSpend(p,Curse.COST[skill])&&(skill!=2||Curse.fingers(p)>=5)&&(skill!=4||Curse.awakened(p)||domainActive(p))&&(!VoidJourney.evolved(p)||VoidCombat.ready(p,skill));}
    public static boolean peelAvailable(ServerPlayer p){return SoulJourney.evolved(p)&&Curse.canSpend(p,Curse.COST[6])&&p.level().getGameTime()>=cooldowns.getOrDefault(p.getUUID(),new long[7])[6];}
    /** 动作只限制自身的时间轴，不再为普通解添加额外冷却。 */
    static boolean beginMotion(ServerPlayer p){return Curse.spend(p,Curse.COST[0]);}
    /** 按住连发每次只产生一道，费用由服务端按本次按住的累计次数结算。 */
    /** 简易领域 · 捌：在圆柱内**随机位置、随机方向**出现的斩击。
     *
     *  <p>御厨子的领域斩击是"球内随机点 + 随机三维方向飞行"，这里对齐同一套观感：
     *  方向用均匀球面采样（{@code y} 均匀、方位角均匀），**不跟随准星**，所以斩击是向四面八方飞出去的。</p> */
    static void domainSlashRandom(ServerPlayer p,Vec3 at,float size,int life){
        if(slashes.size()>=DOMAIN_CUT_LIMIT)return;
        double y=p.getRandom().nextDouble()*2-1,flat=Math.sqrt(Math.max(0,1-y*y)),a=p.getRandom().nextDouble()*Math.PI*2;
        Vec3 dir=new Vec3(Math.cos(a)*flat,y,Math.sin(a)*flat);
        slash(p,at,dir,size,p.getRandom().nextInt(360),0f,life,0);
    }
    static void heldSlash(ServerPlayer p){
        if(SlashForms.qi(p)){SlashForms.basic(p);return;}
        if(VoidJourney.evolved(p)){VoidCombat.cast(p,0);return;}
        LivingEntity target=aimTarget(p);Vec3 point=target==null?aimPoint(p,50):target.getBoundingBox().getCenter();
        strikes.add(new Strike(p,p.serverLevel(),target==null?null:target.getUUID(),point,p.getLookAngle(),p.level().getGameTime(),(float)((Curse.awakened(p)?12:7)*Curse.scale(p)),(float)(12*Curse.scale(p)),Journey.nine(p)?1440:Curse.awakened(p)?720:0));
    }
    public static void cast(ServerPlayer p,int skill){
        if(skill<0||skill>=6||!p.isAlive()||p.isSpectator()||!Journey.unlocked(p))return;
        // 技能5「伏魔御厨子」不再按下即展开：走结印时间轴（按下=结印，再按=取消/放下），见 domainPress。
        if(skill==5){domainPress(p);return;}
        // 结印期间不能释放其它术式：玩家侧由 TechniqueInput 拦，这里再兜住女仆 AI 等直连入口。
        if(sealing(p))return;
        // 简易领域接管期间（维持中或它的冷却中）不走通用冷却：这条技不属于宿傩六式那套 times[] 冷却。
        if(SimpleDomain.owns(p)||FurnaceMax.active(p))return;
        if(!Curse.canUseTechnique(p)){p.displayClientMessage(Component.literal("术式熔断中：只能使用术式反转或修复熔断"),true);return;}
        long now=p.serverLevel().getGameTime();long[] times=cooldowns.computeIfAbsent(p.getUUID(),k->new long[7]);
        if(skill==2&&Curse.fingers(p)<5){p.displayClientMessage(Component.literal("捌需要先吃下5根宿傩手指"),true);return;}
        if(skill==2&&auras.containsKey(p.getUUID())&&!VoidJourney.evolved(p)&&!MaidBridge.body(p).isShiftKeyDown()){auras.remove(p.getUUID());fx(p,9,p.position(),Vec3.ZERO,0,0,0);return;}
        if(SoulCombat.suppressed(p)){p.displayClientMessage(Component.literal("灵魂剥离或灵魂压制中，无法使用术式"),true);return;}
        if(now<times[skill]){
            SdLog.w("通用冷却拦截 skill="+skill+"("+NAMES[skill]+") 剩="+String.format(Locale.ROOT,"%.2f",(times[skill]-now)/20.0)+"秒 领域接管="+SimpleDomain.owns(p)+" 维持中="+SimpleDomain.active(p));
            p.displayClientMessage(Component.literal(NAMES[skill]+" 冷却中 · "+String.format(Locale.ROOT,"%.1f",(times[skill]-now)/20.0)+" 秒"),true);return;}
        boolean awakened=Curse.awakened(p);int tier=Curse.tier(p);double scale=Curse.scale(p);
        if(skill==4&&!awakened&&!domains.containsKey(p.getUUID())){p.displayClientMessage(Component.literal("灶需要先展开伏魔御厨子积聚粉尘"),true);return;}
        if(VoidJourney.evolved(p)&&!VoidCombat.ready(p,skill))return;
        if(!Curse.spend(p,Curse.COST[skill]))return;
        // 记下这一招的适应细分：解 / 捌 / 灵魂剥离（延迟命中的伤害靠它分类）
        AttackTypes.prepare(skill==2?AttackTypes.CLEAVE:skill==6?AttackTypes.MAGIC_SOUL:AttackTypes.DISMANTLE);
        times[skill]=now+(long)Math.ceil(COOLDOWN[skill]*SoulCombat.cooldown(p)*(Curse.active(p,"Siphon")?1.3:1)*(tier>=4?0.8:1));
        // 玩家明确选择剑气模式时必须仍能发射，不能被权限的瞬发入口抢走。
        if(Authority.instant(p)&&!(skill==0&&SlashForms.qi(p))){instantCast(p,skill);return;}
        fx(p,67,p.position(),Vec3.ZERO,Journey.nine(p)?1:0,1,0);SkillSounds.cast(p,skill);
        if(skill!=3)fx(p,70,p.position(),p.getLookAngle(),skill,skill==5?30:12,skill);
        if(CombatExpansion.afterCast(p,skill))return;
        if(skill==0&&SlashForms.qi(p)){SlashForms.basic(p);return;}
        if(VoidJourney.evolved(p)&&skill!=5){if(skill==3)fx(p,70,p.position(),p.getLookAngle(),3,60,3);VoidCombat.cast(p,skill);return;}
        Vec3 forward=p.getLookAngle(),side=new Vec3(-forward.z,0,forward.x).normalize();if(side.lengthSqr()<0.1)side=new Vec3(1,0,0);
        switch(skill){
            case 0 -> {
                LivingEntity target=aimTarget(p);Vec3 point=target==null?aimPoint(p,50):target.getBoundingBox().getCenter();double distance=point.distanceTo(p.getEyePosition());double falloff=Math.pow(0.95,SoulJourney.evolved(p)?0:Math.max(0,(int)(distance/10)));int count=TechniqueInput.holding(p,0)?1:tier>=3?3:tier>=1?2:1;
                for(int i=0;i<count;i++)strikes.add(new Strike(p,p.serverLevel(),target==null?null:target.getUUID(),point,forward,now+i*SlashMotion.INTERVAL,(float)((awakened?12:7)*scale*falloff),(float)(12*scale),Journey.nine(p)?1440:awakened?720:0));
            }
            case 1 -> {
                if(awakened){
                    fx(p,22,p.position().add(0,1,0),Vec3.ZERO,(float)(12*scale),32,now);
                    for(int i=0;i<72;i++){double y=1-2*(i+0.5)/72.0,a=i*2.39996323,r=Math.sqrt(1-y*y);Vec3 d=new Vec3(Math.cos(a)*r,y,Math.sin(a)*r);slash(p,p.getEyePosition().add(d),d,side,(float)(10*scale));}
                }else for(int i=0;i<(tier>=2?16:12);i++){
                    int count=tier>=2?16:12;double a=i*Math.PI*2/count;Vec3 d=new Vec3(Math.cos(a),0,Math.sin(a)),s=new Vec3(-d.z,0,d.x);
                    for(int j=-1;j<=1;j++){slash(p,p.position().add(0,1.5+j*2.5*scale,0).add(d.scale(1.5)),d,s,(float)(10*scale));slash(p,p.position().add(0,1.5,0).add(d.scale(1.5)).add(s.scale(j*2.5*scale)),d,new Vec3(0,1,0),(float)(10*scale));}
                }
            }
            case 2 -> {airCuts.put(p.getUUID(),0);long life=tier>=2?600:400;auras.put(p.getUUID(),new Aura(p,p.serverLevel(),now,now+life,10*scale));fx(p,8,p.position(),Vec3.ZERO,(float)(10*scale),(int)life,now);p.displayClientMessage(Component.literal(awakened?"虚空捌开启 · 伤害随剩余咒力百分比变化，满咒力150% · 金边裂缝追敌":"捌开启 · 每秒30咒力 · 再按释放可关闭"),true);}
            case 3 -> {int draw=tier>=3?20:30;bows.add(new Bow(p,p.serverLevel(),now+draw));fx(p,2,p.position(),forward,1,draw,0);}
            case 4 -> furnaces.add(new Furnace(p,p.serverLevel(),now+40));
        }
        if(skill!=0&&skill!=4)p.serverLevel().playSound(null,p.blockPosition(),skill==3?SoundEvents.BLAZE_SHOOT:SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,1.5f,0.8f);
    }
    /** 权限被动：伤害和切痕直接落在目标身上，不排结印/弓箭/咏唱队列。 */
    static void instantCast(ServerPlayer p,int skill){
        LivingEntity target=aimTarget(p);Vec3 at=target==null?aimPoint(p,96):target.getBoundingBox().getCenter();
        if(VoidJourney.evolved(p)){VoidCombat.instant(p,skill,target,at);return;}
        if(skill==3||skill==4){detonate(p,p.serverLevel(),at,skill==4?16:6,false);return;}
        if(target==null){fx(p,72,at,p.getLookAngle(),12,10,p.getRandom().nextInt(360));return;}
        List<LivingEntity> victims=skill==1?targets(p.serverLevel(),p,at,12):List.of(target);
        for(LivingEntity t:victims){float base=(float)((skill==2?(Curse.awakened(p)?YujiEntity.percent(t,t.getMaxHealth()*.1f)+20:4):Curse.awakened(p)?12:7)*Curse.power(p));scaledDamage(p.serverLevel(),p,t,base,Curse.awakened(p),true,SoulJourney.evolved(p),skill==2?SukunaBranches.cleaveFactor(p):SlashFormRules.DAMAGE);hitEffect(p.serverLevel(),p,t,7,Curse.awakened(p));}
    }
    static Vec3 aimPoint(ServerPlayer p,double range){
        Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(range));var block=p.serverLevel().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return block.getType()==HitResult.Type.MISS?end:block.getLocation();
    }
    private static long slashVisual(ServerPlayer p,Vec3 point,Vec3 dir,float size,long tint,int gesture,boolean hit){
        long angle=p.getRandom().nextInt(360);
        // 捌等其他斩击的表现；解的两段剪裁使用独立队列。
        if(SoulJourney.evolved(p))fx(p,93,point,dir,size,SlashMotion.LIFE,angle+2000);
        else fx(p,gesture==0?72:12,point,dir,size,SlashMotion.LIFE,angle+tint);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,1.2f,0.65f);
        return angle;
    }
    private static void slash(ServerPlayer p,Vec3 pos,Vec3 dir,Vec3 axis,float size){slash(p,pos,dir,size,p.getRandom().nextInt(360),1f);}
    /** 通用斩击：**角度与伤害系数都可指定**（简易领域的随机斩击直接走这一条，表现与伤害一致）。 */
    static void slash(ServerPlayer p,Vec3 pos,Vec3 dir,float size,int angleDegrees,float damageScale){slash(p,pos,dir,size,angleDegrees,damageScale,0);}
    static void slash(ServerPlayer p,Vec3 pos,Vec3 dir,float size,int angleDegrees,float damageScale,int life){slash(p,pos,dir,size,angleDegrees,damageScale,life,SLASH_SPEED);}
    static void slash(ServerPlayer p,Vec3 pos,Vec3 dir,float size,int angleDegrees,float damageScale,int life,double speed){
        if(slashes.size()>=(damageScale<1f?DOMAIN_CUT_LIMIT:400))return; // 领域静止斩击用大池子，飞行斩击维持 400
        Vec3 side=new Vec3(-dir.z,0,dir.x).normalize();if(side.lengthSqr()<0.1)side=new Vec3(1,0,0);
        Vec3 up=dir.cross(side).normalize();double a=Math.toRadians(angleDegrees);
        Vec3 axis=side.scale(Math.cos(a)).add(up.scale(Math.sin(a))).normalize();
        Slash s=new Slash(p,pos,dir,axis,size);s.damageScale=damageScale;
        if(life>0)s.life=life;   // 领域用的随机斩击寿命单独指定（默认 400 刻会瞬间堆满上限）
        s.speed=speed;
        // 【关键】speed<=0 = 静止斩痕：必须用专门给"停在原地的斩击"准备的 STILL 特效（162），
        // 不能用普通斩击特效 0 —— 客户端会自己按 kind 判定位移（Client.position 对 kind 0 每刻推 2 格），
        // 那就是"明明是斩击却看起来像剑气飞出去"的真正原因。STILL 在客户端是零位移的静止斩痕。
        boolean still=speed<=0;
        slashes.add(s);
        if(still){
            // r155 的表现通路：静止斩痕用 SlashFormRules.STILL(162)，长度同时写进 direction 与 size 两处。
            Vec3 unit=dir.lengthSqr()<1e-8?new Vec3(0,0,1):dir.normalize();
            Network.send(p.serverLevel(),new Network.Effect(SlashFormRules.STILL,p.getId(),pos,unit.scale(Math.max(0.5,size)),size,s.life,0));
        }else s.fragments=List.of(new SlashForms.Line(pos.subtract(axis.scale(size/2)),pos.add(axis.scale(size/2))));
    }
    private static boolean tickFlyingSlash(Slash s){
        if(s.age==1)s.fragments=SlashTerrain.launch(s.owner,s.fragments,s.dir);
        if(s.fragments.isEmpty())return false;
        Vec3 step=s.dir.scale(s.speed);var result=SlashTerrain.advance(s.owner,s.fragments,step);
        Set<UUID> thisTick=new HashSet<>();
        for(var sweep:result.swept()){
            for(int i=0;i<=8;i++){Vec3 move=sweep.step().scale(i/8.0);if(CombatExpansion.collideSlash(s.owner,sweep.a().add(move),sweep.b().add(move),s.dir)){SlashTerrain.send(s.owner,s.visualId,List.of(),s.pos);return false;}}
            for(LivingEntity target:s.level.getEntitiesOfClass(LivingEntity.class,new AABB(sweep.a(),sweep.b()).expandTowards(sweep.step()).inflate(.01),e->canHit(s.owner,e)))
                if(!thisTick.contains(target.getUUID())&&(Journey.rotation(s.owner)||!s.hit.contains(target.getUUID()))&&SlashTerrain.hits(sweep,target)){
                    thisTick.add(target.getUUID());s.hit.add(target.getUUID());
                    float amount=(s.world?12:6)*s.power*s.damageScale;boolean soul=SoulJourney.evolved(s.owner);
                    if(scaledDamage(s.level,s.owner,target,amount,s.world,true,soul,SlashFormRules.DAMAGE)){
                        if(soul)hitEffectSoul(s.level,s.owner,target);else hitEffect(s.level,s.owner,target,7,s.world);
                    }
                }
        }
        SlashTerrain.send(s.owner,s.visualId,result.swept(),s.pos);s.fragments=result.remaining();s.pos=s.pos.add(step);
        return !s.fragments.isEmpty();
    }
    private static void hitEffect(ServerLevel l,ServerPlayer caster,LivingEntity target,int kind,boolean red){Vec3 center=target.getBoundingBox().getCenter();float size=kind==10?Math.max(1.5f,Math.min(6,target.getBbHeight()*1.5f)):Math.max(4,target.getBbHeight()*3);Network.send(l,new Network.Effect(red?11:kind,target.getId(),center,center.subtract(target.position()),size,kind==10?6:10,target.getRandom().nextLong()).fromCaster(caster));}
    private static void hitEffectSoul(ServerLevel l,ServerPlayer caster,LivingEntity target){if(!target.isAlive())return;Vec3 center=target.getBoundingBox().getCenter();Network.send(l,new Network.Effect(93,target.getId(),center,Vec3.ZERO,Math.max(1,target.getBbHeight()),12,target.getRandom().nextLong()).fromCaster(caster));}
    private static double projection(LivingEntity e,Vec3 axis){return (Math.abs(axis.x)+Math.abs(axis.z))*e.getBbWidth()/2+Math.abs(axis.y)*e.getBbHeight()/2;}
    private static boolean canHit(ServerPlayer p,LivingEntity e){return CombatSettings.canHit(p,e);}
    private static boolean damage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world,boolean slash,boolean soulSlash,boolean domain){if(DomainClash.active(p)||DomainClash.active(e)||!canHit(p,e))return false;soulSlash=soulSlash||SoulJourney.evolved(p)&&slash;AttackTypes.markPendingDefault(soulSlash?AttackTypes.SOUL:(world||slash||domain?AttackTypes.SLASH:AttackTypes.MAGIC));if(soulSlash){n+=Curse.soulSlashBonus(p);e.getPersistentData().putUUID("SukunaSoulSlashOwner",p.getUUID());e.getPersistentData().putBoolean("SukunaNoHeal",true);}n=domain?CombatSettings.domainSpellDamage(p,n):CombatSettings.spellDamage(p,n);if(domain&&slash)n*=(float)SlashFormRules.DAMAGE;float before=e.getHealth()+e.getAbsorptionAmount();e.invulnerableTime=0;boolean accepted=CombatSettings.hurt(p,e,world?l.damageSources().sonicBoom(p):l.damageSources().indirectMagic(p,p),n);if(accepted&&e instanceof BuddhaEntity boss&&world)boss.interruptSiphon();boolean hurt=accepted&&e.getHealth()+e.getAbsorptionAmount()<before;if(hurt&&slash&&Journey.nine(p)&&e.isAlive())joint(p,e);if(hurt&&soulSlash)SoulCombat.soulHit(p,e);return hurt;}
    static List<LivingEntity> targets(ServerLevel l,ServerPlayer p,Vec3 c,double r){if(r<=128)return l.getEntitiesOfClass(LivingEntity.class,new AABB(c,c).inflate(r),e->canHit(p,e));List<LivingEntity> found=new ArrayList<>();for(var entity:l.getAllEntities())if(entity instanceof LivingEntity e&&canHit(p,e)&&e.position().distanceToSqr(c)<=r*r)found.add(e);return found;}
    private static boolean damage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world){return damage(l,p,e,n,world,true,false);}
    private static boolean damage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world,boolean slash){return damage(l,p,e,n,world,slash,false);}
    private static boolean damage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world,boolean slash,boolean soulSlash){return scaledDamage(l,p,e,n,world,slash,soulSlash,1);}
    private static boolean scaledDamage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world,boolean slash,boolean soulSlash,double fraction){if(DomainClash.active(p)||DomainClash.active(e)||!canHit(p,e))return false;soulSlash=soulSlash||SoulJourney.evolved(p)&&slash;AttackTypes.markPendingDefault(soulSlash?AttackTypes.SOUL:(world||slash?AttackTypes.SLASH:AttackTypes.MAGIC));if(soulSlash){n+=Curse.soulSlashBonus(p);e.getPersistentData().putUUID("SukunaSoulSlashOwner",p.getUUID());e.getPersistentData().putBoolean("SukunaNoHeal",true);}n=(float)(CombatSettings.spellDamage(p,n)*fraction);float before=e.getHealth()+e.getAbsorptionAmount();e.invulnerableTime=0;boolean accepted=CombatSettings.hurt(p,e,world?l.damageSources().sonicBoom(p):l.damageSources().indirectMagic(p,p),n);if(accepted&&e instanceof BuddhaEntity boss&&world)boss.interruptSiphon();boolean hurt=accepted&&e.getHealth()+e.getAbsorptionAmount()<before;if(hurt&&slash&&Journey.nine(p)&&e.isAlive())joint(p,e);if(hurt&&soulSlash)SoulCombat.soulHit(p,e);return hurt;}
    private static boolean hitDamage(ServerLevel l,ServerPlayer p,LivingEntity e,float n,boolean world){return damage(l,p,e,n*(float)Curse.power(p),world);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        tickFistAura();
        VoidCombat.tick();
        tickSeals();
        tickLeaps();
        for(Iterator<MeleeCut> it=meleeCuts.iterator();it.hasNext();){
            MeleeCut cut=it.next();if(!valid(cut.owner,cut.level)){it.remove();continue;}if(cut.level.getGameTime()<cut.due)continue;it.remove();
            if(cut.level.getEntity(cut.target) instanceof LivingEntity target&&canHit(cut.owner,target)){
                fx(cut.owner,72,target.getBoundingBox().getCenter(),cut.owner.getLookAngle(),Math.max(4,target.getBbHeight()*2),SlashMotion.LIFE,cut.owner.getRandom().nextInt(360));
                target.invulnerableTime=0;AttackTypes.mark(AttackTypes.DISMANTLE);CombatSettings.hurt(cut.owner,target,cut.level.damageSources().sonicBoom(cut.owner),CombatSettings.casterDamage(cut.owner,cut.amount)*(float)SlashFormRules.DAMAGE);
            }
        }
        burns.entrySet().removeIf(entry->entry.getKey().isRemoved()||!entry.getKey().isAlive()||!entry.getKey().isOnFire()||entry.getKey().level().getGameTime()>entry.getValue().until);
        charges.entrySet().removeIf(entry->{Charge c=entry.getValue();ServerPlayer p=c.level.getServer().getPlayerList().getPlayer(entry.getKey());if(p!=null&&valid(p,c.level)&&c.level.getGameTime()-c.start==CHARGE_DELAY)SkillSounds.play(p,"space_charge");return p==null||!valid(p,c.level)||c.level.getGameTime()-c.start>200;});
        for(Iterator<Strike> it=strikes.iterator();it.hasNext();){
            Strike s=it.next();if(!valid(s.owner,s.level)){it.remove();continue;}if(s.level.getGameTime()<s.due)continue;it.remove();
            LivingEntity target=s.target!=null&&s.level.getEntity(s.target) instanceof LivingEntity e&&canHit(s.owner,e)?e:null;
            if(s.target!=null&&target==null)continue;
            Vec3 point=target==null?s.point:target.getBoundingBox().getCenter();
            if(target!=null){
                Vec3 toward=s.owner.getEyePosition().subtract(point).normalize();
                point=target.getBoundingBox().inflate(0.08).clip(s.owner.getEyePosition(),point).orElse(point).add(toward.scale(0.05));
            }else point=point.subtract(s.direction.scale(0.08));
            Vec3 collisionAxis=NetBranches.side(s.direction).scale(s.size/2);if(CombatExpansion.collideSlash(s.owner,point.subtract(collisionAxis),point.add(collisionAxis),s.direction))continue;
            if(SoulJourney.evolved(s.owner))soulDismantle(s.owner,target,point,s.direction,SlashReach.INFINITE,s.amount,s.tint>=720);
            else {slashVisual(s.owner,point,s.direction,s.size,s.tint,0,true);
                if(target!=null)impacts.add(new Impact(s.owner,s.level,s.target,s.level.getGameTime()+1,s.amount,s.tint>=720,false,false,SlashFormRules.DAMAGE));}
        }
        for(Iterator<SoulCut> it=soulCuts.iterator();it.hasNext();){
            SoulCut s=it.next();if(!valid(s.owner,s.level)){it.remove();continue;}if(s.level.getGameTime()<s.due)continue;it.remove();
            if(s.size!=0){fx(s.owner,72,s.point,s.direction,s.size,SlashMotion.LIFE,s.angle);
            s.level.playSound(null,s.point.x,s.point.y,s.point.z,SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,1.2f,0.65f);}
            if(s.target!=null&&s.level.getEntity(s.target) instanceof LivingEntity target&&canHit(s.owner,target))scaledDamage(s.level,s.owner,target,s.amount,s.world,false,true,SlashFormRules.DAMAGE);
        }
        for(Iterator<Impact> it=impacts.iterator();it.hasNext();){Impact i=it.next();if(!valid(i.owner,i.level)){it.remove();continue;}if(i.level.getGameTime()<i.due)continue;it.remove();var entity=i.level.getEntity(i.target);if(entity instanceof LivingEntity target&&canHit(i.owner,target)){Vec3 point=target.getBoundingBox().getCenter();scaledDamage(i.level,i.owner,target,i.amount,i.world,false,i.soulSlash,i.fraction);if(i.soulSlash)hitEffectSoul(i.level,i.owner,target);if(i.rift)rift(i.owner,point,target.isAlive()?target:null,200);}}
        for(Iterator<Furnace> it=furnaces.iterator();it.hasNext();){
            Furnace f=it.next();if(!valid(f.owner,f.level)){it.remove();continue;}if(f.level.getGameTime()<f.due)continue;it.remove();ServerPlayer p=f.owner;
            boolean domain=domains.containsKey(p.getUUID());if(!domain&&!Curse.awakened(p))continue;double radius=domain?200:50;
            for(LivingEntity target:targets(f.level,p,p.position(),radius))if(target.distanceToSqr(p)<=radius*radius){AttackTypes.mark(AttackTypes.FIRE);damage(f.level,p,target,16*(float)Curse.power(p)*(domain?1.5f:1)*MahoragaRules.bossDamage(target,MahoragaRules.FURNACE_MULTIPLIER),false,false,false,domain);ignite(p,target,12);fx(p,4,target.getBoundingBox().getCenter(),Vec3.ZERO,4,30,f.due);}
            fx(p,23,p.position(),Vec3.ZERO,(float)radius,45,f.due);
        }
        for(Iterator<Joint> it=joints.values().iterator();it.hasNext();){Joint j=it.next();if(!valid(j.owner,j.level)){it.remove();continue;}if(j.level.getGameTime()<j.due)continue;it.remove();var entity=j.level.getEntity(j.target);if(entity instanceof LivingEntity target&&canHit(j.owner,target)){damage(j.level,j.owner,target,j.power,true,false);fx(j.owner,59,target.getBoundingBox().getCenter(),Vec3.ZERO,4,20,target.getId());SkillSounds.play(j.owner,"fetus_joint");}}
        tickRifts();
        for(Iterator<Slash> it=slashes.iterator();it.hasNext();){Slash s=it.next();if(++s.age>s.life||!valid(s.owner,s.level)){it.remove();continue;}if(s.fragments!=null){if(!tickFlyingSlash(s))it.remove();continue;}Vec3 old=s.pos;s.pos=s.pos.add(s.dir.scale(s.speed));if(CombatExpansion.collideSlash(s.owner,s.pos.subtract(s.axis.scale(s.size/2)),s.pos.add(s.axis.scale(s.size/2)),s.dir)){it.remove();continue;}SkillDestruction.line(s.owner,s.pos.subtract(s.axis.scale(s.size/2)),s.pos.add(s.axis.scale(s.size/2)),0.6);AABB swept=new AABB(old,s.pos).inflate(s.size/2+1);
            for(LivingEntity e:targets(s.level,s.owner,s.pos,s.size/2+3)){if(!e.getBoundingBox().intersects(swept))continue;Vec3 q=e.getBoundingBox().getCenter().subtract(old.lerp(s.pos,0.5));double a=q.dot(s.axis),b=q.dot(s.dir);Vec3 normal=s.axis.cross(s.dir).normalize();double halfThickness=0.45;float amount=(s.space?(float)((YujiEntity.percent(e,e.getMaxHealth()*0.25f)+300)*Journey.bonus(s.owner)*(Curse.active(s.owner,"Disorder")?0.8:1)):(s.world?12:6)*s.power)*s.damageScale;boolean soul=SoulJourney.evolved(s.owner);if(Math.abs(a)<s.size/2+projection(e,s.axis)&&Math.abs(b)<1+projection(e,s.dir)&&Math.abs(q.dot(normal))<halfThickness+projection(e,normal)&&(Journey.rotation(s.owner)||s.hit.add(e.getUUID()))&&scaledDamage(s.level,s.owner,e,amount,s.world,true,soul,SlashFormRules.DAMAGE)){if(soul)hitEffectSoul(s.level,s.owner,e);else hitEffect(s.level,s.owner,e,7,s.world);}}
        }
        for(Iterator<Aura> it=auras.values().iterator();it.hasNext();){Aura a=it.next();long now=a.level.getGameTime(),age=now-a.start;ServerPlayer p=a.owner;
            if(!valid(p,a.level)||!Curse.canUseTechnique(p)||now>=a.end||(age>0&&age%20==0&&!Curse.spend(p,30))){Network.send(a.level,new Network.Effect(9,p.getId(),p.position(),Vec3.ZERO,0,0,0));it.remove();continue;}
            if(age%20==0){fx(p,67,p.position(),Vec3.ZERO,Journey.nine(p)?1:0,1,0);fx(p,8,p.position(),Vec3.ZERO,(float)a.radius,(int)(a.end-now),a.start);}
            boolean awake=Curse.awakened(p);
            if(awake&&age>0&&age%10==0&&airCuts.getOrDefault(p.getUUID(),0)<20){airCuts.merge(p.getUUID(),1,Integer::sum);double angle=p.getRandom().nextDouble()*Math.PI*2,radius=Math.sqrt(p.getRandom().nextDouble())*a.radius;Vec3 point=p.position().add(Math.cos(angle)*radius,0.5+p.getRandom().nextDouble()*5,Math.sin(angle)*radius);rift(p,point,null,a.radius);}
            if(age>0&&age%(Journey.rotation(p)?1:awake?100:10)==0)for(LivingEntity target:targets(a.level,p,p.position(),a.radius))if(target.distanceToSqr(p)<=a.radius*a.radius){boolean hit=awake?scaledDamage(a.level,p,target,(float)((YujiEntity.percent(target,target.getMaxHealth()*0.1f)+20)*Journey.bonus(p)*(Curse.active(p,"Disorder")?0.8:1)),true,true,false,SukunaBranches.cleaveFactor(p)):scaledDamage(a.level,p,target,(float)(4*Curse.power(p)),false,true,false,SukunaBranches.cleaveFactor(p));if(hit){if(SoulJourney.evolved(p))hitEffectSoul(a.level,p,target);else hitEffect(a.level,p,target,7,awake);if(awake&&age%10==0)rift(p,target.getBoundingBox().getCenter().add(0,1,0),target,a.radius);}}
        }
        for(Iterator<Domain> it=domains.values().iterator();it.hasNext();){Domain d=it.next();long now=d.level.getGameTime(),age=now-d.start;ServerPlayer p=d.owner;
            if(DomainClash.active(p))continue;
            if(!valid(p,d.level)||now>=d.end||(age>0&&age%20==0&&!Curse.spend(p,70))){VoidCombat.closeDomain(p,valid(p,d.level)&&!clashing(d));Network.send(d.level,new Network.Effect(5,p.getId(),d.center,Vec3.ZERO,0,0,0));it.remove();endSeal(p);Curse.cooldownDomain(p);continue;}
            boolean clash=clashing(d);
            // 斩击间隔：速度点数把节奏线性加快（累积器，不取整）。展开的那一 tick 先打一次（与原来 age%2==0 在 age=0
            // 命中一致），之后每攒够 SLASH_INTERVAL/speedMult 刻结算一次；九相轮转的"每刻一次"同样按倍率提速。
            // ×1.00 时正好回到原来的每 2 刻一次；虚空/普通两条分支共用这一次节奏判定。
            double interval=(Journey.rotation(p)?1.0:DomainOptions.SLASH_INTERVAL)/d.speedMult;
            boolean beat;
            if(age<=0){d.slashCharge=0;beat=true;}
            else {d.slashCharge++;beat=d.slashCharge>=interval;if(beat)d.slashCharge-=interval;}
            if(VoidJourney.evolved(p)){
                if(!clash&&beat)for(LivingEntity target:targets(d.level,p,d.center,d.radius))if(reachable(d,target))VoidCombat.domainHit(p,target,d.damageScale*d.damageMult);
                if(age%40==0){syncVoidShrine(d);fx(p,1,d.center,d.shrineOffset,(float)d.radius,(int)(d.end-now),-d.start);}
                continue;
            }
            if(!clash&&beat)for(LivingEntity target:targets(d.level,p,d.center,d.radius))if(reachable(d,target))for(int cut=0;cut<5&&target.isAlive();cut++)if(target.position().distanceToSqr(d.center)<=d.radius*d.radius&&damage(d.level,p,target,(Curse.tier(p)>=5?1.25f:1)*(float)Curse.power(p)*d.damageScale*d.damageMult,SoulJourney.evolved(p),true,SoulJourney.evolved(p),true)){boolean red=Curse.awakened(p);if(red)ignite(p,target,4);if(SoulJourney.evolved(p))hitEffectSoul(d.level,p,target);else hitEffect(d.level,p,target,10,red);}
            if(age%40==0){if(VoidJourney.evolved(p))syncVoidShrine(d);fx(p,67,p.position(),Vec3.ZERO,Journey.nine(p)?1:0,1,0);Network.send(d.level,new Network.Effect(1,p.getId(),d.center,d.shrineOffset,(float)d.radius,(int)(d.end-now),SoulJourney.evolved(p)?-d.start:d.start));fx(p,25,p.position(),Vec3.ZERO,CombatSettings.voidFloor(p)?1:0,2,0);}
            if(!clash&&Curse.awakened(p)&&age%20==0){
                fx(p,24,d.center,d.shrineOffset,(float)d.radius,40,now);
                // 领域粉尘自动燃烧；逐个目标判断敌友，避免环境火误伤盟友。
                for(LivingEntity target:targets(d.level,p,d.center,d.radius))if(reachable(d,target)&&target.position().distanceToSqr(d.center)<=d.radius*d.radius){ignite(p,target,3);damage(d.level,p,target,(float)(2*Curse.power(p))*d.damageScale*d.damageMult,false,false,false,true);}
            }
        }
        for(Iterator<Bow> it=bows.iterator();it.hasNext();){Bow b=it.next();if(!valid(b.owner,b.level)){it.remove();continue;}if(b.level.getGameTime()>=b.release){Fire f=new Fire(b.owner);f.purple=b.purple;fires.add(f);if(!f.purple)fx(b.owner,3,f.pos,f.dir,2,100,0);it.remove();}}
        for(Iterator<Fire> it=fires.iterator();it.hasNext();){Fire f=it.next();if(!valid(f.owner,f.level)||++f.age>100){it.remove();continue;}Vec3 next=f.pos.add(f.dir.scale(3));if(f.purple)fx(f.owner,CombatExpansionRules.FX_PURPLE,next,f.dir,2,3,0);var block=f.level.clip(new ClipContext(f.pos,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,f.owner));boolean hit=block.getType()!=HitResult.Type.MISS;for(LivingEntity target:targets(f.level,f.owner,f.pos,5))if(target.getBoundingBox().inflate(1).clip(f.pos,next).isPresent()){hit=true;next=target.position();break;}if(hit){if(block.getType()!=HitResult.Type.MISS)next=block.getLocation();detonate(f.owner,f.level,next,(float)(12*Curse.scale(f.owner)*(Curse.awakened(f.owner)?1.5:1)),f.purple);if(Curse.awakened(f.owner)&&!f.purple)fx(f.owner,23,next,Vec3.ZERO,32,60,0);it.remove();}else f.pos=next;}
        for(Iterator<Detonation> it=detonations.iterator();it.hasNext();){Detonation d=it.next();if(!valid(d.owner,d.level)){it.remove();continue;}if(d.level.getGameTime()>=d.time){detonate(d.owner,d.level,d.pos,d.radius);it.remove();}}
    }
    private static void tickRifts(){
        for(Iterator<Rift> it=rifts.iterator();it.hasNext();){Rift r=it.next();r.age++;
            if(!valid(r.owner,r.level)||r.age>140){Network.send(r.level,new Network.Effect(63,r.owner.getId(),r.pos,Vec3.ZERO,0,1,r.id));it.remove();continue;}
            if(r.age<12)continue;
            LivingEntity target=r.target!=null&&r.level.getEntity(r.target) instanceof LivingEntity e&&canHit(r.owner,e)?e:null;
            if(target==null&&r.age%4==0){target=targets(r.level,r.owner,r.owner.position(),r.radius).stream().filter(e->e.position().distanceToSqr(r.owner.position())<=r.radius*r.radius).min(Comparator.<LivingEntity>comparingDouble(e->aimDistance(r.owner,e)).thenComparingDouble(e->e.distanceToSqr(r.owner))).orElse(null);r.target=target==null?null:target.getUUID();if(target!=null)r.dir=target.getBoundingBox().getCenter().subtract(r.pos).normalize();}
            if(target==null)continue;
            Vec3 aim=target.getBoundingBox().getCenter().subtract(r.pos).normalize();
            if(r.age==12||r.age%5==0||target.getBoundingBox().getCenter().distanceToSqr(r.pos)<36){Vec3 side=new Vec3(-aim.z,0,aim.x).normalize();double zig=r.age<37&&target.getBoundingBox().getCenter().distanceToSqr(r.pos)>=36?((r.age/5)%2==0?1.8:-1.8):0;r.dir=aim.add(side.scale(zig)).normalize();}
            Vec3 next=r.pos.add(r.dir.scale(2.4));boolean hit=false;
            for(LivingEntity victim:targets(r.level,r.owner,r.pos,4))if(victim.getBoundingBox().inflate(0.25).clip(r.pos,next).isPresent()||victim.getBoundingBox().inflate(0.25).contains(r.pos)){AttackTypes.mark(AttackTypes.VOID);damage(r.level,r.owner,victim,8*r.power,true,false);fx(r.owner,64,victim.getBoundingBox().getCenter(),r.dir,3,18,r.id);hit=true;break;}
            if(hit){Network.send(r.level,new Network.Effect(63,r.owner.getId(),r.pos,Vec3.ZERO,0,1,r.id));it.remove();continue;}
            r.pos=next;Network.send(r.level,new Network.Effect(62,r.owner.getId(),r.pos,r.dir,1,2,r.id));
        }
    }
    private static void detonate(ServerPlayer p,ServerLevel l,Vec3 pos,float radius){detonate(p,l,pos,radius,false);}
    private static void detonate(ServerPlayer p,ServerLevel l,Vec3 pos,float radius,boolean purple){AttackTypes.mark(AttackTypes.FIRE);Network.send(l,new Network.Effect(purple?CombatExpansionRules.FX_PURPLE:4,p.getId(),pos,Vec3.ZERO,radius,30,purple?2:l.getGameTime()));if(purple)SkillDestruction.impact(p,pos,4);l.playSound(null,pos.x,pos.y,pos.z,SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,3,0.7f);for(LivingEntity e:targets(l,p,pos,radius))if(e.position().distanceToSqr(pos)<=radius*radius){damage(l,p,e,16*(float)Curse.power(p)*MahoragaRules.bossDamage(e,MahoragaRules.FURNACE_MULTIPLIER),false,false);if(purple){Curse.permanentSoulFire(p,e);fx(p,CombatExpansionRules.FX_PURPLE,e.getBoundingBox().getCenter(),new Vec3(0,1,0),1,20,0);}else ignite(p,e,8);Vec3 push=e.position().subtract(pos).normalize().scale(1.5);e.push(push.x,0.4,push.z);}}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){slashes.clear();auras.clear();domains.clear();bows.clear();fires.clear();detonations.clear();cooldowns.clear();charges.clear();cleaveCharges.clear();cleaveMemory.clear();impacts.clear();meleeCuts.clear();strikes.clear();soulCuts.clear();furnaces.clear();joints.clear();rifts.clear();airCuts.clear();burns.clear();seals.clear();leaps.clear();nextRift=0;}
}
