package character.culling;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 死灭洄游规则卡池与结算（r218）。
 *
 * <p>每 100 积分可以「制定」一条规则，最多 5 条；面板由 {@link CullingRuleScreen} 显示 5 个候选。
 * 规则按玩家存（谁的积分谁制定），界内怪物侧的规则（自爆、分裂、精英化）对所有人可见。
 * 规则状态存在 {@code ExpeditionProgress} 的玩家数据里，掉线重连不丢；离开结界时只清掉临时属性。
 */
public final class CullingRules {
    /** 规则面板：99=请求打开；0~4=选第 n 个候选；5=脱离。走 TechniqueInput 的 phase=7。 */
    public static final int SKILL = 3200, OPEN = 99, ESCAPE = 5, COST = 100, MAX = 5;
    private static final String COUNT = "CullingRuleCount", OPTION = "CullingRuleOption", OPTIONS = "CullingRuleOptions", RUN = "CullingRunTicks";
    private static final UUID HEALTH = UUID.fromString("91f0c8f1-0000-4a11-9c00-00000000c001");
    private static final UUID ATTACK = UUID.fromString("91f0c8f1-0000-4a11-9c00-00000000c003");
    private static final UUID SPEED = UUID.fromString("91f0c8f1-0000-4a11-9c00-00000000c004");
    private static final UUID ATK_SPEED = UUID.fromString("91f0c8f1-0000-4a11-9c00-00000000c005");
    private static final UUID JUMP = UUID.fromString("91f0c8f1-0000-4a11-9c00-00000000c006");
    private static final UUID STOLEN = UUID.fromString("91f0c8f1-0000-4a11-9c00-00000000c007");
    private static final UUID ARMOR = UUID.fromString("91f0c8f1-0000-4a11-9c00-00000000c008");

    /** 一条规则的数值修正；没写到的字段就是 1（无变化）。 */
    public static final class Mods {
        double energy = 1, damage = 1, taken = 1, health = 1, speed = 1, atkSpeed = 1;
        double cooldown = 1, heal = 1, spell = 1, melee = 1, score = 1;
        double flashChance = 1, flashPower = 0;
        int jump = 0;
        boolean noEnergyRegen, noRegen, domainBanned, noFly, armorOff, noHealthBar, killFullHeal;
        final Set<String> special = new LinkedHashSet<>();
        Mods energy(double v){energy=v;return this;}
        Mods damage(double v){damage=v;return this;}
        Mods taken(double v){taken=v;return this;}
        Mods health(double v){health=v;return this;}
        Mods speed(double v){speed=v;return this;}
        Mods atkSpeed(double v){atkSpeed=v;return this;}
        Mods cooldown(double v){cooldown=v;return this;}
        Mods heal(double v){heal=v;return this;}
        Mods spell(double v){spell=v;return this;}
        Mods melee(double v){melee=v;return this;}
        Mods score(double v){score=v;return this;}
        Mods flash(double chance,double power){flashChance=chance;flashPower=power;return this;}
        Mods jump(int v){jump=v;return this;}
        Mods noEnergyRegen(){noEnergyRegen=true;return this;}
        Mods noRegen(){noRegen=true;return this;}
        Mods killFullHeal(){killFullHeal=true;return this;}
        Mods domainBanned(){domainBanned=true;return this;}
        Mods noFly(){noFly=true;return this;}
        Mods armorOff(){armorOff=true;return this;}
        Mods noHealthBar(){noHealthBar=true;return this;}
        Mods special(String... ids){special.addAll(Arrays.asList(ids));return this;}
    }
    private static Mods mods(){return new Mods();}
    public record Rule(String id,String name,String desc,String group,Mods mods) {}
    private static Rule r(String id,String name,String desc,String group,Mods m){return new Rule(id,name,desc,group,m);}

