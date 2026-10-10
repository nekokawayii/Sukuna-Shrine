package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import java.util.concurrent.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** 服务器只在本次连接内暂存自愿上传的壁纸；按发送者身份归属，分片限速，不落盘。 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class WallpaperServer {
    private record Picture(long revision,byte[] data){}
    private static final class Incoming {
        final long token,born;final int count,total;final byte[] data;int next,offset;
        Incoming(WallpaperPackets.Upload p,long tick){token=p.token();born=tick;count=p.count();total=p.total();data=new byte[total];}
    }
    private static final class Delivery {final UUID owner;final Picture image;int part;Delivery(UUID owner,Picture image){this.owner=owner;this.image=image;}}
    private static final Set<UUID> allowed=new HashSet<>();
    private static final Map<UUID,Long> lastUpload=new HashMap<>(),generation=new HashMap<>();
    private static final Map<UUID,Incoming> incoming=new HashMap<>();
    private static final Map<UUID,Picture> pictures=new HashMap<>();
    private static final Map<UUID,ArrayDeque<Delivery>> outgoing=new HashMap<>();
    private static long tick,revision;
    private static final ExecutorService images=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),r->{Thread t=new Thread(r,"sukuna-wallpaper-server");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    public static void consent(ServerPlayer p,boolean on){
        UUID id=p.getUUID();if(on){if(allowed.size()<128)allowed.add(id);return;}
        allowed.remove(id);incoming.remove(id);generation.put(id,++revision);
        if(pictures.remove(id)!=null){
            outgoing.values().forEach(q->q.removeIf(d->d.owner.equals(id)));
            Network.CHANNEL.send(PacketDistributor.ALL.noArg(),new WallpaperPackets.Download(id,revision,0,0,0,new byte[0]));
        }
    }
    public static void upload(ServerPlayer p,WallpaperPackets.Upload packet){
        UUID id=p.getUUID();if(!allowed.contains(id)||!WallpaperRules.validPart(packet.index(),packet.count(),packet.total(),packet.data().length))return;
        Incoming in=incoming.get(id);
        if(packet.index()==0){
            if(tick-lastUpload.getOrDefault(id,-200L)<100||incoming.size()>=128)return;
            lastUpload.put(id,tick);in=new Incoming(packet,tick);incoming.put(id,in);generation.put(id,++revision);
        }
        if(in==null||in.token!=packet.token()||in.count!=packet.count()||in.total!=packet.total()||in.next!=packet.index()){incoming.remove(id);return;}
        System.arraycopy(packet.data(),0,in.data,in.offset,packet.data().length);in.offset+=packet.data().length;in.next++;
        if(in.next!=in.count)return;incoming.remove(id);byte[] data=in.data;long rev=generation.get(id);var server=p.server;
        try{images.execute(()->{
            byte[] clean;try{clean=WallpaperImage.sanitize(data);}catch(Exception ex){return;}
            server.execute(()->{
                if(server.getPlayerList().getPlayer(id)!=p||!allowed.contains(id)||generation.getOrDefault(id,-1L)!=rev)return;
                if(pictures.size()>=128&&!pictures.containsKey(id))return;
                Picture picture=new Picture(rev,clean);pictures.put(id,picture);
                for(ServerPlayer listener:server.getPlayerList().getPlayers())queue(listener,id,picture);
            });
        });}catch(RejectedExecutionException ignored){}
    }
    private static void queue(ServerPlayer player,UUID owner,Picture image){
        var q=outgoing.computeIfAbsent(player.getUUID(),k->new ArrayDeque<>());q.removeIf(d->d.owner.equals(owner));if(q.size()<128)q.add(new Delivery(owner,image));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)for(var entry:pictures.entrySet())queue(p,entry.getKey(),entry.getValue());}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){consent(p,false);UUID id=p.getUUID();outgoing.remove(id);lastUpload.remove(id);generation.remove(id);}}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;tick++;incoming.values().removeIf(i->tick-i.born>200);
        int budget=16;
        // 队列按轮换顺序推进，单人每刻最多两片，不把大图一次性塞进技能数据包。
        var keys=new ArrayList<>(outgoing.keySet());if(!keys.isEmpty())Collections.rotate(keys,(int)(tick%keys.size()));
        for(UUID id:keys){if(budget<=0)break;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();ServerPlayer p=server==null?null:server.getPlayerList().getPlayer(id);if(p==null){outgoing.remove(id);continue;}
            var q=outgoing.get(id);for(int i=0;i<2&&!q.isEmpty()&&budget>0;i++,budget--){Delivery d=q.peek();byte[] bytes=d.image.data;int count=(bytes.length+WallpaperRules.CHUNK-1)/WallpaperRules.CHUNK,from=d.part*WallpaperRules.CHUNK;
                Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new WallpaperPackets.Download(d.owner,d.image.revision,d.part,count,bytes.length,Arrays.copyOfRange(bytes,from,Math.min(bytes.length,from+WallpaperRules.CHUNK))));
                if(++d.part>=count)q.remove();
            }if(q.isEmpty())outgoing.remove(id);
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e){allowed.clear();incoming.clear();pictures.clear();outgoing.clear();generation.clear();lastUpload.clear();tick=0;}
    private WallpaperServer(){}
}
