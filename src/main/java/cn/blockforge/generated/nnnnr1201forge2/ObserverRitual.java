package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** 高位战服务端：入口、权威战斗状态、断线恢复与胜负结算。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class ObserverRitual {
    public static final int FX_START=250,FX_SHAKE=251,FX_BLACK=252,FX_DIALOG=253,FX_ATTACK=254,FX_LINE=255,FX_END=256,FX_FIGHT=257;
    public static final Vec3 SPACE=new Vec3(.5,400,.5),BOSS_AT=SPACE.add(0,0,-4.5);
    public static final String LINE="原来如此……你一直躲在‘边界’之外看着吗？";
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static final class Party {final UUID id=UUID.randomUUID();final ObserverPartyRules health=new ObserverPartyRules();}
    private static final class Session {
        final ServerPlayer player;final UUID token=UUID.randomUUID();final Vec3 backPos,at;final ResourceKey<Level> backDimension;final Party party;
        final float yaw,pitch;final ObserverBattleRules battle;ObserverEntity boss;int finishAt,castSerial,hurtUntil,nextMove;Vec3 target;
        boolean rewarded;
        float cursorX=.5f,cursorY=.5f;int lastCursor=-1;
        Session(ServerPlayer p,int slot,Party party){
            this.party=party;player=p;backPos=p.position();backDimension=p.level().dimension();yaw=p.getYRot();pitch=p.getXRot();
            at=SPACE.add(slot*128,0,0);target=at;battle=new ObserverBattleRules(token.getLeastSignificantBits(),ObserverLoadout.copy(p));party.health.apply(battle);
        }
    }
    public static boolean active(ServerPlayer p){return sessions.containsKey(p.getUUID());}
    public static boolean start(ServerPlayer p){
        if(!BeyondProgressRules.canStart(p.level().dimension()==VoidJourney.ARENA,BeyondRift.carries(p),BeyondRift.readNote(p),active(p)))return false;
        if(!p.isAlive()||p.isSpectator()||p.isPassenger()||SixfoldFinale.active(p)||DomainClash.active(p))return false;
        if(p.server.getLevel(SoulJourney.CORRIDOR)==null)return false;
        int slot=0;while(occupied(slot))slot++;
        Skills.cancelCombat(p);TechniqueInput.cancel(p);FreehandDrawing.cancel(p);p.stopUsingItem();
        // 同一竞技场、64格内，在开场过场结束前分别使用碎片即加入同场。
        Party party=sessions.values().stream().filter(other->other.backDimension==p.level().dimension()&&other.backPos.distanceToSqr(p.position())<=64*64&&other.battle.age<ObserverBattleRules.INTRO&&other.battle.phase==ObserverBattleRules.INTRO_PHASE).map(other->other.party).findFirst().orElseGet(Party::new);
        Session s=new Session(p,slot,party);sessions.put(p.getUUID(),s);
        CompoundTag d=Curse.data(p);d.putBoolean("ObserverRunning",true);d.putBoolean("ObserverShattered",false);
        d.putBoolean("ObserverOldGravity",p.isNoGravity());d.putBoolean("ObserverOldInvulnerable",p.isInvulnerable());d.putBoolean("ObserverOldInvisible",p.isInvisible());
        if(p.hasEffect(MobEffects.CONFUSION))d.put("ObserverOldConfusion",p.getEffect(MobEffects.CONFUSION).save(new CompoundTag()));else d.remove("ObserverOldConfusion");
        p.setInvulnerable(true);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
        fire(p,FX_START,0);sync(s,0);
        sound(p,SoundEvents.WITHER_SPAWN,.6f,.6f);
        return true;
    }
    private static boolean occupied(int slot){double x=SPACE.x+slot*128;return sessions.values().stream().anyMatch(s->s.at.x==x);}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        Session s=sessions.get(p.getUUID());if(s==null)return;
        if(!p.isAlive()||p.isRemoved()||s.player!=p){finish(s,false,false);return;}
        var b=s.battle;int event=b.tick();
        if(!s.rewarded&&AuthorityRewardRules.isVictory(b.phase)){s.rewarded=true;Authority.reward(p);} // 击碎当刻就把奖励塞进背包
        if(b.age>=31&&p.level().dimension()!=SoulJourney.CORRIDOR){finish(s,false,false);return;}
        p.setDeltaMovement(Vec3.ZERO);p.setNoGravity(true);p.setInvulnerable(true);p.fallDistance=0;
        Vec3 anchor=b.age<=30?s.backPos:s.at.add(0,0,7);
        if(p.position().distanceToSqr(anchor)>.01)p.teleportTo(p.serverLevel(),anchor.x,anchor.y,anchor.z,p.getYRot(),p.getXRot());
        if(b.age<30&&b.age%3==0)p.serverLevel().sendParticles(ParticleTypes.CLOUD,p.getX(),p.getY()-.5,p.getZ(),20,5,.3,5,.1);
        if(b.age==30){enterWhite(s);if(sessions.get(p.getUUID())!=s)return;}
        if(b.age==40)p.addEffect(new MobEffectInstance(MobEffects.CONFUSION,80,0,false,false,false));
        if(b.age==100)p.removeEffect(MobEffects.CONFUSION);
        if(b.age==120){fire(p,FX_DIALOG,0);sound(p,SoundEvents.NOTE_BLOCK_BIT.value(),.35f,.8f);}
        if(b.age==160){fire(p,FX_BLACK,20);sound(p,SoundEvents.NOTE_BLOCK_BIT.value(),.35f,.5f);}
        if(b.age==190){fire(p,FX_LINE,0);p.setInvisible(true);}
        boolean castChanged=b.castSerial!=s.castSerial;
        if(s.boss!=null){
            if(b.phase==1&&b.attackUntil==0&&b.age>=b.staggerUntil){
                if(b.age>=s.nextMove||s.boss.position().distanceToSqr(s.target)<.08){
                    s.target=s.at.add((p.getRandom().nextDouble()-.5)*7,0,(p.getRandom().nextDouble()-.5)*5);
                    s.nextMove=b.age+25+p.getRandom().nextInt(36);
                }
                Vec3 delta=s.target.subtract(s.boss.position());
                if(delta.lengthSqr()>.001)s.boss.setPos(s.boss.position().add(delta.normalize().scale(Math.min(.12,delta.length()))));
            }
            int action=b.age<100?3:b.phase==ObserverBattleRules.SHATTER?6:b.staggerUntil>b.age?5:b.attackUntil>b.age?(b.attack==2?4:1):b.age%12<7?1:2;
            if(b.phase==0&&b.age>=100)action=2;
            if(b.age<s.hurtUntil)action=6;
            s.boss.action(action);
            Vec3 delta=s.at.add(0,b.age>=180?9:2.3,8).subtract(s.boss.getEyePosition());
            float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
            s.boss.setYRot(yaw);s.boss.setYHeadRot(yaw);s.boss.setYBodyRot(yaw);
            s.boss.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
            if(b.castSerial!=s.castSerial){s.castSerial=b.castSerial;
                if(b.attack==ObserverBattleRules.SLASH){fire(p,FX_ATTACK,b.age);sound(p,SoundEvents.PLAYER_ATTACK_SWEEP,.28f,1.25f);}
                else sound(p,SoundEvents.BEACON_ACTIVATE,.35f,.8f);
            }
        }
        if(b.phase==ObserverBattleRules.FAILED&&s.finishAt==0)s.finishAt=b.age+90;
        if(b.age==s.finishAt||b.age>18000){finish(s,false,false);return;}
        // 体术预警在出招当刻立即发包，此后每刻同步，不能浪费 7 刻反应窗口。
        if(event!=0||castChanged||b.attack==ObserverBattleRules.MELEE&&b.attackUntil>b.age||b.age%2==0)sync(s,event);
    }
    private static void enterWhite(Session s){
        ServerPlayer p=s.player;ServerLevel level=p.server.getLevel(SoulJourney.CORRIDOR);
        if(level==null){finish(s,false,false);return;}
        level.getChunkAt(BlockPos.containing(s.at));
        p.teleportTo(level,s.at.x,s.at.y,s.at.z+7,180,0);
        s.boss=Content.OBSERVER.get().create(level);
        if(s.boss==null){finish(s,false,false);return;}
        s.boss.moveTo(s.at.x,s.at.y,s.at.z,0,-40);s.boss.action(3);
        s.boss.getPersistentData().putUUID("HighOwner",p.getUUID());
        if(!level.addFreshEntity(s.boss)){finish(s,false,false);return;}
        sound(p,SoundEvents.GLASS_BREAK,.6f,.6f);sync(s,0);
    }
    public static void input(ServerPlayer p,ObserverPackets.Input input){
        Session s=sessions.get(p.getUUID());if(s==null||!s.token.equals(input.session()))return;
        var b=s.battle;int line=0;
        switch(input.operation()){
            case ObserverBattleRules.CURSOR -> {b.cursor(input.x(),input.y());if(AuthorityRules.cursor(input.x(),input.y())&&s.lastCursor!=b.age){s.cursorX=input.x();s.cursorY=input.y();s.lastCursor=b.age;}}
            case ObserverBattleRules.WEAK -> {
                line=b.strike(input.value());
                if(s.party.health.hit(line==2||line==3))for(Session other:List.copyOf(sessions.values()))if(other.party==s.party){s.party.health.apply(other.battle);if(other.battle.phase==ObserverBattleRules.SHATTER)Curse.data(other.player).putBoolean("ObserverShattered",true);if(other!=s)sync(other,line);}
            }
            case ObserverBattleRules.KEY -> line=b.key(input.value());
            case ObserverBattleRules.DESKTOP -> b.desktop(input.value()==1);
            case ObserverBattleRules.CONTACT -> line=b.contact(input.value(),input.x(),input.y());
            case ObserverBattleRules.WINDOW_MOVE -> b.windowMove(input.x(),input.y());
            case ObserverBattleRules.RESET -> {reset(p);return;}
            case ObserverBattleRules.ABORT -> {finish(s,false,false);return;}
            default -> {return;}
        }
        if(b.phase==ObserverBattleRules.SHATTER)Curse.data(p).putBoolean("ObserverShattered",true);
        if(!s.rewarded&&AuthorityRewardRules.isVictory(b.phase)){s.rewarded=true;Authority.reward(p);}
        if(line!=0){if(line==2||line==3){s.hurtUntil=b.age+9;if(s.boss!=null)s.boss.action(6);}sync(s,line);}
    }
    private static void sync(Session s,int line){
        var b=s.battle;
        List<ObserverPackets.Cursor> cursors=new ArrayList<>();for(Session other:sessions.values())if(other.party==s.party&&other!=s&&cursors.size()<32)cursors.add(new ObserverPackets.Cursor(other.player.getUUID(),other.player.getScoreboardName(),other.cursorX,other.cursorY));
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->s.player),new ObserverPackets.Cursors(s.token,List.copyOf(cursors)));
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->s.player),new ObserverPackets.State(s.token,s.boss==null?-1:s.boss.getId(),b.age,b.phase,b.hits,b.shield(),b.weakId,b.weakPart,b.weak()?b.weakUntil-b.age:0,b.attack,Math.max(0,b.attackUntil-b.age),Math.max(0,b.staggerUntil-b.age),b.keysDone,b.keys.clone(),b.dodgeProgress(),b.move,line,b.meleeX,b.meleeY,s.at,List.copyOf(b.blades)));
    }
    private static void restore(ServerPlayer p){
        var d=Curse.data(p);p.setNoGravity(d.getBoolean("ObserverOldGravity"));p.setInvulnerable(d.getBoolean("ObserverOldInvulnerable"));p.setInvisible(d.getBoolean("ObserverOldInvisible"));
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.removeEffect(MobEffects.CONFUSION);
        if(d.contains("ObserverOldConfusion")){var effect=MobEffectInstance.load(d.getCompound("ObserverOldConfusion"));if(effect!=null)p.addEffect(effect);d.remove("ObserverOldConfusion");}
        d.putBoolean("ObserverRunning",false);d.putBoolean("ObserverShattered",false);
    }
    private static void spawn(ServerPlayer p){var over=p.server.overworld();var at=over.getSharedSpawnPos();p.teleportTo(over,at.getX()+.5,at.getY(),at.getZ()+.5,0,0);}
    private static void finish(Session s,boolean victory,boolean toSpawn){
        ServerPlayer p=s.player;sessions.remove(p.getUUID());if(s.boss!=null)s.boss.discard();
        restore(p);
        if(victory)Authority.reward(p);
        Curse.data(p).putBoolean("ObserverSeen",true);
        if(toSpawn)spawn(p);else{var back=p.server.getLevel(s.backDimension);if(back!=null)p.teleportTo(back,s.backPos.x,s.backPos.y,s.backPos.z,s.yaw,s.pitch);else spawn(p);}
        fire(p,FX_END,victory?1:0);
    }
    public static void leave(ServerPlayer p){var s=sessions.get(p.getUUID());if(s!=null)finish(s,false,false);}
    public static void reset(ServerPlayer p){
        var s=sessions.get(p.getUUID());
        if(s==null||s.battle.phase!=ObserverBattleRules.SHATTER)return;
        finish(s,true,true);
    }
    @SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent e){
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("sukuna").then(net.minecraft.commands.Commands.literal("reset").requires(source->source.getEntity() instanceof ServerPlayer player&&active(player)).executes(c->{reset(c.getSource().getPlayerOrException());return 1;})));
        // 已通关但「权限」确实丢了（掉进虚空缝隙等）时的补救，不重复发放。
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("sukuna").then(net.minecraft.commands.Commands.literal("permission").executes(c->{Authority.restoreReward(c.getSource().getPlayerOrException());return 1;})));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!Curse.data(p).getBoolean("ObserverRunning"))return;
        boolean won=Curse.data(p).getBoolean("ObserverShattered");restore(p);if(won)Authority.reward(p);spawn(p);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        var s=sessions.remove(p.getUUID());if(s==null)return;if(s.boss!=null)s.boss.discard();
        // 保留 Running 标记：下次登录恢复原属性并回主世界；只有真正击碎后退出才通关。
        Curse.data(p).putBoolean("ObserverShattered",s.battle.phase==ObserverBattleRules.SHATTER);
    }
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){sessions.clear();}
    private static void sound(ServerPlayer p,SoundEvent sound,float volume,float pitch){p.playNotifySound(sound,SoundSource.MASTER,volume,pitch);}
    private static void fire(ServerPlayer p,int kind,int value){Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Network.Effect(kind,p.getId(),p.position(),Vec3.ZERO,0,value,value));}
    private ObserverRitual(){}
}
