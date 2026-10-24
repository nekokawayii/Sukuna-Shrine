package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL30;

/** Photon 血星主体和血刺；独立几何兜底保证特效失败时仍看得见蓄力、弹道与范围。 */
public final class DeathStarClient {
    private record Visual(Network.Effect e,long born,long end){}
    private record Particle(PhotonFx.Handle handle,long end){}
    private static final Map<String,Visual> visuals=new LinkedHashMap<>();
    private static final Map<String,Particle> emitters=new HashMap<>();
    private static final List<Particle> bursts=new ArrayList<>();
    private static long anemiaEnd,flashAt=-1000;
    private static Object world;
    private static String key(Network.Effect e){return e.owner()+"/"+e.seed();}
    private static long time(){var l=Minecraft.getInstance().level;return l==null?0:l.getGameTime();}
    public static boolean anemic(){return time()<anemiaEnd;}
    public static Vec3 attraction(Vec3 point){return visuals.values().stream().filter(v->v.e.kind()==DeathStar.FX_CHARGE&&v.end>time()&&v.e.position().distanceToSqr(point)<32*32).min(Comparator.comparingDouble(v->v.e.position().distanceToSqr(point))).map(v->v.e.position()).orElse(null);}
    public static boolean charging(){var p=Minecraft.getInstance().player;return p!=null&&visuals.values().stream().anyMatch(v->v.e.owner()==p.getId()&&v.e.kind()==DeathStar.FX_CHARGE&&v.end>time());}
    public static void clear(){emitters.values().forEach(p->PhotonFx.stop(p.handle));bursts.forEach(p->PhotonFx.stop(p.handle));emitters.clear();bursts.clear();visuals.clear();anemiaEnd=0;flashAt=-1000;world=null;}
    public static boolean receive(Network.Effect e){
        if(e.kind()<DeathStar.FX_CHARGE||e.kind()>DeathStar.FX_ANEMIA)return false;
        var mc=Minecraft.getInstance();if(mc.level==null)return true;if(world!=mc.level){clear();world=mc.level;}long now=time();String key=key(e);
        if(e.kind()==DeathStar.FX_ANEMIA){if(mc.player!=null&&mc.player.getId()==e.owner())anemiaEnd=now+e.life();return true;}
        if(e.kind()==DeathStar.FX_STOP){visuals.remove(key);Particle p=emitters.remove(key);if(p!=null)PhotonFx.stop(p.handle);GojoOpticsClient.receive(e);return true;}
        if(visuals.size()>=64&&!visuals.containsKey(key))return true;
        Visual old=visuals.get(key);visuals.put(key,new Visual(e,old==null||old.e.kind()!=e.kind()?now:old.born,now+Math.min(80,e.life())));
        GojoOpticsClient.receive(e);
        if(e.kind()==DeathStar.FX_BURST){
            if(mc.player!=null&&mc.player.distanceToSqr(e.position())<=80*80){flashAt=now;Client.slashShake();}
            if(photon()&&bursts.size()<12){var h=PhotonFx.deathStarBurst(e.position());if(h!=null)bursts.add(new Particle(h,now+30));}
        }else if(photon()){
            Particle p=emitters.get(key);
            if(p==null||p.end<=now){if(p!=null)PhotonFx.stop(p.handle);var h=PhotonFx.deathStar(e.position(),e.kind()==DeathStar.FX_CHARGE?.25+e.size()*.5:.8,e.kind()==DeathStar.FX_CHARGE);if(h!=null)emitters.put(key,new Particle(h,now+8));}
            else PhotonFx.move(p.handle,e.position());
        }
        return true;
    }
    private static boolean photon(){return net.minecraftforge.fml.ModList.get().isLoaded("photon");}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(world!=mc.level){clear();world=mc.level;}long now=time();visuals.values().removeIf(v->v.end<=now);emitters.entrySet().removeIf(e->{if(e.getValue().end>now&&visuals.containsKey(e.getKey()))return false;PhotonFx.stop(e.getValue().handle);return true;});bursts.removeIf(p->{if(p.end>now)return false;PhotonFx.stop(p.handle);return true;});}
    @SubscribeEvent public static void hud(RenderGuiOverlayEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||!event.getOverlay().id().toString().equals("minecraft:hotbar"))return;
        var g=event.getGuiGraphics();int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();long age=time()-flashAt;
        if(age>=0&&age<16){int a=(int)(215*Math.pow(1-age/16.0,2));g.fill(0,0,w,h,(a<<24)|0xFF163A);}
        if(anemic())g.drawCenteredString(mc.font,"严重贫血 "+((anemiaEnd-time()+19)/20)+"秒 · 禁止回血与赤血操术 · 移速/伤害减半",w/2,h-62,0xFFFF6B85);
        for(Visual v:visuals.values())if(v.e.kind()==DeathStar.FX_CHARGE&&v.e.owner()==mc.player.getId()){
            float progress=Math.max(v.e.size(),Math.min(1,(time()-v.born)/40f));int y=h/2+34;g.fill(w/2-70,y,w/2+70,y+5,0xCC200009);g.fill(w/2-70,y,w/2-70+(int)(140*progress),y+5,0xFFB6002C);g.drawCenteredString(mc.font,"死胎星 · 压缩中",w/2,y+9,0xFFFF496D);
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||visuals.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;Vec3 camera=event.getCamera().getPosition();var ps=event.getPoseStack();double now=time()+event.getPartialTick();
        boolean depth=GL30.glIsEnabled(GL30.GL_DEPTH_TEST),blend=GL30.glIsEnabled(GL30.GL_BLEND),cull=GL30.glIsEnabled(GL30.GL_CULL_FACE),mask=GL30.glGetBoolean(GL30.GL_DEPTH_WRITEMASK);
        int src=GL30.glGetInteger(GL30.GL_BLEND_SRC_RGB),dst=GL30.glGetInteger(GL30.GL_BLEND_DST_RGB),srcA=GL30.glGetInteger(GL30.GL_BLEND_SRC_ALPHA),dstA=GL30.glGetInteger(GL30.GL_BLEND_DST_ALPHA);var shader=RenderSystem.getShader();float[] color=RenderSystem.getShaderColor().clone();
        ps.pushPose();try{
            ps.translate(-camera.x,-camera.y,-camera.z);RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
            BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(Visual v:visuals.values()){
                if(v.e.position().distanceToSqr(camera)>180*180)continue;
                var e=v.e;double age=now-v.born,fade=Math.max(0,Math.min(1,(v.end-now)/5));Vec3 at=e.position(),dir=e.direction();
                if(e.kind()==DeathStar.FX_CHARGE){
                    var owner=mc.level.getEntity(e.owner());if(owner!=null)at=owner.getEyePosition().add(owner.getLookAngle().scale(1.15)).add(0,-.22,0);
                    double progress=Math.max(e.size(),Math.min(1,age/40));ball(b,ps,at,.25+progress*.5,camera,now,fade);
                    for(int i=0;i<42;i++){Vec3 ray=direction(i);double t=(now*.06+i*.137)%1;Vec3 start=at.add(ray.scale(2+(1-t)*14));line(b,ps,start,start.lerp(at,.25),.025,0xBB9F0628,camera);}
                    // 地面血水上升、头面七道血流和全身血管汇聚双手；不修改用户皮肤。
                    if(owner!=null){Vec3 base=owner.position(),side=NetBranches.side(owner.getLookAngle());
                        for(int i=0;i<12;i++){double a=i*2.4;Vec3 floor=base.add(Math.cos(a)*8,-.1,Math.sin(a)*8);line(b,ps,floor,floor.lerp(at,.55+.4*Math.sin(now*.05+i)),.035,0xD0900520,camera);}
                        for(int i=0;i<14;i++){double a=i*2.4;Vec3 low=base.add(Math.cos(a)*.3,.15+i*.07,Math.sin(a)*.22),high=base.add(Math.cos(a+.3)*.34,1.45,Math.sin(a+.3)*.25);line(b,ps,low,high,.016,0xFFB9082A,camera);line(b,ps,high,at,.018,0xBBA00026,camera);}
                        for(int i=-3;i<=3;i++){Vec3 face=owner.getEyePosition().add(owner.getLookAngle().scale(.26)).add(side.scale(i*.048)).add(0,-Math.abs(i)*.035,0);line(b,ps,face,face.add(0,-.35,0),.014,0xFF95001F,camera);}
                    }
                }else if(e.kind()==DeathStar.FX_FLIGHT){
                    at=at.add(dir.scale(Math.min(2,now-(v.end-e.life()))*DeathStarRules.SPEED));ball(b,ps,at,.8,camera,now,fade);
                    for(int i=0;i<4;i++)ring(b,ps,at.subtract(dir.scale(i*.6)),dir,1+i*.45,.045,(int)(140*fade)<<24|0xFFC3CE,camera);
                    line(b,ps,at.subtract(dir.scale(4)),at,.2,(int)(180*fade)<<24|0x720017,camera);
                }else if(e.kind()==DeathStar.FX_BURST){
                    double radius=Math.min(50,age*8);
                    for(int i=0;i<192;i++){Vec3 axis=direction(i);line(b,ps,at.add(axis.scale(radius*.04)),at.add(axis.scale(radius*(.85+.15*Math.sin(i)))),.08+fade*.15,(int)(230*fade)<<24|0xD40B35,camera);}
                    for(int i=0;i<3;i++)ring(b,ps,at,direction(i*19),radius,.12,(int)(210*fade)<<24|0xFF2249,camera);
                }
            }
            BufferUploader.drawWithShader(b.end());
        }finally{ps.popPose();RenderSystem.depthMask(mask);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();RenderSystem.blendFuncSeparate(src,dst,srcA,dstA);if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);}
    }
    private static Vec3 direction(int i){double y=Math.sin(i*73.13),a=i*2.399,r=Math.sqrt(1-y*y);return new Vec3(Math.cos(a)*r,y,Math.sin(a)*r);}
    private static void ball(BufferBuilder b,PoseStack ps,Vec3 at,double radius,Vec3 camera,double time,double fade){
        // 实心立体黑核，外沿只有窄红环；不是横穿画面的激光或一张亮色圆片。
        for(int lat=0;lat<10;lat++)for(int lon=0;lon<24;lon++){double a=-Math.PI/2+lat*Math.PI/10,z=a+Math.PI/10,u=lon*Math.PI/12,v=u+Math.PI/12;Client.quad(b,ps,sphere(at,radius,a,u),sphere(at,radius,z,u),sphere(at,radius,z,v),sphere(at,radius,a,v),(int)(255*fade)<<24|0x100006);}
        ring(b,ps,at,camera.subtract(at).normalize(),radius*1.08,.035,(int)(230*fade)<<24|0xDA123A,camera);
        ring(b,ps,at,new Vec3(Math.sin(time*.08),.4,Math.cos(time*.08)).normalize(),radius*1.32,.025,(int)(180*fade)<<24|0xFF264B,camera);
    }
    private static Vec3 sphere(Vec3 p,double r,double a,double u){return p.add(Math.cos(a)*Math.cos(u)*r,Math.sin(a)*r,Math.cos(a)*Math.sin(u)*r);}
    private static void ring(BufferBuilder b,PoseStack ps,Vec3 at,Vec3 normal,double radius,double width,int color,Vec3 camera){Vec3 side=NetBranches.side(normal),up=normal.cross(side).normalize();if(up.lengthSqr()<.1){side=new Vec3(1,0,0);up=new Vec3(0,0,1);}for(int i=0;i<48;i++){double a=i*Math.PI/24,z=(i+1)*Math.PI/24;line(b,ps,at.add(side.scale(Math.cos(a)*radius)).add(up.scale(Math.sin(a)*radius)),at.add(side.scale(Math.cos(z)*radius)).add(up.scale(Math.sin(z)*radius)),width,color,camera);}}
    private static void line(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,double width,int color,Vec3 camera){Client.ribbon(b,ps,a,z,width,color,camera);}
    private DeathStarClient(){}
}
