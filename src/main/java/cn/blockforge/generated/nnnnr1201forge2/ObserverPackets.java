package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;

/** 高位专用通道消息：会话令牌防止上一次战斗的延迟输入落入下一局。 */
public final class ObserverPackets {
    public record Input(UUID session,int operation,int value,float x,float y) {}
    public record State(UUID session,int boss,int age,int phase,int hits,int shield,int weakId,int weakPart,
                        int weakLeft,int attack,int attackLeft,int staggerLeft,int keysDone,int[] keys,
                        float dodge,String move,int line,float meleeX,float meleeY,net.minecraft.world.phys.Vec3 arena,List<ObserverBattleRules.Blade> blades) {
        static State read(FriendlyByteBuf b){
            UUID session=b.readUUID();int boss=b.readVarInt(),age=b.readVarInt(),phase=b.readVarInt(),hits=b.readVarInt(),shield=b.readVarInt();
            int weakId=b.readVarInt(),part=b.readVarInt(),weak=b.readVarInt(),attack=b.readVarInt(),left=b.readVarInt(),stagger=b.readVarInt(),done=b.readVarInt();
            int length=b.readVarInt();if(length<ObserverBattleRules.QTE_MIN||length>ObserverBattleRules.QTE_MAX)throw new IllegalArgumentException("高位按键数量无效");
            int[] keys=new int[length];for(int i=0;i<length;i++){keys[i]=b.readVarInt();if(keys[i]<65||keys[i]>90)throw new IllegalArgumentException("高位按键字符无效");}
            if(done<0||done>length)throw new IllegalArgumentException("高位按键进度无效");
            float dodge=b.readFloat();String move=b.readUtf(96);int line=b.readVarInt();
            float meleeX=b.readFloat(),meleeY=b.readFloat();var arena=Network.vec(b);int n=b.readVarInt();
            if(n<0||n>6)throw new IllegalArgumentException("高位剑气数量无效");
            List<ObserverBattleRules.Blade> blades=new ArrayList<>();
            for(int i=0;i<n;i++){
                float x=b.readFloat(),y=b.readFloat();int life=b.readVarInt(),style=b.readVarInt(),id=b.readVarInt();float vx=b.readFloat(),vy=b.readFloat();
                if(!Float.isFinite(x+y+vx+vy)||style<0||style>11||life<0||life>200)throw new IllegalArgumentException("高位弹幕无效");
                blades.add(new ObserverBattleRules.Blade(x,y,life,style,id,vx,vy));
            }
            return new State(session,boss,age,phase,hits,shield,weakId,part,weak,attack,left,stagger,done,keys,dodge,move,line,meleeX,meleeY,arena,blades);
        }
        void write(FriendlyByteBuf b){
            b.writeUUID(session);for(int v:new int[]{boss,age,phase,hits,shield,weakId,weakPart,weakLeft,attack,attackLeft,staggerLeft,keysDone})b.writeVarInt(v);
            b.writeVarInt(keys.length);for(int k:keys)b.writeVarInt(k);b.writeFloat(dodge);b.writeUtf(move,96);b.writeVarInt(line);b.writeFloat(meleeX);b.writeFloat(meleeY);Network.vec(b,arena);b.writeVarInt(blades.size());
            for(var blade:blades){b.writeFloat(blade.x());b.writeFloat(blade.y());b.writeVarInt(blade.life());b.writeVarInt(blade.style());b.writeVarInt(blade.id());b.writeFloat(blade.vx());b.writeFloat(blade.vy());}
        }
    }
    public record Cursor(UUID player,String name,float x,float y){}
    public record Cursors(UUID session,List<Cursor> players){
        static Cursors read(FriendlyByteBuf b){UUID session=b.readUUID();int count=b.readVarInt();if(count<0||count>32)throw new IllegalArgumentException("高位同伴数量无效");List<Cursor> out=new ArrayList<>();for(int i=0;i<count;i++){UUID id=b.readUUID();String name=b.readUtf(64);float x=b.readFloat(),y=b.readFloat();if(!AuthorityRules.cursor(x,y))throw new IllegalArgumentException("高位鼠标坐标无效");out.add(new Cursor(id,name,x,y));}return new Cursors(session,List.copyOf(out));}
        void write(FriendlyByteBuf b){b.writeUUID(session);b.writeVarInt(players.size());for(Cursor c:players){b.writeUUID(c.player);b.writeUtf(c.name,64);b.writeFloat(c.x);b.writeFloat(c.y);}}
    }
    public static Consumer<Cursors> cursorReceiver=s->{};
    public static Consumer<State> receiver=s->{};
    public static void register(){
        Network.CHANNEL.messageBuilder(Cursors.class,32,NetworkDirection.PLAY_TO_CLIENT).encoder(Cursors::write).decoder(Cursors::read).consumerMainThread((m,c)->{cursorReceiver.accept(m);c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Input.class,27,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeUUID(m.session);b.writeVarInt(m.operation);b.writeVarInt(m.value);b.writeFloat(m.x);b.writeFloat(m.y);})
            .decoder(b->new Input(b.readUUID(),b.readVarInt(),b.readVarInt(),b.readFloat(),b.readFloat()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)ObserverRitual.input(p,m);c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(State.class,28,NetworkDirection.PLAY_TO_CLIENT).encoder(State::write).decoder(State::read)
            .consumerMainThread((m,c)->{receiver.accept(m);c.get().setPacketHandled(true);}).add();
    }
    private ObserverPackets(){}
}
