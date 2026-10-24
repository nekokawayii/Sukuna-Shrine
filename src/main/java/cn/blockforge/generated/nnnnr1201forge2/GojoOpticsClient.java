package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL30;

/** 世界画面深度重建、背景折射与独立点光源；不改变游戏逻辑亮度或放置隐形方块。 */
public final class GojoOpticsClient {
    private static final int MAX=4, MAP=48;
    private static final float RANGE=24, INTENSITY=10;
    private static final Map<String,Light> lights=new LinkedHashMap<>();
    private static final class Light {
        Vec3 pos,previous,shadowPos; float size; int color,keep; boolean lens; long last,shadowAt=-100;
        final float[] distances=new float[MAP*MAP];
    }
    private static ShaderInstance shader;
    private static TextureTarget scene;
    // 这是距离数据缓冲，不是物品/方块美术贴图。RG 两通道保存16位径向深度。
    private static DynamicTexture shadows;
    private static long tick;
    private static boolean warned;
    private static long shadowBudgetTick=Long.MIN_VALUE;
    private static int shadowBudget;
    private static Matrix4f worldView,worldProjection;
    private static Object world;
    private static boolean externalNoted;
    public static final class Registration {
        @SubscribeEvent public static void shaders(RegisterShadersEvent e){
            shader=null;warned=false;
            try{e.registerShader(new ShaderInstance(e.getResourceProvider(),new ResourceLocation("sukuna","gojo_optics"),DefaultVertexFormat.POSITION),s->{shader=s;warned=false;});}
            catch(java.io.IOException|RuntimeException ex){warned=true;com.mojang.logging.LogUtils.getLogger().warn("苍赫茈后期加载失败，保留独立亮核与光环",ex);}
        }
    }
    private static void put(String key,Vec3 at,float size,int color,boolean lens,int keep){
        if(at==null||!Double.isFinite(at.lengthSqr())||!Float.isFinite(size))return;
        Light l=lights.get(key);
        if(l==null){if(lights.size()>=96)return;l=new Light();l.pos=at;l.previous=at;lights.put(key,l);}
        if(l.last!=tick)l.previous=l.pos;
        l.pos=at;l.size=Math.max(.15f,size);l.color=color;l.lens=lens;l.keep=keep;l.last=tick;
    }
    public static void receive(Network.Effect e){
        var level=Minecraft.getInstance().level;if(level==null)return;
        if(world!=level){clear();world=level;}
        String orb=e.owner()+"/orb/"+e.seed(),cast=e.owner()+"/cast/";
        switch(e.kind()){
            case DeathStar.FX_CHARGE,DeathStar.FX_FLIGHT -> put(cast+"death/"+e.seed(),e.position(),e.kind()==DeathStar.FX_CHARGE?.25f+e.size()*.4f:.8f,0x9E0629,true,8);
            case DeathStar.FX_STOP,DeathStar.FX_BURST -> lights.remove(cast+"death/"+e.seed());
            case BloodArts.FX+2 -> put(cast+"blood",e.position(),.24f+.16f*Math.max(0,Math.min(1,e.size())),0xA40928,true,8);
            case GojoFx.BLUE -> put(orb,e.position(),e.size(),0x2580FF,true,5);
            case GojoFx.RED -> put(orb,e.position(),e.size(),0xFF2816,false,5);
            case GojoFx.PURPLE,GojoFx.PURPLE_TAP -> put(orb,e.position(),e.kind()==GojoFx.PURPLE_TAP?.55f:e.size(),0xAA3EFF,true,5);
            case GojoFx.BLUE_END,GojoFx.RED_END,GojoFx.PURPLE_BURST,GojoFx.PURPLE_TAP_BURST,GojoFx.BIG_BANG -> lights.remove(orb);
            case GojoFx.CHARGE -> put(cast+"red",e.position(),.5f,0xFF2816,false,4);
            case GojoFx.PURPLE_CHARGE,GojoFx.PURPLE_FORM -> put(cast+"purple",e.position(),e.size(),0xAA3EFF,true,e.kind()==GojoFx.PURPLE_FORM?8:4);
            case GojoFx.BEAM -> {for(int i=0;i<3;i++)put(cast+"beam"+i,e.position().lerp(e.direction(),(i+.5)/3),.7f,0xFF2816,false,Math.min(20,e.life()));}
            case GojoFx.PURPLE_GATHER,GojoFx.PURPLE_GATHER_SPIN -> {
                var mc=Minecraft.getInstance();var owner=mc.level==null?null:mc.level.getEntity(e.owner());
                Vec3 dir=owner==null?new Vec3(0,0,1):owner.getLookAngle(),side=NetBranches.side(dir);
                double t=Math.max(0,Math.min(1,e.seed()/100.0)),a=e.kind()==GojoFx.PURPLE_GATHER_SPIN?t*Math.PI*3:0;
                Vec3 off=side.scale(Math.cos(a)*Math.max(.12,e.size()*(1-t))).add(0,Math.sin(a)*e.size()*(1-t),0);
                put(cast+"blue",e.position().add(off),1.5f,0x2580FF,true,3);
                put(cast+"red",e.position().subtract(off),.9f,0xFF2816,false,3);
            }
            default -> {}
        }
    }
    public static void tick(){
        var mc=Minecraft.getInstance();if(world!=mc.level){clear();world=mc.level;}
        if(mc.isPaused())return;tick++;lights.values().removeIf(l->tick-l.last>l.keep);
    }
    public static void clear(){
        lights.clear();tick=0;warned=false;shadowBudgetTick=Long.MIN_VALUE;world=null;worldView=worldProjection=null;externalNoted=false;
        if(scene!=null){scene.destroyBuffers();scene=null;}
        if(shadows!=null){shadows.close();shadows=null;}
    }
    /** 八面体方向映射，和片元着色器严格同源；每条射线走方块真实碰撞外形。 */
    private static Vec3 direction(double u,double v){
        double x=u*2-1,y=v*2-1,z=1-Math.abs(x)-Math.abs(y);
        if(z<0){double old=x;x=(1-Math.abs(y))*(old>=0?1:-1);y=(1-Math.abs(old))*(y>=0?1:-1);}
        return new Vec3(x,y,z).normalize();
    }
    private static void shadow(Light l){
        var mc=Minecraft.getInstance();l.shadowPos=l.pos;l.shadowAt=tick;
        // 实体也参与距离遮挡，镜头外的实体不会再因为缺少屏幕深度而完全漏掉阴影。
        List<AABB> blockers=mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(l.pos,l.pos).inflate(RANGE),e->e.isAlive()&&!e.isInvisible()&&!e.isSpectator()).stream().map(e->e.getBoundingBox()).filter(box->!box.inflate(.1).contains(l.pos)).toList();
        for(int y=0;y<MAP;y++)for(int x=0;x<MAP;x++){
            Vec3 end=l.pos.add(direction((x+.5)/MAP,(y+.5)/MAP).scale(RANGE));
            BlockHitResult hit=mc.level.clip(new ClipContext(l.pos,end,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player));
            float distance=hit.getType()==HitResult.Type.MISS?RANGE:(float)l.pos.distanceTo(hit.getLocation());
            for(AABB box:blockers){var p=box.clip(l.pos,end);if(p.isPresent())distance=Math.min(distance,(float)l.pos.distanceTo(p.get()));}
            l.distances[y*MAP+x]=GojoOpticsRules.shadowRange(distance);
        }
    }
    @SubscribeEvent public static void beforeFrame(net.minecraftforge.event.TickEvent.RenderTickEvent e){
        if(e.phase==net.minecraftforge.event.TickEvent.Phase.START)worldView=worldProjection=null;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||ObserverClient.active())return;
        if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_PARTICLES){
            // Forge 1.20.1 的 AFTER_LEVEL 传来的是投影用栈，不是世界视图栈！必须在这里保存相机旋转。
            worldView=new Matrix4f(e.getPoseStack().last().pose());worldProjection=new Matrix4f(e.getProjectionMatrix());
            Vec3 camera=e.getCamera().getPosition();
            var glow=lights.entrySet().stream().filter(entry->!entry.getKey().contains("/death/")).map(Map.Entry::getValue).filter(l->l.pos.distanceToSqr(camera)<96*96)
                .sorted(Comparator.comparingDouble(l->l.pos.distanceToSqr(camera))).limit(24)
                .map(l->new GojoGravityClient.Orb(l.previous.lerp(l.pos,e.getPartialTick()),l.size,l.color,l.lens)).toList();
            GojoGravityClient.render(e,glow,tick+e.getPartialTick());return;
        }
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||lights.isEmpty()||shader==null||warned||worldView==null)return;
        var main=mc.getMainRenderTarget();if(main.width<=0||main.height<=0||main.getDepthTextureId()<0)return;
        Vec3 camera=e.getCamera().getPosition();
        List<Light> visible=lights.values().stream().filter(l->l.pos.distanceToSqr(camera)<96*96)
            .sorted(Comparator.comparingDouble(l->l.pos.distanceToSqr(camera))).limit(MAX).toList();
        if(visible.isEmpty())return;
        // 外部光影接管不同 framebuffer 时不冒险改写它；原版三种画质可用。
        int previousRead=GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),previousDraw=GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        if(previousDraw!=main.frameBufferId){
            if(!externalNoted){externalNoted=true;com.mojang.logging.LogUtils.getLogger().warn("检测到外部光影画面，跳过不兼容的背景折射与投影照明，独立亮核和光子环仍保留");}
            return;
        }
        boolean depth=GL30.glIsEnabled(GL30.GL_DEPTH_TEST),blend=GL30.glIsEnabled(GL30.GL_BLEND),cull=GL30.glIsEnabled(GL30.GL_CULL_FACE);
        boolean mask=GL30.glGetBoolean(GL30.GL_DEPTH_WRITEMASK);
        ShaderInstance previousShader=RenderSystem.getShader();float[] previousColor=RenderSystem.getShaderColor().clone();
        int[] previousTextures={RenderSystem.getShaderTexture(0),RenderSystem.getShaderTexture(1),RenderSystem.getShaderTexture(2)};
        try{
            if(scene==null)scene=new TextureTarget(main.width,main.height,true,Minecraft.ON_OSX);
            else if(scene.width!=main.width||scene.height!=main.height)scene.resize(main.width,main.height,Minecraft.ON_OSX);
            if(main.isStencilEnabled()&&!scene.isStencilEnabled())scene.enableStencil();
            if(shadows==null){shadows=new DynamicTexture(MAP*MAX,MAP,false);shadows.setFilter(false,false);}
            // 最多每刻更新两个方向深度图，快速移动的球优先；未更新的光仍使用同一张图对应的原点。
            if(shadowBudgetTick!=tick){shadowBudgetTick=tick;shadowBudget=2;}
            for(Light l:visible.stream().sorted(Comparator.comparingLong(l->l.shadowAt)).toList())
                if(shadowBudget>0&&l.shadowAt<tick&&(l.shadowPos==null||l.pos.distanceToSqr(l.shadowPos)>.16||tick-l.shadowAt>=2)){shadow(l);shadowBudget--;}
            var pixels=shadows.getPixels();
            for(int i=0;i<MAX;i++)for(int y=0;y<MAP;y++)for(int x=0;x<MAP;x++){
                int d=i<visible.size()&&visible.get(i).shadowPos!=null?Math.round(visible.get(i).distances[y*MAP+x]/RANGE*65535):0;
                pixels.setPixelRGBA(i*MAP+x,y,0xFF000000|(d&255)<<8|(d>>>8));
            }
            shadows.upload();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,scene.frameBufferId);
            GL30.glBlitFramebuffer(0,0,main.width,main.height,0,0,main.width,main.height,GL30.GL_COLOR_BUFFER_BIT|GL30.GL_DEPTH_BUFFER_BIT,GL30.GL_NEAREST);
            main.bindWrite(true);
            Matrix4f vp=new Matrix4f(worldProjection).mul(worldView);
            shader.safeGetUniform("InverseViewProjection").set(new Matrix4f(vp).invert());
            shader.safeGetUniform("ViewProjection").set(vp);
            shader.safeGetUniform("FrameSize").set((float)main.width,(float)main.height);
            shader.safeGetUniform("LightCount").set(visible.size());
            for(int i=0;i<MAX;i++){
                Light l=i<visible.size()?visible.get(i):null;
                Vec3 pos=l==null?Vec3.ZERO:l.previous.lerp(l.pos,e.getPartialTick()).subtract(camera);
                shader.safeGetUniform("Light"+i).set((float)pos.x,(float)pos.y,(float)pos.z,l==null?0:l.size);
                Vec3 origin=l==null||l.shadowPos==null?pos:l.shadowPos.subtract(camera);
                shader.safeGetUniform("Shadow"+i).set((float)origin.x,(float)origin.y,(float)origin.z,l!=null&&l.shadowPos!=null?1:0);
                shader.safeGetUniform("Tint"+i).set(l==null?0:((l.color>>16)&255)/255f,l==null?0:((l.color>>8)&255)/255f,l==null?0:(l.color&255)/255f,l!=null&&l.lens?1:0);
                Vector4f projected=new Vector4f((float)pos.x,(float)pos.y,(float)pos.z,1).mul(vp);
                float radius=l==null?0:GojoOpticsRules.radius(worldProjection.m11(),l.size,projected.w);
                shader.safeGetUniform("Lens"+i).set(projected.w<=.05?-.5f:projected.x/projected.w*.5f+.5f,projected.w<=.05?-.5f:projected.y/projected.w*.5f+.5f,radius,(float)pos.length());
            }
            shader.safeGetUniform("Intensity").set(INTENSITY);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.disableBlend();
            RenderSystem.setShader(()->shader);
            RenderSystem.setShaderTexture(0,scene.getColorTextureId());RenderSystem.setShaderTexture(1,scene.getDepthTextureId());RenderSystem.setShaderTexture(2,shadows.getId());
            BufferBuilder b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);
            b.vertex(-1,-1,0).endVertex();b.vertex(1,-1,0).endVertex();b.vertex(1,1,0).endVertex();b.vertex(-1,1,0).endVertex();
            BufferUploader.drawWithShader(b.end());
        }catch(RuntimeException ex){
            if(!warned){com.mojang.logging.LogUtils.getLogger().warn("苍赫茈光学效果暂停，保留原有术式特效",ex);warned=true;}
            // 不清光源：即使后期失败，独立亮核和光环也持续到术式结束。
        }finally{
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,previousRead);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,previousDraw);
            RenderSystem.depthMask(mask);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.setShader(()->previousShader);
            for(int i=0;i<3;i++)RenderSystem.setShaderTexture(i,previousTextures[i]);
            RenderSystem.setShaderColor(previousColor[0],previousColor[1],previousColor[2],previousColor[3]);
        }
    }
    private GojoOpticsClient(){}
}
