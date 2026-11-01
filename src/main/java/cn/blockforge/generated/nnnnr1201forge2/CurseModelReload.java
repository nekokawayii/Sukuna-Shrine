package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;

/** 在资源加载工作线程预解码原始模型，不在第一只咒灵出现的渲染帧解析数MB JSON。 */
public final class CurseModelReload extends SimplePreparableReloadListener<Map<String,BbModel>> {
    public static void register(RegisterClientReloadListenersEvent event){event.registerReloadListener(new CurseModelReload());}
    @Override protected Map<String,BbModel> prepare(ResourceManager resources,ProfilerFiller profiler){
        Map<String,BbModel> models=new HashMap<>();
        for(var form:CurseCatalog.FORMS)models.put(form.id(),BbModel.prepare(resources,form.id()));
        return Map.copyOf(models);
    }
    @Override protected void apply(Map<String,BbModel> models,ResourceManager resources,ProfilerFiller profiler){LowCurseRenderer.install(models);}
}
