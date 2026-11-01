package cn.blockforge.generated.nnnnr1201forge2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
/** 探索积分、图纸和归途坐标属于玩家，不随切换术式角色而清空。 */
public final class ExpeditionProgress {
    public static CompoundTag data(Player p){var root=p.getPersistentData();if(!root.contains("SukunaExpedition"))root.put("SukunaExpedition",new CompoundTag());return root.getCompound("SukunaExpedition");}
    public record State(int scrollMask){}
    public static java.util.function.Consumer<State> receiver=s->{};
    public static void sync(net.minecraft.server.level.ServerPlayer p){
        if(p instanceof net.minecraftforge.common.util.FakePlayer)return;
        var d=data(p);int mask=(d.getBoolean("ExpeditionScroll_furnace_scroll")?1:0)|(d.getBoolean("ExpeditionScroll_ice_scroll")?2:0)|(d.getBoolean("ExpeditionScroll_ultimate_scroll")?4:0);
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new State(mask));
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent e){if(e.phase==net.minecraftforge.event.TickEvent.Phase.END&&e.player instanceof net.minecraft.server.level.ServerPlayer p&&p.tickCount%100==0)sync(p);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){e.getEntity().getPersistentData().put("SukunaExpedition",data(e.getOriginal()).copy());}
    private ExpeditionProgress(){}
}
