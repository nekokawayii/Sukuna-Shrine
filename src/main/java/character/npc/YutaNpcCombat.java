package character.npc;

import cn.blockforge.generated.nnnnr1201forge2.*;
import character.yuta.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Mob;

/**
 * 乙骨 NPC 的「复制」行为（r284，按用户口径重写）：
 *
 * <ul>
 *   <li>从**全部角色的全部术式**里随机挑一个当「主要术式」（{@code YutaMain}）；</li>
 *   <li>主要术式可以**无限次**释放；</li>
 *   <li>其它术式每招只能放**一次**（记在 {@code YutaUsed} 里），放完就不再出现。</li>
 * </ul>
 */
public final class YutaNpcCombat {
    private static List<String[]> pool;

    public static void copy(NpcBridge.NpcActor actor) {
        if (!(actor.mob instanceof Mob mob) || !RikaRules.active(actor)) return;
        List<String[]> all = pool();
        if (all.isEmpty()) return;
        var data = mob.getPersistentData();

        String main = data.getString("YutaMain");
        if (main.isEmpty()) {
            String[] pick = all.get(mob.getRandom().nextInt(all.size()));
            main = pick[0] + ":" + pick[1];
            data.putString("YutaMain", main);
        }

        // 随机尝试几次：主要术式随便放，其余术式每招只给一次机会
        for (int attempt = 0; attempt < 4; attempt++) {
            String entry;
            if (mob.getRandom().nextInt(3) == 0) {
                entry = main;
            } else {
                String[] pick = all.get(mob.getRandom().nextInt(all.size()));
                entry = pick[0] + ":" + pick[1];
            }
            if (entry.equals(main)) {
                if (cast(actor, mob, main)) return;
                continue;
            }
            ListTag used = data.getList("YutaUsed", Tag.TAG_STRING);
            boolean alreadyUsed = false;
            for (int i = 0; i < used.size(); i++) {
                if (used.getString(i).equals(entry)) { alreadyUsed = true; break; }
            }
            if (alreadyUsed) continue;
            if (cast(actor, mob, entry)) {
                used.add(StringTag.valueOf(entry));
                data.put("YutaUsed", used);
                return;
            }
        }
    }

    private static boolean cast(NpcBridge.NpcActor actor, Mob mob, String entry) {
        int cut = entry.indexOf(':');
        if (cut <= 0) return false;
        String characterId = entry.substring(0, cut);
        int skillId;
        try {
            skillId = Integer.parseInt(entry.substring(cut + 1));
        } catch (NumberFormatException error) {
            return false;
        }
        var definition = character.CharacterRegistry.get(characterId);
        if (definition == null) return false;
        var skill = definition.config().skill(skillId);
        if (skill == null) return false;
        String cooldownKey = "YutaCopyCooldown_" + skillId;
        if (Curse.active(actor, cooldownKey)) return false;
        if (!Curse.spend(actor, skill.cost())) return false;
        CopyBorrow.begin(actor);
        try {
            boolean ok = character.CharacterCasting.castAs(actor, characterId, skillId,
                character.CharacterService.rank(actor));
            if (ok) Curse.status(actor, cooldownKey, 40);
            return ok;
        } finally {
            CopyBorrow.end(actor);
        }
    }

    /** 全部角色（跳过乙骨自己）的自定义术式池，缓存一次。 */
    private static List<String[]> pool() {
        if (pool != null) return pool;
        List<String[]> out = new ArrayList<>();
        for (var definition : character.CharacterRegistry.all()) {
            if ("yuta".equals(definition.id())) continue;
            for (var skill : definition.config().customSkills()) {
                out.add(new String[]{definition.id(), Integer.toString(skill.id())});
            }
        }
        pool = out;
        return pool;
    }

    private YutaNpcCombat() { }
}
