package cn.blockforge.generated.nnnnr1201forge2;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NPC 血量的整合包倍率：**整合包模式开启时所有 NPC 血量 ×30，关闭时保持原值**。
 *
 * <p>属性表里的写法（`BalanceRules.modpackMode() ? base*30 : base`）只在**属性注册那一刻**求值一次，
 * 如果启动时配置还没读好、或者中途切了模式，数值就会停在旧的状态。这里再在实体进入世界时兜一层：
 * 按当前的 {@link BalanceRules#modpackMode()} 重新核对一次基础血量，不一致就改过来并回满。
 *
 * <p>覆盖的是"新的"那几个 NPC：胀相 / 里梅 / 漏壶 / 东堂葵 / 石流龙。
 */
@Mod.EventBusSubscriber(modid = "sukuna")
public final class NpcBalance {
    /** NPC → 关闭模式下的基础血量（开启时 ×30）。 */
    private static final Map<String, Double> BASE = new LinkedHashMap<>();
    static {
        BASE.put("sukuna:choso", 550.0);
        BASE.put("sukuna:uraume_npc", 500.0);
        BASE.put("sukuna:jogo_npc", 800.0);
        BASE.put("sukuna:todo_npc", 600.0);
        BASE.put("sukuna:ryu_npc", 700.0);
        BASE.put("sukuna:hakari_npc", 900.0);
        BASE.put("sukuna:naoya_npc", 800.0);
        BASE.put("sukuna:higuruma_npc", 900.0);
        BASE.put("sukuna:sukuna_npc", 2000.0);
    }
    public static final double MODPACK_MULTIPLIER = 30.0;

    public static double health(String entityId) {
        Double base = BASE.get(entityId);
        if (base == null) return 1;
        return BalanceRules.modpackMode() ? base * MODPACK_MULTIPLIER : base;
    }

    @SubscribeEvent public static void join(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof LivingEntity living)) return;
        String id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(living.getType()).toString();
        Double base = BASE.get(id);
        if (base == null) return;
        var attribute = living.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        double want = BalanceRules.modpackMode() ? base * MODPACK_MULTIPLIER : base;
        if (Math.abs(attribute.getBaseValue() - want) > 0.5) {
            attribute.setBaseValue(want);
            living.setHealth(living.getMaxHealth());
            com.mojang.logging.LogUtils.getLogger().info("[NPC血量] {} 按整合包模式({}) 设为 {}",
                id, BalanceRules.modpackMode() ? "开启×30" : "关闭", want);
        }
    }

    private NpcBalance() {}
}
