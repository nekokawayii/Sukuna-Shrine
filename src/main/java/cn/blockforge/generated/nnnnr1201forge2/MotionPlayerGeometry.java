package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.core.Direction;

/** 保持原版尺寸和皮肤UV，将12像素四肢拆成6+6，让屈肘护脸和屈膝蓄力真正可见。 */
public final class MotionPlayerGeometry {
    public static ModelPart player(boolean slim){
        ModelPart base=LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot();
        Map<String,ModelPart> parts=parts(base,true);
        parts.put("left_arm",arm(true,slim,32,48,0,false,64));parts.put("right_arm",arm(false,slim,40,16,0,false,64));
        parts.put("left_sleeve",arm(true,slim,48,48,.25f,false,64));parts.put("right_sleeve",arm(false,slim,40,32,.25f,false,64));
        parts.put("left_leg",leg(1.9f,16,48,0,false,64));parts.put("right_leg",leg(-1.9f,0,16,0,false,64));
        parts.put("left_pants",leg(1.9f,0,48,0.25f,false,64));parts.put("right_pants",leg(-1.9f,0,32,0.25f,false,64));
        return new ModelPart(List.of(),parts);
    }
    public static HumanoidModel<net.minecraft.client.player.AbstractClientPlayer> armor(float inflate){
        ModelPart base=LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(inflate),0),64,32).bakeRoot();
        Map<String,ModelPart> parts=parts(base,false);
        parts.put("left_arm",arm(true,false,40,16,inflate,true,32));parts.put("right_arm",arm(false,false,40,16,inflate,false,32));
        parts.put("left_leg",leg(1.9f,0,16,inflate,true,32));parts.put("right_leg",leg(-1.9f,0,16,inflate,false,32));
        return new HumanoidModel<>(new ModelPart(List.of(),parts));
    }
    private static Map<String,ModelPart> parts(ModelPart base,boolean player){
        Map<String,ModelPart> parts=new HashMap<>();
        for(String name:List.of("head","hat","body","left_arm","right_arm","left_leg","right_leg"))parts.put(name,base.getChild(name));
        if(player)for(String name:List.of("left_sleeve","right_sleeve","left_pants","right_pants","jacket","cloak","ear"))parts.put(name,base.getChild(name));
        return parts;
    }
    private static ModelPart leg(float x,int u,int v,float inflate,boolean mirror,int textureHeight){
        var knee=new ModelPart(half(u,v,6,inflate,mirror,textureHeight),Map.of());knee.setPos(0,6,0);
        var leg=new ModelPart(half(u,v,0,inflate,mirror,textureHeight),Map.of("knee",knee));leg.setPos(x,12,0);return leg;
    }
    private static List<ModelPart.Cube> half(int u,int v,int cut,float inflate,boolean mirror,int height){
        // 侧面截取上/下六像素；几何膨胀不改变UV，脚底沿用原版取样。
        return List.of(new ModelPart.Cube(u,v+cut,-2,0,-2,4,6,4,inflate,inflate,inflate,mirror,64,height,EnumSet.of(Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST)),
            new ModelPart.Cube(u,v,-2,0,-2,4,6,4,inflate,inflate,inflate,mirror,64,height,EnumSet.of(Direction.UP,Direction.DOWN)));
    }
    private static ModelPart arm(boolean left,boolean slim,int u,int v,float inflate,boolean mirror,int height){
        int width=slim?3:4;float x=left?-1:slim?-2:-3;
        var elbow=new ModelPart(armHalf(u,v,6,x,0,width,inflate,mirror,height),Map.of());elbow.setPos(0,4,0);
        var arm=new ModelPart(armHalf(u,v,0,x,-2,width,inflate,mirror,height),Map.of("elbow",elbow));
        arm.setPos(left?5:-5,slim?2.5f:2,0);return arm;
    }
    private static List<ModelPart.Cube> armHalf(int u,int v,int cut,float x,float y,int width,float inflate,boolean mirror,int height){
        // 侧面取原皮肤上下各六像素；顶底取原手臂端面，窄臂维持三像素宽。
        return List.of(new ModelPart.Cube(u,v+cut,x,y,-2,width,6,4,inflate,inflate,inflate,mirror,64,height,EnumSet.of(Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST)),
            new ModelPart.Cube(u,v,x,y,-2,width,6,4,inflate,inflate,inflate,mirror,64,height,EnumSet.of(Direction.UP,Direction.DOWN)));
    }
    /** 专用动作模型会轮流画多个玩家，每次必须先清掉上一个人的关节和枢轴。 */
    public static void reset(PlayerModel<?> model,boolean slim){
        for(var part:List.of(model.head,model.hat,model.body,model.leftArm,model.rightArm,model.leftLeg,model.rightLeg,
            model.jacket,model.leftSleeve,model.rightSleeve,model.leftPants,model.rightPants)){
            part.xRot=part.yRot=part.zRot=0;part.skipDraw=false;
        }
        model.head.setPos(0,0,0);model.body.setPos(0,0,0);
        model.leftArm.setPos(5,slim?2.5f:2,0);model.rightArm.setPos(-5,slim?2.5f:2,0);
        model.leftLeg.setPos(1.9f,12,0);model.rightLeg.setPos(-1.9f,12,0);
        knees(model,0,0);elbows(model,0,0);model.crouching=false;
    }
    public static void elbows(HumanoidModel<?> model,float left,float right){
        if(model.leftArm.hasChild("elbow"))model.leftArm.getChild("elbow").xRot=left;
        if(model.rightArm.hasChild("elbow"))model.rightArm.getChild("elbow").xRot=right;
        if(model instanceof PlayerModel<?> p){
            if(p.leftSleeve.hasChild("elbow"))p.leftSleeve.getChild("elbow").xRot=left;
            if(p.rightSleeve.hasChild("elbow"))p.rightSleeve.getChild("elbow").xRot=right;
        }
    }
    public static void knees(HumanoidModel<?> model,float left,float right){
        if(model.leftLeg.hasChild("knee"))model.leftLeg.getChild("knee").xRot=left;
        if(model.rightLeg.hasChild("knee"))model.rightLeg.getChild("knee").xRot=right;
        if(model instanceof PlayerModel<?> player){player.leftPants.getChild("knee").xRot=left;player.rightPants.getChild("knee").xRot=right;}
    }
    private MotionPlayerGeometry(){}
}