    /** 卡池：与用户给的清单一一对应（同名的两个「背水一战」用不同 id 保留）。 */
    public static final List<Rule> ALL = List.of(
        // 一、咒力与强化系
        r("mana_flow","咒力奔流","咒力上限翻倍，技能冷却延长 20%","咒力强化",mods().energy(2).cooldown(1.2)),
        r("minimal","极简主义","平A 伤害 +100%，术式伤害 -50%","咒力强化",mods().melee(2).spell(.5)),
        r("flesh_weak","血肉苦弱","术式伤害 +50%，最大生命 -30%","咒力强化",mods().spell(1.5).health(.7)),
        r("flash_fever","黑闪狂热","黑闪概率翻倍，伤害指数 3 → 3.5","咒力强化",mods().flash(2,.5)),
        r("art_open","术式公开","敌人伤害 +30%，出手前摇变长","咒力强化",mods().taken(1.3)),
        r("mana_dry","咒力枯竭","无法自然恢复咒力，只能靠击杀或药水回蓝","咒力强化",mods().noEnergyRegen()),
        r("overload","过载运转","连续施术叠加「过热」：伤害升高但自己持续掉血","咒力强化",mods().special("overload")),
        r("reverse_ban","反转剥夺","所有治疗效果减半","咒力强化",mods().heal(.5)),
        // 二、环境与地形系
        r("lava_rain","天降火雨","露天时随机被岩浆雨点燃，必须找掩体","环境",mods().special("lava_rain")),
        r("no_light","无光之夜","结界变暗，怪物索敌范围翻倍","环境",mods().special("no_light")),
        r("gravity_mess","重力紊乱","跳跃高度 ×3，摔落伤害翻倍","环境",mods().jump(3).special("fall_double")),
        r("blood_swamp","血液沼泽","地面随机生成赤血沼泽，减速并持续扣血","环境",mods().special("blood_swamp")),
        r("live_maze","活体迷宫","废墟变活体血肉，靠近会被挤压","环境",mods().special("live_maze")),
        r("forbidden_zone","绝对禁区","结界中心生成黑色立方体，靠近持续扣咒力","环境",mods().special("forbidden_zone")),
        // 三、敌人与狂潮系
        r("ancient_awaken","古代术师觉醒","术师刷新概率 ×3，积分掉落翻倍","狂潮",mods().score(2).special("ancient")),
        r("twin_mirror","双生镜像","每只特级咒灵分裂出一个半血残影","狂潮",mods().special("twin_mirror")),
        r("bomb_wave","自爆浪潮","非 Boss 敌人死亡时剧烈爆炸","狂潮",mods().special("bomb")),
        r("curse_overflow","咒灵操术·失控","无规则刷出大量特级咒灵（无掉落）","狂潮",mods().special("overflow")),
        r("assimilation","同化危机","受击叠加「受肉同化」，满 10 层爆体","狂潮",mods().taken(1.2).special("assimilation")),
        r("elite_hunt","精英猎杀","只刷咒胎戴天级精英，不再刷杂兵","狂潮",mods().special("elite")),
        // 四、限制与狂赌系
        r("no_domain","禁魔结界","禁止领域展开，基础属性 +50%","狂赌",mods().domainBanned().health(1.5).damage(1.5).speed(1.5)),
        r("natural_select","物竞天择","每次受击永久偷取敌人 1 点攻击力","狂赌",mods().special("steal_attack")),
        r("desperado","亡命之徒","血越低伤害越高（最高 ×2），没有血条提示","狂赌",mods().noHealthBar().special("desperado")),
        r("final_sprint","末路狂飙","所有生物移速翻倍","狂赌",mods().speed(2)),
        r("sense_deprive","剥夺感官","敌人视野被迷雾笼罩，你的屏幕变暗","狂赌",mods().special("fog")),
        r("fast_only","唯快不破","攻速 +100%，每次攻击消耗饱食度","狂赌",mods().atkSpeed(2).special("hunger_hit")),
        r("gravity_all","万有引力","所有飞行生物被拉向地面","狂赌",mods().noFly()),
        r("unjust_game","不义游戏","每 30 秒被强制随机交换位置","狂赌",mods().special("swap30")),
        r("last_stand","背水一战","结界内无法退出，死亡即清空积分","狂赌",mods().special("no_exit")),
        r("final_clear","最终清算","界内只剩你时获得「天元庇护」：全属性 +200%","狂赌",mods().special("final_clear")),
        r("blood_feast","血怒狂宴","所有生物造成与受到的伤害都 +100%","狂赌",mods().damage(2).taken(2)),
        r("burn_boat","破釜沉舟","无法自然回血，但击杀立刻回满","狂赌",mods().noRegen().killFullHeal()),
        r("armor_break","绝对破防","护甲与物理免伤失效，所有人移速 +50%","狂赌",mods().armorOff().speed(1.5)),
        r("mana_pour","咒力倾泻","术式不耗咒力，改为每次施术消耗半颗心","狂赌",mods().special("hp_for_mana")),
        r("overclock","技能超频","冷却缩短 80%，但每次施术后虚弱 5 秒","狂赌",mods().cooldown(.2).special("weak_after")),
        r("desperado2","背水一战·改","生命越低伤害越高，最大生命 -30%","狂赌",mods().health(.7).special("desperado")),
        r("precise","精准打击","所有攻击必定暴击（伤害翻倍），基础血量 -50%","狂赌",mods().health(.5).damage(2).special("crit")),
        r("mutual_destruction","同归于尽","任何生物死亡都会引发 10 格无差别爆炸","狂赌",mods().special("death_bomb")),
        // 五、时空与物理篡改
        r("gravity_float","引力沉浮","可空中二段跳，但摔落伤害极高","时空",mods().jump(1).special("double_jump","fall_double")),
        r("quantum","量子纠缠","飞行弹道速度 -50%，判定范围 ×2","时空",mods().special("bullet_slow")),
        r("swap10","换位游戏","所有生物每 10 秒被强制传送到附近 5 格内","时空",mods().special("swap10")),
        r("time_warp","时间扭曲","药水与增益时长延长","时空",mods().special("time_warp")),
        r("fluid_air","流体窒息","移速 -30%，跳跃高度 ×3","时空",mods().speed(.7).jump(3)),
        r("no_fly_zone","低空禁飞","禁止飞行，但获得跳跃提升与安全落地","时空",mods().noFly().jump(2).special("safe_land")),
        r("mirror_shot","镜面反射","远程弹道命中方块后折射一次","时空",mods().special("mirror_shot")),
        r("no_light2","无光之夜·蚀","全图失明，怪物视野同样缩短","时空",mods().special("no_light")),
        // 六、怪物强化与掉落反转
        r("curse_mutate","咒胎变异","怪物生命 -50%，攻击与移速 +100%","怪物",mods().special("mutate_mob")),
        r("death_split","死亡分裂","怪物死亡分裂成 2 只小怪，小怪掉极品材料","怪物",mods().special("death_split")),
        r("blood_thirst","嗜血狂潮","敌人伤害提升，但击杀积分与残秽翻倍","怪物",mods().taken(1.3).score(2)),
        r("berserk","狂暴法则","怪物无视距离仇恨你，但攻击前摇 +50%","怪物",mods().special("berserk")),
        r("assimilation2","同化危机·蚀","受击叠加同化值（满 10 巨额伤害），但每次受击回咒力","怪物",mods().special("assimilation2")),
        r("elite_law","精英法则","只刷高血量精英，击杀一只 +500 积分与顶级图纸","怪物",mods().special("elite_law")),
        r("thorn_armor","反伤装甲","怪物获得 20% 反伤，你的平A/近战伤害翻倍","怪物",mods().melee(2).special("reflect")),
        r("bomb_frenzy","自爆狂潮","怪物死亡自爆，爆炸同样炸死其他怪物","怪物",mods().special("bomb")),
        // 七、生存环境与诅咒
        r("acid_rain","酸雨倾盆","露天时护甲耐久狂掉，但地面会刷修补材料","环境",mods().special("acid_rain")),
        r("live_maze2","活体迷宫·噬","血肉废墟会主动攻击靠近的玩家","环境",mods().special("live_maze")),
        r("resource_swap","资源互换","每制定一条规则，随机传送一个终极宝箱","环境",mods().special("treasure")),
        r("dead_run","亡命狂奔","移速攻速 +100%，连续奔跑 10 秒开始掉血","环境",mods().speed(2).atkSpeed(2).special("run_drain")),
        r("poison_air","剧毒空气","所有人持续中毒","环境",mods().special("poison_air")),
        r("kenjaku_deal","契约赌局","界内刷出「羂索的幻影」，可交易回满血与咒力","环境",mods().special("kenjaku"))
    );

