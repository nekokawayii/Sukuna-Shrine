package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Jade 联动：在生物信息栏顶部显示咒力（当前 / 上限）。 */
@WailaPlugin("sukuna")
public final class JadeNpcPlugin implements IWailaPlugin {
    private static final ResourceLocation UID = new ResourceLocation("sukuna", "npc_curse");

    private static final IEntityComponentProvider PROVIDER = new IEntityComponentProvider() {
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            var entity = accessor.getEntity();
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) return;
            double cur = NpcCurse.energy(living);
            double max = NpcCurse.max(living);
            tooltip.add(Component.literal("\u5492\u529b " + (long) cur + " / " + (long) max));
        }
        @Override public ResourceLocation getUid() { return UID; }
    };

    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(PROVIDER, Mob.class);
    }
}
