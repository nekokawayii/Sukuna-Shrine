package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CullingGame {
    public static final ResourceKey<Level> DIM=ResourceKey.create(Registries.DIMENSION,new ResourceLocation("sukuna","culling_game"));
    public static void enter(ServerPlayer p){
        if(PrisonSystem.locked(p)){p.sendSystemMessage(Component.literal("封印中无法使用死灭回游通行咒符。"));return;}
        if(p.level().dimension().equals(DIM)){leave(p);return;}
        ServerLevel to=p.server.getLevel(DIM);if(to==null){p.sendSystemMessage(Component.literal("死灭回游空间尚未加载，请重新进入世界。"));return;}
        CompoundTag n=new CompoundTag();n.putString("Dim",p.level().dimension().location().toString());n.putDouble("X",p.getX());n.putDouble("Y",p.getY());n.putDouble("Z",p.getZ());ExpeditionProgress.data(p).put("ExpeditionReturn",n);
        // r251：先进 1024,1024 的初始登记室，右键木门才进城市
        character.culling.CullingGate.ensureRoom(to);
        var room=character.culling.CullingGate.spawnPos();
        p.teleportTo(to,room.getX()+.5,room.getY()+1,room.getZ()+.5,0,0);p.fallDistance=0;
        character.culling.CullingEntry.onEntered(p);p.sendSystemMessage(Component.literal("进入512×512死灭回游结界。击杀参赛者得5分、咒灵得1分；每 100 分可按 shift+P 制定一条规则，定满 5 条才能脱离。中央通讯塔可兑换。"));
    }
    public static void leave(ServerPlayer p){
        if(!p.level().dimension().equals(DIM))return;
        // r218：必须制定满 5 条规则，才能在规则面板里选「脱离」离开
        if(character.culling.CullingRules.picked(p)<character.culling.CullingRules.ESCAPE_AT){
            p.sendSystemMessage(Component.literal("至少制定 "+character.culling.CullingRules.ESCAPE_AT+" 条规则才能离开死灭洄游（shift+P 打开规则面板）。").withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }
        character.culling.CullingRules.onLeave(p);
        character.culling.CullingExtras.onLeave(p);
        var n=ExpeditionProgress.data(p).getCompound("ExpeditionReturn");
        ResourceLocation id=ResourceLocation.tryParse(n.getString("Dim"));ServerLevel to=id==null?null:p.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));if(to==null)to=p.server.overworld();
        BlockPos desired=n.contains("X")?BlockPos.containing(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z")):to.getSharedSpawnPos();
        to.getChunk(desired);BlockPos safe=ExpeditionEvents.safePosition(to,desired,8);
        if(safe==null){BlockPos spawn=to.getSharedSpawnPos();to.getChunk(spawn);safe=ExpeditionEvents.safePosition(to,to.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spawn),16);}
        if(safe==null){p.sendSystemMessage(Component.literal("返回处暂时没有安全落脚点，未执行传送；请先确认主世界出生点安全。"));return;}
        p.teleportTo(to,safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());p.fallDistance=0;
    }
    public static void exchange(ServerPlayer p){
        String[] ids={"special_blueprint","curse_concentrate","star_iron_fragment","sukuna:yuyun"};int[] costs={40,10,15,100};
        var d=ExpeditionProgress.data(p);int selection=Math.floorMod(d.getInt("CullingExchangeChoice"),ids.length),points=d.getInt("CullingPoints");
        if(p.isShiftKeyDown()){selection=(selection+1)%ids.length;d.putInt("CullingExchangeChoice",selection);p.sendSystemMessage(Component.literal("积分 "+points+"；选择 "+new ItemStack(RitualRecipes.resolve(ids[selection])).getHoverName().getString()+"，需要 "+costs[selection]+" 分。普通右键购买。"));return;}
        int cost=character.culling.CullingRules.special(p,"half_price")?Math.max(1,costs[selection]/2):costs[selection];
        if(points<cost){p.sendSystemMessage(Component.literal("积分不足：现有"+points+"，需要"+cost+"；潜行右键换商品。"));return;}
        d.putInt("CullingPoints",points-cost);Journey.give(p,new ItemStack(RitualRecipes.resolve(ids[selection])));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||!p.level().dimension().equals(DIM)||p.isSpectator())return;
        if((p.getX()< -255||p.getX()>254||p.getZ()< -255||p.getZ()>254||p.getY()<1||p.getY()>510)
            &&!character.culling.CullingGate.inRoomArea(p.blockPosition())){
            p.teleportTo(p.serverLevel(),.5,65,-15.5,p.getYRot(),p.getXRot());p.fallDistance=0;}
        // r220：旧刷怪逻辑已整体交给 CullingSpawns（这里原来还会把参赛者塞进同一个阵营队伍、
        // 关掉友伤，导致「参赛者之间强制敌对」不生效，且和 CullingSpawns 重复刷怪）。此处只保留边界检查。
    }
    /** r259：老存档里已经生成的沙砾层，在区块加载时替换成基岩。 */
    @SubscribeEvent
    public static void chunkLoad(net.minecraftforge.event.level.ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !level.dimension().equals(DIM)) return;
        var chunk = e.getChunk();
        BlockPos origin = chunk.getPos().getWorldPosition();
        boolean changed = false;
        // r260：整个区块（全高度）的沙砾统统换成基岩
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++)
            for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                BlockPos pos = origin.offset(dx, y, dz);
                if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GRAVEL)) {
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState(), 2);
                    changed = true;
                }
            }
        if (changed) com.mojang.logging.LogUtils.getLogger().info("[死灭洄游] 区块 {} 的沙砾层已换成基岩", chunk.getPos());
    }

    private CullingGame(){}
}