    private CullingRules() { }

    // ===================== 状态 =====================
    private static CompoundTag ruleData(Player p){return ExpeditionProgress.data(p);}
    private static CompoundTag runData(Player p){return Curse.data(p);}
    public static int picked(Player p){return ruleData(p).getInt(COUNT);}
    public static int points(Player p){return ExpeditionProgress.data(p).getInt("CullingPoints");}
    public static boolean has(Player p,String id){
        int n=picked(p);
        for(int i=0;i<n;i++)if(id.equals(ruleData(p).getString(OPTION+i)))return true;
        return false;
    }
    public static List<Rule> active(Player p){
        int n=Math.min(MAX,picked(p));List<Rule> out=new ArrayList<>();
        for(int i=0;i<n;i++){
            String id=ruleData(p).getString(OPTION+i);
            for(Rule rule:ALL)if(rule.id().equals(id)){out.add(rule);break;}
        }
        return out;
    }
    public static boolean inDimension(Player p){return p.level().dimension().equals(CullingGame.DIM);}
    public static double mul(Player p,java.util.function.ToDoubleFunction<Mods> get){
        if(!inDimension(p))return 1;
        double v=1;for(Rule rule:active(p))v*=get.applyAsDouble(rule.mods());
        return v;
    }
    public static boolean flag(Player p,java.util.function.Predicate<Mods> test){
        if(!inDimension(p))return false;
        for(Rule rule:active(p))if(test.test(rule.mods()))return true;
        return false;
    }
    public static boolean special(Player p,String id){
        if(!inDimension(p))return false;
        for(Rule rule:active(p))if(rule.mods().special.contains(id))return true;
        return false;
    }
    public static double energyMul(Player p){return mul(p,m->m.energy);}
    public static double scoreMul(Player p){return mul(p,m->m.score);}
    public static double cooldownMul(Player p){return mul(p,m->m.cooldown);}
    public static double healMul(Player p){return mul(p,m->m.heal);}
    public static double flashChanceMul(Player p){return mul(p,m->m.flashChance);}
    public static double flashPowerAdd(Player p){return inDimension(p)?mul(p,m->m.flashPower):0;}
    public static boolean domainBanned(Player p){return flag(p,m->m.domainBanned);}
    public static boolean armorOff(Player p){return flag(p,m->m.armorOff);}
    public static boolean noHealthBar(Player p){return flag(p,m->m.noHealthBar);}

