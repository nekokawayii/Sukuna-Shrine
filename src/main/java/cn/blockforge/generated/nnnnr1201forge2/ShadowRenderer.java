package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class ShadowRenderer extends EntityRenderer<ShadowEntity> {
    private final ShadowModel[] models=new ShadowModel[TenShadowRules.COUNT];
    private static final float[] HEIGHT={1.35f,1.35f,1.8f,.9f,1.2f,2.6f,2.8f,.65f,3.1f,2.2f,2.1f};
    public ShadowRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.5f;}
    @Override public ResourceLocation getTextureLocation(ShadowEntity e){return new ResourceLocation("sukuna","textures/entity/ten_shadows/"+TenShadowRules.IDS[e.kind()]+"_0.png");}
    @Override public boolean shouldRender(ShadowEntity e,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){return frustum.isVisible(e.getBoundingBox().inflate(6));}
    @Override public void render(ShadowEntity e,float yaw,float partial,PoseStack ps,MultiBufferSource buffers,int light){
        int kind=e.kind();ShadowModel model=models[kind];if(model==null)models[kind]=model=new ShadowModel(TenShadowRules.IDS[kind]);
        float scale=HEIGHT[kind]/model.height(),age=e.actionAge()+partial,alpha=1;
        ps.pushPose();
        if(e.action()==ShadowEntity.EMERGING){float t=Math.min(1,age/20f);var d=e.emergeOffset();ps.translate(d.x*(1-t),d.y*(1-t)-HEIGHT[kind]*(1-t)+Math.sin(t*Math.PI)*.6,d.z*(1-t));alpha=Math.max(.05f,t);}
        if(e.action()==ShadowEntity.DISSOLVE){float t=Math.min(1,age/20f);ps.translate(0,-HEIGHT[kind]*t,0);alpha=Math.max(.01f,1-t);}
        ps.mulPose(Axis.YP.rotationDegrees(180-yaw));
        if(kind==TenShadowRules.NUE&&e.action()==ShadowEntity.ATTACK)ps.mulPose(Axis.XP.rotationDegrees(25));
        if(kind==TenShadowRules.SERPENT&&e.action()==ShadowEntity.ATTACK){float bend=(float)Math.sin(Math.min(1,age/18)*Math.PI);ps.translate(0,0,-bend*.7);ps.mulPose(Axis.XP.rotationDegrees(-bend*10));}
        ps.scale(scale,scale,scale);ps.translate(-model.centerX(),-model.minY(),-model.centerZ());
        boolean moving=e.walkAnimation.speed()>0.025f||e.getDeltaMovement().horizontalDistanceSqr()>.001;
        String animation=animation(kind,e.action(),moving);float seconds=e.action()==ShadowEntity.ATTACK||e.action()==ShadowEntity.SPECIAL?age/20f:(e.tickCount+partial)/20f;
        if(kind==TenShadowRules.OX&&!moving&&e.action()!=ShadowEntity.ATTACK&&e.action()!=ShadowEntity.SPECIAL)seconds=0;
        model.render(ps,buffers,light,animation,seconds,alpha);ps.popPose();super.render(e,yaw,partial,ps,buffers,light);
    }
    public static String animation(int kind,int action,boolean moving){
        boolean hit=action==ShadowEntity.ATTACK,special=action==ShadowEntity.SPECIAL;
        return switch(kind){
            case 0,1->hit?"attack_bite":moving?"run":"idle";
            case 2->hit?"claw_attack":moving?"run":"idle";
            case 3->"待机（飞行）";
            case 4->special?"吐舌头":moving?"跳跃行走":"待机";
            case 5->moving||hit?"弯曲爬行":"盘起来待机";
            case 6->special?"鼻子吐水":hit?"撞击":moving?"奔跑":"待机";
            case 7->moving?"奔跑":"待机";
            case 8->"待机";
            case 9->hit||special?"蓄力奔跑冲撞":"奔跑循环";
            case 10->hit?"爪击":moving?"奔跑":"待机";
            default->"idle";
        };
    }
}
