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
        if(!p.isAlive()||p.isCreative()||p.tickCount%300!=0||!p.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING))return;
        if(p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,p.getBoundingBox().inflate(80),m->m.getPersistentData().getBoolean("CullingEnemy")).size()>28)return;
        var r=p.getRandom();for(int i=0;i<4;i++){int x=(int)p.getX()+r.nextInt(65)-32,z=(int)p.getZ()+r.nextInt(65)-32;if(Math.abs(x)<22&&Math.abs(z)<22||Math.abs(x)>245||Math.abs(z)>245)continue;
            String role=i<2?"mutant":switch(r.nextInt(3)){case 0->"participant_ryu";case 1->"participant_kashimo";default->"participant_mahito";};
            var floor=new BlockPos(x,65,z);if(!p.serverLevel().hasChunkAt(floor))continue;BlockPos at=p.level().getBlockState(floor).isAir()?floor:floor.above();
            var m=ExpeditionMobs.spawn(p.serverLevel(),at,role,p);if(m!=null){String faction=role.startsWith("participant")?role.substring("participant_".length()):"mutants";var board=p.serverLevel().getScoreboard();String teamId="culling_"+faction;var team=board.getPlayerTeam(teamId);if(team==null){team=board.addPlayerTeam(teamId);team.setAllowFriendlyFire(false);team.setDisplayName(Component.literal("死灭阵营 · "+faction));}board.addPlayerToTeam(m.getScoreboardName(),team);m.getPersistentData().putString("CullingFaction",faction);m.getPersistentData().putBoolean("CullingEnemy",true);m.getPersistentData().putLong("ExpeditionDespawnAt",p.level().getGameTime()+4800);}
        }
    }
    private CullingGame(){}
}
