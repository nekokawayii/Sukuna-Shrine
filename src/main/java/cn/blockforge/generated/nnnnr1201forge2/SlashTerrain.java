package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.*;

/** 服务端权威的局部截断。普通剑气不再先破坏墙体；世界斩不调用此入口。 */
public final class SlashTerrain {
    public static final int SCAR=260;
    public record Sweep(Vec3 a,Vec3 b,Vec3 step){}
    public record Frame(long id,int owner,int batch,int batches,UUID wallpaperOwner,boolean desktopSlashes,List<Sweep> lines){
        static Frame read(FriendlyByteBuf b){
            long id=b.readLong();int owner=b.readVarInt(),batch=b.readVarInt(),batches=b.readVarInt();UUID source=b.readBoolean()?b.readUUID():null;boolean desktop=b.readBoolean();int n=b.readVarInt();
            if(n<0||n>512||batches<1||batches>128||batch<0||batch>=batches)throw new IllegalArgumentException("剑气分段过多");
            List<Sweep> lines=new ArrayList<>();for(int i=0;i<n;i++)lines.add(new Sweep(Network.vec(b),Network.vec(b),Network.vec(b)));
            return new Frame(id,owner,batch,batches,source,desktop,List.copyOf(lines));
        }
        void write(FriendlyByteBuf b){b.writeLong(id);b.writeVarInt(owner);b.writeVarInt(batch);b.writeVarInt(batches);b.writeBoolean(wallpaperOwner!=null);if(wallpaperOwner!=null)b.writeUUID(wallpaperOwner);b.writeBoolean(desktopSlashes);b.writeVarInt(lines.size());for(Sweep s:lines){Network.vec(b,s.a);Network.vec(b,s.b);Network.vec(b,s.step);}}
    }
    public static java.util.function.Consumer<Frame> receiver=f->{};
    private static long next;
    public static long id(){return ++next;}
    public static void register(){Network.CHANNEL.messageBuilder(Frame.class,33,NetworkDirection.PLAY_TO_CLIENT).encoder(Frame::write).decoder(Frame::read).consumerMainThread((m,c)->{receiver.accept(m);c.get().setPacketHandled(true);}).add();}
    public static void send(ServerPlayer owner,long id,List<Sweep> lines,Vec3 at){
        var appearance=new Network.Effect(0,owner.getId(),at,Vec3.ZERO,1,1,0).fromCaster(owner);
        int batches=Math.max(1,(lines.size()+511)/512);
        List<ServerPlayer> audience=owner.serverLevel().players().stream().filter(listener->lines.isEmpty()?listener.position().distanceToSqr(at)<512*512:lines.stream().anyMatch(s->new AABB(s.a,s.b).expandTowards(s.step).distanceToSqr(listener.position())<192*192)).toList();
        // 分批发送，宽墙/手绘图案不触发单包上限；只发给同维度附近玩家。
        for(int batch=0;batch<batches&&batch<128;batch++){
            var part=List.copyOf(lines.subList(batch*512,Math.min(lines.size(),(batch+1)*512)));
            Frame frame=new Frame(id,owner.getId(),batch,Math.min(128,batches),appearance.wallpaperOwner(),appearance.desktopSlashes(),part);
            for(ServerPlayer listener:audience)Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->listener),frame);
        }
    }
    public record Result(List<SlashForms.Line> remaining,List<Sweep> swept){}
    private record CachedShape(net.minecraft.world.level.block.state.BlockState state,List<AABB> boxes){}
    private static final class Cache {long tick;final Map<BlockPos,CachedShape> shapes=new HashMap<>();}
    private static final Map<ServerLevel,Cache> shapeCaches=new WeakHashMap<>();
    private static List<AABB> boxes(ServerLevel level,BlockPos pos){
        Cache cache=shapeCaches.computeIfAbsent(level,k->new Cache());
        if(cache.tick!=level.getGameTime()){cache.tick=level.getGameTime();cache.shapes.clear();}
        var state=level.getBlockState(pos);if(state.isAir())return List.of();
        CachedShape previous=cache.shapes.get(pos);if(previous!=null&&previous.state==state)return previous.boxes;
        List<AABB> boxes=state.getCollisionShape(level,pos).toAabbs().stream().map(b->b.move(pos)).toList();
        if(cache.shapes.size()<65536)cache.shapes.put(pos,new CachedShape(state,boxes));return boxes;
    }
    /** 近身前移出生点也要检查，避免贴墙时剑气直接出生在墙后。 */
    public static List<SlashForms.Line> launch(ServerPlayer owner,List<SlashForms.Line> lines,Vec3 direction){
        List<SlashForms.Line> out=new ArrayList<>();
        for(var line:lines){
            double distance=line.a().lerp(line.b(),.5).subtract(owner.getEyePosition()).dot(direction);
            if(distance>.01&&distance<=4){Vec3 shift=direction.scale(distance);out.addAll(advance(owner,List.of(new SlashForms.Line(line.a().subtract(shift),line.b().subtract(shift))),shift).remaining());}
            else out.add(line);
        }
        return out;
    }
    private record Wall(SlashSweepRules.Contact contact,AABB box,BlockPos pos){}
    private static double[] xyz(Vec3 v){return new double[]{v.x,v.y,v.z};}
    private static SlashSweepRules.Contact contact(Vec3 a,Vec3 b,Vec3 step,AABB box){return SlashSweepRules.contact(xyz(a),xyz(b.subtract(a)),xyz(step),new double[]{box.minX,box.minY,box.minZ},new double[]{box.maxX,box.maxY,box.maxZ});}
    public static boolean hits(Sweep s,LivingEntity target){
        // 使用实体真实盒子，不把墙后的生物膨胀到墙前。
        boolean hit=contact(s.a,s.b,s.step,target.getBoundingBox())!=null;
        if(hit)SlashImpact.plane(target,s.b.subtract(s.a),s.step.lengthSqr()<1e-8?new Vec3(0,0,1):s.step);
        return hit;
    }
    public static Result advance(ServerPlayer owner,List<SlashForms.Line> lines,Vec3 step){
        List<SlashForms.Line> remaining=new ArrayList<>();List<Sweep> swept=new ArrayList<>();
        for(SlashForms.Line line:lines){
            // 分成最多两格的小面以限制斜长剑气的 broadphase 体积；仍精确裁剪，不靠离散点漏检。
            int parts=Math.max(1,(int)Math.ceil(line.a().distanceTo(line.b())/2));
            for(int part=0;part<parts;part++){
                Vec3 a=line.a().lerp(line.b(),part/(double)parts),b=line.a().lerp(line.b(),(part+1)/(double)parts);
                AABB bounds=new AABB(a,b).expandTowards(step).inflate(.001);
                List<Wall> walls=new ArrayList<>();ServerLevel level=owner.serverLevel();
                for(BlockPos mutable:BlockPos.betweenClosed(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ).offset(-1,-1,-1),BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ).offset(1,1,1))){
                    BlockPos pos=mutable.immutable();
                    // 不加载远方区块；遇到未知边界宁可停止，不猜测墙后是空气。
                    if(!level.hasChunkAt(pos)){
                        AABB box=new AABB(pos);var c=contact(a,b,step,box);if(c!=null)walls.add(new Wall(c,box,pos));continue;
                    }
                    for(AABB box:boxes(level,pos)){
                        var c=contact(a,b,step,box);if(c!=null)walls.add(new Wall(c,box,pos));
                    }
                }
                List<SlashSweepRules.Contact> contacts=walls.stream().map(Wall::contact).toList();
                Set<BlockPos> marked=new HashSet<>();
                for(var span:SlashSweepRules.split(contacts)){
                    Vec3 from=a.lerp(b,span.lo()),to=a.lerp(b,span.hi());
                    Vec3 travel=step.scale(Math.max(0,span.time()-(span.blocked()?1e-5:0)));
                    if(span.time()>1e-7||!span.blocked())appendSweep(swept,new Sweep(from,to,travel));
                    if(!span.blocked())append(remaining,new SlashForms.Line(from.add(step),to.add(step)));
                    else for(Wall wall:walls)if(span.lo()<wall.contact.hi()&&span.hi()>wall.contact.lo()&&Math.abs(span.time()-wall.contact.time())<1e-7&&marked.add(wall.pos)){
                        if(level.hasChunkAt(wall.pos))scar(owner,wall,from,to,step);break;
                    }
                }
            }
        }
        return new Result(remaining,swept);
    }
    private static void appendSweep(List<Sweep> out,Sweep sweep){
        if(!out.isEmpty()){
            var old=out.get(out.size()-1);
            if(old.b.distanceToSqr(sweep.a)<1e-12&&old.step.distanceToSqr(sweep.step)<1e-12&&old.b.subtract(old.a).normalize().dot(sweep.b.subtract(sweep.a).normalize())>.999999){out.set(out.size()-1,new Sweep(old.a,sweep.b,sweep.step));return;}
        }
        out.add(sweep);
    }
    private static void append(List<SlashForms.Line> out,SlashForms.Line line){
        if(!out.isEmpty()){
            var old=out.get(out.size()-1);Vec3 d=old.b().subtract(old.a()),next=line.b().subtract(line.a());
            if(old.b().distanceToSqr(line.a())<1e-12&&d.normalize().dot(next.normalize())>.999999){out.set(out.size()-1,new SlashForms.Line(old.a(),line.b()));return;}
        }
        if(out.size()<4096)out.add(line);
    }
    private static double clamp(double n,double a,double b){return Math.max(a,Math.min(b,n));}
    private static Vec3 clamp(Vec3 p,AABB b){return new Vec3(clamp(p.x,b.minX,b.maxX),clamp(p.y,b.minY,b.maxY),clamp(p.z,b.minZ,b.maxZ));}
    private static void scar(ServerPlayer owner,Wall wall,Vec3 a,Vec3 b,Vec3 step){
        Vec3 middle=a.lerp(b,.5),at=wall.box.clip(middle,middle.add(step)).orElse(clamp(middle.add(step.scale(wall.contact.time())),wall.box));
        double[] distance={Math.abs(at.y-wall.box.minY),Math.abs(at.y-wall.box.maxY),Math.abs(at.z-wall.box.minZ),Math.abs(at.z-wall.box.maxZ),Math.abs(at.x-wall.box.minX),Math.abs(at.x-wall.box.maxX)};
        int face=0;for(int i=1;i<6;i++)if(distance[i]<distance[face])face=i;
        Direction side=Direction.from3DDataValue(face);Vec3 normal=Vec3.atLowerCornerOf(side.getNormal()),edge=b.subtract(a);
        Vec3 tangent=edge.subtract(normal.scale(edge.dot(normal)));
        if(tangent.lengthSqr()<1e-8)tangent=step.subtract(normal.scale(step.dot(normal)));
        if(tangent.lengthSqr()<1e-8)return;
        Vec3 half=tangent.normalize().scale(Math.min(1.5,tangent.length())*.5);
        Vec3 from=clamp(at.subtract(half),wall.box).add(normal.scale(.003)),to=clamp(at.add(half),wall.box).add(normal.scale(.003));
        if(from.distanceToSqr(to)<1e-8)return;
        Network.send(owner.serverLevel(),new Network.Effect(SCAR,owner.getId(),from,to.subtract(from),face,200,wall.pos.asLong()));
    }
    private SlashTerrain(){}
}
