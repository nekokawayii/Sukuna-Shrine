package character.yuta;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 乙骨忧太的「复制术式」。
 * <p>掠夺方式：潜行 + 右键生物 → 里香短暂出现撕咬 → 里香消失 → 从该生物的池子里随机获得一条**未拥有**的术式。
 * <p>池子按目标判定：目标是玩家就看该玩家当前选择的角色；目标是已知 Boss 实体就按其类型。
 * <p>已复制的术式编号存在该角色的存档段（{@code YutaCopies}），切换角色再切回不会丢。
 */
@Mod.EventBusSubscriber(modid = "sukuna")
public final class YutaCopies {
    private static final String KEY = "YutaCopies";
    private static final String USED = "YutaUsedDuringRika";

    // 五条悟：只抢苍(34)/赫(35)/茈(36) 三选一
    private static final int[] GOJO = {34, 35, 36};
    // 里梅：霜凪(22)/直瀑(23)
    private static final int[] URAUME = {22, 23};
    // 咒胎九相图：除领域(74)与死胎星(75)外的全部
    private static final int[] BLOOD = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 76, 77, 78, 79};
    // 虎杖：咒力拳(7)/径庭拳(8)/连续拳(13)
    private static final int[] YUJI = {7, 8, 13};
    // 宿傩：解·远程斩击(0)/捌·必断斩击(2) + 网分支/形状分支(17,18)
    private static final int[] SUKUNA = {0, 2, 17, 18};

    public static Set<Integer> copies(Player p) {
        Set<Integer> set = new LinkedHashSet<>();
        for (int id : cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).getIntArray(KEY)) set.add(id);
        return set;
    }

    public static boolean has(Player p, int skill) { return copies(p).contains(skill); }

    private static void store(Player p, Set<Integer> set) {
        int[] arr = new int[set.size()];
        int i = 0;
        for (int id : set) arr[i++] = id;
        cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).putIntArray(KEY, arr);
    }

    public static void add(Player p, int skill) {
        Set<Integer> set = copies(p);
        if (set.add(skill)) store(p, set);
    }

    /** 里香召唤时重置「单次里香每招只能用一次」的记录。 */
    public static void resetUses(Player p) { cn.blockforge.generated.nnnnr1201forge2.Curse.data(p).remove(USED); }

    /** 尝试消耗一次使用额度：单次里香内同一招只能用一次。 */
    public static boolean consumeUse(Player p, int skill) {
        var data = cn.blockforge.generated.nnnnr1201forge2.Curse.data(p);
        var used = data.getCompound(USED);
        String k = Integer.toString(skill);
        if (used.getBoolean(k)) return false;
        used.putBoolean(k, true);
        data.put(USED, used);
        return true;
    }

    public static String describe(Player p) {
        Set<Integer> set = copies(p);
        if (set.isEmpty()) return "（还没有复制任何术式）";
        List<String> names = new ArrayList<>();
        for (int id : set) names.add(nameOf(id));
        return String.join("、", names);
    }

    /** 复制列表的原始编号数组，用于同步给客户端拼进轮盘。 */
    public static int[] array(Player p) {
        Set<Integer> set = copies(p);
        int[] arr = new int[set.size()];
        int i = 0;
        for (int id : set) arr[i++] = id;
        return arr;
    }

    public static String nameOf(int id) {
        // 五条的 34–42 不在 AuthoritySkills 的映射里（那边只认 100+），必须按 WHEEL 换算，
        // 否则会显示成「未刻写」。
        if (id >= 34 && id < 34 + cn.blockforge.generated.nnnnr1201forge2.GojoPlayer.SLOTS) {
            return cn.blockforge.generated.nnnnr1201forge2.GojoPlayer.nameOf(
                cn.blockforge.generated.nnnnr1201forge2.GojoPlayer.WHEEL[id - 34]);
        }
        return cn.blockforge.generated.nnnnr1201forge2.AuthoritySkills.name(id);
    }

    /** 目标对应的可复制池；空数组表示这个生物没有可掠夺的术式。 */
    public static int[] poolFor(Entity target) {
        if (target instanceof Player p) {
            var d = character.CharacterService.selected(p);
            if (d == null) return new int[0];
            return switch (d.id()) {
                case "gojo" -> GOJO;
                case "uraume" -> URAUME;
                case "blood" -> BLOOD;
                case "yuji" -> YUJI;
                case "sukuna" -> SUKUNA;
                default -> new int[0];
            };
        }
        if (target instanceof cn.blockforge.generated.nnnnr1201forge2.GojoEntity) return GOJO;
        if (target instanceof cn.blockforge.generated.nnnnr1201forge2.YujiEntity) return YUJI;
        if (target instanceof cn.blockforge.generated.nnnnr1201forge2.GuardianEntity) return YUJI;
        return new int[0];
    }

    /**
     * 潜行 + 右键生物：夺取一条术式。
     * <p>只有乙骨本人、且该生物有可掠夺的池子时才生效；成功与否都会取消这次交互，
     * 避免同时触发原版的交易/骑乘等行为。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown()) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        var d = character.CharacterService.selected(p);
        if (d == null || !"yuta".equals(d.id())) return;

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

        int[] pool = poolFor(target);
        if (pool.length == 0) {
            p.displayClientMessage(Component.literal("这个生物没有可以复制的术式"), true);
            return;
        }
        Set<Integer> owned = copies(p);
        List<Integer> candidates = new ArrayList<>();
        for (int id : pool) if (!owned.contains(id)) candidates.add(id);
        if (candidates.isEmpty()) {
            p.displayClientMessage(Component.literal("这一家术式已经全部复制完了"), true);
            return;
        }
        int picked = candidates.get(p.getRandom().nextInt(candidates.size()));

        // 里香短暂出现、撕咬、消失——与是否处于召唤状态无关。
        cn.blockforge.generated.nnnnr1201forge2.RikaRules.flashBite(p, target);
        add(p, picked);
        p.displayClientMessage(Component.literal("复制成功：" + nameOf(picked)), true);
    }

    private YutaCopies() {}
}
