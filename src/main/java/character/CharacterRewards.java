package character;

import cn.blockforge.generated.nnnnr1201forge2.Curse;
import cn.blockforge.generated.nnnnr1201forge2.Journey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 每角色每章节只发一次；标记随角色存档，切换、重登、死亡不会再次领取。 */
public final class CharacterRewards {
    public static void grant(ServerPlayer p) {
        var d=CharacterService.selected(p);
        if(d==null||d.family()==CharacterDefinition.Family.SHRINE||!p.isAlive())return;
        var data=Curse.data(p);var paid=data.getCompound("CharacterPaidChapters");
        int rank=CharacterService.rank(p);
        for(int i=0;i<rank;i++){
            var chapter=d.story().get(i);if(paid.getBoolean(chapter.id()))continue;
            var reward=chapter.grants();
            // 整章先验证物品，拼错编号时不吞奖励，也不先发一半导致重登重复领取。
            boolean valid=true;
            for(var item:reward.items()){
                var key=new ResourceLocation(item.id());
                if(!ForgeRegistries.ITEMS.containsKey(key)||ForgeRegistries.ITEMS.getValue(key)==Items.AIR){valid=false;break;}
            }
            if(!valid){
                if(!data.getBoolean("RewardError_"+chapter.id())){
                    p.sendSystemMessage(Component.literal("章节奖励暂未发放："+chapter.title()+"含不存在的物品编号，请修正角色配置；奖励记录已保留。"));
                    data.putBoolean("RewardError_"+chapter.id(),true);
                }
                continue;
            }
            paid.putBoolean(chapter.id(),true);data.put("CharacterPaidChapters",paid);
            data.remove("RewardError_"+chapter.id());
            p.giveExperiencePoints(reward.experience());Curse.add(p,reward.energy());
            for(var item:reward.items()){
                var type=ForgeRegistries.ITEMS.getValue(new ResourceLocation(item.id()));int left=item.count();
                while(left>0){var stack=new ItemStack(type,Math.min(left,type.getMaxStackSize()));left-=stack.getCount();Journey.give(p,stack);}
            }
            if(!reward.equals(CharacterReward.NONE))p.sendSystemMessage(Component.literal("已领取「"+chapter.title()+"」奖励："+d.rewards(i)));
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&p.tickCount%20==0)grant(p);
    }
    private CharacterRewards() {}
}
