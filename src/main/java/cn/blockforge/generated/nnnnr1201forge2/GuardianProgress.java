package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 主世界保存通关名单：跨维度、离线、重启均保留；不复制物品、手指、控制器或临时状态。 */
public final class GuardianProgress extends SavedData {
    private CompoundTag victories=new CompoundTag(),profiles=new CompoundTag(),completed=new CompoundTag(),pendingLegacy=new CompoundTag();
    private static GuardianProgress get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(GuardianProgress::load,GuardianProgress::new,"sukuna_guardian_progress");}
    public static GuardianProgress load(CompoundTag tag){GuardianProgress state=new GuardianProgress();state.victories=tag.getCompound("Victories").copy();state.profiles=tag.getCompound("Profiles").copy();state.completed=tag.getCompound("CompletedBattles").copy();state.pendingLegacy=tag.getCompound("PendingLegacy").copy();return state;}
    @Override public CompoundTag save(CompoundTag tag){tag.put("Victories",victories.copy());tag.put("Profiles",profiles.copy());tag.put("CompletedBattles",completed.copy());tag.put("PendingLegacy",pendingLegacy.copy());return tag;}
    private boolean snapshot(ServerPlayer p){
        CompoundTag profile=new CompoundTag();profile.putString("Name",p.getGameProfile().getName());profile.put("Relations",Curse.data(p).getCompound("Relations").copy());
        String id=p.getUUID().toString();boolean changed=!profile.equals(profiles.getCompound(id));
        if(changed){profiles.put(id,profile);setDirty();}
        // 兼容旧版已经记录的胜利，不要求玩家重新打一次。
        if(GuardianCombat.awakened(p))changed|=award(id);
        return changed;
    }
    private boolean award(String id){if(victories.getBoolean(id))return false;victories.putBoolean(id,true);setDirty();return true;}
    private int relation(String from,String to){return profiles.getCompound(from).getCompound("Relations").getCompound(to).getInt("Mode");}
    private String team(MinecraftServer server,String id){String name=profiles.getCompound(id).getString("Name");var team=server.getScoreboard().getPlayersTeam(name);return team==null?"":team.getName();}
    private void share(MinecraftServer server){
        Set<String> known=new HashSet<>(profiles.getAllKeys());known.addAll(victories.getAllKeys());
        for(String id:profiles.getAllKeys())known.addAll(profiles.getCompound(id).getCompound("Relations").getAllKeys());
        Map<String,String> teams=new HashMap<>();for(String id:known)teams.put(id,team(server,id));
        ArrayDeque<String> queue=new ArrayDeque<>(victories.getAllKeys());Set<String> visited=new HashSet<>();
        while(!queue.isEmpty()){
            String winner=queue.removeFirst();if(!visited.add(winner))continue;
            for(String other:known){
                if(victories.getBoolean(other))continue;
                int a=relation(winner,other),b=relation(other,winner);
                boolean sameTeam=!teams.getOrDefault(winner,"").isEmpty()&&teams.get(winner).equals(teams.get(other));
                if(GuardianProgressRules.shares(a,b,sameTeam)&&award(other))queue.addLast(other);
            }
        }
        for(ServerPlayer p:server.getPlayerList().getPlayers())if(victories.getBoolean(p.getUUID().toString())){
            Curse.data(p).putBoolean("GuardianDefeated",true);
            String id=p.getUUID().toString();int pending=pendingLegacy.getInt(id);
            if(VoidJourney.evolved(p)&&(pending>0||!Curse.data(p).getBoolean("LegacyItemGiven"))){
                int count=Math.max(1,Math.min(64,pending));Curse.data(p).putBoolean("LegacyItemGiven",true);
                Journey.give(p,new net.minecraft.world.item.ItemStack(StageFood.LEGACY.get(),count));pendingLegacy.putInt(id,Math.max(0,pending-count));setDirty();
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("虎杖的对话结束：获得传承。自己吃下或喂给虚空御厨子阶段的女仆，解锁新技能与固定成长。"));
            }
        }
    }
    public static void complete(MinecraftServer server,UUID battle,Collection<UUID> participants){
        GuardianProgress state=get(server);
        for(ServerPlayer p:server.getPlayerList().getPlayers())state.snapshot(p);
        if(!state.completed.getBoolean(battle.toString())){
            for(UUID id:participants){state.award(id.toString());state.pendingLegacy.putInt(id.toString(),Math.min(1024,state.pendingLegacy.getInt(id.toString())+1));}
            state.completed.putBoolean(battle.toString(),true);state.setDirty();
        }
        state.share(server);
    }
    public static void deliverMaid(MaidBridge.Actor actor){
        GuardianProgress state=get(actor.server);String id=actor.getUUID().toString();int count=state.pendingLegacy.getInt(id);
        if(count<=0||!VoidJourney.evolved(actor))return;
        int delivered=Math.min(64,count);Journey.give(actor,new net.minecraft.world.item.ItemStack(StageFood.LEGACY.get(),delivered));
        state.pendingLegacy.putInt(id,count-delivered);state.setDirty();Curse.data(actor).putBoolean("GuardianDefeated",true);
    }
    public static void sync(ServerPlayer p){GuardianProgress state=get(p.server);state.snapshot(p);state.share(p.server);Curse.sync(p);Network.sendController(p);}
    public static void relationsChanged(ServerPlayer p){sync(p);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)get(p.server).snapshot(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||e.getServer().getTickCount()%100!=0)return;
        MinecraftServer server=e.getServer();GuardianProgress state=get(server);
        for(ServerPlayer p:server.getPlayerList().getPlayers())state.snapshot(p);
        state.share(server);
    }
}
