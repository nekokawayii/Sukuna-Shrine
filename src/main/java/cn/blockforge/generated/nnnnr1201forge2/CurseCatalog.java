package cn.blockforge.generated.nnnnr1201forge2;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.util.RandomSource;

/** 同一份原始模型尺寸目录供客户端缩放、服务端判定等级和碰撞尺寸使用。 */
public final class CurseCatalog {
    public record Form(String id,String name,float originalHeight,int grade,float height,float width,float centerX,float floor,float centerZ,String idle,String walk,String attack) {
        public String gradeName(){return grade==0?"特级":grade+"级";}
        public double health(){return switch(grade){case 4->32;case 3->60;case 2->110;case 1->220;default->450;};}
        public double damage(){return switch(grade){case 4->4;case 3->6;case 2->9;case 1->13;default->20;};}
        public int weight(){return switch(grade){case 4->40;case 3->22;case 2->10;case 1->3;default->1;};}
    }
    public static final List<Form> FORMS=load();
    private static String animation(JsonArray animations,String... hints){
        for(String hint:hints)for(JsonElement e:animations){String name=e.getAsString();if(name.equals(hint)||name.contains(hint))return name;}
        return "";
    }
    private static List<Form> load(){
        try(var in=CurseCatalog.class.getResourceAsStream("/data/jujutsu_kaisen_cursed_paths/curse_catalog.json")){
            if(in==null)throw new IllegalStateException("缺少咒灵目录");
            var list=new ArrayList<Form>();var rows=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray();
            for(var row:rows){var o=row.getAsJsonObject();var b=o.getAsJsonArray("bounds");float loX=b.get(0).getAsJsonArray().get(0).getAsFloat(),hiX=b.get(0).getAsJsonArray().get(1).getAsFloat(),loZ=b.get(2).getAsJsonArray().get(0).getAsFloat(),hiZ=b.get(2).getAsJsonArray().get(1).getAsFloat();
                float raw=o.get("original_height").getAsFloat(),height=o.get("height").getAsFloat();var animations=o.getAsJsonArray("animations");
                // MC碰撞体只有正方形底面：宽度跟身体同比缩放，上限3格，长尾/手臂仍保留原造型。
                float width=Math.max(.65f,Math.min(3f,Math.min(hiX-loX,hiZ-loZ)*height/raw));
                list.add(new Form(o.get("id").getAsString(),o.get("name").getAsString(),raw,o.get("grade").getAsInt(),height,width,(loX+hiX)/2,b.get(1).getAsJsonArray().get(0).getAsFloat(),(loZ+hiZ)/2,
                    animation(animations,"idle","待机","combat_ready","floating","float","struggle","悬浮"),animation(animations,"walk","走路","行走","追击","move_drag","spectral_drift","漂浮","游龙"),animation(animations,"attack","攻击","lunge_punch","melee","左勾拳","施法")));
            }return List.copyOf(list);
        }catch(Exception e){throw new IllegalStateException("无法载入咒灵等级目录",e);}
    }
    public static Form form(int index){return FORMS.get(Math.floorMod(index,FORMS.size()));}
    public static int index(String id){for(int i=0;i<FORMS.size();i++)if(FORMS.get(i).id().equals(id))return i;return 0;}
    public static int choose(RandomSource random,boolean lowOnly){
        int total=0;for(Form f:FORMS)if(!lowOnly||f.grade()>=3)total+=f.weight();int roll=random.nextInt(total);
        for(int i=0;i<FORMS.size();i++){Form f=FORMS.get(i);if(lowOnly&&f.grade()<3)continue;roll-=f.weight();if(roll<0)return i;}return 0;
    }
    private CurseCatalog(){}
}
