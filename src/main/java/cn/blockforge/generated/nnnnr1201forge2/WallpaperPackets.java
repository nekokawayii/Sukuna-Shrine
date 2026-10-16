package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;

public final class WallpaperPackets {
    public static Consumer<Download> receiver=p->{};
    public record Consent(boolean allowed){}
    public record Upload(long token,int index,int count,int total,byte[] data){
        static Upload read(FriendlyByteBuf b){return new Upload(b.readLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readByteArray(WallpaperRules.CHUNK));}
        void write(FriendlyByteBuf b){b.writeLong(token);b.writeVarInt(index);b.writeVarInt(count);b.writeVarInt(total);b.writeByteArray(data);}
    }
    public record Download(UUID owner,long revision,int index,int count,int total,byte[] data){
        static Download read(FriendlyByteBuf b){return new Download(b.readUUID(),b.readLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readByteArray(WallpaperRules.CHUNK));}
        void write(FriendlyByteBuf b){b.writeUUID(owner);b.writeLong(revision);b.writeVarInt(index);b.writeVarInt(count);b.writeVarInt(total);b.writeByteArray(data);}
    }
    public static void register(){
        Network.CHANNEL.messageBuilder(Consent.class,19,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeBoolean(m.allowed())).decoder(b->new Consent(b.readBoolean())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)WallpaperServer.consent(p,m.allowed());c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Upload.class,20,NetworkDirection.PLAY_TO_SERVER).encoder(Upload::write).decoder(Upload::read).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)WallpaperServer.upload(p,m);c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Download.class,21,NetworkDirection.PLAY_TO_CLIENT).encoder(Download::write).decoder(Download::read).consumerMainThread((m,c)->{receiver.accept(m);c.get().setPacketHandled(true);}).add();
    }
    private WallpaperPackets(){}
}
