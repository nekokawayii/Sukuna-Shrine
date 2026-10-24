package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.*;

/** 服务端保存调服和存续令牌；区块卸载不允许绕过唯一式神/五只虾蟇限制。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class ShadowSystem {
    public record Request(int operation,int kind,int count){}
    public record State(int mask,boolean fused,int[] active,int[] seconds){}
    public record Fx(int kind,Vec3 from,Vec3 to,float radius,int ticks){}
    public static java.util.function.Consumer<State> receiver=s->{};
    public static java.util.function.Consumer<Fx> fxReceiver=s->{};
    private static final String BOOK="ShadowActive",TAMED="ShadowTamed",FUSED="ShadowDogsFused";
    public static void register(){
        Network.CHANNEL.messageBuilder(Request.class,38,NetworkDirection.PLAY_TO_SERVER).encoder((s,b)->{b.writeVarInt(s.operation);b.writeVarInt(s.kind);b.writeVarInt(s.count);}).decoder(b->new Request(b.readVarInt(),b.readVarInt(),b.readVarInt())).consumerMainThread((s,c)->{var p=c.get().getSender();if(p!=null)request(p,s);c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(State.class,39,NetworkDirection.PLAY_TO_CLIENT).encoder((s,b)->{b.writeVarInt(s.mask);b.writeBoolean(s.fused);for(int i=0;i<TenShadowRules.COUNT;i++){b.writeVarInt(s.active[i]);b.writeVarInt(s.seconds[i]);}}).decoder(b->{int mask=b.readVarInt();boolean fused=b.readBoolean();int[] n=new int[11],time=new int[11];for(int i=0;i<11;i++){n[i]=b.readVarInt();time[i]=b.readVarInt();}return new State(mask,fused,n,time);}).consumerMainThread((s,c)->{receiver.accept(s);c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Fx.class,40,NetworkDirection.PLAY_TO_CLIENT).encoder((s,b)->{b.writeVarInt(s.kind);b.writeDouble(s.from.x);b.writeDouble(s.from.y);b.writeDouble(s.from.z);b.writeDouble(s.to.x);b.writeDouble(s.to.y);b.writeDouble(s.to.z);b.writeFloat(s.radius);b.writeVarInt(s.ticks);}).decoder(b->new Fx(b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readVarInt())).consumerMainThread((s,c)->{fxReceiver.accept(s);c.get().setPacketHandled(true);}).add();
    }
    public static void fx(ServerLevel level,int kind,Vec3 from,Vec3 to,float radius,int ticks){
        Network.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(from.x,from.y,from.z,160,level.dimension())),new Fx(kind,from,to,radius,ticks));
    }
    public static boolean fused(Player p){return Curse.data(p).getBoolean(FUSED);}
    public static int mask(Player p){return TenShadowRules.mask(Curse.data(p).getInt(TAMED),fused(p));}
    public static boolean tamed(Player p,int kind){return TenShadowRules.valid(kind)&&(mask(p)&(1<<kind))!=0;}
    private static CompoundTag book(Player p){var d=Curse.data(p);if(!d.contains(BOOK))d.put(BOOK,new CompoundTag());return d.getCompound(BOOK);}
    private static void prune(ServerPlayer p){var b=book(p);long now=p.level().getGameTime();for(String key:new ArrayList<>(b.getAllKeys()))if(b.getCompound(key).getLong("End")<=now)b.remove(key);}
    public static boolean authorized(ServerPlayer p,ShadowEntity e){var row=book(p).getCompound(e.getUUID().toString());return row.getLong("End")>p.level().getGameTime();}
    public static void restoreInheritance(ServerPlayer p,ShadowEntity e){var row=book(p).getCompound(e.getUUID().toString());if(TenShadowRules.dog(e.kind())&&fused(p)&&row.getInt("Kind")==TenShadowRules.TOTALITY){e.inheritTotality();enroll(p,e);}}
    private static void enroll(ServerPlayer p,ShadowEntity e){CompoundTag row=new CompoundTag();row.putInt("Kind",e.kind());row.putLong("End",e.expires());book(p).put(e.getUUID().toString(),row);}
    public static void forget(ServerPlayer p,ShadowEntity e){book(p).remove(e.getUUID().toString());}
    public static int count(ServerPlayer p,int kind){prune(p);int n=0;for(String key:book(p).getAllKeys())if(book(p).getCompound(key).getInt("Kind")==kind)n++;return n;}
    public static void sync(ServerPlayer p){
        prune(p);int[] counts=new int[11],seconds=new int[11];long now=p.level().getGameTime();
        for(String key:book(p).getAllKeys()){var row=book(p).getCompound(key);int k=row.getInt("Kind");if(TenShadowRules.valid(k)){counts[k]++;seconds[k]=Math.max(seconds[k],(int)Math.max(0,(row.getLong("End")-now+19)/20));}}
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new State(mask(p),fused(p),counts,seconds));
    }
    private static void tell(ServerPlayer p,String message){p.displayClientMessage(Component.literal(message),true);}
    public static void request(ServerPlayer p,Request request){
        if(PrisonSystem.locked(p)||!TenShadows.incarnated(p)||!p.isAlive()||p.isSpectator())return;
        int op=request.operation,k=request.kind;
        if(op<0||op>6)return;
        long now=p.level().getGameTime();String rate=op==0?"ShadowSyncAfter":"ShadowActionAfter";
        if(p.getPersistentData().getLong(rate)>now)return;p.getPersistentData().putLong(rate,now+(op==0?10:5));
        if(op==0){sync(p);return;}
        if(op==3&&TenShadowRules.valid(k)){recall(p,k);sync(p);return;}
        if(op==4){recallAim(p);sync(p);return;}
        if(op==6){recall(p,-1);MahoragaAdaptStore.recallAll(p);sync(p);return;}
        if(TenShadows.locked(p))return;
        if(op!=2&&(!Curse.canUseTechnique(p)||ObserverRitual.active(p)||Skills.sealing(p)))return;
        if(op==5){if(!fused(p)){summon(p,0,1,false);summon(p,1,1,false);}sync(p);return;}
        if(!TenShadowRules.valid(k)||fused(p)&&TenShadowRules.dog(k)||!fused(p)&&k==2)return;
        if(op==1)summon(p,k,request.count,false);
        if(op==2&&!tamed(p,k))summon(p,k,1,true);
        sync(p);
    }
    public static void summon(ServerPlayer p,int kind,int wanted,boolean ritual){
        if(!ritual&&!tamed(p,kind)){tell(p,"先调服"+TenShadowRules.NAMES[kind]+"，调服仪式不消耗咒力，可反复挑战");return;}
        if(!ritual&&!TenShadowRules.canSummon(mask(p),kind,count(p,kind))){tell(p,TenShadowRules.NAMES[kind]+"已达到同时存在上限");return;}
        int batch=ritual?1:TenShadowRules.batch(kind,wanted),done=0;
        for(int i=0;i<batch;i++){
            if(!ritual&&!Curse.canSpend(p,TenShadowRules.cost(kind)))break;
            ShadowEntity e=ShadowContent.SHIKIGAMI.get().create(p.serverLevel());if(e==null)break;
            e.configure(p,kind,ritual);
            Vec3 at=findSpace(p.serverLevel(),e,p.position(),i);
            if(at==null)break;
            e.moveTo(at.x,at.y,at.z,p.getYRot(),0);
            if(!p.serverLevel().addFreshEntity(e))break;
            if(!ritual){Curse.spend(p,TenShadowRules.cost(kind));enroll(p,e);}
            e.emergeFrom(p.position());done++;
        }
        if(done==0)tell(p,ritual?"附近空间不足，请换个开阔位置":"召唤失败：请检查剩余咒力和周围空间");
        else tell(p,ritual?"调服仪式开始 · 击败"+TenShadowRules.NAMES[kind]+"即可解锁，可反复挑战":TenShadowRules.NAMES[kind]+" × "+done+" 已显现");
    }
    static Vec3 findSpace(ServerLevel level,ShadowEntity entity,Vec3 center,int offset){
        Vec3 original=entity.position();
        try{for(int i=0;i<28;i++){
            double angle=(offset*2.399+i*.91),r=1.3+(i/7)*1.2;
            Vec3 column=center.add(Math.cos(angle)*r,2,Math.sin(angle)*r);
            var ground=level.clip(new ClipContext(column,column.add(0,-7,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity));
            if(ground.getType()!=HitResult.Type.BLOCK)continue;
            Vec3 v=new Vec3(column.x,ground.getLocation().y+.01,column.z);
            entity.setPos(v);if(level.noCollision(entity)&&level.getWorldBorder().isWithinBounds(entity.getBoundingBox()))return v;
        }return null;}finally{entity.setPos(original);}
    }
    public static void recall(ServerPlayer p,int kind){
        var b=book(p);Set<UUID> ids=new HashSet<>();
        for(String key:new ArrayList<>(b.getAllKeys()))if(kind<0||b.getCompound(key).getInt("Kind")==kind){try{ids.add(UUID.fromString(key));}catch(IllegalArgumentException ignored){}b.remove(key);}
        for(ServerLevel level:p.server.getAllLevels())for(UUID id:ids){Entity e=level.getEntity(id);if(e instanceof ShadowEntity shadow&&shadow.ownedBy(p))shadow.dissolve();}
    }
    private static void recallAim(ServerPlayer p){
        Vec3 start=p.getEyePosition(),end=start.add(p.getLookAngle().scale(64));
        Vec3 wall=p.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();
        double closest=start.distanceToSqr(wall)+.01;ShadowEntity found=null;MahoragaEntity maho=null;
        for(LivingEntity e:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().expandTowards(end.subtract(start)).inflate(2),x->x instanceof ShadowEntity s&&s.ownedBy(p)&&!s.ritual()||x instanceof MahoragaEntity m&&m.isAllyOf(p.getUUID()))){
            var hit=e.getBoundingBox().inflate(.25).clip(start,end);if(hit.isPresent()){double d=start.distanceToSqr(hit.get());if(d<closest){closest=d;found=e instanceof ShadowEntity s?s:null;maho=e instanceof MahoragaEntity m?m:null;}}
        }
        if(found!=null){forget(p,found);found.dissolve();}else if(maho!=null)MahoragaAdaptStore.recallSpecific(p,maho);else tell(p,"准星未对准自己的式神（最远64格，不能隔墙）");
    }
    public static boolean friendly(Player owner,LivingEntity target){return target instanceof ShadowEntity s&&!s.ritual()&&s.ownerId()!=null&&s.ownerId().equals(owner.getUUID());}
    public static void defeated(ShadowEntity e){
        if(!(e.owner() instanceof ServerPlayer p))return;
        if(e.ritual()){
            Curse.data(p).putInt(TAMED,mask(p)|(1<<e.kind()));sync(p);tell(p,"调服成功 · "+TenShadowRules.NAMES[e.kind()]+"已解锁");return;
        }
        forget(p,e);
        if(TenShadowRules.dog(e.kind())&&!fused(p)){
            Curse.data(p).putBoolean(FUSED,true);
            var b=book(p);
            for(String key:new ArrayList<>(b.getAllKeys()))if(TenShadowRules.dog(b.getCompound(key).getInt("Kind"))){
                UUID id;try{id=UUID.fromString(key);}catch(IllegalArgumentException ex){b.remove(key);continue;}
                ShadowEntity live=null;for(ServerLevel level:p.server.getAllLevels())if(level.getEntity(id) instanceof ShadowEntity s){live=s;break;}
                if(live!=null&&live.isAlive()){live.inheritTotality();enroll(p,live);}else{var row=b.getCompound(key);row.putInt("Kind",TenShadowRules.TOTALITY);row.putLong("End",p.level().getGameTime()+TenShadowRules.duration(TenShadowRules.TOTALITY));}
            }
            tell(p,"玉犬的力量已继承 · 此后只能召唤玉犬浑");
        }
        sync(p);
    }
    public static LivingEntity target(ShadowEntity e,LivingEntity owner){
        if(e.ritual())return e.validTarget(owner)?owner:null;
        if(owner instanceof ServerPlayer p)return CombatSafety.summonTarget(p,e,24);
        LivingEntity hit=owner.getLastHurtMob();
        if(owner.tickCount-owner.getLastHurtMobTimestamp()<200&&e.validTarget(hit))return hit;
        hit=owner.getLastHurtByMob();if(owner.tickCount-owner.getLastHurtByMobTimestamp()<200&&e.validTarget(hit))return hit;
        var nearby=e.level().getEntitiesOfClass(LivingEntity.class,e.getBoundingBox().inflate(24),x->e.validTarget(x));
        LivingEntity threat=nearby.stream().filter(x->x instanceof Mob m&&m.getTarget()==owner).min(Comparator.comparingDouble(e::distanceToSqr)).orElse(null);if(threat!=null)return threat;
        LivingEntity current=e.getTarget();if(e.validTarget(current)&&e.distanceToSqr(current)<64*64)return current;
        return nearby.stream().filter(x->x instanceof Enemy).min(Comparator.comparingDouble(e::distanceToSqr)).orElse(null);
    }
    @SubscribeEvent public static void inheritAttack(net.minecraftforge.event.entity.living.LivingDamageEvent event){
        ServerPlayer p=CombatSettings.caster(event.getSource());
        if(event.getAmount()>0&&p!=null&&TenShadows.incarnated(p)&&!friendly(p,event.getEntity()))p.setLastHurtMob(event.getEntity());
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer p&&TenShadows.incarnated(p)&&p.tickCount%20==0)sync(p);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)recall(p,-1);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p){recall(p,-1);sync(p);}}
    @SubscribeEvent public static void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer p)recall(p,-1);}
    private ShadowSystem(){}
}
