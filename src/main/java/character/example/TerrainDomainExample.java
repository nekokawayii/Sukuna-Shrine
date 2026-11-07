package character.example;

import character.TechniqueApi;
import character.domain.DomainPresentation;
import character.domain.DomainTerrain;
import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 可编译的地形领域范例，默认不登记技能。先让角色目录包含9101，再调用 install(role)。 */
@Mod.EventBusSubscriber(modid="jujutsu_kaisen_cursed_paths")
public final class TerrainDomainExample {
    public static final int SKILL=9101;
    private static final int RADIUS=12,DURATION=600;
    private static final Map<UUID,Field> FIELDS=new HashMap<>();
    private static final class Field implements DomainClash.Lifecycle {
        final ServerPlayer owner;final ServerLevel level;final BlockPos origin;final Vec3 center;
        final DomainTerrain.Handle terrain;
        final List<BlockPos> plan=new ArrayList<>();
        long end;int cursor;boolean closed;
        Field(ServerPlayer owner){
            this.owner=owner;level=owner.serverLevel();origin=MaidBridge.body(owner).blockPosition();
            center=Vec3.atCenterOf(origin);end=level.getGameTime()+DURATION;
            terrain=DomainTerrain.begin(level,owner.getUUID());
            for(int x=-RADIUS;x<=RADIUS;x++)for(int z=-RADIUS;z<=RADIUS;z++){
                if(x*x+z*z>RADIUS*RADIUS)continue;
                plan.add(origin.offset(x,-1,z));
                for(int y=0;y<=RADIUS;y++)if(x*x+y*y+z*z<=RADIUS*RADIUS)plan.add(origin.offset(x,y,z));
            }
        }
        public boolean active(){return !closed&&FIELDS.get(owner.getUUID())==this&&owner.isAlive()&&owner.level()==level&&!owner.hasDisconnected();}
        public void extend(int ticks){end+=Math.max(0,ticks);}
        public void close(){
            if(closed)return;closed=true;FIELDS.remove(owner.getUUID(),this);plan.clear();terrain.close();
            DomainClash.closed(owner);Curse.endDomain(owner,false);
        }
        public void clash(DomainClash.ClashView view){
            // 可用 DomainClash.partition(owner.getUUID()) 查询同一切面。
            // 共用 terrain 与天空自动裁切，这里不重新放整球，也不自行延长计时。
        }
        public void resolved(DomainClash.Outcome outcome){
            // WON 已经统一延长600刻并解除裁切；LOST/CANCELLED 已经调用 close。
        }
        void tick(){
            if(!active()||level.getGameTime()>=end){close();return;}
            // 对拼时仍提交结构计划，由共用事务决定哪半边可见。
            for(int budget=256;budget>0&&cursor<plan.size();budget--){
                var pos=plan.get(cursor++);double squared=pos.distSqr(origin);
                var target=pos.getY()==origin.getY()-1?Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                    :squared>(RADIUS-1)*(RADIUS-1)?Blocks.BLACK_CONCRETE.defaultBlockState():Blocks.AIR.defaultBlockState();
                terrain.place(pos,target);
            }
            if(DomainClash.active(owner))return;
            // 此处才做必中伤害、扣维持费等效果，玩家与NPC共用一份实现。
        }
    }
    public static void install(String role){
        TechniqueApi.registerDomain(role,SKILL,(caster,skill,rank)->open(TechniqueApi.actor(caster.body())));
    }
    public static void open(ServerPlayer owner){
        if(owner==null||!owner.isAlive())return;
        if(DomainClash.registered(owner)){DomainClash.withdraw(owner);return;}
        if(!character.mainline.Mainline.requireDomain(owner)||DomainClash.hasDomain(owner))return;
        var field=new Field(owner);FIELDS.put(owner.getUUID(),field);
        TechniqueApi.opened(owner,field.center,RADIUS,field,
            DomainPresentation.Style.image("jujutsu_kaisen_cursed_paths:textures/block/yuki_star_shell.png",0x77BBFF));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase==TickEvent.Phase.END)for(var field:List.copyOf(FIELDS.values()))field.tick();
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event){for(var field:List.copyOf(FIELDS.values()))field.close();}
    private TerrainDomainExample(){ }
}
