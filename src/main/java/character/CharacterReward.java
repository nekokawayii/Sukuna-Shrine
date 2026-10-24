package character;

import java.util.List;
import character.CharacterConfig.Stats;

/** 一章的真实奖励：永久属性按已完成章计算，一次性物品/经验/咒力用章节编号去重。 */
public record CharacterReward(Stats stats, int experience, double energy, List<Item> items) {
    public static final CharacterReward NONE = new CharacterReward(Stats.ZERO,0,0,List.of());
    public record Item(String id, int count) {
        public Item {
            if(id==null||!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||count<1||count>4096)
                throw new IllegalArgumentException("奖励物品需完整编号且数量为1至4096");
        }
    }
    public CharacterReward {
        if(stats==null||experience<0||experience>1000000||!Double.isFinite(energy)||energy<0||energy>1000000)
            throw new IllegalArgumentException("奖励经验与咒力需为0至1000000");
        items=List.copyOf(items);if(items.size()>16)throw new IllegalArgumentException("每章最多16种物品奖励");
    }
    public String summary() {
        var parts=new java.util.ArrayList<String>();
        if(stats.energy()>0)parts.add("咒力上限+"+number(stats.energy()));
        if(stats.health()>0)parts.add("生命上限+"+number(stats.health()));
        if(stats.attack()>0)parts.add("近战+"+number(stats.attack()));
        if(stats.spell()>0)parts.add("术式加成+"+number(stats.spell()));
        if(stats.recovery()>0)parts.add("每秒回咒+"+number(stats.recovery()));
        if(experience>0)parts.add("经验点+"+experience);
        if(energy>0)parts.add("补充咒力"+number(energy));
        for(Item item:items)parts.add(item.id()+"×"+item.count());
        return String.join("，",parts);
    }
    private static String number(double n) { return n==(long)n?Long.toString((long)n):Double.toString(n); }
}
