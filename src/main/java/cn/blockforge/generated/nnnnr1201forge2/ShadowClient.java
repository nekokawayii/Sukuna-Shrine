package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/** 显式注册的客户端表现；按键只提交操作，不能自行更改服务端调服/扣费。 */
public final class ShadowClient {
    public static final KeyMapping RECALL=new KeyMapping("key.sukuna.shadow_recall",net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_L,"key.categories.sukuna");
    public static final KeyMapping OPEN=new KeyMapping("key.sukuna.ten_shadows",net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,"key.categories.sukuna");
    private static ShadowSystem.State state=new ShadowSystem.State(3,false,new int[11],new int[11]);
    private record Effect(ShadowSystem.Fx fx,long start){}
    private static final List<Effect> effects=new ArrayList<>();
    public static final class Registration {@SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(RECALL);e.register(OPEN);}}
    public static void receive(ShadowSystem.State s){state=s;}
    public static boolean fused(){return state.fused();}
    public static boolean tamed(int kind){return (state.mask()&(1<<kind))!=0;}
    public static int active(int kind){return state.active()[kind];}
    public static int seconds(int kind){return state.seconds()[kind];}
    public static void send(int operation,int kind,int count){Network.CHANNEL.sendToServer(new ShadowSystem.Request(operation,kind,count));}
    public static void effect(ShadowSystem.Fx fx){var level=Minecraft.getInstance().level;if(level==null)return;if(effects.size()>=512)effects.remove(0);effects.add(new Effect(fx,level.getGameTime()));}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        while(RECALL.consumeClick())if(mc.screen==null&&mc.player!=null&&TenShadowsClient.has())send(4,0,0);
        while(OPEN.consumeClick())if(mc.screen==null&&mc.player!=null&&TenShadowsClient.has())mc.setScreen(new TenShadowsScreen());
        if(mc.level!=null)effects.removeIf(f->mc.level.getGameTime()-f.start>f.fx.ticks());
    }
    // 原版“进度”也默认使用 L；只在已受肉且两个键仍冲突时，把本次 L 留给收回。
    @SubscribeEvent public static void opening(ScreenEvent.Opening e){
        var mc=Minecraft.getInstance();
        if(e.getCurrentScreen()==null&&e.getNewScreen() instanceof net.minecraft.client.gui.screens.advancements.AdvancementsScreen&&TenShadowsClient.has()&&RECALL.same(mc.options.keyAdvancements)){
            e.setCanceled(true);send(4,0,0);while(RECALL.consumeClick()){}
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){effects.clear();state=new ShadowSystem.State(3,false,new int[11],new int[11]);}
    private static final class InkType extends RenderType {
        private InkType(){super("sukuna_shadow_ink",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
        static final RenderType TYPE=RenderType.create("sukuna_shadow_ink",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,4096,false,true,CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;var mc=Minecraft.getInstance();if(mc.level==null||effects.isEmpty())return;
        PoseStack ps=e.getPoseStack();ps.pushPose();Vec3 camera=e.getCamera().getPosition();ps.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();VertexConsumer out=buffers.getBuffer(InkType.TYPE);
        for(Effect effect:effects){var f=effect.fx;float age=mc.level.getGameTime()-effect.start+e.getPartialTick(),t=Math.min(1,age/f.ticks()),fade=(1-t);
            if(f.kind()==0){
                float radius=f.radius()*(.45f+.55f*(float)Math.sin(Math.PI*t));Vec3 center=f.from().add(0,.035,0);
                for(int i=0;i<48;i++){double a=i*Math.PI*2/48,b=(i+1)*Math.PI*2/48;double r0=radius*(1+.07*Math.sin(i*3+age*.3)),r1=radius*(1+.07*Math.sin((i+1)*3+age*.3));
                    vertex(out,ps,center,5,5,10,(int)(225*fade));vertex(out,ps,center.add(Math.cos(a)*r0,0,Math.sin(a)*r0),5,5,10,(int)(225*fade));vertex(out,ps,center.add(Math.cos(b)*r1,0,Math.sin(b)*r1),5,5,10,(int)(225*fade));vertex(out,ps,center,5,5,10,(int)(225*fade));}
                for(int ring=0;ring<2;ring++)ring(out,ps,center.add(0,.005,0),radius*(.45+ring*.4),.025,35,33,55,(int)(140*fade));
            }else if(f.kind()==1){
                Vec3 previous=f.from();for(int i=1;i<=9;i++){double u=i/9.0;Vec3 point=f.from().lerp(f.to(),u);if(i<9)point=point.add(Math.sin(i*7+effect.start)*.4,Math.cos(i*9)*.2,Math.cos(i*5+effect.start)*.4);beam(out,ps,previous,point,.055,180,205,255,(int)(255*fade));previous=point;}
            }else if(f.kind()==2){beam(out,ps,f.from(),f.to(),f.radius(),60,150,245,(int)(175*fade));beam(out,ps,f.from(),f.to(),f.radius()*.3,180,235,255,(int)(240*fade));}
            else if(f.kind()==3)beam(out,ps,f.from(),f.to(),f.radius(),190,65,95,(int)(230*fade));
            else if(f.kind()==4){ring(out,ps,f.to().add(0,-.8,0),.8+t*.5,.045,120,255,185,(int)(180*fade));}
        }
        ps.popPose();buffers.endBatch(InkType.TYPE);
    }
    private static void ring(VertexConsumer out,PoseStack ps,Vec3 c,double radius,double thickness,int r,int g,int b,int a){for(int i=0;i<40;i++){double p=i*Math.PI/20,q=(i+1)*Math.PI/20;vertex(out,ps,c.add(Math.cos(p)*radius,0,Math.sin(p)*radius),r,g,b,a);vertex(out,ps,c.add(Math.cos(p)*(radius+thickness),0,Math.sin(p)*(radius+thickness)),r,g,b,a);vertex(out,ps,c.add(Math.cos(q)*(radius+thickness),0,Math.sin(q)*(radius+thickness)),r,g,b,a);vertex(out,ps,c.add(Math.cos(q)*radius,0,Math.sin(q)*radius),r,g,b,a);}}
    private static void beam(VertexConsumer out,PoseStack ps,Vec3 from,Vec3 to,double width,int r,int g,int b,int a){Vec3 dir=to.subtract(from).normalize();Vec3 right=dir.cross(new Vec3(0,1,0));if(right.lengthSqr()<.01)right=new Vec3(1,0,0);right=right.normalize().scale(width);Vec3 up=dir.cross(right).normalize().scale(width);for(Vec3 side:new Vec3[]{right,up}){vertex(out,ps,from.subtract(side),r,g,b,a);vertex(out,ps,from.add(side),r,g,b,a);vertex(out,ps,to.add(side),r,g,b,a);vertex(out,ps,to.subtract(side),r,g,b,a);}}
    private static void vertex(VertexConsumer out,PoseStack ps,Vec3 at,int r,int g,int b,int a){out.vertex(ps.last().pose(),(float)at.x,(float)at.y,(float)at.z).color(r,g,b,a).endVertex();}
    private ShadowClient(){}
}