    // ===================== 面板 =====================
    public static void request(ServerPlayer p){
        if(!inDimension(p)){p.sendSystemMessage(Component.literal("规则只能在死灭洄游结界内制定。").withStyle(ChatFormatting.GRAY));return;}
        Network.sendTo(p,new Network.Effect(620,p.getId(),Vec3.ZERO,Vec3.ZERO,picked(p),points(p),packOptions(p)));
    }
    private static long packOptions(ServerPlayer p){
        List<Integer> opts=options(p);long packed=0;
        for(int i=0;i<5&&i<opts.size();i++)packed|=(long)opts.get(i)<<(7*i);
        return packed;
    }
    private static List<Integer> options(ServerPlayer p){
        CompoundTag d=ruleData(p);
        String raw=d.getString(OPTIONS);
        List<Integer> out=new ArrayList<>();
        if(!raw.isEmpty()){
            for(String s:raw.split(",")){
                try{int i=Integer.parseInt(s);if(i>=0&&i<ALL.size()&&!has(p,ALL.get(i).id()))out.add(i);}catch(NumberFormatException ignored){}
            }
            if(out.size()==5)return out;
        }
        out.clear();
        List<Integer> pool=new ArrayList<>();
        for(int i=0;i<ALL.size();i++)if(!has(p,ALL.get(i).id()))pool.add(i);
        Random rnd=new Random(p.getUUID().getMostSignificantBits()^picked(p));
        for(int i=0;i<5&&!pool.isEmpty();i++)out.add(pool.remove(rnd.nextInt(pool.size())));
        StringBuilder sb=new StringBuilder();
        for(int i:out){if(sb.length()>0)sb.append(',');sb.append(i);}
        d.putString(OPTIONS,sb.toString());
        return out;
    }
    public static void choose(ServerPlayer p,int index){
        if(!inDimension(p))return;
        int n=picked(p);
        if(index==ESCAPE){
            if(n<MAX){p.sendSystemMessage(Component.literal("必须先制定 "+MAX+" 条规则才能脱离死灭洄游（当前 "+n+" 条）。").withStyle(ChatFormatting.GRAY));return;}
            CullingGame.leave(p);return;
        }
        if(index<0||index>4)return;
        List<Integer> opts=options(p);
        if(index>=opts.size())return;
        if(n>=MAX){p.sendSystemMessage(Component.literal("已经制定满 "+MAX+" 条规则。").withStyle(ChatFormatting.GRAY));return;}
        if(points(p)<COST){p.sendSystemMessage(Component.literal("积分不足：制定规则需要 "+COST+" 分，现有 "+points(p)+"。").withStyle(ChatFormatting.GRAY));return;}
        Rule rule=ALL.get(opts.get(index));
        ExpeditionProgress.data(p).putInt("CullingPoints",points(p)-COST);
        ruleData(p).putString(OPTION+n,rule.id());
        ruleData(p).putInt(COUNT,n+1);
        ruleData(p).putString(OPTIONS,"");
        p.sendSystemMessage(Component.literal("【规则生效】"+rule.name()+" · "+rule.desc()).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.PLAYERS,1f,1.4f);
        applyModifiers(p);
        if(special(p,"treasure"))spawnTreasure(p);
        request(p);
    }

