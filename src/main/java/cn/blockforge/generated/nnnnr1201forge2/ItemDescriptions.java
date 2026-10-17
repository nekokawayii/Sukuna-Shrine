package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

@Mod.EventBusSubscriber(modid="sukuna")
public final class ItemDescriptions {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        var id=ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());if(id==null||!id.getNamespace().equals("sukuna"))return;
        String text=switch(id.getPath()){
            case "sukuna_finger"->"不可合成；击败神庙内咒胎戴天获得。长按右键吞噬，最多20根。|每根固定增加："+GrowthConfig.FINGER.description(Network.tooltipFactor(BalanceRules.Category.PLAYER_HEALTH),Network.tooltipFactor(BalanceRules.Category.PLAYER_ATTACK),Network.tooltipFactor(BalanceRules.Category.DAMAGE))+"。|5根起普攻附带一次原伤害70%的解；Del单独切换附拳。|5/10/15/18/20根解锁招式强化。|15根解锁反转术式：轮盘左侧方格，松手直接自疗；永久生命成长越高，恢复量和咒力消耗越高。";
            case "buddha_body"->"击败即身佛掉落；吃满20根手指后长按右键融合，仅一次。|"+GrowthConfig.BODY.description(Network.tooltipFactor(BalanceRules.Category.PLAYER_HEALTH),Network.tooltipFactor(BalanceRules.Category.PLAYER_ATTACK),Network.tooltipFactor(BalanceRules.Category.DAMAGE))+"；解锁飞行、世界斩、虚空捌和空间斩。";
            case "residue"->"任何生物的有效击杀有0.1%概率掉落；包含其他模组生物。|九相图阶段后食用，每个增加3%伤害，34个达到102%并完成凝练。|凝练完成前击败虎杖残影不会掉落升级碎片，不能跨阶段。|Shift+右键自己的女仆可喂食；各女仆独立成长。";
            case "soul_shrine_fragment"->"凝练满100%后击败虎杖残影掉落。|自己吃下或Shift+右键喂给同阶段女仆：觉醒灵魂御厨子与灵魂剥离。|"+GrowthConfig.SOUL.description(Network.tooltipFactor(BalanceRules.Category.PLAYER_HEALTH),Network.tooltipFactor(BalanceRules.Category.PLAYER_ATTACK),Network.tooltipFactor(BalanceRules.Category.DAMAGE));
            case "void_phase_fetus"->"击败五条悟固定掉落1个，不因主人未觉醒而漏掉掉落。|需要先觉醒灵魂御厨子，再自己吃下或Shift+右键喂给同阶段女仆。|通关记录与食用觉醒分开，不能跨阶段或重复吸收。|"+GrowthConfig.VOID.description(Network.tooltipFactor(BalanceRules.Category.PLAYER_HEALTH),Network.tooltipFactor(BalanceRules.Category.PLAYER_ATTACK),Network.tooltipFactor(BalanceRules.Category.DAMAGE));
            case "legacy"->"击败净土守护者，对话结束后获得。|虚空御厨子阶段可自己吃下，或Shift+右键喂给女仆；只生效一次。|解锁守护者技能、术式反转外放与蓝色觉醒；"+GrowthConfig.GUARDIAN.description(Network.tooltipFactor(BalanceRules.Category.PLAYER_HEALTH),Network.tooltipFactor(BalanceRules.Category.PLAYER_ATTACK),Network.tooltipFactor(BalanceRules.Category.DAMAGE));
            case "curse_orb"->"寄生佛掉落的咒力结晶。右键消耗一个，恢复150咒力。";
            case "golden_relic"->"即身佛留下的金色舍利；首领战利品与收藏品。";
            case "severance_seal"->"右键打断16格内即身佛的咒力虹吸。|成功时消耗一张，使用冷却3秒。";
            case "buddha_incense"->"非和平难度右键召唤即身佛，前方8格需要空地。|96格内已有即身佛时无法再次召唤。";
            case "buddha_spawn_egg"->"测试用：对方块右键生成即身佛，当前模式默认"+BalanceRules.number(BalanceRules.strength(5000,BalanceRules.Category.BOSS))+"生命，拥有两阶段术式。";
            case "daitian_spawn_egg"->"测试用：生成咒胎戴天；当前模式"+BalanceRules.number(BalanceRules.strength(200,BalanceRules.Category.BOSS))+"生命，属性不随玩家成长改变。";
            case "void_key"->"击败虎杖残影后获得。右键进入基岩层虚空竞技场，再次右键返回。|击败五条悟后吃下虚空相咒胎，解锁虚空御厨子；虎杖残影通关后可前往Y=1000真人区域。|丢失后可用 /sukuna void key 补领。";
            case "gojo_spawn_egg"->"测试用：生成五条悟。|150000 生命、20 防御，无下限免疫除世界斩与领域展延外的全部伤害。|击败固定掉落虚空相咒胎与六眼。";
            case "gojo_fragment"->"六眼：击败五条悟固定掉落 1 枚，没有任何阶段要求。|玩家：直接右键（潜行右键也行）即为吃下，屏幕最右侧开启五条悟技能轮盘。|女仆：对准自家女仆潜行右键喂下，之后在女仆任务里选择「六眼」，她会自己放苍、赫、茈与无量空处。|与宿傩记忆、里梅记忆共存，互不覆盖，任务随时可切回。|无限制茈会伤到自己，慎用。";
            case "void_controller"->"右键打开控制面板，填写生命、频率和伤害，按回车保存。|填写整合包原值（开启模式严格 100% 生效）；关闭模式按分类比值生效（首领生命/护甲/受伤上限 ×"+BalanceRules.number(BalanceRules.percent(BalanceRules.factor(false,BalanceRules.Category.BOSS)))+"%），时间、数量和范围不变。|整合包模式开关：config/sukuna-growth.toml 的 balance.modpackMode；完整分类比值可用 /sukuna balance 查看。|首领设置全存档共用，立即覆盖已加载首领；生命修改同时补满。|末页可开启全部首领与所有生物互相敌对。|可选配方：工作台九格泥土；默认关闭。|在 config/sukuna-growth.toml 的 crafting 中将 controllerCrafting 改为 true，重启后生效；联机以服务端为准。";
            case "true_false_love"->"女仆饰品｜放入女仆饰品栏生效，放在背包或手中无效。|女仆需已吃下宿傩记忆、选择伏魔御厨子任务并站立，随后会与主人对战。|女仆降至1血时结束对战并恢复满血；主人死亡重生后也会摘下返还。|对战结束，饰品返回主人背包；背包已满则掉在主人身边。";
            case "eternal_love"->"右键生物：你的术式不再伤害或锁定它；Shift＋右键解除。|工作台：上中金锭；中排紫水晶碎片、钻石、紫水晶碎片；下中虞美人。";
            case "sukuna_memory"->"长按右键吞噬一次，解锁宿傩术式与成长任务。|N选择术式，R释放，J查看任务；纸与腐肉可合成补领。";
            case "mahoraga_ritual"->"调伏仪式：右键在脚下展开法阵，召出八握剑异戒神将·魔虚罗（无阶段要求）。|必须独自参加：出现「与你不互为敌对」的玩家/女仆/旁观怪就立即失败；正在和你互相攻击的怪不算。|魔虚罗不分敌我，召唤者也会被打；逃出 500 格它会消失，仪式失败。|核心机制：法轮每受击 6 次转 1 格，转满 4 格（24 次）后该类攻击完全无效。|它还会每 60 秒适应「你本身」一次（最多 6 次，每次伤害 +20%），满层后攻击变真伤且命中 20 秒禁疗。|仪式失败后被打死的魔虚罗不掉任何东西。";
            case "mahoraga_wheel"->"魔虚罗掉落物，也是一件**头盔**：普通右键直接戴到头上（潜行右键不会戴上），一圈法轮横着悬在头顶（不遮挡头部建模与皮肤）。|戴上后获得「自身适应」：打中同一个生物 12 次 → **完全适应它的存在**，之后你对它的伤害是**真伤**且**翻倍**，并让它 20 秒无法回血。|被同一个来源打也会累积：12 次时减伤到 **50%**，攒到 24 次**直接免疫**（魔虚罗与友方魔虚罗同规则）。|潜行右键（对着空气也行）：召唤友方魔虚罗；召唤后手里与背包的法轮全部变成「法轮 · 收回」，右键收回即变回法轮。|固定 800 生命、绝对友方、每次召唤满血、继承你已有的适应（不继承调伏仪式里的）。";
            case "mahoraga_wheel_recall"->"召唤友方魔虚罗后由法轮变成。|右键：把场上的友方魔虚罗收回，并**变回法轮**（可继续戴在头上）。|场上已经没有友方魔虚罗时右键，同样会变回法轮。";
            case "mahoraga_spawn_egg"->"测试用：直接生成魔虚罗。|没有调伏仪式的独自限制，但它依然不分敌我、并且照常按攻击类型适应。";
            case "nine_death_paintings"->"九种试炼咒胎各一个合成，完成即身佛阶段后吞噬一次。|"+GrowthConfig.NINE.description(Network.tooltipFactor(BalanceRules.Category.PLAYER_HEALTH),Network.tooltipFactor(BalanceRules.Category.PLAYER_ATTACK),Network.tooltipFactor(BalanceRules.Category.DAMAGE))+"。|斩击命中1秒后联合追击；V开启15秒九相轮转。";
            case "nether_core"->"祭坛材料；黑曜石放中央，上下左右各一份烈焰粉合成。";
            case "forbidden_fetus"->"即身佛掉落的禁忌胚胎，凝聚咒胎祭坛的核心。";
            case "cursed_altar"->"放置后右键查看当前试炼。|背包带齐九种咒胎时右键，消耗各一个凝聚九相图。";
            default->id.getPath().startsWith("page_")?"九相试炼直接奖励的咒胎；兼容已有存档。":"";
        };
        for(String line:text.split("\\|"))if(!line.isEmpty())event.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GRAY));
    }
}
