package character;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.function.Consumer;
import character.toji.TojiCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;

/** 消息只带角色编号和服务端计算后的显示值，客户端不能提交解锁标记或体术余额。 */
public final class CharacterPackets {
    public record Open() {}
    public record Choose(String id) {}
    public record State(String id,double stamina,double maximum,int rank,boolean changed) {}
    public record Physical(int kind) {}
    public static Runnable openReceiver=()->{};
    public static Consumer<State> receiver=s->{};
    public static void register() {
        Network.CHANNEL.messageBuilder(Open.class,41,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{}).decoder(b->new Open()).consumerMainThread((m,c)->{openReceiver.run();c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Choose.class,42,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeUtf(m.id(),48)).decoder(b->new Choose(b.readUtf(48))).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)CharacterService.choose(p,m.id());c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(State.class,43,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeUtf(m.id(),48);b.writeDouble(m.stamina());b.writeDouble(m.maximum());b.writeVarInt(m.rank());b.writeBoolean(m.changed());}).decoder(b->new State(b.readUtf(48),b.readDouble(),b.readDouble(),b.readVarInt(),b.readBoolean())).consumerMainThread((m,c)->{receiver.accept(m);c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Physical.class,44,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeVarInt(m.kind())).decoder(b->new Physical(b.readVarInt())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)TojiCombat.cast(p,m.kind());c.get().setPacketHandled(true);}).add();
    }
    public static void open(ServerPlayer p) { sync(p,false);Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Open()); }
    public static void sync(ServerPlayer p,boolean changed) {
        if(p instanceof net.minecraftforge.common.util.FakePlayer)return;
        var d=CharacterService.selected(p);
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new State(d==null?"":d.id(),TojiCombat.stamina(p),TojiCombat.maximum(p),CharacterService.rank(p),changed));
    }
    private CharacterPackets() {}
}
