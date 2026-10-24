package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** 短促的白色气流：跟随真实拳/脚关节，只写颜色、不写深度，不增加实体或攻击包。 */
public final class MeleeWindClient {
    private static final class WindType extends RenderType {
        private WindType(){super("sukuna_melee_wind",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
        private static final RenderType TYPE=RenderType.create("sukuna_melee_wind",DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,4096,false,true,CompositeState.builder().setShaderState(POSITION_COLOR_SHADER)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
    /** 第三人称附在现有动作渲染器上，旁观玩家收到同一连招后自然同步，不另造时间轴。 */
    public static final class Layer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        public Layer(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent){super(parent);}
        @Override public void render(PoseStack ps,MultiBufferSource buffers,int light,AbstractClientPlayer player,
                                     float limb,float amount,float partial,float age,float yaw,float pitch){
            if(player.isInvisible())return;
            var mc=Minecraft.getInstance();
            if(player==mc.getCameraEntity()&&mc.options.getCameraType().isFirstPerson())return;
            var frame=MeleeComboClient.windFrame(player.getId(),partial);if(frame==null)return;
            PlayerModel<AbstractClientPlayer> model=getParentModel();boolean slim="slim".equals(player.getModelName());
            if(MeleeComboRules.kick(frame.stage())){
                ModelPart leg=frame.stage()==5?model.leftLeg:model.rightLeg;
                limb(ps,buffers,leg,"knee",0,.375,frame.stage(),frame.age(),false);
            }else for(int side:new int[]{-1,1})if(MeleeViewRules.striking(frame.stage(),side)){
                ModelPart arm=side<0?model.leftArm:model.rightArm;
                limb(ps,buffers,arm,"elbow",center(side,slim),frame.stage()==6?0:.375,frame.stage(),frame.age(),false);
            }
        }
    }
    public static double center(int side,boolean slim){return -side*(slim?.03125:.0625);}
    /** firstPerson 的调用方已放好肩膀并完成坐标转换。 */
    public static void limb(PoseStack ps,MultiBufferSource buffers,ModelPart limb,String joint,double x,double y,
                            int stage,double age,boolean firstPerson){
        if(MeleeViewRules.windAlpha(stage,age)<=0)return;
        ps.pushPose();
        try{
            limb.translateAndRotate(ps);limb.getChild(joint).translateAndRotate(ps);ps.translate(x,y,0);
            // 肘击气流沿肘尖，不沿折回来的拳头方向。
            if(stage==6)ps.mulPose(Axis.XP.rotationDegrees(125));
            draw(ps,buffers,stage,age,firstPerson);
        }finally{ps.popPose();}
    }
    /** 气流沿肢体末端的 +Y，弧片留有缺口，不用整块白面挡住准星。 */
    private static void draw(PoseStack ps,MultiBufferSource buffers,int stage,double age,boolean firstPerson){
        double alpha=MeleeViewRules.windAlpha(stage,age),progress=MeleeViewRules.windProgress(stage,age);
        if(alpha<=.005)return;
        var out=buffers.getBuffer(WindType.TYPE);
        double size=(stage==8?1.2:MeleeComboRules.kick(stage)?1.1:1)*(firstPerson?.82:1);
        int opacity=(int)((firstPerson?132:176)*alpha);
        for(int ring=0;ring<2;ring++){
            double radius=(.15+progress*.12+ring*.065)*size;
            double axial=(.035+progress*.20-ring*.20)*size;
            for(int arc=0;arc<3;arc++){
                double start=arc*Math.PI*2/3+progress*.7+ring*.4;
                for(int i=0;i<10;i++){
                    double t0=i/10.0,t1=(i+1)/10.0;
                    double a0=start+t0*1.38,a1=start+t1*1.38;
                    double w0=(.009+.018*Math.sin(Math.PI*t0))*size,w1=(.009+.018*Math.sin(Math.PI*t1))*size;
                    int c0=(int)(opacity*Math.sin(Math.PI*t0)*(ring==0?1:.55));
                    int c1=(int)(opacity*Math.sin(Math.PI*t1)*(ring==0?1:.55));
                    point(out,ps,a0,radius-w0,axial,c0);point(out,ps,a0,radius+w0,axial,c0);
                    point(out,ps,a1,radius+w1,axial,c1);point(out,ps,a1,radius-w1,axial,c1);
                }
            }
        }
        // 五束收尖速度线从拳后拖向拳前，每束略微弯曲，避免像常驻光环。
        for(int line=0;line<5;line++)for(int i=0;i<6;i++){
            double t0=i/6.0,t1=(i+1)/6.0,angle=line*Math.PI*2/5+.3;
            double r0=(.13+.04*Math.sin(t0*Math.PI))*size,r1=(.13+.04*Math.sin(t1*Math.PI))*size;
            double y0=(-.48+t0*.64+progress*.12)*size,y1=(-.48+t1*.64+progress*.12)*size;
            double w0=.05*Math.sin(t0*Math.PI),w1=.05*Math.sin(t1*Math.PI);
            int c0=(int)(opacity*.8*Math.sin(t0*Math.PI)),c1=(int)(opacity*.8*Math.sin(t1*Math.PI));
            point(out,ps,angle+t0*.22-w0,r0,y0,c0);point(out,ps,angle+t0*.22+w0,r0,y0,c0);
            point(out,ps,angle+t1*.22+w1,r1,y1,c1);point(out,ps,angle+t1*.22-w1,r1,y1,c1);
        }
    }
    private static void point(VertexConsumer out,PoseStack ps,double angle,double radius,double y,int alpha){
        out.vertex(ps.last().pose(),(float)(Math.cos(angle)*radius),(float)y,(float)(Math.sin(angle)*radius))
            .color(232,244,255,alpha).endVertex();
    }
    private MeleeWindClient(){}
}
