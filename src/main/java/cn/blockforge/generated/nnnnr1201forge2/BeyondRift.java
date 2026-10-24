package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/**
 * 隐藏剧情「观测者」的服务端部分。
 * ①灵魂通道北端有一道常驻裂隙（表现见 BeyondRiftClient）；
 * ②击败五条悟之后靠近裂隙，会从里面掉出「不属于这个世界的碎片」；
 * ③带着碎片回到岩手地形的虎杖家：虎天帝消失，地上留下那封信，屏幕出现系统错误。
 */
@Mod.EventBusSubscriber(modid="sukuna")
public final class BeyondRift {
    /** 裂隙中心：灵魂通道北端内壁前 0.14 格（墙面在 z=-48.0，放更里面会被墙挡住）。 */
    public static final Vec3 CENTER=new Vec3(0.5,72.5,-47.86);
    public static final double HALF_WIDTH=14,HALF_HEIGHT=9;
    /** 虎杖家四方庭院：与 IwateVillage 的家区域完全一致。 */
    public static final int HOME_X=IwateVillage.HOME_X,HOME_Z=IwateVillage.HOME_Z,HOME_SIZE=IwateVillage.HOME_SIZE;
    private static final double DROP_RADIUS=18,GUARD_RADIUS=96;
    private static final int DROP_COOLDOWN=200;
    private static final String SEEN="BeyondVoidSeen",STAGE="BeyondStage",DROP_OWNER="BeyondDropOwner";
    private static final Map<UUID,Long> nextDrop=new HashMap<>();

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p instanceof net.minecraftforge.common.util.FakePlayer||!p.isAlive()||p.isSpectator())return;
        // 讲台读信也算亲自阅读，不要求把唯一一本书从讲台拿走。
        if(p.containerMenu instanceof net.minecraft.world.inventory.LecternMenu menu&&isNote(menu.getBook()))recordRead(p);
        if(p.level().dimension()!=SoulJourney.CORRIDOR)return;
        if(p.tickCount%10!=0)return;
        ServerLevel level=p.serverLevel();
        dropFragment(p,level);
        if(p.tickCount%20==0)readLetter(p,level);
    }

    /** 每人的碎片独立掉落；他人的掉落不占名额，读过信但遗失碎片也能回来取。 */
    private static void dropFragment(ServerPlayer p,ServerLevel level){
        if(!BeyondProgressRules.needsFragment(VoidJourney.defeated(p)||VoidJourney.evolved(p),Authority.unlocked(p),carries(p),false))return;
        if(p.position().distanceToSqr(CENTER)>DROP_RADIUS*DROP_RADIUS)return;
        long now=level.getGameTime();
        if(now<nextDrop.getOrDefault(p.getUUID(),0L))return;
        AABB area=new AABB(CENTER,CENTER).inflate(24);
        boolean fallen=!level.getEntitiesOfClass(ItemEntity.class,area,it->it.getItem().is(Content.BEYOND_FRAGMENT.get())&&ownedDrop(p,it)).isEmpty();
        if(fallen)return;
        nextDrop.put(p.getUUID(),now+DROP_COOLDOWN);
        Vec3 from=CENTER.add(0,-2.2,1.2);   // 往房间内侧（+z）掉，不能掉进墙里
        ItemEntity item=new ItemEntity(level,from.x,from.y,from.z,new ItemStack(Content.BEYOND_FRAGMENT.get()));
        item.setDeltaMovement(p.position().subtract(from).normalize().scale(0.22).add(0,0.12,0));
        item.setPickUpDelay(20);
        ownDrop(p,item);
        item.setGlowingTag(true);
        if(!level.addFreshEntity(item))return;
        level.sendParticles(ParticleTypes.PORTAL,from.x,from.y,from.z,24,0.5,0.8,0.5,0.35);
        level.playSound(null,from.x,from.y,from.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1.2f,0.55f);
        Curse.data(p).putInt(STAGE,Math.max(1,Curse.data(p).getInt(STAGE)));
        com.mojang.logging.LogUtils.getLogger().info("[裂隙] 碎片掉出：玩家={} 位置=({},{},{})",p.getName().getString(),String.format(Locale.ROOT,"%.1f",from.x),String.format(Locale.ROOT,"%.1f",from.y),String.format(Locale.ROOT,"%.1f",from.z));
    }

    /** 信：带着碎片走进虎杖家，虎天帝消失、地上留信、屏幕报错。 */
    private static void readLetter(ServerPlayer p,ServerLevel level){
        if(!carries(p)||Authority.unlocked(p))return;
        if(p.getY()<=SoulCorridorGenerator.IWATE_FLOOR||!inHome(p))return;
        var data=Curse.data(p);var state=SoulWorldState.get(level);
        // 虎天帝消失是世界事件，但不能把后到的玩家永远挡在没有守护者的分支外。
        if(data.getBoolean(SEEN)&&!state.beyondDone())state.markBeyondDone();
        if(state.beyondDone()){
            if(!data.getBoolean(SEEN)){data.putBoolean(SEEN,true);data.putInt(STAGE,Math.max(2,data.getInt(STAGE)));glitch(p);}
            ensureNote(p,level);
            return;
        }
        AABB area=new AABB(HOME_X,IwateVillage.Y-32,HOME_Z,HOME_X+HOME_SIZE,IwateVillage.Y+64,HOME_Z+HOME_SIZE).inflate(GUARD_RADIUS,0,GUARD_RADIUS);
        List<GuardianEntity> guards=level.getEntitiesOfClass(GuardianEntity.class,area,GuardianEntity::peacefulResident);
        if(guards.isEmpty())return;   // 虎天帝不在（还没和解/还没召唤）就不触发，避免把剧情空放掉
        GuardianEntity guard=guards.get(0);
        Vec3 at=guard.position();
        for(GuardianEntity other:guards)vanish(other);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,at.x,at.y+1,at.z,48,0.6,1.0,0.6,0.02);
        level.sendParticles(ParticleTypes.PORTAL,at.x,at.y+1,at.z,64,0.7,1.1,0.7,0.5);
        level.playSound(null,at.x,at.y,at.z,SoundEvents.PORTAL_TRAVEL,SoundSource.PLAYERS,1.4f,0.6f);
        level.playSound(null,at.x,at.y,at.z,SoundEvents.GLASS_BREAK,SoundSource.PLAYERS,1.0f,0.55f);
        ItemEntity letter=new ItemEntity(level,at.x,at.y+0.35,at.z,letter());
        letter.setPickUpDelay(20);
        ownDrop(p,letter);
        letter.setGlowingTag(true);
        level.addFreshEntity(letter);
        Curse.data(p).putBoolean(SEEN,true);
        Curse.data(p).putInt(STAGE,2);
        SoulWorldState.get(level).markBeyondDone();
        com.mojang.logging.LogUtils.getLogger().info("[裂隙] 虎天帝消失并留下信：玩家={} 位置=({},{},{})",p.getName().getString(),String.format(Locale.ROOT,"%.1f",at.x),String.format(Locale.ROOT,"%.1f",at.y),String.format(Locale.ROOT,"%.1f",at.z));
        glitch(p);
    }
    private static void vanish(GuardianEntity guard){
        guard.discard();
    }

    // 隐藏线不注册可被聊天补全发现的调试命令，也不发送前置条件指引。

    private static boolean ownedDrop(ServerPlayer p,ItemEntity item){
        var tag=item.getPersistentData();
        return BeyondProgressRules.blocksFragment(p.getUUID(),tag.hasUUID(DROP_OWNER)?tag.getUUID(DROP_OWNER):null);
    }
    private static void ownDrop(ServerPlayer p,ItemEntity item){
        item.setTarget(p.getUUID());item.getPersistentData().putUUID(DROP_OWNER,p.getUUID());
    }
    private static boolean isNote(ItemStack stack){
        CompoundTag tag=stack.getTag();
        return stack.is(Items.WRITTEN_BOOK)&&tag!=null&&(tag.getBoolean("SukunaBeyondNote")
            ||"观测者的低语".equals(tag.getString("title"))&&"Itadori_Yūji.pl?a!y@er".equals(tag.getString("author")));
    }
    private static void ensureNote(ServerPlayer p,ServerLevel level){
        if(!BeyondProgressRules.canReceiveNote(carries(p),inHome(p)&&p.getY()>SoulCorridorGenerator.IWATE_FLOOR,
            SoulWorldState.get(level).beyondDone(),Curse.data(p).getBoolean(SEEN),readNote(p)))return;
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(isNote(p.getInventory().getItem(i)))return;
        if(isNote(p.containerMenu.getCarried()))return;
        AABB home=new AABB(HOME_X,IwateVillage.Y-32,HOME_Z,HOME_X+HOME_SIZE,IwateVillage.Y+64,HOME_Z+HOME_SIZE).inflate(GUARD_RADIUS,0,GUARD_RADIUS);
        if(!level.getEntitiesOfClass(ItemEntity.class,home,it->isNote(it.getItem())&&ownedDrop(p,it)).isEmpty())return;
        // 仅交付，不替玩家勾上「读过」；背包满则下一秒重试，不刷出一地的书。
        if(p.getInventory().getFreeSlot()>=0&&p.getInventory().add(letter()))p.inventoryMenu.broadcastChanges();
    }
    private static void recordRead(ServerPlayer p){
        var data=Curse.data(p);data.putBoolean("BeyondNoteRead",true);data.putBoolean(SEEN,true);
        data.putInt(STAGE,Math.max(2,data.getInt(STAGE)));
    }
    public static boolean readNote(ServerPlayer p){return Curse.data(p).getBoolean("BeyondNoteRead");}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void openNote(PlayerInteractEvent.RightClickItem e){
        if(e.getEntity() instanceof ServerPlayer p&&isNote(e.getItemStack())&&!p.getCooldowns().isOnCooldown(Items.WRITTEN_BOOK))recordRead(p);
    }

    /** 碎片对着方块或生物也走同一入口，避免联机准星命中队友时右键被交互吞掉。 */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void useFragment(PlayerInteractEvent e){
        if(!(e instanceof PlayerInteractEvent.RightClickItem||e instanceof PlayerInteractEvent.RightClickBlock
            ||e instanceof PlayerInteractEvent.EntityInteract||e instanceof PlayerInteractEvent.EntityInteractSpecific))return;
        if(!e.getItemStack().is(Content.BEYOND_FRAGMENT.get()))return;
        if(e.getEntity() instanceof ServerPlayer p)activateFragment(p,e.getHand());
        e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);e.setCanceled(true);
    }
    public static void activateFragment(ServerPlayer p,net.minecraft.world.InteractionHand hand){
        if(!p.getItemInHand(hand).is(Content.BEYOND_FRAGMENT.get())||!p.isAlive()||p.isSpectator()||ObserverRitual.active(p))return;
        if(Authority.unlocked(p)){
            p.setItemInHand(hand,new ItemStack(Authority.EDITOR.get()));AuthorityPackets.open(p);
        }else ObserverRitual.start(p);
    }
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){nextDrop.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){nextDrop.clear();}

    /** 那封信：书与笔，只有一页。 */
    private static ItemStack letter(){
        ItemStack book=new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag=book.getOrCreateTag();
        tag.putBoolean("SukunaBeyondNote",true);
        tag.putString("title","观测者的低语");
        tag.putString("author","Itadori_Yūji.pl?a!y@er");
        ListTag pages=new ListTag();
        pages.add(StringTag.valueOf("{\"text\":\"你切开过「世界」，\\n但你有没有想过——\\n世界外面是什么？\"}"));
        tag.put("pages",pages);
        return book;
    }

    /** 系统错误：红色标题 + 聊天栏乱码。 */
    private static void glitch(ServerPlayer p){
        if(p.connection==null)return;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(8,70,24));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("[系统错误：检测到越界数据]").withStyle(ChatFormatting.RED,ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Itadori_Yūji.pl?a!y@er 正在尝试越权...").withStyle(ChatFormatting.DARK_RED)));
        p.sendSystemMessage(Component.literal("[系统错误：检测到越界数据] Itadori_Yūji.pl?a!y@er 正在尝试越权...").withStyle(ChatFormatting.RED));
        p.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL,SoundSource.MASTER,0.5f,1.6f);
    }

    public static boolean inHome(ServerPlayer p){
        return p.getX()>=HOME_X&&p.getX()<HOME_X+HOME_SIZE&&p.getZ()>=HOME_Z&&p.getZ()<HOME_Z+HOME_SIZE;
    }
    public static boolean carries(ServerPlayer p){
        var inventory=p.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(Content.BEYOND_FRAGMENT.get()))return true;
        return false;
    }
    private BeyondRift(){}
}
