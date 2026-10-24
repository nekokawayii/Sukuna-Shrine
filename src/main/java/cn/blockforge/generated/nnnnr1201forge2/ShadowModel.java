package cn.blockforge.generated.nnnnr1201forge2;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 原样读取十影 bbmodel。网格顶点相对自身 origin；立方体仍使用绝对坐标。
 * 几何与关键帧只编译一次，不在每帧重复解析数万条 JSON。 */
public final class ShadowModel {
    private record Vertex(Vector3f position,float u,float v){}
    private record Face(Vertex[] vertices,Vector3f normal,int texture){}
    private record Key(float time,Vector3f value,boolean step,boolean smooth){}
    private record Track(List<Key> rotation,List<Key> position,List<Key> scale){}
    private record Animation(float length,boolean loop,Map<String,Track> tracks){}
    private record Bone(String id,Vector3f origin,Vector3f rotation,List<Bone> children,List<Face> faces){}
    private final String id;
    private final String folder;
    private final List<Bone> roots=new ArrayList<>();
    private final Map<String,Animation> animations=new HashMap<>();
    private ResourceLocation[] textures;
    private final Vector3f minimum=new Vector3f(Float.POSITIVE_INFINITY),maximum=new Vector3f(Float.NEGATIVE_INFINITY);
    private float width,height,centerX,centerZ,minY;
    public ShadowModel(String id){this("ten_shadows",id);}
    public ShadowModel(String folder,String id){this.folder=folder;this.id=id;load();}
    public float height(){return height;}
    public float width(){return width;}
    public float minY(){return minY;}
    public float centerX(){return centerX;}
    public float centerZ(){return centerZ;}
    public boolean has(String name){return animations.containsKey(name);}
    private static Vector3f vector(JsonObject o,String name){if(!o.has(name))return new Vector3f();JsonArray a=o.getAsJsonArray(name);return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());}
    private static float n(JsonObject o,String key,float fallback){return o.has(key)?o.get(key).getAsFloat():fallback;}
    private static Matrix4f transform(Vector3f origin,Vector3f degrees,boolean mesh){
        Matrix4f m=new Matrix4f().translation(origin).rotateZ((float)Math.toRadians(degrees.z)).rotateY((float)Math.toRadians(degrees.y)).rotateX((float)Math.toRadians(degrees.x));
        return mesh?m:m.translate(-origin.x,-origin.y,-origin.z);
    }
    private void load(){
        try(var in=Minecraft.getInstance().getResourceManager().open(new ResourceLocation("sukuna","models/"+folder+"/"+id+".bbmodel"));var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){
            JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();
            Map<String,JsonObject> elements=new HashMap<>(),groups=new HashMap<>();
            for(JsonElement e:root.getAsJsonArray("elements")){JsonObject o=e.getAsJsonObject();elements.put(o.get("uuid").getAsString(),o);}
            if(root.has("groups"))for(JsonElement e:root.getAsJsonArray("groups")){JsonObject o=e.getAsJsonObject();groups.put(o.get("uuid").getAsString(),o);}
            textures=new ResourceLocation[root.getAsJsonArray("textures").size()];for(int i=0;i<textures.length;i++)textures[i]=new ResourceLocation("sukuna","textures/entity/"+folder+"/"+id+"_"+i+".png");
            float tw=root.getAsJsonObject("resolution").get("width").getAsFloat(),th=root.getAsJsonObject("resolution").get("height").getAsFloat();
            for(JsonElement node:root.getAsJsonArray("outliner"))roots.add(compile(node,elements,groups,tw,th,new Matrix4f()));
            for(JsonElement e:root.getAsJsonArray("animations")){
                JsonObject a=e.getAsJsonObject();Map<String,Track> tracks=new HashMap<>();
                for(var entry:a.getAsJsonObject("animators").entrySet()){
                    JsonObject animator=entry.getValue().getAsJsonObject();if(!animator.has("keyframes"))continue;
                    List<Key> rotation=new ArrayList<>(),position=new ArrayList<>(),scale=new ArrayList<>();
                    for(JsonElement k:animator.getAsJsonArray("keyframes")){
                        JsonObject key=k.getAsJsonObject();String channel=key.get("channel").getAsString();JsonObject point=key.getAsJsonArray("data_points").get(0).getAsJsonObject();float fallback=channel.equals("scale")?1:0;
                        Vector3f value=new Vector3f(n(point,"x",fallback),n(point,"y",fallback),n(point,"z",fallback));String interp=key.has("interpolation")?key.get("interpolation").getAsString():"linear";
                        Key frame=new Key(n(key,"time",0),value,interp.equals("step"),interp.equals("catmullrom"));
                        switch(channel){case "rotation"->rotation.add(frame);case "position"->position.add(frame);case "scale"->scale.add(frame);default->{}}
                    }
                    for(var list:List.of(rotation,position,scale))list.sort(Comparator.comparingDouble(Key::time));tracks.put(entry.getKey(),new Track(rotation,position,scale));
                }
                animations.put(a.get("name").getAsString(),new Animation(Math.max(.001f,n(a,"length",1)),a.has("loop")&&a.get("loop").getAsString().equals("loop"),tracks));
            }
            height=(maximum.y-minimum.y)/16;width=Math.max(maximum.x-minimum.x,maximum.z-minimum.z)/16;centerX=(minimum.x+maximum.x)/32;centerZ=(minimum.z+maximum.z)/32;minY=minimum.y/16;
            if(!Float.isFinite(height)||height<=0)throw new IllegalArgumentException("模型无可见几何");
        }catch(Exception e){throw new IllegalStateException("无法加载十影原始模型："+id,e);}
    }
    private Bone compile(JsonElement node,Map<String,JsonObject> elements,Map<String,JsonObject> groups,float tw,float th,Matrix4f parent){
        if(node.isJsonPrimitive()){
            JsonObject e=elements.get(node.getAsString());if(e==null)throw new IllegalArgumentException("模型引用丢失："+node);
            List<Face> faces=new ArrayList<>();if(e.has("visibility")&&!e.get("visibility").getAsBoolean())return new Bone("",new Vector3f(),new Vector3f(),List.of(),faces);
            boolean mesh=e.has("type")&&e.get("type").getAsString().equals("mesh");Matrix4f local=transform(vector(e,"origin"),vector(e,"rotation"),mesh);
            JsonObject fs=e.getAsJsonObject("faces");
            if(mesh){
                JsonObject vertices=e.getAsJsonObject("vertices");
                for(var entry:fs.entrySet()){
                    JsonObject f=entry.getValue().getAsJsonObject();if(!f.has("texture")||f.get("texture").isJsonNull())continue;
                    JsonArray names=f.getAsJsonArray("vertices");List<Vertex> pts=new ArrayList<>();
                    for(JsonElement name:names){String key=name.getAsString();JsonArray uv=f.getAsJsonObject("uv").getAsJsonArray(key);Vector3f p=local.transformPosition(vector(vertices,key));pts.add(new Vertex(p,uv.get(0).getAsFloat()/tw,uv.get(1).getAsFloat()/th));}
                    for(int i=1;i+1<pts.size();i++)face(faces,new Vertex[]{pts.get(0),pts.get(i),pts.get(i+1),pts.get(i+1)},f.get("texture").getAsInt(),parent);
                }
            }else{
                Vector3f a=vector(e,"from"),b=vector(e,"to");float inf=n(e,"inflate",0);a.sub(inf,inf,inf);b.add(inf,inf,inf);float x=a.x,y=a.y,z=a.z,X=b.x,Y=b.y,Z=b.z;
                float[][][] quads={{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}},{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}},{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}},{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}}};String[] names={"north","south","west","east","up","down"};
                for(int i=0;i<6;i++){
                    if(!fs.has(names[i]))continue;JsonObject f=fs.getAsJsonObject(names[i]);if(!f.has("texture")||f.get("texture").isJsonNull())continue;JsonArray uv=f.getAsJsonArray("uv");float u=uv.get(0).getAsFloat()/tw,v=uv.get(1).getAsFloat()/th,U=uv.get(2).getAsFloat()/tw,V=uv.get(3).getAsFloat()/th;float[][] coords={{u,V},{U,V},{U,v},{u,v}};Vertex[] pts=new Vertex[4];int rot=(int)n(f,"rotation",0)/90;
                    for(int j=0;j<4;j++){int k=(j+rot)%4;pts[j]=new Vertex(local.transformPosition(new Vector3f(quads[i][j])),coords[k][0],coords[k][1]);}face(faces,pts,f.get("texture").getAsInt(),parent);
                }
            }
            return new Bone("",new Vector3f(),new Vector3f(),List.of(),faces);
        }
        JsonObject n=node.getAsJsonObject();String uuid=n.get("uuid").getAsString();JsonObject g=groups.getOrDefault(uuid,n);Vector3f origin=vector(g,"origin"),rotation=vector(g,"rotation");Matrix4f world=new Matrix4f(parent).mul(transform(origin,rotation,false));List<Bone> children=new ArrayList<>();
        List<Face> staticFaces=new ArrayList<>();
        for(JsonElement child:n.getAsJsonArray("children")){
            Bone compiled=compile(child,elements,groups,tw,th,world);
            // 静态方块已烘焙局部变换，合入所属骨骼，避免法阵数千个部件每帧重复分配矩阵。
            if(compiled.id.isEmpty()&&compiled.children.isEmpty())staticFaces.addAll(compiled.faces);else children.add(compiled);
        }
        return new Bone(uuid,origin,rotation,children,staticFaces);
    }
    private void face(List<Face> out,Vertex[] vertices,int texture,Matrix4f world){
        if(texture<0||texture>=textures.length)throw new IllegalArgumentException("模型贴图索引越界");
        Vector3f normal=new Vector3f(vertices[1].position).sub(vertices[0].position).cross(new Vector3f(vertices[2].position).sub(vertices[0].position));if(normal.lengthSquared()<1e-12)return;normal.normalize();
        for(Vertex vertex:vertices){Vector3f p=world.transformPosition(new Vector3f(vertex.position));minimum.min(p);maximum.max(p);}out.add(new Face(vertices,normal,texture));
    }
    private static Vector3f sample(List<Key> keys,float time,float fallback){
        if(keys.isEmpty())return new Vector3f(fallback);Key a=keys.get(0),b=a;
        for(Key k:keys){if(k.time<=time)a=k;if(k.time>=time){b=k;break;}b=k;}
        float t=Math.max(0,Math.min(1,(time-a.time)/Math.max(.0001f,b.time-a.time)));if(a.step)t=0;else if(a.smooth)t=t*t*(3-2*t);return new Vector3f(a.value).lerp(b.value,t);
    }
    public void render(PoseStack ps,MultiBufferSource buffers,int light,String name,float seconds,float alpha){
        Animation anim=animations.get(name);float time=anim==null?0:anim.loop?seconds%anim.length:Math.min(seconds,anim.length);VertexConsumer[] consumers=new VertexConsumer[textures.length];
        for(int i=0;i<consumers.length;i++)consumers[i]=buffers.getBuffer(alpha<.999f?RenderType.entityTranslucent(textures[i]):RenderType.entityCutoutNoCull(textures[i]));
        for(Bone bone:roots)draw(bone,new Matrix4f(),anim,time,ps,consumers,light,alpha);
    }
    private void draw(Bone bone,Matrix4f parent,Animation animation,float time,PoseStack ps,VertexConsumer[] consumers,int light,float alpha){
        Vector3f r=new Vector3f(bone.rotation),p=new Vector3f(),s=new Vector3f(1);Track track=animation==null?null:animation.tracks.get(bone.id);
        if(track!=null){r.add(sample(track.rotation,time,0));p=sample(track.position,time,0);s=sample(track.scale,time,1);}
        if(Math.abs(s.x*s.y*s.z)<1e-8f)return; // 原模型用零缩放隐藏部件，不能求奇异矩阵的法线。
        Matrix4f local=new Matrix4f().translation(bone.origin.x+p.x,bone.origin.y+p.y,bone.origin.z+p.z).rotateZ((float)Math.toRadians(r.z)).rotateY((float)Math.toRadians(r.y)).rotateX((float)Math.toRadians(r.x)).scale(s).translate(-bone.origin.x,-bone.origin.y,-bone.origin.z);
        Matrix4f m=new Matrix4f(parent).mul(local);
        if(!bone.faces.isEmpty()){
            Matrix4f combined=new Matrix4f(ps.last().pose()).scale(1f/16).mul(m);org.joml.Matrix3f normalMatrix=m.normal(new org.joml.Matrix3f());Vector3f normal=new Vector3f();
            for(Face face:bone.faces){normalMatrix.transform(face.normal,normal).normalize();VertexConsumer out=consumers[face.texture];for(Vertex vertex:face.vertices){Vector3f at=vertex.position;out.vertex(combined,at.x,at.y,at.z).color(1,1,1,alpha).uv(vertex.u,vertex.v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(ps.last().normal(),normal.x,normal.y,normal.z).endVertex();}}
        }
        for(Bone child:bone.children)draw(child,m,animation,time,ps,consumers,light,alpha);
    }
}
