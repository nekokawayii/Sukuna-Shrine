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
        p.teleportTo(to,.5,65,-15.5,0,0);p.fallDistance=0;p.sendSystemMessage(Component.literal("进入512×512死灭回游结界。击杀参赛者得5分、咒灵得1分；每 100 分可按 shift+P 制定一条规则，定满 5 条才能脱离。中央通讯塔可兑换。"));
    }
    public static void leave(ServerPlayer p){
        if(!p.level().dimension().equals(DIM))return;
        // r218：必须制定满 5 条规则，才能在规则面板里选「脱离」离开
        if(character.culling.CullingRules.picked(p)<character.culling.CullingRules.MAX){
            p.sendSystemMessage(Component.literal("必须先制定 "+character.culling.CullingRules.MAX+" 条规则才能离开死灭洄游（shift+P 打开规则面板）。").withStyle(net.minecraft.ChatFormatting.GRAY));
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
        if(points<costs[selection]){p.sendSystemMessage(Component.literal("积分不足：现有"+points+"，需要"+costs[selection]+"；潜行右键换商品。"));return;}
        d.putInt("CullingPoints",points-costs[selection]);Journey.give(p,new ItemStack(RitualRecipes.resolve(ids[selection])));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||!p.level().dimension().equals(DIM)||p.isSpectator())return;
        if(p.getX()< -255||p.getX()>254||p.getZ()< -255||p.getZ()>254||p.getY()<1||p.getY()>510){p.teleportTo(p.serverLevel(),.5,65,-15.5,p.getYRot(),p.getXRot());p.fallDistance=0;}
        // r220：旧刷怪逻辑已整体交给 CullingSpawns（这里原来还会把参赛者塞进同一个阵营队伍、
        // 关掉友伤，导致「参赛者之间强制敌对」不生效，且和 CullingSpawns 重复刷怪）。此处只保留边界检查。
    }
    private CullingGame(){}
}
