package character.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** 无游戏依赖的重叠地形规则：原状只保存一次，关闭任意层都不会把已关闭的领域复活。 */
public final class DomainTerrainLayers<K,V> {
    private final V original;
    private final Map<K,V> layers=new LinkedHashMap<>();
    public DomainTerrainLayers(V original){this.original=original;}
    public V original(){return original;}
    public void put(K owner,V value){layers.put(owner,value);}
    public void remove(K owner){layers.remove(owner);}
    public boolean empty(){return layers.isEmpty();}
    public V visible(Predicate<K> allowed){
        V result=original;
        for(var e:layers.entrySet())if(allowed.test(e.getKey()))result=e.getValue();
        return result;
    }
}
