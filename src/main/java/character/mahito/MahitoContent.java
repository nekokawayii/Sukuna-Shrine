package character.mahito;

import cn.blockforge.generated.nnnnr1201forge2.Content;
import cn.blockforge.generated.nnnnr1201forge2.ExpeditionContent;
import cn.blockforge.generated.nnnnr1201forge2.ProgressionContent;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

public final class MahitoContent {
    public static final RegistryObject<EntityType<MahitoPuppetEntity>> PUPPET=Content.ENTITIES.register("mahito_puppet",()->EntityType.Builder.of(MahitoPuppetEntity::new,MobCategory.MONSTER).sized(.8f,1.9f).clientTrackingRange(8).updateInterval(3).build("jujutsu_kaisen_cursed_paths:mahito_puppet"));
    public static final RegistryObject<MobEffect> BLEEDING=ExpeditionContent.EFFECTS.register("mahito_bleeding",()->new MobEffect(MobEffectCategory.HARMFUL,0x9c3545){
        @Override public boolean isDurationEffectTick(int duration,int amplifier){return duration%20==0;}
        @Override public void applyEffectTick(LivingEntity entity,int amplifier){entity.hurt(entity.damageSources().magic(),1+amplifier);}
    });
    public static final RegistryObject<Block> FLESH=ProgressionContent.BLOCKS.register("mahito_flesh",()->new Block(BlockBehaviour.Properties.copy(Blocks.SLIME_BLOCK).strength(3).speedFactor(.85f).lightLevel(s->4)));
    public static final RegistryObject<Block> FACES=ProgressionContent.BLOCKS.register("mahito_faces",()->new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).strength(50,3600000).lightLevel(s->7)));
    public static void init(net.minecraftforge.eventbus.api.IEventBus bus){bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(PUPPET.get(),MahitoPuppetEntity.attributes().build()));}
    private MahitoContent(){}
}
