package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import java.util.function.Consumer;
import character.domain.DomainSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.network.*;

/** 每人仅一个会话、一个物理首端；延伸分段查询已加载区块，不生成成千上万的链节实体。 */
public final class ThousandMileChain {
    public static final int ANCHOR = 0, EXTENDING = 1, BOUND = 2, STOPPED = 3, RETURNING = 4, END = 5;
    private static final UUID NONE = new UUID(0, 0);
    private static final UUID ROOT = UUID.fromString("c564dd18-0345-45a3-b1ca-cbb5d5310683");
    private static final String GRAVITY = "ThousandMilePreviousGravity";
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Session> CAPTIVES = new HashMap<>();
    public static Consumer<Snapshot> receiver = s -> {};

    /** 客户端仅提交本人的镜头参数；首端坐标、目标和命中结果由服务端掌握。 */
    public record Press(UUID token, Vec3 camera, Vec3 forward, double fov, double aspect, boolean cancel) {
        static Press read(FriendlyByteBuf b) { return new Press(b.readUUID(), Network.vec(b), Network.vec(b), b.readDouble(), b.readDouble(), b.readBoolean()); }
        void write(FriendlyByteBuf b) { b.writeUUID(token); Network.vec(b,camera); Network.vec(b,forward); b.writeDouble(fov); b.writeDouble(aspect); b.writeBoolean(cancel); }
    }
    public record Snapshot(UUID owner, UUID token, ResourceLocation dimension, int phase, Vec3 hand, Vec3 anchor, Vec3 tip, int target, float width, float height, boolean armed) {
        static Snapshot read(FriendlyByteBuf b) { return new Snapshot(b.readUUID(),b.readUUID(),b.readResourceLocation(),b.readVarInt(),Network.vec(b),Network.vec(b),Network.vec(b),b.readInt(),b.readFloat(),b.readFloat(),b.readBoolean()); }
        void write(FriendlyByteBuf b) { b.writeUUID(owner);b.writeUUID(token);b.writeResourceLocation(dimension);b.writeVarInt(phase);Network.vec(b,hand);Network.vec(b,anchor);Network.vec(b,tip);b.writeInt(target);b.writeFloat(width);b.writeFloat(height);b.writeBoolean(armed); }
    }
    public record Configure() {}
    private static final class Session {
        final ServerPlayer owner;
        final ServerLevel level;
        final UUID token = UUID.randomUUID();
        final UUID itemId;
        final boolean armed;
        Vec3 anchor, velocity, tip, direction = Vec3.ZERO, pin;
        long boundAt;
        LivingEntity target;
        int phase = ANCHOR, returnTicks;
        long nextPress;
        boolean landed;
        final Set<UUID> viewers = new HashSet<>();
        Session(ServerPlayer p, UUID itemId) {
            owner=p;level=p.serverLevel();this.itemId=itemId;armed=ThousandMileChainItem.armed(p.getMainHandItem());
            anchor=p.getEyePosition().add(p.getLookAngle().scale(.5));tip=anchor;
            velocity=p.getLookAngle().scale(.7).add(0,.12,0);nextPress=level.getGameTime()+4;
        }
    }
    public static void register() {
        Network.CHANNEL.messageBuilder(Press.class,50,NetworkDirection.PLAY_TO_SERVER).encoder(Press::write).decoder(Press::read)
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)press(p,m);c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Snapshot.class,51,NetworkDirection.PLAY_TO_CLIENT).encoder(Snapshot::write).decoder(Snapshot::read)
            .consumerMainThread((m,c)->{receiver.accept(m);c.get().setPacketHandled(true);}).add();
        Network.CHANNEL.messageBuilder(Configure.class,52,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{}).decoder(b->new Configure())
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)ThousandMileChainItem.configure(p);c.get().setPacketHandled(true);}).add();
    }
    public static boolean active(Player p) { return SESSIONS.containsKey(p.getUUID()); }
    public static boolean bound(Entity e) { return CAPTIVES.containsKey(e.getUUID()); }
    private static boolean allowed(ServerPlayer p) {
        return p.isAlive()&&!p.isRemoved()&&!p.hasDisconnected()&&!p.isSpectator()&&!p.isSleeping()
            &&!PrisonSystem.locked(p)&&!bound(p)&&!ObserverRitual.active(p)&&!TenShadows.locked(p)
            &&!DomainClash.active(p)&&!SixfoldFinale.controlled(p)&&!NetBranches.locked(p)
            &&!Uraume.locked(p)&&!CombatExpansion.controlled(p)&&!GojoEntity.mindLocked(p);
    }
    private static void press(ServerPlayer p, Press m) {
        if (!ThousandMileChainItem.isChain(p.getMainHandItem())) return;
        Session s=SESSIONS.get(p.getUUID());
        if (s!=null && !valid(s)) { finish(s);return; }
        if (s!=null && p.level().getGameTime()<s.nextPress) return;
        if (!allowed(p)) return;
        if (s==null) {
            if(m.cancel()||!m.token().equals(NONE))return;
            ItemStack stack=p.getMainHandItem();
            if(stack.getCount()!=1){tell(p,"请先把叠放的万里之锁拆成单条");return;}
            UUID itemId=UUID.randomUUID();stack.getOrCreateTag().putUUID("ThousandMileId",itemId);
            s=new Session(p,itemId);SESSIONS.put(p.getUUID(),s);MeleeCombo.cancel(p);
            sound(s);sync(s);tell(p,"首端已抛下 · 转开视线或用墙遮住它，再左键延伸");return;
        }
        if(!s.token.equals(m.token()))return;
        s.nextPress=s.level.getGameTime()+4;
        if(m.cancel()||s.phase!=ANCHOR){recall(s);return;}
        if(!validCamera(p,m)){tell(p,"镜头尚未同步，请再按一次左键");return;}
        if(visible(s,m)){tell(p,"首端还在你的视野内，不能无限延伸；潜行左键可直接收回");return;}
        s.phase=EXTENDING;s.tip=p.getEyePosition();s.direction=p.getLookAngle().normalize();
        sound(s);sync(s);tell(p,"万里之锁直线延伸 · 命中缠绕，再按左键收回");
    }
    private static boolean finite(Vec3 v){return Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z);}
    private static boolean validCamera(ServerPlayer p,Press m) {
        if(!finite(m.camera())||!finite(m.forward())||!Double.isFinite(m.fov())||!Double.isFinite(m.aspect()))return false;
        double dot=m.forward().normalize().dot(p.getLookAngle());
        // 支持第一人称、背面第三人称和正面第三人称；允许一小段网络旋转延迟。
        return m.camera().distanceToSqr(p.getEyePosition())<=49 && Math.abs(dot)>.75
            &&m.forward().lengthSqr()>.9&&m.forward().lengthSqr()<1.1&&m.fov()>=1&&m.fov()<=175&&m.aspect()>=.2&&m.aspect()<=8;
    }
    private static boolean visible(Session s,Press m) {
        // 与客户端的首端绘制距离一致，也为遮挡查询设定固定工作上限。
        if(s.anchor.distanceToSqr(m.camera())>80*80)return false;
        Vec3 f=m.forward().normalize();Vec3 right=f.cross(new Vec3(0,1,0)).normalize();
        if(right.lengthSqr()<.01)right=new Vec3(1,0,0);
        Vec3 up=right.cross(f).normalize();double tan=Math.tan(Math.toRadians(m.fov()/2));
        // 首端占有体积，边缘露出也算看见；只读发起者镜头，不枚举其他玩家视线。
        for(int i=0;i<9;i++) {
            Vec3 point=i==0?s.anchor:s.anchor.add((i&1)==0?-.13:.13,(i&2)==0?-.13:.13,(i&4)==0?-.13:.13);
            Vec3 delta=point.subtract(m.camera());double depth=delta.dot(f);
            if(depth<=0||Math.abs(delta.dot(right))>depth*tan*m.aspect()+.02||Math.abs(delta.dot(up))>depth*tan+.02)continue;
            // 分段检查，未加载地形不请求生成，也不把未知地形当作透明。
            Vec3 from=m.camera();int steps=(int)Math.ceil(delta.length()/4);boolean clear=true;
            for(int step=1;step<=steps;step++) {
                Vec3 to=m.camera().lerp(point,step/(double)steps);
                if(!loaded(s.level,from,to)){clear=false;break;}
                var wall=clip(s,from,to);
                if(wall.getType()!=HitResult.Type.MISS&&wall.getLocation().distanceToSqr(point)>.025){clear=false;break;}
                from=to;
            }
            if(clear)return true;
        }
        return false;
    }
    private static boolean valid(Session s) {
        ItemStack stack=s.owner.getMainHandItem();
        return s.owner.level()==s.level&&s.owner.isAlive()&&!s.owner.isRemoved()&&!s.owner.hasDisconnected()&&!s.owner.isSpectator()
            &&ThousandMileChainItem.isChain(stack)&&stack.getCount()==1&&stack.hasTag()&&stack.getTag().hasUUID("ThousandMileId")
            &&s.itemId.equals(stack.getTag().getUUID("ThousandMileId"))&&!PrisonSystem.locked(s.owner);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        for(Session s:List.copyOf(SESSIONS.values())) {
            if(!valid(s)){finish(s);continue;}
            if(s.target!=null&&(!s.target.isAlive()||s.target.isRemoved()||s.target.level()!=s.level||!CombatSettings.canHit(s.owner,s.target)||PrisonSystem.locked(s.target))){finish(s);continue;}
            if(s.phase==RETURNING){returnTick(s);continue;}
            physics(s);
            if(!SESSIONS.containsKey(s.owner.getUUID()))continue;
            if(s.phase==EXTENDING)extend(s);
            if(s.phase==BOUND){
                hold(s);
                if(s.armed&&s.level.getGameTime()%10==0)TechniqueSeverance.maintainSeal(s.target);
                // 持续伤害：每秒 1 点，装天逆蛑翻倍
                if(s.target!=null&&(s.level.getGameTime()-s.boundAt)%20==0)
                    s.target.hurt(s.owner.damageSources().magic(), s.armed ? 2f : 1f);
            }
            if(s.level.getGameTime()%2==0)sync(s);
        }
    }
    private static BlockHitResult clip(Session s,Vec3 from,Vec3 to) {
        return s.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.owner));
    }
    private static boolean loaded(ServerLevel level,Vec3 from,Vec3 to) {
        for(int x=((int)Math.floor(Math.min(from.x,to.x)))>>4;x<=((int)Math.floor(Math.max(from.x,to.x)))>>4;x++)
            for(int z=((int)Math.floor(Math.min(from.z,to.z)))>>4;z<=((int)Math.floor(Math.max(from.z,to.z)))>>4;z++)
                if(!level.hasChunk(x,z))return false;
        return true;
    }
    private static void physics(Session s) {
        if(s.landed){
            if(s.level.getGameTime()%10!=0||!s.level.hasChunkAt(BlockPos.containing(s.anchor)))return;
            if(clip(s,s.anchor,s.anchor.add(0,-.2,0)).getType()!=HitResult.Type.MISS)return;
            s.landed=false;
        }
        Vec3 next=s.anchor.add(s.velocity);if(!loaded(s.level,s.anchor,next))return;
        if(next.y<s.level.getMinBuildHeight()-8||!s.level.getWorldBorder().isWithinBounds(BlockPos.containing(next))){finish(s);return;}
        var hit=clip(s,s.anchor,next);
        if(hit.getType()!=HitResult.Type.MISS){
            Vec3 normal=Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            s.anchor=hit.getLocation().add(normal.scale(.08));
            s.velocity=s.velocity.subtract(normal.scale(1.3*s.velocity.dot(normal))).scale(.45);
            if(hit.getDirection()==net.minecraft.core.Direction.UP&&s.velocity.lengthSqr()<.035){s.velocity=Vec3.ZERO;s.landed=true;}
        }else{s.anchor=next;s.velocity=s.velocity.scale(.97).add(0,-.045,0);}
        if(s.phase==ANCHOR)s.tip=s.anchor;
    }
    private static void extend(Session s) {
        // 每刻最多16格，4次短射线；总长度无上限，不累计链节实体、路径列表或区块票据。
        for(int i=0;i<4;i++){
            Vec3 next=s.tip.add(s.direction.scale(4));
            if(next.y<s.level.getMinBuildHeight()||next.y>s.level.getMaxBuildHeight()||!s.level.getWorldBorder().isWithinBounds(BlockPos.containing(next))){s.phase=STOPPED;tell(s.owner,"锁链抵达世界边界，左键收回");return;}
            if(!loaded(s.level,s.tip,next))return; // 等待自然加载；不是固定射程限制。
            var wall=clip(s,s.tip,next);Vec3 end=wall.getType()==HitResult.Type.MISS?next:wall.getLocation();
            LivingEntity nearest=null;double distance=Double.MAX_VALUE;
            for(LivingEntity target:s.level.getEntitiesOfClass(LivingEntity.class,new AABB(s.tip,end).inflate(.4),t->CombatSettings.canHit(s.owner,t)&&!bound(t)&&!PrisonSystem.locked(t))){
                var box=target.getBoundingBox().inflate(.2);var hit=box.contains(s.tip)?Optional.of(s.tip):box.clip(s.tip,end);
                if(hit.isPresent()){double d=hit.get().distanceToSqr(s.tip);if(d<distance){distance=d;nearest=target;}}
            }
            if(nearest!=null){bind(s,nearest);return;}
            s.tip=end;
            if(wall.getType()!=HitResult.Type.MISS){s.phase=STOPPED;sound(s);tell(s.owner,"锁链碰到方块，左键收回");return;}
        }
    }
    private static void bind(Session s,LivingEntity target) {
        if(s.armed)TechniqueSeverance.sever(target,true,s.owner);
        // 术式切断可能关闭领域并安全疏散目标，所以必须在切断后记录新的位置。
        if(target.level()!=s.level||!target.isAlive())return;
        s.target=target;s.pin=target.position();s.tip=target.getBoundingBox().getCenter();s.phase=BOUND;
        CAPTIVES.put(target.getUUID(),s);target.stopRiding();target.ejectPassengers();
        target.getPersistentData().putBoolean(GRAVITY,target.isNoGravity());target.setNoGravity(true);
        immobilize(target);
        // 锁定伤害：抓到瞬间 25 点（装天逆蛑翻倍）
        target.hurt(s.owner.damageSources().playerAttack(s.owner), s.armed ? 50f : 25f);
        s.boundAt=s.level.getGameTime();
        hold(s);sound(s);sync(s);tell(s.owner,s.armed?"已缠绕 · 天逆蛑持续封锁咒术，左键拉回":"已缠绕定身 · 左键拉到面前");
    }
    private static void immobilize(LivingEntity target) {
        for(var attribute:List.of(Attributes.MOVEMENT_SPEED,Attributes.FLYING_SPEED)){
            var value=target.getAttribute(attribute);
            if(value!=null&&value.getModifier(ROOT)==null)value.addTransientModifier(new AttributeModifier(ROOT,"万里之锁缠绕",-1,AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
    private static void restore(LivingEntity target) {
        if(target.getPersistentData().contains(GRAVITY)){
            target.setNoGravity(target.getPersistentData().getBoolean(GRAVITY));target.getPersistentData().remove(GRAVITY);
        }
        for(var attribute:List.of(Attributes.MOVEMENT_SPEED,Attributes.FLYING_SPEED)){
            var value=target.getAttribute(attribute);if(value!=null)value.removeModifier(ROOT);
        }
    }
    private static void hold(Session s) {
        LivingEntity target=s.target;if(target==null)return;
        immobilize(target);target.setNoGravity(true);
        // 领域收回等外部安全疏散优先；不把目标强拉回已被恢复成墙体的旧位置。
        AABB old=target.getBoundingBox().move(s.pin.subtract(target.position()));
        if(!s.level.noCollision(target,old)){s.pin=target.position();}
        target.stopRiding();target.setDeltaMovement(Vec3.ZERO);target.fallDistance=0;target.hurtMarked=true;
        if(target instanceof Mob mob)mob.getNavigation().stop();
        if(target.position().distanceToSqr(s.pin)>.0025)target.teleportTo(s.pin.x,s.pin.y,s.pin.z);
        s.tip=target.getBoundingBox().getCenter();
    }
    /** 抓住方块或敌人后「潜行 + 右键」：把自己拉到锁链另一端。 */
    public static boolean pullSelf(ServerPlayer p) {
        Session s = null;
        for (Session it : SESSIONS.values()) if (it.owner == p) s = it;
        if (s == null) return false;
        // 落点 = 万里之锁的「前端」（锁链首端/命中点），不是目标背后
        Vec3 dest = s.tip;
        if (dest == null) dest = s.target != null ? s.target.getBoundingBox().getCenter() : s.anchor;
        Vec3 dir = dest.subtract(p.position());
        if (dir.lengthSqr() < 1) return false;
        // 站在锁链前端靠玩家这一侧 0.6 格，避免直接叠进方块/生物里
        Vec3 at = dest.subtract(dir.normalize().scale(0.6));
        p.teleportTo(at.x, at.y, at.z);
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        p.hurtMarked = true;
        tell(p, "万里之锁 · 已拉到自己过去");
        finish(s);
        return true;
    }

    private static void recall(Session s) {
        if(s.phase==RETURNING)return;
        // 拉到敌人时不再判断安全地形：直接拉
        s.phase=RETURNING;s.returnTicks=0;sound(s);sync(s);
    }
    private static Vec3 safeFront(Session s) {
        var target=s.target;Vec3 look=s.owner.getLookAngle();Vec3 forward=new Vec3(look.x,0,look.z).normalize();
        if(forward.lengthSqr()<.1)forward=new Vec3(0,0,1);
        Vec3 side=new Vec3(-forward.z,0,forward.x);
        double distance=Math.max(2.2,target.getBbWidth()/2+s.owner.getBbWidth()/2+.6);
        for(int ring=0;ring<3;ring++)for(int lateral:new int[]{0,1,-1,2,-2})for(int dy:new int[]{0,1,-1,2,-2,3,-3}){
            Vec3 at=s.owner.position().add(forward.scale(distance+ring)).add(side.scale(lateral)).add(0,dy,0);
            at=new Vec3(at.x,Math.floor(at.y),at.z);
            if(DomainSafety.safe(s.level,target,at)&&!target.getBoundingBox().move(at.subtract(target.position())).intersects(s.owner.getBoundingBox()))return at;
        }
        return null;
    }
    /** 落地保底：卡进方块就轻推到旁边一格，并给 1 秒无敌帧，避免窒息/挤压伤害。 */
    private static void resolveStuck(ServerPlayer owner, LivingEntity target) {
        // 只对友方做保底：敌人依旧卡在墙里（用户明确要求）
        if (CombatSettings.canHit(owner, target)) return;
        if(target.level().noCollision(target,target.getBoundingBox()))return;
        double[][] offsets={{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{1,0,1},{-1,0,-1},{1,0,-1},{-1,0,1},{0,1,0},{0,2,0}};
        Vec3 base=target.position();
        for(double[] o:offsets){
            Vec3 at=base.add(o[0],o[1],o[2]);
            if(target.level().noCollision(target,target.getBoundingBox().move(at.subtract(base)))){
                target.teleportTo(at.x,at.y,at.z);
                target.setDeltaMovement(Vec3.ZERO);
                target.fallDistance=0;
                target.hurtMarked=true;
                break;
            }
        }
        target.invulnerableTime=Math.max(target.invulnerableTime,20);   // 保底期间不受伤害
    }

    private static void returnTick(Session s) {
        s.returnTicks++;
        Vec3 hand=hand(s.owner);
        if(s.target!=null){
            Vec3 dest=safeFront(s);
            if(dest==null){Vec3 lk=s.owner.getLookAngle();Vec3 fw=new Vec3(lk.x,0,lk.z).normalize();
                if(fw.lengthSqr()<.1)fw=new Vec3(0,0,1);
                dest=s.owner.position().add(fw.scale(Math.max(2.2,s.owner.getBbWidth()/2+s.target.getBbWidth()/2+.6)));}
            Vec3 from=s.target.position(),delta=dest.subtract(from);
            Vec3 step=from.add(delta.normalize().scale(Math.min(6,delta.length())));
            boolean clear=loaded(s.level,from,step);
            for(double t=.1;clear&&t<=1.001;t+=.1){Vec3 at=from.lerp(step,t);clear=s.level.noCollision(s.target,s.target.getBoundingBox().move(at.subtract(from)));}
            // 有墙或路途太远时直接放到已检查的安全落点，不让目标卡在途中。
            Vec3 at=!clear||s.returnTicks>=12?dest:step;
            s.target.teleportTo(at.x,at.y,at.z);s.target.setDeltaMovement(Vec3.ZERO);s.target.fallDistance=0;s.target.hurtMarked=true;
            resolveStuck(s.owner,s.target);
            s.pin=at;s.tip=s.target.getBoundingBox().getCenter();
            if(s.armed)TechniqueSeverance.maintainSeal(s.target);
            if(at.distanceToSqr(dest)<.05){tell(s.owner,"万里之锁已收回 · 目标已拉到面前");finish(s);return;}
        }else s.tip=s.tip.lerp(hand,.5);
        s.anchor=s.anchor.lerp(hand,.5);
        if(s.returnTicks>=12){finish(s);return;}sync(s);
    }
    private static Vec3 hand(ServerPlayer p){return p.getEyePosition().add(0,-.4,0);}
    private static Snapshot snapshot(Session s,int phase){return new Snapshot(s.owner.getUUID(),s.token,s.level.dimension().location(),phase,hand(s.owner),s.anchor,s.tip,s.target==null?-1:s.target.getId(),s.target==null?0:s.target.getBbWidth(),s.target==null?0:s.target.getBbHeight(),s.armed);}
    private static double lineDistance(Vec3 p,Vec3 a,Vec3 b){Vec3 d=b.subtract(a);double t=d.lengthSqr()<1e-8?0:Math.max(0,Math.min(1,p.subtract(a).dot(d)/d.lengthSqr()));return p.distanceToSqr(a.add(d.scale(t)));}
    private static void sync(Session s) {
        Snapshot packet=snapshot(s,s.phase);
        for(ServerPlayer viewer:s.level.players()){
            if(viewer==s.owner||viewer==s.target||lineDistance(viewer.position(),packet.hand(),s.anchor)<128*128||lineDistance(viewer.position(),packet.hand(),s.tip)<128*128){
                Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->viewer),packet);s.viewers.add(viewer.getUUID());
            }
        }
    }
    private static void finish(Session s) {
        if(!SESSIONS.remove(s.owner.getUUID(),s))return;
        if(s.target!=null){CAPTIVES.remove(s.target.getUUID(),s);restore(s.target);s.target.setDeltaMovement(Vec3.ZERO);s.target.fallDistance=0;s.target.hurtMarked=true;}
        Snapshot packet=snapshot(s,END);
        for(UUID id:s.viewers){ServerPlayer p=s.owner.server.getPlayerList().getPlayer(id);if(p!=null)Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),packet);}
    }
    private static void leave(Entity e){Session own=SESSIONS.get(e.getUUID()),captive=CAPTIVES.get(e.getUUID());if(own!=null)finish(own);if(captive!=null)finish(captive);}
    private static void sound(Session s){s.level.playSound(null,s.owner.blockPosition(),SoundEvents.CHAIN_PLACE,SoundSource.PLAYERS,.8f,1);}
    private static void tell(ServerPlayer p,String text){p.displayClientMessage(Component.literal(text),true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent e){if(bound(e.getEntity())||ThousandMileChainItem.isChain(e.getEntity().getMainHandItem()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void mine(PlayerInteractEvent.LeftClickBlock e){if(bound(e.getEntity())||ThousandMileChainItem.isChain(e.getEntity().getMainHandItem()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void melee(LivingAttackEvent e){if(e.getSource().getDirectEntity() instanceof LivingEntity attacker&&bound(attacker)&&(e.getSource().is(DamageTypes.MOB_ATTACK)||e.getSource().is(DamageTypes.MOB_ATTACK_NO_AGGRO)||e.getSource().is(DamageTypes.PLAYER_ATTACK)))e.setCanceled(true);}
    @SubscribeEvent public static void joined(EntityJoinLevelEvent e){if(!e.getLevel().isClientSide&&e.getEntity() instanceof LivingEntity living&&!bound(living))restore(living);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void living(LivingEvent.LivingTickEvent e){
        if(e.getEntity().level().isClientSide)return;
        Session s=CAPTIVES.get(e.getEntity().getUUID());
        if(s!=null&&s.phase==BOUND)hold(s);
        else if(s==null&&e.getEntity().getPersistentData().contains(GRAVITY))restore(e.getEntity());
        // 不取消整次生物tick，保留受伤无敌时间、药水和死亡计时的正常推进。
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void teleport(EntityTeleportEvent e){if(bound(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void travel(EntityTravelToDimensionEvent e){if(bound(e.getEntity()))e.setCanceled(true);else leave(e.getEntity());}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){leave(e.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){leave(e.getEntity());}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){for(Session s:List.copyOf(SESSIONS.values()))finish(s);CAPTIVES.clear();}
    private ThousandMileChain() {}
}
