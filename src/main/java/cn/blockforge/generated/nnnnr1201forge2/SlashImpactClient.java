package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.particles.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/** 捕获实体当时的真实蒙皮顶点，沿斩击平面裁成两块；不生成掉落物、不改死亡结算。 */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class SlashImpactClient {
    private static final DustParticleOptions BLOOD=new DustParticleOptions(new Vector3f(.65f,.012f,.025f),1.15f);
    private record V(Vec3 p,float r,float g,float b,float a,float u,float v,int overlay,int light,Vec3 normal){
        V mix(V q,double t){float f=(float)t;return new V(p.lerp(q.p,t),r+(q.r-r)*f,g+(q.g-g)*f,b+(q.b-b)*f,a+(q.a-a)*f,u+(q.u-u)*f,v+(q.v-v)*f,overlay,light,normal.lerp(q.normal,t).normalize());}
    }
    private static final class Piece {
        final Map<RenderType,List<V>> mesh=new LinkedHashMap<>();
        final List<Vec3> seam=new ArrayList<>();
        Vec3 at,old,velocity;final float spin;float angle;boolean ground;
        Piece(Vec3 at,Vec3 velocity,float spin){this.at=at;old=at;this.velocity=velocity;this.spin=spin;}
    }
    private record Corpse(UUID id,List<Piece> pieces,long born){}
    private static final Deque<Corpse> corpses=new ArrayDeque<>();
    private static long tick;
    private static boolean capturing;
    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();var level=mc.level;if(level==null)return;
        LivingEntity target=level.getEntity(e.owner()) instanceof LivingEntity l?l:null;
        Vec3 center=target==null?e.position().add(0,.9,0):target.getBoundingBox().getCenter();
        Random random=new Random(e.owner()*31L+tick);
        int count=e.size()>0?38:18;
        for(int i=0;i<count;i++){
            Vec3 v=e.direction().scale((random.nextDouble()-.5)*.8).add((random.nextDouble()-.5)*.2,.04+random.nextDouble()*.14,(random.nextDouble()-.5)*.2);
            level.addParticle(BLOOD,center.x+(random.nextDouble()-.5)*.4,center.y+(random.nextDouble()-.5)*.4,center.z+(random.nextDouble()-.5)*.4,v.x,v.y,v.z);
        }
        for(int i=0;i<7;i++)level.addParticle(ParticleTypes.SMOKE,center.x,center.y,center.z,(random.nextDouble()-.5)*.12,.05+random.nextDouble()*.07,(random.nextDouble()-.5)*.12);
        level.playLocalSound(center.x,center.y,center.z,SoundEvents.PLAYER_ATTACK_CRIT,SoundSource.PLAYERS,.7f,.65f+random.nextFloat()*.2f,false);
        if(mc.player!=null&&(e.seed()==mc.player.getId()||mc.player.distanceToSqr(center)<25))Client.slashShake();
        if(e.size()>0&&target!=null)capture(target,e.position(),e.direction());
    }
    public static void clear(){corpses.clear();tick=0;}
    private static double distance(V v,Vec3 normal,double height){return v.p.subtract(0,height*.5,0).dot(normal);}
    private static List<V> clip(List<V> in,Vec3 normal,double height,int sign){
        if(in.isEmpty())return in;List<V> out=new ArrayList<>();V a=in.get(in.size()-1);double da=distance(a,normal,height)*sign;
        for(V b:in){double db=distance(b,normal,height)*sign;if((da>=0)!=(db>=0))out.add(a.mix(b,da/(da-db)));if(db>=0)out.add(b);a=b;da=db;}return out;
    }
    private static final class Capture implements VertexConsumer {
        final RenderType type;final Piece[] pieces;final Vec3 normal;final double height;
        final List<V> quad=new ArrayList<>(4);int count;Vec3 p=Vec3.ZERO,n=new Vec3(0,1,0);float r=1,g=1,b=1,a=1,u,v;int overlay,light;
        Capture(RenderType type,Piece[] pieces,Vec3 normal,double height){this.type=type;this.pieces=pieces;this.normal=normal;this.height=height;}
        public VertexConsumer vertex(double x,double y,double z){p=new Vec3(x,y,z);return this;}
        public VertexConsumer color(int r,int g,int b,int a){this.r=r/255f;this.g=g/255f;this.b=b/255f;this.a=a/255f;return this;}
        public VertexConsumer uv(float u,float v){this.u=u;this.v=v;return this;}
        public VertexConsumer overlayCoords(int x,int y){overlay=x|(y<<16);return this;}
        public VertexConsumer uv2(int x,int y){light=x|(y<<16);return this;}
        public VertexConsumer normal(float x,float y,float z){n=new Vec3(x,y,z);return this;}
        public void defaultColor(int r,int g,int b,int a){color(r,g,b,a);}
        public void unsetDefaultColor(){}
        public void endVertex(){
            if(type.mode()!=VertexFormat.Mode.QUADS||count++>=24000)return;
            quad.add(new V(p,r,g,b,a,u,v,overlay,light,n));if(quad.size()!=4)return;
            for(int i=0;i<4;i++){
                V v0=quad.get(i),v1=quad.get((i+1)%4);double d0=distance(v0,normal,height),d1=distance(v1,normal,height);
                if((d0<0)!=(d1<0)){Vec3 cross=v0.p.lerp(v1.p,d0/(d0-d1));for(Piece piece:pieces)if(piece.seam.size()<128&&piece.seam.stream().noneMatch(q->q.distanceToSqr(cross)<1e-8))piece.seam.add(cross);}
            }
            for(int side=0;side<2;side++){
                List<V> poly=clip(quad,normal,height,side==0?1:-1);if(poly.size()<3)continue;
                List<V> mesh=pieces[side].mesh.computeIfAbsent(type,k->new ArrayList<>());
                for(int i=1;i+1<poly.size();i++){mesh.add(poly.get(0));mesh.add(poly.get(i));mesh.add(poly.get(i+1));mesh.add(poly.get(i+1));}
            }
            quad.clear();
        }
    }
    private static void capture(LivingEntity entity,Vec3 at,Vec3 normal){
        if(corpses.stream().anyMatch(c->c.id.equals(entity.getUUID())))return;
        Vec3 n=normal.lengthSqr()<.1?new Vec3(0,1,0):normal.normalize();
        Piece[] pieces={new Piece(at,n.scale(.16).add(0,.18,0),.025f),new Piece(at,n.scale(-.16).add(0,.22,0),-.03f)};
        var mc=Minecraft.getInstance();Map<RenderType,Capture> buffers=new LinkedHashMap<>();int death=entity.deathTime,hurt=entity.hurtTime;
        try{
            capturing=true;entity.deathTime=0;entity.hurtTime=0;
            var renderer=mc.getEntityRenderDispatcher().getRenderer(entity);
            renderer.render(entity,entity.getYRot(),0,new PoseStack(),type->buffers.computeIfAbsent(type,t->new Capture(t,pieces,n,entity.getBbHeight())),mc.getEntityRenderDispatcher().getPackedLightCoords(entity,0));
            if(pieces[0].mesh.isEmpty()||pieces[1].mesh.isEmpty())return;
            Vec3 side=n.cross(new Vec3(0,0,1)).normalize();if(side.lengthSqr()<.1)side=new Vec3(1,0,0);Vec3 up=n.cross(side).normalize();final Vec3 right=side;
            for(Piece piece:pieces)if(piece.seam.size()>=3){Vec3 center=piece.seam.stream().reduce(Vec3.ZERO,Vec3::add).scale(1.0/piece.seam.size());piece.seam.sort(Comparator.comparingDouble(v->Math.atan2(v.subtract(center).dot(up),v.subtract(center).dot(right))));}
            if(corpses.size()>=32)corpses.removeFirst();corpses.addLast(new Corpse(entity.getUUID(),List.of(pieces),tick));
        }catch(RuntimeException ex){com.mojang.logging.LogUtils.getLogger().debug("该实体的斩杀碎块无法捕获，保留原死亡模型",ex);}
        finally{capturing=false;entity.deathTime=death;entity.hurtTime=hurt;}
    }
    @SubscribeEvent public static void hide(RenderLivingEvent.Pre<?,?> e){if(!capturing&&corpses.stream().anyMatch(c->c.id.equals(e.getEntity().getUUID())))e.setCanceled(true);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level==null){clear();return;}if(mc.isPaused())return;
        tick++;corpses.removeIf(c->tick-c.born>=100);
        for(Corpse c:corpses)for(Piece p:c.pieces){
            p.old=p.at;if(p.ground)continue;Vec3 next=p.at.add(p.velocity);
            var hit=mc.level.clip(new ClipContext(p.at.add(0,.12,0),next.add(0,.12,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
            if(hit.getType()!=HitResult.Type.MISS){p.at=hit.getLocation();p.ground=true;}
            else{p.at=next;p.velocity=p.velocity.scale(.96).add(0,-.035,0);p.angle+=p.spin;}
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||corpses.isEmpty())return;
        var mc=Minecraft.getInstance();var source=mc.renderBuffers().bufferSource();PoseStack ps=e.getPoseStack();Vec3 camera=e.getCamera().getPosition();
        for(Corpse c:corpses)for(Piece p:c.pieces){
            Vec3 at=p.old.lerp(p.at,e.getPartialTick()).subtract(camera);ps.pushPose();ps.translate(at.x,at.y,at.z);ps.mulPose(com.mojang.math.Axis.ZP.rotation(p.angle));
            for(var entry:p.mesh.entrySet()){
                VertexConsumer consumer=source.getBuffer(entry.getKey());
                for(V v:entry.getValue())consumer.vertex(ps.last().pose(),(float)v.p.x,(float)v.p.y,(float)v.p.z).color(v.r,v.g,v.b,v.a).uv(v.u,v.v).overlayCoords(v.overlay).uv2(v.light).normal(ps.last().normal(),(float)v.normal.x,(float)v.normal.y,(float)v.normal.z).endVertex();
            }
            if(p.seam.size()>=3){
                var cap=source.getBuffer(RenderType.debugQuads());Vec3 center=p.seam.stream().reduce(Vec3.ZERO,Vec3::add).scale(1.0/p.seam.size());
                for(int i=0;i<p.seam.size();i++){Vec3 a=p.seam.get(i),z=p.seam.get((i+1)%p.seam.size());for(Vec3 v:List.of(center,a,z,z))cap.vertex(ps.last().pose(),(float)v.x,(float)v.y,(float)v.z).color(105,5,16,255).endVertex();}
            }
            ps.popPose();
        }source.endBatch();
    }
    private SlashImpactClient(){}
}
