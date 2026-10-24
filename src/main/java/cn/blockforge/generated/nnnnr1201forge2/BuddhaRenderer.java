package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** 原样加载玩家提供的灰烬长手怪，沿用其 idle / walk / melee / cast / death 动画。 */
public final class BuddhaRenderer<T extends Mob> extends EntityRenderer<T> {
    public static final BbModel BUDDHA=new BbModel("ashen_longarms","ashen_longarms");
    private static final BbModel PARASITE=new BbModel("parasite","parasite"),MOTHER=new BbModel("mother","parasite");
    private record Animation(String name,int start){}
    private final java.util.Map<T,Animation> animations=new java.util.WeakHashMap<>();
    public BuddhaRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0.6f;}
    @Override public boolean shouldRender(T entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){return frustum.isVisible(entity.getBoundingBox().inflate(entity instanceof BuddhaEntity?6:1));}
    @Override public ResourceLocation getTextureLocation(T entity){return new ResourceLocation("sukuna","textures/entity/"+(entity instanceof BuddhaEntity?"ashen_longarms":"parasite")+"_0.png");}
    @Override public void render(T entity,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int light){
        ps.pushPose();ps.mulPose(Axis.YP.rotationDegrees(180-yaw));float age=entity.tickCount+partial;
        if(entity instanceof BuddhaEntity b){
            float scale=b.getBbHeight()/BUDDHA.heightBlocks();ps.scale(scale,scale,scale);
            Animation previous=animations.get(entity);boolean melee=b.swinging||previous!=null&&previous.name.equals("melee")&&age-previous.start<Math.min(48,20*(b.phase()==2?1:2));
            String name=!b.isAlive()?"death":b.action()!=0?"cast":melee?"melee":b.getDeltaMovement().horizontalDistanceSqr()>.0003?"walk":"idle";
            if(previous==null||!previous.name.equals(name)){previous=new Animation(name,b.tickCount);animations.put(entity,previous);}
            float seconds=(age-previous.start)/20;boolean red=b.phase()==2;
            BUDDHA.render(ps,buffers,light,name,seconds,1,red?.63f:1,red?.67f:1,1);
            if(b.action()==7||b.action()==11){ps.scale(1.012f,1.012f,1.012f);BUDDHA.render(ps,buffers,15728880,name,seconds,1,.35f,.18f,.2f);}
        }else if(entity instanceof ParasiteEntity p){boolean mother=p.mother();ps.translate(0,Math.abs(Math.sin(age*.3))*(mother?.03:.045),0);ps.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(age*.3)*(mother?1:4)));(mother?MOTHER:PARASITE).render(ps,buffers,light,"idle",age/20,1,1,1,1);}
        ps.popPose();super.render(entity,yaw,partial,ps,buffers,light);
    }
}
