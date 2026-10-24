package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** 魔虚罗渲染：原始 bbmodel + 完全适应后的免疫光环。
 *  光环颜色与纹样按攻击类型区分（斩击=白刀纹、火焰=赤火纹、徒手=白水纹…），玩家据此判断还剩几种类型可用。
 *
 *  <p><b>法轮不给魔虚罗本体</b>：它是召唤者头顶的东西（见 {@link MahoragaWheelRenderer}），
 *  每受击 6 次转 1 格、转满 4 格完全适应。</p> */
public final class MahoragaRenderer<T extends MahoragaEntity> extends EntityRenderer<T> {
    private static final BbModel BODY=new BbModel("mahoraga","mahoraga");
    private static final BbModel WHEEL=new BbModel("mahoraga_wheel","mahoraga_wheel");
    private static final java.util.Map<MahoragaEntity,float[]> turns=new java.util.WeakHashMap<>();
    private static final ResourceLocation RING=new ResourceLocation("sukuna","textures/effect/ring.png");
    private static final ResourceLocation STREAK=new ResourceLocation("sukuna","textures/effect/streak.png");
    /** 模型高 3.69 格。 */
    private static final float MODEL_HEIGHT=3.69f;
    public MahoragaRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=1.7f;}
    @Override public ResourceLocation getTextureLocation(T entity){return new ResourceLocation("sukuna","textures/entity/mahoraga_0.png");}
    @Override public boolean shouldRender(T entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){return frustum.isVisible(entity.getBoundingBox().inflate(9));}
    @Override public void render(T entity,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int light){
        float age=entity.tickCount+partial;
        ps.pushPose();ps.mulPose(Axis.YP.rotationDegrees(180-yaw));
        // 只画模型本体与自带动画：法轮与位移的特效由 Photon 负责，本体一律不挂光环（避免光污染）
        BODY.render(ps,buffers,light,animationOf(entity),secondsOf(entity,age),1,1,1,1);
        ps.popPose();
        float state=entity.wheelCells()+Integer.bitCount(entity.ownMask());float[] turn=turns.computeIfAbsent(entity,k->new float[]{state,age,0});
        if(turn[0]!=state){turn[0]=state;turn[1]=age;turn[2]+=90;}
        float phase=Math.min(1,Math.max(0,(age-turn[1])/16)),angle=age*.45f+turn[2]-90*(1-phase)*(1-phase);
        ps.pushPose();ps.translate(0,entity.getBbHeight()+.38,0);ps.mulPose(Axis.XP.rotationDegrees(-90));ps.mulPose(Axis.ZP.rotationDegrees(angle));ps.scale(1.5f,1.5f,1.5f);
        WHEEL.render(ps,buffers,15728880,"spin",0,1,phase<1?.95f:.78f,phase<1?.65f:.3f,1);ps.popPose();
        super.render(entity,yaw,partial,ps,buffers,light);
    }
    /** 完全适应的类型数（免疫光环个数）。 */
    public static int adaptedCount(MahoragaEntity entity){return Integer.bitCount(entity.immuneMask());}
    /** 动画只跟**攻击动作**走：法轮转动是叠加在头顶的表现，不再抢占动画、也不再让本体定住。 */
    private static <T extends MahoragaEntity> String animationOf(T entity){
        return switch(entity.action()){
            case MahoragaEntity.ACT_EMERGE->"emerge";
            case MahoragaEntity.ACT_CLEAVE->"cleave";
            case MahoragaEntity.ACT_SWEEP->"sweep";
            case MahoragaEntity.ACT_SLAM->"slam";
            case MahoragaEntity.ACT_THRUST->"thrust";
            case MahoragaEntity.ACT_LEAP->"leap";
            default->far(entity)?"sprint":moving(entity)?"walk":"idle";
        };
    }
    private static <T extends MahoragaEntity> boolean far(T entity){
        LivingEntity target=entity.getTarget();
        return target!=null&&entity.distanceToSqr(target)>36;
    }
    private static <T extends MahoragaEntity> boolean moving(T entity){return entity.getDeltaMovement().horizontalDistanceSqr()>1.0e-4;}
    private static <T extends MahoragaEntity> float secondsOf(T entity,float age){
        return switch(entity.action()){
            case MahoragaEntity.ACT_EMERGE->Math.min(3f,entity.actionAge()/20f);
            case MahoragaEntity.ACT_CLEAVE,MahoragaEntity.ACT_SWEEP,MahoragaEntity.ACT_SLAM,MahoragaEntity.ACT_THRUST->entity.actionAge()/20f;
            case MahoragaEntity.ACT_LEAP->Math.min(1f,entity.actionAge()/20f);
            default->age/20f;
        };
    }
}
