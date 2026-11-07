package character.mainline;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.*;
import org.joml.Vector3f;

/** 所有术式共用的服务端成长线；永久状态随玩家保存，临时领域不写进角色快照。 */
public final class Mainline {
    public static final int SORCERER=0,REVERSE=1,REVERSE_OUT=2,BARRIER=3,SIMPLE=4,DOMAIN=5,AMPLIFY=6;
    public static final int SKILL_REVERSE=3100,SKILL_REVERSE_OUT=3101,SKILL_BARRIER=3102,SKILL_SIMPLE=3103,SKILL_DOMAIN=3104,SKILL_AMPLIFY=3105;
    /** 「咒术师」之后的咒力总量训练：第 1 级在索引 7，共 10 级；第 n 级要 3n 只、给 300n 咒力上限。 */
    public static final int TRAIN_FIRST=7,TRAIN_COUNT=10,TRAIN_KILLS_PER=3,TRAIN_ENERGY_PER=300;
    public static final String[] NAMES,IDS,REQUIREMENTS;
    private static final int[] PARENT;
    static {
        String[] names={"咒术师","反转术式","反转术式外放","结界术","简易领域","领域展开","领域展延"};
        String[] ids={"sorcerer","reverse","reverse_out","barrier","simple","domain","amplify"};
        String[] req={"使用术式选择书","生命降至 5% 以下时，有 50% 概率领悟","附近友方生命降至 15% 以下时，有 50% 概率领悟","在敌方领域内连续存活 15 秒","击败一名拥有领域的敌人","累计成功使用简易领域 15 次","累计成功展开领域 10 次"};
        int[] parent={-1,0,1,0,3,4,5};
        int total=names.length+TRAIN_COUNT;
        NAMES=new String[total];IDS=new String[total];REQUIREMENTS=new String[total];PARENT=new int[total];
        for(int i=0;i<names.length;i++){NAMES[i]=names[i];IDS[i]=ids[i];REQUIREMENTS[i]=req[i];PARENT[i]=parent[i];}
        for(int n=1;n<=TRAIN_COUNT;n++){
            int i=TRAIN_FIRST+n-1;
            NAMES[i]="咒力总量训练"+n;
            IDS[i]="energy_train_"+n;
            REQUIREMENTS[i]="累计击杀 "+(TRAIN_KILLS_PER*n)+" 只咒灵，咒力上限 +"+(TRAIN_ENERGY_PER*n);
            PARENT[i]=i==TRAIN_FIRST?SORCERER:i-1;
        }
    }
    private static final String PREFIX="MainlineUnlocked",COUNT="MainlineCount",KILLS="MainlineKills";
    public static final int SIMPLE_RADIUS=10,SIMPLE_TICKS=200,BARRIER_RADIUS=15,BARRIER_TICKS=200;
    private record Field(ServerPlayer owner,ServerLevel level,Vec3 center,long end,Map<BlockPos,BlockState> blocks) { }
    private static final Map<UUID,Field> SIMPLE_FIELDS=new HashMap<>(),BARRIERS=new HashMap<>();
    private static final Map<UUID,Set<UUID>> LOW_ALLIES=new HashMap<>();
    private static final DustParticleOptions BLUE=new DustParticleOptions(new Vector3f(.12f,.48f,1f),1.3f);

