package cn.blockforge.generated.nnnnr1201forge2;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import java.util.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.mojang.logging.LogUtils;

/** 原样读取 Blockbench 5 模型；保留部件 UV、三轴旋转和骨骼层级。 */
public final class ShrineModel {
    private record Face(Vector3f[] points,float[] uv,Vector3f normal,int texture,boolean bone){}
    private static List<Face> faces;
    public static double topHeight(){
        if(faces==null)load();
        double top=0;
        for(Face face:faces)for(Vector3f point:face.points)top=Math.max(top,point.y);
        return top+0.1;
    }
    private static float[] vec(JsonObject o,String key,float[] fallback){if(!o.has(key))return fallback;JsonArray a=o.getAsJsonArray(key);return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()};}
    private static Matrix4f transform(JsonObject o){float[] r=vec(o,"rotation",new float[3]),p=vec(o,"origin",new float[3]);return new Matrix4f().translation(p[0],p[1],p[2]).rotateZ((float)Math.toRadians(r[2])).rotateY((float)Math.toRadians(r[1])).rotateX((float)Math.toRadians(r[0])).translate(-p[0],-p[1],-p[2]);}
    private static void load(){
        faces=new ArrayList<>();
        try(var in=Minecraft.getInstance().getResourceManager().open(new ResourceLocation("sukuna","models/shrine.bbmodel"));var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){
            JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();Map<String,JsonObject> elements=new HashMap<>(),groups=new HashMap<>();
            for(JsonElement e:root.getAsJsonArray("elements")){JsonObject o=e.getAsJsonObject();elements.put(o.get("uuid").getAsString(),o);}
            if(root.has("groups"))for(JsonElement e:root.getAsJsonArray("groups")){JsonObject o=e.getAsJsonObject();groups.put(o.get("uuid").getAsString(),o);}
            float tw=root.getAsJsonObject("resolution").get("width").getAsFloat(),th=root.getAsJsonObject("resolution").get("height").getAsFloat();
            for(JsonElement e:root.getAsJsonArray("outliner"))walk(e,new Matrix4f(),elements,groups,tw,th);
        }catch(Exception e){LogUtils.getLogger().error("无法读取御厨子原始模型",e);}
    }
    private static void walk(JsonElement node,Matrix4f parent,Map<String,JsonObject> elements,Map<String,JsonObject> groups,float tw,float th){
        if(node.isJsonPrimitive()){JsonObject cube=elements.get(node.getAsString());if(cube!=null)cube(cube,new Matrix4f(parent).mul(transform(cube)),tw,th);return;}
        JsonObject o=node.getAsJsonObject(),g=o.has("uuid")?groups.getOrDefault(o.get("uuid").getAsString(),o):o;
        Matrix4f m=new Matrix4f(parent).mul(transform(g));if(o.has("children"))for(JsonElement c:o.getAsJsonArray("children"))walk(c,m,elements,groups,tw,th);
    }
    private static void cube(JsonObject c,Matrix4f matrix,float tw,float th){
        if(c.has("visibility")&&!c.get("visibility").getAsBoolean())return;
        float[] a=vec(c,"from",new float[3]),b=vec(c,"to",new float[3]);float x=a[0],y=a[1],z=a[2],X=b[0],Y=b[1],Z=b[2];
        float[][][] quads={{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}},{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}},{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}},{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}}};
        String[] names={"north","south","west","east","up","down"};
        JsonObject fs=c.getAsJsonObject("faces");
        for(int i=0;i<6;i++){
            if(!fs.has(names[i]))continue;JsonObject f=fs.getAsJsonObject(names[i]);if(!f.has("texture")||f.get("texture").isJsonNull())continue;
            Vector3f[] v=new Vector3f[4];for(int j=0;j<4;j++)v[j]=matrix.transformPosition(new Vector3f(quads[i][j][0],quads[i][j][1],quads[i][j][2])).div(16);
            JsonArray u=f.getAsJsonArray("uv");float u0=u.get(0).getAsFloat()/tw,v0=u.get(1).getAsFloat()/th,u1=u.get(2).getAsFloat()/tw,v1=u.get(3).getAsFloat()/th;
            float[] uv={u0,v1,u1,v1,u1,v0,u0,v0};int rot=f.has("rotation")?f.get("rotation").getAsInt()/90:0;if(rot!=0){float[] old=uv.clone();for(int j=0;j<4;j++){int k=(j+rot)%4;uv[j*2]=old[k*2];uv[j*2+1]=old[k*2+1];}}
            Vector3f n=new Vector3f(v[1]).sub(v[0]).cross(new Vector3f(v[2]).sub(v[0]));if(n.lengthSquared()>0)n.normalize();else n.set(0,1,0);
            String name=c.has("name")?c.get("name").getAsString().toLowerCase(java.util.Locale.ROOT):"";
            faces.add(new Face(v,uv,n,f.get("texture").getAsInt(),name.contains("骨")||name.contains("bone")||name.contains("skull")));
        }
    }
    public static void render(PoseStack poses,MultiBufferSource buffers,boolean burning,boolean soul,float age){if(faces==null)load();var pose=poses.last();for(Face f:faces){boolean ember=burning&&f.bone;VertexConsumer out=(soul||ember)?buffers.getBuffer(RenderType.entityTranslucent(new ResourceLocation("sukuna","textures/shrine_"+f.texture+".png"))):buffers.getBuffer(RenderType.entityCutoutNoCull(new ResourceLocation("sukuna","textures/shrine_"+f.texture+".png")));for(int i=0;i<4;i++){Vector3f p=f.points[i];int red=soul?90:255,green=soul?205:(ember?185:255),blue=soul?255:(ember?65:255),alpha=soul?150:(ember?(int)(255-Math.min(160,age*2)):255);out.vertex(pose.pose(),p.x,p.y,p.z).color(red,green,blue,alpha).uv(f.uv[i*2],f.uv[i*2+1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(pose.normal(),f.normal.x,f.normal.y,f.normal.z).endVertex();}}}
}
