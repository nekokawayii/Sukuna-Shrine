package character.npc;

import character.CharacterRegistry;
import cn.blockforge.generated.nnnnr1201forge2.BloodRules;
import java.util.List;

/** all 动态读取玩家技能目录，后续增加技能无需再改 NPC 清单；only 是主动限制。 */
public record NpcLoadout(String role, boolean allSkills, List<Integer> selected) {
    public NpcLoadout {
        if (role == null || role.isBlank()) throw new IllegalArgumentException("角色编号不能为空");
        selected = List.copyOf(selected);
    }
    public static NpcLoadout all(String role) { return new NpcLoadout(role, true, List.of()); }
    public static NpcLoadout only(String role, Integer... skills) { return new NpcLoadout(role, false, List.of(skills)); }
    public List<Integer> skills() {
        if (!allSkills) return selected;
        if (role.equals("sukuna")) return List.of(0,1,2,3,4,5,6,17,18,19,20,21,24,25,26,27,28,29,30,31,32,33,43,44,200,201,202,203,204,205);
        if (role.equals("blood")) return java.util.stream.IntStream.rangeClosed(BloodRules.FIRST,BloodRules.LAST).boxed().toList();
        if (role.equals("toji")) return java.util.stream.IntStream.rangeClosed(1,20).boxed().toList();
        var definition = CharacterRegistry.get(role);
        return definition == null ? List.of() : definition.skills();
    }
}
