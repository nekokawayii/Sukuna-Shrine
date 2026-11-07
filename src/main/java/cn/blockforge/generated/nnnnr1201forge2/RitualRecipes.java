package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

/** 消耗先校验再扣除，输出只在仪式完成时产生。后续祭坛配方只需登记一行。 */
public final class RitualRecipes {
    public record Recipe(String id,int tier,int ticks,String output,int count,boolean blueprint,Map<String,Integer> input){
        public ItemStack result(){return new ItemStack(resolve(output),count);}
        public boolean available(ServerPlayer p){return (!blueprint||ExpeditionProgress.data(p).getBoolean("ExpeditionSpecialBlueprint"))&&input.entrySet().stream().allMatch(e->p.getInventory().countItem(resolve(e.getKey()))>=e.getValue());}
        public boolean availableAt(ServerPlayer p,net.minecraft.core.BlockPos pos){
            if(blueprint&&!ExpeditionProgress.data(p).getBoolean("ExpeditionSpecialBlueprint"))return false;
            var dropped=offerings(p,pos);
            return input.entrySet().stream().allMatch(e->p.getInventory().countItem(resolve(e.getKey()))+dropped.stream().filter(it->it.getItem().is(resolve(e.getKey()))).mapToInt(it->it.getItem().getCount()).sum()>=e.getValue());
        }
        public void consumeAt(ServerPlayer p,net.minecraft.core.BlockPos pos){
            var dropped=offerings(p,pos);
            input.forEach((id,total)->{var item=resolve(id);int n=total;for(var entity:dropped){if(entity.getItem().is(item)){int used=Math.min(n,entity.getItem().getCount());entity.getItem().shrink(used);n-=used;if(entity.getItem().isEmpty())entity.discard();if(n==0)break;}}if(n>0)take(p,item,n);});
        }
        private java.util.List<net.minecraft.world.entity.item.ItemEntity> offerings(ServerPlayer p,net.minecraft.core.BlockPos pos){
            if(tier!=1)return java.util.List.of();
            return p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1.5,1,1.5),it->it.isAlive()&&(it.getOwner()==null||it.getOwner()==p));
        }
        public void consume(ServerPlayer p){input.forEach((id,n)->take(p,resolve(id),n));}
        public String description(){StringBuilder s=new StringBuilder(result().getHoverName().getString()).append(" ← ");input.forEach((id,n)->s.append(new ItemStack(resolve(id)).getHoverName().getString()).append('×').append(n).append(' '));return s.toString();}
    }
    public static final List<Recipe> ALL=new ArrayList<>();
    static {
        add("white_seal",0,200,"seal_white",4,false,"minecraft:paper",4,"minecraft:ink_sac",1,"residue_low",2);
        add("alloy",0,300,"jujutsu_alloy",2,false,"raw_jujutsu_ore",2,"minecraft:iron_ingot",2,"minecraft:gold_ingot",1,"residue_low",3);
        add("purify",0,240,"residue_high",1,false,"residue_low",9);
        add("disc",0,240,"simple_domain_disc",1,false,"jujutsu_alloy",2,"seal_gold",1,"curse_crystal",2,"residue_medium",2);
        add("doll",0,300,"substitute_doll",1,false,"minecraft:oak_log",3,"soul_echo",2,"deformed_bone",4,"residue_medium",2);
        add("injection",0,200,"reverse_injection",2,false,"curse_concentrate",1,"blood_wheat",2,"residue_low",2);
        add("blood_blade",1,72000,"blood_blade",1,false,"minecraft:iron_sword",1,"mutant_gland",4,"curse_flesh",4,"residue_high",3);
        add("blood_armor",1,72000,"blood_armor",1,false,"minecraft:iron_chestplate",1,"mutant_gland",6,"curse_flesh",8,"residue_high",3);
        add("poison",1,72000,"poison_coating",4,false,"mutant_gland",4,"curse_flesh",2,"residue_high",2);
        add("incarnation",1,72000,"incarnation_reagent",1,false,"soul_molt",1,"kenjaku_thread",2,"curse_flesh",4,"residue_high",3);
        add("tenka",2,240,"jujutsu_kaisen_cursed_paths:tenka_spear",1,true,"heavenly_bone",1,"tenka_fragment",3,"star_curse_core",1,"jujutsu_alloy",8,"residue_high",9);
        add("soul_blade",2,240,"soul_splitter",1,true,"heavenly_bone",1,"soul_echo",8,"star_curse_core",1,"jujutsu_alloy",8,"residue_high",9);
        add("playful_cloud",2,240,"jujutsu_kaisen_cursed_paths:yuyun",1,true,"heavenly_bone",1,"playful_cloud_parts",3,"star_curse_core",1,"jujutsu_alloy",8,"residue_high",9);
        add("ultimate",2,240,"ultimate_scroll",1,false,"star_curse_core",1,"volcanic_core",1,"seal_gold",3,"residue_high",9);
        // r249：狱门僵与点数改为祭坛专属（工作台配方已删除，材料同步加价）
        add("prison_realm",1,2400,"jujutsu_kaisen_cursed_paths:prison_realm",1,false,
            "heavenly_bone",2,"tenka_fragment",6,"star_curse_core",3,"prison_fragment",8,"residue_high",9);
        add("culling_points",0,1200,"jujutsu_kaisen_cursed_paths:culling_points",1,false,
            "tenka_fragment",8,"star_curse_core",4,"golden_relic",2,"curse_concentrate",4,"residue_high",6);
        // 同一批"特殊道具"全部改成祭坛专属（工作台配方已删）
        add("kenjaku_invitation",2,300,"jujutsu_kaisen_cursed_paths:kenjaku_invitation",1,false,
            "residue_high",4,"ancient_talisman",2,"prison_fragment",2);
        add("void_key",1,2400,"jujutsu_kaisen_cursed_paths:void_key",1,false,
            "tenka_fragment",4,"star_curse_core",3,"curse_crystal",6,"golden_relic",2,"residue_high",6);
        add("eternal_love",0,1200,"jujutsu_kaisen_cursed_paths:eternal_love",1,false,
            "star_curse_core",2,"curse_crystal",4,"golden_relic",1,"residue_high",3);
        add("culling_key",0,1200,"jujutsu_kaisen_cursed_paths:culling_key",1,false,
            "seal_gold",2,"star_iron_fragment",4,"residue_high",4);
        add("curse_summon_talisman",0,800,"jujutsu_kaisen_cursed_paths:curse_summon_talisman",1,false,
            "seal_white",2,"curse_concentrate",2,"residue_medium",4);
        add("buddha_incense",0,800,"jujutsu_kaisen_cursed_paths:buddha_incense",1,false,
            "seal_white",1,"curse_concentrate",1,"soul_echo",2,"residue_medium",3);
    }
    private static void add(String id,int tier,int ticks,String output,int count,boolean bp,Object... parts){Map<String,Integer> m=new LinkedHashMap<>();for(int i=0;i<parts.length;i+=2)m.put((String)parts[i],(Integer)parts[i+1]);ALL.add(new Recipe(id,tier,ticks,output,count,bp,Collections.unmodifiableMap(m)));}
    public static Item resolve(String id){return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id.contains(":")?id:"jujutsu_kaisen_cursed_paths:"+id));}
    public static Recipe get(String id){return ALL.stream().filter(r->r.id.equals(id)).findFirst().orElse(null);}
    public static void take(ServerPlayer p,Item item,int amount){for(int i=0;i<p.getInventory().getContainerSize()&&amount>0;i++){var s=p.getInventory().getItem(i);if(s.is(item)){int n=Math.min(amount,s.getCount());s.shrink(n);amount-=n;}}}
    private RitualRecipes(){}
}
