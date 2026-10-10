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
            case "sukuna_finger"->"不可合成；击败神庙内咒胎戴天获得。长按右键吞噬，最多20根。|每根固定增加："+GrowthConfig.FINGER.description(Network.tooltipFactor())+"。|5根起普攻附带一次原伤害70%的解；Del单独切换附拳。|5/10/15/18/20根解锁招式强化。|15根解锁反转术式：轮盘左侧方格，松手直接自疗；永久生命成长越高，恢复量和咒力消耗越高。";
            case "buddha_body"->"击败即身佛掉落；吃满20根手指后长按右键融合，仅一次。|"+GrowthConfig.BODY.description(Network.tooltipFactor())+"；解锁飞行、世界斩、虚空捌和空间斩。";
            case "residue"->"任何生物的有效击杀有0.1%概率掉落；包含其他模组生物。|九相图阶段后食用，每个增加3%伤害，34个达到102%并完成凝练。|凝练完成前击败虎杖残影不会掉落升级碎片，不能跨阶段。|Shift+右键自己的女仆可喂食；各女仆独立成长。";
            case "soul_shrine_fragment"->"凝练满100%后击败虎杖残影掉落。|自己吃下或Shift+右键喂给同阶段女仆：觉醒灵魂御厨子与灵魂剥离。|"+GrowthConfig.SOUL.description(Network.tooltipFactor());
            case "void_phase_fetus"->"击败虚无相固定掉落1个，不因主人未觉醒而漏掉掉落。|需要先觉醒灵魂御厨子，再自己吃下或Shift+右键喂给同阶段女仆。|通关记录与食用觉醒分开，不能跨阶段或重复吸收。|"+GrowthConfig.VOID.description(Network.tooltipFactor());
            case "legacy"->"击败净土守护者，对话结束后获得。|虚空御厨子阶段可自己吃下，或Shift+右键喂给女仆；只生效一次。|解锁守护者技能、术式反转外放与蓝色觉醒；"+GrowthConfig.GUARDIAN.description(Network.tooltipFactor());
            case "curse_orb"->"寄生佛掉落的咒力结晶。右键消耗一个，恢复150咒力。";
            case "golden_relic"->"即身佛留下的金色舍利；首领战利品与收藏品。";
            case "severance_seal"->"右键打断16格内即身佛的咒力虹吸。|成功时消耗一张，使用冷却3秒。";
            case "buddha_incense"->"非和平难度右键召唤即身佛，前方8格需要空地。|96格内已有即身佛时无法再次召唤。";
            case "buddha_spawn_egg"->"测试用：对方块右键生成即身佛，当前模式默认"+BalanceRules.number(5000*Network.tooltipFactor())+"生命，拥有两阶段术式。";
            case "daitian_spawn_egg"->"测试用：生成咒胎戴天；当前模式"+BalanceRules.number(200*Network.tooltipFactor())+"生命，属性不随玩家成长改变。";
            case "void_key"->"击败虎杖残影后获得。右键进入基岩层虚空竞技场，再次右键返回。|击败虚无相后吃下虚空相咒胎，解锁虚空御厨子；虎杖残影通关后可前往Y=1000真人区域。|丢失后可用 /sukuna void key 补领。";
            case "void_spawn_egg"->"测试用：生成构筑咒胎受肉：虚无相。|击败固定掉落虚空相咒胎；需先觉醒灵魂御厨子，再食用解锁虚空御厨子。";
            case "void_controller"->"右键打开控制面板，填写生命、频率和伤害，按回车保存。|填写整合包原值；普通模式下强度按10%生效，时间、数量和范围不变。|整合包模式开关：config/sukuna-growth.toml 的 balance.modpackMode。|首领设置全存档共用，立即覆盖已加载首领；生命修改同时补满。|末页可开启全部首领与所有生物互相敌对。|可选配方：工作台九格泥土；默认关闭。|在 config/sukuna-growth.toml 的 crafting 中将 controllerCrafting 改为 true，重启后生效；联机以服务端为准。";
            case "true_false_love"->"女仆饰品｜放入女仆饰品栏生效，放在背包或手中无效。|女仆需已吃下宿傩记忆、选择伏魔御厨子任务并站立，随后会与主人对战。|女仆降至1血时结束对战并恢复满血；主人死亡重生后也会摘下返还。|对战结束，饰品返回主人背包；背包已满则掉在主人身边。";
            case "eternal_love"->"右键生物：你的术式不再伤害或锁定它；Shift＋右键解除。|工作台：上中金锭；中排紫水晶碎片、钻石、紫水晶碎片；下中虞美人。";
            case "sukuna_memory"->"长按右键吞噬一次，解锁宿傩术式与成长任务。|N选择术式，R释放，J查看任务；纸与腐肉可合成补领。";
            case "nine_death_paintings"->"九种试炼咒胎各一个合成，完成即身佛阶段后吞噬一次。|"+GrowthConfig.NINE.description(Network.tooltipFactor())+"。|斩击命中1秒后联合追击；V开启15秒九相轮转。";
            case "nether_core"->"祭坛材料；黑曜石放中央，上下左右各一份烈焰粉合成。";
            case "forbidden_fetus"->"即身佛掉落的禁忌胚胎，凝聚咒胎祭坛的核心。";
            case "cursed_altar"->"放置后右键查看当前试炼。|背包带齐九种咒胎时右键，消耗各一个凝聚九相图。";
            default->id.getPath().startsWith("page_")?"九相试炼直接奖励的咒胎；兼容已有存档。":"";
        };
        for(String line:text.split("\\|"))if(!line.isEmpty())event.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GRAY));
    }
}
