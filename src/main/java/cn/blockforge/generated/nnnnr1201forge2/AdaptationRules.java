package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;

/** 两种显现只合并进度，不相加，不倒退。 */
public final class AdaptationRules {
    public static Map<String,Integer> merge(Map<String,Integer> saved,Map<String,Integer> incoming,int cap){
        Map<String,Integer> result=new LinkedHashMap<>();
        for(Map<String,Integer> source:List.of(saved,incoming))for(var entry:source.entrySet()){
            String key=entry.getKey();if(key==null||key.isEmpty())continue;
            if(!key.startsWith("taken/")&&!key.startsWith("dealt/"))key="taken/"+key;
            int value=Math.max(0,Math.min(cap,entry.getValue()));if(value>0)result.merge(key,value,Math::max);
        }
        return result;
    }
    public static float takenFactor(int hits){if(hits<=0)return 1;if(hits>=24)return 0;return 1-hits/24f;}
    private AdaptationRules(){}
}
