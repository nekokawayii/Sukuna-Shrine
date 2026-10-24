package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

/** 戴上「法轮」之后的**自身适应**：与魔虚罗同款的法轮机制，但直接以"整只生物"为单位。
 *
 *  <p>规则：被某只生物打中 → 对该生物**整只**累积适应，打满 {@link #CLEAR} 次后它的**所有攻击**对你无效
 *  （不再区分招式）。所有非生物来源的伤害（摔落、火焰、溺水、虚空、中毒、其他模组的场地伤害…）
 *  共用一条「其他伤害」进度，打满后一并免疫。进度按玩家存档，召唤友方魔虚罗时**继承**过去。</p> */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="sukuna")
public final class MahoragaAdaptStore {
    /** 打同一个来源达到这一次数 → "完全适应它的存在"：对其真伤 ×2 + 命中 20 秒禁疗。 */
    public static final int CLEAR=12;
    /** 挨同一个来源打：{@link #HALF} 次减伤到 50%，攒到 {@link #IMMUNE} 次直接免疫。 */
    public static final int HALF=12,IMMUNE=24;
    /** 挨打进度对应的伤害倍率：0~11 次 100%→50%，12~23 次 50%→0%，24 次起免疫（0）。 */
    public static float takenFactor(int hits){
        return AdaptationRules.takenFactor(hits);
    }
    /** 完全适应之后的收益：对其伤害**翻倍**、转为**真伤**，且命中后 20 秒内它无法回血。 */
    public static final float ADAPTED_DAMAGE=2f;
    public static final int ADAPTED_NO_HEAL_TICKS=20*20;
    /** 被"已完全适应你的存在"的对手打中后的禁疗标记。 */
    public static final String NO_HEAL_UNTIL="MahoNoHealUntil";
    /** 该生物此刻是否不能回血。 */
    public static boolean noHeal(LivingEntity e){
        return e!=null&&e.getPersistentData().getLong(NO_HEAL_UNTIL)>e.level().getGameTime();
    }
    private static void applyNoHeal(LivingEntity e){
        e.getPersistentData().putLong(NO_HEAL_UNTIL,e.level().getGameTime()+ADAPTED_NO_HEAL_TICKS);
        e.getPersistentData().putBoolean("SukunaNoHeal",true);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void noHealEvent(net.minecraftforge.event.entity.living.LivingHealEvent event){
        if(noHeal(event.getEntity()))event.setCanceled(true);
    }
    /** 非生物伤害（环境/摔落/其他模组场地伤害）专用的键。 */
    public static final String ENVIRONMENT="env";
    private static final String KEY="MahoAdapted";
    /** 键 → 显示名（键是实体类型 id 或 {@link #ENVIRONMENT}）。 */
    private static final Map<UUID,Map<String,String>> NAMES=new HashMap<>();
    /** 运行时缓存：实体类型 id → 显示名，避免每次都查注册表。 */
    private static final Map<String,String> TYPE_NAMES=new HashMap<>();

    private static String typeKey(Entity entity){
        ResourceLocation id=ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id==null?"unknown":id.toString();
    }
    private static String display(String key){
        if(ENVIRONMENT.equals(key))return "其他伤害";
        if(key.equals("technique:blood"))return "血液";
        if(key.equals("technique:ten_shadows"))return "十影";
        return TYPE_NAMES.computeIfAbsent(key,k->{
            try{
                ResourceLocation id=new ResourceLocation(k);
                EntityType<?> type=ForgeRegistries.ENTITY_TYPES.getValue(id);
                if(type!=null)return type.getDescription().getString();
            }catch(RuntimeException ignored){}
            return k;
        });
    }
    /** 两条独立的适应进度：被它打（TAKEN）与打它（DEALT），各自累积，各自打满即完全适应。 */
    public static final String TAKEN="taken/",DEALT="dealt/";
    private static Map<String,Integer> read(ServerPlayer p){
        CompoundTag tag=Curse.data(p).getCompound(KEY);
        Map<String,Integer> out=new LinkedHashMap<>();
        for(String key:tag.getAllKeys())out.put(key,Math.max(0,Math.min(IMMUNE,tag.getInt(key))));
        return out;
    }
    private static void write(ServerPlayer p,Map<String,Integer> map){
        CompoundTag tag=new CompoundTag();
        for(var e:map.entrySet())if(e.getValue()>0)tag.putInt(e.getKey(),Math.min(IMMUNE,e.getValue()));
        Curse.data(p).put(KEY,tag);
    }
    /** 双向合并只取较大进度；收回、重召不会叠加次数，更不会覆盖较新的存档。 */
    public static void merge(ServerPlayer owner,Map<String,Integer> incoming){
        Map<String,Integer> saved=read(owner),merged=AdaptationRules.merge(saved,incoming,IMMUNE);
        if(!saved.equals(merged)){write(owner,merged);sync(owner);}
    }
    private static boolean trueDamage;
    /** 记一次进度（被攻击 / 攻击共用）。 */
    private static boolean record(ServerPlayer p,String counter,LivingEntity source,DamageSource damageSource){
        String key=damageSource!=null?keyOf(damageSource):(source==null?null:keyOfEntity(source));
        if(key==null)return false;
        Map<String,Integer> map=read(p);
        String slot=counter+key;
        int hits=map.getOrDefault(slot,0);
        if(hits>=IMMUNE)return false;
        int next=hits+1;map.put(slot,next);write(p,map);sync(p);
        String name=display(key);
        int threshold=counter.equals(TAKEN)?IMMUNE:CLEAR;
        boolean done=next>=threshold;
        p.displayClientMessage(Component.literal(done
            ?"法轮已适应「"+name+"」："+(counter.equals(TAKEN)?"该来源伤害免疫":"对其真伤翻倍并禁疗20秒")
            :"法轮适应「"+name+"」 "+(counter.equals(TAKEN)?"受击":"攻击")+" "+next+"/"+threshold)
            .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(done?0xFFD24A:0xE8D8FF)),true);
        if(done){
            p.serverLevel().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.PLAYERS,1.6f,1.4f);
            MahoragaVisuals.adaptAt(p.position().add(0,p.getBbHeight()*0.5,0),completedCount(p));
        }
        return true;
    }
    /** 已完全适应的键集合（打满 {@link #CLEAR}）。 */
    public static Set<String> completed(ServerPlayer p){
        Set<String> out=new LinkedHashSet<>();
        for(var e:read(p).entrySet())if(e.getValue()>=(e.getKey().startsWith(TAKEN)?IMMUNE:CLEAR))out.add(e.getKey().substring(e.getKey().indexOf('/')+1));
        return out;
    }
    /** 已完全适应的来源数量（含"其他伤害"）。 */
    public static int completedCount(ServerPlayer p){return completed(p).size();}
    /** 正在累积中的总进度 0~1（用于法轮在完全适应之前也能转）。 */
    public static float partial(ServerPlayer p){
        Map<String,Integer> map=read(p);if(map.isEmpty())return 0;
        float total=0;for(int hits:map.values())if(hits<CLEAR)total+=hits/(float)CLEAR;
        return Math.min(1f,total);
    }
    /** 法轮四格显示值（0~4）。 */
    public static int cells(ServerPlayer p){return Math.min(MahoragaEntity.CELLS,completedCount(p));}
    /** 对某个伤害来源是否已经完全适应（完全免疫）。 */
    public static boolean adapted(ServerPlayer p,DamageSource source){
        String key=keyOf(source);
        if(key==null)return false;
        Map<String,Integer> map=read(p);
        return map.getOrDefault(TAKEN+key,0)>=IMMUNE;
    }
    /** 玩家打某个目标是否也不再造成伤害（攻击适应满了）。 */
    public static boolean adaptedDealt(ServerPlayer p,LivingEntity victim){
        String key=keyOfEntity(victim);
        if(key==null)return false;
        return read(p).getOrDefault(DEALT+key,0)>=CLEAR;
    }
    /** 实体对应的适应键（实体类型 id）。 */
    public static String keyOfEntity(Entity entity){return entity==null?null:typeKey(entity);}
    /** 伤害来源对应的适应键：生物 → 它的实体类型；其余 → {@link #ENVIRONMENT}。 */
    public static String keyOf(DamageSource source){
        if(source==null)return null;
        String type=AttackTypes.of(source);if(AttackTypes.BLOOD.equals(type))return "technique:blood";if(AttackTypes.TEN_SHADOWS.equals(type))return "technique:ten_shadows";
        Entity attacker=source.getEntity()!=null?source.getEntity():source.getDirectEntity();
        if(attacker instanceof LivingEntity&&!(attacker instanceof ServerPlayer))return typeKey(attacker);
        if(attacker instanceof ServerPlayer)return typeKey(attacker); // 玩家也按"整只生物"算
        return ENVIRONMENT;
    }
    /** 被某个来源打中一次（12 次后完全适应，它的攻击对你无效）。 */
    public static boolean adapt(ServerPlayer p,LivingEntity attacker,DamageSource source){return record(p,TAKEN,attacker,source);}
    /** 打中某个来源一次（12 次后完全适应"它的存在"，你打它也不再造成伤害）。 */
    public static boolean adaptDealt(ServerPlayer p,LivingEntity victim){return record(p,DEALT,victim,null);}
    /** 把适应进度同步给客户端（头顶法轮用）。 */
    public static void sync(ServerPlayer p){
        int completed=completedCount(p),partialHits=0;
        for(int hits:read(p).values())if(hits<CLEAR)partialHits+=hits;
        Network.send(p.serverLevel(),new Network.Effect(MahoragaVisuals.WHEEL_BEAT,p.getId(),p.position(),net.minecraft.world.phys.Vec3.ZERO,
            Math.min(MahoragaEntity.CELLS,completed)*16+Math.min(15,partialHits),1,completed));
    }

    /** 这只生物对这名玩家是否"自己人"：主人的友方魔虚罗 + 主人自己的女仆。 */
    public static boolean friendlyTo(ServerPlayer player,LivingEntity target){
        if(player==null||target==null)return false;
        if(target instanceof MahoragaEntity m&&m.ally()){
            UUID owner=m.allyOwner();
            if(owner==null)return false;
            if(owner.equals(player.getUUID()))return true;
            ServerPlayer real=player.server.getPlayerList().getPlayer(owner);
            return real!=null&&!CombatSettings.canHit(player,real)&&!CombatSettings.canHit(real,player);
        }
        return false;
    }
    // ------------------------------------------------------------ 召唤友方魔虚罗
    public static final double SUMMON_RANGE=48;
    /** 潜行右键空气：放出（或叫回）**同一只**友方魔虚罗。 */
    public static boolean summonAlly(ServerPlayer owner){
        if(!TenShadows.incarnated(owner)||!TenShadows.tamed(owner))return false;
        ServerLevel level=owner.serverLevel();
        MahoragaEntity ally=findAlly(level,owner);
        boolean fresh=false;
        if(ally==null){
            recallAll(owner); // 远处或其他维度的旧实体先保存并收回，杜绝重复显现。
            ally=VoidContent.MAHORAGA.get().create(level);
            if(ally==null)return false;
            fresh=true;
        }
        // 两种显现共用主人存档中的适应；不继承敌对调服本体的招式适应。
        var look=owner.getLookAngle();
        double x=owner.getX()-look.x*2.5,z=owner.getZ()-look.z*2.5;
        ally.moveTo(x,owner.getY(),z,owner.getYRot(),0);
        if(!level.noCollision(ally))ally.moveTo(owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),0);
        if(!level.noCollision(ally)){owner.displayClientMessage(Component.literal("这里容不下魔虚罗，请到更开阔的地方召唤"),true);return false;}
        ally.setAllyMode(owner.getUUID(),true);
        ally.shareAdaptation(owner); // 先合并实体进度，再读取主人较新的进度
        ally.setHealth(ally.getMaxHealth()); // 每次召唤血量回满
        ally.clearRitual();
        if(ally.getMaxHealth()!=MahoragaEntity.ALLY_HEALTH)ally.resizeAllyHealth();
        if(fresh){if(!level.addFreshEntity(ally))return false;ally.emerge();}
        ShadowSystem.fx(level,0,owner.position(),owner.position(),2,40);
        swapHeld(owner,VoidContent.MAHORAGA_WHEEL_RECALL.get()); // 手里的法轮 → 法轮·收回
        owner.displayClientMessage(Component.literal("友方魔虚罗已就位 · 生命 "+BalanceRules.number(ally.getMaxHealth())
            +" · 与不完全显现共享适应").setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFFD24A)),true);
        return true;
    }
    public static void recallAll(ServerPlayer owner){
        java.util.List<MahoragaEntity> found=new java.util.ArrayList<>();
        for(ServerLevel level:owner.server.getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof MahoragaEntity ally&&ally.isAllyOf(owner.getUUID()))found.add(ally);
        for(MahoragaEntity ally:found)recallSpecific(owner,ally);
    }
    public static void recallSpecific(ServerPlayer owner,MahoragaEntity ally){
        if(!ally.isAllyOf(owner.getUUID()))return;
        ShadowSystem.fx((ServerLevel)ally.level(),0,ally.position(),ally.position(),2,35);
        ally.shareAdaptation(owner);ally.stopRiding();ally.discard();restoreWheel(owner);
        owner.displayClientMessage(Component.literal("友方魔虚罗已收回"),true);
    }
    /** 收回：只有场上确实有友方魔虚罗时才成功，并把「收回」变回法轮。 */
    public static boolean recallAlly(ServerPlayer owner,net.minecraft.world.InteractionHand hand){
        MahoragaEntity ally=findAlly(owner.serverLevel(),owner);
        if(ally==null){
            restoreWheel(owner);
            owner.displayClientMessage(Component.literal("场上已经没有你的友方魔虚罗（被打掉或走远了），法轮已变回原样").setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFF9A5A)),true);
            return false;
        }
        // 收回前落盘；下一次实体重新创建但适应不清空。
        ally.shareAdaptation(owner);ally.stopRiding();
        ally.discard();
        restoreWheel(owner);
        owner.displayClientMessage(Component.literal("友方魔虚罗已收回，法轮变回原样").setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFFD24A)),true);
        return true;
    }
    /** 切到「不完全显现」时调用：两种显现互斥，场上若有友方魔虚罗则先收回（没有就什么都不做）。 */
    public static void banishAlly(ServerPlayer owner){
        MahoragaEntity ally=findAlly(owner.serverLevel(),owner);
        if(ally==null)return;
        ally.shareAdaptation(owner);ally.stopRiding();
        ally.discard();
        restoreWheel(owner);
        owner.displayClientMessage(Component.literal("已选择不完全显现 · 友方魔虚罗先收回").setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xE8D8FF)),true);
    }
    /** 手上是否拿着「法轮 · 收回」。 */
    private static boolean holdsRecall(ServerPlayer owner){
        for(net.minecraft.world.InteractionHand hand:net.minecraft.world.InteractionHand.values())
            if(owner.getItemInHand(hand).is(VoidContent.MAHORAGA_WHEEL_RECALL.get()))return true;
        return false;
    }
    /** 把手里与背包里的「法轮 · 收回」全部变回法轮（召唤结束/友方已消失时调用）。 */
    private static void restoreWheel(ServerPlayer owner){
        boolean done=false;
        for(net.minecraft.world.InteractionHand hand:net.minecraft.world.InteractionHand.values()){
            var stack=owner.getItemInHand(hand);
            if(stack.is(VoidContent.MAHORAGA_WHEEL_RECALL.get())){
                var next=new net.minecraft.world.item.ItemStack(VoidContent.MAHORAGA_WHEEL.get(),stack.getCount());
                if(stack.getTag()!=null)next.setTag(stack.getTag().copy());
                owner.setItemInHand(hand,next);done=true;
            }
        }
        var inv=owner.getInventory();
        for(int i=0;i<inv.getContainerSize();i++){
            var stack=inv.getItem(i);
            if(stack.is(VoidContent.MAHORAGA_WHEEL_RECALL.get())){
                var next=new net.minecraft.world.item.ItemStack(VoidContent.MAHORAGA_WHEEL.get(),stack.getCount());
                if(stack.getTag()!=null)next.setTag(stack.getTag().copy());
                inv.setItem(i,next);done=true;
            }
        }
        if(done)inv.setChanged();
    }
    /** 召唤成功后：手里与背包里的法轮**全部**换成「收回」，避免出现"头上戴着 + 手里还有一份"的复制感。 */
    private static void swapHeld(ServerPlayer owner,net.minecraft.world.item.Item to){
        boolean done=false;
        for(net.minecraft.world.InteractionHand hand:net.minecraft.world.InteractionHand.values()){
            var stack=owner.getItemInHand(hand);
            if(stack.is(VoidContent.MAHORAGA_WHEEL.get())||stack.is(VoidContent.MAHORAGA_WHEEL_RECALL.get())){
                var next=new net.minecraft.world.item.ItemStack(to,stack.getCount());
                if(stack.getTag()!=null)next.setTag(stack.getTag().copy());
                owner.setItemInHand(hand,next);
                done=true;
            }
        }
        var inv=owner.getInventory();
        for(int i=0;i<inv.getContainerSize();i++){
            var stack=inv.getItem(i);
            if(stack.is(VoidContent.MAHORAGA_WHEEL.get())||stack.is(VoidContent.MAHORAGA_WHEEL_RECALL.get())){
                var next=new net.minecraft.world.item.ItemStack(to,stack.getCount());
                if(stack.getTag()!=null)next.setTag(stack.getTag().copy());
                inv.setItem(i,next);
                done=true;
            }
        }
        if(done)inv.setChanged();
    }
    private static MahoragaEntity findAlly(ServerLevel level,ServerPlayer owner){
        for(MahoragaEntity e:level.getEntitiesOfClass(MahoragaEntity.class,owner.getBoundingBox().inflate(SUMMON_RANGE),LivingEntity::isAlive))
            if(e.isAllyOf(owner.getUUID()))return e;
        return null;
    }
    // ------------------------------------------------------------ 事件
    /** 戴法轮的玩家每秒把进度同步到头顶法轮（客户端只用它画模型）。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var server=event.getServer();if(server==null)return;
        if(server.getTickCount()%20!=0)return;
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            if(MahoragaRules.wheelOnHead(p))sync(p);
            if(holdsRecall(p)&&findAlly(p.serverLevel(),p)==null)restoreWheel(p); // 友方没了：自动变回法轮
        }
    }
    /** 挨打：完全适应过的来源直接免疫；没满的记一次进度。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event){
        if(event.getAmount()<=0)return;
        LivingEntity victim=event.getEntity();
        // 玩家本体或"女仆施术上下文"都归到同一个玩家身上
        ServerPlayer wearer=victim instanceof ServerPlayer self?self:MaidBridge.progression(victim);
        if(wearer==null)return;
        if(!MahoragaRules.wheelOnHead(wearer))return; // 没戴法轮：没有自身适应
        if(event.getSource().getEntity() instanceof LivingEntity attacker&&friendlyTo(wearer,attacker))return;
        // 被同一个来源打：0~11 次递减到 50%，12~23 次继续递减，24 次起完全免疫
        String srcKey=keyOf(event.getSource());
        if(srcKey!=null){
            int taken=read(wearer).getOrDefault(TAKEN+srcKey,0);
            if(taken>0){
                float factor=takenFactor(taken);
                if(factor<=0f){
                    event.setAmount(0);event.setCanceled(true);
                    MahoragaVisuals.adaptAt(wearer.position().add(0,wearer.getBbHeight()*0.5,0),completedCount(wearer));
                    return;
                }
                event.setAmount(event.getAmount()*factor);
                if(wearer.tickCount%20==0)wearer.displayClientMessage(Component.literal("法轮减伤 "+Math.min(taken,IMMUNE)+"/"+IMMUNE+"（当前 "+Math.round(factor*100)+"%）")
                    .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0x9BE8FF)),true);
            }
        }
        Entity attacker=event.getSource().getEntity()!=null?event.getSource().getEntity():event.getSource().getDirectEntity();
        // 挨打只负责涨"被攻击"这条进度：12 次之后，**你打它**才是真伤翻倍禁疗（见 dealt）
        adapt(wearer,attacker instanceof LivingEntity le?le:null,event.getSource());
    }
    /** 玩家打中某个生物：也计入"适应它的存在"（12 次后你打它、它打你都无效）。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void dealt(net.minecraftforge.event.entity.living.LivingHurtEvent event){
        if(trueDamage||event.getAmount()<=0||event.getSource().getDirectEntity() instanceof MahoragaEntity)return;
        LivingEntity victim=event.getEntity();
        if(victim.level().isClientSide)return;
        ServerPlayer p=CombatSettings.caster(event.getSource());
        if(p==null&&event.getSource().getEntity() instanceof ServerPlayer sp)p=sp;
        if(p==null)return;
        if(!MahoragaRules.wheelOnHead(p))return;
        if(!CombatSafety.canAffect(p,victim))return; // 友方、默认玩家和禁止 PvP 都不计适应
        if(victim instanceof MahoragaEntity m&&m.ally())return;
        if(adaptedDealt(p,victim)){
            // 已经完全适应它的存在：这一击变成**真伤**（无视护甲/抗性/无敌帧）且**伤害翻倍**，
            // 并让它 20 秒内无法回血
            LivingEntity le=victim;
            float amount=event.getAmount()*ADAPTED_DAMAGE;
            le.invulnerableTime=0;
            trueDamage=true;
            try{if(CombatSafety.hurt(p,le,p.serverLevel().damageSources().genericKill(),amount))applyNoHeal(le);}finally{trueDamage=false;}
            event.setAmount(0);event.setCanceled(true);
            MahoragaVisuals.adaptAt(le.getBoundingBox().getCenter(),completedCount(p));
            if(le.tickCount%20==0)p.displayClientMessage(Component.literal("法轮 · 真伤 x"+BalanceRules.number(ADAPTED_DAMAGE)+" · 目标 20 秒禁疗")
                .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFFD24A)),true);
            return;
        }
        adaptDealt(p,victim);
    }
    private MahoragaAdaptStore(){}
}
