package cn.blockforge.generated.nnnnr1201forge2;

import java.util.HashSet;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/** 宿傩 NPC 原身形态的客户端标记：服务端用 Effect(640) 广播实体 id（r275）。 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class SukunaNpcTrueFormClient {
    private static final Set<Integer> IDS = new HashSet<>();

    public static void set(Network.Effect e) {
        if (e.size() > 0) IDS.add(e.owner());
        else IDS.remove(e.owner());
    }

    public static boolean has(int entityId) {
        return IDS.contains(entityId);
    }

    private SukunaNpcTrueFormClient() { }
}
