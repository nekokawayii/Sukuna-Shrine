package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL30;

/** 动态血液材质负责主体，Photon 负责雾、飞沫与亮点；后者失败不吞掉整招。 */
public final class BloodClient {
    private static final ResourceLocation TEXTURE=new ResourceLocation("sukuna","textures/effect/living_blood.png");
    private static ShaderInstance shader;
    private record Visual(Network.Effect e,long born,long end){}
    private record Particle(PhotonFx.Handle h,long end){}
    private static final List<Visual> visuals=new ArrayList<>();
    private static final Map<Long,Visual> domains=new HashMap<>();
    private static final Map<Long,Visual> oceans=new HashMap<>();
    private static final List<Particle> particles=new ArrayList<>();
    private static Object world;
    private static long tick;
    public static final class Registration {
        @SubscribeEvent public static void shaders(RegisterShadersEvent event){shader=null;try{event.registerShader(new ShaderInstance(event.getResourceProvider(),new ResourceLocation("sukuna","blood_fluid"),DefaultVertexFormat.POSITION_COLOR_TEX),s->shader=s);}catch(java.io.IOException|RuntimeException error){com.mojang.logging.LogUtils.getLogger().warn("赤血动态材质加载失败，保留流动血线与基础材质",error);}}
    }
    public static boolean receive(Network.Effect e){
        if((e.kind()<BloodArts.FX||e.kind()>BloodDomain.FX_CLOSE)&&(e.kind()<BloodExpansion.FX_OCEAN||e.kind()>BloodExpansion.FX_OCEAN_CLOSE))return false;
        var mc=Minecraft.getInstance();if(mc.level==null)return true;if(world!=mc.level){clear();world=mc.level;}tick=mc.level.getGameTime();
        if(e.kind()==BloodExpansion.FX_OCEAN_CLOSE){oceans.remove(e.seed());return true;}
        if(e.kind()==BloodExpansion.FX_OCEAN){oceans.put(e.seed(),new Visual(e,(long)e.direction().x,tick+e.life()));return true;}
        if(e.kind()==BloodDomain.FX_CLOSE){domains.remove(e.seed());return true;}
        if(e.kind()==BloodDomain.FX_DOMAIN){Visual old=domains.get(e.seed());domains.put(e.seed(),new Visual(e,old==null?tick:old.born,tick+e.life()));return true;}
        if(e.life()<=0)return true;
        if(e.kind()==BloodArts.FX+2)GojoOpticsClient.receive(e);
        if(e.kind()==BloodArts.FX+4||e.kind()==BloodArts.FX+5&&e.seed()==0||e.kind()==BloodArts.FX+7||e.kind()==BloodArts.FX+10||e.kind()==BloodArts.FX+13&&e.seed()==0||e.kind()==BloodArts.FX+2||e.kind()==BloodArts.FX_HEART||e.kind()==BloodArts.FX_READY)visuals.removeIf(v->v.e.kind()==e.kind()&&v.e.owner()==e.owner()&&v.e.seed()==e.seed());
        if(visuals.size()>=700)visuals.remove(0);visuals.add(new Visual(e,tick,tick+Math.min(700,e.life())));
        if(net.minecraftforge.fml.ModList.get().isLoaded("photon")&&particles.size()<96&&(e.kind()==BloodArts.FX_HIT||e.kind()==BloodArts.FX+8||e.kind()==BloodArts.FX+11||e.kind()==BloodArts.FX_SHATTER)){
            boolean mist=e.kind()==BloodArts.FX+11;
            var h=PhotonFx.sphere(e.position(),mist?Math.min(6,e.size()):.7,mist?1.8:.15,mist?0xAA760D28:0xFFD30D2A,0xFF480711,"textures/effect/dust.png",false,5,mist?20:12,mist?5:2,.6);
            if(h!=null)particles.add(new Particle(h,tick+30));
        }
        return true;
    }
    public static void clear(){BloodRealmRenderer.clear();for(Particle p:particles)PhotonFx.stop(p.h);particles.clear();visuals.clear();domains.clear();oceans.clear();BloodVisionClient.clear();world=null;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(world!=mc.level){clear();world=mc.level;}if(mc.level==null)return;tick=mc.level.getGameTime();visuals.removeIf(v->v.end<=tick);domains.values().removeIf(v->v.end<=tick);oceans.values().removeIf(v->v.end<=tick);for(Iterator<Particle> it=particles.iterator();it.hasNext();){Particle p=it.next();if(p.end<=tick){PhotonFx.stop(p.h);it.remove();}}}
    public static boolean ownDomain(){var p=Minecraft.getInstance().player;return p!=null&&domains.values().stream().anyMatch(v->v.e.owner()==p.getId()&&v.end>timeNow()&&BloodRules.inside(p.position().distanceToSqr(v.e.position())));}
    public static boolean vision(){var p=Minecraft.getInstance().player;return p!=null&&visuals.stream().anyMatch(v->v.end>timeNow()&&v.e.kind()==BloodArts.FX+4&&v.e.owner()==p.getId()&&v.e.seed()==1);}
    public static boolean predicted(int id){return vision()&&visuals.stream().anyMatch(v->v.end>timeNow()&&v.e.kind()==BloodArts.FX_HEART&&v.e.owner()==id);}
    private static long timeNow(){var level=Minecraft.getInstance().level;return level==null?0:level.getGameTime();}
    public static Vec3 domainCenter(Vec3 at){Visual domain=containing(at);return domain==null?null:domain.e.position();}
    private static Visual containing(Vec3 at){return domains.values().stream().filter(v->BloodRules.inside(at.distanceToSqr(v.e.position()))).min(Comparator.comparingDouble(v->at.distanceToSqr(v.e.position()))).orElse(null);}
    @SubscribeEvent public static void fog(ViewportEvent.ComputeFogColor event){if(containing(event.getCamera().getPosition())!=null){event.setRed(.18f);event.setGreen(.008f);event.setBlue(.025f);}}
    @SubscribeEvent public static void fogDistance(ViewportEvent.RenderFog event){if(containing(event.getCamera().getPosition())!=null){event.setNearPlaneDistance(55);event.setFarPlaneDistance(115);event.setCanceled(true);}}
    @SubscribeEvent public static void hud(RenderGuiOverlayEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||!event.getOverlay().id().toString().equals("minecraft:hotbar"))return;
        boolean vision=visuals.stream().anyMatch(v->v.e.kind()==BloodArts.FX+4&&v.e.owner()==mc.player.getId()&&v.e.seed()==1);
        var charge=visuals.stream().filter(v->v.e.kind()==BloodArts.FX+2&&v.e.owner()==mc.player.getId()).findFirst().orElse(null);
        if(charge!=null){var g=event.getGuiGraphics();int x=mc.getWindow().getGuiScaledWidth()/2,y=mc.getWindow().getGuiScaledHeight()/2+30;g.fill(x-60,y,x+60,y+5,0xAA22040B);g.fill(x-60,y,x-60+(int)(120*Math.min(1,charge.e.size())),y+5,0xFFD22B4A);g.drawCenteredString(mc.font,charge.e.size()>=1?"百敛完成 松开储存":"百敛蓄力中",x,y+8,0xFFFFB4C3);}
        if(vision){var g=event.getGuiGraphics();int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();for(int i=0;i<12;i++){int a=(12-i)*4<<24|0x9D082B;g.fill(i,0,i+1,h,a);g.fill(w-i-1,0,w-i,h,a);g.fill(0,i,w,i+1,a);g.fill(0,h-i-1,w,h-i,a);}}
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||visuals.isEmpty()&&domains.isEmpty()&&oceans.isEmpty())return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        Vec3 camera=event.getCamera().getPosition();double time=mc.level.getGameTime()+event.getPartialTick();PoseStack ps=event.getPoseStack();
        boolean depth=GL30.glIsEnabled(GL30.GL_DEPTH_TEST),blend=GL30.glIsEnabled(GL30.GL_BLEND),cull=GL30.glIsEnabled(GL30.GL_CULL_FACE),mask=GL30.glGetBoolean(GL30.GL_DEPTH_WRITEMASK);
        int src=GL30.glGetInteger(GL30.GL_BLEND_SRC_RGB),dst=GL30.glGetInteger(GL30.GL_BLEND_DST_RGB),srcA=GL30.glGetInteger(GL30.GL_BLEND_SRC_ALPHA),dstA=GL30.glGetInteger(GL30.GL_BLEND_DST_ALPHA),texture=RenderSystem.getShaderTexture(0);
        var previous=RenderSystem.getShader();float[] color=RenderSystem.getShaderColor().clone();
        ps.pushPose();try{
            ps.translate(-camera.x,-camera.y,-camera.z);RenderSystem.enableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShaderTexture(0,TEXTURE);
            if(shader!=null){shader.safeGetUniform("BloodTime").set((float)(time/20));shader.safeGetUniform("FluidMode").set(0f);RenderSystem.setShader(()->shader);}else RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
            BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR_TEX);
            for(Visual v:visuals)if(v.e.position().distanceToSqr(camera)<180*180)draw(b,ps,v,camera,time);
            for(Visual v:oceans.values())if(v.e.position().distanceToSqr(camera)<240*240)ocean(b,ps,v,camera,time);
            BufferUploader.drawWithShader(b.end());
            if(shader!=null)shader.safeGetUniform("FluidMode").set(1f);
            b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR_TEX);
            for(Visual v:domains.values())if(v.e.position().distanceToSqr(camera)<240*240)domain(b,ps,v,camera,time);
            BufferUploader.drawWithShader(b.end());
            // 眼白与瞳孔独立使用纯色通道，绝不经过红色血液材质染色。
            RenderSystem.setShader(GameRenderer::getPositionColorShader);b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            for(Visual v:domains.values())eyes(b,ps,v,camera,time);
            BufferUploader.drawWithShader(b.end());
            if(shader!=null){shader.safeGetUniform("FluidMode").set(0f);RenderSystem.setShader(()->shader);}else RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
            // 预测轨迹不修改服务端时间，也不会标记友方。
            RenderSystem.disableDepthTest();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR_TEX);
            for(Visual v:visuals)if(v.e.kind()==BloodArts.FX_HEART){Entity target=mc.level.getEntity(v.e.owner());Vec3 at=target==null?v.e.position():target.position();heart(b,ps,at.add(0,target==null?2:target.getEyeHeight()+.3,0),camera,time);prediction(b,ps,at,v.e.direction(),v.e.size(),camera,time);}
            BufferUploader.drawWithShader(b.end());
        }finally{ps.popPose();RenderSystem.depthMask(mask);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();RenderSystem.blendFuncSeparate(src,dst,srcA,dstA);if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();RenderSystem.setShaderTexture(0,texture);RenderSystem.setShader(()->previous);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);}
    }
    private static void draw(BufferBuilder b,PoseStack ps,Visual visual,Vec3 camera,double time){
        var e=visual.e;int skill=e.kind()-BloodArts.FX;double age=time-visual.born,fade=Math.min(1,(visual.end-time)/6);Vec3 at=e.position(),dir=e.direction();double size=Math.max(.05,e.size());
        Entity owner=Minecraft.getInstance().level.getEntity(e.owner());
        if(skill==4||skill==5&&e.seed()==0||skill==7||skill==10){if(owner!=null)at=owner.position();}
        switch(skill){
            case 0 -> {if(e.seed()>0){Entity target=Minecraft.getInstance().level.getEntity((int)e.seed());if(target!=null)at=target.position();for(int i=0;i<6;i++)ring(b,ps,at.add(0,.2+i*.27,0),new Vec3(0,1,0),.58+Math.sin(time*.2+i)*.035,.055,camera,time,fade);for(int i=0;i<8;i++){double a=i*Math.PI/4;rope(b,ps,at.add(Math.cos(a)*.6,0,Math.sin(a)*.6),at.add(Math.cos(a+.5)*.6,size,Math.sin(a+.5)*.6),.04,camera,time,fade);}}else{Vec3 side=NetBranches.side(dir);for(int i=-5;i<=5;i++){Vec3 tip=at.add(dir.scale(9)).add(side.scale(i*.9)).add(0,-.8,0);rope(b,ps,at.add(0,-1,0),tip,.035,camera,time,fade);for(int j=1;j<=4;j++)rope(b,ps,at.add(dir.scale(j*2)).add(side.scale(-j)),at.add(dir.scale(j*2)).add(side.scale(j)),.025,camera,time+j,fade);}}}
            case 1 -> {if(e.life()>5){Vec3 side=NetBranches.side(dir);for(int i=-1;i<=1;i++)ring(b,ps,at.add(dir.scale(.8)).add(side.scale(i*.65)).add(0,i==0?.35:-.15,0),dir,.42,.09,camera,time,fade);}else{ring(b,ps,at,dir,.65,.12,camera,time,fade);ring(b,ps,at.subtract(dir.scale(.35)),dir,.61,.04,camera,time,fade*.5);}}
            case 2 -> {orb(b,ps,at,.18+size*.22,camera,time,fade);for(int i=0;i<3;i++)ring(b,ps,at,dir,.5+i*.15,.015,camera,time+i,fade*.55);}
            case 3 -> {if(e.direction().distanceToSqr(at)>4){orb(b,ps,at,.19*size,camera,time,fade);rope(b,ps,at,e.direction(),.045*size,camera,time,fade);Vec3 axis=e.direction().subtract(at),side=NetBranches.side(axis.normalize()),up=axis.normalize().cross(side);for(int i=0;i<96;i++){double t=i/96.0,u=(i+1)/96.0,a=t*axis.length()*2-time*.8,z=u*axis.length()*2-time*.8;Vec3 x=at.add(axis.scale(t)).add(side.scale(Math.cos(a)*.13*size)).add(up.scale(Math.sin(a)*.13*size)),y=at.add(axis.scale(u)).add(side.scale(Math.cos(z)*.13*size)).add(up.scale(Math.sin(z)*.13*size));line(b,ps,x,y,.025*size,camera,fade);}}}
            case 4 -> {for(int i=0;i<7;i++){double a=i*.9+time*.15;rope(b,ps,at.add(Math.cos(a)*.35,.2,Math.sin(a)*.35),at.add(Math.cos(a+.4)*.4,1.6,Math.sin(a+.4)*.4),.045,camera,time+i,fade);Vec3 velocity=e.direction();line(b,ps,at.add(0,.8,0),at.add(0,.8,0).subtract(velocity.scale(8)),.17,camera,fade*.7);}}
            case 5 -> {if(e.seed()==1)arc(b,ps,at,dir,1.4,.12,camera,time,fade);else{Vec3 facing=owner==null?dir:owner.getLookAngle();rope(b,ps,at.add(0,.8,0).add(NetBranches.side(facing).scale(.4)),at.add(0,1.1,0).add(facing.scale(1.1)),.09,camera,time,fade);}}
            case 6 -> {orb(b,ps,at.add(dir.scale(e.seed()==2?0:Math.min(4,age*.35))),.6,camera,time,fade);if(e.seed()==2)ring(b,ps,at,dir,age*.16,.08,camera,time,fade);}
            case 7 -> {for(int i=0;i<6;i++)ring(b,ps,at.add(0,.3+i*.24,0),new Vec3(0,1,0),.42,.13,camera,time+i,fade);}
            case 8 -> {int rays=(int)Math.max(96,Math.min(192,e.seed()));double distance=Math.min(16,age*1.3);orb(b,ps,at,Math.max(.15,2.1-age*.09)*size,camera,time,fade);for(int i=0;i<rays;i++){double a=i*2.399963,y=Math.sin(i*1.7)*.6;Vec3 v=new Vec3(Math.cos(a),y,Math.sin(a)).normalize();line(b,ps,at.add(v.scale(distance*.22)),at.add(v.scale(distance)),.06*size,camera,fade);if(i%3==0)orb(b,ps,at.add(v.scale(distance)),.12*size,camera,time+i,fade);}for(int i=0;i<4;i++)ring(b,ps,at.add(0,(i-1.5)*.22,0),new Vec3(0,1,0),distance*(.55+i*.16),.12*size,camera,time+i,fade);}
            case 71 -> {for(int i=0;i<16;i++){double a=i*2.399;Vec3 ray=new Vec3(Math.cos(a),.25+Math.sin(i)*.2,Math.sin(a));line(b,ps,at,at.add(ray.scale(Math.min(2.8,age*.5))),.045,camera,fade);}ring(b,ps,at,new Vec3(0,1,0),Math.min(2,age*.3),.08,camera,time,fade);if(!dir.equals(Vec3.ZERO))rope(b,ps,at,dir,.025,camera,time,fade);}
            case 72 -> {for(int i=0;i<4;i++)ring(b,ps,at.add(0,.006*i,0),new Vec3(0,1,0),.2+i*.18,.15,camera,time*.25+i,fade*(e.seed()==0?.22:.55));}
            case 73 -> {line(b,ps,at,dir,.018,camera,fade);line(b,ps,at,dir,.055,camera,fade*.2);}
            case 74 -> {Vec3 center=at.lerp(dir,Math.min(1,age/3));crystal(b,ps,center,.4,camera,time,fade);rope(b,ps,at.subtract(dir.subtract(at).scale(1.5)),center,.085,camera,time,fade*.7);}
            case 9 -> {if(e.seed()<0){rope(b,ps,at,e.direction(),.06,camera,time,fade);break;}Entity target=Minecraft.getInstance().level.getEntity((int)e.seed());Vec3 end=target==null?e.direction():target.getBoundingBox().getCenter();Vec3 source=owner==null?at:owner.getEyePosition();rope(b,ps,source,end,.06,camera,time,fade);orb(b,ps,end,.18+.035*Math.sin(time*.5),camera,time,fade);}
            case 10 -> {Vec3 facing=owner==null?dir:owner.getLookAngle(),side=NetBranches.side(facing);for(int sign:new int[]{-1,1})for(int i=0;i<6;i++){Vec3 tip=at.add(side.scale(sign*(1.4+i*.22))).add(facing.scale(-.5-i*.18)).add(0,1.8-i*.15+Math.sin(time*.35)*.3,0);rope(b,ps,at.add(0,1.2,0),tip,.16,camera,time+i,fade);}}
            case 11 -> {if(e.seed()==1)for(int i=0;i<22;i++){double a=i*2.4,r=Math.sqrt((i+.5)/22.0)*size;orb(b,ps,at.add(Math.cos(a)*r,1.2+Math.sin(time*.07+i)*.5,Math.sin(a)*r),1.3,camera,time+i,fade*.38);}}
            case 12 -> {for(int i=0;i<4;i++){Vec3 base=at.add(Math.sin(i*2.7)*.5,0,Math.cos(i*2.7)*.5),tip=base.add(Math.sin(i)*.35,Math.min(3,age*.55),Math.cos(i)*.35);rope(b,ps,base,tip,.22,camera,time+i,fade);}}
            case 13 -> {if(e.seed()==0&&owner!=null){dir=owner.getLookAngle();at=owner.getEyePosition().add(dir);}ring(b,ps,at,dir,1.25,.18,camera,time,fade);for(int i=0;i<6;i++)ring(b,ps,at,dir,i*.2,.13,camera,time+i,fade*.6);}
            case 20,21,25 -> {for(int i=0;i<(skill==25?32:12);i++){double a=i*2.399,y=Math.sin(i*7.1);Vec3 d=new Vec3(Math.cos(a)*Math.sqrt(1-y*y),y,Math.sin(a)*Math.sqrt(1-y*y));double travel=age*(skill==25?.15:.07)*size;Vec3 p=at.add(d.scale(travel)).add(0,-age*age*.002,0);orb(b,ps,p,(skill==21?.10:.065)*size,camera,time+i,fade);}}
            case 22 -> {ring(b,ps,at,new Vec3(0,1,0),size*(.8+Math.sin(time*.1)*.05),.35,camera,time,fade*.65);}
            case 24 -> rope(b,ps,at,e.direction(),.045,camera,time,fade);
            case 26 -> {Vec3 p=owner==null?at:owner.getEyePosition().add(owner.getLookAngle().scale(.7));orb(b,ps,p,.26,camera,time,fade);}
            default -> {}
        }
    }
    private static void domain(BufferBuilder b,PoseStack ps,Visual visual,Vec3 camera,double time){
        Vec3 center=visual.e.position();double radius=100*Math.min(1,Math.max(.02,(time-visual.born)/12.0));double pulse=1+.0025*Math.sin(time*.15);radius*=pulse;
        // 封闭球壁使用不透明内外双面流体材质，不以稀疏粒子冒充结界。
        for(int lat=0;lat<24;lat++)for(int lon=0;lon<64;lon++){double a=-Math.PI/2+lat*Math.PI/24,z=a+Math.PI/24,u=lon*Math.PI*2/64,v=u+Math.PI*2/64;quad(b,ps,sphere(center,radius,a,u),sphere(center,radius,z,u),sphere(center,radius,z,v),sphere(center,radius,a,v),lon*.6,lat*.6,(lon+1)*.6,(lat+1)*.6,1,.7f);}
        // 血海分格波面；只在圆内绘制，不生成十万余个真实水方块。
        for(int x=-96;x<100;x+=8)for(int z=-96;z<100;z+=8){if(x*x+z*z>94*94)continue;Vec3 a=water(center,x,z,time),c=water(center,x+8,z,time),d=water(center,x+8,z+8,time),e=water(center,x,z+8,time);quad(b,ps,a,c,d,e,x*.13,z*.13,(x+8)*.13,(z+8)*.13,1,1);}
        // 血管仍走流动材质；眼睛留给独立的黑白通道。
        for(int i=0;i<96;i++){double a=i*2.399,latitude=Math.sin(i*1.7)*.95;Vec3 at=sphere(center,radius*.975,latitude,a),normal=center.subtract(at).normalize();Vec3 side=NetBranches.side(normal),up=normal.cross(side);rope(b,ps,at,at.add(up.scale(7)).add(side.scale(3)),.16,camera,time,1);}
        // 血雨只在相机附近采样，数量不随100格半径平方增长。
        if(BloodRules.inside(camera.distanceToSqr(center)))for(int i=0;i<200;i++){double x=Math.sin(i*67.1)*24,z=Math.cos(i*31.7)*24,y=18-((time*.38+i*.73)%24);Vec3 at=camera.add(x,y,z),attractor=DeathStarClient.attraction(at);if(attractor!=null){double pull=(time*.045+i*.137)%1;at=at.lerp(attractor,pull);line(b,ps,at,at.lerp(attractor,.18),.03,camera,.85);}else line(b,ps,at,at.add(.08,-.9,.03),.025,camera,.75);if(i<40){Vec3 bubble=water(center,camera.x-center.x+x,camera.z-center.z+z,time).add(0,.15+Math.abs(Math.sin(time*.07+i))*.4,0);orb(b,ps,bubble,.15,camera,time,.65);ring(b,ps,bubble.subtract(0,.2,0),new Vec3(0,1,0),.4+(time*.03+i)%1.2,.035,camera,time,.45);}}
    }
    private static void crystal(BufferBuilder b,PoseStack ps,Vec3 at,double r,Vec3 camera,double time,double fade){
        Vec3 top=at.add(0,r*1.2,0),bottom=at.add(0,-r,0);for(int i=0;i<6;i++){double a=i*Math.PI/3+time*.07,z=a+Math.PI/3;Vec3 p=at.add(Math.cos(a)*r,0,Math.sin(a)*r),q=at.add(Math.cos(z)*r,0,Math.sin(z)*r);quad(b,ps,top,p,q,top,0,0,1,1,fade,.65f+i*.05f);quad(b,ps,bottom,q,p,bottom,0,0,1,1,fade,.55f);}
    }
    private static void prediction(BufferBuilder b,PoseStack ps,Vec3 at,Vec3 velocity,float scale,Vec3 camera,double time){
        Vec3 v=velocity.lengthSqr()>1?velocity.normalize():velocity;double horizon=12*scale;Vec3 previous=at.add(0,.1,0);
        for(int i=1;i<=12;i++){double t=i*horizon/12;Vec3 next=at.add(v.scale(t)).add(0,.1,0);line(b,ps,previous,next,.028,camera,1-i*.05);if(i%3==0)ring(b,ps,next,new Vec3(0,1,0),.22,.025,camera,time,.65);previous=next;}
        if(v.lengthSqr()<.0001)ring(b,ps,at.add(0,.1,0),new Vec3(0,1,0),.4,.025,camera,time,.7);
    }
    private static void eyes(BufferBuilder b,PoseStack ps,Visual visual,Vec3 camera,double time){
        Vec3 center=visual.e.position();double radius=97.5*Math.min(1,Math.max(.02,(time-visual.born)/12.0));
        for(int i=0;i<96;i++){Vec3 at=sphere(center,radius,Math.sin(i*1.7)*.95,i*2.399),normal=center.subtract(at).normalize();Vec3 side=NetBranches.side(normal),up=normal.cross(side).normalize();double blink=.12+.88*Math.abs(Math.sin(time*.025+i));
            ellipse(b,ps,at,side,up,2.45,1.16*blink,0xFF560819);ellipse(b,ps,at.add(normal.scale(.025)),side,up,2.2,blink,0xFFF9F9F3);
            Vec3 gaze=camera.subtract(at).normalize();Vec3 pupil=at.add(normal.scale(.055)).add(side.scale(Math.max(-.7,Math.min(.7,gaze.dot(side))))).add(up.scale(gaze.dot(up)*.2*blink));
            ellipse(b,ps,pupil,side,up,.54,.77*blink,0xFF08080B);ellipse(b,ps,pupil.add(normal.scale(.015)).add(side.scale(-.16)).add(up.scale(.22*blink)),side,up,.1,.12*blink,0xFFFDFDFF);
        }
    }
    private static void ellipse(BufferBuilder b,PoseStack ps,Vec3 at,Vec3 side,Vec3 up,double width,double height,int color){for(int i=0;i<32;i++){double a=i*Math.PI/16,z=(i+1)*Math.PI/16;Client.quad(b,ps,at,at.add(side.scale(Math.cos(a)*width)).add(up.scale(Math.sin(a)*height)),at.add(side.scale(Math.cos(z)*width)).add(up.scale(Math.sin(z)*height)),at,color);}}
    private static void ocean(BufferBuilder b,PoseStack ps,Visual visual,Vec3 camera,double time){
        Vec3 center=visual.e.position();double age=time-visual.born,radius=BloodRules.oceanRadius((int)age);if(radius<.1)return;
        // 单张逻辑海面只在镜头周围绘制细波，不创建水方块或数万个实体。
        int minX=(int)Math.max(-100,Math.floor(camera.x-center.x-60)),maxX=(int)Math.min(100,Math.ceil(camera.x-center.x+60));
        int minZ=(int)Math.max(-100,Math.floor(camera.z-center.z-60)),maxZ=(int)Math.min(100,Math.ceil(camera.z-center.z+60));
        for(int x=minX;x<maxX;x+=4)for(int z=minZ;z<maxZ;z+=4){if((x+2)*(x+2)+(z+2)*(z+2)>radius*radius)continue;Vec3 a=water(center,x,z,time),c=water(center,x+4,z,time),d=water(center,x+4,z+4,time),e=water(center,x,z+4,time);quad(b,ps,a,c,d,e,x*.2,z*.2,(x+4)*.2,(z+4)*.2,.7,.65f);}
        ring(b,ps,center.add(0,.12,0),new Vec3(0,1,0),radius,.23,camera,time,.9);
        if(age>=400){int cursor=(int)visual.e.direction().y,front=Math.min(40401,cursor+(int)((time-(visual.end-500))*256));for(int n=0;n<96;n++){int cell=Math.max(0,front-n*5),x=cell%201-100,z=cell/201-100;if(x*x+z*z>10000)continue;Vec3 at=water(center,x,z,time);if(at.distanceToSqr(camera)>80*80)continue;double f=(time*.25+n*.37)%1;for(int ray=0;ray<5;ray++){double a=ray*Math.PI*2/5;line(b,ps,at,at.add(Math.cos(a)*f*2,.3+f*2,Math.sin(a)*f*2),.025,camera,1-f);}}}
    }
    private static Vec3 sphere(Vec3 c,double r,double a,double u){return c.add(Math.cos(a)*Math.cos(u)*r,Math.sin(a)*r,Math.cos(a)*Math.sin(u)*r);}
    private static Vec3 water(Vec3 c,double x,double z,double time){return c.add(x,.035+Math.sin(x*.17+time*.07)*.08+Math.cos(z*.21-time*.05)*.06,z);}
    private static void heart(BufferBuilder b,PoseStack ps,Vec3 at,Vec3 camera,double time){Vec3 normal=camera.subtract(at).normalize(),side=NetBranches.side(normal),up=normal.cross(side).normalize();double size=.027*(1+.12*Math.sin(time*.55));for(int i=0;i<32;i++){double a=i*Math.PI*2/32,z=(i+1)*Math.PI*2/32;Vec3 p=at.add(side.scale(16*Math.pow(Math.sin(a),3)*size)).add(up.scale((13*Math.cos(a)-5*Math.cos(2*a)-2*Math.cos(3*a)-Math.cos(4*a))*size)),q=at.add(side.scale(16*Math.pow(Math.sin(z),3)*size)).add(up.scale((13*Math.cos(z)-5*Math.cos(2*z)-2*Math.cos(3*z)-Math.cos(4*z))*size));line(b,ps,p,q,.04,camera,1);}}
    private static void orb(BufferBuilder b,PoseStack ps,Vec3 at,double radius,Vec3 camera,double time,double alpha){Vec3 n=camera.subtract(at).normalize(),side=NetBranches.side(n),up=n.cross(side).normalize();for(int i=0;i<16;i++){double a=i*Math.PI*2/16,z=(i+1)*Math.PI*2/16,ra=radius*(1+.08*Math.sin(time*.2+i));Vec3 p=at.add(side.scale(Math.cos(a)*ra)).add(up.scale(Math.sin(a)*ra)),q=at.add(side.scale(Math.cos(z)*ra)).add(up.scale(Math.sin(z)*ra));quad(b,ps,at,p,q,at,.5,.5,1,1,alpha,1);}}
    private static void ring(BufferBuilder b,PoseStack ps,Vec3 at,Vec3 normal,double radius,double width,Vec3 camera,double time,double alpha){Vec3 side=NetBranches.side(normal.normalize()),up=normal.normalize().cross(side);if(up.lengthSqr()<.1){side=new Vec3(1,0,0);up=new Vec3(0,0,1);}for(int i=0;i<40;i++){double a=i*Math.PI*2/40,z=(i+1)*Math.PI*2/40;double r=radius*(1+.025*Math.sin(time*.25+i*1.7));Vec3 p=at.add(side.scale(Math.cos(a)*r)).add(up.scale(Math.sin(a)*r)),q=at.add(side.scale(Math.cos(z)*r)).add(up.scale(Math.sin(z)*r));line(b,ps,p,q,width,camera,alpha);}}
    private static void arc(BufferBuilder b,PoseStack ps,Vec3 at,Vec3 dir,double radius,double width,Vec3 camera,double time,double alpha){Vec3 side=NetBranches.side(dir),up=new Vec3(0,1,0);for(int i=0;i<24;i++){double a=-1.2+i*.1,z=a+.1;line(b,ps,at.add(side.scale(Math.sin(a)*radius)).add(up.scale(Math.cos(a)*radius)),at.add(side.scale(Math.sin(z)*radius)).add(up.scale(Math.cos(z)*radius)),width*Math.sin((i+.5)/24*Math.PI),camera,alpha);}}
    private static void rope(BufferBuilder b,PoseStack ps,Vec3 start,Vec3 end,double width,Vec3 camera,double time,double alpha){Vec3 axis=end.subtract(start),side=NetBranches.side(axis.normalize());int count=Math.max(4,Math.min(48,(int)(axis.length()*3)));Vec3 previous=start;for(int i=1;i<=count;i++){double t=i/(double)count;Vec3 p=start.lerp(end,t).add(side.scale(Math.sin(t*18-time*.22)*Math.sin(t*Math.PI)*width*1.8));line(b,ps,previous,p,width*(.86+.14*Math.sin(time*.3+i)),camera,alpha);previous=p;}}
    private static void line(BufferBuilder b,PoseStack ps,Vec3 start,Vec3 end,double width,Vec3 camera,double alpha){Vec3 axis=end.subtract(start);if(axis.lengthSqr()<1e-10)return;Vec3 side=axis.cross(camera.subtract(start)).normalize().scale(width);quad(b,ps,start.subtract(side),end.subtract(side),end.add(side),start.add(side),0,0,Math.max(.1,axis.length()),1,alpha,1);}
    private static void quad(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 c,Vec3 d,Vec3 e,double u0,double v0,double u1,double v1,double alpha,float tint){vertex(b,ps,a,u0,v0,alpha,tint);vertex(b,ps,c,u1,v0,alpha,tint);vertex(b,ps,d,u1,v1,alpha,tint);vertex(b,ps,e,u0,v1,alpha,tint);}
    private static void vertex(BufferBuilder b,PoseStack ps,Vec3 p,double u,double v,double alpha,float tint){b.vertex(ps.last().pose(),(float)p.x,(float)p.y,(float)p.z).color(tint,tint,tint,(float)Math.max(0,Math.min(1,alpha))).uv((float)u,(float)v).endVertex();}
    private BloodClient(){}
}
