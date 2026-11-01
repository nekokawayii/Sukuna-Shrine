package character.domain;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 数据包更换现有领域天空，无需再改渲染器；图片资源由客户端资源包提供。 */
public final class DomainStyleReload extends SimpleJsonResourceReloadListener {
    public DomainStyleReload(){super(new Gson(),"domain_styles");}
    @SubscribeEvent public static void add(AddReloadListenerEvent event){event.addListener(new DomainStyleReload());}
    @Override protected void apply(Map<ResourceLocation,JsonElement> values,ResourceManager resources,ProfilerFiller profiler){
        Map<String,DomainPresentation.Style> styles=new HashMap<>();
        for(var entry:values.entrySet()){
            try{
                var json=entry.getValue().getAsJsonObject();
                String role=json.has("role")?json.get("role").getAsString():entry.getKey().getPath();
                String texture=json.has("texture")?json.get("texture").getAsString():"";
                int color=json.has("color")?(int)Long.parseLong(json.get("color").getAsString().replace("#",""),16):0x77BBFF;
                float yaw=json.has("yaw")?json.get("yaw").getAsFloat():0;
                float spin=json.has("spin")?json.get("spin").getAsFloat():0;
                boolean camera=!json.has("camera_sky")||json.get("camera_sky").getAsBoolean();
                styles.put(role,new DomainPresentation.Style(texture,color,yaw,spin,camera));
            }catch(RuntimeException ex){com.mojang.logging.LogUtils.getLogger().warn("领域外观配置 {} 无效，保留默认外观",entry.getKey(),ex);}
        }
        DomainPresentation.reload(styles);
    }
}
