package character.domain;

import java.util.*;
import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;

/** 服务端只声明外观与快照，不加载任何客户端渲染类。图片文件由双方资源包提供。 */
public final class DomainPresentation {
    public record Style(String texture,int color,float yaw,float spin,boolean cameraSky) {
        public Style {
            if(texture==null||texture.length()>256||!texture.isEmpty()&&ResourceLocation.tryParse(texture)==null
                ||!Float.isFinite(yaw)||!Float.isFinite(spin))throw new IllegalArgumentException("领域外观参数无效");
        }
        public static Style image(String path,int color){return new Style(path,color,0,0,true);}
        public static Style colored(int color){return new Style("",color,0,0,false);}
    }
    /** progress 是本方进度；normal 与 plane 是两方共同的交界平面。phase：0普通，1准备，2对拼。 */
    public record Frame(UUID owner,Vec3 center,double radius,Style style,int phase,float progress,Vec3 plane,Vec3 normal,int remaining) { }
    public record Snapshot(ResourceLocation dimension,List<Frame> fields) { }
    private static final Map<String,Style> STYLES=new HashMap<>();
    private static final Map<String,Style> DATA_STYLES=new HashMap<>();
    static void reload(Map<String,Style> styles){DATA_STYLES.clear();DATA_STYLES.putAll(styles);}
    public static java.util.function.Consumer<Snapshot> receiver=s->{};
    public static void register(String role,Style style){STYLES.put(Objects.requireNonNull(role),Objects.requireNonNull(style));}
    public static Style style(LivingEntity owner){
        String role=owner instanceof ServerPlayer p&&character.CharacterService.selected(p)!=null
            ?character.CharacterService.selected(p).id():owner instanceof GojoEntity?"gojo":NpcBridge.character(owner);
        var configured=DATA_STYLES.getOrDefault(role,STYLES.get(role));if(configured!=null)return configured;
        var definition=character.CharacterRegistry.get(role);
        int color=definition==null?owner instanceof GuardianEntity?0xE6C56B:owner instanceof YujiEntity?0xEE8899:0x77BBFF:definition.color();
        // r238：石流龙暂时不用晚霞天空图（它由 DomainWorldRenderer 画成包住玩家的球面，
        // 用户实测"球壳还在"就是这层）。改回纯色后不再生成任何球面；要恢复把下面这行换回来即可。
        // if("ryu".equals(role))return Style.image("jujutsu_kaisen_cursed_paths:textures/effect/domain_sunset.png",color);
        if("ryu".equals(role))return Style.colored(color);
        return Style.colored(color);
    }
    /** 内置方块/旧特效领域仅在对拼时补齐可裁切的天空；平时保留原来的专用表现。 */
    public static Style clashStyle(LivingEntity owner,Style fallback){
        String role=owner instanceof ServerPlayer p&&character.CharacterService.selected(p)!=null
            ?character.CharacterService.selected(p).id():owner instanceof GojoEntity?"gojo":NpcBridge.character(owner);
        if(!fallback.texture().isEmpty()||DATA_STYLES.containsKey(role)||STYLES.containsKey(role))return fallback;
        int color=fallback.color();
        return switch(role){
            case "jogo" -> Style.image("jujutsu_kaisen_cursed_paths:textures/effect/domain_jogo/wall_base.png",color);
            case "ryu" -> Style.image("jujutsu_kaisen_cursed_paths:textures/effect/domain_sunset.png",color);
            case "yuki" -> Style.image("jujutsu_kaisen_cursed_paths:textures/block/yuki_star_shell.png",color);
            case "blood" -> new Style("jujutsu_kaisen_cursed_paths:textures/effect/living_blood.png",color,0,.3f,false);
            case "gojo" -> Style.colored(color); // 保留动态特效与分界，不再覆盖 UV 云图。
            default -> Style.colored(color);
        };
    }
    public static void send(ServerPlayer player,List<Frame> frames){
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Snapshot(player.level().dimension().location(),List.copyOf(frames)));
    }
    public static void registerPackets(){
        Network.CHANNEL.messageBuilder(Snapshot.class,46,NetworkDirection.PLAY_TO_CLIENT)
            .encoder(DomainPresentation::write).decoder(DomainPresentation::read)
            .consumerMainThread((s,c)->{receiver.accept(s);c.get().setPacketHandled(true);}).add();
    }
    private static void vec(FriendlyByteBuf b,Vec3 v){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}
    private static Vec3 vec(FriendlyByteBuf b){return new Vec3(b.readDouble(),b.readDouble(),b.readDouble());}
    private static void write(Snapshot s,FriendlyByteBuf b){
        b.writeResourceLocation(s.dimension);b.writeVarInt(s.fields.size());
        for(var f:s.fields){b.writeUUID(f.owner);vec(b,f.center);b.writeDouble(f.radius);
            b.writeUtf(f.style.texture,256);b.writeInt(f.style.color);b.writeFloat(f.style.yaw);b.writeFloat(f.style.spin);b.writeBoolean(f.style.cameraSky);
            b.writeVarInt(f.phase);b.writeFloat(f.progress);vec(b,f.plane);vec(b,f.normal);b.writeVarInt(f.remaining);}
    }
    private static Snapshot read(FriendlyByteBuf b){
        var dimension=b.readResourceLocation();int count=b.readVarInt();
        if(count<0||count>64)throw new IllegalArgumentException("领域快照过多");
        List<Frame> fields=new ArrayList<>();
        for(int i=0;i<count;i++){
            var id=b.readUUID();var center=vec(b);double radius=b.readDouble();
            var style=new Style(b.readUtf(256),b.readInt(),b.readFloat(),b.readFloat(),b.readBoolean());
            int phase=b.readVarInt();float progress=b.readFloat();Vec3 plane=vec(b),normal=vec(b);int remaining=b.readVarInt();
            if(!Double.isFinite(radius)||radius<=0||radius>2048||!Float.isFinite(progress))throw new IllegalArgumentException("领域范围无效");
            fields.add(new Frame(id,center,radius,style,phase,progress,plane,normal,remaining));
        }
        return new Snapshot(dimension,List.copyOf(fields));
    }
    private DomainPresentation(){ }
}
