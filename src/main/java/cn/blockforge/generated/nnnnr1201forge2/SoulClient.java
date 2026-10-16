package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import static cn.blockforge.generated.nnnnr1201forge2.Client.*;

@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class SoulClient {
    private record Visual(Network.Effect e,long start){}
    private record Domain(Vec3 center,long end,long born,float radius,boolean station){}
    private static final List<Visual> effects=new ArrayList<>();
    private static final Map<Integer,Domain> domains=new HashMap<>();
    private static long time,gapEnd,crackEnd,whiteEnd;private static int gapBoss,layers,stacks;
    public static void collect(Vec3 center,double radius,List<Vec3> out){effects.removeIf(v->{if(v.e.kind()!=93&&v.e.kind()!=125&&v.e.kind()!=126||v.e.position().distanceToSqr(center)>radius*radius)return false;if(out.size()<6000)out.add(v.e.position());return true;});}
    public static void clear(){effects.clear();domains.clear();time=gapEnd=crackEnd=whiteEnd=0;layers=stacks=0;}
    public static void tick(){time++;effects.removeIf(v->time-v.start>=v.e.life());domains.values().removeIf(d->time>=d.end);BlackFlashRender.tick();}
    public static void receive(Network.Effect e){var mc=Minecraft.getInstance();if(mc.player==null)return;
        if(e.kind()==89){layers=(int)e.seed();stacks=e.life();return;}
        if(e.kind()==82&&e.owner()==mc.player.getId()){gapEnd=time+e.life();gapBoss=(int)e.seed();return;}
        if(e.kind()==85){BlackFlashRender.play(e.position(),e.direction());if(e.seed()==mc.player.getId())crackEnd=time+e.life();}
        if(e.kind()==90){Domain old=domains.get(e.owner());if(old==null&&mc.player.distanceToSqr(e.position())<48*48)whiteEnd=time+7;domains.put(e.owner(),new Domain(e.position(),time+e.life(),old==null?time:old.born,e.size(),e.seed()==1));return;}
        if(e.kind()==91){domains.remove(e.owner());return;}
        if(e.kind()==94)return;
        if(effects.size()<200)effects.add(new Visual(e,time));
    }
    private static Domain inside(){var p=Minecraft.getInstance().player;if(p==null)return null;return domains.values().stream().filter(d->p.position().distanceToSqr(d.center)<d.radius*d.radius).findFirst().orElse(null);}
    @SubscribeEvent public static void hideOthers(RenderLivingEvent.Pre<?,?> e){if(time<gapEnd&&e.getEntity()!=Minecraft.getInstance().player&&e.getEntity().getId()!=gapBoss)e.setCanceled(true);}
    @SubscribeEvent public static void fog(ViewportEvent.ComputeFogColor e){if(time<gapEnd){e.setRed(0.005f);e.setGreen(0.005f);e.setBlue(0.012f);}else if(inside()!=null){e.setRed(0.88f);e.setGreen(0.78f);e.setBlue(0.63f);}}
    @SubscribeEvent public static void fogDistance(ViewportEvent.RenderFog e){if(inside()!=null&&e.getType()==net.minecraft.world.level.material.FogType.NONE){e.setNearPlaneDistance(25);e.setFarPlaneDistance(65);e.setCanceled(true);}}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui)return;int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();var g=e.getGuiGraphics();
        if(progress.nine())g.drawString(mc.font,"咒胎受肉 "+layers+" / 9"+(stacks>0?"  灵魂僵直 "+stacks+"层 · 移速 -"+(stacks*15)+"% · 冷却 +"+(stacks*10)+"%":""),12,h-144,0xFFC7DCE7);
        if(time<gapEnd){g.fill(0,0,w,h,0x7003060C);g.drawCenteredString(mc.font,"灵魂间隙 · 禁止领域与治疗 · "+((gapEnd-time+19)/20)+"秒",w/2,h/2-50,0xFFE1E2EA);}
        Domain d=inside();if(d!=null)g.drawCenteredString(mc.font,"领域展开 · 岩手观光 · "+((d.end-time+19)/20)+"秒",w/2,36,0xFFFFE5B7);
        // 黑闪的屏幕演出：**以准星（屏幕正中）为圆心，黑色火花围成一个圆环向外扩散，圆心处再簇拥一小撮火花**。
        // 每颗火花＝外面一层红色辉光 + 里面一颗近黑火花，逐颗独立闪烁（波谷整颗跳过）做出"噼啪炸开"的张力。
        if(time<crackEnd){ScreenSeverance.draw(g,37,(crackEnd-time)/16f);
            float life=Math.max(0,Math.min(1,(crackEnd-time)/16f));
            int cx=w/2,cy=h/2;
            double rad=(0.10+0.90*(1-life))*Math.min(w,h)*0.62;      // 半径随演出扩散
            int dots=28;
            for(int i=0;i<dots;i++){
                double ang=i*Math.PI*2/dots+time*0.05;
                int x=(int)(cx+Math.cos(ang)*rad),y=(int)(cy+Math.sin(ang)*rad);
                if(Math.sin(time*1.9+i*3.1)<-0.30)continue;           // 整颗跳过 = 闪烁
                int a=(int)((0.30+0.70*Math.abs(Math.sin(time*1.9+i*3.1)))*225*life);
                g.fill(x-4,y-4,x+4,y+4,((int)(a*0.35)<<24)|0xFF2A18);   // 红色辉光（光污染）
                g.fill(x-2,y-2,x+2,y+2,(a<<24)|0x0A0A0A);               // 近黑火花
            }
            for(int i=0;i<10;i++){                                   // 圆心：一小撮火花
                double ang=i*Math.PI*2/10+time*0.11,rr=6+((i%3)*5);
                int x=(int)(cx+Math.cos(ang)*rr),y=(int)(cy+Math.sin(ang)*rr);
                if(Math.sin(time*2.3+i*1.7)<-0.25)continue;
                int a=(int)(215*life);
                g.fill(x-3,y-3,x+3,y+3,((int)(a*0.40)<<24)|0xFF2A18);
                g.fill(x-1,y-1,x+1,y+1,(a<<24)|0x0A0A0A);
            }
        }
        if(time<whiteEnd)g.fill(0,0,w,h,((int)(150*(whiteEnd-time)/7.0)<<24)|0xFFF5DC);
    }
    @SubscribeEvent public static void scene(RenderLevelStageEvent e){var mc=Minecraft.getInstance();Domain d=inside();if(d==null||mc.level==null||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS)return;
        PoseStack ps=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();ps.pushPose();ps.translate(-cam.x,-cam.y,-cam.z);
        RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShader(GameRenderer::getPositionColorShader);
        // 通道内的风景贴合固定房间，伤害范围仍以实际展开位置为中心。
        Vec3 sceneryCenter=mc.level.dimension()==SoulJourney.CORRIDOR&&d.center.y>=62&&d.center.y<86?new Vec3(0,64,0):d.center;
        RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);if(d.station)SoulScenery.station(b,ps,sceneryCenter,cam,d.radius);else SoulScenery.draw(b,ps,sceneryCenter,cam,time-d.born+e.getPartialTick());BufferUploader.drawWithShader(b.end());RenderSystem.enableCull();RenderSystem.disableBlend();ps.popPose();
    }
    public static void draw(BufferBuilder b,PoseStack ps,Vec3 cam,float partial){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        BlackFlashRender.draw(b,ps,cam,partial);   // 黑闪的纯黑核心（自绘，不依赖 .fx）
        for(Visual v:effects){Network.Effect e=v.e;SlashAppearance.use(e);float age=time-v.start+partial,fade=Math.max(0,1-age/Math.max(1,e.life()));Vec3 p=e.position(),q=e.direction();int alpha=(int)(220*fade);
            // 超新星：红色弹幕。弹头按本刻位移前进，拖一段同色短尾，不再走斩击剑气与壁纸通路。
            if(e.kind()==125){
                Vec3 step=q.subtract(p),head=step.lengthSqr()<1e-8?p:p.lerp(q,Math.min(1,age));
                Vec3 dir=step.lengthSqr()<1e-8?new Vec3(0,1,0):step.normalize();
                Vec3 tail=head.subtract(dir.scale(0.4+0.6*fade));
                int glow=(Math.max(1,(int)(alpha*0.4))<<24)|0x8A0A12,body=(alpha<<24)|0xD81C14,core=(alpha<<24)|0xFF5038;
                ribbon(b,ps,tail,head,0.30,glow,cam);
                ribbon(b,ps,tail.lerp(head,0.30),head,0.16,body,cam);
                ribbon(b,ps,tail.lerp(head,0.62),head,0.06,core,cam);
                Vec3 normal=cam.subtract(head);if(normal.lengthSqr()<1e-8)normal=new Vec3(0,0,1);
                GuardianVisuals.disc(b,ps,head,normal,0.26,0.055,body,cam);
                GuardianVisuals.disc(b,ps,head,normal,0.13,0.035,core,cam);
            }
            if(e.kind()==126){double r=e.size();for(int i=-8;i<=8;i++){double n=i*r/8;for(int y=GuardianGridSlash.FIRST_LINE_HEIGHT;y<=GuardianGridSlash.LAST_LINE_HEIGHT;y+=GuardianGridSlash.LINE_HEIGHT_STEP){SlashStyle.draw(b,ps,p.add(-r,y,n),p.add(r,y,n),.1,fade,cam);SlashStyle.draw(b,ps,p.add(n,y,-r),p.add(n,y,r),.1,fade,cam);}}}
            if(e.kind()==127){var target=mc.level.getEntity(e.owner());if(target!=null)p=target.getPosition(partial).add(0,1,0);for(int i=0;i<8;i++){double a=i*Math.PI/4+age*0.3;Vec3 s=p.add(Math.cos(a)*0.85,(i%3-1)*0.55,Math.sin(a)*0.85);ribbon(b,ps,s,s.add(-Math.sin(a)*0.5,0.15,Math.cos(a)*0.5),0.025,(alpha<<24)|0xC6EEFF,cam);}}
            if(e.kind()==80){if(e.size()==2||e.size()==5){ring(b,ps,p.add(0,0.08,0),1+age*0.025,0.025,(alpha<<24)|0xCDBDC7,cam);}}
            if(e.kind()==81){var target=mc.level.getEntity((int)e.seed());if(target!=null)p=target.getPosition(partial).add(0,0.25,0);for(int line=0;line<4;line++)for(int i=0;i<7;i++){Vec3 a=p.add(-0.48+i*0.14,0.25+line*0.4,0.38);ribbon(b,ps,a,a.add(0.075,0.065,0),0.012,(alpha<<24)|0xEDF0EF,cam);}}
            if(e.kind()==83){ring(b,ps,q.add(0,0.05,0),e.size(),0.06,(alpha<<24)|0xD9E8F2,cam);for(int j=0;j<12;j++){double a=j*Math.PI/6;ribbon(b,ps,q.add(0,0.05,0),q.add(Math.cos(a)*e.size(),0.05,Math.sin(a)*e.size()),0.018,(alpha<<24)|0xD9E8F2,cam);}Vec3 tip=p.lerp(q,Math.min(1,age/8));for(int i=-2;i<=2;i++){Vec3 off=new Vec3(i*0.2,0.03,0);ribbon(b,ps,p.add(off),tip.add(off),0.015,(alpha<<24)|0xCAD3D8,cam);}}
            // 黑闪（85）的表现已交给 Photon 的 .fx，这里只保留冲刺蓄力（84）的丝带。
            if(e.kind()==84){for(int i=0;i<10;i++){double a=i*Math.PI/5;Vec3 dir=new Vec3(Math.cos(a),Math.sin(a),Math.sin(i*4)*0.5);Vec3 start=p;for(int j=1;j<=4;j++){Vec3 end=p.add(dir.scale(j*0.18)).add(0,Math.sin(j*9+i)*0.14,0);ribbon(b,ps,start,end,0.047,(alpha<<24)|0xBE1732,cam);ribbon(b,ps,start,end,0.028,(alpha<<24)|0x050207,cam);start=end;}}}
            if(e.kind()==86){ribbon(b,ps,p,q,0.12,(alpha<<24)|0xD1E0E4,cam);ribbon(b,ps,p.add(0,-0.1,0),q.add(0,-0.1,0),0.05,(alpha/2<<24)|0xFFFFFF,cam);}
            if(e.kind()==87){Vec3 soul=p.lerp(q,Math.min(1,age/15));for(int i=0;i<5;i++){Vec3 a=soul.add((i-2)*0.13,0.3,0);ribbon(b,ps,a,a.add(0,1.1,0),0.06,(alpha/2<<24)|0xD6E5EB,cam);}ribbon(b,ps,p.add(0,0.8,0),soul.add(0,0.8,0),0.025,(alpha<<24)|0xE2E2F0,cam);}
            if(e.kind()==88){Vec3 a=p.add(p.x<0?1.015:-0.015,0,0);for(int i=0;i<4;i++)ribbon(b,ps,a.add(0,i*0.25,0.05),a.add(0,i*0.25,0.95),0.13,(alpha<<24)|0xFFFFFF,cam);}
            if(e.kind()==92){for(int i=0;i<9;i++){double a=i*0.7+age*0.08;Vec3 base=p.add(Math.cos(a)*0.45,Math.sin(i*2.1+age*0.12)*0.5+0.3,Math.sin(a)*0.45);ribbon(b,ps,base,base.add(Math.cos(a+1.7)*0.35,0.35,Math.sin(a+1.7)*0.35),0.035,(alpha<<24)|0x72D8FF,cam);}ring(b,ps,p,0.55+age*0.01,0.045,(alpha<<24)|0xA8EEFF,cam);}
            if(e.kind()==93){
                Vec3 toward=cam.subtract(p).normalize(),side=new Vec3(-toward.z,0,toward.x).normalize();
                if(side.lengthSqr()<0.01)side=new Vec3(1,0,0);
                Vec3 up=toward.cross(side).normalize();
                // 目标受击消息使用实体编号，将蓝色剪裁线放在身体朝镜头的表面。
                if(q.lengthSqr()<0.01){
                    var target=mc.level.getEntity(e.owner());
                    if(target!=null){Vec3 center=target.getPosition(partial).add(0,target.getBbHeight()*0.5,0);p=target.getBoundingBox().inflate(0.06).clip(cam,center).orElse(center).add(toward.scale(0.03));}
                }
                double angle=Math.toRadians(Math.floorMod(e.seed(),360));
                Vec3 axis=side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                double length=Math.max(1.2,Math.min(4,e.size()*0.4));
                SlashStyle.draw(b,ps,p.subtract(axis.scale(length*.5)),p.add(axis.scale(length*.5)),.08,fade,cam);
                if(age>2){
                    Vec3 ghost=p.add(side.scale(Math.sin(Math.min(1,(age-2)/8)*Math.PI)*0.55)).add(0,0.1,0);
                    for(int i=-1;i<=1;i++)ribbon(b,ps,ghost.add(side.scale(i*0.18)).add(0,-0.4,0),ghost.add(side.scale(i*0.18)).add(0,0.55-Math.abs(i)*0.2,0),0.055,(alpha/3<<24)|0xB8F5FF,cam);
                }
                if(age>4)SlashStyle.detail(b,ps,p.subtract(axis.scale(length*0.5)),p.add(axis.scale(length*0.5)),0.018,(alpha<<24)|0x4CB7FF,cam);
            }
            if(e.kind()==95){for(int i=0;i<12;i++){double a=i*0.524+age*0.12;Vec3 base=p.add(Math.cos(a)*0.45,0.2+(i%5)*0.22,Math.sin(a)*0.45);ribbon(b,ps,base,base.add(Math.cos(a+0.8)*0.22,0.22,Math.sin(a+0.8)*0.22),0.045,(alpha<<24)|0x42B9FF,cam);}ring(b,ps,p,0.7+age*0.025,0.06,(alpha<<24)|0xA8F5FF,cam);}
        }
    }
    public static void soulDomain(BufferBuilder b,PoseStack ps,Vec3 shrine,Vec3 center,Vec3 cam,float age,float fade){
        for(int strand=0;strand<12;strand++)for(int i=0;i<18;i++){double t=i/17.0,a=strand*Math.PI*2/12+Math.sin(age*0.02+strand)*0.2;Vec3 p=shrine.add(Math.cos(a+age*0.01)*((2.5+strand%4)*t),0.5+i*0.45,Math.sin(a+age*0.01)*((2.5+strand%4)*t));Vec3 q=p.add(Math.sin(age*0.04+strand+i)*0.18,0.45,Math.cos(age*0.03+strand)*0.16);ribbon(b,ps,p,q,0.055,((int)(170*fade)<<24)|0x72D8FF,cam);}
        for(int i=0;i<24;i++){double a=i*Math.PI/12+age*0.015;Vec3 p=shrine.add(Math.cos(a)*5,2+(i%8)*1.5,Math.sin(a)*5);ribbon(b,ps,p,p.add(0,1.2+Math.sin(age*0.05+i)*0.4,0),0.045,((int)(150*fade)<<24)|0xC6F5FF,cam);}
        for(int i=0;i<64;i++){double a=i*2.39996,r=Math.sqrt((i+1)/64.0)*198,t=(age*0.035+i*0.3)%3;Vec3 p=center.add(Math.cos(a)*r,0.2+t,Math.sin(a)*r);ribbon(b,ps,p,p.add(0,0.22,0),0.04,((int)(110*(1-t/3)*fade)<<24)|0x3EAFFF,cam);}
    }
    public static void burning(BufferBuilder b,PoseStack ps,Vec3 shrine,Vec3 center,Vec3 cam,float age,float fade){
        // 神龛内部赤黑火舌，骨骸周围升起金色余烬。
        for(int i=0;i<48;i++){double a=i*2.39996,r=2+(i%7)*0.55;Vec3 base=shrine.add(Math.cos(a)*r,0.4,Math.sin(a)*r);Vec3 prev=base;for(int j=1;j<=6;j++){double t=j/6.0;Vec3 next=base.add(Math.sin(age*0.3+i-t*4)*t*0.6,t*(3.5+i%5),Math.cos(age*0.2+i)*t*0.35);ribbon(b,ps,prev,next,(1-t)*0.45+0.03,((int)(190*fade)<<24)|0x10020A,cam);ribbon(b,ps,prev,next,(1-t)*0.22+0.015,((int)(220*fade)<<24)|0xAD1027,cam);prev=next;}}
        for(int i=0;i<100;i++){double t=(age*0.05+i*0.193)%8,a=i*2.39996;Vec3 p=shrine.add(Math.cos(a)*(3+i%10)+Math.sin(age*0.03+i),0.5+t,Math.sin(a)*(3+i%10));ribbon(b,ps,p,p.add(0,0.15+t*0.02,0),0.025,((int)(210*(1-t/8)*fade)<<24)|0xFFD575,cam);}
        // 粉尘覆盖整个200格范围，近处火屑清楚，远处只保留稀疏亮点。
        for(int i=0;i<120;i++){double a=i*2.39996,r=Math.sqrt((i+0.5)/120.0)*198,t=(age*0.06+i*0.7)%4;Vec3 p=center.add(Math.cos(a)*r,0.25+t,Math.sin(a)*r);ribbon(b,ps,p,p.add(Math.sin(i+age*0.1)*0.12,0.3,0),0.065,((int)(160*(1-t/4)*fade)<<24)|0xE34B1D,cam);}
    }
}
