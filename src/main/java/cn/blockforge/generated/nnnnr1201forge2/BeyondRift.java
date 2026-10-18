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
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
    private static final String SEEN="BeyondVoidSeen",STAGE="BeyondStage";
    private static final Map<UUID,Long> nextDrop=new HashMap<>();

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        if(p.level().dimension()!=SoulJourney.CORRIDOR)return;
        if(p.tickCount%10!=0)return;
        ServerLevel level=p.serverLevel();
        dropFragment(p,level);
        if(p.tickCount%20==0)readLetter(p,level);
    }

    /** 碎片：五条悟通关后靠近裂隙才会掉，同一名玩家 10 秒最多一次，地上还有就不再掉。 */
    private static void dropFragment(ServerPlayer p,ServerLevel level){
        if(!VoidJourney.defeated(p)&&!VoidJourney.evolved(p))return;
        if(Curse.data(p).getBoolean(SEEN))return;
        if(carries(p))return;
        if(p.position().distanceToSqr(CENTER)>DROP_RADIUS*DROP_RADIUS)return;
        long now=level.getGameTime();
        if(now<nextDrop.getOrDefault(p.getUUID(),0L))return;
        AABB area=new AABB(CENTER,CENTER).inflate(24);
        boolean fallen=!level.getEntitiesOfClass(ItemEntity.class,area,it->it.getItem().is(Content.BEYOND_FRAGMENT.get())).isEmpty();
        if(fallen)return;
        nextDrop.put(p.getUUID(),now+DROP_COOLDOWN);
        Vec3 from=CENTER.add(0,-2.2,1.2);   // 往房间内侧（+z）掉，不能掉进墙里
        ItemEntity item=new ItemEntity(level,from.x,from.y,from.z,new ItemStack(Content.BEYOND_FRAGMENT.get()));
        item.setDeltaMovement(p.position().subtract(from).normalize().scale(0.22).add(0,0.12,0));
        item.setPickUpDelay(20);
        item.setGlowingTag(true);
        if(!level.addFreshEntity(item))return;
        level.sendParticles(ParticleTypes.PORTAL,from.x,from.y,from.z,24,0.5,0.8,0.5,0.35);
        level.playSound(null,from.x,from.y,from.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1.2f,0.55f);
        Curse.data(p).putInt(STAGE,1);
        com.mojang.logging.LogUtils.getLogger().info("[裂隙] 碎片掉出：玩家={} 位置=({},{},{})",p.getName().getString(),String.format(Locale.ROOT,"%.1f",from.x),String.format(Locale.ROOT,"%.1f",from.y),String.format(Locale.ROOT,"%.1f",from.z));
        p.sendSystemMessage(Component.literal("【裂隙】有什么东西从裂缝里掉了出来……").withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** 信：带着碎片走进虎杖家，虎天帝消失、地上留信、屏幕报错。 */
    private static void readLetter(ServerPlayer p,ServerLevel level){
        if(Curse.data(p).getBoolean(SEEN))return;
        if(!carries(p))return;
        if(p.getY()<=SoulCorridorGenerator.IWATE_FLOOR)return;
        if(!inHome(p))return;
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

    /** 测试与自查用：/sukuna beyond status|tp|give|reset。 */
    @SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent e){
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("sukuna").then(net.minecraft.commands.Commands.literal("beyond")
            .then(net.minecraft.commands.Commands.literal("status").executes(c->{
                ServerPlayer p=c.getSource().getPlayerOrException();
                p.sendSystemMessage(Component.literal("五条悟通关："+(VoidJourney.defeated(p)||VoidJourney.evolved(p))+"；携带碎片："+carries(p)+"；已在虎杖家看过信："+Curse.data(p).getBoolean(SEEN)+"；虎天帝是否已消失："+SoulWorldState.get(p.serverLevel()).beyondDone()));
                return 1;}))
            .then(net.minecraft.commands.Commands.literal("tp").executes(c->{
                ServerPlayer p=c.getSource().getPlayerOrException();ServerLevel level=p.server.getLevel(SoulJourney.CORRIDOR);
                if(level==null){p.sendSystemMessage(Component.literal("灵魂通道维度未载入"));return 0;}
                p.teleportTo(level,0.5,63,-44,180,0);
                p.sendSystemMessage(Component.literal("已传送到灵魂通道北端：抬头就是那道裂隙。"));
                return 1;}))
            .then(net.minecraft.commands.Commands.literal("give").requires(s->s.hasPermission(2)).executes(c->{
                ServerPlayer p=c.getSource().getPlayerOrException();
                if(!p.getInventory().add(new ItemStack(Content.BEYOND_FRAGMENT.get())))p.drop(new ItemStack(Content.BEYOND_FRAGMENT.get()),false);
                p.sendSystemMessage(Component.literal("已获得：不属于这个世界的碎片"));
                return 1;}))
            .then(net.minecraft.commands.Commands.literal("reset").requires(s->s.hasPermission(2)).executes(c->{
                ServerPlayer p=c.getSource().getPlayerOrException();
                Curse.data(p).remove(SEEN);Curse.data(p).remove(STAGE);
                SoulWorldState.get(p.serverLevel()).resetBeyond();
                p.sendSystemMessage(Component.literal("隐藏剧情已重置（虎天帝可用合照重新召唤）。"));
                return 1;}))));
    }

    /** 那封信：书与笔，只有一页。 */
    private static ItemStack letter(){
        ItemStack book=new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag=book.getOrCreateTag();
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
