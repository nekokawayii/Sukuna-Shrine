package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ModTab {
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,"sukuna");
    public static final RegistryObject<CreativeModeTab> MAIN=TABS.register("sukuna",()->CreativeModeTab.builder().title(Component.literal("宿傩 · 咒术与净土")).icon(()->new ItemStack(Content.FINGER.get())).displayItems((parameters,output)->Content.ITEMS.getEntries().stream().filter(item->item!=Content.BEYOND_FRAGMENT&&!Authority.hidden(item.get())&&!character.mahito.MahitoTransfigure.isTransfigured(new ItemStack(item.get()))).forEach(item->output.accept(item.get()))).build());
    public static void init(IEventBus bus){TABS.register(bus);}
    private ModTab(){}
}
