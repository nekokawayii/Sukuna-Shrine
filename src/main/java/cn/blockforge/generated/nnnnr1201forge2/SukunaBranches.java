package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;

public final class SukunaBranches {
    private record Line(Vec3 from,Vec3 to){}
    private record Web(ServerPlayer owner,ServerLevel level,long due,List<Line> lines,Vec3 center,int radius){}
    private record Multi(ServerPlayer owner,ServerLevel level,UUID target,long due,int left){}
    private static final class Wall {
        final ServerPlayer owner;final ServerLevel level;final Vec3 direction;final List<Line> lines;final AABB bounds;final Map<UUID,BitSet> hit=new HashMap<>();int age;
        Wall(ServerPlayer p,List<Line> lines){
            owner=p;level=p.serverLevel();direction=p.getLookAngle().normalize();this.lines=lines;
            AABB box=new AABB(lines.get(0).from,lines.get(0).to);
            for(Line line:lines)box=box.minmax(new AABB(line.from,line.to));
            bounds=box.inflate(1);
        }
    }
    private static final List<Web> webs=new ArrayList<>();
    private static final List<Multi> multis=new ArrayList<>();
    private static final List<Wall> walls=new ArrayList<>();
    public static int stage(ServerPlayer p){return GuardianCombat.awakened(p)?10:VoidJourney.evolved(p)?9:SoulJourney.evolved(p)?8:Journey.nine(p)?7:Curse.awakened(p)?6:Math.max(0,Curse.tier(p)-1);}
    public static double cleaveFactor(ServerPlayer p){return BranchRules.cleave(Curse.rawEnergy(p),Curse.rawMaximum(p));}
    public static int wallSize(ServerPlayer p){return BranchRules.wallSize(Journey.nine(p),SoulJourney.evolved(p),VoidJourney.evolved(p),GuardianCombat.awakened(p));}
    public static int wallSpacing(ServerPlayer p){int n=Curse.data(p).getInt("WallSpacing");return n>=1&&n<=20?n:4;}
    public static boolean diagonal(ServerPlayer p){return Curse.data(p).getBoolean("WallDiagonal");}
    public static void configure(ServerPlayer p,int halfBlocks,boolean diagonal){if(halfBlocks<1||halfBlocks>20)return;Curse.data(p).putInt("WallSpacing",halfBlocks);Curse.data(p).putBoolean("WallDiagonal",diagonal);}
    /** 伏魔御厨子的三项点数 + 「纵向伤敌」开关（per-player NBT，键名 DomainRadiusPts/DomainDamagePts/DomainSpeedPts/DomainVerticalKill）：
     *  没存过时用 {@link DomainOptions} 的默认 4/3/3 + 开关默认开，换算表也全在那边（服务端第 80 刻展开与客户端界面共用）。 */
    public static int domainRadiusPts(ServerPlayer p){return domainPoints(p)[0];}
    public static int domainDamagePts(ServerPlayer p){return domainPoints(p)[1];}
    public static int domainSpeedPts(ServerPlayer p){return domainPoints(p)[2];}
    /** 「纵向伤敌」：没存过时默认**开**（照现在行为，能打到下方的目标）；关掉则可用总点数多 1（{@link DomainOptions#total(boolean)}）。 */
    public static boolean domainVerticalKill(ServerPlayer p){var tag=Curse.data(p);return !tag.contains("DomainVerticalKill")||tag.getBoolean("DomainVerticalKill");}
    /** 三项一起读：任何一项缺失（新玩家）或整体不合法（存档被改过）都退回整套默认值，绝不给出半套分配。 */
    private static int[] domainPoints(ServerPlayer p){
        var tag=Curse.data(p);boolean vertical=domainVerticalKill(p);
        int radius=tag.contains("DomainRadiusPts")?tag.getInt("DomainRadiusPts"):DomainOptions.DEFAULT_RADIUS;
        int damage=tag.contains("DomainDamagePts")?tag.getInt("DomainDamagePts"):DomainOptions.DEFAULT_DAMAGE;
        int speed=tag.contains("DomainSpeedPts")?tag.getInt("DomainSpeedPts"):DomainOptions.DEFAULT_SPEED;
        // 合法性按**同一份存档里的开关**校验：关掉纵向伤敌时 11 点是合法的（多出来的那 1 点就是它的补偿），开着时仍是 10。
        return DomainOptions.valid(radius,damage,speed,vertical)?new int[]{radius,damage,speed}:new int[]{DomainOptions.DEFAULT_RADIUS,DomainOptions.DEFAULT_DAMAGE,DomainOptions.DEFAULT_SPEED};
    }
    /** 保存伏魔御厨子的三项点数与「纵向伤敌」开关：越界或三项之和超过可用总点数（开关开=10 / 关=11）就整包拒收
     *  （存档里永远是合法分配，客户端发的值不当真）。 */
    public static void configureDomain(ServerPlayer p,int radiusPts,int damagePts,int speedPts,boolean verticalKill){
        if(!DomainOptions.valid(radiusPts,damagePts,speedPts,verticalKill))return;
        var tag=Curse.data(p);
        tag.putInt("DomainRadiusPts",radiusPts);tag.putInt("DomainDamagePts",damagePts);tag.putInt("DomainSpeedPts",speedPts);tag.putBoolean("DomainVerticalKill",verticalKill);
    }
    public static int cost(ServerPlayer p,int skill){return skill==BranchRules.WALL?BranchRules.wallCost(wallSpacing(p)):skill==BranchRules.WEB?180:120;}
    public static boolean available(ServerPlayer p,int skill){return skill>=BranchRules.WEB&&skill<=BranchRules.WALL&&Journey.unlocked(p)&&(skill==BranchRules.WALL?Curse.awakened(p):Curse.fingers(p)>=5)&&valid(p,p.serverLevel())&&!p.isSpectator()&&!MaidBridge.recovering(p)&&!SoulCombat.suppressed(p)&&Curse.canUseTechnique(p)&&!Curse.active(p,"BranchCooldown"+skill)&&Curse.canSpend(p,cost(p,skill))&&!(skill==BranchRules.MULTI&&Curse.data(p).getBoolean("MultiArmed"));}
    private static boolean valid(ServerPlayer p,ServerLevel l){return p.isAlive()&&!p.isRemoved()&&p.serverLevel()==l&&!EternalLove.settling(MaidBridge.body(p));}
    public static void cast(ServerPlayer p,int skill){
        if(skill<BranchRules.WEB||skill>BranchRules.WALL)return;
        if(CastFeedback.cooldown(p,"BranchCooldown"+skill,BranchRules.name(skill)))return;
        if(!Journey.unlocked(p)||(skill==BranchRules.WALL?!Curse.awakened(p):Curse.fingers(p)<5)){CastFeedback.tell(p,skill==BranchRules.WALL?"墙需要先融合即身佛肉体":"捌的分支需要先吃下5根宿傩手指");return;}
        if(skill==BranchRules.MULTI&&Curse.data(p).getBoolean("MultiArmed")){CastFeedback.tell(p,"多重斩已就绪：请先普攻命中敌人");return;}
        if(!available(p,skill)){CastFeedback.unavailable(p,cost(p,skill));return;}
        if(webs.size()>=128||walls.size()>=128){p.displayClientMessage(Component.literal("当前同时施法过多，请稍后再试"),true);return;}
        if(!Curse.spend(p,cost(p,skill)))return;
        Curse.status(p,"BranchCooldown"+skill,skill==BranchRules.WEB?240:skill==BranchRules.MULTI?200:160);SkillSounds.cast(p,0);AttackTypes.prepare(AttackTypes.NET_BRANCH); // 捌的分支：蜘蛛网/多重斩/墙
        CombatExpansion.afterCast(p,skill);
        if(skill==BranchRules.WEB){web(p);return;}
        if(skill==BranchRules.MULTI){Curse.data(p).putBoolean("MultiArmed",true);p.displayClientMessage(Component.literal("多重斩已就绪：下一次普攻命中敌人追加 "+BranchRules.cuts(stage(p))+" 道解"),true);return;}
        Vec3 dir=p.getLookAngle().normalize(),side=NetBranches.side(dir),up=dir.cross(side).normalize();
        if(diagonal(p)){Vec3 a=side.add(up).normalize();up=up.subtract(side).normalize();side=a;}
        List<Line> lines=new ArrayList<>();Vec3 center=p.getEyePosition().add(dir.scale(2));double spacing=BranchRules.spacing(wallSpacing(p)),half=wallSize(p)/2.0;
        int count=BranchRules.wallLines(wallSpacing(p),wallSize(p))/2;
        for(int i=0;i<count;i++){double v=(i-(count-1)/2.0)*spacing;lines.add(new Line(center.add(side.scale(v)).subtract(up.scale(half)),center.add(side.scale(v)).add(up.scale(half))));lines.add(new Line(center.add(up.scale(v)).subtract(side.scale(half)),center.add(up.scale(v)).add(side.scale(half))));}
        if(walls.size()<128){Wall w=new Wall(p,lines);walls.add(w);for(Line line:lines)Network.send(w.level,new Network.Effect(BranchRules.FX_WALL,p.getId(),line.from,line.to.subtract(line.from),1,40,pack(dir)));}
    }
    // 量化方向供客户端重建移动速度，不使用客户端准星。
    private static long pack(Vec3 d){int yaw=(int)Math.round(Math.toDegrees(Math.atan2(d.z,d.x))*100)+18000,pitch=(int)Math.round(Math.asin(d.y)*18000/Math.PI)+9000;return ((long)yaw<<16)|(pitch&65535);}
    public static Vec3 unpack(long v){double yaw=Math.toRadians(((v>>16)-18000)/100.0),pitch=Math.toRadians(((v&65535)-9000)/100.0);return new Vec3(Math.cos(yaw)*Math.cos(pitch),Math.sin(pitch),Math.sin(yaw)*Math.cos(pitch));}
    private static Vec3 surface(ServerPlayer p,double x,double z,double y){
        var l=p.serverLevel();BlockPos pos=BlockPos.containing(x,y,z);if(!l.hasChunkAt(pos))return null;
        var hit=l.clip(new ClipContext(new Vec3(x,y+16,z),new Vec3(x,y-24,z),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        return hit.getType()==HitResult.Type.MISS?null:hit.getLocation().add(0,0.15,0);
    }
    private static void web(ServerPlayer p){
        if(webs.size()>=128)return;int radius=BranchRules.radius(stage(p)),spokes=16;LivingEntity aimed=Skills.aimTarget(p);Vec3 aim=aimed==null?Skills.aimPoint(p,32):aimed.position();Vec3 floor=surface(p,aim.x,aim.z,aim.y);Vec3 center=floor==null?aim:floor;List<Line> lines=new ArrayList<>();
        double step=Math.max(1,radius/50.0);Vec3[][] rings=new Vec3[11][spokes];
        for(int i=0;i<spokes;i++){
            double a=i*Math.PI*2/spokes;Vec3 prev=surface(p,center.x,center.z,center.y);double y=center.y;
            for(double r=step;r<=radius+0.01;r+=step){Vec3 next=surface(p,center.x+Math.cos(a)*r,center.z+Math.sin(a)*r,y);if(next!=null&&prev!=null&&Math.abs(next.y-prev.y)<=8)lines.add(new Line(prev,next));prev=next;if(next!=null)y=next.y;}
            for(int j=1;j<=10;j++)rings[j][i]=surface(p,center.x+Math.cos(a)*radius*j/10.0,center.z+Math.sin(a)*radius*j/10.0,y);
        }
        for(int j=1;j<=10;j++)for(int i=0;i<spokes;i++){
            Vec3 a=rings[j][i],b=rings[j][(i+1)%spokes];if(a==null||b==null)continue;Vec3 prev=a;int n=Math.max(1,(int)Math.ceil(a.distanceTo(b)/2));
            for(int k=1;k<=n;k++){Vec3 q=a.lerp(b,k/(double)n),next=surface(p,q.x,q.z,prev==null?q.y:prev.y);if(prev!=null&&next!=null&&Math.abs(prev.y-next.y)<=8)lines.add(new Line(prev,next));prev=next;}
        }
        for(Line line:lines)Network.send(p.serverLevel(),new Network.Effect(BranchRules.FX_LINE,p.getId(),line.from,line.to.subtract(line.from),0.1f,23,0));
        webs.add(new Web(p,p.serverLevel(),p.level().getGameTime()+20,List.copyOf(lines),center,radius));
    }
    private static boolean near(LivingEntity target,Line line,double radius){AABB box=target.getBoundingBox().inflate(radius);return box.contains(line.from)||box.contains(line.to)||box.clip(line.from,line.to).isPresent();}
    private static void hit(ServerPlayer p,LivingEntity target,double fraction){if(!CombatSettings.canHit(p,target)||DomainClash.active(p)||DomainClash.active(target))return;target.invulnerableTime=0;AttackTypes.mark(AttackTypes.pending()!=null?AttackTypes.pending():AttackTypes.NET_BRANCH);CombatSettings.hurt(p,target,p.damageSources().sonicBoom(p),(float)(NetBranches.dismantleDamage(p,target)*fraction));}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void melee(LivingDamageEvent e){
        ServerPlayer p=CombatSettings.caster(e.getSource());if(p==null||e.isCanceled()||e.getAmount()<=0||!Curse.data(p).getBoolean("MultiArmed")||!Curse.canUseTechnique(p)||!CombatSettings.canHit(p,e.getEntity()))return;
        if(!e.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)&&!(MaidBridge.isCaster(p)&&e.getSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)))return;
        Curse.data(p).putBoolean("MultiArmed",false);if(multis.size()<1024)multis.add(new Multi(p,p.serverLevel(),e.getEntity().getUUID(),p.level().getGameTime()+1,BranchRules.cuts(stage(p))));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(Web w:List.copyOf(webs)){
            if(!valid(w.owner,w.level)||DomainClash.active(w.owner)){webs.remove(w);continue;}if(w.level.getGameTime()<w.due)continue;webs.remove(w);
            List<LivingEntity> victims=Skills.targets(w.level,w.owner,w.center,w.radius+3).stream().filter(t->{Vec3 d=t.position().subtract(w.center);return d.x*d.x+d.z*d.z<=w.radius*w.radius&&Math.abs(d.y)<24;}).toList();
            Set<UUID> direct=new HashSet<>();for(LivingEntity t:victims)if(w.lines.stream().anyMatch(line->near(t,line,1.4))){direct.add(t.getUUID());hit(w.owner,t,2);}
            // 全网未被线切到的敌人仅补一次，避免直接命中者吃双份。
            for(LivingEntity t:victims)if(!direct.contains(t.getUUID())){hit(w.owner,t,2);Network.send(w.level,new Network.Effect(CombatExpansionRules.FX_DUST,w.owner.getId(),t.position(),Vec3.ZERO,2,12,0));}
            int flung=0;Set<BlockPos> used=new HashSet<>();
            for(LivingEntity t:victims){if(!SkillDestruction.enabled(w.level)||!valid(w.owner,w.level)||flung>=CombatExpansionRules.WEB_BLOCKS)break;BlockPos c=t.blockPosition();
                for(BlockPos pos:BlockPos.betweenClosed(c.offset(-6,-3,-6),c.offset(6,1,6))){if(flung>=CombatExpansionRules.WEB_BLOCKS)break;Vec3 center=Vec3.atCenterOf(pos);
                    if(Math.pow(center.x-w.center.x,2)+Math.pow(center.z-w.center.z,2)>w.radius*w.radius||!CombatExpansionRules.webBlock(center.distanceToSqr(t.position()))||!used.add(pos.immutable())||!w.level.hasChunkAt(pos)||!w.level.getBlockState(pos.above()).getCollisionShape(w.level,pos.above()).isEmpty())continue;
                    if(SkillDestruction.fling(w.owner,pos.immutable(),center.subtract(t.position())))flung++;
                }
            }
            for(Line line:w.lines)SkillDestruction.line(w.owner,line.from,line.to,0.7);
        }
        for(Multi m:List.copyOf(multis)){
            if(!valid(m.owner,m.level)){multis.remove(m);continue;}if(m.level.getGameTime()<m.due)continue;multis.remove(m);
            if(m.level.getEntity(m.target) instanceof LivingEntity t&&t.isAlive()&&CombatSettings.canHit(m.owner,t)&&!DomainClash.active(t)&&!DomainClash.active(m.owner)){
                Vec3 center=t.getBoundingBox().getCenter(),side=NetBranches.side(m.owner.getLookAngle());double a=m.owner.getRandom().nextDouble()*Math.PI*2;Vec3 axis=side.scale(Math.cos(a)).add(0,Math.sin(a),0);
                Network.send(m.level,new Network.Effect(BranchRules.FX_LINE,m.owner.getId(),center.subtract(axis.scale(2)),axis.scale(4),1,8,1));hit(m.owner,t,1);SkillDestruction.impact(m.owner,center,1);
                if(m.left>1)multis.add(new Multi(m.owner,m.level,m.target,m.due+1,m.left-1));
            }
        }
        for(Wall w:List.copyOf(walls)){
            if(!valid(w.owner,w.level)||DomainClash.active(w.owner)||w.age>=40){walls.remove(w);continue;}
            Vec3 motion=w.direction.scale(w.age*1.5),next=motion.add(w.direction.scale(1.5));
            AABB swept=w.bounds.move(motion).expandTowards(w.direction.scale(1.5));
            for(LivingEntity t:w.level.getEntitiesOfClass(LivingEntity.class,swept,t->CombatSettings.canHit(w.owner,t)))if(!DomainClash.active(t)){
                BitSet hitLines=w.hit.computeIfAbsent(t.getUUID(),id->new BitSet(w.lines.size()));
                for(int i=0;i<w.lines.size()&&t.isAlive();i++){if(hitLines.get(i))continue;Line line=w.lines.get(i);boolean intersects=false;
                    for(int sample=0;sample<=4;sample++){Vec3 offset=motion.lerp(next,sample/4.0);if(near(t,new Line(line.from.add(offset),line.to.add(offset)),.35)){intersects=true;break;}}
                    if(intersects){hitLines.set(i);hit(w.owner,t,.5);}
                }
            }
            for(Line line:w.lines)SkillDestruction.line(w.owner,line.from.add(next),line.to.add(next),0.5);w.age++;
        }
    }
    public static void cancel(ServerPlayer p){webs.removeIf(w->w.owner==p);walls.removeIf(w->w.owner==p);multis.removeIf(m->m.owner==p);Curse.data(p).remove("MultiArmed");}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){webs.clear();walls.clear();multis.clear();}
    private SukunaBranches(){}
}
