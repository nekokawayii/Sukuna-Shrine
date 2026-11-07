package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Jade 只显示服务器提供的余额，绝不在客户端初始化一个假的满咒力池。 */
@WailaPlugin("jujutsu_kaisen_cursed_paths")
public final class JadeNpcPlugin implements IWailaPlugin {
    private static final ResourceLocation UID = new ResourceLocation("jujutsu_kaisen_cursed_paths", "npc_curse");
    private static final Provider PROVIDER = new Provider();
    private static final class Provider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
        @Override public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
            if (!(accessor.getEntity() instanceof Mob mob)) return;
            if(mob instanceof LowCurseEntity curse){tag.putString("SukunaCurseGrade",curse.form().gradeName());tag.putFloat("SukunaCurseOriginalHeight",curse.form().originalHeight());}
            boolean physical = "toji".equals(NpcBridge.character(mob));
            var value = new CompoundTag();
            value.putBoolean("Physical", physical);
            if (physical) {
                var actor = NpcBridge.actorFor(mob);
                if (actor == null) return;
                value.putDouble("Current", character.toji.TojiCombat.stamina(actor));
                value.putDouble("Maximum", character.toji.TojiCombat.maximum(actor));
            } else {
                value.putDouble("Current", NpcCurse.energy(mob));
                value.putDouble("Maximum", NpcCurse.max(mob));
            }
            value.putBoolean("Clashing", DomainClash.active(mob));
            value.putBoolean("Stunned", character.npc.NpcControl.locked(mob) || DomainClash.stunned(mob));
            tag.put("SukunaNpcResource", value);
        }
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            var tag = accessor.getServerData();
            if(tag.contains("SukunaCurseGrade"))tooltip.add(Component.literal("咒灵等级："+tag.getString("SukunaCurseGrade")+" · 原高 "+String.format(java.util.Locale.ROOT,"%.2f",tag.getFloat("SukunaCurseOriginalHeight"))+"格"));
            if (!tag.contains("SukunaNpcResource")) return;
            var value = tag.getCompound("SukunaNpcResource");
            tooltip.add(Component.literal((value.getBoolean("Physical") ? "体术 " : "咒力 ")
                + (long)value.getDouble("Current") + " / " + (long)value.getDouble("Maximum")));
            if (value.getBoolean("Clashing")) tooltip.add(Component.literal("领域对拼中"));
            else if (value.getBoolean("Stunned")) tooltip.add(Component.literal("短暂硬直中"));
        }
        @Override public ResourceLocation getUid() { return UID; }
    }
    @Override public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(PROVIDER, Mob.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(PROVIDER, Mob.class);
    }
}
