package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.RegistryObject;

public final class PrisonContent {
    public static final RegistryObject<EntityType<PrisonRealmEntity>> REALM=Content.ENTITIES.register("prison_realm",()->EntityType.Builder.<PrisonRealmEntity>of(PrisonRealmEntity::new,MobCategory.MISC).sized(1,1.25f).fireImmune().clientTrackingRange(16).updateInterval(1).build("sukuna:prison_realm"));
    public static final RegistryObject<EntityType<JacobsLadderEntity>> LIGHT=Content.ENTITIES.register("jacobs_ladder",()->EntityType.Builder.<JacobsLadderEntity>of(JacobsLadderEntity::new,MobCategory.MISC).sized(10,16).fireImmune().clientTrackingRange(24).updateInterval(1).build("sukuna:jacobs_ladder"));
    public static final RegistryObject<Item> PRISON=Content.ITEMS.register("prison_realm",PrisonRealmItem::new);
    public static final RegistryObject<Item> LADDER=Content.ITEMS.register("jacobs_ladder",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            ItemStack stack=player.getItemInHand(hand);if(PrisonSystem.locked(player))return InteractionResultHolder.fail(stack);
            if(player instanceof ServerPlayer p){
                if(p.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);
                HitResult hit=p.pick(24,0,false);Vec3 at=hit.getLocation();
                // 射线优先选中狱门僵，法阵中心不会落到方块背后。
                Vec3 start=p.getEyePosition(),end=start.add(p.getLookAngle().scale(24));double nearest=start.distanceToSqr(at);
                for(PrisonRealmEntity r:level.getEntitiesOfClass(PrisonRealmEntity.class,p.getBoundingBox().inflate(25))){
                    var clip=r.getBoundingBox().inflate(.15).clip(start,end);if(clip.isPresent()&&start.distanceToSqr(clip.get())<=nearest){nearest=start.distanceToSqr(clip.get());at=r.position();}
                }
                if(hit.getType()==HitResult.Type.MISS&&nearest>=24*24-.1)at=p.position().add(p.getLookAngle().x*6,0,p.getLookAngle().z*6);
                JacobsLadderEntity light=LIGHT.get().create(level);if(light==null)return InteractionResultHolder.fail(stack);
                light.moveTo(at.x,at.y+.03,at.z,0,0);light.configure(p.getUUID());if(!level.addFreshEntity(light))return InteractionResultHolder.fail(stack);
                p.getCooldowns().addCooldown(this,200);p.sendSystemMessage(Component.literal("邪去悔天梯 · 圣光持续6秒，结束时破坏法阵中的狱门僵并解封"));
            }
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){PrisonClient.item(consumer);}
        @Override public void appendHoverText(ItemStack s,Level l,java.util.List<Component> lines,TooltipFlag f){lines.add(Component.literal("右键向准星释放6秒圣光；半径5格，持续光伤害"));lines.add(Component.literal("结束时破坏范围内的狱门僵，释放被封印者并归还物品"));}
    });
    public static void init(){MinecraftForge.EVENT_BUS.register(PrisonSystem.class);}
    private PrisonContent(){}
}
