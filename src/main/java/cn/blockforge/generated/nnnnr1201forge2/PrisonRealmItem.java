package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** 封印物品携带完整囚徒快照；客户端只接收封印标识与数量，避免大量实体NBT导致网络包超限。 */
public final class PrisonRealmItem extends Item {
    public static final String SEAL="PrisonSeal";
    public PrisonRealmItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());}
    public static CompoundTag seal(ItemStack stack){return stack.getTag()==null?new CompoundTag():stack.getTag().getCompound(SEAL);}
    public static ItemStack packed(CompoundTag snapshot){
        ItemStack stack=new ItemStack(PrisonContent.PRISON.get());CompoundTag room=snapshot.getCompound("Room"),seal=new CompoundTag();
        seal.putUUID("Realm",room.getUUID("Id"));seal.putUUID("Token",room.getUUID("PortableToken"));seal.putInt("Count",snapshot.getList("Prisoners",Tag.TAG_COMPOUND).size());seal.put("Snapshot",snapshot);
        stack.getOrCreateTag().put(SEAL,seal);return stack;
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getPlayer()==null||c.getClickedFace()!=Direction.UP||PrisonSystem.locked(c.getPlayer()))return InteractionResult.FAIL;
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!(c.getPlayer() instanceof ServerPlayer player))return InteractionResult.FAIL;
        ItemStack stack=c.getItemInHand();CompoundTag seal=seal(stack);boolean carried=stack.getTag()!=null&&stack.getTag().contains(SEAL);
        PrisonStore store=PrisonStore.get(player.server);
        if(carried&&(!seal.hasUUID("Realm")||!seal.hasUUID("Token")||!store.canPlace(seal.getUUID("Realm"),seal.getUUID("Token")))){
            player.sendSystemMessage(Component.literal("该封印载体已放置或解封，或不属于此存档，不能重复释放囚徒"));return InteractionResult.FAIL;
        }
        // 狱门僵本体也不能放进封印维度，避免解封出口指回牢房。
        if(c.getLevel().dimension().equals(PrisonSystem.DIMENSION))return InteractionResult.FAIL;
        var pos=c.getClickedPos().above();PrisonRealmEntity realm=PrisonContent.REALM.get().create(c.getLevel());if(realm==null)return InteractionResult.FAIL;
        realm.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,player.getYRot(),0);
        if(!c.getLevel().noCollision(realm)||!c.getLevel().getEntities(realm,realm.getBoundingBox()).isEmpty())return InteractionResult.FAIL;
        if(carried){
            for(var level:player.server.getAllLevels())if(level.getEntity(seal.getUUID("Realm"))!=null){player.sendSystemMessage(Component.literal("同一狱门僵已经存在，请勿重复放置"));return InteractionResult.FAIL;}
            realm.setUUID(seal.getUUID("Realm"));
        }
        realm.configure(player.getUUID());if(!c.getLevel().addFreshEntity(realm))return InteractionResult.FAIL;
        if(carried)store.placed(realm.getUUID(),player.getUUID(),c.getLevel().dimension().location().toString(),realm.getX(),realm.getY(),realm.getZ());
        else PrisonSystem.registerRealm(realm);
        // 携带封印的物品即使在创造模式也消耗，旧令牌一次性失效。
        if(carried||!player.getAbilities().instabuild)stack.shrink(1);
        player.sendSystemMessage(Component.literal(store.occupied(realm.getUUID())?"狱门僵已放置 · 原封印与内部生物保持不变，可用邪去悔天梯解封":"狱门僵已放置 · 在附近聊天输入：狱门僵，开门"));return InteractionResult.CONSUME;
    }
    @Override public CompoundTag getShareTag(ItemStack stack){
        CompoundTag source=stack.getTag();if(source==null)return null;CompoundTag shared=source.copy();
        if(shared.contains(SEAL))shared.getCompound(SEAL).remove("Snapshot");return shared;
    }
    @Override public void inventoryTick(ItemStack stack,Level level,net.minecraft.world.entity.Entity holder,int slot,boolean selected){
        if(level.isClientSide||holder.getServer()==null||holder.tickCount%100!=0)return;CompoundTag seal=seal(stack);
        if(!seal.hasUUID("Realm")||!seal.hasUUID("Token"))return;PrisonStore store=PrisonStore.get(holder.getServer());
        if(store.canPlace(seal.getUUID("Realm"),seal.getUUID("Token"))){CompoundTag snapshot=store.portableSnapshot(seal.getUUID("Realm"));if(snapshot!=null){seal.put("Snapshot",snapshot);seal.putInt("Count",snapshot.getList("Prisoners",Tag.TAG_COMPOUND).size());}}
    }
    @Override public boolean canFitInsideContainerItems(){return false;}
    @Override public boolean canBeHurtBy(DamageSource source){return false;}
    @Override public boolean onEntityItemUpdate(ItemStack stack,ItemEntity entity){
        if(!entity.level().isClientSide){entity.setUnlimitedLifetime();entity.setInvulnerable(true);if(entity.getY()<entity.level().getMinBuildHeight()){entity.setPos(entity.getX(),entity.level().getMinBuildHeight()+4,entity.getZ());entity.setNoGravity(true);entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}}return false;
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){PrisonClient.item(consumer);}
    @Override public void appendHoverText(ItemStack s,Level l,java.util.List<Component> lines,TooltipFlag f){
        lines.add(Component.literal("右键地面放置；聊天：狱门僵，开门／狱门僵，闭门"));
        lines.add(Component.literal("闭门后，放置者空手右键收起；封印与内部数据随物品保存"));
        if(s.getTag()!=null&&s.getTag().contains(SEAL))lines.add(Component.literal("已封印："+seal(s).getInt("Count")+" 个生物 · 放下后以邪去悔天梯解封"));
        lines.add(Component.literal("可放普通箱子；不能放潜影盒或收纳袋；封印绑定当前存档"));
    }
}
