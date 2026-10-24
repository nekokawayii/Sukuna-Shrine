package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.*;

/** 只同步小型目录和实体编号，军队完整存档不离开服务端。 */
public final class AuthorityPackets {
    public record State(boolean enabled,int members,int stored,int[] targets,int[] skills){}
    public record Open(){}
    public record Action(int operation,int value,int extra){}
    public static final int SELECT=0,STRIKE=1,INSCRIBE=2,RECALL=3,RELEASE=4,CLEAR=5,REPLACE_TOOLS=6;
    public static Consumer<State> receiver=s->{};
    public static Runnable openReceiver=()->{};
    private static int[] array(FriendlyByteBuf b,int max){int n=b.readVarInt();if(n<0||n>max)throw new IllegalArgumentException("权限目录超限");int[] out=new int[n];for(int i=0;i<n;i++)out[i]=b.readVarInt();return out;}
    private static void array(FriendlyByteBuf b,int[] values){b.writeVarInt(values.length);for(int i:values)b.writeVarInt(i);}
    public static void sync(ServerPlayer p){if(p instanceof net.minecraftforge.common.util.FakePlayer)return;boolean enabled=Authority.unlocked(p);Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new State(enabled,enabled?Legion.count(p):0,enabled?Legion.stored(p):0,enabled?Legion.targetIds(p).stream().mapToInt(Integer::intValue).toArray():new int[0],enabled?AuthoritySkills.owned(p).stream().mapToInt(Integer::intValue).toArray():new int[0]));}
    public static void open(ServerPlayer p){if(!Authority.unlocked(p)||ObserverRitual.active(p))return;sync(p);Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Open());}
    public static void send(int op,int value,int extra){Network.CHANNEL.sendToServer(new Action(op,value,extra));}
    private static void handle(ServerPlayer p,Action m){
        if(p==null||!Authority.unlocked(p)||!p.isAlive()||p.isSpectator()||ObserverRitual.active(p))return;
        boolean editor=Authority.editorHeld(p);
        switch(m.operation){
            case SELECT->{} // 旧版左键包不再选目标；标记只接受道具的服务端右键判定。
            case STRIKE->{if(editor&&p.serverLevel().getEntity(m.value) instanceof LivingEntity t)AuthorityEditor.strike(p,t);}
            case INSCRIBE->{if(editor&&!Curse.active(p,"AuthorityMenuCooldown")){Curse.status(p,"AuthorityMenuCooldown",5);Authority.inscribe(p,m.value,m.extra);}}
            case RECALL,RELEASE->{if(editor&&!Curse.active(p,"AuthorityMenuCooldown")){Curse.status(p,"AuthorityMenuCooldown",10);if(m.operation==RECALL)Legion.storeAll(p);else Legion.releaseAll(p);}}
            case CLEAR->{if(editor)Legion.clearTargets(p);}
            case REPLACE_TOOLS->{if(editor&&!Curse.active(p,"AuthorityMenuCooldown")){Curse.status(p,"AuthorityMenuCooldown",20);for(var item:List.of(Authority.CURSE.get(),Authority.BANNER.get()))if(p.getInventory().countItem(item)==0)Journey.give(p,new net.minecraft.world.item.ItemStack(item));Curse.data(p).putBoolean("AuthorityMarkerDelivered",false);Authority.settleMarker(p);}}
            default->{}
        }
    }
    public static void register(){
        Network.CHANNEL.messageBuilder(State.class,29,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeBoolean(m.enabled);b.writeVarInt(m.members);b.writeVarInt(m.stored);array(b,m.targets);array(b,m.skills);}).decoder(b->new State(b.readBoolean(),b.readVarInt(),b.readVarInt(),array(b,32),array(b,128))).consumerMainThread((m,c)->{receiver.accept(m);c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Open.class,30,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{}).decoder(b->new Open()).consumerMainThread((m,c)->{openReceiver.run();c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Action.class,31,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeVarInt(m.operation);b.writeVarInt(m.value);b.writeVarInt(m.extra);}).decoder(b->new Action(b.readVarInt(),b.readVarInt(),b.readVarInt())).consumerMainThread((m,c)->{handle(c.get().getSender(),m);c.get().setPacketHandled(true);}).add();
    }
    private AuthorityPackets(){}
}