    // ===================== 属性 =====================
    public static void applyModifiers(ServerPlayer p){
        boolean on=inDimension(p)&&picked(p)>0;
        put(p,Attributes.MAX_HEALTH,HEALTH,(on?mul(p,m->m.health):1)-1);
        put(p,Attributes.MOVEMENT_SPEED,SPEED,(on?mul(p,m->m.speed):1)-1);
        put(p,Attributes.ATTACK_DAMAGE,ATTACK,(on?mul(p,m->m.damage):1)-1);
        put(p,Attributes.ATTACK_SPEED,ATK_SPEED,(on?mul(p,m->m.atkSpeed):1)-1);
        put(p,Attributes.ARMOR,ARMOR,on&&flag(p,m->m.armorOff)?-1:0);
        int jump=0;if(on)for(Rule rule:active(p))jump+=rule.mods().jump;
        put(p,Attributes.JUMP_STRENGTH,JUMP,jump*.08);
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
    }
    private static final Attribute[] ATTRS={Attributes.MAX_HEALTH,Attributes.ATTACK_DAMAGE,Attributes.MOVEMENT_SPEED,Attributes.ATTACK_SPEED,Attributes.JUMP_STRENGTH,Attributes.ARMOR};
    private static void put(ServerPlayer p,Attribute attr,UUID id,double amount){
        AttributeInstance a=p.getAttribute(attr);if(a==null)return;
        var old=a.getModifier(id);
        if(Math.abs(amount)<1e-6){if(old!=null)a.removeModifier(id);return;}
        if(old!=null&&Math.abs(old.getAmount()-amount)<1e-6)return;
        if(old!=null)a.removeModifier(id);
        a.addTransientModifier(new AttributeModifier(id,"死灭规则",amount,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    public static void clear(ServerPlayer p){
        for(UUID id:new UUID[]{HEALTH,ATTACK,SPEED,ATK_SPEED,JUMP,ARMOR}){
            for(Attribute attr:ATTRS){AttributeInstance a=p.getAttribute(attr);if(a!=null&&a.getModifier(id)!=null)a.removeModifier(id);}
        }
    }
    public static void onLeave(ServerPlayer p){
        clear(p);
        runData(p).putBoolean("CullingFinalClear",false);
        runData(p).putInt("CullingAssimilation",0);
        runData(p).putInt("CullingOverload",0);
    }

    // ===================== 伤害 =====================
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void hurt(LivingHurtEvent e){
        LivingEntity victim=e.getEntity();
        var attacker=e.getSource().getEntity();
        boolean spell=e.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
            ||e.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
            ||e.getSource().is(net.minecraft.world.damagesource.DamageTypes.WITHER);
        boolean cullingMob = attacker instanceof Mob am && am.level().dimension().equals(CullingGame.DIM)
            && am.getPersistentData().getBoolean("CullingEnemy");
        if (cullingMob) e.setAmount((float) (e.getAmount() * CullingSpawns.npcDamageMul()));
        boolean cullingVictim = victim instanceof Mob vm && vm.level().dimension().equals(CullingGame.DIM)
            && vm.getPersistentData().getBoolean("CullingEnemy");
        if (cullingVictim) e.setAmount((float) (e.getAmount() * CullingSpawns.npcTakenMul()));
        if(attacker instanceof ServerPlayer ap&&inDimension(ap)){
            double mul=(spell?mul(ap,m->m.spell):mul(ap,m->m.melee))*mul(ap,m->m.damage);
            if(special(ap,"desperado"))mul*=1+(1-ap.getHealth()/Math.max(1,ap.getMaxHealth()));
            if(special(ap,"crit"))mul*=2;
            if(special(ap,"overload"))mul*=1+.15*Math.min(6,runData(ap).getInt("CullingOverload"));
            e.setAmount((float)(e.getAmount()*mul));
            if(special(ap,"weak_after")&&runData(ap).getBoolean("CullingWeak"))e.setAmount(e.getAmount()*.5f);
            if(special(ap,"hunger_hit"))ap.causeFoodExhaustion(2f);
        }
        if(victim instanceof ServerPlayer vp&&inDimension(vp)){
            double mul=mul(vp,m->m.taken);
            if(special(vp,"desperado"))mul*=1+(1-vp.getHealth()/Math.max(1,vp.getMaxHealth()))*.5;
            if(vp.getPersistentData().getBoolean("CullingArmorOff")){/* 由 tick 维护，这里只兜底 */}
            e.setAmount((float)(e.getAmount()*mul));
            if(special(vp,"steal_attack")&&attacker instanceof LivingEntity living){
                var a=living.getAttribute(Attributes.ATTACK_DAMAGE);
                if(a!=null&&a.getValue()>1){
                    a.removeModifier(STOLEN);
                    a.addTransientModifier(new AttributeModifier(STOLEN,"被偷攻击",-1,AttributeModifier.Operation.ADDITION));
                }
            }
            if(special(vp,"assimilation")||special(vp,"assimilation2")){
                CompoundTag d=runData(vp);
                int stacks=Math.min(10,d.getInt("CullingAssimilation")+1);d.putInt("CullingAssimilation",stacks);
                vp.displayClientMessage(Component.literal("受肉同化 "+stacks+" / 10").withStyle(ChatFormatting.DARK_RED),true);
                if(stacks>=10){
                    d.putInt("CullingAssimilation",0);
                    vp.hurt(vp.damageSources().magic(),vp.getMaxHealth()*.6f);
                    if(special(vp,"assimilation2"))Curse.addRaw(vp,Curse.maximum(vp)*.05);
                }
            }
            if(special(vp,"reflect")&&attacker instanceof LivingEntity living)living.hurt(vp.damageSources().thorns(vp),e.getAmount()*.2f);
            if(special(vp,"fall_double")&&e.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL))e.setAmount(e.getAmount()*2);
        }
    }

    // ===================== 死亡 =====================
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void killed(LivingDeathEvent e){
        LivingEntity dead=e.getEntity();
        if(!dead.level().dimension().equals(CullingGame.DIM))return;
        ServerPlayer killer=CombatSettings.caster(e.getSource());
        if(killer==null&&dead.getKillCredit() instanceof ServerPlayer credited)killer=credited;
        if(killer!=null&&inDimension(killer)){
            if(special(killer,"killFullHeal"))killer.heal(killer.getMaxHealth());
            if(special(killer,"death_split")&&dead instanceof Mob mob&&dead.getMaxHealth()<800)splitMob(mob);
        }
        if(dead instanceof ServerPlayer dp&&inDimension(dp)&&special(dp,"no_exit")){
            ExpeditionProgress.data(dp).putInt("CullingPoints",0);
            dp.sendSystemMessage(Component.literal("背水一战：死亡清空所有死灭积分。").withStyle(ChatFormatting.RED));
        }
        boolean bomb=false;
        for(ServerPlayer p:players())if(inDimension(p)&&(special(p,"bomb")||special(p,"death_bomb"))){bomb=true;break;}
        if(bomb&&dead.getMaxHealth()<800&&dead.level() instanceof ServerLevel level)
            level.explode(null,dead.getX(),dead.getY()+.5,dead.getZ(),3.2f,net.minecraft.world.level.Level.ExplosionInteraction.NONE);
    }
    private static List<ServerPlayer> players(){
        var server=ServerLifecycleHooks.getCurrentServer();
        return server==null?List.of():new ArrayList<>(server.getPlayerList().getPlayers());
    }
    private static void splitMob(Mob mob){
        if(!(mob.level() instanceof ServerLevel level))return;
        for(int i=0;i<2;i++){
            Mob child=ExpeditionMobs.spawn(level,mob.blockPosition(),"low_curse",null);
            if(child==null)return;
            var hp=child.getAttribute(Attributes.MAX_HEALTH);
            if(hp!=null){hp.setBaseValue(Math.max(4,hp.getBaseValue()*.3));child.setHealth(child.getMaxHealth());}
        }
    }
    private static void spawnTreasure(ServerPlayer p){
        if(!(p.level() instanceof ServerLevel level))return;
        var rnd=level.random;
        for(int tries=0;tries<32;tries++){
            int x=(int)p.getX()+rnd.nextInt(121)-60,z=(int)p.getZ()+rnd.nextInt(121)-60;
            BlockPos base=new BlockPos(x,65,z);
            if(!level.hasChunkAt(base))continue;
            BlockPos at=level.getBlockState(base).isAir()?base:base.above();
            level.setBlockAndUpdate(at,Blocks.CHEST.defaultBlockState());
            if(level.getBlockEntity(at) instanceof ChestBlockEntity chest)
                chest.setLootTable(new ResourceLocation("sukuna","chests/hidden_rich"),rnd.nextLong());
            for(ServerPlayer viewer:level.players())
                if(viewer.distanceToSqr(Vec3.atCenterOf(at))<96*96)
                    viewer.displayClientMessage(Component.literal("资源互换：终极宝箱出现在 ("+x+", "+at.getY()+", "+z+")").withStyle(ChatFormatting.GOLD),false);
            return;
        }
    }

    // ===================== 每刻 =====================
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p instanceof FakePlayer)return;
        if(!inDimension(p)){
            if(p.tickCount%100==0)clear(p);
            return;
        }
        if(p.tickCount%20==0)applyModifiers(p);
        int n=picked(p);
        if(n==0)return;
        CompoundTag d=runData(p);
        long now=p.level().getGameTime();
        if(flag(p,m->m.noEnergyRegen))Curse.status(p,"RecoveryDelay",40);
        if(flag(p,m->m.noRegen)&&p.tickCount%20==0&&p.getHealth()<p.getMaxHealth())
            p.setHealth(Math.max(1,p.getHealth()-0.01f));
        if(special(p,"lava_rain")&&p.tickCount%40==0&&p.level().canSeeSky(p.blockPosition()))
            p.setRemainingFireTicks(Math.max(p.getRemainingFireTicks(),60));
        if(special(p,"poison_air")&&p.tickCount%60==0&&!p.hasEffect(MobEffects.POISON))
            p.addEffect(new MobEffectInstance(MobEffects.POISON,100,0,false,false));
        if(special(p,"acid_rain")&&p.tickCount%40==0&&p.level().canSeeSky(p.blockPosition()))
            for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
                ItemStack stack=p.getItemBySlot(slot);
                if(stack.isDamageableItem())stack.hurtAndBreak(2,p,pl->pl.broadcastBreakEvent(slot));
            }
        if((special(p,"no_light")||special(p,"fog"))&&p.tickCount%80==0)
            p.addEffect(new MobEffectInstance(special(p,"fog")?MobEffects.DARKNESS:MobEffects.BLINDNESS,100,0,false,false));
        if(special(p,"blood_swamp")&&p.tickCount%40==0&&p.onGround()&&p.getRandom().nextInt(3)==0){
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0,false,false));
            p.hurt(p.damageSources().magic(),1.5f);
        }
        if(special(p,"live_maze")&&p.tickCount%40==0&&p.getRandom().nextInt(4)==0)
            p.hurt(p.damageSources().magic(),2f);
        if(special(p,"forbidden_zone")&&p.tickCount%20==0){
            double dist=p.position().distanceTo(Vec3.atCenterOf(new BlockPos(0,65,0)));
            if(dist<24)Curse.add(p,-Math.max(1,Curse.maximum(p)*.01));
        }
        if(special(p,"swap30")&&now%600==0)swapRandom(p,64);
        if(special(p,"swap10")&&now%200==0)swapRandom(p,5);
        if(special(p,"run_drain")){
            if(p.isSprinting())d.putInt(RUN,d.getInt(RUN)+1);else d.putInt(RUN,0);
            if(d.getInt(RUN)>200&&d.getInt(RUN)%20==0)p.hurt(p.damageSources().generic(),1f);
        }
        if(special(p,"overload")){
            if(Curse.active(p,"CharacterGlobalCooldown"))d.putLong("CullingOverloadAt",now);
            else if(now-d.getLong("CullingOverloadAt")>60)d.putInt("CullingOverload",0);
            int stacks=d.getInt("CullingOverload");
            if(stacks>0&&p.tickCount%20==0)p.hurt(p.damageSources().magic(),stacks*.5f);
        }
        if(special(p,"weak_after")){
            boolean weak=d.getBoolean("CullingWeak");
            if(weak&&now>=d.getLong("CullingWeakUntil"))d.putBoolean("CullingWeak",false);
        }
        if(special(p,"final_clear")&&p.tickCount%40==0&&!d.getBoolean("CullingFinalClear")){
            int others=0;
            for(ServerPlayer other:p.serverLevel().players())
                if(other!=p&&!other.isSpectator()&&inDimension(other))others++;
            if(others==0){
                d.putBoolean("CullingFinalClear",true);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,400,4,false,false));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,400,4,false,false));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,400,3,false,false));
                p.displayClientMessage(Component.literal("最终清算：天元庇护发动（全属性 +200%）").withStyle(ChatFormatting.GOLD),false);
            }
        }
    }
    private static void swapRandom(ServerPlayer p,int range){
        var rnd=p.getRandom();
        for(int tries=0;tries<24;tries++){
            double x=p.getX()+rnd.nextInt(range*2+1)-range,z=p.getZ()+rnd.nextInt(range*2+1)-range;
            BlockPos at=new BlockPos((int)Math.floor(x),(int)p.getY(),(int)Math.floor(z));
            if(!p.level().hasChunkAt(at))continue;
            if(!p.level().getBlockState(at).getCollisionShape(p.level(),at).isEmpty())continue;
            if(!p.level().getBlockState(at.above()).getCollisionShape(p.level(),at.above()).isEmpty())continue;
            p.teleportTo(x,at.getY(),z);p.fallDistance=0;return;
        }
    }
    /** 技能施放后由 CharacterCasting 调用：过载叠层、超频虚弱、咒力倾泻扣血。 */
    public static void afterCast(ServerPlayer p){
        if(!inDimension(p))return;
        CompoundTag d=runData(p);
        if(special(p,"overload"))d.putInt("CullingOverload",Math.min(6,d.getInt("CullingOverload")+1));
        if(special(p,"weak_after")){d.putBoolean("CullingWeak",true);d.putLong("CullingWeakUntil",p.level().getGameTime()+100);}
        if(special(p,"hp_for_mana"))p.hurt(p.damageSources().magic(),1f);
    }
}
