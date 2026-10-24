package cn.blockforge.generated.nnnnr1201forge2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** 与游戏无关的渲染表修复规则，供客户端与无窗口回归测试共同使用。 */
public final class RendererRepair {
    public record Factory<V>(Class<?> rendererClass, Supplier<? extends V> create) {
        public Factory {
            Objects.requireNonNull(rendererClass);
            Objects.requireNonNull(create);
        }
    }
    public record Result<K,V>(Map<K,V> renderers, List<K> repaired) {}

    /**
     * 每次检查内容，不把“同一张表”误当成“表内没有变”。
     * 仅修复本模组明确登记的类型；全部创建成功后才返回新表，不能半途修改原表。
     */
    public static <K,V> Result<K,V> repair(Map<K,V> live, Map<K,Factory<V>> required) {
        Objects.requireNonNull(live);
        if(required.isEmpty())throw new IllegalStateException("实体渲染器目录为空，不能判定检查成功");
        Map<K,V> copy=null;
        List<K> repaired=new ArrayList<>();
        for(var entry:required.entrySet()) {
            K type=entry.getKey();
            Factory<V> factory=entry.getValue();
            if(factory.rendererClass().isInstance(live.get(type)))continue;
            V renderer;
            try {
                renderer=factory.create().get();
            } catch(RuntimeException | LinkageError failure) {
                throw new IllegalStateException("无法创建实体渲染器："+type,failure);
            }
            if(!factory.rendererClass().isInstance(renderer))
                throw new IllegalStateException("实体渲染器返回空值或错误类型："+type);
            if(copy==null)copy=new LinkedHashMap<>(live);
            copy.put(type,renderer);
            repaired.add(type);
        }
        return new Result<>(copy==null?live:copy,List.copyOf(repaired));
    }
    private RendererRepair() {}
}
