package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.core.Direction;

/** 保持原版尺寸和皮肤UV，只将12像素腿拆成6+6，新增膝关节。 */
public final class MotionPlayerGeometry {
    public static ModelPart player(boolean slim){
        ModelPart base=LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot();
        Map<String,ModelPart> parts=parts(base,true);
        parts.put("left_leg",leg(1.9f,16,48,0,false,64));parts.put("right_leg",leg(-1.9f,0,16,0,false,64));
        parts.put("left_pants",leg(1.9f,0,48,0.25f,false,64));parts.put("right_pants",leg(-1.9f,0,32,0.25f,false,64));
        return new ModelPart(List.of(),parts);
    }
    public static HumanoidModel<net.minecraft.client.player.AbstractClientPlayer> armor(float inflate){
        ModelPart base=LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(inflate),0),64,32).bakeRoot();
        Map<String,ModelPart> parts=parts(base,false);
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
    public static void knees(HumanoidModel<?> model,float left,float right){
        if(model.leftLeg.hasChild("knee"))model.leftLeg.getChild("knee").xRot=left;
        if(model.rightLeg.hasChild("knee"))model.rightLeg.getChild("knee").xRot=right;
        if(model instanceof PlayerModel<?> player){player.leftPants.getChild("knee").xRot=left;player.rightPants.getChild("knee").xRot=right;}
    }
    private MotionPlayerGeometry(){}
}
