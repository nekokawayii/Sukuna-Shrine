package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 开局赠送《游玩方法手册》（r171）：涵盖全部角色、全流程、全术式、全物品，不含隐藏支线。
 * <p>本工程未引入 Patchouli 编译期依赖，所以发的是成书（written_book，页内容由代码生成）。
 */
public final class GuideBook {

    @SubscribeEvent public static void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var data = Curse.data(p);
        if (data.getBoolean("SukunaGuideGiven")) return;
        data.putBoolean("SukunaGuideGiven", true);
        p.getInventory().add(book());
        p.sendSystemMessage(Component.literal("已获得《游玩方法手册》：全部角色 / 全流程 / 全术式 / 全物品")
            .withStyle(ChatFormatting.GOLD));
    }

    /** 有帕秋莉就给帕秋莉手册，没有就给成书（帕秋莉是可选依赖，用注册表查物品，不需要编译期依赖）。 */
    public static ItemStack book() {
        var patchouli = net.minecraft.core.registries.BuiltInRegistries.ITEM
            .get(new net.minecraft.resources.ResourceLocation("patchouli", "guide_book"));
        if (patchouli != net.minecraft.world.item.Items.AIR
            && net.minecraftforge.fml.ModList.get().isLoaded("patchouli")) {
            ItemStack stack = new ItemStack(patchouli);
            stack.getOrCreateTag().putString("patchouli:book", "sukuna:guide");
            return stack;
        }
        ItemStack stack = new ItemStack(Content.MANUAL.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", "游玩方法手册");
        tag.putString("author", "宿傩 · 伏魔御厨子");
        ListTag pages = new ListTag();
        for (String page : PAGES) {
            // 1.20.1 的书页是「JSON 文本组件」列表，直接塞纯字符串会解析失败 → 书页空白
            pages.add(StringTag.valueOf("{\"text\":\"" + page.replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"}"));
        }
        tag.put("pages", pages);
        return stack;
    }

    private static final String[] PAGES = {
        "§l欢迎来到咒术回战\n§r本手册收录：\n· 全部可玩角色\n· 全流程推进顺序\n· 全术式与领域\n· 全物品与材料\n（不含隐藏支线）",
        "§l开始\n§r1. 拿到《咒术选择书》右键选择角色\n2. 每个角色有 3 章剧情，靠击杀或使用技能推进\n3. 按 N 选技能，按 R 释放\n4. 按 L 打开共同主线成就\n5. 按 J 查看个人线",
        "§l角色一览（一）\n§r两面宿傩：解 / 捌 / 开 / 灶 / 伏魔御厨子\n五条悟：六眼九式 / 无量空处\n里梅：冰凝咒法\n虎杖悠仁：咒力强化 + 赤血\n赤血操术：20 招\n伏黑甚尔：纯 QTE 连招",
        "§l角色一览（二）\n§r漏壶：灾厄火焰 / 盖棺铁围山\n真人：无为转变 / 自闭圆盾裹\n伏黑惠：十种影法术\n日车宽见：诛伏赐死 + 判决\n鹿紫云一：幻兽琥珀\n东堂葵：不义游戏\n七海建人：十划咒法",
        "§l角色一览（三）\n§r石流龙：咒力强化 / 打上花火\n禅院直哉：投射咒法 / 时胞月宫殿\n秤金次：坐杀博徒（老虎机）\n九十九由基：星之怒\n乙骨忧太：真赝相爱\n领域王：万域",
        "§l共同主线（L 键）\n§r反转术式 / 反转术式外放\n结界术 / 简易领域\n领域展开 / 领域展延（G 键）\n达成条件见 L 面板，解锁后全角色通用。",
        "§l领域对拼\n§r两个领域球体重叠 → 按空格比手速\n胜方领域延长 20 秒\n败方领域解除并进入熔断\n秤金次的坐杀博徒对拼必胜",
        "§l物品与材料（一）\n§r宿傩手指 / 佛体 / 九相图：吞噬成长\n咒力结晶：右键回 150 咒力\n解咒符：打断即身佛虹吸\n佛香：召唤即身佛\n虚空钥匙：进虚空竞技场（可合成）",
        "§l物品与材料（二）\n§r虚空相咒胎 / 传承：阶段突破\n六眼碎片：继承六眼\n魔虚罗法轮：头盔，戴上转动法轮\n天逆鉾 / 黑绳 / 屠坐魔 / 游云：咒具\n领域方块：可换图装饰",
        "§l首领\n§r咒胎戴天 → 即身佛 → 五条悟 → 魔虚罗\n虎杖残影（虚空竞技场）→ 净土守护者\n破碎真人 / 寄生佛 / 母体\n全部首领已解除前置限制，可直接挑战。",
        "§l提示\n§r破坏地形需开启：/sukuna gamerule desblock true\n出问题可 /sukuna reset 复位\n隐藏支线不在本手册范围，请自行探索",
    };

    private GuideBook() {}
}
