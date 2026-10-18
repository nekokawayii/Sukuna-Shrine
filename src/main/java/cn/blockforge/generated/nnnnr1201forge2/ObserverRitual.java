package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * 第三阶段「观测者」的服务端编排：虚空竞技场用碎片 → 场地坍塌 → 纯白空间 →
 * 黑屏 + 系统错误窗口 → 假玩家（观测者）盯着镜头说话 → 对着镜头挥刀（客户端裂屏）。
 * 纯白空间复用灵魂通道维度的 y=400 空域（那里没有任何方块），白色由客户端画，不新增维度。
 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class ObserverRitual {
    public static final int FX_START=250,FX_SHAKE=251,FX_BLACK=252,FX_DIALOG=253,FX_ATTACK=254,FX_LINE=255,FX_END=256,FX_FIGHT=257;
    public static final Vec3 SPACE=new Vec3(0.5,400,0.5);
    /** 观测者站位：玩家正前方 4.5 格（第三人称镜头在玩家背后，所以他正对镜头）。 */
    public static final Vec3 BOSS_AT=SPACE.add(0,0,-4.5);
    public static final String LINE="原来如此……你一直躲在「边界」之外看着吗？";
    private static final String SEEN="ObserverSeen";
    private static final int[] ATTACK_AT={300,344,388};
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static final class Session {
        final ServerPlayer player;final ResourceKey<Level> backDimension;final Vec3 backPos,arenaAnchor;final float backYaw,backPitch;
        ObserverEntity boss;int tick,swing;
        Session(ServerPlayer p){player=p;backDimension=p.level().dimension();backPos=p.position();arenaAnchor=p.position();backYaw=p.getYRot();backPitch=p.getXRot();}
    }

    public static boolean active(ServerPlayer p){return sessions.containsKey(p.getUUID());}

    /** 在虚空竞技场里使用「不属于这个世界的碎片」时触发。 */
    public static boolean start(ServerPlayer p){
        if(p.level().dimension()!=VoidJourney.ARENA||sessions.containsKey(p.getUUID()))return false;
        Session s=new Session(p);sessions.put(p.getUUID(),s);Curse.data(p).putBoolean("ObserverRunning",true);
        p.setInvulnerable(true);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,700,6,false,false,false));
        fire(p,SPACE,FX_SHAKE,45,0);
        p.serverLevel().playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.WITHER_SPAWN,SoundSource.PLAYERS,1.6f,0.55f);
        return true;
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        Session s=sessions.get(p.getUUID());
        if(s==null||s.player!=p)return;
        s.tick++;
        p.setDeltaMovement(Vec3.ZERO);
        keep(p,s.tick<30?s.arenaAnchor:SPACE);
        switch(s.tick){
            case 30 -> enterWhite(s);
            case 40 -> {
                fire(p,SPACE,FX_SHAKE,30,0);
                p.serverLevel().playSound(null,SPACE.x,SPACE.y,SPACE.z,SoundEvents.WARDEN_ROAR,SoundSource.PLAYERS,1.4f,0.7f);
            }
            case 70 -> {
                fire(p,SPACE,FX_SHAKE,24,0);
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION,240,2,false,false,false));
                fire(p,SPACE,FX_BLACK,26,0);
                p.serverLevel().playSound(null,SPACE.x,SPACE.y,SPACE.z,SoundEvents.ELDER_GUARDIAN_CURSE,SoundSource.PLAYERS,1.5f,0.6f);
            }
            case 96 -> fire(p,SPACE,FX_DIALOG,40,0);
            case 200 -> {if(s.boss!=null)s.boss.action(2);}
            case 236 -> fire(p,SPACE,FX_LINE,240,0);
            default -> {}
        }
        // 300 刻起进入战斗：之后血量/护盾/QTE 全部由客户端 ObserverFight 驱动，服务端只负责冻结与收尾。
        if(s.tick==300){fire(p,SPACE,FX_FIGHT,0,0);if(s.boss!=null)s.boss.action(2);}
        if(s.boss!=null){
            // 每刻把观测者的脑袋对准"第三人称镜头"：玩家眼位沿视线后退 4 格
            Vec3 camera=p.getEyePosition().subtract(p.getLookAngle().scale(4));
            Vec3 from=s.boss.getEyePosition(),delta=camera.subtract(from);
            float yaw=(float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90);
            float pitch=(float)(-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
            s.boss.setYRot(yaw);s.boss.setYHeadRot(yaw);s.boss.setYBodyRot(yaw);s.boss.setXRot(pitch);
            if(s.swing>0&&--s.swing==0)s.boss.action(2);
        }
        if(s.tick>=12000)finish(s,false);   // 10 分钟兜底，正常打完由客户端 /sukuna reset 收尾
    }

    private static void keep(ServerPlayer p,Vec3 at){
        if(p.position().distanceToSqr(at)>0.36)p.teleportTo(p.serverLevel(),at.x,at.y,at.z,p.getYRot(),p.getXRot());
    }

    private static void enterWhite(Session s){
        ServerPlayer p=s.player;
        ServerLevel level=p.server.getLevel(SoulJourney.CORRIDOR);
        if(level==null){finish(s,false);return;}
        level.getChunkAt(new BlockPos((int)SPACE.x,(int)SPACE.y,(int)SPACE.z));
        p.teleportTo(level,SPACE.x,SPACE.y,SPACE.z,180,0);
        p.setNoGravity(true);p.setInvulnerable(true);p.setDeltaMovement(Vec3.ZERO);
        fire(p,SPACE,FX_START,0,0);
        ObserverEntity boss=Content.OBSERVER.get().create(level);
        if(boss!=null){
            boss.moveTo(BOSS_AT.x,BOSS_AT.y,BOSS_AT.z,0,0);
            boss.setNoAi(true);boss.setInvulnerable(true);boss.setNoGravity(true);
            level.addFreshEntity(boss);s.boss=boss;
        }
        level.playSound(null,SPACE.x,SPACE.y,SPACE.z,SoundEvents.GLASS_BREAK,SoundSource.PLAYERS,2f,0.4f);
    }

    /** 收尾：把玩家送回竞技场，恢复属性，记下"见过观测者"。 */
    private static void finish(Session s,boolean toSpawn){
        ServerPlayer p=s.player;sessions.remove(p.getUUID());
        if(s.boss!=null){s.boss.discard();s.boss=null;}
        if(toSpawn){
            ServerLevel over=p.server.overworld();
            var at=over.getSharedSpawnPos();
            p.teleportTo(over,at.getX()+0.5,at.getY(),at.getZ()+0.5,0,0);
        }else{
            ServerLevel back=p.server.getLevel(s.backDimension);
            if(back!=null)p.teleportTo(back,s.backPos.x,s.backPos.y,s.backPos.z,s.backYaw,s.backPitch);
        }
        p.setNoGravity(false);p.setInvulnerable(false);p.setDeltaMovement(Vec3.ZERO);
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);p.removeEffect(MobEffects.CONFUSION);
        Curse.data(p).putBoolean(SEEN,true);Curse.data(p).putBoolean("ObserverCleared",true);Curse.data(p).putBoolean("ObserverRunning",false);
        fire(p,p.position(),FX_END,0,0);
    }
    public static void leave(ServerPlayer p){Session s=sessions.get(p.getUUID());if(s!=null)finish(s,false);}


    /** 「上帝收回控制权」：/sukuna reset 或强退后再进游戏，都送回主世界默认出生点。 */
    public static void reset(ServerPlayer p){
        Session s=sessions.get(p.getUUID());
        if(s!=null){finish(s,true);return;}
        ServerLevel over=p.server.overworld();var at=over.getSharedSpawnPos();
        p.teleportTo(over,at.getX()+0.5,at.getY(),at.getZ()+0.5,0,0);
        p.setNoGravity(false);p.setInvulnerable(false);
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);p.removeEffect(MobEffects.CONFUSION);
        Curse.data(p).putBoolean("ObserverRunning",false);Curse.data(p).putBoolean("ObserverCleared",true);
        fire(p,p.position(),FX_END,0,0);
    }

    @SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent e){
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("sukuna").then(net.minecraft.commands.Commands.literal("reset").executes(c->{
            ServerPlayer p=c.getSource().getPlayerOrException();
            reset(p);
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("世界已收回。"));
            return 1;})));
    }

    /** 上一局是崩端/强退走的：进游戏就送回主世界默认出生点。 */
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        if(!Curse.data(p).getBoolean("ObserverRunning"))return;
        ServerLevel over=p.server.overworld();var at=over.getSharedSpawnPos();
        p.teleportTo(over,at.getX()+0.5,at.getY(),at.getZ()+0.5,0,0);
        p.setNoGravity(false);p.setInvulnerable(false);
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);p.removeEffect(MobEffects.CONFUSION);
        Curse.data(p).putBoolean("ObserverRunning",false);Curse.data(p).putBoolean("ObserverCleared",true);
    }

    /** 直接关掉游戏也算"上帝收回控制权"：断开时按胜利结算，下次进来在出生点。 */
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        Session s=sessions.remove(p.getUUID());
        if(s==null)return;
        if(s.boss!=null)s.boss.discard();
        Curse.data(p).putBoolean("ObserverRunning",false);Curse.data(p).putBoolean("ObserverCleared",true);
    }

    /** 效果包只发给当事人（位置先换维度时也能收到）。 */
    private static void fire(ServerPlayer p,Vec3 pos,int kind,int life,long seed){
        Network.Effect effect=new Network.Effect(kind,p.getId(),pos,Vec3.ZERO,0,life,seed);
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),effect);
    }
    private ObserverRitual(){}
}