    public static boolean unlocked(Player p,int id){return id>=0&&id<NAMES.length&&Curse.data(p).getBoolean(PREFIX+id);}
    public static int count(Player p,int id){return Curse.data(p).getInt(COUNT+id);}
    /** 累计击杀的咒灵数（10 个训练节点共用这一个计数）。 */
    public static int kills(Player p){return Curse.data(p).getInt(KILLS);}
    /**
     * 咒力总量训练给的额外咒力上限：按**已解锁的级数**现算 300/600/900…，不另存累加器，
     * 所以既不会重复发放，也不会因为丢一次写入就少一截。
     */
    public static double energyBonus(Player p){
        double sum=0;
        for(int n=1;n<=TRAIN_COUNT;n++)if(unlocked(p,TRAIN_FIRST+n-1))sum+=TRAIN_ENERGY_PER*n;
        return sum;
    }
    /** 「咒灵」口径：所有 Monster（含模组里的各级咒灵、首领）。玩家、NPC、友方召唤物不算。 */
    public static boolean curseSpirit(LivingEntity e){return e instanceof net.minecraft.world.entity.monster.Monster;}
    /** 记一次击杀并结算训练节点：3/6/9… 逐级点亮，父节点没解锁就停。 */
    private static void addKill(ServerPlayer p,LivingEntity victim){
        if(p instanceof FakePlayer||victim==p||ally(p,victim)||!curseSpirit(victim))return;
        var d=Curse.data(p);int total=d.getInt(KILLS)+1;d.putInt(KILLS,total);
        for(int n=1;n<=TRAIN_COUNT;n++){
            int id=TRAIN_FIRST+n-1;
            if(unlocked(p,id))continue;
            if(total<TRAIN_KILLS_PER*n)break;
            int before=(int)energyBonus(p);
            unlock(p,id);
            int after=(int)energyBonus(p);
            if(after>before)p.sendSystemMessage(Component.literal("咒力总量上限 +"+(after-before)+"（当前 "+after+"）").withStyle(ChatFormatting.AQUA));
        }
        sync(p);
    }
    public static boolean domainUnlocked(Player p){return p instanceof FakePlayer||unlocked(p,DOMAIN);}
    public static boolean requireDomain(ServerPlayer p){if(domainUnlocked(p))return true;p.displayClientMessage(Component.literal("尚未解锁领域展开：先累计使用简易领域 15 次（L 查看进度）"),true);return false;}
    public static boolean domainSkill(int id){return MainlineRules.domainSkill("",id);}
    public static boolean domainSkill(String role,int id){return MainlineRules.domainSkill(role,id);}
    public static boolean domainSkill(Player p,int id){var d=character.CharacterService.selected(p);String role=d==null?"":d.id();
        if(d!=null&&role.equals("yuta")&&d.config().skill(id)==null&&p instanceof ServerPlayer sp&&character.yuta.YutaCopies.has(sp,id))role=character.yuta.YutaCopies.ownerOf(id);
        return domainSkill(role,id);
    }
    public static void unlock(ServerPlayer p,int id){
        if(p instanceof FakePlayer||id<0||id>=NAMES.length||unlocked(p,id)||(PARENT[id]>=0&&!unlocked(p,PARENT[id])))return;
        Curse.data(p).putBoolean(PREFIX+id,true);award(p,id);
        p.sendSystemMessage(Component.literal("【成就解锁】"+NAMES[id]).withStyle(ChatFormatting.GOLD));sync(p);
    }
    private static void award(ServerPlayer p,int id){var a=p.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsu_kaisen_cursed_paths","mainline/"+IDS[id]));if(a!=null)for(String criterion:a.getCriteria().keySet())p.getAdvancements().award(a,criterion);}
    private static void awardCount(ServerPlayer p,int id,int count){var a=p.server.getAdvancements().getAdvancement(new ResourceLocation("jujutsu_kaisen_cursed_paths","mainline/"+IDS[id]));if(a!=null)for(int i=1;i<=count;i++)p.getAdvancements().award(a,"use_"+i);}
    private static void addCount(ServerPlayer p,int id,int cap){Curse.data(p).putInt(COUNT+id,MainlineRules.nextCount(count(p,id),cap));awardCount(p,id==SIMPLE?DOMAIN:AMPLIFY,count(p,id));sync(p);}
    public static void onCharacterChosen(ServerPlayer p){unlock(p,SORCERER);}
    public static int mask(Player p){int m=0;for(int i=0;i<NAMES.length;i++)if(unlocked(p,i))m|=1<<i;return m;}
    public static void sync(ServerPlayer p){if(p instanceof FakePlayer)return;MainlinePackets.send(p,mask(p),count(p,SIMPLE),count(p,DOMAIN),kills(p));}

    public static boolean ally(ServerPlayer p,LivingEntity e){
        if(e==p||e==null||e.isSpectator()||e.level()!=p.level()||CombatSettings.relation(p,e.getUUID())==CombatSettings.ENEMY)return false;
        UUID owner=CombatSafety.ownerId(e);
        return CombatSettings.relation(p,e.getUUID())==CombatSettings.ALLY||p.isAlliedTo(e)
            ||p.getUUID().equals(owner)||owner!=null&&CombatSettings.relation(p,owner)==CombatSettings.ALLY
            ||Legion.allied(p,e)||ShadowSystem.friendly(p,e)||MahoragaAdaptStore.friendlyTo(p,e);
    }
    /** 实际血量判定，越过阈值才抽一次；满血恢复后下一次濒死可再次尝试。 */
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p instanceof FakePlayer)return;
        var d=Curse.data(p);
        if(!p.isAlive()||p.isSpectator()){d.remove("MainlineSurviveTicks");return;}
        if(!unlocked(p,SORCERER))return;
        boolean low=MainlineRules.below(p.getHealth(),p.getMaxHealth(),.05f);
        if(low&&!d.getBoolean("MainlineLowHealth")&&!unlocked(p,REVERSE)&&p.getRandom().nextBoolean())unlock(p,REVERSE);
        d.putBoolean("MainlineLowHealth",low);
        if(unlocked(p,REVERSE)&&!unlocked(p,REVERSE_OUT)){
            Set<UUID> before=LOW_ALLIES.computeIfAbsent(p.getUUID(),k->new HashSet<>()),current=new HashSet<>();
            for(LivingEntity friend:p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(12),t->t.isAlive()&&ally(p,t))){
                if(!MainlineRules.below(friend.getHealth(),friend.getMaxHealth(),.15f))continue;
                current.add(friend.getUUID());
                if(!before.contains(friend.getUUID())&&p.getRandom().nextBoolean())unlock(p,REVERSE_OUT);
            }
            LOW_ALLIES.put(p.getUUID(),current);
        }
        if(!unlocked(p,BARRIER)){
            int ticks=DomainClash.inEnemyDomain(p)?d.getInt("MainlineSurviveTicks")+1:0;
            d.putInt("MainlineSurviveTicks",ticks);if(ticks>=300)unlock(p,BARRIER);
        }
        if(p.tickCount%100==0){for(int i=0;i<NAMES.length;i++)if(unlocked(p,i))award(p,i);awardCount(p,DOMAIN,count(p,SIMPLE));awardCount(p,AMPLIFY,count(p,DOMAIN));sync(p);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void killed(LivingDeathEvent e){
        if(e.isCanceled())return;
        ServerPlayer p=CombatSettings.caster(e.getSource());
        if(p==null&&e.getEntity().getKillCredit() instanceof ServerPlayer credited)p=credited;
        if(p==null||p instanceof FakePlayer)return;
        // 咒力总量训练：任何玩家击杀咒灵都累计（不限角色、不限是否已解锁结界术）
        addKill(p,e.getEntity());
        if(unlocked(p,BARRIER)&&!ally(p,e.getEntity())&&e.getEntity()!=p&&DomainClash.domainCapable(e.getEntity()))unlock(p,SIMPLE);
    }
    public static void onKillDomainOwner(ServerPlayer p){unlock(p,SIMPLE);}
    public static void onDomainOpened(ServerPlayer p){
        if(p instanceof FakePlayer||!unlocked(p,DOMAIN))return;
        addCount(p,DOMAIN,10);if(count(p,DOMAIN)>=10)unlock(p,AMPLIFY);
    }
    private static boolean ready(ServerPlayer p,int id,int cooldown,double cost){
        if(!unlocked(p,id)){p.displayClientMessage(Component.literal("尚未解锁"+NAMES[id]+"（L 查看进度）"),true);return false;}
        if(!p.isAlive()||p.isSpectator()||PrisonSystem.locked(p)||DomainClash.active(p)||SixfoldFinale.active(p)||SixfoldFinale.controlled(p)||DeathStar.anemic(p))return false;
        if(Curse.severed(p))return false;
        if(id!=REVERSE&&id!=REVERSE_OUT&&id!=SIMPLE&&!Curse.canUseTechnique(p))return false;
        String key="MainlineCooldown_"+id;
        if(Curse.active(p,key)){p.displayClientMessage(Component.literal(NAMES[id]+" 冷却中"),true);return false;}
        if(!Curse.spend(p,cost))return false;
        Curse.status(p,key,cooldown);return true;
    }
    public static void reverse(ServerPlayer p){if(unlocked(p,REVERSE))Curse.reverse(p);}
    public static void reverseOut(ServerPlayer p){
        Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(16));
        var wall=p.level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(wall.getType()!=HitResult.Type.MISS)end=wall.getLocation();
        LivingEntity target=null;double best=eye.distanceToSqr(end);final Vec3 rayEnd=end;
        for(var t:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(eye,end).inflate(1),t->t!=p&&t.isAlive()&&!t.isSpectator())){
            var hit=t.getBoundingBox().inflate(.15).clip(eye,rayEnd);
            if(hit.isPresent()&&eye.distanceToSqr(hit.get())<=best){target=t;best=eye.distanceToSqr(hit.get());}
        }
        if(target==null||!ally(p,target)){p.displayClientMessage(Component.literal("请用准星对准 16 格内的友方，不能隔墙治疗"),true);return;}
        if(target.getHealth()>=target.getMaxHealth())return;
        if(!ready(p,REVERSE_OUT,100,100))return;
        target.heal(target.getMaxHealth()*.35f);
        Network.send(p.serverLevel(),new Network.Effect(137,target.getId(),target.position(),Vec3.ZERO,1,20,0));
    }
    public static void barrier(ServerPlayer p){
        if(BARRIERS.containsKey(p.getUUID())||!ready(p,BARRIER,240,80))return;
        var saved=new LinkedHashMap<BlockPos,BlockState>();BlockPos c=p.blockPosition();int r=BARRIER_RADIUS;
        for(int x=-r;x<=r;x++)for(int y=-r;y<=r;y++)for(int z=-r;z<=r;z++){
            int d=x*x+y*y+z*z;if(d>r*r||d<=(r-1)*(r-1))continue;
            BlockPos at=c.offset(x,y,z);if(!p.serverLevel().hasChunkAt(at)||p.level().isOutsideBuildHeight(at))continue;
            BlockState old=p.level().getBlockState(at);if(old.hasBlockEntity()||old.getDestroySpeed(p.level(),at)<0)continue;
            // 不覆盖正在借用的球壳，防止两个结界互相还原错位。
            if(BARRIERS.values().stream().anyMatch(f->f.level==p.level()&&f.blocks.containsKey(at)))continue;
            saved.put(at,old);p.serverLevel().setBlock(at,Blocks.BLACK_CONCRETE.defaultBlockState(),2);
        }
        BARRIERS.put(p.getUUID(),new Field(p,p.serverLevel(),Vec3.atCenterOf(c),p.level().getGameTime()+BARRIER_TICKS,saved));
    }
    public static void simple(ServerPlayer p){
        if(SIMPLE_FIELDS.containsKey(p.getUUID())||!ready(p,SIMPLE,240,60))return;
        SIMPLE_FIELDS.put(p.getUUID(),new Field(p,p.serverLevel(),p.position(),p.level().getGameTime()+SIMPLE_TICKS,Map.of()));
        addCount(p,SIMPLE,15);if(count(p,SIMPLE)>=15)unlock(p,DOMAIN);
    }
    public static boolean simpleProtected(LivingEntity e){
        if(e==null)return false;
        for(Field f:SIMPLE_FIELDS.values())if(f.level==e.level()&&f.owner.isAlive()&&f.end>f.level.getGameTime()){
            Vec3 delta=e.position().subtract(f.center);
            if(MainlineRules.inSimple(delta.x,delta.y,delta.z))return true;
        }
        return false;
    }
    public static void cast(ServerPlayer p,int skill){switch(skill){
        case SKILL_REVERSE->reverse(p);case SKILL_REVERSE_OUT->reverseOut(p);case SKILL_BARRIER->barrier(p);case SKILL_SIMPLE->simple(p);
        case SKILL_AMPLIFY->DomainAmplification.toggle(p);case SKILL_DOMAIN->requireDomain(p);default->{} }
    }
    private static void restore(Field f){if(!f.blocks.isEmpty())character.domain.DomainSafety.evacuate(f.level,new AABB(f.center,f.center).inflate(BARRIER_RADIUS));for(var e:f.blocks.entrySet())if(f.level.getBlockState(e.getKey()).is(Blocks.BLACK_CONCRETE))f.level.setBlock(e.getKey(),e.getValue(),2);}
    @SubscribeEvent public static void fields(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(var map:List.of(BARRIERS,SIMPLE_FIELDS))for(var it=map.values().iterator();it.hasNext();){
            Field f=it.next();if(!f.owner.isAlive()||f.owner.hasDisconnected()||f.owner.level()!=f.level||f.level.getGameTime()>=f.end){restore(f);it.remove();continue;}
            if(map==SIMPLE_FIELDS&&f.level.getGameTime()%4==0)for(int i=0;i<96;i++){
                double a=i*Math.PI*2/96;f.level.sendParticles(BLUE,f.center.x+Math.cos(a)*SIMPLE_RADIUS,f.center.y+.08,f.center.z+Math.sin(a)*SIMPLE_RADIUS,1,0,0,0,0);
            }
        }
    }
    public static void copyShared(CompoundTag from,CompoundTag to){for(String k:new ArrayList<>(to.getAllKeys()))if(k.startsWith("Mainline"))to.remove(k);for(String k:from.getAllKeys())if(k.startsWith("Mainline"))to.put(k,from.get(k).copy());}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){for(int i=0;i<NAMES.length;i++)if(unlocked(p,i))award(p,i);awardCount(p,DOMAIN,count(p,SIMPLE));awardCount(p,AMPLIFY,count(p,DOMAIN));sync(p);}}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){LOW_ALLIES.remove(e.getEntity().getUUID());Curse.data(e.getEntity()).remove("MainlineSurviveTicks");}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void clone(PlayerEvent.Clone e){Curse.data(e.getEntity()).remove("MainlineSurviveTicks");Curse.data(e.getEntity()).remove("MainlineLowHealth");}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){Curse.data(e.getEntity()).remove("MainlineSurviveTicks");}
    @SubscribeEvent public static void stop(ServerStoppingEvent e){BARRIERS.values().forEach(Mainline::restore);BARRIERS.clear();SIMPLE_FIELDS.clear();LOW_ALLIES.clear();}
    private Mainline() { }
}
