package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** 每位玩家独立保存，不改镜头方向、不跨墙、不替客户端判定伤害。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class AimAssist {
    public static final int ENABLED=202,ANGLE=203,RANGE=204,CIRCLE=205;
    private static CompoundTag tag(Player p){return Curse.data(p).getCompound("AimAssist");}
    public static boolean enabled(Player p){var t=tag(p);return !t.contains("Enabled")||t.getBoolean("Enabled");}
    public static boolean circle(Player p){return tag(p).getBoolean("Circle");}
    public static double angle(Player p){var t=tag(p);return t.contains("Angle")?AimRules.angle(t.getDouble("Angle")):AimRules.DEFAULT_ANGLE;}
    public static double range(Player p){var t=tag(p);return t.contains("Range")?AimRules.range(t.getDouble("Range")):AimRules.DEFAULT_RANGE;}
    public static Network.AimState state(ServerPlayer p){return new Network.AimState(enabled(p),angle(p),range(p),circle(p));}
    public static void sync(ServerPlayer p){if(!MaidBridge.isCaster(p))Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),state(p));}
    public static void toggle(ServerPlayer p,int field){var t=tag(p);if(field==ENABLED)t.putBoolean("Enabled",!enabled(p));else if(field==CIRCLE)t.putBoolean("Circle",!circle(p));else return;Curse.data(p).put("AimAssist",t);sync(p);}
    public static void set(ServerPlayer p,int field,double value){var t=tag(p);
        if(!Double.isFinite(value)||field==ANGLE&&(value<AimRules.MIN_ANGLE||value>AimRules.MAX_ANGLE)||field==RANGE&&(value<AimRules.MIN_RANGE||value>AimRules.MAX_RANGE))throw new NumberFormatException();
        if(field==ANGLE)t.putDouble("Angle",value);else if(field==RANGE)t.putDouble("Range",value);else return;
        Curse.data(p).put("AimAssist",t);sync(p);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    private AimAssist(){}
}
