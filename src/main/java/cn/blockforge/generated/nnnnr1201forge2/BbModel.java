package cn.blockforge.generated.nnnnr1201forge2;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 直接使用原始 bbmodel，支持三轴部件、骨骼及关键帧动画。 */
public final class BbModel {
    private final String name,texture;
    private JsonObject root;
    private int action;
    public void action(int action){this.action=action;}
    private Matrix4f pose(JsonObject o,Matrix4f m){if(!o.has("name"))return m;String n=o.get("name").getAsString();boolean seated=action==1||action==9||action==10;float[] p=vec(o,"origin");if(name.equals("daitian")&&action>0&&n.equals("shoulder_r"))m.translate(p[0],p[1],p[2]).rotateX(-1.2f).translate(-p[0],-p[1],-p[2]);if(seated&&name.equals("buddha")&&n.equals("root"))m.translate(0,-124,0);if(action==11&&(n.startsWith("arm")||n.equals("chest")))m.translate(n.endsWith("L")?-2:n.endsWith("R")?2:0,0,0);return m;}
    private final Map<String,JsonObject> cubes=new HashMap<>(),groups=new HashMap<>();
    private record Face(float[] vertices,float[] uv,Vector3f normal,int texture){}
    private final Map<JsonObject,List<Face>> geometry=new IdentityHashMap<>();
    private final Map<JsonObject,Matrix4f> staticTransforms=new IdentityHashMap<>();
    private final Map<Integer,ResourceLocation> textures=new HashMap<>();
    private final Map<Integer,RenderType> opaqueTypes=new HashMap<>(),transparentTypes=new HashMap<>();
    private final Vector3f scratchNormal=new Vector3f();
    private Matrix4f fixed(JsonObject o){return staticTransforms.computeIfAbsent(o,k->transform(k,null,0));}
    /** 索引：uuid(原样) / 规范化 uuid（压缩前导零，解决 Gson 把 "0000...9e3779b1" 读成数字后 toString 丢零的坑） / 名字。 */
    private final Map<String,JsonObject> byKey=new HashMap<>();
    /** 把 uuid 规范成"去掉连字符与前导零"的形式：两边做同样的规范化才能对上。 */
    private static String canon(String uuid){
        if(uuid==null)return "";
        StringBuilder sb=new StringBuilder(uuid.length());
        for(int i=0;i<uuid.length();i++){
            char c=uuid.charAt(i);
            if(c!='-')sb.append(Character.toLowerCase(c));
        }
        String hex=sb.toString();
        int i=0;while(i<hex.length()-1&&hex.charAt(i)=='0')i++;
        return hex.substring(i);
    }
    private static String objUuid(JsonObject o){
        if(!o.has("uuid"))return "";
        var e=o.get("uuid");
        return e.isJsonPrimitive()?e.getAsString():e.toString(); // 数字型 uuid 也要能取出来
    }
    /** 解析模型里的引用：先用 uuid 找，再退回规范化 uuid 与名字。 */
    private JsonObject resolve(JsonElement node){
        if(node==null)return null;
        if(node.isJsonPrimitive()){
            String key=node.getAsString();
            JsonObject hit=cubes.get(key);
            if(hit==null)hit=groups.get(key);
            if(hit==null)hit=byKey.get(key);
            if(hit==null)hit=byKey.get(canon(key));
            return hit;
        }
        return null;
    }
    public BbModel(String name,String texture){this.name=name;this.texture=texture;}
    /** 某个面使用的贴图（多张贴图模型：第 index 张）。 */
    /** 提前加载模型并量出高度（格）：按 y 轴包围盒 /16。供刷怪与缩放使用。 */
    public void prepare(){if(root==null)load();}
    public static BbModel prepare(net.minecraft.server.packs.resources.ResourceManager resources,String name){
        BbModel model=new BbModel(name,name);model.load(resources);
        for(JsonObject element:model.cubes.values()){model.geometry.put(element,model.bake(element));model.fixed(element);}
        return model;
    }
    /** 模型高度（格，按 y 包围盒）。 */
    private float[] bounds;
    public float heightBlocks(){measure();return Math.max(.05f,bounds[4]-bounds[1]);}
    public void fitHeight(PoseStack ps,float height){measure();float s=height/heightBlocks();ps.scale(s,s,s);ps.translate(-(bounds[0]+bounds[3])/2,-bounds[1],-(bounds[2]+bounds[5])/2);}
    private void measure(){
        if(root==null)load();if(bounds!=null)return;
        bounds=new float[]{Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        for(JsonElement node:root.getAsJsonArray("outliner"))measureNode(node,new Matrix4f());
        if(bounds[0]>bounds[3])bounds=new float[]{0,0,0,1,1,1};
    }
    private void include(Matrix4f matrix,float x,float y,float z){Vector3f p=matrix.transformPosition(new Vector3f(x,y,z)).div(16);float[] v={p.x,p.y,p.z};for(int i=0;i<3;i++){bounds[i]=Math.min(bounds[i],v[i]);bounds[i+3]=Math.max(bounds[i+3],v[i]);}}
    private void measureNode(JsonElement node,Matrix4f parent){
        if(node.isJsonPrimitive()){
            JsonObject e=resolve(node);if(e==null||e.has("visibility")&&!e.get("visibility").getAsBoolean())return;
            Matrix4f m=new Matrix4f(parent).mul(transform(e,null,0));
            if(e.has("vertices")){for(var entry:e.getAsJsonObject("vertices").entrySet()){var p=entry.getValue().getAsJsonArray();include(m,p.get(0).getAsFloat(),p.get(1).getAsFloat(),p.get(2).getAsFloat());}}
            else{float[] a=vec(e,"from"),b=vec(e,"to");float inflate=number(e,"inflate",0);for(int i=0;i<8;i++)include(m,(i&1)==0?a[0]-inflate:b[0]+inflate,(i&2)==0?a[1]-inflate:b[1]+inflate,(i&4)==0?a[2]-inflate:b[2]+inflate);}return;
        }
        JsonObject n=node.getAsJsonObject(),g=n.has("uuid")?resolve(n.get("uuid")):n;if(g==null)g=n;
        Matrix4f m=new Matrix4f(parent).mul(transform(g,null,0));if(n.has("children"))for(var child:n.getAsJsonArray("children"))measureNode(child,m);
    }
    /** 模型是否带某段动画。 */
    public boolean hasAnimation(String animation){
        if(root==null)load();
        if(!root.has("animations"))return false;
        for(JsonElement e:root.getAsJsonArray("animations"))if(e.getAsJsonObject().get("name").getAsString().equals(animation))return true;
        return false;
    }
    public ResourceLocation texture(int index){return textures.computeIfAbsent(index,k->new ResourceLocation("jujutsu_kaisen_cursed_paths","textures/entity/"+texture+"_"+k+".png"));}
    private float textureSize(int index,boolean width){
        String key=width?"uv_width":"uv_height",fallback=width?"width":"height";
        if(root.has("textures")&&index>=0&&index<root.getAsJsonArray("textures").size()){
            JsonObject t=root.getAsJsonArray("textures").get(index).getAsJsonObject();
            if(t.has(key)&&t.get(key).getAsFloat()>0)return t.get(key).getAsFloat();
        }
        return root.getAsJsonObject("resolution").get(fallback).getAsFloat();
    }
    private static float number(JsonObject o,String key,float fallback){return o.has(key)?o.get(key).getAsFloat():fallback;}
    private static float[] vec(JsonObject o,String key){if(!o.has(key))return new float[3];JsonArray a=o.getAsJsonArray(key);return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()};}
    private void load(){load(Minecraft.getInstance().getResourceManager());}
    private void load(net.minecraft.server.packs.resources.ResourceManager resources){try(var in=resources.open(new ResourceLocation("jujutsu_kaisen_cursed_paths","models/"+name+".bbmodel"));var r=new InputStreamReader(in,StandardCharsets.UTF_8)){root=JsonParser.parseReader(r).getAsJsonObject();if(root.has("textures"))for(JsonElement tex:root.getAsJsonArray("textures"))tex.getAsJsonObject().remove("source");for(JsonElement e:root.getAsJsonArray("elements")){JsonObject o=e.getAsJsonObject();String u=objUuid(o);cubes.put(u,o);byKey.put(u,o);byKey.put(canon(u),o);if(o.has("name"))byKey.putIfAbsent(o.get("name").getAsString(),o);}if(root.has("groups"))for(JsonElement e:root.getAsJsonArray("groups")){JsonObject o=e.getAsJsonObject();String u=objUuid(o);groups.put(u,o);byKey.put(u,o);byKey.put(canon(u),o);if(o.has("name"))byKey.putIfAbsent(o.get("name").getAsString(),o);}}catch(Exception e){throw new IllegalStateException("无法读取模型 "+name,e);}}
    // 每个模型实例只整理一次关键帧；不在每帧、每骨骼、每通道反复排序分配列表。
    private final Map<JsonObject,Map<String,List<JsonObject>>> channels=new IdentityHashMap<>();
    private float[] sample(JsonObject animator,String channel,float time){
        List<JsonObject> keys=channels.computeIfAbsent(animator,a->{
            Map<String,List<JsonObject>> tracks=new HashMap<>();
            if(a.has("keyframes"))for(JsonElement e:a.getAsJsonArray("keyframes")){
                JsonObject k=e.getAsJsonObject();tracks.computeIfAbsent(k.get("channel").getAsString(),v->new ArrayList<>()).add(k);
            }
            tracks.values().forEach(list->list.sort(Comparator.comparingDouble(k->number(k,"time",0))));return tracks;
        }).getOrDefault(channel,List.of());
        if(keys.isEmpty())return channel.equals("scale")?new float[]{1,1,1}:new float[3];JsonObject a=keys.get(0),b=a;for(JsonObject k:keys){if(number(k,"time",0)<=time)a=k;if(number(k,"time",0)>=time){b=k;break;}b=k;}float t=Math.max(0,Math.min(1,(time-number(a,"time",0))/Math.max(0.0001f,number(b,"time",0)-number(a,"time",0))));if(a.has("interpolation")&&a.get("interpolation").getAsString().equals("step"))t=0;else if(a.has("interpolation")&&a.get("interpolation").getAsString().equals("catmullrom"))t=t*t*(3-2*t);JsonObject x=a.getAsJsonArray("data_points").get(0).getAsJsonObject(),y=b.getAsJsonArray("data_points").get(0).getAsJsonObject();String[] axes={"x","y","z"};float[] out=new float[3];for(int i=0;i<3;i++){float av=number(x,axes[i],channel.equals("scale")?1:0),bv=number(y,axes[i],av);out[i]=av+(bv-av)*t;}return out;}
    public void render(PoseStack ps,MultiBufferSource buffers,int light,String animation,float seconds,float red,float green,float blue,float alpha){if(root==null)load();JsonObject anim=null;if(root.has("animations"))for(JsonElement e:root.getAsJsonArray("animations")){JsonObject a=e.getAsJsonObject();if(a.get("name").getAsString().equals(animation)){anim=a;break;}}float time=seconds;if(anim!=null){float length=number(anim,"length",1);time=anim.get("loop").getAsString().equals("loop")?seconds%length:Math.min(seconds,length);anim=anim.getAsJsonObject("animators");}for(JsonElement node:root.getAsJsonArray("outliner"))walk(node,new Matrix4f(),anim,time,ps,buffers,light,red,green,blue,alpha);}
    private Matrix4f transform(JsonObject o,JsonObject anim,float time){float[] p=vec(o,"origin"),r=vec(o,"rotation"),pos=new float[3],scale={1,1,1};if(anim!=null&&o.has("uuid")&&anim.has(o.get("uuid").getAsString())){JsonObject a=anim.getAsJsonObject(o.get("uuid").getAsString());float[] ar=sample(a,"rotation",time);for(int i=0;i<3;i++)r[i]+=ar[i];pos=sample(a,"position",time);scale=sample(a,"scale",time);}return new Matrix4f().translation(p[0]+pos[0],p[1]+pos[1],p[2]+pos[2]).rotateZ((float)java.lang.Math.toRadians(r[2])).rotateY((float)java.lang.Math.toRadians(r[1])).rotateX((float)java.lang.Math.toRadians(r[0])).scale(scale[0],scale[1],scale[2]).translate(-p[0],-p[1],-p[2]);}
    private void walk(JsonElement node,Matrix4f parent,JsonObject anim,float time,PoseStack ps,MultiBufferSource buffers,int light,float red,float green,float blue,float alpha){
        if(node.isJsonPrimitive()){
            JsonObject c=resolve(node);
            if(c!=null){
                Matrix4f m=new Matrix4f(parent).mul(fixed(c));
                renderBaked(c,m,ps,buffers,light,red,green,blue,alpha);
            }
            return;
        }JsonObject n=node.getAsJsonObject();JsonObject g=n.has("uuid")?resolve(n.get("uuid")):n;if(g==null)g=n;Matrix4f m=new Matrix4f(parent).mul(pose(g,transform(g,anim,time)));if(n.has("children"))for(JsonElement child:n.getAsJsonArray("children"))walk(child,m,anim,time,ps,buffers,light,red,green,blue,alpha);}
    /** 原始模型只解码一次几何与UV。帧内仅合并骨骼矩阵并提交缓存顶点，保留所有细节。 */
    private void renderBaked(JsonObject element,Matrix4f matrix,PoseStack ps,MultiBufferSource buffers,int light,float red,float green,float blue,float alpha){
        List<Face> faces=geometry.computeIfAbsent(element,this::bake);
        Matrix4f combined=new Matrix4f(ps.last().pose()).scale(1f/16f).mul(matrix);
        org.joml.Matrix3f normalMatrix=new org.joml.Matrix3f(matrix).invert().transpose();
        normalMatrix=new org.joml.Matrix3f(ps.last().normal()).mul(normalMatrix);
        for(Face face:faces){
            RenderType type=(alpha<1?transparentTypes:opaqueTypes).computeIfAbsent(face.texture,k->alpha<1?RenderType.entityTranslucent(texture(k)):RenderType.entityCutoutNoCull(texture(k)));
            VertexConsumer out=buffers.getBuffer(type);
            normalMatrix.transform(face.normal,scratchNormal).normalize();
            float[] xyz=face.vertices,uv=face.uv;
            for(int i=0;i<xyz.length/3;i++)out.vertex(combined,xyz[i*3],xyz[i*3+1],xyz[i*3+2]).color(red,green,blue,alpha)
                .uv(uv[i*2],uv[i*2+1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(scratchNormal.x,scratchNormal.y,scratchNormal.z).endVertex();
        }
    }
    private List<Face> bake(JsonObject c){
        if(c.has("visibility")&&!c.get("visibility").getAsBoolean()||!c.has("faces"))return List.of();
        List<Face> result=new ArrayList<>();JsonObject faces=c.getAsJsonObject("faces");
        if(c.has("vertices")){
            var vertices=c.getAsJsonObject("vertices");
            for(var entry:faces.entrySet()){
                var face=entry.getValue().getAsJsonObject();
                if(!face.has("texture")||face.get("texture").isJsonNull()||!face.has("vertices")||!face.has("uv"))continue;
                var names=face.getAsJsonArray("vertices");if(names.size()<3)continue;
                int tex=face.get("texture").getAsInt();float tw=textureSize(tex,true),th=textureSize(tex,false);
                Vector3f[] points=new Vector3f[names.size()];float[][] uv=new float[names.size()][2];boolean ok=true;
                for(int i=0;i<names.size();i++){
                    String key=names.get(i).getAsString();if(!vertices.has(key)){ok=false;break;}
                    var p=vertices.getAsJsonArray(key);points[i]=new Vector3f(p.get(0).getAsFloat(),p.get(1).getAsFloat(),p.get(2).getAsFloat());
                    var t=face.getAsJsonObject("uv").getAsJsonArray(key);if(t!=null){uv[i][0]=t.get(0).getAsFloat()/tw;uv[i][1]=t.get(1).getAsFloat()/th;}
                }
                if(!ok)continue;
                for(int i=1;i+1<points.length;i++)addFace(result,points,uv,new int[]{0,i,i+1,i+1},tex);
            }
        }else{
            float[] a=vec(c,"from"),b=vec(c,"to");float inf=number(c,"inflate",0);
            float x=a[0]-inf,y=a[1]-inf,z=a[2]-inf,X=b[0]+inf,Y=b[1]+inf,Z=b[2]+inf;
            float[][][] quads={{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}},{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}},{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}},{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}}};
            String[] names={"north","south","west","east","up","down"};
            for(int i=0;i<6;i++){
                if(!faces.has(names[i]))continue;var face=faces.getAsJsonObject(names[i]);
                if(!face.has("texture")||face.get("texture").isJsonNull()||!face.has("uv"))continue;
                int tex=face.get("texture").getAsInt();float tw=textureSize(tex,true),th=textureSize(tex,false);var raw=face.getAsJsonArray("uv");
                float u=raw.get(0).getAsFloat()/tw,v=raw.get(1).getAsFloat()/th,U=raw.get(2).getAsFloat()/tw,V=raw.get(3).getAsFloat()/th;
                float[][] coords={{u,V},{U,V},{U,v},{u,v}},uv=new float[4][2];Vector3f[] points=new Vector3f[4];int rot=Math.floorMod((int)number(face,"rotation",0)/90,4);
                for(int j=0;j<4;j++){points[j]=new Vector3f(quads[i][j]);uv[j]=coords[(j+rot)%4];}
                addFace(result,points,uv,new int[]{0,1,2,3},tex);
            }
        }
        return List.copyOf(result);
    }
    private static void addFace(List<Face> faces,Vector3f[] p,float[][] uv,int[] order,int tex){
        float[] vertices=new float[12],coords=new float[8];for(int j=0;j<4;j++){Vector3f v=p[order[j]];vertices[j*3]=v.x;vertices[j*3+1]=v.y;vertices[j*3+2]=v.z;coords[j*2]=uv[order[j]][0];coords[j*2+1]=uv[order[j]][1];}
        Vector3f normal=new Vector3f(p[order[1]]).sub(p[order[0]]).cross(new Vector3f(p[order[2]]).sub(p[order[0]]));
        if(normal.lengthSquared()<1e-10)normal.set(0,1,0);else normal.normalize();faces.add(new Face(vertices,coords,normal,tex));
    }
    /** Blockbench 的 **mesh 元素**：{@code vertices} 是 {v0:[x,y,z]...}，{@code faces} 每个面给顶点名与逐顶点 UV。
     *  这里按扇形三角化（面通常是四边形），并用 {@code QUADS} 通路画——兼容立方体与网格混用的模型。 */
    private void mesh(JsonObject m,Matrix4f matrix,PoseStack ps,MultiBufferSource buffers,int light,float red,float green,float blue,float alpha){
        if(m.has("visibility")&&!m.get("visibility").getAsBoolean())return;
        JsonObject verts=m.has("vertices")?m.getAsJsonObject("vertices"):null,faces=m.has("faces")?m.getAsJsonObject("faces"):null;
        if(verts==null||faces==null)return;
        float tw=root.getAsJsonObject("resolution").get("width").getAsFloat(),th=root.getAsJsonObject("resolution").get("height").getAsFloat();
        java.util.Map<Integer,VertexConsumer> consumers=new java.util.HashMap<>();
        for(var fe:faces.entrySet()){
            JsonObject f=fe.getValue().getAsJsonObject();
            if(!f.has("vertices")||!f.has("uv")||!f.has("texture")||f.get("texture").isJsonNull())continue;
            JsonArray names=f.getAsJsonArray("vertices");JsonObject uv=f.getAsJsonObject("uv");
            int n=names.size();if(n<3)continue;
            int texIdx=f.get("texture").getAsInt();tw=textureSize(texIdx,true);th=textureSize(texIdx,false);
            VertexConsumer out=consumers.computeIfAbsent(texIdx,k->{
                ResourceLocation tex=new ResourceLocation("jujutsu_kaisen_cursed_paths","textures/entity/"+texture+"_"+k+".png");
                return buffers.getBuffer(alpha<1?RenderType.entityTranslucent(tex):RenderType.entityCutoutNoCull(tex));
            });
            Vector3f[] p=new Vector3f[n];float[] u=new float[n],v=new float[n];
            boolean ok=true;
            for(int i=0;i<n;i++){
                String vn=names.get(i).getAsString();
                if(!verts.has(vn)){ok=false;break;}
                JsonArray pos=verts.getAsJsonArray(vn);
                p[i]=matrix.transformPosition(new Vector3f(pos.get(0).getAsFloat(),pos.get(1).getAsFloat(),pos.get(2).getAsFloat())).div(16);
                JsonArray uvs=uv.has(vn)?uv.getAsJsonArray(vn):null;
                if(uvs!=null&&uvs.size()>=2){u[i]=uvs.get(0).getAsFloat()/tw;v[i]=uvs.get(1).getAsFloat()/th;}
            }
            if(!ok)continue;
            Vector3f nrm=new Vector3f(p[1]).sub(p[0]).cross(new Vector3f(p[2]).sub(p[0]));
            if(nrm.lengthSquared()>0)nrm.normalize();else nrm.set(0,1,0);
            for(int i=1;i+1<n;i++){ // 扇形三角化：第 4 个顶点复制第 3 个（零面积），维持 QUADS 顶点格式
                emit(out,ps,p[0],u[0],v[0],nrm,red,green,blue,alpha,light);
                emit(out,ps,p[i],u[i],v[i],nrm,red,green,blue,alpha,light);
                emit(out,ps,p[i+1],u[i+1],v[i+1],nrm,red,green,blue,alpha,light);
                emit(out,ps,p[i+1],u[i+1],v[i+1],nrm,red,green,blue,alpha,light);
            }
        }
    }
    private static void emit(VertexConsumer out,PoseStack ps,Vector3f p,float u,float v,Vector3f n,float red,float green,float blue,float alpha,int light){
        out.vertex(ps.last().pose(),p.x,p.y,p.z).color(red,green,blue,alpha).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(ps.last().normal(),n.x,n.y,n.z).endVertex();
    }
    private void cube(JsonObject c,Matrix4f matrix,PoseStack ps,MultiBufferSource buffers,int light,float red,float green,float blue,float alpha){if(c.has("visibility")&&!c.get("visibility").getAsBoolean())return;float[] a=vec(c,"from"),b=vec(c,"to");float inflate=number(c,"inflate",0);float x=a[0]-inflate,y=a[1]-inflate,z=a[2]-inflate,X=b[0]+inflate,Y=b[1]+inflate,Z=b[2]+inflate;float[][][] quads={{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}},{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}},{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}},{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}}};String[] names={"north","south","west","east","up","down"};float tw=root.getAsJsonObject("resolution").get("width").getAsFloat(),th=root.getAsJsonObject("resolution").get("height").getAsFloat();JsonObject fs=c.getAsJsonObject("faces");for(int i=0;i<6;i++){if(!fs.has(names[i]))continue;JsonObject f=fs.getAsJsonObject(names[i]);if(!f.has("texture")||f.get("texture").isJsonNull())continue;Vector3f[] points=new Vector3f[4];for(int j=0;j<4;j++)points[j]=matrix.transformPosition(new Vector3f(quads[i][j][0],quads[i][j][1],quads[i][j][2])).div(16);Vector3f normal=new Vector3f(points[1]).sub(points[0]).cross(new Vector3f(points[2]).sub(points[0]));if(normal.lengthSquared()>0)normal.normalize();else normal.set(0,1,0);int textureIndex=f.get("texture").getAsInt();tw=textureSize(textureIndex,true);th=textureSize(textureIndex,false);JsonArray uv=f.getAsJsonArray("uv");float u=uv.get(0).getAsFloat()/tw,v=uv.get(1).getAsFloat()/th,U=uv.get(2).getAsFloat()/tw,V=uv.get(3).getAsFloat()/th;float[] coords={u,V,U,V,U,v,u,v};int rot=(int)number(f,"rotation",0)/90;ResourceLocation tex=new ResourceLocation("jujutsu_kaisen_cursed_paths","textures/entity/"+texture+"_"+f.get("texture").getAsInt()+".png");VertexConsumer out=buffers.getBuffer(alpha<1?RenderType.entityTranslucent(tex):RenderType.entityCutoutNoCull(tex));for(int j=0;j<4;j++){int k=(j+rot)%4;Vector3f p=points[j];out.vertex(ps.last().pose(),p.x,p.y,p.z).color(red,green,blue,alpha).uv(coords[k*2],coords[k*2+1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(ps.last().normal(),normal.x,normal.y,normal.z).endVertex();}}}
}
