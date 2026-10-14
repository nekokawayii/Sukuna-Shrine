package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 「神武解」右键雷击的**彩色闪电**（纯自绘顶点，不用 Photon、也不用原版蓝闪电）。
 *
 *  <p>每道雷由若干段折线组成，从目标上方 24 格劈到目标身上，配色按彩虹循环（hue 随索引与时间偏移），
 *  用加色混合画成"彩色雷"；落地再补一圈扩散光环。存活 {@link #LIFE} 刻。</p> */
@Mod.EventBusSubscriber(modid="sukuna",value=Dist.CLIENT)
public final class ShinbukeVisuals {
    /** 网络编号（避开已有区段：180-186 终式、187-203 五条悟、204-218 魔虚罗）。 */
    public static final int STRIKE=219;
    /** 充能同步编号（服务端 → 客户端，数值在 size 字段里）。 */
    public static final int CHARGE=220;
    /** 雷击存活刻数。 */
    public static final int LIFE=14;
    private static final int SEGMENTS=7;
    private static final double SKY=24;
    private record Bolt(Vec3 at,long born,int hue,long seed){}
    private static final Map<Integer,Bolt> bolts=new ConcurrentHashMap<>();
    private static int nextId=1;
    /** 服务端同步过来的"已停止充能、等右键释放"标志。 */
    private static volatile boolean storedFlag;
    public static void receive(Network.Effect e){
        if(e.kind()==CHARGE){Shinbuke.setClientCharge((int)e.size());storedFlag=e.seed()==1;return;}
        synchronized(bolts){
            bolts.put(nextId++,new Bolt(e.position(),System.currentTimeMillis(),(int)e.seed()*47,e.seed()));
        }
    }
    /** HUD：手持神武解时在准星下方显示充能与"下一发的范围/伤害"。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void hud(net.minecraftforge.client.event.RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.options.hideGui||mc.screen!=null)return;
        if(!Shinbuke.holding(mc.player))return;
        int charge=Shinbuke.clientCharge();
        double range=Shinbuke.RANGE*Shinbuke.rangeFactor(charge);
        boolean stored=storedFlag; // 已停止充能、等右键释放
        String text=charge>0
            ? (stored?"神武解 充能完毕 ":"神武解 充能 ")+charge+" / "+Shinbuke.CHARGE_MAX
                +"  ·  范围 "+BalanceRules.number(range)+" 格 · 伤害 ×"+BalanceRules.number(Shinbuke.damageFactor(charge))
                +(stored?"  ·  右键释放":"")
            : "神武解（按住释放键充能）";
        var g=e.getGuiGraphics();
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        // UI 放在**物品栏上方**（快捷栏 22px + 生命/饥饿 39px 之上），不再跟着准星飘、也不会充能完后消失
        int y=h-64;
        int barW=140,cx=w/2;
        int pct=Math.min(100,charge*100/Math.max(1,Shinbuke.CHARGE_MAX));
        g.drawCenteredString(mc.font,text,cx,y-11,charge>=Shinbuke.CHARGE_STEP?0xFFFFE27A:0xFF9BE8FF);
        g.fill(cx-barW/2-1,y-2,cx+barW/2+1,y+5,0xB0181019);
        int filled=(int)(barW*charge/(float)Math.max(1,Shinbuke.CHARGE_MAX));
        g.fill(cx-barW/2,y-1,cx-barW/2+filled,y+4,charge>=Shinbuke.CHARGE_STEP?0xFFFFE27A:0xFF6FD8FF);
        // 每 100 点的档位刻度
        for(int i=1;i<=Shinbuke.CHARGE_MAX/Shinbuke.CHARGE_STEP;i++){
            int x=cx-barW/2+(int)(barW*i*Shinbuke.CHARGE_STEP/(float)Shinbuke.CHARGE_MAX);
            g.fill(x,y-3,x+1,y+6,0x80FFFFFF);
        }
    }
    public static void tick(){
        long now=System.currentTimeMillis();
        bolts.entrySet().removeIf(en->now-en.getValue().born()>LIFE*50L);
    }
    @SubscribeEvent public static void stage(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        if(bolts.isEmpty())return;
        var mc=Minecraft.getInstance();
        if(mc.level==null)return;
        long now=System.currentTimeMillis();
        Vec3 cam=e.getCamera().getPosition();
        PoseStack ps=e.getPoseStack();
        RenderSystem.disableCull();RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
        BufferBuilder b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        var pose=ps.last().pose();
        for(Bolt bolt:bolts.values()){
            float t=Math.min(1f,(now-bolt.born())/(float)(LIFE*50));
            float alpha=(1f-t)*(1f-t)*255f;
            if(alpha<6)continue;
            Vec3 top=bolt.at().add(0,SKY,0),bottom=bolt.at();
            Random rnd=new Random(bolt.seed()*1315423911L);
            Vec3 prev=top;
            for(int s=1;s<=SEGMENTS;s++){
                double f=s/(double)SEGMENTS;
                Vec3 point=top.lerp(bottom,f);
                if(s<SEGMENTS){ // 抖动，让折线像闪电
                    double jitter=1.4*(1-f)+0.35;
                    point=point.add((rnd.nextDouble()-0.5)*jitter,(rnd.nextDouble()-0.5)*jitter*0.4,(rnd.nextDouble()-0.5)*jitter);
                }
                float hue=((bolt.hue()+s*36+t*90f)%360f)/360f;
                int rgb=hsv(hue,0.85f,1f);
                int r=(rgb>>16)&255,g=(rgb>>8)&255,bl=rgb&255;
                float width=(float)(0.34*(1-f*0.42)); // 加粗：原来 0.16，现在约两倍
                // 用朝向镜头的两条边构成一根"带子"
                Vec3 dir=point.subtract(prev);
                Vec3 side=dir.cross(cam.subtract(point)).normalize().scale(width);
                emit(b,pose,prev.subtract(side).subtract(cam),r,g,bl,(int)(alpha*0.15f));
                emit(b,pose,point.subtract(side).subtract(cam),r,g,bl,(int)(alpha*0.35f));
                emit(b,pose,point.add(side).subtract(cam),r,g,bl,(int)(alpha*0.35f));
                emit(b,pose,prev.add(side).subtract(cam),r,g,bl,(int)(alpha*0.15f));
                prev=point;
            }
            // 落地光环
            float radius=0.8f+2.6f*t;
            int rgb=hsv(((bolt.hue()+t*120f)%360f)/360f,0.7f,1f);
            for(int i=0;i<24;i++){
                double a0=i*Math.PI*2/24,a1=(i+1)*Math.PI*2/24;
                float x0=(float)Math.cos(a0)*radius,z0=(float)Math.sin(a0)*radius;
                float x1=(float)Math.cos(a1)*radius,z1=(float)Math.sin(a1)*radius;
                double y=0.12;
                emit(b,pose,new Vec3(bottom.x+x0-cam.x,bottom.y+y-cam.y,bottom.z+z0-cam.z),(rgb>>16)&255,(rgb>>8)&255,rgb&255,(int)(alpha*0.5f));
                emit(b,pose,new Vec3(bottom.x+x1-cam.x,bottom.y+y-cam.y,bottom.z+z1-cam.z),(rgb>>16)&255,(rgb>>8)&255,rgb&255,(int)(alpha*0.5f));
                emit(b,pose,new Vec3(bottom.x+x1-cam.x,bottom.y+y+0.06-cam.y,bottom.z+z1-cam.z),(rgb>>16)&255,(rgb>>8)&255,rgb&255,(int)(alpha*0.25f));
                emit(b,pose,new Vec3(bottom.x+x0-cam.x,bottom.y+y+0.06-cam.y,bottom.z+z0-cam.z),(rgb>>16)&255,(rgb>>8)&255,rgb&255,(int)(alpha*0.25f));
            }
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableDepthTest();RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();RenderSystem.enableCull();RenderSystem.disableBlend();
    }
    private static void emit(BufferBuilder b,org.joml.Matrix4f pose,Vec3 v,int r,int g,int bl,int a){
        b.vertex(pose,(float)v.x,(float)v.y,(float)v.z).color(r,g,bl,a).endVertex();
    }
    /** 简化 HSV→RGB（只用于闪电配色）。 */
    private static int hsv(float h,float s,float v){
        int i=(int)(h*6);float f=h*6-i,p=v*(1-s),q=v*(1-f*s),t=v*(1-(1-f)*s);
        float r,g,b;
        switch(i%6){case 0->{r=v;g=t;b=p;}case 1->{r=q;g=v;b=p;}case 2->{r=p;g=v;b=t;}case 3->{r=p;g=q;b=v;}case 4->{r=t;g=p;b=v;}default->{r=v;g=p;b=q;}}
        return ((int)(r*255)<<16)|((int)(g*255)<<8)|(int)(b*255);
    }
    public static void clear(){bolts.clear();}
    private ShinbukeVisuals(){}
}
