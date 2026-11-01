package character.mainline;

import cn.blockforge.generated.nnnnr1201forge2.Network;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

/** 主线进度专用同步，不再占用烟花/甜品特效编号。 */
public final class MainlinePackets {
    /** r217：多带一个「累计击杀咒灵数」，给咒力总量训练的进度用。 */
    public record State(int mask,int simple,int domain,int kills) { }
    public static void register(){
        Network.CHANNEL.messageBuilder(State.class,45,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeVarInt(m.mask());b.writeVarInt(m.simple());b.writeVarInt(m.domain());b.writeVarInt(m.kills());})
            .decoder(b->new State(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt()))
            .consumerMainThread((m,c)->{MainlineState.mask=m.mask();MainlineState.simple=m.simple();MainlineState.domain=m.domain();MainlineState.kills=m.kills();c.get().setPacketHandled(true);}).add();
    }
    public static void send(ServerPlayer p,int mask,int simple,int domain,int kills){Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new State(mask,simple,domain,kills));}
    private MainlinePackets() { }
}
