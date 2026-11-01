package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.*;

public final class TechniqueEffects {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,"sukuna");
    public static final RegistryObject<MobEffect> BURNOUT=EFFECTS.register("technique_burnout",()->new MobEffect(MobEffectCategory.HARMFUL,0x853343){
        @Override public List<ItemStack> getCurativeItems(){return List.of();}
    });
    /** 天逆蛑造成的 3 秒「术式封印」：只负责显示与说明，真正的封锁判定在 {@link Curse#canUseTechnique}。 */
    public static final RegistryObject<MobEffect> TECHNIQUE_SEAL=EFFECTS.register("technique_seal",()->new MobEffect(MobEffectCategory.HARMFUL,0xCFF3FF){
        @Override public List<ItemStack> getCurativeItems(){return List.of();}
    });
    private TechniqueEffects(){}
}
