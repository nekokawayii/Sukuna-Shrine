package character.todo;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 不义游戏：1 注入咒力（给任意方块/实体/弹幕挂标记，永久有效直到被消耗）、
 * 2 换位（无 CD，与准星最近的有咒力标记的东西换位，换位前后各 0.3 秒免疫伤害）。
 * <p>玩家与 NPC（{@code sukuna:todo_npc}）共用同一套标记与换位逻辑。
 */
public final class TodoCombat {
    public static final String DODGE_UNTIL = "TodoDodgeUntil";
    /** 换位规避：前后各 0.3 秒 = 共 12 刻。 */
    public static final int DODGE_TICKS = 12;
    /** NPC 的换位/注入间隔（刻）。 */
    public static final int NPC_INJECT_INTERVAL = 40, NPC_SWAP_INTERVAL = 20;

    public static boolean isTodo(Player p) {
        var d = character.CharacterService.selected(p);
        return d != null && "todo".equals(d.id());
    }

    /** 换位规避 + NPC 的自动闪避。 */
    @SubscribeEvent public static void hit(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (e.getAmount() <= 0) return;
        if (e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (TodoMarks.dodging(victim)) {
            e.setCanceled(true);
            return;
        }
        if (victim instanceof TodoNpcEntity npc && npc.tryDodge()) e.setCanceled(true);
    }

    /** 1 注入咒力。 */
    public static void inject(ServerPlayer p) {
        if (!TodoMarks.inject(p, TodoMarks.INJECT_RANGE)) {
            p.displayClientMessage(Component.literal("注入咒力：准星 24 格内没有可注入的东西（生物本来就自带咒力）"), true);
            p.sendSystemMessage(Component.literal("注入咒力：准星 24 格内没有可注入的东西").withStyle(ChatFormatting.GRAY));
            return;
        }
        p.displayClientMessage(Component.literal("注入咒力：已标记（永久有效，换位时消耗）"), true);
    }

    /** 2 换位：无 CD，与准星最近的有咒力的东西互换位置。 */
    public static void swap(ServerPlayer p) {
        if (!TodoMarks.swap(p)) {
            p.displayClientMessage(Component.literal("换位：准星方向 32 格内没有可以换位的东西"), true);
            return;
        }
        p.displayClientMessage(Component.literal("不义游戏！"), true);
    }

    /** 创造恢复符：清掉规避窗口与本人挂的所有标记。 */
    public static void testReset(ServerPlayer p) {
        CompoundTag data = Curse.data(p);
        data.remove(DODGE_UNTIL);
        TodoMarks.release(p.getUUID());
    }

    private TodoCombat() {}
}
