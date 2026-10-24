package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** 终式表现按服务端年龄续租；只移动本机镜头，不替换相机实体或修改全局按键。 */
public final class FinaleClient {
    private static final class State {
        Network.Effect e;long received;float age;boolean shattered;
        State(Network.Effect e){this.e=e;received=tick;age=e.size();}
    }
    private record Visual(Network.Effect e,long born){}
    private record Scar(long seed,long end,Network.Effect appearance){}
    private static final Map<Integer,State> states=new HashMap<>();
    private static final List<Visual> effects=new ArrayList<>();
    private static final Map<Integer,Scar> scars=new HashMap<>();
    private static final List<Vec3> absorbed=new ArrayList<>();
    private static Network.Effect absorbAppearance;
    private static CameraType savedCamera;
    private static final java.lang.reflect.Method CAMERA_POSITION=Arrays.stream(Camera.class.getDeclaredMethods()).filter(m->m.getReturnType()==void.class&&Arrays.equals(m.getParameterTypes(),new Class<?>[]{Vec3.class})).findFirst().map(m->{m.setAccessible(true);return m;}).orElse(null);
    private static Object level;private static long tick,muteUntil,bellsUntil,titleUntil,whiteUntil;private static Vec3 absorbHand=Vec3.ZERO;private static long absorbStart=-100;
    public static Network.CombatState combat=new Network.CombatState(false,0,false,0,0);
    public static boolean active(int id){return states.containsKey(id);}
    public static boolean local(){var p=Minecraft.getInstance().player;return p!=null&&active(p.getId());}
    public static float age(int id){State s=states.get(id);return s==null?-1:s.age;}
    public static int variant(int id){State s=states.get(id);return s==null?0:(int)s.e.direction().y;}
    public static boolean ready(){return combat.finaleReady()&&!local();}
    public static void state(Network.CombatState state){combat=state;}
    public static void receive(Network.Effect e){
        var mc=Minecraft.getInstance();if(mc.level==null)return;if(level!=mc.level){clear();level=mc.level;}
        if(e.kind()==FinaleRules.FX_STATE){
            State s=states.get(e.owner());if(e.life()<=0){if(s!=null&&s.e.seed()==e.seed())states.remove(e.owner());if(mc.player!=null&&e.owner()==mc.player.getId())restoreCamera();return;}
            if(s==null||s.e.seed()!=e.seed()){s=new State(e);states.put(e.owner(),s);}else{s.e=e;s.received=tick;s.age=Math.max(s.age,e.size());}
            if(mc.player!=null&&e.owner()==mc.player.getId())camera(s,mc.getFrameTime());return;
        }
        if(e.kind()==FinaleRules.FX_SCAR){scars.put(e.owner(),new Scar(e.seed(),tick+e.life(),e));return;}
        if(e.kind()==FinaleRules.FX_ABSORB){
            absorbAppearance=e;absorbed.clear();absorbStart=tick;Entity owner=mc.level.getEntity(e.owner());absorbHand=owner==null?e.position().add(0,1.2,0):owner.getEyePosition().add(owner.getLookAngle().scale(.7)).add(0,-.35,0);
            Client.collectSlashes(e.position(),e.size(),absorbed);SlashFormVisuals.collect(e.position(),e.size(),absorbed);ExpansionVisuals.collect(e.position(),e.size(),absorbed);BranchVisuals.collect(e.position(),e.size(),absorbed);NetBranchVisuals.collect(e.position(),e.size(),absorbed);VoidTechniqueClient.collect(e.position(),e.size(),absorbed);SoulClient.collect(e.position(),e.size(),absorbed);VoidVisuals.collect(e.position(),e.size(),absorbed);
            return;
        }
        if(effects.size()<4096)effects.add(new Visual(e,tick));
    }
    public static boolean absorbing(Vec3 p){return tick-absorbStart<14&&p.distanceToSqr(absorbHand)<=FinaleRules.RADIUS*FinaleRules.RADIUS;}
    private static void restoreCamera(){
        var mc=Minecraft.getInstance();if(savedCamera!=null)mc.options.setCameraType(savedCamera);
        savedCamera=null;
    }
    public static void clear(){restoreCamera();FinaleScreenGlass.clear();states.clear();effects.clear();scars.clear();absorbed.clear();absorbAppearance=null;combat=new Network.CombatState(false,0,false,0,0);level=null;muteUntil=bellsUntil=titleUntil=whiteUntil=0;absorbStart=-100;}
    private static void camera(State s,float partial){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        if(savedCamera==null)savedCamera=mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        // Minecraft.cameraEntity 和 Camera.entity 均保留 LocalPlayer：移动与本机玩家绘制都依赖它。
    }
    @SubscribeEvent public static void renderTick(TickEvent.RenderTickEvent e){if(e.phase!=TickEvent.Phase.START)return;var p=Minecraft.getInstance().player;if(p!=null){State s=states.get(p.getId());if(s!=null)camera(s,e.renderTickTime);}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||level!=null&&level!=mc.level){clear();level=mc.level;return;}if(mc.isPaused())return;tick++;
        if(combat.noRecovery()>0||combat.finaleBurnout()>0)combat=new Network.CombatState(combat.finaleReady(),combat.lingers(),combat.stream(),Math.max(0,combat.noRecovery()-1),Math.max(0,combat.finaleBurnout()-1));
        for(var entry:List.copyOf(states.entrySet())){
            State s=entry.getValue();Entity owner=mc.level.getEntity(entry.getKey());
            if(owner==null||!owner.isAlive()||tick-s.received>30||s.age>FinaleRules.END+10){states.remove(entry.getKey());continue;}
            s.age++;
            if((int)s.age==FinaleRules.WHITE&&s.shattered)mc.getSoundManager().stop();
            if(s.age>=FinaleRules.SHATTER&&!s.shattered&&mc.player.distanceToSqr(s.e.position())<=512*512){s.shattered=true;mc.getSoundManager().stop();mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GLASS_BREAK,.65f,.7f));muteUntil=tick+50;whiteUntil=tick+50;titleUntil=tick+120;bellsUntil=tick+100;}
        }
        if(!local())restoreCamera();
        if(states.values().stream().noneMatch(s->s.age>=FinaleRules.SHATTER&&s.age<FinaleRules.WHITE))FinaleScreenGlass.clear();
        effects.removeIf(v->tick-v.born>=v.e.life());scars.values().removeIf(s->tick>=s.end);if(tick-absorbStart>=14)absorbed.clear();
        if(tick>=muteUntil&&tick<bellsUntil&&tick%2==0){
            // 用错拍、不同音高制造密集钟群，不提高主音量，不修改玩家的音量设置。
            for(int i=0;i<3;i++){float pitch=.5f+(float)((tick*7+i*11)%31)/20;mc.getSoundManager().play(SimpleSoundInstance.forUI(i==0?SoundEvents.BELL_RESONATE:SoundEvents.BELL_BLOCK,pitch,.14f));}
        }
    }
    @SubscribeEvent public static void sound(PlaySoundEvent e){if(tick<muteUntil)e.setSound(null);}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    @SubscribeEvent public static void input(MovementInputUpdateEvent e){
        var mc=Minecraft.getInstance();if(e.getEntity()!=mc.player||!local()||age(mc.player.getId())<FinaleRules.CHARGE)return;
        var input=e.getInput();input.forwardImpulse=input.leftImpulse=0;input.up=input.down=input.left=input.right=input.jumping=input.shiftKeyDown=false;
    }
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered e){if(local()){e.setCanceled(true);e.setSwingHand(false);}}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void angles(ViewportEvent.ComputeCameraAngles e){
        if(!local())return;var mc=Minecraft.getInstance();State s=states.get(mc.player.getId());float partial=(float)e.getPartialTick();camera(s,partial);
        double age=s.age+partial;var shot=FinaleCameraRules.shot(age);Vec3 at=mc.player.getPosition(partial);
        double yaw=Math.toRadians(age>=FinaleRules.TRIGGERS[1]&&age<FinaleRules.STARTS[2]?mc.player.yBodyRot:s.e.direction().x);
        Vec3 forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw)),side=new Vec3(Math.cos(yaw),0,Math.sin(yaw));
        Vec3 wanted=at.add(forward.scale(shot.forward())).add(side.scale(shot.side())).add(0,shot.height(),0),focus=at.add(0,shot.focusHeight(),0);
        // 从玩家肩部检查碰撞；空间足够时严格使用分镜位置，防止机位埋进实心方块。
        Vec3 shoulder=at.add(0,1.3,0);var hit=mc.level.clip(new net.minecraft.world.level.ClipContext(shoulder,wanted,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player));
        if(hit.getType()!=HitResult.Type.MISS)wanted=hit.getLocation().add(shoulder.subtract(wanted).normalize().scale(.3));
        e.getCamera().setup(mc.level,mc.player,true,false,partial);
        if(CAMERA_POSITION!=null)try{CAMERA_POSITION.invoke(e.getCamera(),wanted);}catch(ReflectiveOperationException ignored){wanted=e.getCamera().getPosition();}
        Vec3 direction=focus.subtract(wanted);double shake=FinaleCameraRules.shake(age);
        e.setYaw((float)Math.toDegrees(Math.atan2(-direction.x,direction.z))+(float)shake*.4f);
        e.setPitch((float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z)))+(float)shake);e.setRoll((float)shake*.5f);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void fov(ViewportEvent.ComputeFov e){if(local()&&e.usedConfiguredFov())e.setFOV(FinaleCameraRules.shot(age(Minecraft.getInstance().player.getId())+e.getPartialTick()).fov());}
    @SubscribeEvent public static void models(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;var mc=Minecraft.getInstance();if(mc.level==null)return;
        Vec3 cam=e.getCamera().getPosition();PoseStack ps=e.getPoseStack();MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
        for(State s:states.values())if(s.age>=FinaleRules.TRIGGERS[0]&&s.age<FinaleRules.TRIGGERS[0]+12){
            float age=s.age+e.getPartialTick()-FinaleRules.TRIGGERS[0];double height=Math.max(0,36*(1-age/12));
            ps.pushPose();ps.translate(s.e.position().x-cam.x,s.e.position().y+height-cam.y,s.e.position().z-cam.z);ps.mulPose(Axis.YP.rotationDegrees((float)s.e.direction().x));ps.scale(2.5f,2.5f,2.5f);ShrineModel.render(ps,buffers,false,true,age);ps.popPose();
        }
        buffers.endBatch();
    }
    private static Vec3 hand(Entity owner,float partial){return owner.getPosition(partial).add(0,owner.getBbHeight()*.7,0).add(owner.getLookAngle().scale(.65));}
    private static void sphere(BufferBuilder b,PoseStack ps,Vec3 p,double r,int color){for(int y=0;y<8;y++)for(int i=0;i<16;i++){double a=i*Math.PI/8,z=(i+1)*Math.PI/8,h=-Math.PI/2+y*Math.PI/8,H=h+Math.PI/8;Client.quad(b,ps,point(p,r,a,h),point(p,r,z,h),point(p,r,z,H),point(p,r,a,H),color);}}
    private static Vec3 point(Vec3 p,double r,double a,double h){return p.add(Math.cos(a)*Math.cos(h)*r,Math.sin(h)*r,Math.sin(a)*Math.cos(h)*r);}
    private static void cut(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,Vec3 cam,double width,float fade){SlashStyle.draw(b,ps,a,z,width,fade,cam);}
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 cam,float partial){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        for(State s:states.values()){
            SlashAppearance.use(s.e);Entity owner=mc.level.getEntity(s.e.owner());if(owner==null)continue;double age=s.age+partial;SlashAppearance.age(age);
            if(age<FinaleRules.TRIGGERS[0]){
                Vec3 center=owner.getPosition(partial).add(0,5,0);double charge=Math.min(1,age/FinaleRules.CHARGE);sphere(b,ps,center,.25+charge*1.1,0xECFFFFFF);
                for(int i=0;i<100;i++){double a=i*2.399963,t=(age*.025+i*.031)%1,r=(1-t)*(4+charge*9),y=Math.sin(i*1.7)*r*.7;Vec3 at=center.add(Math.cos(a+age*.01)*r,y,Math.sin(a+age*.01)*r);Vec3 axis=center.subtract(at).normalize();cut(b,ps,at.subtract(axis.scale(.5+charge)),at.add(axis.scale(.5+charge)),cam,.09,(float)(.4+.6*t));}
            }
            if(age>=FinaleRules.TRIGGERS[4]&&age<FinaleRules.SHATTER){Vec3 h=hand(owner,partial);sphere(b,ps,h,.045,0xFFFFFFFF);}
        }
        for(Visual v:effects){Network.Effect e=v.e;SlashAppearance.use(e);double age=tick-v.born+partial;SlashAppearance.age(age);float fade=(float)Math.min(1,(e.life()-age)/3);Vec3 p=e.position();
            if(e.kind()==FinaleRules.FX_SHRINE){double r=Math.min(FinaleRules.RADIUS,age*10);for(int j=0;j<3;j++)Client.ring(b,ps,p.add(0,.15+j*.3,0),Math.max(.1,r-j*2),.3,((int)(fade*200)<<24)|0xFFFFFF,cam);}
            if(e.kind()==FinaleRules.FX_WALL){double y=-age*2;for(int i=-150;i<=150;i++){cut(b,ps,p.add(i,y,-150),p.add(i,y,150),cam,.12,fade);cut(b,ps,p.add(-150,y,i),p.add(150,y,i),cam,.12,fade);}}
            if(e.kind()==FinaleRules.FX_WORLD){double radius=Math.min(FinaleRules.RADIUS,(age+1)*15);for(int i=0;i<96;i++){double a=i*Math.PI/48,z=(i+1)*Math.PI/48;WallpaperSlash.draw(b,ps,p.add(Math.cos(a)*radius,.08,Math.sin(a)*radius),p.add(Math.cos(z)*radius,.08,Math.sin(z)*radius),new Vec3(0,1,0),fade,cam,e.wallpaperOwner());if(i%2==0)WallpaperSlash.drawBillboard(b,ps,p.add(0,.04,0),p.add(Math.cos(a)*radius,.04,Math.sin(a)*radius),fade,cam,e.wallpaperOwner());}}
            if(e.kind()==FinaleRules.FX_COMBO){
                int kind=(int)e.size();Vec3 from=e.direction(),dir=p.subtract(from).normalize(),side=NetBranches.side(dir),up=side.cross(dir).normalize();
                if(kind==7){Vec3 last=p.add(0,14,0);for(int i=0;i<10;i++){Vec3 next=p.add(Math.sin(i*7+e.seed())*.5,14-i*1.55,Math.cos(i*4+e.seed())*.5);Client.ribbon(b,ps,last,next,.07,0xFFDAF7FF,cam);last=next;}}
                else if(kind==8){Vec3 center=from.lerp(p,Math.min(1,age/5));cut(b,ps,center.subtract(side.scale(.7)),center.add(side.scale(.7)),cam,.14,fade);}
                else{
                    Vec3 center=(e.seed()%2==0)?p:from.lerp(p,Math.min(1,age/3));
                    if(kind==2){for(var segment:SlashFormRules.shape((int)e.seed())){Vec3 a=center.add(side.scale(segment.x1())).add(up.scale(segment.y1())),z=center.add(side.scale(segment.x2())).add(up.scale(segment.y2()));cut(b,ps,a,z,cam,.14,fade);}}
                    else if(kind==4){for(int i=0;i<12;i++){double a=i*Math.PI/6,z=(i+1)*Math.PI/6;Vec3 axis=new Vec3(Math.cos(a),0,Math.sin(a));cut(b,ps,p.subtract(axis.scale(4)),p.add(axis.scale(4)),cam,.09,fade);for(int r=1;r<=4;r++)cut(b,ps,p.add(Math.cos(a)*r,0,Math.sin(a)*r),p.add(Math.cos(z)*r,0,Math.sin(z)*r),cam,.07,fade);}}
                    else{int n=switch(kind){case 1,3->2;case 5->6;default->1;};for(int i=0;i<n;i++){double angle=(i*Math.PI/n)+(e.seed()%11)*.2;Vec3 axis=side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));cut(b,ps,center.subtract(axis.scale(kind==6?10:3)),center.add(axis.scale(kind==6?10:3)),cam,.18,fade);}}
                }
            }
        }
        if(absorbAppearance!=null)SlashAppearance.use(absorbAppearance);else SlashAppearance.reset();
        double gather=(tick-absorbStart+partial)/14.0;
        if(gather>=0&&gather<1)for(Vec3 start:absorbed){Vec3 at=start.lerp(absorbHand,gather*gather),axis=absorbHand.subtract(start).normalize();cut(b,ps,at.subtract(axis.scale((1-gather)*1.5)),at.add(axis.scale((1-gather)*1.5)),cam,.1*(1-gather),1);}
        for(var entry:scars.entrySet()){
            SlashAppearance.use(entry.getValue().appearance);Entity t=mc.level.getEntity(entry.getKey());if(t==null||!t.isAlive())continue;Vec3 center=t.getPosition(partial).add(0,t.getBbHeight()*.55,0),toward=cam.subtract(center).normalize(),side=NetBranches.side(toward);double surface=t.getBbWidth()*.65;
            center=center.add(toward.scale(surface));for(int i=0;i<3;i++){Vec3 axis=side.scale(Math.cos(i*1.1+.2)).add(0,Math.sin(i*1.1+.2),0).normalize();double length=Math.min(t.getBbHeight(),t.getBbWidth()*1.5);Vec3 a=center.subtract(axis.scale(length*.5)).add(0,(i-1)*.15,0),z=center.add(axis.scale(length*.5)).add(0,(i-1)*.15,0);if(SlashAppearance.desktop())SlashStyle.draw(b,ps,a,z,.04,1,cam);else Client.ribbon(b,ps,a,z,.018,0xFFFFFFFF,cam);}
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hud(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();if(mc.player==null)return;GuiGraphics g=e.getGuiGraphics();int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        if(Client.progress.sukuna()&&!mc.options.hideGui&&mc.screen==null){
            String text="断流 ["+Client.STREAM.getTranslatedKeyMessage().getString()+"]："+(combat.stream()?"开启":"关闭");int x=w-mc.font.width(text)-10;g.fill(x-5,8,w-5,25,0xAD100D18);g.drawString(mc.font,text,x,12,combat.stream()?0xFFFFDA91:0xFFC9BDCF);
            if(combat.noRecovery()>0)g.drawString(mc.font,"咒力恢复暂停 "+((combat.noRecovery()+19)/20)+"秒",w-145,30,0xFFE3C3FF);
            if(combat.finaleBurnout()>0)g.drawString(mc.font,"终式熔断 "+((combat.finaleBurnout()+19)/20)+"秒",w-145,42,0xFFFF8D9B);
        }
        State local=states.get(mc.player.getId());if(local!=null&&local.age<FinaleRules.CHARGE){float f=local.age/FinaleRules.CHARGE;g.fill(w/2-80,h/2+35,w/2+80,h/2+41,0xAA17131B);g.fill(w/2-80,h/2+35,w/2-80+(int)(160*f),h/2+41,0xFFF2E5C3);g.drawCenteredString(mc.font,"终式蓄力 "+String.format(Locale.ROOT,"%.1f",Math.max(0,8-local.age/20))+"秒 · 可移动",w/2,h/2+46,0xFFFFFFFF);}
        if(mc.screen==null)overlay(g,e.getPartialTick());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void screen(ScreenEvent.Render.Post e){overlay(e.getGuiGraphics(),e.getPartialTick());}
    private static void overlay(GuiGraphics g,float partial){
        var mc=Minecraft.getInstance();if(mc.player==null)return;int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        g.pose().pushPose();g.pose().translate(0,0,1000);
        State finalState=states.values().stream().filter(s->s.age>=FinaleRules.TRIGGERS[4]&&mc.player.distanceToSqr(s.e.position())<=512*512).max(Comparator.comparingDouble(s->s.age)).orElse(null);
        if(finalState!=null){
            double age=finalState.age+partial;
            if(age<FinaleRules.SHATTER){int a=(int)(255*Math.min(1,(age-FinaleRules.TRIGGERS[4])/14));g.fill(0,0,w,h,a<<24);g.fill(w/2-1,h/2-1,w/2+2,h/2+2,0xFFFFFFFF);}
            else if(age<FinaleRules.WHITE){FinaleScreenGlass.draw(g,w,h,age-FinaleRules.SHATTER,finalState.e.seed());glass(g,w,h,age-FinaleRules.SHATTER);}
            else if(age<FinaleRules.END){FinaleScreenGlass.clear();g.fill(0,0,w,h,0xFFFFFFFF);}
        }
        if(tick<whiteUntil&&whiteUntil-tick<=40)g.fill(0,0,w,h,0xFFFFFFFF);
        if(tick>=muteUntil&&tick<titleUntil){int a=(int)(255*Math.min(1,(titleUntil-tick)/20.0));g.drawCenteredString(mc.font,"六道·連斬·終式",w/2,h/2-4,(a<<24)|0xE8BD53);}
        g.flush();g.pose().popPose();
    }
    /** (2,1)二阶振型：中央节点、左右双瓣；碎片沿振型正负方向反向翘起。 */
    private static void glass(GuiGraphics g,int w,int h,double age){
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);PoseStack ps=g.pose();
        for(var shard:GlassGeometryRules.shards(w,h,age)){
            Vec3 a=new Vec3(shard.a().x(),shard.a().y(),0),c=new Vec3(shard.b().x(),shard.b().y(),0),z=new Vec3(shard.c().x(),shard.c().y(),0);
            int alpha=(int)(42*Math.max(0,1-age/10)),color=switch(shard.shade()){case 0->0xADB9CD;case 1->0xE8F2FF;default->0x748AA4;};
            Client.quad(b,ps,a,c,z,z,(alpha<<24)|color);guiLine(b,ps,a,c,0xFFBFCFDD);guiLine(b,ps,c,z,0xFFA2B5C8);guiLine(b,ps,z,a,0xFFE7F2FF);
        }
        BufferUploader.drawWithShader(b.end());RenderSystem.enableCull();RenderSystem.disableBlend();
    }
    private static void guiLine(BufferBuilder b,PoseStack ps,Vec3 a,Vec3 z,int color){Vec3 side=new Vec3(-(z.y-a.y),z.x-a.x,0).normalize().scale(.45);Client.quad(b,ps,a.add(side),z.add(side),z.subtract(side),a.subtract(side),color);}
    private FinaleClient(){}
}
