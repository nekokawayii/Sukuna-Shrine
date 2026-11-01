package character.mainline;

import cn.blockforge.generated.nnnnr1201forge2.Network;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

/** 主线进度专用同步，不再占用烟花/甜品特效编号。 */
public final class MainlinePackets {
    public record State(int mask,int simple,int domain) { }
    public static void register(){
        Network.CHANNEL.messageBuilder(State.class,45,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeVarInt(m.mask());b.writeVarInt(m.simple());b.writeVarInt(m.domain());})
            .decoder(b->new State(b.readVarInt(),b.readVarInt(),b.readVarInt()))
            .consumerMainThread((m,c)->{MainlineState.mask=m.mask();MainlineState.simple=m.simple();MainlineState.domain=m.domain();c.get().setPacketHandled(true);}).add();
    }
    public static void send(ServerPlayer p,int mask,int simple,int domain){Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new State(mask,simple,domain));}
    private MainlinePackets() { }
}
